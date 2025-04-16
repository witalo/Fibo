package com.example.fibo.reports

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    // Estados del ViewModel
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val printers by viewModel.availablePrinters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val currentUiState = uiState

    // Estados locales - usar remember para evitar recreaciones
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBluetoothAlert by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPrintingInProgress by remember { mutableStateOf(false) }

    // Estados y permisos
    val bluetoothManager = LocalContext.current.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    val (hasPermissions, requestPermissions) = rememberBluetoothPermissionsState()

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Esperar un momento para asegurar que Bluetooth esté completamente inicializado
            viewModel.scanForPrinters(context)
        } else {
            errorMessage = "Bluetooth no fue activado"
        }
    }
    // Función para activar Bluetooth con verificación de permisos
    fun enableBluetooth() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()
                    return
                }
            }

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothLauncher.launch(enableBtIntent)
        } catch (e: SecurityException) {
            errorMessage = "Permiso denegado: ${e.message}"
            requestPermissions()
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        }
    }

    // Manejo de estados del ViewModel - optimizado para manejar cambios de estado eficientemente
    LaunchedEffect(uiState) {
        when (uiState) {
            is PdfDialogUiState.Error -> {
                errorMessage = (uiState as PdfDialogUiState.Error).message
                if (errorMessage?.contains("permiso", ignoreCase = true) == true) {
                    showPermissionDialog = true
                }
                isPrintingInProgress = false
            }
            is PdfDialogUiState.BluetoothDisabled -> {
                showBluetoothAlert = true
                isPrintingInProgress = false
            }
            is PdfDialogUiState.Printing -> {
                isPrintingInProgress = true
            }
            is PdfDialogUiState.PrintComplete -> {
                isPrintingInProgress = false
                delay(500)
                onPrintSuccess()
            }
            else -> {
                if (uiState !is PdfDialogUiState.ScanningPrinters) {
                    isPrintingInProgress = false
                }
            }
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Botón Bluetooth/Permisos
        Button(
            onClick = {
                when {
                    !hasPermissions -> requestPermissions()
                    !isBluetoothActive -> enableBluetooth()
                    else -> viewModel.scanForPrinters(context)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPrintingInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    !isBluetoothActive -> Color(0xFFFF9800)
                    !hasPermissions -> Color(0xFFFF5722)
                    else -> Color(0xFF2196F3)
                }
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
                            !isBluetoothActive -> "Activar Bluetooth"
                            !hasPermissions -> "Solicitar Permisos"
                            selectedPrinter != null -> "Impresora: ${selectedPrinter?.name?.take(10) ?: ""}..."
                            else -> "Buscar impresoras"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Selector de impresoras - optimizado para evitar recreaciones innecesarias
        if (printers.isNotEmpty() || currentUiState is PdfDialogUiState.ScanningPrinters) {
            PrinterSelector(
                printers = printers,
                selectedPrinter = selectedPrinter,
                onPrinterSelected = { viewModel.selectPrinter(it) },
                isLoading = currentUiState is PdfDialogUiState.ScanningPrinters,
                enabled = !isPrintingInProgress
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Imprimir
        Button(
            onClick = {
                pdfFile?.let {
                    isPrintingInProgress = true
                    viewModel.printOperation(context, it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPrinter != null &&
                    pdfFile != null &&
                    isBluetoothActive &&
                    hasPermissions &&
                    !isPrintingInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedPrinter != null) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
            )
        ) {
            if (isPrintingInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Imprimir"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imprimir")
                }
            }
        }

        // Mostrar errores
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    // Diálogo para activar Bluetooth - usando una variable independiente para controlar visibilidad
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
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothLauncher.launch(enableBtIntent)
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        }
                    }
                ) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBluetoothAlert = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para permisos faltantes
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permisos Requeridos") },
            text = { Text("La aplicación necesita permisos de Bluetooth para buscar impresoras") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        requestPermissions()
                    }
                ) {
                    Text("Conceder Permisos")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de impresión en progreso - utilizamos un estado local para controlar esto
    if (isPrintingInProgress) {
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
    isLoading: Boolean,
    enabled: Boolean = true
){
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
                modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.6f),
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
                            onSelect = { if (enabled) onPrinterSelected(printer) }
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