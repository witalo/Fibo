package com.example.fibo.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Clase que maneja los permisos de Bluetooth y su activación
 */
class BluetoothPermissionHandler(private val activity: ComponentActivity) {

    // Lanzador para solicitar activar Bluetooth
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>

    // Lanzador para solicitar permisos de Bluetooth
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Callback para cuando el Bluetooth sea activado
    private var onBluetoothEnabled: (() -> Unit)? = null

    // Callback para cuando se concedan los permisos
    private var onPermissionsGranted: (() -> Unit)? = null

    init {
        // Inicializar los lanzadores
        initializeBluetoothLauncher()
        initializePermissionLauncher()
    }

    private fun initializeBluetoothLauncher() {
        bluetoothEnableLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth activado correctamente
                onBluetoothEnabled?.invoke()
            } else {
                // El usuario rechazó activar Bluetooth
                // Aquí podrías mostrar un mensaje o realizar otra acción
            }
        }
    }

    private fun initializePermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Verificar si todos los permisos necesarios fueron concedidos
            val allGranted = permissions.entries.all { it.value }

            if (allGranted) {
                onPermissionsGranted?.invoke()
            } else {
                // Algunos permisos fueron denegados
                // Aquí podrías mostrar un mensaje o realizar otra acción
            }
        }
    }

    /**
     * Solicita los permisos necesarios para Bluetooth según la versión de Android
     */
    fun requestBluetoothPermissions(onPermissionsGranted: () -> Unit) {
        this.onPermissionsGranted = onPermissionsGranted

        val permissionsToRequest = mutableListOf<String>()

        // Permisos necesarios según la versión de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) y superior requiere estos permisos
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            // Versiones anteriores a Android 12
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Verificar si ya se tienen los permisos
        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNotGranted.isEmpty()) {
            // Si ya se tienen todos los permisos, llamar al callback directamente
            onPermissionsGranted.invoke()
        } else {
            // Solicitar los permisos faltantes
            permissionLauncher.launch(permissionsNotGranted)
        }
    }

    /**
     * Activa el Bluetooth si no está activo
     */
    fun enableBluetooth(onBluetoothEnabled: () -> Unit) {
        this.onBluetoothEnabled = onBluetoothEnabled

        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Dispositivo no soporta Bluetooth
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            // Verificar permisos para Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Si no tenemos permisos, los solicitamos primero
                    requestBluetoothPermissions {
                        // Una vez tenemos permisos, activamos Bluetooth
                        enableBluetoothWithIntent(bluetoothAdapter)
                    }
                    return
                }
            }

            // Activar Bluetooth con Intent
            enableBluetoothWithIntent(bluetoothAdapter)
        } else {
            // Si Bluetooth ya está activo, llamar al callback directamente
            onBluetoothEnabled.invoke()
        }
    }

    private fun enableBluetoothWithIntent(bluetoothAdapter: BluetoothAdapter) {
        // En Android 12+, necesitamos verificar el permiso BLUETOOTH_CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Usar Intent para activar Bluetooth
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher.launch(enableBtIntent)
    }
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true // For older versions, we don't need explicit permission for device name
        }
    }
}