package com.example.fibo.reports

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.contentcapture.ContentCaptureManager.Companion.isEnabled
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.model.IOperation
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.PdfDialogUiState
import com.itextpdf.io.exceptions.IOException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CancellationException
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.io.ByteArrayOutputStream
@HiltViewModel
class PdfDialogViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<PdfDialogUiState>(PdfDialogUiState.Initial)
    val uiState: StateFlow<PdfDialogUiState> = _uiState.asStateFlow()

    private val _availablePrinters = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availablePrinters: StateFlow<List<BluetoothDevice>> = _availablePrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<BluetoothDevice?>(null)
    val selectedPrinter: StateFlow<BluetoothDevice?> = _selectedPrinter.asStateFlow()

    private var currentOperation: IOperation? = null
    private var scanPrintersJob: Job? = null
    private var printOperationJob: Job? = null

    // En PdfDialogViewModel
    private var currentOperationId: Int? = null

    override fun onCleared() {
        super.onCleared()
        scanPrintersJob?.cancel()
        printOperationJob?.cancel()
    }
    fun fetchOperationById(operationId: Int) {
        // Evitar múltiples llamadas para la misma operación
        if (currentOperationId == operationId &&
            (_uiState.value is PdfDialogUiState.Success || _uiState.value is PdfDialogUiState.Loading)) {
            Log.d("PdfDialogViewModel", "Operación $operationId ya está cargada o cargando, evitando duplicados")
            return
        }

        viewModelScope.launch {
            _uiState.value = PdfDialogUiState.Loading
            currentOperationId = operationId
            Log.d("PdfDialogViewModel", "Iniciando carga de operación $operationId")

            try {
                val operation = operationRepository.getOperationById(operationId)
                currentOperation = operation
                _uiState.value = PdfDialogUiState.Success(operation)
                Log.d("PdfDialogViewModel", "Operación $operationId cargada exitosamente")
            } catch (e: ApolloException) {
                _uiState.value = PdfDialogUiState.Error("Error de conexión: ${e.message}")
                Log.e("PdfDialogViewModel", "Error de conexión cargando operación $operationId", e)
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error inesperado: ${e.message}")
                Log.e("PdfDialogViewModel", "Error cargando operación $operationId", e)
            }
        }
    }
