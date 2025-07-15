package com.example.fibo.ui.screens.guide

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.reports.PrintControlsSection
import com.example.fibo.reports.GuidesPrintControlsSection
import com.example.fibo.reports.PdfDialogViewModel
import com.example.fibo.model.IGuideData
import com.example.fibo.utils.Constants
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

enum class PdfLoadingState {
    LOADING, SUCCESS, ERROR
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GuidePdfPreviewDialog(
    isVisible: Boolean,
    guideId: Int,
    onDismiss: () -> Unit,
    viewModel: PdfDialogViewModel = hiltViewModel()
) {
    if (!isVisible) return

    val context = LocalContext.current
    
    // Estados para el PDF y Bluetooth - usar isVisible como clave principal
    var pdfLoadingState by remember(isVisible, guideId) { mutableStateOf(PdfLoadingState.LOADING) }
    var isBluetoothActive by remember { mutableStateOf(false) }
    var pdfFile by remember(isVisible, guideId) { mutableStateOf<File?>(null) }
    var guideData by remember(isVisible, guideId) { mutableStateOf<IGuideData?>(null) }
    var currentGuideId by remember { mutableStateOf(0) }
    
    // URL del PDF para previsualización
    val pdfUrl = remember(guideId) { Constants.getPreviewPdfUrl(guideId) }
    
    // BluetoothAdapter
    val bluetoothAdapter: BluetoothAdapter? by remember {
        mutableStateOf(BluetoothAdapter.getDefaultAdapter())
    }

    // Verificar estado de Bluetooth
    LaunchedEffect(bluetoothAdapter) {
        isBluetoothActive = bluetoothAdapter?.isEnabled == true
    }

    // Limpiar estado cuando se cierra el diálogo
    DisposableEffect(isVisible) {
        onDispose {
            if (!isVisible) {
                Log.d("GuidePdfPreview", "Limpiando estado del diálogo")
                // Resetear el estado del ViewModel para evitar conflictos
                viewModel.resetState()
            }
        }
    }

    // Cargar datos de la guía solo cuando cambia el guideId y es diferente al actual
    LaunchedEffect(guideId, isVisible) {
        if (isVisible && guideId > 0 && guideId != currentGuideId) {
            Log.d("GuidePdfPreview", "Cargando datos para guía ID: $guideId")
            currentGuideId = guideId
            try {
                val repository = viewModel.operationRepository
                val data = repository.getGuideById(guideId)
                guideData = data
                Log.d("GuidePdfPreview", "Datos cargados exitosamente para guía ID: $guideId")
            } catch (e: Exception) {
                Log.e("GuidePdfPreview", "Error cargando datos de guía: ${e.message}")
            }
        }
    }

    // Función para descargar PDF desde URL
    fun downloadPdfToFile(context: Context, url: String, callback: (File?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("GuidePdfPreview", "Descargando PDF desde: $url")
                val urlConnection = URL(url).openConnection() as HttpURLConnection
                urlConnection.connectTimeout = 10000 // 10 segundos timeout
                urlConnection.readTimeout = 30000 // 30 segundos timeout
                urlConnection.connect()

                if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("GuidePdfPreview", "Error HTTP: ${urlConnection.responseCode}")
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                    return@launch
                }

                val inputStream = BufferedInputStream(urlConnection.inputStream)
                val tempFile = File(context.cacheDir, "guide_${guideId}_${System.currentTimeMillis()}.pdf")

                withContext(Dispatchers.IO) {
                    // Limpiar archivos antiguos del mismo guideId
                    context.cacheDir.listFiles()?.filter { 
                        it.name.startsWith("guide_${guideId}_") && it.name.endsWith(".pdf")
                    }?.forEach { it.delete() }

                    val outputStream = FileOutputStream(tempFile)
                    val buffer = ByteArray(4 * 1024)
                    var bytesRead: Int

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.close()
                    inputStream.close()
                    urlConnection.disconnect()
                }

                // Verificar que el archivo se creó correctamente
                if (tempFile.exists() && tempFile.length() > 0) {
                    withContext(Dispatchers.Main) {
                        Log.d("GuidePdfPreview", "PDF descargado exitosamente: ${tempFile.absolutePath}, tamaño: ${tempFile.length()} bytes")
                        callback(tempFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("GuidePdfPreview", "El archivo PDF está vacío o no se creó correctamente")
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                Log.e("GuidePdfPreview", "Error descargando PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    // Función para compartir PDF por WhatsApp
    fun sharePdfViaWhatsApp(context: Context, pdfFile: File) {
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
            }

            // Intentar con WhatsApp Business primero, luego WhatsApp normal
            try {
                shareIntent.setPackage("com.whatsapp.w4b")
                context.startActivity(shareIntent)
            } catch (e: ActivityNotFoundException) {
                try {
                    shareIntent.setPackage("com.whatsapp")
                    context.startActivity(shareIntent)
                } catch (e2: ActivityNotFoundException) {
                    // Si no hay WhatsApp, mostrar selector general
                    shareIntent.setPackage(null)
                    context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
                }
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

    Dialog(
        onDismissRequest = {
            // Verificar permisos antes de cancelar el descubrimiento
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } else {
                bluetoothAdapter?.cancelDiscovery()
            }
            // Resetear el estado del ViewModel antes de cerrar
            viewModel.resetState()
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val maxHeight = this.maxHeight
                val headerHeight = 60.dp
                val minControlsHeight = 200.dp
                val maxControlsHeight = 320.dp
                val pdfViewHeight = maxHeight - headerHeight - minControlsHeight

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Header con título y botón cerrar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Guía de Remisión #$guideId",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            IconButton(onClick = {
                                // Resetear el estado del ViewModel antes de cerrar
                                viewModel.resetState()
                                onDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // 2. Visualizador de PDF
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pdfViewHeight)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        when (pdfLoadingState) {
                            PdfLoadingState.LOADING -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(text = "Cargando PDF...")
                                    }
                                }

                                // Iniciar la descarga del PDF solo si no se ha descargado ya
                                LaunchedEffect(pdfUrl, pdfLoadingState) {
                                    if (pdfLoadingState == PdfLoadingState.LOADING && pdfFile == null) {
                                        Log.d("GuidePdfPreview", "Iniciando descarga de PDF desde: $pdfUrl")
                                        downloadPdfToFile(context, pdfUrl) { file ->
                                            if (file != null) {
                                                Log.d("GuidePdfPreview", "PDF descargado exitosamente: ${file.absolutePath}")
                                                pdfFile = file
                                                pdfLoadingState = PdfLoadingState.SUCCESS
                                            } else {
                                                Log.e("GuidePdfPreview", "Error: PDF file es null")
                                                pdfLoadingState = PdfLoadingState.ERROR
                                            }
                                        }
                                    }
                                }
                            }
                            PdfLoadingState.SUCCESS -> {
                                AndroidView(
                                    factory = { ctx ->
                                        val frameLayout = FrameLayout(ctx)
                                        frameLayout.layoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )

                                        val pdfView = PDFView(ctx, null)
                                        val pdfLayoutParams = FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                        )
                                        pdfView.layoutParams = pdfLayoutParams
                                        frameLayout.addView(pdfView)

                                        pdfFile?.let { file ->
                                            pdfView.fromFile(file)
                                                .enableSwipe(true)
                                                .enableDoubletap(true)
                                                .defaultPage(0)
                                                .swipeHorizontal(false)
                                                .onLoad {
                                                    Log.d("GuidePdfPreview", "PDF cargado completamente")
                                                }
                                                .onError { error ->
                                                    Log.e("GuidePdfPreview", "Error cargando PDF: $error")
                                                    pdfLoadingState = PdfLoadingState.ERROR
                                                }
                                                .load()
                                        }

                                        frameLayout
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            PdfLoadingState.ERROR -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Error al cargar el PDF",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Verifique su conexión a internet",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                pdfLoadingState = PdfLoadingState.LOADING
                                                downloadPdfToFile(context, pdfUrl) { file ->
                                                    if (file != null) {
                                                        pdfFile = file
                                                        pdfLoadingState = PdfLoadingState.SUCCESS
                                                    } else {
                                                        pdfLoadingState = PdfLoadingState.ERROR
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Reintentar")
                                        }
                                    }
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
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Controles de impresión
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = minControlsHeight, max = maxControlsHeight)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            // Log para debug
                            LaunchedEffect(pdfFile) {
                                Log.d("GuidePdfPreview", "pdfFile estado en PrintControlsSection: ${pdfFile?.absolutePath ?: "NULL"}")
                            }
                            
                                        // Solo mostrar GuidesPrintControlsSection cuando pdfFile y guideData estén disponibles
            if (pdfFile != null && guideData != null) {
                Log.d("GuidePdfPreview", "Pasando pdfFile y guideData a GuidesPrintControlsSection")
                GuidesPrintControlsSection(
                    viewModel = viewModel,
                    context = context,
                    pdfFile = pdfFile,
                    guideData = guideData,
                    isBluetoothActive = isBluetoothActive,
                    onPrintSuccess = { 
                        // No cerrar automáticamente el diálogo
                        // El usuario puede cerrar manualmente si desea
                        Log.d("GuidePdfPreview", "Impresión completada exitosamente")
                    }
                )
                            } else {
                                // Mostrar mensaje mientras se carga el PDF o los datos de la guía
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = when {
                                                pdfFile == null -> "Preparando archivo para impresión..."
                                                guideData == null -> "Cargando datos de la guía..."
                                                else -> "Preparando para impresión..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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