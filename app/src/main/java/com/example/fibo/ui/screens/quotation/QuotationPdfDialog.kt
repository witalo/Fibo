package com.example.fibo.ui.screens.quotation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.fibo.model.IOperation
import com.example.fibo.utils.BluetoothState
import com.example.fibo.utils.OperationState
import com.example.fibo.utils.PdfState
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationPdfDialog(
    isVisible: Boolean,
    quotation: IOperation,
    onDismiss: () -> Unit,
    viewModel: QuotationPdfViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State variables
    val pdfState by viewModel.pdfState.collectAsState()
    val bluetoothState by viewModel.bluetoothState.collectAsState()
    val devices by viewModel.devicesList.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    var showDevicesList by remember { mutableStateOf(false) }
    var printingInProgress by remember { mutableStateOf(false) }
    // Estado de la operación detallada
    val quotationState by viewModel.quotationState.collectAsState()

    // Request permissions
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.updateBluetoothState(BluetoothState.Enabled)
        } else {
            viewModel.updateBluetoothState(BluetoothState.Error("Permisos de Bluetooth denegados"))
        }
    }

    // Handle Bluetooth connection
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == -1) { // RESULT_OK
            viewModel.updateBluetoothState(BluetoothState.Enabled)
        } else {
            viewModel.updateBluetoothState(BluetoothState.Error("Bluetooth no activado"))
        }
    }

    // Generate and load PDF when dialog opens
    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.resetAll() // Limpiar antes de generar nuevo
            viewModel.getQuotationById(quotation.id)
