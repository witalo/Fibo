package com.example.fibo.ui.screens.noteofsale

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.model.IOperation
import com.example.fibo.ui.components.BluetoothStatusUI
import com.example.fibo.utils.MyBluetoothState
import com.example.fibo.utils.PdfState
import com.example.fibo.utils.downloadAndSavePdf
import com.example.fibo.viewmodels.NoteOfSaleViewModel
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import kotlinx.coroutines.launch
import java.io.File


@Composable
fun NoteOfSalePdfDialog(
    isVisible: Boolean,
    noteOfSale: IOperation,
    onDismiss: () -> Unit,
    viewModel: NoteOfSaleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Solicitar permisos Bluetooth en Android 12+
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.enableBluetooth()
        }
    }
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val context = LocalContext.current
            val pdfUrl = remember { "https://ng.tuf4ctur4.net.pe/operations/quotation/${noteOfSale.id}/" }

            // Estados para el PDF
            val (pdfState, setPdfState) = remember { mutableStateOf<PdfState>(PdfState.Loading) }

            // Estados Bluetooth
            val bluetoothState by viewModel.bluetoothState.collectAsState()
            val selectedDevice = remember { mutableStateOf<BluetoothDevice?>(null) }

            // Cargar PDF al abrir el diálogo
            LaunchedEffect(Unit) {
                try {
                    coroutineScope.launch {
                        try {
                            val file = downloadAndSavePdf(context, pdfUrl, "note_${noteOfSale.id}.pdf")
                            setPdfState(PdfState.Success(file))
                        } catch (e: Exception) {
                            setPdfState(PdfState.Error(e.message ?: "Error al cargar PDF"))
                        }
                    }
                } catch (e: Exception) {
                    setPdfState(PdfState.Error(e.message ?: "Error al cargar PDF"))
                }
            }

            // Resetear estados al cerrar
            DisposableEffect(Unit) {
                onDispose { viewModel.resetBluetoothState() }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize(0.95f)  // Pequeño margen para no ocupar toda la pantalla
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    DialogHeader(noteOfSale, onDismiss)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Contenedor principal (PDF + Controles)
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Visor de PDF (70% del ancho)
                        Box(
                            modifier = Modifier
                                .weight(0.7f)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (pdfState) {
                                is PdfState.Loading -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Cargando PDF...")
                                    }
                                }
                                is PdfState.Success -> {
                                    ImprovedPdfViewer(file = (pdfState as PdfState.Success).file)
                                }
                                is PdfState.Error -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = (pdfState as PdfState.Error).message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                setPdfState(PdfState.Loading)
                                                coroutineScope.launch {
                                                    try {
                                                        val file = downloadAndSavePdf(context, pdfUrl, "note_${noteOfSale.id}.pdf")
                                                        setPdfState(PdfState.Success(file))
                                                    } catch (e: Exception) {
                                                        setPdfState(PdfState.Error(e.message ?: "Error al cargar PDF"))
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Reintentar")
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        // Controles de impresión (30% del ancho)
                        Column(
                            modifier = Modifier
                                .weight(0.3f)
                                .fillMaxHeight()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Controles de Impresión",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Sección de Bluetooth
                                    BluetoothStatusUI(
                                        state = bluetoothState,
                                        onEnableBluetooth = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                bluetoothPermissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.BLUETOOTH_CONNECT,
                                                        Manifest.permission.BLUETOOTH_SCAN
                                                    )
                                                )
                                            } else {
                                                viewModel.enableBluetooth()
                                            }
                                        },
                                        onScanDevices = { viewModel.scanForDevices() },
                                        onConnectDevice = { device ->
                                            selectedDevice.value = device
                                            viewModel.connectToDevice(device)
                                        },
                                        onPrint = {
                                            viewModel.printNote(noteOfSale)
                                        },
                                        onDisconnect = {
                                            viewModel.resetBluetoothState()
                                        }
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
@Composable
private fun DialogHeader(noteOfSale: IOperation, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Nota de Salida ${noteOfSale.serial}-${noteOfSale.correlative}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Divider()
}

@Composable
fun ImprovedPdfViewer(file: File) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PDFView(context, null).apply {
                fromFile(file)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableDoubletap(true)
                    .defaultPage(0)
                    .enableAnnotationRendering(true)
                    .scrollHandle(DefaultScrollHandle(context))
                    .spacing(10)
                    .load()
            }
        }
    )
}

@Composable
private fun PrintControlsSection(
    bluetoothState: MyBluetoothState,
    onEnableBluetooth: () -> Unit,
    onScanDevices: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onPrint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Estado actual Bluetooth
            BluetoothStatusUI(
                state = bluetoothState,
                onEnableBluetooth = onEnableBluetooth,
                onScanDevices = onScanDevices,
                onConnectDevice = onConnectDevice,
                onPrint = onPrint,
                onDisconnect = { /* Implementar si es necesario */ }
            )
        }
    }
}
// PdfViewer.kt (simplificado)
@Composable
fun PdfViewer(file: File) {
    // Implementación básica - en producción usaría una librería como AndroidPdfViewer
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Visualización de PDF: ${file.name}")
    }
}