//    fun fetchOperationById(operationId: Int) {
//        viewModelScope.launch {
//            _uiState.value = PdfDialogUiState.Loading
//            try {
//                val operation = operationRepository.getOperationById(operationId)
//                currentOperation = operation
//                _uiState.value = PdfDialogUiState.Success(operation)
//            } catch (e: ApolloException) {
//                _uiState.value = PdfDialogUiState.Error("Error de conexión: ${e.message}")
//            } catch (e: Exception) {
//                _uiState.value = PdfDialogUiState.Error("Error inesperado: ${e.message}")
//            }
//        }
//    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun scanForPrinters(context: Context) {
        // Cancelar trabajo anterior si existe
        scanPrintersJob?.cancel()

        _uiState.value = PdfDialogUiState.ScanningPrinters

        scanPrintersJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("PrintViewModel", "Iniciando escaneo de impresoras...")

                // Verificar permisos de Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val requiredPermissions = arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )

                    val missingPermissions = requiredPermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missingPermissions.isNotEmpty()) {
                        updateUiState(PdfDialogUiState.Error(
                            "Se requieren permisos de Bluetooth para continuar"
                        ))
                        return@launch
                    }
                }

                // Obtener adaptador Bluetooth con manejo de errores
                val bluetoothAdapter = try {
                    BluetoothAdapter.getDefaultAdapter()
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error obteniendo adapter", e)
                    null
                }

                if (bluetoothAdapter == null) {
                    updateUiState(PdfDialogUiState.Error("Bluetooth no soportado"))
                    return@launch
                }

                if (!bluetoothAdapter.isEnabled) {
                    updateUiState(PdfDialogUiState.BluetoothDisabled)
                    return@launch
                }

                // Verificar permisos adicionales según versión Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val missingPermissions = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                        PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) !=
                        PackageManager.PERMISSION_GRANTED) {
                        missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                    }

                    if (missingPermissions.isNotEmpty()) {
                        updateUiState(PdfDialogUiState.Error(
                            "Se requieren permisos: ${missingPermissions.joinToString()}"
                        ))
                        return@launch
                    }
                }

                // Verificar permisos para versiones anteriores
                if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                        updateUiState(PdfDialogUiState.Error(
                            "Se requiere permiso de ubicación para escanear dispositivos Bluetooth"
                        ))
                        return@launch
                    }
                }

                // Obtener dispositivos con timeout
                val pairedDevices = try {
                    withTimeout(5000) {
                        bluetoothAdapter.bondedDevices ?: emptySet()
                    }
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error obteniendo dispositivos", e)
                    updateUiState(PdfDialogUiState.Error("Error: ${e.message}"))
                    return@launch
                }

                // Filtrar y ordenar impresoras
                val printers = pairedDevices.sortedByDescending { device ->
                    val name = device.name?.lowercase() ?: ""
                    when {
                        name.contains("adv") && name.contains("7010") -> 100
                        name.contains("adv") || name.contains("advance") -> 50
                        name.contains("print") || name.contains("impres") -> 25
                        else -> 0
                    }
                }

                // Actualizar la UI en el hilo principal
                withContext(Dispatchers.Main) {
                    _availablePrinters.value = printers

                    val currentState = _uiState.value
                    if (currentState is PdfDialogUiState.Success) {
                        _uiState.value = currentState.copy(printers = printers)
                    } else {
                        _uiState.value = PdfDialogUiState.PrintersFound(printers)
                    }

                    // Autoseleccionar la primera impresora preferida
                    printers.firstOrNull {
                        it.name?.contains("ADV", ignoreCase = true) == true ||
                                it.name?.contains("Advance", ignoreCase = true) == true
                    }?.let { advancePrinter ->
                        _selectedPrinter.value = advancePrinter
                    }
                }

            } catch (e: Exception) {
                Log.e("PrintViewModel", "Error al escanear impresoras", e)
                updateUiState(PdfDialogUiState.Error("Error: ${e.message}"))
            }
        }
    }
    // Helper function for current date
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun selectPrinter(device: BluetoothDevice) {
        _selectedPrinter.value = device
    }

    // Helper para fecha actual
    private fun getCurrentDateTime(): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(Date())
    }

    // Helper para formatear moneda
    private fun formatCurrency(value: Double?): String {
        return String.format("%.2f", value ?: 0.0)
    }
    // Métodos auxiliares para formatear texto y fechas
    private fun limitText(text: String, maxLength: Int): String {
        return if (text.length <= maxLength) {
            String.format("%-${maxLength}s", text)
        } else {
            String.format("%-${maxLength}s", text.substring(0, maxLength - 3) + "...")
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: return dateStr)
        } catch (e: Exception) {
            dateStr
        }
    }
    fun resetState() {
        viewModelScope.launch {
            try {
                // Cancelar operaciones en curso
                scanPrintersJob?.cancel()
                scanPrintersJob = null

                printOperationJob?.cancel()
                printOperationJob = null

                // Liberar recursos
                currentOperation = null
                // Resetear ID de operación
                currentOperationId = null

                // Restablecer estados
                _uiState.value = PdfDialogUiState.Initial
                _selectedPrinter.value = null
                _availablePrinters.value = emptyList()
                Log.d("PdfDialogViewModel", "Estado reseteado correctamente")
            } catch (e: Exception) {
                Log.e("PdfDialogViewModel", "Error al resetear estado", e)
            }
        }
    }
    // Mejora el método resetState en el PdfDialogViewModel
