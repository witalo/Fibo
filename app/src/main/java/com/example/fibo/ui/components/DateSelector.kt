package com.example.fibo.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    currentDate: String,
    onDateSelected: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // Parsear la fecha actual
    val initialDate = remember(currentDate) {
        try {
            dateFormatter.parse(currentDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    // Define el degradado blanco (puedes ajustar los colores)
    val whiteGradient = Brush.horizontalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),  // Blanco con ligera transparencia
            Color.White.copy(alpha = 1f),    // Blanco sólido
            Color.White.copy(alpha = 0.8f)  // Blanco con ligera transparencia
        )
    )

    TextButton(
        onClick = { showDatePicker = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.Transparent
        )
    ) {
        // Texto con degradado
        Text(
            text = currentDate,
            style = MaterialTheme.typography.bodyLarge.copy(
                brush = whiteGradient
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Icono blanco sólido (sin degradado)
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Fecha",
            modifier = Modifier.size(20.dp),
            tint = Color.White // Fijo en blanco
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcCalendar =
                                Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = millis
                                }
                            onDateSelected(dateFormatter.format(utcCalendar.time))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Seleccionar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorLimit(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    daysBefore: Int = 3,  // Días anteriores permitidos (default: 3)
    daysAfter: Int = 0    // Días posteriores permitidos (default: 0 = solo hoy)
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // Calculamos los límites de fecha
    val calendar = remember { Calendar.getInstance(TimeZone.getTimeZone("UTC")) }
    val currentTime = calendar.timeInMillis

    // Fecha mínima permitida (díasBefore días antes de hoy)
    val minDate = remember(daysBefore) {
        calendar.apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, -daysBefore)
        }.timeInMillis
    }

    // Fecha máxima permitida (díasAfter días después de hoy)
    val maxDate = remember(daysAfter) {
        calendar.apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, daysAfter)
        }.timeInMillis
    }

    // Parsear la fecha actual
    val initialDate = remember(currentDate) {
        try {
            val parsed = dateFormatter.parse(currentDate)?.time
            // Aseguramos que la fecha inicial esté dentro de los límites
            when {
                parsed == null -> currentTime
                parsed < minDate -> minDate
                parsed > maxDate -> maxDate
                else -> parsed
            }
        } catch (e: Exception) {
            currentTime
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate,
        yearRange = IntRange(
            Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.get(Calendar.YEAR),
            Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.get(Calendar.YEAR)
        ),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in minDate..maxDate
            }

            override fun isSelectableYear(year: Int): Boolean {
                val minYear = Calendar.getInstance().apply { timeInMillis = minDate }.get(Calendar.YEAR)
                val maxYear = Calendar.getInstance().apply { timeInMillis = maxDate }.get(Calendar.YEAR)
                return year in minYear..maxYear
            }
        }
    )

    // Define el degradado blanco
    val whiteGradient = Brush.horizontalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.8f),
            Color.White.copy(alpha = 1f),
            Color.White.copy(alpha = 0.8f)
        )
    )

    TextButton(
        onClick = { showDatePicker = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.Transparent
        ),
        modifier = Modifier.height(48.dp)
    ) {
        Text(
            text = currentDate.ifEmpty { dateFormatter.format(Date(currentTime)) },
            style = MaterialTheme.typography.bodyLarge.copy(
                brush = whiteGradient
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Fecha",
            modifier = Modifier.size(20.dp),
            tint = Color.White
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            if (millis in minDate..maxDate) {
                                val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = millis
                                }
                                onDateSelected(dateFormatter.format(utcCalendar.time))
                            } else {
                                Toast.makeText(
                                    context,
                                    "Solo se permiten fechas desde ${dateFormatter.format(Date(minDate))} hasta ${dateFormatter.format(Date(maxDate))}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Seleccionar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancelar", color = Color.White)
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}