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
import android.os.Handler
import android.os.Looper
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
import com.example.fibo.model.IGuideData
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.CancellationException

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
    var isBluetoothOperationInProgress by remember { mutableStateOf(false) }

    // Track Bluetooth state locally to ensure UI updates
    var localBluetoothActive by remember { mutableStateOf(isBluetoothActive) }

    // Estados y permisos
    val bluetoothManager = LocalContext.current.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    val (hasPermissions, requestPermissions) = rememberBluetoothPermissionsState()

    // Usar CoroutineScope limitado al tiempo de vida del composable
    val composableScope = rememberCoroutineScope()

    // Referencia al trabajo de monitoreo Bluetooth para poder cancelarlo
    var bluetoothMonitorJob by remember { mutableStateOf<Job?>(null) }

    // Continuous check for Bluetooth state with job reference for clean cancellation
    LaunchedEffect(Unit) {
        bluetoothMonitorJob = launch {
            try {
                while (isActive) { // isActive es una propiedad de la corrutina que indica si está activa
                    localBluetoothActive = bluetoothAdapter?.isEnabled == true
                    delay(1000) // Check every second
                }
            } catch (e: CancellationException) {
                // Cancelación normal, no hacer nada
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error checking Bluetooth state", e)
            }
        }
    }
    // Efecto de limpieza
    DisposableEffect(Unit) {
        onDispose {
            bluetoothMonitorJob?.cancel()
            Log.d("PrintControlsSection", "Disposable effect cleanup executed")
        }
    }
    // Añadir este efecto en PrintControlsSection
    LaunchedEffect(hasPermissions) {
        // Si los permisos han sido concedidos, detener el indicador de progreso
        if (hasPermissions && isBluetoothOperationInProgress) {
            isBluetoothOperationInProgress = false

            // Si también tenemos Bluetooth activo, podemos iniciar el escaneo automáticamente
            if (localBluetoothActive) {
                try {
                    viewModel.scanForPrinters(context)
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error escaneando después de permiso", e)
                    errorMessage = "Error al buscar impresoras: ${e.message}"
                }
            }
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Directly check current Bluetooth state instead of relying on result code
        val isEnabled = bluetoothAdapter?.isEnabled == true
        localBluetoothActive = isEnabled

        if (isEnabled) {
            // Use Handler to give Bluetooth system a moment to fully initialize
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    viewModel.scanForPrinters(context)
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error scanning printers", e)
                    errorMessage = "Error al buscar impresoras: ${e.message}"
                    isBluetoothOperationInProgress = false
                }
            }, 500)
        } else {
            errorMessage = "Bluetooth no fue activado"
            isBluetoothOperationInProgress = false
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
            isBluetoothOperationInProgress = false
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
            isBluetoothOperationInProgress = false
        }
    }

    // Manejo de estados del ViewModel - optimizado para manejar cambios de estado eficientemente
    LaunchedEffect(uiState) {
        try {
            when (uiState) {
                is PdfDialogUiState.Error -> {
                    errorMessage = (uiState as PdfDialogUiState.Error).message
                    if (errorMessage?.contains("permiso", ignoreCase = true) == true) {
                        showPermissionDialog = true
                    }
                    isPrintingInProgress = false
                    isBluetoothOperationInProgress = false
                }
                is PdfDialogUiState.BluetoothDisabled -> {
                    showBluetoothAlert = true
                    isPrintingInProgress = false
                    isBluetoothOperationInProgress = false
                }
                is PdfDialogUiState.Printing -> {
                    isPrintingInProgress = true
                }
                is PdfDialogUiState.PrintComplete -> {
                    isPrintingInProgress = false
                    // Asegurarse de completar correctamente antes de llamar onPrintSuccess
                    //viewModel.resetState()
                   // delay(500)
                    // Usar el scope del composable para el cierre seguro
                    composableScope.launch {
                        try {
                            onPrintSuccess()
                            delay(1000)
                            // Asegurarnos que el resetState se complete
                            withContext(Dispatchers.Main) {
                                viewModel.resetState()
                            }
                        } catch (e: Exception) {
                            Log.e("PrintControlsSection", "Error en onPrintSuccess", e)
                        }
                    }
                }
                is PdfDialogUiState.ScanningPrinters -> {
                    isBluetoothOperationInProgress = true
                }
                else -> {
                    if (uiState !is PdfDialogUiState.ScanningPrinters) {
                        isPrintingInProgress = false
                        isBluetoothOperationInProgress = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrintControlsSection", "Error en LaunchedEffect", e)
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        // Botón Bluetooth/Permisos
        Button(
            onClick = {
                try {
                    when {
                        !hasPermissions -> {
                            isBluetoothOperationInProgress = true
                            requestPermissions()
                        }
                        !localBluetoothActive -> {
                            isBluetoothOperationInProgress = true
                            enableBluetooth()
                        }
                        else -> {
                            isBluetoothOperationInProgress = true
                            viewModel.scanForPrinters(context)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PrintControlsSection", "Error en botón Bluetooth", e)
                    errorMessage = "Error: ${e.message}"
                    isBluetoothOperationInProgress = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isPrintingInProgress && !isBluetoothOperationInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    !localBluetoothActive -> Color(0xFFFF9800)
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

                if (currentUiState is PdfDialogUiState.ScanningPrinters || isBluetoothOperationInProgress) {
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
                            !localBluetoothActive -> "Activar Bluetooth"
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
                isLoading = currentUiState is PdfDialogUiState.ScanningPrinters || isBluetoothOperationInProgress,
                enabled = !isPrintingInProgress
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Imprimir
        Button(
            onClick = {
                try {
                    pdfFile?.let {
                        isPrintingInProgress = true
                        viewModel.printOperation(context, it)
                    }
                } catch (e: Exception) {
                    Log.e("PrintControlsSection", "Error en botón imprimir", e)
                    errorMessage = "Error: ${e.message}"
                    isPrintingInProgress = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPrinter != null &&
                    pdfFile != null &&
                    localBluetoothActive &&
                    hasPermissions &&
                    !isPrintingInProgress &&
                    !isBluetoothOperationInProgress,
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
            onDismissRequest = {
                showBluetoothAlert = false
                isBluetoothOperationInProgress = false
            },
            title = { Text("Bluetooth Desactivado") },
            text = { Text("Es necesario activar el Bluetooth para buscar impresoras. ¿Desea activarlo ahora?") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            showBluetoothAlert = false
                            isBluetoothOperationInProgress = true
                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            bluetoothLauncher.launch(enableBtIntent)
                        } catch (e: Exception) {
                            Log.e("PrintControlsSection", "Error activando Bluetooth", e)
                            errorMessage = "Error: ${e.message}"
                            isBluetoothOperationInProgress = false
                        }
                    }
                ) {
                    Text("Activar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBluetoothAlert = false
                    isBluetoothOperationInProgress = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para permisos faltantes
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                isBluetoothOperationInProgress = false
            },
            title = { Text("Permisos Requeridos") },
            text = { Text("La aplicación necesita permisos de Bluetooth para buscar impresoras") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            showPermissionDialog = false
                            requestPermissions()
                        } catch (e: Exception) {
                            Log.e("PrintControlsSection", "Error en diálogo de permisos", e)
                            isBluetoothOperationInProgress = false
                        }
                    }
                ) {
                    Text("Conceder Permisos")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    isBluetoothOperationInProgress = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de impresión en progreso
    if (isPrintingInProgress && currentUiState is PdfDialogUiState.Printing) {
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

@SuppressLint("MissingPermission")
@Composable
fun GuidesPrintControlsSection(
    viewModel: PdfDialogViewModel,
    context: Context,
    pdfFile: File?,
    guideData: IGuideData?,
    isBluetoothActive: Boolean,
    onPrintSuccess: () -> Unit
) {
    // Estados del ViewModel
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val printers by viewModel.availablePrinters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Estados locales
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBluetoothAlert by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isPrintingInProgress by remember { mutableStateOf(false) }
    var isBluetoothOperationInProgress by remember { mutableStateOf(false) }
    var localBluetoothActive by remember { mutableStateOf(isBluetoothActive) }

    // Estados y permisos
    val bluetoothManager = LocalContext.current.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = remember { bluetoothManager?.adapter }
    val (hasPermissions, requestPermissions) = rememberBluetoothPermissionsState()

    val composableScope = rememberCoroutineScope()
    var bluetoothMonitorJob by remember { mutableStateOf<Job?>(null) }

    // Monitoreo continuo del estado de Bluetooth
    LaunchedEffect(Unit) {
        bluetoothMonitorJob = launch {
            try {
                while (isActive) {
                    localBluetoothActive = bluetoothAdapter?.isEnabled == true
                    delay(1000)
                }
            } catch (e: CancellationException) {
                // Cancelación normal
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error checking Bluetooth state", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bluetoothMonitorJob?.cancel()
            Log.d("GuidesPrintControlsSection", "Disposable effect cleanup executed")
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions && isBluetoothOperationInProgress) {
            isBluetoothOperationInProgress = false
            if (localBluetoothActive) {
                try {
                    viewModel.scanForPrinters(context)
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error escaneando después de permiso", e)
                    errorMessage = "Error al buscar impresoras: ${e.message}"
                }
            }
        }
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val isEnabled = bluetoothAdapter?.isEnabled == true
        localBluetoothActive = isEnabled

        if (isEnabled) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    viewModel.scanForPrinters(context)
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error scanning printers", e)
                    errorMessage = "Error al buscar impresoras: ${e.message}"
                    isBluetoothOperationInProgress = false
                }
            }, 500)
        } else {
            errorMessage = "Bluetooth no fue activado"
            isBluetoothOperationInProgress = false
        }
    }

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
            isBluetoothOperationInProgress = false
        } catch (e: Exception) {
            Log.e("GuidesPrintControlsSection", "Error activando Bluetooth", e)
            errorMessage = "Error al activar Bluetooth: ${e.message}"
            isBluetoothOperationInProgress = false
        }
    }

    // Manejar estados del ViewModel
    LaunchedEffect(uiState) {
        when (uiState) {
            is PdfDialogUiState.PrintComplete -> {
                isPrintingInProgress = false
                try {
                    onPrintSuccess()
                } catch (e: Exception) {
                    Log.e("GuidesPrintControlsSection", "Error en onPrintSuccess", e)
                }
            }
            is PdfDialogUiState.Error -> {
                isPrintingInProgress = false
                errorMessage = (uiState as PdfDialogUiState.Error).message
            }
            is PdfDialogUiState.Printing -> {
                isPrintingInProgress = true
                errorMessage = null
            }
            else -> {
                // No hacer nada para otros estados
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Título
        Text(
            text = "Controles de Impresión",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Botón Bluetooth
        Button(
            onClick = {
                try {
                    if (!localBluetoothActive) {
                        enableBluetooth()
                    } else {
                        if (hasPermissions) {
                            viewModel.scanForPrinters(context)
                        } else {
                            requestPermissions()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GuidesPrintControlsSection", "Error en botón Bluetooth", e)
                    errorMessage = "Error: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBluetoothOperationInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (localBluetoothActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isBluetoothOperationInProgress -> "Procesando..."
                        !localBluetoothActive -> "Activar Bluetooth"
                        printers.isEmpty() -> "Buscar Impresoras"
                        else -> "Actualizar Lista"
                    },
                    color = Color.White
                )
            }
        }

        // Lista de impresoras
        if (printers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Impresoras disponibles:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 120.dp)
            ) {
                items(printers) { printer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPrinter(printer) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPrinter == printer,
                            onClick = { viewModel.selectPrinter(printer) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        printer.name ?: "Dispositivo desconocido"
                                    } else {
                                        "Dispositivo sin permisos"
                                    }
                                } else {
                                    printer.name ?: "Dispositivo desconocido"
                                }
                            } catch (e: SecurityException) {
                                "Dispositivo sin permisos"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Botón de impresión
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                try {
                    selectedPrinter?.let { printer ->
                        guideData?.let { data ->
                            viewModel.printToBluetoothDevice(context, printer, data)
                        } ?: run {
                            errorMessage = "No hay datos de guía para imprimir"
                        }
                    } ?: run {
                        errorMessage = "Selecciona una impresora"
                    }
                } catch (e: Exception) {
                    Log.e("GuidesPrintControlsSection", "Error en botón imprimir", e)
                    errorMessage = "Error al imprimir: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPrinter != null && guideData != null && !isPrintingInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPrintingInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPrintingInProgress) "Imprimiendo..." else "Imprimir Guía",
                    color = Color.White
                )
            }
        }

        // Mostrar errores
        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}