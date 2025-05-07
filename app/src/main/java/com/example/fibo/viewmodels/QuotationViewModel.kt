package com.example.fibo.viewmodels

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ICompany
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISubsidiary
import com.example.fibo.reports.PrinterCommands
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.PdfDialogUiState
import com.example.fibo.utils.QuotationState
import com.example.fibo.viewmodels.HomeViewModel.InvoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuotationViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _quotationState = MutableStateFlow<QuotationState>(QuotationState.WaitingForUser)
    val quotationState: StateFlow<QuotationState> = _quotationState.asStateFlow()
    val userData = preferencesManager.userData
    val subsidiaryData = preferencesManager.subsidiaryData
    val companyData = preferencesManager.companyData

    // Create a StateFlow to hold the quotation data
    private val _quotationData = MutableStateFlow<IOperation?>(null)
    val quotationData: StateFlow<IOperation?> = _quotationData

    // Create a loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Create an error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Cargar cotizaciones cuando se inicializa el ViewModel y hay un usuario válido
        viewModelScope.launch {
            // Observar cambios en userData y cargar cotizaciones cuando tenemos un usuario
            preferencesManager.userData.collect { user ->
                if (user != null) {
                    // Aquí pasamos el ID del usuario junto con la fecha
                    loadQuotation(_selectedDate.value, user.id)
                } else {
                    _quotationState.value = QuotationState.WaitingForUser
                }
            }
        }
    }

    fun loadQuotation(date: String, userId: Int) {
        viewModelScope.launch {
            _quotationState.value = QuotationState.Loading
            val types = listOf("48")
            try {
                val quotation =
                    operationRepository.getOperationsByDateAndUserId(date, userId, types)
                _quotationState.value = QuotationState.Success(quotation)
            } catch (e: Exception) {
                _quotationState.value = QuotationState.Error(
                    e.message ?: "Error al cargar las cotizaciones"
                )
            }
        }
    }

    // Método corregido para pasar también el userId
    fun updateDate(date: String) {
        _selectedDate.value = date
        // Obtenemos el usuario actual
        val user = userData.value

        // Si tenemos un usuario, cargamos las cotizaciones con la nueva fecha
        if (user != null) {
            loadQuotation(date, user.id)
        } else {
            _quotationState.value = QuotationState.WaitingForUser
        }
    }

    // Enhanced function to get quotation details by ID
    fun getQuotationById(operationId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val operation = operationRepository.getOperationById(operationId)
                _quotationData.value = operation
            } catch (e: ApolloException) {
                _error.value = "Error de red: ${e.message}"
                Log.e("QuotationViewModel", "Apollo exception", e)
            } catch (e: Exception) {
                _error.value = "Error al obtener datos: ${e.message}"
                Log.e("QuotationViewModel", "General exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Reset function to clear data when needed
    fun resetQuotationData() {
        _quotationData.value = null
        _error.value = null
    }

    // Función para imprimir a dispositivo Bluetooth
    fun printToBluetoothDevice(context: Context, device: BluetoothDevice, quotation: IOperation) {
        // Obtener datos de empresa y subsidiaria
        val currentCompany = companyData.value
        val currentSubsidiary = subsidiaryData.value

        // Verificar que los datos existan antes de proceder
        if (currentCompany == null || currentSubsidiary == null) {
            Toast.makeText(
                context,
                "Error: No se pudo obtener información de la empresa",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Crear un UUID para el servicio de impresión SPP
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

                // Obtener un socket Bluetooth
                val socket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

                // Cancelar descubrimiento antes de conectar
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                // Verificar permisos antes de cancelar el descubrimiento
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

                try {
                    // Conectar al dispositivo
                    socket.connect()

                    // Obtener flujo de salida
                    val outputStream = socket.outputStream
                    // Pasar los datos de la empresa y subsidiaria directamente
                    sendPrintContent(
                        outputStream,
                        quotation,
                        currentCompany,
                        currentSubsidiary
                    )

                    // Cerrar conexión de manera segura
                    try {
                        outputStream.close()
                    } catch (e: Exception) {
                        Log.e(
                            "BluetoothPrint",
                            "Error al cerrar outputStream: ${e.message}"
                        )
                    }

                    try {
                        socket.close()
                    } catch (e: Exception) {
                        Log.e("BluetoothPrint", "Error al cerrar socket: ${e.message}")
                    }

                    // Mostrar mensaje de éxito en el hilo principal
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Impresión enviada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    // Cerrar el socket si hubo un error durante la conexión o impresión
                    try {
                        socket.close()
                    } catch (closeEx: Exception) {
                        Log.e(
                            "BluetoothPrint",
                            "Error al cerrar socket después de excepción: ${closeEx.message}"
                        )
                    }

                    throw e  // Re-lanzar la excepción para manejarla en el bloque catch externo
                }
            } catch (e: Exception) {
                Log.e("BluetoothPrint", "Error en la impresión: ${e.message}")
                e.printStackTrace()

                // Mostrar mensaje de error en el hilo principal
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error en la impresión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun sendPrintContent(
        outputStream: OutputStream,
        operation: IOperation,
        company: ICompany,  // Añadir parámetro para la empresa
        subsidiary: ISubsidiary  // Añadir parámetro para la subsidiaria
    ) {
        // Usar buffer para escritura más eficiente
        val writer = BufferedWriter(OutputStreamWriter(outputStream))
        val numberFormat = DecimalFormat("#,##0.00")

        try {
            // Encabezado - Logo/Nombre de la empresa
            outputStream.write(PrinterCommands.INIT)
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("${company.businessName}\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Datos de la empresa
            writer.write("RUC: ${company.doc}\n")
            writer.write("DIRECCION: ${subsidiary.address}\n\n")

            // Tipo de documento
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("COTIZACION\n")
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
                    if (detail.description.isNotBlank()) {
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

            val opDescuento =
                "DESCUENTO:".padEnd(labelWidth) + numberFormat.format(operation.totalDiscount)
                    .padStart(10)
            val opGravada =
                "OP. GRAVADA:".padEnd(labelWidth) + numberFormat.format(operation.totalTaxed)
                    .padStart(10)
            val opInafecta =
                "OP. INAFECTA:".padEnd(labelWidth) + numberFormat.format(operation.totalUnaffected)
                    .padStart(10)
            val opExonerada =
                "OP. EXONERADA:".padEnd(labelWidth) + numberFormat.format(operation.totalExonerated)
                    .padStart(10)
            val opGratuita =
                "OP. GRATUITA:".padEnd(labelWidth) + numberFormat.format(operation.totalFree)
                    .padStart(10)
            val igv =
                "IGV:".padEnd(labelWidth) + numberFormat.format(operation.totalIgv).padStart(10)

            // Total con formato destacado
            val total = "TOTAL:".padEnd(labelWidth) + numberFormat.format(operation.totalAmount)
                .padStart(10)

            writer.write("$opDescuento\n")
            writer.write("$opExonerada\n")
            writer.write("$opInafecta\n")
            writer.write("$opGratuita\n")
            writer.write("$opGravada\n")
            writer.write("$igv\n")
            outputStream.write(PrinterCommands.ESC_BOLD_ON)
            writer.write("$total\n")
            outputStream.write(PrinterCommands.ESC_BOLD_OFF)

            // Pie de página
            outputStream.write(PrinterCommands.ESC_ALIGN_CENTER)
            writer.write("4 SOLUCIONES\n")
            writer.write("https://www.tuf4ct.com\n\n")
            writer.write("\n\n\n")

            // Finalización
            outputStream.write(PrinterCommands.ESC_FEED_PAPER_AND_CUT)
            writer.flush()
            outputStream.flush()
        } catch (e: Exception) {
            Log.e("PrintOperation", "Error al imprimir: ${e.message}")
            throw e
        } finally {
            try {
                writer.close()
            } catch (e: Exception) {
                Log.e("PrintOperation", "Error al cerrar el writer: ${e.message}")
            }
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