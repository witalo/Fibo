package com.example.fibo.reports

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.utils.PdfDialogUiState
import com.example.fibo.utils.showToast
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // Check Bluetooth state on composition
    var isBluetoothActive by remember { mutableStateOf(false) }
    // Estado para mantener la referencia al archivo PDF generado
    var pdfFile by remember { mutableStateOf<File?>(null) }
    // Scope para gestionar corrutinas
    val coroutineScope = rememberCoroutineScope()
    var bluetoothCheckJob by remember { mutableStateOf<Job?>(null) }

    // Flag para controlar si se está cerrando el diálogo
    var isDialogClosing by remember { mutableStateOf(false) }
    // Flag para controlar la inicialización única***
    var hasInitialized by remember { mutableStateOf(false) }
    // Efecto para verificar Bluetooth de forma segura***
    LaunchedEffect(operationId, isVisible) {
        if (isVisible && !hasInitialized) {
            try {
                Log.d("PdfViewerDialog", "Inicializando diálogo para operación $operationId")
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                isBluetoothActive = bluetoothAdapter?.isEnabled == true

                // Cargar datos iniciales solo una vez
                viewModel.fetchOperationById(operationId)
                hasInitialized = true

                // Monitoreo de Bluetooth solo si es necesario
                bluetoothCheckJob = launch {
                    snapshotFlow {
                        try {
                            bluetoothAdapter?.isEnabled == true
                        } catch (e: Exception) {
                            Log.e("BluetoothCheck", "Error verificando estado", e)
                            false
                        }
                    }
                        .distinctUntilChanged()
                        .collect { enabled ->
                            isBluetoothActive = enabled
                        }
                }
            } catch (e: Exception) {
                Log.e("Bluetooth", "Error inicializando estado Bluetooth", e)
                isBluetoothActive = false
            }
        } else if (!isVisible) {
            // Reset cuando el diálogo se cierra
            hasInitialized = false
        }
    }
//    LaunchedEffect(Unit) {
//        try {
//            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//            isBluetoothActive = bluetoothAdapter?.isEnabled == true
//
//            // Cargar datos iniciales solo una vez
//            viewModel.fetchOperationById(operationId)
//
//            // Usar snapshotFlow para observar cambios en el estado Bluetooth de manera eficiente
//            bluetoothCheckJob = launch {
//                snapshotFlow {
//                    try {
//                        bluetoothAdapter?.isEnabled == true
//                    } catch (e: Exception) {
//                        Log.e("BluetoothCheck", "Error verificando estado", e)
//                        false
//                    }
//                }
//                    .distinctUntilChanged()
//                    .collect { enabled ->
//                        isBluetoothActive = enabled
//                    }
//            }
//        } catch (e: Exception) {
//            Log.e("Bluetooth", "Error inicializando estado Bluetooth", e)
//            isBluetoothActive = false
//        }
//    }
// Función segura para cerrar el diálogo - Asegurarse de que hasInitialized se resetea
fun safelyDismissDialog() {
    if (isDialogClosing) return // Evitar múltiples cierres

    isDialogClosing = true

    coroutineScope.launch {
        try {
            // Cancelar jobs y limpiar recursos
            bluetoothCheckJob?.cancel()
            bluetoothCheckJob = null

            // Resetear ViewModel
            viewModel.resetState()

            // Pequeña pausa para permitir que finalicen las operaciones
            delay(100)

            // Resetear el flag de inicialización
            hasInitialized = false

            // Liberar referencia del archivo
            pdfFile = null

            // Finalmente llamar al callback de cierre
            onDismiss()
        } catch (e: Exception) {
            Log.e("PdfViewerDialog", "Error al cerrar diálogo", e)
            // Asegurar que se cierre incluso con error
            onDismiss()
        } finally {
            isDialogClosing = false
        }
    }
}
    // Función segura para cerrar el diálogo