//    fun resetState() {
//        viewModelScope.launch {
//            try {
//                // Cancelar operaciones en curso
//                scanPrintersJob?.cancel()
//                scanPrintersJob = null
//
//                printOperationJob?.cancel()
//                printOperationJob = null
//
//                // Liberar recursos
//                currentOperation = null
//
//                // Restablecer estados
//                _uiState.value = PdfDialogUiState.Initial
//                _selectedPrinter.value = null
//                _availablePrinters.value = emptyList()
//
//                Log.d("PdfDialogViewModel", "Estado reseteado correctamente")
//            } catch (e: Exception) {
//                Log.e("PdfDialogViewModel", "Error al resetear estado", e)
//            }
//        }
//    }


    fun printOperation(context: Context, pdfFile: File) {
        _uiState.value = PdfDialogUiState.Printing
        // Cancelar job anterior si existe
        printOperationJob?.cancel()

        printOperationJob = viewModelScope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            var outputStream: OutputStream? = null

            try {
                // Validaciones iniciales
                val printer = _selectedPrinter.value ?: run {
                    updateUiState(PdfDialogUiState.Error("No hay impresora seleccionada"))
                    return@launch
                }

                val operation = currentOperation ?: run {
                    updateUiState(PdfDialogUiState.Error("No hay operación para imprimir"))
                    return@launch
                }

                // Verificación de permisos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !hasBluetoothConnectPermission(context)) {
                    updateUiState(PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT requerido"))
                    return@launch
                }

                // Conexión con timeout
                socket = connectWithTimeout(context, printer) ?: return@launch

                // Configurar output stream
                outputStream = socket.outputStream

                // Generar y enviar contenido
                sendPrintContent(outputStream, operation)

                // Finalización exitosa
                updateUiState(PdfDialogUiState.PrintComplete)
            }  catch (e: CancellationException) {
                // Manejar cancelación explícitamente
                Log.d("PrintViewModel", "Operación de impresión cancelada normalmente")
                closeResources(socket, outputStream)
                updateUiState(PdfDialogUiState.Initial)
            } catch (e: Exception) {
                Log.e("PrintViewModel", "Error en impresión", e)
                updateUiState(PdfDialogUiState.Error("Error al imprimir: ${e.message ?: "Desconocido"}"))
            } finally {
                closeResources(socket, outputStream)
            }
        }
    }

    // Funciones auxiliares
    private suspend fun updateUiState(state: PdfDialogUiState) {
        withContext(Dispatchers.Main) {
            _uiState.value = state
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hasBluetoothConnectPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectWithTimeout(context: Context, printer: BluetoothDevice): BluetoothSocket? {
        return try {
            // Verificar permiso BLUETOOTH_CONNECT para Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    updateUiState(PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT requerido"))
                    return null
                }
            }

            withTimeout(15_000) { // 15 segundos para conexión
                try {
                    printer.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    ).apply {
                        connect()
                    }
                } catch (e: SecurityException) {
                    // Manejar específicamente la excepción de seguridad
                    throw IOException("Permiso denegado: ${e.message}")
                }
            }
        } catch (e: TimeoutCancellationException) {
            updateUiState(PdfDialogUiState.Error("Tiempo de conexión agotado"))
            null
        } catch (e: IOException) {
            updateUiState(PdfDialogUiState.Error("Error conectando: ${e.message}"))
            null
        } catch (e: SecurityException) {
            updateUiState(PdfDialogUiState.Error("Permiso denegado: ${e.message}"))
            null
        }
    }

    private fun sendPrintContent(outputStream: OutputStream, operation: IOperation) {
        // Usar buffer para escritura más eficiente
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        val numberFormat = DecimalFormat("#,##0.00")

        try {
            // Encabezado - Logo/Nombre de la empresa
            outputStream.write(PrinterCommands.INIT)
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${pdfGenerator.company.logo}\n")
            writer.write("${pdfGenerator.company.businessName}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)
            // 1. Imprimir logo (versión simplificada)
            printLogo(outputStream, pdfGenerator.company.logo)
            // Datos de la empresa
            writer.write("RUC: ${pdfGenerator.company.doc}\n")
            writer.write("DIRECCION: ${pdfGenerator.subsidiary.address}\n\n")

            // Tipo de documento
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${operation.documentTypeReadable}\n")
            writer.write("${operation.serial}-${operation.correlative}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            writer.write("--------------------------------\n")

            // Sección de cliente - Todo alineado a la izquierda
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("DATOS DEL CLIENTE\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)
            writer.write("${formatDocumentType(operation.client.documentType)}: ${operation.client.documentNumber ?: ""}\n")
            writer.write("DENOMINACION: ${operation.client.names ?: ""}\n")

            if (!operation.client.phone.isNullOrEmpty()) {
                writer.write("TELEFONO: ${operation.client.phone}\n")
            }

            writer.write("DIRECCION: ${operation.client.address ?: ""}\n")
            writer.write("FECHA: ${"${operation.emitDate} ${operation.emitTime}".formatToDisplayDateTime()}\n")

            writer.write("--------------------------------\n")

            // Encabezado de productos
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("DETALLE DE PRODUCTOS\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Encabezado de columnas para la segunda línea
            writer.write("CANT   P.UNIT   DSCTO   IMPORTE\n")
            writer.write("--------------------------------\n")

            // Añadir productos - Formato ajustado según requerimiento
            operation.operationDetailSet.forEach { detail ->
                // Primera línea: Descripción completa del producto
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
//                writer.write("${detail.tariff.productName}\n")
                writer.write(
                    if (detail.description != null && detail.description.isNotBlank()) {
                        "${detail.tariff.productName} (${detail.description})\n"
                    } else {
                        "${detail.tariff.productName}\n"
                    }
                )

                // Segunda línea: Datos numéricos alineados a la derecha
                outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT)

                // Formateamos cada valor con el ancho adecuado para alineación
                val quantity = numberFormat.format(detail.quantity).padStart(4)
                val unitPrice = numberFormat.format(detail.unitPrice).padStart(7)
                val discount = numberFormat.format(detail.totalDiscount).padStart(7)
                val total = numberFormat.format(detail.totalAmount).padStart(7)

                writer.write("$quantity $unitPrice $discount $total\n")

                // Separador entre productos
                writer.write("----------------------------\n")
            }

            // Sección de totales - Todo alineado a la derecha
            outputStream.write(PrinterCommands.ESC_ALIGN_RIGHT)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("RESUMEN\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Formatear los montos para que queden alineados
            val labelWidth = 15 // Ancho para las etiquetas

            val opDescuento = "DESCUENTO:".padEnd(labelWidth) + numberFormat.format(operation.totalDiscount).padStart(10)
            val opGravada = "OP. GRAVADA:".padEnd(labelWidth) + numberFormat.format(operation.totalTaxed).padStart(10)
            val opInafecta = "OP. INAFECTA:".padEnd(labelWidth) + numberFormat.format(operation.totalUnaffected).padStart(10)
            val opExonerada = "OP. EXONERADA:".padEnd(labelWidth) + numberFormat.format(operation.totalExonerated).padStart(10)
            val opGratuita = "OP. GRATUITA:".padEnd(labelWidth) + numberFormat.format(operation.totalFree).padStart(10)
            val igv = "IGV:".padEnd(labelWidth) + numberFormat.format(operation.totalIgv).padStart(10)

            // Total con formato destacado
            val total = "TOTAL:".padEnd(labelWidth) + numberFormat.format(operation.totalAmount).padStart(10)

            writer.write("$opDescuento\n")
            writer.write("$opExonerada\n")
            writer.write("$opInafecta\n")
            writer.write("$opGratuita\n")
            writer.write("$opGravada\n")
            writer.write("$igv\n")
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("$total\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)
            // Antes de "Gracias por su compra"
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)

            // Generar contenido del QR
            val qrContent = buildString {
                appendln("${pdfGenerator.company.businessName}")
                appendln("RUC:${pdfGenerator.company.doc}")
                appendln("${operation.documentTypeReadable}")
                appendln("${operation.serial}-${operation.correlative}")
                appendln("Total:${numberFormat.format(operation.totalAmount)}")
                appendln(operation.emitDate.formatToDisplayDateTime())
            }

            // Imprimir QR
            val qrBitmap = generateQrCode(qrContent, 180) // Tamaño ajustable
            val qrBytes = convertBitmapToEscPos(qrBitmap)
            outputStream.write(qrBytes)
            // Pie de página
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            writer.write("\nGracias por su compra\n")
            writer.write("4 SOLUCIONES\n")
            writer.write("https://www.tuf4ct.com\n\n")
            writer.write("\n\n\n")

            // Finalización
            outputStream.write(PrinterCommands.ESC_FEED_PAPER_AND_CUT)
            writer.flush()
            outputStream.flush()
        } finally {
            writer.close()
        }
    }
    private fun printLogo(outputStream: OutputStream, base64Logo: String) {
        try {
            // Extraer solo los datos Base64 si viene con prefijo
            val cleanBase64 = base64Logo.substringAfter("base64,")

            // Decodificar a bitmap
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Redimensionar (ancho máximo 384px para impresoras térmicas)
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                384,
                (384 * bitmap.height / bitmap.width).toInt(),
                true
            )

            // Convertir a formato imprimible (versión corregida)
            val logoBytes = ByteArrayOutputStream().apply {
                // 1. Escribir comando de alineación
                write(PrinterCommands.ESC_ALIGN_CENTER)

                // 2. Escribir cabecera de comando de imagen ESC/POS
                write(byteArrayOf(0x1D, 0x76, 0x30, 0x00)) // GS v 0

                // 3. Escribir dimensiones de la imagen (little-endian)
                val width = scaledBitmap.width
                val height = scaledBitmap.height
                val widthBytes = (width + 7) / 8 // Ancho en bytes (redondeado hacia arriba)

                write(byteArrayOf(
                    (widthBytes % 256).toByte(),  // LSB del ancho
                    (widthBytes / 256).toByte(),  // MSB del ancho
                    (height % 256).toByte(),      // LSB del alto
                    (height / 256).toByte()       // MSB del alto
                ))

                // 4. Escribir datos de imagen (versión corregida)
                val imageData = ByteArray(widthBytes * height)

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val pixel = scaledBitmap.getPixel(x, y)
                        val isBlack = Color.red(pixel) < 128 &&
                                Color.green(pixel) < 128 &&
                                Color.blue(pixel) < 128

                        if (isBlack) {
                            val bytePos = y * widthBytes + x / 8
                            val bitPos = 7 - (x % 8)
                            imageData[bytePos] = (imageData[bytePos].toInt() or (1 shl bitPos)).toByte()
                        }
                    }
                }

                // Escribir todos los datos de imagen de una vez
                write(imageData)
            }.toByteArray()

            outputStream.write(logoBytes)
            outputStream.write(PrinterCommands.FEED_LINE) // Salto de línea

        } catch (e: Exception) {
            // Si falla, imprimir texto alternativo
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            outputStream.write("[LOGO]".toByteArray())
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
        }
    }
    private fun printQrCode(outputStream: OutputStream, qrText: String) {
        try {
            val qrBitmap = generateQrCode(qrText)
            val qrBytes = convertBitmapToEscPos(qrBitmap)

            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(qrBytes)
            outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
        } catch (e: Exception) {
            Log.e("PrintQR", "Error al imprimir QR: ${e.message}")
        }
    }
    private fun generateQrCode(text: String, size: Int = 200): Bitmap {
        val writer = MultiFormatWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
    private fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8
        val imageData = ByteArray(widthBytes * height)

        // Convertir bitmap a datos monocromo
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                if (luminance < 160) { // Umbral para negro
                    val bytePos = y * widthBytes + x / 8
                    val bitPos = 7 - (x % 8)
                    imageData[bytePos] = (imageData[bytePos].toInt() or (1 shl bitPos)).toByte()
                }
            }
        }

        // Construir comando ESC/POS correctamente
        return byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,  // GS v 0
            (widthBytes % 256).toByte(),
            (widthBytes / 256).toByte(),
            (height % 256).toByte(),
            (height / 256).toByte()
        ) + imageData // Concatenar arrays de bytes
    }
    private fun closeResources(socket: BluetoothSocket?, outputStream: OutputStream?) {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e("PrintViewModel", "Error cerrando output stream", e)
        }

        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("PrintViewModel", "Error cerrando socket", e)
        }
    }
    // Función actualizada para manejar los tipos de documento
    private fun formatDocumentType(documentType: String?): String {
        // Si es nulo, retornamos DOCUMENTO directamente
        if (documentType == null) return "DOCUMENTO"

        // Procesamos el código para quitar el prefijo A_ si existe
        val processedType = if (documentType.startsWith("A_")) {
            documentType.substring(2) // Quita los primeros 2 caracteres (A_)
        } else {
            documentType
        }

        // Procesamos el tipo de documento usando el valor limpio
        return when (processedType.uppercase()) {
            "01", "1" -> "DNI"
            "06", "6" -> "RUC"
            "07", "7", "CE" -> "CARNET DE EXTRANJERÍA"
            "08", "8", "PAS" -> "PASAPORTE"
            "09", "9", "CDI" -> "CÉDULA DE IDENTIDAD"
            "OTR" -> "OTROS"
            else -> processedType
        }
    }
    // Función para formatear la fecha y hora en formato legible
    private fun String.formatToDisplayDateTime(): String {
        try {
            // Asumiendo que el formato de entrada es "YYYY-MM-DD HH:MM:SS"
            val parts = this.split(" ")
            if (parts.size != 2) return this

            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")

            if (dateParts.size != 3 || timeParts.size < 2) return this

            val year = dateParts[0]
            val month = dateParts[1]
            val day = dateParts[2]

            val hour = timeParts[0]
            val minute = timeParts[1]

            // Convertir mes numérico a nombre
            val monthName = when (month) {
                "01" -> "ENE"
                "02" -> "FEB"
                "03" -> "MAR"
                "04" -> "ABR"
                "05" -> "MAY"
                "06" -> "JUN"
                "07" -> "JUL"
                "08" -> "AGO"
                "09" -> "SEP"
                "10" -> "OCT"
                "11" -> "NOV"
                "12" -> "DIC"
                else -> month
            }

            return "$day-$monthName-$year $hour:$minute"
        } catch (e: Exception) {
            // En caso de error, devolver el string original
            return this
        }
    }
}
