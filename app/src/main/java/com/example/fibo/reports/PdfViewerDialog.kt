package com.example.fibo.reports

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
    // Efecto para verificar Bluetooth de forma segura
    LaunchedEffect(Unit) {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            isBluetoothActive = bluetoothAdapter?.isEnabled == true

            // Cargar datos iniciales solo una vez
            viewModel.fetchOperationById(operationId)

            // Usar snapshotFlow para observar cambios en el estado Bluetooth de manera eficiente
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
    }
    // Función segura para cerrar el diálogo
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
        onDispose {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comprobante",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar"
                            )
                        }
                    }
                    Divider()
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
        }
    }
}