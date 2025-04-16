package com.example.fibo.reports

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fibo.utils.PdfDialogUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
@SuppressLint("MissingPermission")
@Composable
fun PrintControlsSection(
    viewModel: PdfDialogViewModel,
    context: Context,
    pdfFile: File?,
    isBluetoothActive: Boolean,
    onPrintSuccess: () -> Unit
) {
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val printers by viewModel.availablePrinters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity
    val currentUiState = uiState

    // Estado local para manejo de errores y diálogos
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBluetoothAlert by remember { mutableStateOf(false) }

    // Manejar los estados
    LaunchedEffect(uiState) {
        when (uiState) {
            is PdfDialogUiState.Error -> {
                errorMessage = (uiState as PdfDialogUiState.Error).message
                // Mostrar Toast sin bloquear la UI
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
            is PdfDialogUiState.BluetoothDisabled -> {
                showBluetoothAlert = true
            }
            is PdfDialogUiState.PrintComplete -> {
                delay(500)
                onPrintSuccess()
            }
            else -> {}
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Botón Bluetooth con manejo seguro
        Button(
            onClick = {
                try {
                    if (isBluetoothActive) {
                        viewModel.scanForPrinters(context)
                    } else {
                        showBluetoothAlert = true
                    }
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error en botón Bluetooth", e)
                    errorMessage = "Error: ${e.message}"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBluetoothActive) Color(0xFF2196F3) else Color(0xFFFF9800)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth"
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (currentUiState is PdfDialogUiState.ScanningPrinters) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscando impresoras...")
                } else {
                    Text(
                        text = when {
                            isBluetoothActive && selectedPrinter != null -> "Impresora: ${selectedPrinter?.name?.take(10) ?: ""}..."
                            isBluetoothActive -> "Buscar impresoras"
                            else -> "Activar Bluetooth"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Mostrar selector de impresoras
        if (printers.isNotEmpty() || currentUiState is PdfDialogUiState.ScanningPrinters) {
            PrinterSelector(
                printers = printers,
                selectedPrinter = selectedPrinter,
                onPrinterSelected = { viewModel.selectPrinter(it) },
                isLoading = currentUiState is PdfDialogUiState.ScanningPrinters
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Imprimir
        Button(
            onClick = {
                pdfFile?.let {
                    viewModel.printOperation(context, it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPrinter != null && pdfFile != null && isBluetoothActive &&
                    currentUiState !is PdfDialogUiState.Printing &&
                    currentUiState !is PdfDialogUiState.ScanningPrinters,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedPrinter != null) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Print,
                contentDescription = "Imprimir"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Imprimir")
        }

        // Mostrar errores si existen
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Diálogo para activar Bluetooth
    if (showBluetoothAlert) {
        AlertDialog(
            onDismissRequest = { showBluetoothAlert = false },
            title = { Text("Bluetooth Desactivado") },
            text = { Text("Es necesario activar el Bluetooth para buscar impresoras. ¿Desea activarlo ahora?") },
            confirmButton = {
                Button(
                    onClick = {
                        showBluetoothAlert = false
                        try {
                            // Usar Intent en lugar de ActivityResultLauncher para mayor compatibilidad
                            activity?.let {
                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                it.startActivity(enableBtIntent)
                            }
                        } catch (e: Exception) {
                            Log.e("Bluetooth", "Error activando", e)
                            errorMessage = "Error: ${e.message}"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Activar")
                }
            },
            dismissButton = {
                Button(onClick = { showBluetoothAlert = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de impresión en progreso
    if (currentUiState is PdfDialogUiState.Printing) {
        AlertDialog(
            onDismissRequest = { /* No cerrar */ },
            title = { Text("Imprimiendo...") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enviando datos a ${selectedPrinter?.name ?: "la impresora"}")
                }
            },
            confirmButton = { /* Sin botones */ }
        )
    }
}
@Composable
fun PrinterSelector(
    printers: List<BluetoothDevice>,
    selectedPrinter: BluetoothDevice?,
    onPrinterSelected: (BluetoothDevice) -> Unit,
    isLoading: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = if (isLoading) "Buscando impresoras..." else
                if (printers.isEmpty()) "No se encontraron dispositivos"
                else "Impresoras disponibles (${printers.size}):",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        } else if (printers.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    items(printers) { printer ->
                        PrinterItem(
                            printer = printer,
                            isSelected = selectedPrinter?.address == printer.address,
                            onSelect = { onPrinterSelected(printer) }
                        )
                        if (printer != printers.last()) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Parece que no hay impresoras emparejadas. Empareje su impresora en Ajustes de Bluetooth.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}