package com.example.fibo.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
            timeZone = TimeZone.getTimeZone("UTC") // Usar UTC para evitar problemas de zona horaria
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

    TextButton(
        onClick = { showDatePicker = true },
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = currentDate)
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Fecha",
            modifier = Modifier.size(20.dp)
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Crear fecha en UTC para evitar desfases
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
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DateSelector(
//    currentDate: String,
//    onDateSelected: (String) -> Unit
//) {
//    var showDatePicker by remember { mutableStateOf(false) }
//    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
//
//    // Parsear la fecha actual sin conversiones complejas
//    val initialDate = remember(currentDate) {
//        try {
//            dateFormatter.parse(currentDate)?.time ?: System.currentTimeMillis()
//        } catch (e: Exception) {
//            System.currentTimeMillis()
//        }
//    }
//
//    val datePickerState = rememberDatePickerState(
//        initialSelectedDateMillis = initialDate
//    )
//
//    TextButton(
//        onClick = { showDatePicker = true },
//        colors = ButtonDefaults.textButtonColors(
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )
//    ) {
//        Text(text = currentDate)
//        Spacer(modifier = Modifier.width(4.dp))
//        Icon(
//            imageVector = Icons.Default.CalendarMonth,
//            contentDescription = "Fecha",
//            modifier = Modifier.size(20.dp)
//        )
//    }
//
//    if (showDatePicker) {
//        DatePickerDialog(
//            onDismissRequest = { showDatePicker = false },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        datePickerState.selectedDateMillis?.let { millis ->
//                            // Formatear directamente sin conversiones de zona horaria
//                            val calendar = Calendar.getInstance().apply {
//                                timeInMillis = millis
//                                // Ajustar a medianoche para evitar desfases
//                                set(Calendar.HOUR_OF_DAY, 0)
//                                set(Calendar.MINUTE, 0)
//                                set(Calendar.SECOND, 0)
//                                set(Calendar.MILLISECOND, 0)
//                            }
//                            onDateSelected(dateFormatter.format(calendar.time))
//                        }
//                        showDatePicker = false
//                    }
//                ) {
//                    Text("Seleccionar")
//                }
//            }
//        ) {
//            DatePicker(state = datePickerState)
//        }
//    }
//}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DateSelector(
//    currentDate: String,
//    onDateSelected: (String) -> Unit
//) {
//    var showDatePicker by remember { mutableStateOf(false) }
//
//    val dateFormatter = remember {
//        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//    }
//
//    val initialDateMillis = remember(currentDate) {
//        try {
//            dateFormatter.parse(currentDate)?.time
//        } catch (e: Exception) {
//            System.currentTimeMillis()
//        }
//    }
//
//    val datePickerState = rememberDatePickerState(
//        initialSelectedDateMillis = initialDateMillis
//    )
//
//    IconButton(onClick = { showDatePicker = true }) {
//        Icon(
//            imageVector = Icons.Default.AddCircle,
//            contentDescription = "Seleccionar fecha"
//        )
//    }
//
//    if (showDatePicker) {
//        DatePickerDialog(
//            onDismissRequest = { showDatePicker = false },
//            confirmButton = {
//                TextButton(
//                    onClick = {
//                        datePickerState.selectedDateMillis?.let {
//                            val selectedDate = dateFormatter.format(Date(it))
//                            onDateSelected(selectedDate)
//                        }
//                        showDatePicker = false
//                    }
//                ) {
//                    Text("Seleccionar")
//                }
//            }
//        ) {
//            DatePicker(state = datePickerState)
//        }
//    }
//}
