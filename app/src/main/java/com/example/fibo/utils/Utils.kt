package com.example.fibo.utils

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.fibo.model.IProduct
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

// Para obtener la fecha en formato "Y-MM-dd" (o "yyyy-MM-dd" si prefieres año calendario)
fun getCurrentFormattedDate(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    } else {
        val calendar = Calendar.getInstance()
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}

// Para obtener la hora en formato "H:mm:ss" (24 horas)
fun getCurrentFormattedTime(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } else {
        val calendar = Calendar.getInstance()
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendar.time)
    }
}
fun getAffectationColor(typeAffectationId: Int): Color {
    return when (typeAffectationId) {
        1 -> Color(0xFF058F0C) // Gravado - verde claro
        2 -> Color(0xFF00BCD4) // Inafecto - amarillo claro
        3 -> Color(0xFF00BCD4) // Exonerado - celeste
        4 -> Color(0xFFFF9800) // Gratuita - rosado claro
        else -> Color.White
    }
}
fun getAffectationTypeShort(typeAffectationId: Int): String {
    return when (typeAffectationId) {
        1 -> "GRAV" // Gravada
        2 -> "EXON"  // Exonerada
        3 -> "INAF" // Inafecta
        4 -> "GRAT"  // Gratuita
        else -> "N/D"
    }
}
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}