//    fun safelyDismissDialog() {
//        if (isDialogClosing) return // Evitar múltiples cierres
//
//        isDialogClosing = true
//
//        coroutineScope.launch {
//            try {
//                // Cancelar jobs y limpiar recursos
//                bluetoothCheckJob?.cancel()
//                bluetoothCheckJob = null
//
//                // Resetear ViewModel
//                viewModel.resetState()
//
//                // Pequeña pausa para permitir que finalicen las operaciones
//                delay(100)
//
//                // Liberar referencia del archivo
//                pdfFile = null
//
//                // Finalmente llamar al callback de cierre
//                onDismiss()
//            } catch (e: Exception) {
//                Log.e("PdfViewerDialog", "Error al cerrar diálogo", e)
//                // Asegurar que se cierre incluso con error
//                onDismiss()
//            } finally {
//                isDialogClosing = false
//            }
//        }
//    }

    // Generación eficiente del PDF (solo si es necesario)
    LaunchedEffect(uiState) {
        if (uiState is PdfDialogUiState.Success && pdfFile == null) {
            try {
                val operation = (uiState as PdfDialogUiState.Success).operation
                pdfFile = withContext(Dispatchers.IO) {
                    try {
                        pdfGenerator.generatePdf(context, operation)
                    } catch (e: Exception) {
                        Log.e("PDF", "Error generando PDF", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("PDF", "Error en LaunchedEffect", e)
            }
        }
    }
    // Limpiar recursos al cerrar
    LaunchedEffect(isVisible) {
        if (!isVisible && !isDialogClosing) {
            safelyDismissDialog()
        }
    }
    // Efecto de limpieza para asegurar que se limpie todo al desmontar el composable
    DisposableEffect(Unit) {
        Log.d("PdfViewerDialog", "DisposableEffect iniciado para operación $operationId")
        onDispose {
            Log.d("PdfViewerDialog", "DisposableEffect limpieza ejecutada para operación $operationId")
            bluetoothCheckJob?.cancel()
            bluetoothCheckJob = null
            viewModel.resetState()
            pdfFile = null
            Log.d("PdfViewerDialog", "DisposableEffect cleanup executed")
        }
    }
    if (isVisible) {
        Dialog(
            onDismissRequest = {
                // Uso de nuestra función segura para cerrar
                safelyDismissDialog()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box {
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
                    // Barra superior con título y botón cerrar
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(8.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Comprobante",
//                            style = MaterialTheme.typography.titleLarge
//                        )
//
//                        IconButton(onClick = onDismiss) {
//                            Icon(
//                                imageVector = Icons.Default.Close,
//                                contentDescription = "Cerrar"
//                            )
//                        }
//                    }
//                    Divider()
                    // Contenido principal - Visualizador PDF y controles
                    Box(modifier = Modifier.weight(1f)) {
                        when (uiState) {
                            is PdfDialogUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
//                            is PdfDialogUiState.Success -> {
//                                // Mostrar PDF
//                                if (pdfFile != null) {
//                                    AndroidView(
//                                        factory = { ctx ->
//                                            PDFView(ctx, null).apply {
//                                                fromFile(pdfFile)
//                                                    .enableSwipe(true)
//                                                    .swipeHorizontal(false)
//                                                    .enableDoubletap(true)
//                                                    .defaultPage(0)
//                                                    .load()
//                                            }
//                                        },
//                                        modifier = Modifier.fillMaxSize()
//                                    )
//                                } else {
//                                    Box(
//                                        modifier = Modifier.fillMaxSize(),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                                            CircularProgressIndicator()
//                                            Spacer(modifier = Modifier.height(8.dp))
//                                            Text("Generando PDF...")
//                                        }
//                                    }
//                                }
//                            }
                            is PdfDialogUiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Error: ${(uiState as PdfDialogUiState.Error).message}")
                                }
                            }
                            else -> {
                                if (pdfFile != null) {
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
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Generando PDF...")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Controles de impresión al final
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        PrintControlsSection(
                            viewModel = viewModel,
                            context = context,
                            pdfFile = pdfFile,
                            isBluetoothActive = isBluetoothActive,
                            onPrintSuccess = { safelyDismissDialog() }
                        )
                    }
                }
            }
                // Botón flotante para compartir
                if (pdfFile != null) {
                    FloatingActionButton(
                        onClick = {
                            sharePdfViaWhatsApp(context, pdfFile!!)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(48.dp),
                        shape = CircleShape,
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50), // Verde claro
                                            Color(0xFF2E7D32)  // Verde oscuro
                                        ),
                                        radius = 100f
                                    )
                                )
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir por WhatsApp",
                                tint = Color.White,  // Icono blanco
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
//                    FloatingActionButton(
//                        onClick = {
//                            sharePdfViaWhatsApp(context, pdfFile!!)
//                        },
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .padding(16.dp),
//                        containerColor = MaterialTheme.colorScheme.primaryContainer, // Cambiado a containerColor
//                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Ajustado para coincidir
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Share,
//                            contentDescription = "Compartir por WhatsApp"
//                        )
//                    }
                }
            }
        }
    }
}
// Función para compartir el PDF por WhatsApp
private fun sharePdfViaWhatsApp(context: Context, pdfFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // Opcional: Especificar WhatsApp directamente
            // setPackage("com.whatsapp")
        }
//        context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF via"))
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF via"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error al compartir el PDF: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
        Log.e("SharePDF", "Error sharing PDF", e)
    }
}