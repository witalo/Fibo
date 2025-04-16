package com.example.fibo.reports
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberBluetoothPermissionsState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Actualizar estado de permisos después de la respuesta
        val allGranted = permissions.all { it.value }
        hasPermissions = allGranted

        // Registrar en log para depuración
        Log.d("BluetoothPermissions", "Permisos actualizados: $allGranted")
    }

    val requestPermissions = remember {
        {
            val permissionsToRequest = mutableListOf<String>().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                }
                // Para versiones anteriores a Android 12
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.R
                ) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                hasPermissions = true
            }
        }
    }

    // Verificar permisos al inicio
    LaunchedEffect(Unit) {
        hasPermissions = checkBluetoothPermissions(context)
    }

    return Pair(hasPermissions, requestPermissions)
}

fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}