package com.example.fibo.ui.screens.quotation

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.viewmodels.QuotationViewModel
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

@Composable
fun QuotationPdfDialog(
    isVisible: Boolean,
    quotation: IOperation,
    onDismiss: () -> Unit,
    viewModel: QuotationViewModel = hiltViewModel()
) {
    if (!isVisible) return

    // Estados para el PDF y Bluetooth
    var pdfLoadingState by remember { mutableStateOf(PdfLoadingState.LOADING) }
    var bluetoothState by remember { mutableStateOf(BluetoothState.UNKNOWN) }
    var devicesList by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    // Estado para almacenar el archivo PDF local
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // URL del PDF
    val pdfUrl = remember { "https://ng.tuf4ctur4.net.pe/operations/quotation/${quotation.id}/" }

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
            selectedDevice = null
            devicesList = emptyList()
            bluetoothState = if (bluetoothAdapter?.isEnabled == true) {
                BluetoothState.ENABLED
            } else {
                BluetoothState.DISABLED
            }
            // Eliminar el archivo temporal si existe
            pdfFile?.delete()
        }
    }

    // Receptor de Broadcast para eventos Bluetooth
    val bluetoothReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        when (state) {
                            BluetoothAdapter.STATE_ON -> bluetoothState = BluetoothState.ENABLED
                            BluetoothAdapter.STATE_OFF -> bluetoothState = BluetoothState.DISABLED
                        }
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        if (device != null && !devicesList.contains(device)) {
                            devicesList = devicesList + device
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        bluetoothState = BluetoothState.ENABLED
                    }
                }
            }
        }
    }

    // Registrar el receptor para eventos Bluetooth
    DisposableEffect(Unit) {
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, intentFilter)

        onDispose {
            try {
                context.unregisterReceiver(bluetoothReceiver)
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
            } catch (e: Exception) {
                // Ignorar excepciones al desregistrar
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
                val tempFile = File(context.cacheDir, "COTIZACION_${quotation.id}.pdf")

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

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Opcional: Especificar WhatsApp directamente
                setPackage("com.whatsapp")
            }

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

    // Diálogo principal
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
            // Contenedor principal usando BoxWithConstraints para controlar el layout
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val maxHeight = this.maxHeight
                val headerHeight = 50.dp
                val controlsHeight = 280.dp
                val pdfViewHeight = maxHeight - headerHeight - controlsHeight

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Header con título y botón de cierre - ALTURA FIJA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF8C00),  // Naranja oscuro
                                        Color(0xFFFFAB40)   // Naranja claro
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "COTIZACIÓN ${quotation.serial}-${quotation.correlative}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        IconButton(
                            onClick = {
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
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .align(Alignment.CenterEnd)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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

                                // Botón flotante para compartir
                                pdfFile?.let { file ->
                                    FloatingActionButton(
                                        onClick = { sharePdfViaWhatsApp(context, file) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp),
                                        containerColor = Color.Transparent,
                                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                                        interactionSource = remember { MutableInteractionSource() } // Esto desactiva el ripple
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_whasap),
                                            contentDescription = "Compartir en WhatsApp",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                            PdfLoadingState.ERROR -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Error al cargar el PDF",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { pdfLoadingState = PdfLoadingState.LOADING },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Text("Reintentar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 3. Sección de controles Bluetooth - ALTURA FIJA
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(controlsHeight)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shadowElevation = 4.dp,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp)
                        ) {
                            // Botón para Bluetooth con gradiente naranja
                            Button(
                                onClick = {
                                    when (bluetoothState) {
                                        BluetoothState.DISABLED -> {
                                            // Solicitar activación de Bluetooth
                                            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    if (ActivityCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.BLUETOOTH_CONNECT
                                                        ) == PackageManager.PERMISSION_GRANTED) {
                                                        (context as? Activity)?.startActivityForResult(enableBtIntent, 1)
                                                    }
                                                } else {
                                                    (context as? Activity)?.startActivityForResult(enableBtIntent, 1)
                                                }
                                            } catch (e: Exception) {
                                                // Manejar posibles excepciones
                                                Toast.makeText(
                                                    context,
                                                    "No se pudo activar Bluetooth",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        BluetoothState.ENABLED -> {
                                            // Buscar dispositivos
                                            devicesList = emptyList()
                                            bluetoothState = BluetoothState.DISCOVERING

                                            // Verificar permisos según versión de Android
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_SCAN
                                                    ) == PackageManager.PERMISSION_GRANTED &&
                                                    ActivityCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_CONNECT
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    bluetoothAdapter?.startDiscovery()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Se requieren permisos de Bluetooth",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    bluetoothState = BluetoothState.ENABLED
                                                }
                                            } else {
                                                if (ActivityCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.ACCESS_FINE_LOCATION
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    bluetoothAdapter?.startDiscovery()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Se requiere permiso de ubicación para buscar dispositivos",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    bluetoothState = BluetoothState.ENABLED
                                                }
                                            }
                                        }
                                        else -> {
                                            // Estado no conocido o no soportado
                                            Toast.makeText(
                                                context,
                                                "Bluetooth no disponible",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFFF8C00),  // Naranja oscuro
                                                    Color(0xFFFFAB40)   // Naranja claro
                                                )
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (bluetoothState) {
                                                BluetoothState.DISABLED -> "Activar Bluetooth"
                                                BluetoothState.ENABLED -> "Buscar Dispositivos"
                                                BluetoothState.DISCOVERING -> "Buscando..."
                                                else -> "Bluetooth No Disponible"
                                            },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Lista de dispositivos - ALTURA LIMITADA
                            if (devicesList.isNotEmpty()) {
                                Text(
                                    text = "Dispositivos Disponibles:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                                )

                                // Limitamos la altura de la lista para que no ocupe todo el espacio
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                ) {
                                    items(devicesList) { device ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedDevice = device
                                                }
                                                .padding(vertical = 0.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedDevice == device,
                                                onClick = { selectedDevice = device }
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_CONNECT
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    device.name ?: "Dispositivo Desconocido"
                                                } else {
                                                    "Dispositivo Desconocido"
                                                }
                                            } else {
                                                device.name ?: "Dispositivo Desconocido"
                                            }

                                            Text(
                                                text = deviceName,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            } else if (bluetoothState == BluetoothState.DISCOVERING) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFF8C00)  // Color naranja para el indicador
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Botón de impresión con gradiente naranja
                            Button(
                                onClick = {
                                    // Imprimir el texto básico del documento
                                    selectedDevice?.let { device ->
                                        try {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                if (ActivityCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.BLUETOOTH_CONNECT
                                                    ) == PackageManager.PERMISSION_GRANTED) {
                                                    printToBluetoothDevice(context, device, quotation)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Se requiere permiso para conectar",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                printToBluetoothDevice(context, device, quotation)
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "Error al imprimir: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                enabled = selectedDevice != null,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent
                                ),
                                contentPadding = PaddingValues(vertical = 6.dp, horizontal = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = if (selectedDevice != null) {
                                                    listOf(
                                                        Color(0xFFFF8C00),  // Naranja oscuro
                                                        Color(0xFFFFAB40)   // Naranja claro
                                                    )
                                                } else {
                                                    listOf(
                                                        Color(0xFFAAAAAA),  // Gris cuando está deshabilitado
                                                        Color(0xFFCCCCCC)
                                                    )
                                                }
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Imprimir",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
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

// Función para imprimir a dispositivo Bluetooth
private fun printToBluetoothDevice(context: Context, device: BluetoothDevice, quotation: IOperation) {
    try {
        // Crear un UUID para el servicio de impresión SPP
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Obtener un socket Bluetooth
        val socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED) {
                device.createRfcommSocketToServiceRecord(uuid)
            } else {
                throw SecurityException("Se requiere permiso BLUETOOTH_CONNECT")
            }
        } else {
            device.createRfcommSocketToServiceRecord(uuid)
        }

        // Cancelar descubrimiento antes de conectar
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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

        // Conectar al dispositivo
        socket.connect()

        // Obtener flujo de salida
        val outputStream = socket.outputStream

        // Crear los datos para imprimir
        val printData = """
            |----------------------------
            |COTIZACION
            |----------------------------
            |ID: ${quotation.id}
            |SERIE: ${quotation.serial}
            |CORRELATIVO: ${quotation.correlative}
            |----------------------------
            |
            |
        """.trimIndent()

        // Enviar datos al dispositivo
        outputStream.write(printData.toByteArray())

        // Cerrar conexión
        outputStream.close()
        socket.close()

        // Mostrar mensaje de éxito
        Toast.makeText(
            context,
            "Impresión enviada correctamente",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        // Mostrar mensaje de error
        Toast.makeText(
            context,
            "Error en la impresión: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

// Enumeración para estados de carga del PDF
enum class PdfLoadingState {
    LOADING, SUCCESS, ERROR
}

// Enumeración para estados de Bluetooth
enum class BluetoothState {
    UNKNOWN, NOT_SUPPORTED, DISABLED, ENABLED, DISCOVERING
}