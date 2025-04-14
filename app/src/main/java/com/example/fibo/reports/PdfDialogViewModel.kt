package com.example.fibo.reports
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.PdfDialogUiState
import com.itextpdf.io.exceptions.IOException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

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

    fun fetchOperationById(operationId: Int) {
        viewModelScope.launch {
            _uiState.value = PdfDialogUiState.Loading
            try {
                val operation = operationRepository.getOperationById(operationId)
                _uiState.value = PdfDialogUiState.Success(operation)
            } catch (e: ApolloException) {
                _uiState.value = PdfDialogUiState.Error("Error de conexión: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun scanForPrinters(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = PdfDialogUiState.ScanningPrinters

                // 1. Verificar si el dispositivo soporta Bluetooth
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    _uiState.value = PdfDialogUiState.Error("Bluetooth no soportado en este dispositivo")
                    return@launch
                }

                // 2. Verificar si Bluetooth está activado
                if (!bluetoothAdapter.isEnabled) {
                    _uiState.value = PdfDialogUiState.BluetoothDisabled
                    return@launch
                }

                // 3. Verificar permisos para Android 12+ (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val missingPermissions = mutableListOf<String>().apply {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            add(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_SCAN
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            add(Manifest.permission.BLUETOOTH_SCAN)
                        }
                    }

                    if (missingPermissions.isNotEmpty()) {
                        _uiState.value = PdfDialogUiState.Error(
                            "Se requieren permisos: ${missingPermissions.joinToString()}"
                        )
                        return@launch
                    }
                }

                // 4. Para Android 6-10, verificar permiso de ubicación
                if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        _uiState.value = PdfDialogUiState.Error("Se requiere permiso de ubicación para escanear dispositivos Bluetooth")
                        return@launch
                    }
                }

                // 5. Obtener dispositivos emparejados
                val pairedDevices: Set<BluetoothDevice> = try {
                    bluetoothAdapter.bondedDevices
                } catch (e: SecurityException) {
                    _uiState.value = PdfDialogUiState.Error("Permisos insuficientes para acceder a dispositivos Bluetooth")
                    return@launch
                } catch (e: Exception) {
                    _uiState.value = PdfDialogUiState.Error("Error al obtener dispositivos Bluetooth: ${e.message}")
                    return@launch
                }

                // 6. Filtrar solo impresoras (opcional)
                val printers = pairedDevices.filter { device ->
                    // Puedes añadir filtros específicos para impresoras aquí
                    // Ejemplo: device.name?.contains("Printer") == true
                    true // Por ahora aceptamos todos los dispositivos emparejados
                }

                // 7. Actualizar estado con los resultados
                _availablePrinters.value = printers

                // 8. Mantener los datos de la operación si estaban en Success
                val currentState = _uiState.value
                if (currentState is PdfDialogUiState.Success) {
                    _uiState.value = currentState.copy(printers = printers)
                } else {
                    _uiState.value = PdfDialogUiState.PrintersFound(printers)
                }

            } catch (e: SecurityException) {
                _uiState.value = PdfDialogUiState.Error("Error de permisos: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    fun selectPrinter(device: BluetoothDevice) {
        _selectedPrinter.value = device
    }

    fun printPdf(context: Context, pdfFile: File) {
        viewModelScope.launch {
            try {
                val printer = _selectedPrinter.value
                if (printer == null) {
                    _uiState.value = PdfDialogUiState.Error("No hay una impresora seleccionada")
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        _uiState.value = PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT denegado")
                        return@launch
                    }
                }

                _uiState.value = PdfDialogUiState.Printing

                val socket = printer.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                )
                socket.connect()

                val outputStream = socket.outputStream
                val currentState = _uiState.value
                if (currentState is PdfDialogUiState.Success) {
                    val operation = currentState.operation

                    outputStream.write("EMPRESA DEMO S.A.C.\n".toByteArray())
                    outputStream.write("${operation.documentTypeReadable}\n".toByteArray())
                    outputStream.write("${operation.serial} - ${operation.correlative}\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("Cliente: ${operation.client.names}\n".toByteArray())

                    operation.operationDetailSet.forEach { detail ->
                        val line = "${detail.quantity} x ${detail.tariff.productName} - S/ ${detail.totalAmount}\n"
                        outputStream.write(line.toByteArray())
                    }

                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("TOTAL: S/ ${operation.totalAmount}\n".toByteArray())
                }

                outputStream.close()
                socket.close()

                _uiState.value = PdfDialogUiState.PrintComplete

            } catch (e: SecurityException) {
                _uiState.value = PdfDialogUiState.Error("Permiso de Bluetooth denegado: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error al imprimir: ${e.message}")
            }
        }
    }
    fun printOperation(context: Context, pdfFile: File) {
        viewModelScope.launch {
            try {
                val printer = _selectedPrinter.value
                if (printer == null) {
                    _uiState.value = PdfDialogUiState.Error("No hay una impresora seleccionada")
                    return@launch
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        _uiState.value = PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT denegado")
                        return@launch
                    }
                }

                _uiState.value = PdfDialogUiState.Printing

                // Obtener la operación actual
                val currentState = _uiState.value
                if (currentState !is PdfDialogUiState.Success) {
                    _uiState.value = PdfDialogUiState.Error("No se encontró la operación a imprimir")
                    return@launch
                }

                val operation = currentState.operation

                // Crear una conexión segura
                val socket = withContext(Dispatchers.IO) {
                    try {
                        printer.createRfcommSocketToServiceRecord(
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        ).also { it.connect() }
                    } catch (e: IOException) {
                        null
                    }
                }

                if (socket == null) {
                    _uiState.value = PdfDialogUiState.Error("No se pudo conectar a la impresora")
                    return@launch
                }

                try {
                    val outputStream = socket.outputStream

                    // Formato para la moneda
                    val formatoCurrency = DecimalFormat("#,##0.00")

                    // Encabezado
                    outputStream.write("\n\n".toByteArray())
                    outputStream.write("EMPRESA DEMO S.A.C.\n".toByteArray())
                    outputStream.write("${operation.documentTypeReadable}\n".toByteArray())
                    outputStream.write("${operation.serial} - ${operation.correlative}\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("Cliente: ${operation.client.names}\n".toByteArray())
                    if (operation.client.documentNumber.isNullOrEmpty()) {
                        outputStream.write("${operation.client.documentType}: ${operation.client.documentNumber}\n".toByteArray())
                    }
                    outputStream.write("Fecha: ${formatDate(operation.emitDate)}\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())

                    // Detalles
                    outputStream.write("CANT  PRODUCTO           PRECIO\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())

                    operation.operationDetailSet.forEach { detail ->
                        val cantidadStr = String.format("%-5s", detail.quantity)
                        val nombreStr = limitText(detail.tariff.productName, 17)
                        val precioStr = String.format("%7s", "S/${formatoCurrency.format(detail.totalAmount)}")

                        val line = "$cantidadStr$nombreStr $precioStr\n"
                        outputStream.write(line.toByteArray())
                    }

                    // Totales
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("SUBTOTAL: S/${formatoCurrency.format(operation.totalAmount - operation.totalIgv)}\n".toByteArray())
                    outputStream.write("IGV (18%): S/${formatoCurrency.format(operation.totalIgv)}\n".toByteArray())
                    outputStream.write("TOTAL:     S/${formatoCurrency.format(operation.totalAmount)}\n".toByteArray())

                    // Pie de página
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("¡GRACIAS POR SU COMPRA!\n".toByteArray())
                    outputStream.write("\n\n\n\n".toByteArray()) // Espacio para cortar

                    outputStream.flush()

                    // Cerrar todo correctamente
                    withContext(Dispatchers.IO) {
                        try {
                            outputStream.close()
                            socket.close()
                        } catch (e: Exception) {
                            // Ignorar errores al cerrar
                        }
                    }

                    _uiState.value = PdfDialogUiState.PrintComplete

                } catch (e: Exception) {
                    withContext(Dispatchers.IO) {
                        try {
                            socket.close()
                        } catch (e: Exception) {
                            // Ignorar errores al cerrar
                        }
                    }
                    _uiState.value = PdfDialogUiState.Error("Error al imprimir: ${e.message}")
                }

            } catch (e: SecurityException) {
                _uiState.value = PdfDialogUiState.Error("Permiso de Bluetooth denegado: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error al imprimir: ${e.message}")
            }
        }
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
}