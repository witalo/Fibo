package com.example.fibo.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
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