package com.example.fibo.reports

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// Versión mejorada de ComposableBluetoothHandler
class ComposableBluetoothHandler(private val context: Context) {
    private val activity = context as? ComponentActivity
    private var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null
    private var enableBluetoothLauncher: ActivityResultLauncher<Intent>? = null

    private var onPermissionsGranted: (() -> Unit)? = null
    private var onPermissionsDenied: (() -> Unit)? = null
    private var onBluetoothEnabled: (() -> Unit)? = null
    private var onBluetoothNotAvailable: (() -> Unit)? = null
    private var onBluetoothEnableDenied: (() -> Unit)? = null

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        activity?.let {
            permissionsLauncher = it.activityResultRegistry.register(
                "bluetooth_permissions",
                ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Manejo de permisos */ }

            enableBluetoothLauncher = it.activityResultRegistry.register(
                "enable_bluetooth",
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    onBluetoothEnabled?.invoke()
                } else {
                    // Verificar si se activó manualmente
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) {
                            onBluetoothEnabled?.invoke()
                        } else {
                            onBluetoothEnableDenied?.invoke()
                        }
                    }, 1000) // Pequeño delay para asegurar
                }
            }
        }
    }
    var onBluetoothStateChanged: ((Boolean) -> Unit)? = null
    fun checkBluetoothPermissions(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit
    ) {
        this.onPermissionsGranted = onPermissionsGranted
        this.onPermissionsDenied = onPermissionsDenied

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            onPermissionsGranted()
        } else {
            permissionsLauncher?.launch(permissions)
        }
    }

    fun enableBluetooth(
        onBluetoothEnabled: () -> Unit,
        onBluetoothNotAvailable: () -> Unit,
        onBluetoothEnableDenied: () -> Unit,
        onPermissionsRequired: () -> Unit
    ) {
        this.onBluetoothEnabled = onBluetoothEnabled
        this.onBluetoothNotAvailable = onBluetoothNotAvailable
        this.onBluetoothEnableDenied = onBluetoothEnableDenied

        // Verificación de permisos para Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionsRequired()
            return
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        when {
            bluetoothAdapter == null -> {
                onBluetoothNotAvailable()
            }
            bluetoothAdapter.isEnabled -> {
                onBluetoothEnabled()
            }
            else -> {
                try {
                    // Configurar un BroadcastReceiver temporal para detectar cambios
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                                val state = intent.getIntExtra(
                                    BluetoothAdapter.EXTRA_STATE,
                                    BluetoothAdapter.ERROR
                                )
                                if (state == BluetoothAdapter.STATE_ON) {
                                    context.unregisterReceiver(this)
                                    onBluetoothEnabled()
                                }
                            }
                        }
                    }

                    context.registerReceiver(
                        receiver,
                        IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    )

                    // Configurar timeout por si el usuario no actúa
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            context.unregisterReceiver(receiver)
                            if (!bluetoothAdapter.isEnabled) {
                                onBluetoothEnableDenied()
                            }
                        } catch (e: Exception) { /* Ya desregistrado */ }
                    }, 30000) // 30 segundos timeout

                    // Lanzar diálogo de activación
                    enableBluetoothLauncher?.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )

                } catch (e: Exception) {
                    Log.e("BluetoothHandler", "Error al activar Bluetooth", e)
                    onBluetoothEnableDenied()
                }
            }
        }
    }


    fun cleanup() {
        enableBluetoothLauncher?.unregister()
        enableBluetoothLauncher = null
        onBluetoothEnabled = null
        onBluetoothNotAvailable = null
        onBluetoothEnableDenied = null
    }
}