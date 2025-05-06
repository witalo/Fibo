package com.example.fibo.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

object DeviceUtils {
    @SuppressLint("HardwareIds")
    fun getDeviceDescription(context: Context): String {
        return buildString {
            append("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
            append("Pantalla: ${getScreenResolution(context)}\n")
            append("ID: ${getDeviceId(context)}")
        }
    }

    private fun getScreenResolution(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            Log.e("DeviceUtils", "Error al obtener ID", e)
            "error"
        }
    }
}
object BluetoothUtils {
    @SuppressLint("MissingPermission")
    fun getDeviceName(device: BluetoothDevice, context: Context): String {
        return if (hasBluetoothConnectPermission(context)) {
            device.name ?: "Dispositivo Bluetooth"
        } else {
            "Dispositivo (sin permisos)"
        }
    }

    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}