//            viewModel.generatePdf(quotation, context)
        }
    }
    // Efecto para generar el PDF cuando tenemos los datos detallados
    LaunchedEffect(quotationState) {
        if (quotationState is OperationState.Success) {
            val detailedQuotation = (quotationState as OperationState.Success).operation
            viewModel.generatePdf(detailedQuotation, context)
        }
    }

    // Cleanup resources when dialog closes
    DisposableEffect(isVisible) {
        onDispose {
            if (!isVisible) {
                viewModel.cleanup()
                viewModel.resetAll()
            }
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = {
                viewModel.resetAll()
                onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(0.95f)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Dialog header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFAF330C),
                                        Color(0xFFDC870A),
                                    )
                                )
                            )
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Cotización ${quotation.serial}-${quotation.correlative}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }

                    // PDF Viewer area (70% of height)
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        when (pdfState) {
                            is PdfState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Cargando PDF...")
                                    }
                                }
                            }
                            is PdfState.Success -> {
                                (pdfState as PdfState.Success).file.let { file ->
                                    PdfViewerAndroidView(
                                        file = file,
                                        modifier = Modifier.fillMaxSize(),
                                        onPdfLoadComplete = {
                                            // Opcional: Acciones cuando el PDF termina de cargar
                                        }
                                    )
                                }
                            }
                            is PdfState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Error al cargar el PDF",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = (pdfState as PdfState.Error).message,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                viewModel.generatePdf(quotation, context)
                                            }
                                        ) {
                                            Text("Reintentar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Bluetooth section (30% of height)
                    Column(
                        modifier = Modifier
                            .weight(0.3f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())  // <-- Añade esto para hacer scrollable
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Impresión Bluetooth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bluetooth status indicator
                        BluetoothStatusIndicator(bluetoothState)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bluetooth action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Button to toggle Bluetooth
                            Button(
                                onClick = {
                                    if (bluetoothState is BluetoothState.Disabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val permissions = arrayOf(
                                                Manifest.permission.BLUETOOTH_CONNECT,
                                                Manifest.permission.BLUETOOTH_SCAN
                                            )

                                            val allGranted = permissions.all {
                                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                            }

                                            if (!allGranted) {
                                                bluetoothPermissionLauncher.launch(permissions)
                                            } else {
                                                // Turn on Bluetooth
                                                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                                val bluetoothAdapter = bluetoothManager.adapter

                                                if (bluetoothAdapter != null) {
                                                    if (!bluetoothAdapter.isEnabled) {
                                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                        enableBluetoothLauncher.launch(enableBtIntent)
                                                    } else {
                                                        viewModel.updateBluetoothState(BluetoothState.Enabled)
                                                    }
                                                }
                                            }
                                        } else {
                                            // For older Android versions
                                            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                            val bluetoothAdapter = bluetoothManager.adapter

                                            if (bluetoothAdapter != null) {
                                                if (!bluetoothAdapter.isEnabled) {
                                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                    enableBluetoothLauncher.launch(enableBtIntent)
                                                } else {
                                                    viewModel.updateBluetoothState(BluetoothState.Enabled)
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (bluetoothState) {
                                        is BluetoothState.Disabled -> MaterialTheme.colorScheme.primary
                                        is BluetoothState.Enabled,
                                        is BluetoothState.Connected -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                ),
                                enabled = bluetoothState is BluetoothState.Disabled ||
                                        bluetoothState is BluetoothState.Enabled ||
                                        bluetoothState is BluetoothState.Connected,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (bluetoothState) {
                                            is BluetoothState.Disabled -> Icons.Default.BluetoothDisabled
                                            else -> Icons.Default.Bluetooth
                                        },
                                        contentDescription = "Estado Bluetooth"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (bluetoothState) {
                                            is BluetoothState.Disabled -> "Activar Bluetooth"
                                            is BluetoothState.Enabled -> "Bluetooth Activado"
                                            is BluetoothState.Connected -> "Conectado"
                                            else -> "Activar Bluetooth"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Button to scan for devices
                            Button(
                                onClick = {
                                    // CORRECCIÓN: Asegurarse de que la lista se muestre cuando se haga clic
                                    if (bluetoothState is BluetoothState.Enabled ||
                                        bluetoothState is BluetoothState.DevicesFound) {
                                        showDevicesList = true
                                        viewModel.scanForDevices(context)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showDevicesList)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                enabled = bluetoothState is BluetoothState.Enabled ||
                                        bluetoothState is BluetoothState.DevicesFound ||
                                        bluetoothState is BluetoothState.Scanning,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (bluetoothState is BluetoothState.Scanning)
                                            Icons.Default.BluetoothSearching
                                        else
                                            Icons.Default.Search,
                                        contentDescription = "Buscar dispositivos"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (bluetoothState is BluetoothState.Scanning)
                                            "Buscando..."
                                        else
                                            "Buscar Impresora"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Print button
                        Button(
                            onClick = {
                                printingInProgress = true
                                coroutineScope.launch {
                                    try {
                                        // Simulate printing process
                                        delay(2000)
                                        selectedDevice?.let { device ->
                                            viewModel.connectAndPrint(context, device, quotation)
                                        }
                                    } finally {
                                        printingInProgress = false
                                    }
                                }
                            },
                            enabled = selectedDevice != null && !printingInProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDevice != null && !printingInProgress) {
                                    // Color cuando está habilitado y listo para imprimir
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    // Color cuando está deshabilitado
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                },
                                contentColor = if (selectedDevice != null && !printingInProgress) {
                                    // Color del contenido cuando está habilitado
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    // Color del contenido cuando está deshabilitado
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                },
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (printingInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = "Imprimir",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (printingInProgress) "Imprimiendo..." else "Imprimir",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Show print progress indicator when printing
                        if (printingInProgress) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // CORRECCIÓN: La lista de dispositivos ahora siempre está visible cuando showDevicesList es true
                        // en lugar de basarse en el estado de Bluetooth
                        if (showDevicesList) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Dispositivos Disponibles",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Show scanning indicator or device list
                                    if (bluetoothState is BluetoothState.Scanning) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Buscando dispositivos...",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    } else if (devices.isEmpty()) {
                                        Text(
                                            text = "No se encontraron dispositivos",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        // CORRECCIÓN: Aumentamos la altura máxima de la lista para asegurar que es visible
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp) // Aumentamos la altura para mayor visibilidad
                                        ) {
                                            items(devices) { device ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.selectDevice(device)
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = selectedDevice?.address == device.address,
                                                        onClick = { viewModel.selectDevice(device) }
                                                    )

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = device.name ?: "Dispositivo desconocido",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = device.address,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun BluetoothStatusIndicator(bluetoothState: BluetoothState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when (bluetoothState) {
                    is BluetoothState.Disabled -> Color(0xFFE5E5E5)
                    is BluetoothState.Enabled -> Color(0xFFD0F0D0)
                    is BluetoothState.Scanning -> Color(0xFFE0E0FF)
                    is BluetoothState.DevicesFound -> Color(0xFFD0F0D0)
                    is BluetoothState.Connected -> Color(0xFFD0F0FF)
                    is BluetoothState.Error -> Color(0xFFFFE0E0)
                }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (bluetoothState) {
                is BluetoothState.Disabled -> Icons.Default.BluetoothDisabled
                is BluetoothState.Scanning -> Icons.Default.BluetoothSearching
                is BluetoothState.Error -> Icons.Default.BluetoothDisabled
                else -> Icons.Default.Bluetooth
            },
            contentDescription = "Estado Bluetooth",
            tint = when (bluetoothState) {
                is BluetoothState.Disabled -> Color.Gray
                is BluetoothState.Enabled,
                is BluetoothState.DevicesFound,
                is BluetoothState.Connected -> Color(0xFF2E7D32)
                is BluetoothState.Scanning -> Color(0xFF1976D2)
                is BluetoothState.Error -> Color(0xFFD32F2F)
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = when (bluetoothState) {
                is BluetoothState.Disabled -> "Bluetooth desactivado"
                is BluetoothState.Enabled -> "Bluetooth activado"
                is BluetoothState.Scanning -> "Buscando dispositivos..."
                is BluetoothState.DevicesFound -> "Dispositivos encontrados"
                is BluetoothState.Connected -> "Conectado a impresora"
                is BluetoothState.Error -> (bluetoothState as BluetoothState.Error).message
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when (bluetoothState) {
                is BluetoothState.Disabled -> Color.Gray
                is BluetoothState.Error -> Color(0xFFD32F2F)
                else -> Color.Black
            }
        )
    }
}
