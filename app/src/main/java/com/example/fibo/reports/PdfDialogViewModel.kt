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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.model.IOperation
import com.example.fibo.model.IGuideData
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import kotlin.math.min
import androidx.core.app.ActivityCompat

@HiltViewModel
class PdfDialogViewModel @Inject constructor(
    val operationRepository: OperationRepository,
    val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<PdfDialogUiState>(PdfDialogUiState.Initial)
    val uiState: StateFlow<PdfDialogUiState> = _uiState.asStateFlow()

    private val _availablePrinters = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val availablePrinters: StateFlow<List<BluetoothDevice>> = _availablePrinters.asStateFlow()

    private val _selectedPrinter = MutableStateFlow<BluetoothDevice?>(null)
    val selectedPrinter: StateFlow<BluetoothDevice?> = _selectedPrinter.asStateFlow()

    private var currentOperation: IOperation? = null
    private var currentGuideData: IGuideData? = null
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
    private fun sendPrintContent(outputStream: OutputStream, operation: IOperation) {
        // Usar buffer para escritura más eficiente
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        val numberFormat = DecimalFormat("#,##0.00")

        try {
            // Inicialización de la impresora
            outputStream.write(PrinterCommands.INIT)

            // === ENCABEZADO CENTRADO ===
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)

            // IMPRIMIR LOGO PRIMERO
//            printLogo(outputStream, pdfGenerator.company.logo)

            // Encabezado - Nombre de la empresa (CENTRADO)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${pdfGenerator.company.businessName}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Datos de la empresa (CENTRADO)
            writer.write("RUC: ${pdfGenerator.company.doc}\n")
            writer.write("DIRECCION: ${pdfGenerator.subsidiary.address}\n\n")

            // Tipo de documento (CENTRADO)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${operation.documentTypeReadable}\n")
            writer.write("${operation.serial}-${operation.correlative}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            writer.write("--------------------------------\n")

            // === SECCIÓN DE CLIENTE A LA IZQUIERDA ===
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

            // === DETALLES CENTRADO ===
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("DETALLE DE PRODUCTOS\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Encabezado de columnas para la segunda línea (CENTRADO)
            writer.write("CANT   P.UNIT   DSCTO   IMPORTE\n")
            writer.write("--------------------------------\n")

            // Añadir productos
            operation.operationDetailSet.forEach { detail ->
                // Primera línea: Descripción completa del producto (IZQUIERDA)
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
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

            // === TOTALES CENTRADO ===
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("RESUMEN\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Formatear los montos para que queden alineados (CENTRADO)
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

            // ===== QR CODE NATIVO =====
            writer.flush() // MUY IMPORTANTE: Flush antes del QR

            val qrText = "|${operation.serial}-${operation.correlative}|${operation.totalAmount}|${operation.documentTypeReadable}|${operation.client.documentNumber}|${operation.client.names}"

            printNativeQRLarge(outputStream, qrText)

            // ===== DESPUÉS DEL QR: RESTABLECER ALINEACIÓN CENTRADA =====
            // 1. Flush completo para asegurar que el QR se procesó
            writer.flush()
            outputStream.flush()

            // 2. FORZAR alineación centrada para el separador y final
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1 (CENTRAR)
            writer.write("--------------------------------\n")
            writer.write("\nGracias por su compra\n")
            writer.write("DESARROLLADO POR 4 SOLUCIONES\n")
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
    //------------------------Logo PEQUEÑO CENTRADO-----------------------------------------------
//    private fun printLogo(outputStream: OutputStream, base64Logo: String?) {
//        if (base64Logo.isNullOrEmpty()) return
//
//        try {
//            val logoData = base64Logo
//            val base64Data = if (logoData.contains("data:image")) {
//                logoData.substring(logoData.indexOf(",") + 1)
//            } else {
//                logoData
//            }
//
//            val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
//            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//
//            if (originalBitmap != null) {
//                // TAMAÑO MODERADO Y SEGURO
//                val centeredBitmap = createSimpleCenteredBitmap(originalBitmap)
//                val processedBitmap = convertToMonochrome(centeredBitmap)
//                val imageData = convertBitmapToThermalPrinter(processedBitmap)
//
//                // SIN RESET - Solo alineación izquierda
//                outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Alineación izquierda
//
//                // Enviar imagen directamente
//                outputStream.write(imageData)
//                outputStream.flush()
//
//                // UNA SOLA LÍNEA EN BLANCO Y CONTINUAR
//                outputStream.write("\n".toByteArray())
//
//                // Limpiar recursos
//                processedBitmap.recycle()
//                centeredBitmap.recycle()
//                originalBitmap.recycle()
//            }
//        } catch (e: Exception) {
//            Log.e("PrintViewModel", "Error al imprimir logo: ${e.message}", e)
//        }
//    }
//
//    private fun createSimpleCenteredBitmap(originalBitmap: Bitmap): Bitmap {
//        // TAMAÑO CONSERVADOR PARA EVITAR PROBLEMAS
//        val logoMaxWidth = 220   // Tamaño moderado
//        val logoMaxHeight = 110  // Altura moderada
//        val paperWidth = 384
//
//        // Redimensionar logo
//        val resizedLogo = resizeBitmap(originalBitmap, logoMaxWidth, logoMaxHeight)
//
//        // Crear bitmap centrado
//        val centeredBitmap = Bitmap.createBitmap(paperWidth, resizedLogo.height, Bitmap.Config.RGB_565)
//        val canvas = Canvas(centeredBitmap)
//        canvas.drawColor(Color.WHITE)
//
//        // Centrar horizontalmente
//        val x = (paperWidth - resizedLogo.width) / 2
//        canvas.drawBitmap(resizedLogo, x.toFloat(), 0f, null)
//
//        if (resizedLogo != originalBitmap) {
//            resizedLogo.recycle()
//        }
//
//        return centeredBitmap
//    }
//
//    private fun resizeBitmap(original: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
//        val width = original.width
//        val height = original.height
//
//        if (width <= maxWidth && height <= maxHeight) {
//            return original
//        }
//
//        val ratioWidth = maxWidth.toFloat() / width.toFloat()
//        val ratioHeight = maxHeight.toFloat() / height.toFloat()
//        val ratio = minOf(ratioWidth, ratioHeight)
//
//        val newWidth = (width * ratio).toInt()
//        val newHeight = (height * ratio).toInt()
//
//        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
//    }
//
//    private fun convertToMonochrome(bitmap: Bitmap): Bitmap {
//        val width = bitmap.width
//        val height = bitmap.height
//        val monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
//
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                val pixel = bitmap.getPixel(x, y)
//                val gray = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114).toInt()
//                val monoColor = if (gray < 128) Color.BLACK else Color.WHITE
//                monoBitmap.setPixel(x, y, monoColor)
//            }
//        }
//
//        return monoBitmap
//    }
//
//    private fun convertBitmapToThermalPrinter(bitmap: Bitmap): ByteArray {
//        val width = bitmap.width
//        val height = bitmap.height
//        val bytesPerRow = (width + 7) / 8
//
//        val commands = mutableListOf<Byte>()
//
//        // Comando GS v 0
//        commands.add(0x1D.toByte()) // GS
//        commands.add(0x76.toByte()) // v
//        commands.add(0x30.toByte()) // 0
//        commands.add(0x00.toByte()) // m = 0
//
//        // Ancho en bytes (little endian)
//        commands.add((bytesPerRow and 0xFF).toByte())
//        commands.add(((bytesPerRow shr 8) and 0xFF).toByte())
//
//        // Alto en píxeles (little endian)
//        commands.add((height and 0xFF).toByte())
//        commands.add(((height shr 8) and 0xFF).toByte())
//
//        // Convertir bitmap a datos binarios
//        for (y in 0 until height) {
//            for (x in 0 until width step 8) {
//                var byteValue = 0
//
//                for (bit in 0 until 8) {
//                    val pixelX = x + bit
//                    if (pixelX < width) {
//                        val pixel = bitmap.getPixel(pixelX, y)
//                        if (pixel == Color.BLACK || Color.red(pixel) < 128) {
//                            byteValue = byteValue or (0x80 shr bit)
//                        }
//                    }
//                }
//
//                commands.add(byteValue.toByte())
//            }
//        }
//
//        return commands.toByteArray()
//    }
    //-------------------------logo-----------------------------------------------

    //-----------------------------QR---------------------------------
    private fun printNativeQR(outputStream: OutputStream, text: String) {
        try {
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val dataLength = textBytes.size

            // 1. Centrar
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1

            // 2. Seleccionar modelo QR (Modelo 2)
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

            // 3. Configurar tamaño del módulo (3 = tamaño medio)
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x03))

            // 4. Configurar nivel de corrección de errores (L = 48, M = 49, Q = 50, H = 51)
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30)) // Nivel L

            // 5. Almacenar datos del QR
            val pL = (dataLength + 3) and 0xFF
            val pH = ((dataLength + 3) shr 8) and 0xFF
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
            outputStream.write(textBytes)

            // 6. Imprimir QR
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

            // 7. Volver a alineación izquierda
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))

            outputStream.write("\n\n".toByteArray())
            outputStream.flush()

            Log.d("QR", "QR nativo enviado correctamente")

        } catch (e: Exception) {
            Log.e("QR", "Error QR nativo: ${e.message}")
        }
    }

    // VERSIÓN ALTERNATIVA CON TAMAÑO DIFERENTE (CORREGIDA)
    private fun printNativeQRLarge(outputStream: OutputStream, text: String) {
        try {
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val dataLength = textBytes.size

            Log.d("QR", "Iniciando impresión de QR con texto: $text")

            // 1. Centrar para el QR
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar
            outputStream.flush()
            Thread.sleep(50)

            // 2. Modelo QR 2
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
            outputStream.flush()
            Thread.sleep(50)

            // 3. Tamaño más grande (5)
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05))
            outputStream.flush()
            Thread.sleep(50)

            // 4. Nivel de corrección M (mejor que L)
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))
            outputStream.flush()
            Thread.sleep(50)

            // 5. Almacenar datos
            val pL = (dataLength + 3) and 0xFF
            val pH = ((dataLength + 3) shr 8) and 0xFF
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
            outputStream.write(textBytes)
            outputStream.flush()
            Thread.sleep(100)

            // 6. Imprimir QR
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
            outputStream.flush()
            Thread.sleep(200) // Pausa más larga para que se procese el QR

            // 7. RESETEAR completamente la alineación a izquierda
            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00)) // Izquierda
            outputStream.flush()

            Log.d("QR", "QR impreso exitosamente")

        } catch (e: IOException) {
            Log.e("QR", "Error de E/S al imprimir QR: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("QR", "Error inesperado al imprimir QR: ${e.message}")
            throw e
        }
    }

    // VERSIÓN SIMPLE Y PEQUEÑA
    private fun printNativeQRSmall(outputStream: OutputStream, text: String) {
        try {
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val dataLength = textBytes.size

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x01)) // Centrar

            // Comandos básicos para QR pequeño
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00)) // Modelo
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x02)) // Tamaño 2
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30)) // Error L

            // Datos
            val pL = (dataLength + 3) and 0xFF
            val pH = ((dataLength + 3) shr 8) and 0xFF
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
            outputStream.write(textBytes)

            // Imprimir
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

            outputStream.write(byteArrayOf(0x1B, 0x61, 0x00))
            outputStream.write("\n".toByteArray())
            outputStream.flush()

        } catch (e: Exception) {
            Log.e("QR", "Error QR pequeño: ${e.message}")
        }
    }
    //---------------------------QR--------------------------------------------

    //------------------------LOGO VERSION MEJORADA-----------------------------
    private fun printLogo(outputStream: OutputStream, base64Logo: String?) {
        if (base64Logo.isNullOrEmpty()) return

        try {
            val pureBase64 = base64Logo.substringAfter(",")
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return

            val maxWidth = 300
            val maxHeight = 150

            val scaledBitmap = resizeBitmap(originalBitmap, maxWidth, maxHeight)
            val monoBitmap = convertToMonochromeProper(scaledBitmap) // Cambiado a nueva función
            val printCommands = generatePrintCommands(monoBitmap)

            outputStream.write(printCommands)
            outputStream.write("\n".toByteArray())
            outputStream.flush()

            monoBitmap.recycle()
            scaledBitmap.recycle()
            originalBitmap.recycle()
        } catch (e: Exception) {
            Log.e("PrintLogo", "Error: ${e.message}")
        }
    }

