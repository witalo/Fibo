package com.example.fibo.ui.screens.noteofsale

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.model.IOperation
import com.example.fibo.ui.components.BluetoothStatusUI
import com.example.fibo.utils.MyBluetoothState
import com.example.fibo.utils.PdfState
import com.example.fibo.utils.downloadAndSavePdf
import com.example.fibo.viewmodels.NoteOfSaleViewModel
import java.io.File


@Composable
fun NoteOfSalePdfDialog(
    isVisible: Boolean,
    noteOfSale: IOperation,
    onDismiss: () -> Unit,
    viewModel: NoteOfSaleViewModel = hiltViewModel()
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val context = LocalContext.current
            val pdfUrl = remember { "https://ng.tuf4ctur4.net.pe/operations/quotation/${noteOfSale.id}/" }

            // Estados para el PDF
            val (pdfState, setPdfState) = remember { mutableStateOf<PdfState>(PdfState.Loading) }
            val (pdfFile, setPdfFile) = remember { mutableStateOf<File?>(null) }

            // Estados Bluetooth
            val bluetoothState by viewModel.bluetoothState.collectAsState()
            val selectedDevice = remember { mutableStateOf<BluetoothDevice?>(null) }

            // Cargar PDF al abrir el diálogo
            LaunchedEffect(Unit) {
                try {
                    val file = downloadAndSavePdf(context, pdfUrl, "note_${noteOfSale.id}.pdf")
                    setPdfFile(file)
                    setPdfState(PdfState.Success(file))
                } catch (e: Exception) {
                    setPdfState(PdfState.Error(e.message ?: "Error al cargar PDF"))
                }
            }

            // Resetear estados al cerrar
            DisposableEffect(Unit) {
                onDispose { viewModel.resetBluetoothState() }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
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

                    // Visor de PDF
                    PdfViewerSection(pdfState)

                    // Controles de impresión (solo si PDF está cargado)
                    if (pdfState is PdfState.Success) {
                        PrintControlsSection(
                            bluetoothState = bluetoothState,
                            onEnableBluetooth = { viewModel.enableBluetooth() },
                            onScanDevices = { viewModel.scanForDevices() },
                            onConnectDevice = { device ->
                                selectedDevice.value = device
                                viewModel.connectToDevice(device)
                            },
                            onPrint = {
                                viewModel.printNote(noteOfSale)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(noteOfSale: IOperation, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Nota de Salida ${noteOfSale.serial}-${noteOfSale.correlative}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar")
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun PdfViewerSection(pdfState: PdfState) {
    Box(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        when (pdfState) {
            is PdfState.Loading -> CircularProgressIndicator()
            is PdfState.Success -> PdfViewer(file = pdfState.file)
            is PdfState.Error -> Text(
                text = pdfState.message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
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