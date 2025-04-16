package com.example.fibo.reports

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fibo.utils.PdfDialogUiState
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

    // Check if bluetooth is scanning
    val isScanning = uiState is PdfDialogUiState.ScanningPrinters
    // LaunchedEffect to handle print complete
    LaunchedEffect(uiState) {
        when (uiState) {
            is PdfDialogUiState.PrintComplete -> {
                onPrintSuccess()
            }
            is PdfDialogUiState.Error -> {
                Toast.makeText(context, (uiState as PdfDialogUiState.Error).message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    // Lanzador para activar Bluetooth
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth activado, buscar impresoras
            viewModel.scanForPrinters(context)
        } else {
            Toast.makeText(context, "Bluetooth requerido", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Botón Bluetooth
        Button(
            onClick = {
                if (isBluetoothActive) {
                    viewModel.scanForPrinters(context)
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothLauncher.launch(enableBtIntent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isBluetoothActive) Color(0xFF2196F3) else Color(0xFFFF9800)
            ),
            enabled = !isScanning && uiState !is PdfDialogUiState.Printing
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Bluetooth"
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isScanning) {
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
                        isBluetoothActive && selectedPrinter != null -> "Impresora: ${selectedPrinter?.name?.take(10)}..."
                        isBluetoothActive -> "Buscar impresoras"
                        else -> "Activar Bluetooth"
                    }
                )
            }
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
                    !isScanning && uiState !is PdfDialogUiState.Printing,
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

        // Lista de impresoras
        if (printers.isNotEmpty()) {
            Text(
                text = "Impresoras disponibles:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(printers) { printer ->
                    PrinterItem(
                        printer = printer,
                        isSelected = selectedPrinter?.address == printer.address,
                        onSelect = { viewModel.selectPrinter(printer) }
                    )
                }
            }
        }
    }

    // Manejar estados de impresión
    if (uiState is PdfDialogUiState.Printing) {
        AlertDialog(
            onDismissRequest = {}, // No permite cerrar haciendo clic fuera
            title = { Text("Imprimiendo...") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enviando datos a la impresora...")
                }
            },
            confirmButton = {}
        )
    }
}