// ... (resizeBitmap sigue igual) ...

    private fun convertToMonochromeProper(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Convertir a escala de grises
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        // Aplicar umbral manteniendo la relación original (negro sigue negro, blanco sigue blanco)
        val threshold = 128
        val thresholdMatrix = ColorMatrix(floatArrayOf(
            85f, 85f, 85f, 0f, -128f * 85f,  // R
            85f, 85f, 85f, 0f, -128f * 85f,  // G
            85f, 85f, 85f, 0f, -128f * 85f,  // B
            0f, 0f, 0f, 1f, 0f               // A
        ))

        val combinedMatrix = ColorMatrix().apply {
            postConcat(grayscaleMatrix)
            postConcat(thresholdMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            // Horizontal
            newWidth = min(bitmap.width, maxWidth)
            newHeight = (newWidth / aspectRatio).toInt()
        } else {
            // Vertical
            newHeight = min(bitmap.height, maxHeight)
            newWidth = (newHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun convertToMonochromeSimple(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // 1. Convertir a escala de grises
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f) // Eliminar saturación (escala de grises)
        }

        // 2. Aplicar umbral (threshold) para convertir a blanco/negro
        val threshold = 128 // Puntos más oscuros que esto se vuelven negros
        val thresholdMatrix = ColorMatrix(floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, (-threshold).toFloat(),
            0.299f, 0.587f, 0.114f, 0f, (-threshold).toFloat(),
            0.299f, 0.587f, 0.114f, 0f, (-threshold).toFloat(),
            0f, 0f, 0f, 1f, 255f
        ))

        // Combinar ambas matrices
        val combinedMatrix = ColorMatrix().apply {
            postConcat(grayscaleMatrix)
            postConcat(thresholdMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(combinedMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun generatePrintCommands(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerLine = (width + 7) / 8
        val output = ByteArrayOutputStream()

        // --- Centrado manual: calcular padding horizontal ---
        // Ancho máximo de impresión (en píxeles, típicamente 384 para impresoras de 80mm)
        val printerMaxWidthPixels = 384
        val paddingPixels = (printerMaxWidthPixels - width) / 2
        val paddingBytes = paddingPixels / 8

        // Comando GS v 0 (modo raster)
        output.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))

        // Ancho en bytes (little endian) incluyendo padding
        val totalBytesPerLine = bytesPerLine + paddingBytes
        output.write(byteArrayOf(
            (totalBytesPerLine % 256).toByte(),
            (totalBytesPerLine / 256).toByte()
        ))

        // Alto en píxeles (little endian)
        output.write(byteArrayOf(
            (height % 256).toByte(),
            (height / 256).toByte()
        ))

        // Datos de la imagen con padding
        for (y in 0 until height) {
            // Añadir bytes de padding (blanco) al inicio de cada línea
            repeat(paddingBytes) { output.write(0x00) }

            // Bytes de la imagen
            for (x in 0 until width step 8) {
                var byteValue = 0
                for (bit in 0..7) {
                    val px = x + bit
                    if (px < width && bitmap.getPixel(px, y) == Color.BLACK) {
                        byteValue = byteValue or (1 shl (7 - bit))
                    }
                }
                output.write(byteValue)
            }
        }

        return output.toByteArray()
    }

    // Método para imprimir guía a dispositivo Bluetooth
    fun printToBluetoothDevice(context: Context, device: BluetoothDevice, guideData: IGuideData) {
        viewModelScope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            var outputStream: OutputStream? = null
            
            try {
                // Mostrar estado de impresión
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.Loading)
                }

                // Cancelar descubrimiento antes de conectar
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        bluetoothAdapter?.cancelDiscovery()
                    }
                } else {
                    bluetoothAdapter?.cancelDiscovery()
                }

                // Pequeña pausa para asegurar que el descubrimiento se cancele
                delay(500)

                // Crear un UUID para el servicio de impresión SPP
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                // Obtener un socket Bluetooth con timeout
                socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        device.createRfcommSocketToServiceRecord(uuid)
                    } else {
                        throw SecurityException("Se requiere permiso BLUETOOTH_CONNECT")
                    }
                } else {
                    device.createRfcommSocketToServiceRecord(uuid)
                }

                Log.d("BluetoothPrint", "Intentando conectar a ${device.name}")

                // Conectar con timeout
                withTimeout(15000) { // 15 segundos timeout
                    socket.connect()
                }

                Log.d("BluetoothPrint", "Conectado exitosamente a ${device.name}")

                // Obtener flujo de salida
                outputStream = socket.outputStream
                
                // Pequeña pausa después de conectar
                delay(1000)
                
                // Enviar contenido de la guía con manejo de errores mejorado
                sendGuideContentSafely(outputStream, guideData)

                // Pausa antes de cerrar para asegurar que todos los datos se envíen
                delay(2000)

                Log.d("BluetoothPrint", "Impresión completada exitosamente")

                // Mostrar mensaje de éxito en el hilo principal
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.PrintComplete)
                }

            } catch (e: TimeoutCancellationException) {
                Log.e("BluetoothPrint", "Timeout al conectar con la impresora")
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.Error("Timeout al conectar con la impresora. Verifique que esté encendida y cerca."))
                }
            } catch (e: SecurityException) {
                Log.e("BluetoothPrint", "Error de permisos: ${e.message}")
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.Error("Error de permisos Bluetooth: ${e.message}"))
                }
            } catch (e: IOException) {
                Log.e("BluetoothPrint", "Error de conexión Bluetooth: ${e.message}")
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.Error("Error de conexión: ${e.message}. Verifique que la impresora esté encendida."))
                }
            } catch (e: Exception) {
                Log.e("BluetoothPrint", "Error en la impresión: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    updateUiState(PdfDialogUiState.Error("Error en la impresión: ${e.message}"))
                }
            } finally {
                // Cerrar recursos de manera segura
                try {
                    outputStream?.close()
                    Log.d("BluetoothPrint", "OutputStream cerrado")
                } catch (e: Exception) {
                    Log.e("BluetoothPrint", "Error al cerrar outputStream: ${e.message}")
                }

                try {
                    socket?.close()
                    Log.d("BluetoothPrint", "Socket cerrado")
                } catch (e: Exception) {
                    Log.e("BluetoothPrint", "Error al cerrar socket: ${e.message}")
                }
            }
        }
    }

    // Función para convertir texto con acentos a formato compatible con impresora
    private fun convertAccents(text: String): String {
        return text
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")
            .replace("Ñ", "N")
            .replace("ü", "u")
            .replace("Ü", "U")
    }

    private fun sendGuideContentSafely(outputStream: OutputStream, guideData: IGuideData) {
        try {
            sendGuideContent(outputStream, guideData)
        } catch (e: IOException) {
            Log.e("BluetoothPrint", "Error de E/S durante la impresión: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("BluetoothPrint", "Error inesperado durante la impresión: ${e.message}")
            throw e
        }
    }

    private fun sendGuideContent(outputStream: OutputStream, guideData: IGuideData) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8))
        val numberFormat = DecimalFormat("#,##0.00")

        try {
            // Inicialización de la impresora
            outputStream.write(PrinterCommands.INIT)
            outputStream.flush()
            Thread.sleep(200) // Pequeña pausa después de inicializar
            
            // Configurar codificación básica para impresoras térmicas
            outputStream.write(byteArrayOf(0x1B, 0x74, 0x00)) // ESC t 0 (PC437)
            outputStream.flush()
            Thread.sleep(100)
            
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.flush()

            // Encabezado - Nombre de la empresa
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${convertAccents(guideData.subsidiary.company?.businessName ?: "")}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Dirección de la empresa
            writer.write("${convertAccents(guideData.subsidiary.address)}\n")
            writer.write("${convertAccents(guideData.subsidiary.geographicLocationByDistrict)}\n")
            writer.write("RUC ${guideData.subsidiary.company?.doc ?: ""}\n\n")

            // Tipo de documento
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            when (guideData.documentType) {
                "A_09" -> writer.write("GUIA DE REMISION REMITENTE ELECTRONICA\n")
                "A_31" -> writer.write("GUIA DE REMISION TRANSPORTISTA ELECTRONICA\n")
                else -> writer.write("${convertAccents(guideData.documentTypeReadable)}\n")
            }
            writer.write("${guideData.serial}-${String.format("%06d", guideData.correlative)}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            writer.write("\n")
            writer.flush()
            outputStream.flush()
            Thread.sleep(100) // Pausa después del encabezado

            // Sección de remitente (solo para guías de remisión remitente)
            if (guideData.documentType == "A_09") {
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
                outputStream.write(PrinterCommands.ESC_BOLD_ON)
                writer.write("REMITENTE\n")
                outputStream.write(PrinterCommands.ESC_BOLD_OFF)
                writer.write("${guideData.client.documentType}: ${guideData.client.documentNumber}\n")
                writer.write("${convertAccents(guideData.client.names ?: "")}\n\n")
            }

            // Sección de destinatario (solo para guías de remisión transportista)
            if (guideData.documentType == "A_31") {
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
                outputStream.write(PrinterCommands.ESC_BOLD_ON)
                writer.write("DESTINATARIO\n")
                outputStream.write(PrinterCommands.ESC_BOLD_OFF)
                writer.write("${guideData.receiver?.documentType ?: ""}: ${guideData.receiver?.documentNumber ?: ""}\n")
                writer.write("${convertAccents(guideData.receiver?.names ?: "")}\n\n")
            }

            // Datos del traslado
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("DATOS DEL TRASLADO\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)
            writer.write("FECHA EMISION: ${formatDate(guideData.emitDate)}\n")
            guideData.transferDate?.let { transferDate ->
                writer.write("FECHA DE ENTREGA DE BIENES\nAL TRANSPORTISTA: ${formatDate(transferDate)}\n")
            }

            // Motivo y modalidad solo para guías de remisión remitente
            if (guideData.documentType == "A_09") {
                guideData.guideReasonTransferReadable?.let { reason ->
                    writer.write("MOTIVO DE TRASLADO: ${convertAccents(reason)}\n")
                }
                guideData.guideModeTransferReadable?.let { mode ->
                    writer.write("MODALIDAD DE TRANSPORTE:\n${convertAccents(mode)}\n")
                }
            }

            guideData.totalWeight?.let { weight ->
                val unitName = guideData.weightMeasurementUnit?.shortName ?: "KGM"
                writer.write("PESO BRUTO TOTAL ($unitName): $weight\n")
            }
            guideData.quantityPackages?.let { packages ->
                writer.write("NUMERO DE BULTOS: $packages\n")
            }

            writer.write("\n")
            writer.flush()
            outputStream.flush()
            Thread.sleep(100) // Pausa después de datos del traslado

            // Datos del transporte
            writer.write("DATOS DEL TRANSPORTE\n")

            // Transportista (solo para modalidad transporte público)
            guideData.transportationCompany?.let { company ->
                writer.write("TRANSPORTISTA: ${company.documentType} ${company.documentNumber}\n${convertAccents(company.names ?: "")}\n")
            }

            // Vehículo principal (solo para modalidad transporte privado)
            guideData.mainVehicle?.let { vehicle ->
                writer.write("VEHICULO PRINCIPAL: ${vehicle.licensePlate}\n")
            }

            // Conductor principal (solo para modalidad transporte privado)
            guideData.mainDriver?.let { driver ->
                writer.write("CONDUCTOR PRINCIPAL: ${driver.documentType} ${driver.documentNumber}\n${convertAccents(driver.names ?: "")}\n")
                driver.driverLicense?.let { license ->
                    writer.write("LICENCIA DE CONDUCIR DEL\nCONDUCTOR PRINCIPAL: $license\n")
                }
            }

            // Punto de partida
            guideData.guideOrigin?.let { origin ->
                writer.write("PUNTO DE PARTIDA\n")
                writer.write("(${origin.district.id}) - ${convertAccents(origin.district.description)} - ${convertAccents(origin.address)}\n")
            }

            // Punto de llegada
            guideData.guideArrival?.let { arrival ->
                writer.write("PUNTO DE LLEGADA\n")
                writer.write("(${arrival.district.id}) - ${convertAccents(arrival.district.description)} - ${convertAccents(arrival.address)}\n")
            }

            writer.write("\n")
            writer.flush()
            outputStream.flush()
            Thread.sleep(100) // Pausa después de datos del transporte

            // Detalle de productos
            writer.write("DETALLE DE PRODUCTOS\n")
            writer.write("#      DESCRIPCION      CANT\n")
            guideData.operationDetailSet.forEachIndexed { index, detail ->
                outputStream.write(PrinterCommands.ESC_ALIGN_LEFT)
                writer.write(
                    if (detail.description.isNotBlank()) {
                        "${index + 1}   ${detail.productName.uppercase()} (${detail.description.uppercase()})   ${detail.quantity}\n"
                    } else {
                        "${index + 1}   ${detail.productName.uppercase()}   ${detail.quantity}\n"
                    }
                )
            }

            writer.write("\n")

            // Documentos relacionados
            if (guideData.relatedDocuments.isNotEmpty()) {
                outputStream.write(PrinterCommands.ESC_BOLD_ON)
                writer.write("DOCUMENTOS RELACIONADOS:\n")
                outputStream.write(PrinterCommands.ESC_BOLD_OFF)
                guideData.relatedDocuments.forEach { doc ->
                    val docTypeName = when (doc.documentType) {
                        "A_01" -> "FACTURA ELECTRONICA"
                        "A_03" -> "BOLETA ELECTRONICA"
                        else -> convertAccents(doc.documentType ?: "")
                    }
                    writer.write("$docTypeName: ${doc.serial}-${String.format("%06d", doc.correlative)}\n")
                }
                writer.write("\n")
            }

            // Pie de página
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            writer.write("Representacion impresa de la \n${convertAccents(guideData.documentTypeReadable ?: "")}\n")
            writer.write("ELECTRONICA, para ver el documento visita\n")
            writer.write("https://www.tuf4ct.com/cpe\n")
            
            // IMPORTANTE: Flush antes del QR para asegurar que todo el texto se imprima primero
            writer.flush()
            outputStream.flush()
            Thread.sleep(500) // Pausa más larga antes del QR

            // Código QR
            val qrText = "https://www.tuf4ct.com/cpe/${guideData.id}"
            printNativeQRLarge(outputStream, qrText)

            // Pausa después del QR para asegurar que se procese
            Thread.sleep(1000)

            writer.write("\n\n\n")

            // Finalización
            outputStream.write(PrinterCommands.ESC_FEED_PAPER_AND_CUT)
            writer.flush()
            outputStream.flush()
        } finally {
            writer.close()
        }
    }


}
