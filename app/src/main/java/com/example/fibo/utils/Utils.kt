package com.example.fibo.utils

import android.os.Build
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