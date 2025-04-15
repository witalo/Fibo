package com.example.fibo.reports

// 7. Importaciones necesarias para Bluetooth
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.utils.PdfDialogUiState
import com.example.fibo.utils.showToast
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PdfViewerDialog(
    isVisible: Boolean,
    operationId: Int,
    onDismiss: () -> Unit,
    viewModel: PdfDialogViewModel = hiltViewModel(),
) {
    val pdfGenerator = viewModel.pdfGenerator
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val availablePrinters by viewModel.availablePrinters.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Creamos el handler con remember

    val bluetoothHandler = remember { ComposableBluetoothHandler(context) }
    // Estado de Bluetooth
    var isBluetoothActive by remember { mutableStateOf(false) }

    // Verificar estado de Bluetooth
    LaunchedEffect(Unit) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        isBluetoothActive = bluetoothAdapter?.isEnabled == true
    }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    isBluetoothActive = state == BluetoothAdapter.STATE_ON
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Estado para rastrear la solicitud de permisos

    var requestingPermissions by remember { mutableStateOf(false) }
    // Estado para mantener la referencia al archivo PDF generado
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // Efecto para cargar los datos cuando el diálogo es visible
    LaunchedEffect(isVisible, operationId) {
        if (isVisible && operationId > 0) {
            viewModel.fetchOperationById(operationId)
        }
    }
    // Generar el PDF cuando los datos están disponibles
    LaunchedEffect(uiState) {
        if (uiState is PdfDialogUiState.Success) {
            val operation = (uiState as PdfDialogUiState.Success).operation
            try {
                pdfFile = pdfGenerator.generatePdf(context, operation)
            } catch (e: Exception) {
                // Manejar error en la generación del PDF
            }
        }
    }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize()
                ) {
                    // Título del diálogo
                    Text(
                        text = "Comprobante",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Contenido según el estado
                    when (uiState) {
                        is PdfDialogUiState.Initial -> {
                            // Estado inicial, no hacer nada
                        }

                        is PdfDialogUiState.Loading -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is PdfDialogUiState.Success -> {
                            // Estado para controlar si el PDF está listo
                            var isPdfReady by remember { mutableStateOf(false) }

                            // Efecto para cargar el PDF solo una vez
                            LaunchedEffect(Unit) {
                                try {
                                    val operation = (uiState as PdfDialogUiState.Success).operation
                                    pdfFile = pdfGenerator.generatePdf(
                                        context,
                                        operation
                                    ) // Usamos la instancia inyectada
                                    isPdfReady = true
                                } catch (e: Exception) {
                                    // Manejar error si es necesario
                                    Log.e("PdfViewerDialog", "Error al generar PDF", e)
                                }
                            }

                            // Mostrar contenido basado en el estado
                            if (isPdfReady && pdfFile != null && pdfFile!!.exists()) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Visor de PDF con borde
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(4.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        AndroidView(
                                            factory = { context ->
                                                PDFView(context, null).apply {
                                                    fromFile(pdfFile)
                                                        .enableSwipe(true)
                                                        .swipeHorizontal(false)
                                                        .enableDoubletap(true)
                                                        .defaultPage(0)
                                                        .load()
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    // Sección de controles de impresión
                                    PrintControlsSection(
                                        viewModel = viewModel,
                                        context = LocalContext.current,
                                        pdfFile = pdfFile,
                                        isBluetoothActive = isBluetoothActive,
                                        onPrintSuccess = onDismiss
                                    )
                                }
                            } else {
                                // Espera 3 segundos antes de mostrar el error
                                var showError by remember { mutableStateOf(false) }

                                LaunchedEffect(Unit) {
                                    delay(3000)
                                    showError = true
                                }
                                // Muestra un indicador de carga en lugar del mensaje de error
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (showError) {
                                        Text("Error al generar el PDF")
                                    } else {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        is PdfDialogUiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = (uiState as PdfDialogUiState.Error).message,
                                    color = Color.Red
                                )
                                Button(
                                    onClick = { viewModel.fetchOperationById(operationId) },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Intentar nuevamente")
                                }
                            }
                        }

                        is PdfDialogUiState.ScanningPrinters -> {
                            // Mostrar el PDF si existe
                            if (pdfFile != null && pdfFile!!.exists()) {
                                AndroidView(
                                    factory = { ctx ->
                                        PDFView(ctx, null).apply {
                                            fromFile(pdfFile)
                                                .enableSwipe(true)
                                                .swipeHorizontal(false)
                                                .enableDoubletap(true)
                                                .defaultPage(0)
                                                .load()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Buscando impresoras...",
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }

                        is PdfDialogUiState.PrintersFound -> {
                            // Este estado debería ser manejado por Success después del primer escaneo
                        }

                        is PdfDialogUiState.BluetoothDisabled -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Bluetooth desactivado",
                                    color = Color.Red
                                )
                                Text(
                                    text = "Por favor, activa el Bluetooth para buscar impresoras",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Button(
                                    onClick = {
                                        // Verificar permisos antes de escanear
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.BLUETOOTH_SCAN
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) {
                                            viewModel.scanForPrinters(context)
                                        } else {
                                            // Manejar caso cuando no hay permisos
                                            // Puedes lanzar una solicitud de permisos o mostrar un mensaje
                                            Toast.makeText(
                                                context,
                                                "Se necesitan permisos de Bluetooth",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Intentar nuevamente")
                                }
                            }
                        }

                        is PdfDialogUiState.Printing -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Imprimiendo documento...",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        is PdfDialogUiState.PrintComplete -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "¡Impresión completada!",
                                    color = Color.Green
                                )
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Cerrar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}