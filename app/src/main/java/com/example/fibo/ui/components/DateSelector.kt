package com.example.fibo.ui.components
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
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
                            val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
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