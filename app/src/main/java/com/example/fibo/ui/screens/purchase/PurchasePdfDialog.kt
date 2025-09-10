package com.example.fibo.ui.screens.purchase

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.viewmodels.PurchaseViewModel
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Composable
fun PurchasePdfDialog(
    isVisible: Boolean,
    purchase: IOperation,
    onDismiss: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    if (!isVisible) return

    // Estados para el PDF y Bluetooth
    var pdfLoadingState by remember { mutableStateOf(PdfLoadingState.LOADING) }
    var bluetoothState by remember { mutableStateOf(BluetoothState.UNKNOWN) }
    var devicesList by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    // Estado para almacenar el archivo PDF local
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // URL del PDF - usando la ruta que proporcionaste
    val pdfUrl = remember { "https://ng.tuf4ctur4.net.pe/operations/purchase/${purchase.id}/" }

    // Contexto para operaciones de sistema
    val context = LocalContext.current

    // BluetoothAdapter
    val bluetoothAdapter: BluetoothAdapter? by remember {
        mutableStateOf(BluetoothAdapter.getDefaultAdapter())
    }

    // Verifica el estado de Bluetooth al inicio
    LaunchedEffect(bluetoothAdapter) {
        bluetoothState = if (bluetoothAdapter == null) {
            BluetoothState.NOT_SUPPORTED
        } else if (bluetoothAdapter!!.isEnabled) {
            BluetoothState.ENABLED
        } else {
            BluetoothState.DISABLED
        }
    }

    // Limpiar estados cuando se cierra el diálogo
    DisposableEffect(isVisible) {
        onDispose {
            if (!isVisible) {
                pdfFile = null
                devicesList = emptyList()
                selectedDevice = null
            }
        }
    }

    // Altura fija para el visor de PDF
    val pdfViewHeight = 500.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // 1. Header con título y botones
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PDF de Compra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        // Botón compartir
                        IconButton(
                            onClick = {
                                pdfFile?.let { file ->
                                    sharePdfViaWhatsApp(context, file)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Botón cerrar
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 2. Visualizador de PDF - ALTURA FIJA Y CONTROLADA
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

                            // Iniciar la carga del PDF
                            LaunchedEffect(pdfUrl) {
                                try {
                                    // Primero descargamos el PDF para obtener el archivo local
                                    downloadPdfToFile(context, pdfUrl) { file ->
                                        if (file != null) {
                                            pdfFile = file
                                            pdfLoadingState = PdfLoadingState.SUCCESS
                                        } else {
                                            pdfLoadingState = PdfLoadingState.ERROR
                                        }
                                    }
                                } catch (e: Exception) {
                                    pdfLoadingState = PdfLoadingState.ERROR
                                }
                            }
                        }
                        PdfLoadingState.SUCCESS -> {
                            // Solución: Contener el PDFView en un FrameLayout y controlarlo cuidadosamente
                            AndroidView(
                                factory = { ctx ->
                                    // Creamos primero un FrameLayout contenedor
                                    val frameLayout = FrameLayout(ctx)
                                    frameLayout.layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )

                                    // Creamos el PDFView dentro del FrameLayout
                                    val pdfView = com.github.barteksc.pdfviewer.PDFView(ctx, null)
                                    val pdfLayoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                    pdfView.layoutParams = pdfLayoutParams

                                    // Añadimos el PDFView al FrameLayout
                                    frameLayout.addView(pdfView)

                                    // Configuramos el PDFView desde el archivo local
                                    pdfFile?.let { file ->
                                        pdfView.fromFile(file)
                                            .enableSwipe(true)
                                            .enableDoubletap(true)
                                            .defaultPage(0)
                                            .swipeHorizontal(false)
                                            .onLoad {
                                                // PDF cargado completamente
                                            }
                                            .onError {
                                                pdfLoadingState = PdfLoadingState.ERROR
                                            }
                                            .load()
                                    }

                                    frameLayout  // Retornamos el FrameLayout
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
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Error al cargar el PDF",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No se pudo cargar el documento",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Footer con información de la compra
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Información de la Compra",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Documento: ${purchase.serial}-${purchase.correlative}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Tipo: ${purchase.documentTypeReadable}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Fecha: ${purchase.emitDate}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Total: S/. ${String.format("%.2f", purchase.totalToPay)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Función para descargar PDF y guardarlo localmente
fun downloadPdfToFile(context: Context, url: String, callback: (File?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val urlConnection = URL(url).openConnection() as HttpURLConnection
            urlConnection.connect()

            if (urlConnection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
                return@launch
            }

            val inputStream = urlConnection.inputStream
            // Crear un archivo temporal en el directorio de caché
            val tempFile = File(context.cacheDir, "PURCHASE_${System.currentTimeMillis()}.pdf")

            withContext(Dispatchers.IO) {
                // Asegurarse de que el archivo se sobreescribe si ya existe
                if (tempFile.exists()) {
                    tempFile.delete()
                }

                val outputStream = FileOutputStream(tempFile)
                val buffer = ByteArray(4 * 1024) // Buffer de 4KB
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()
            }

            withContext(Dispatchers.Main) {
                callback(tempFile)
            }
        } catch (e: Exception) {
            Log.e("PDF_DOWNLOAD", "Error downloading PDF: ${e.message}", e)
            withContext(Dispatchers.Main) {
                callback(null)
            }
        }
    }
}

// Función para compartir PDF
fun sharePdfViaWhatsApp(context: Context, pdfFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )

        // Crear intent específico para WhatsApp que abra directamente la pantalla de contactos
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }

        val whatsappBusinessIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp.w4b")
        }

        // Intentar primero con WhatsApp Business, luego WhatsApp normal
        try {
            context.startActivity(whatsappBusinessIntent)
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(whatsappIntent)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
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

// Estados para el PDF
enum class PdfLoadingState {
    LOADING, SUCCESS, ERROR
}

// Estados para Bluetooth
enum class BluetoothState {
    UNKNOWN, ENABLED, DISABLED, NOT_SUPPORTED
}
