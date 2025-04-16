package com.example.fibo.reports

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
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
    private var currentOperation: IOperation? = null

    fun fetchOperationById(operationId: Int) {
        viewModelScope.launch {
            _uiState.value = PdfDialogUiState.Loading
            try {
                val operation = operationRepository.getOperationById(operationId)
                currentOperation = operation
                _uiState.value = PdfDialogUiState.Success(operation)
            } catch (e: ApolloException) {
                _uiState.value = PdfDialogUiState.Error("Error de conexión: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun scanForPrinters(context: Context) {
        _uiState.value = PdfDialogUiState.ScanningPrinters
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("PrintViewModel", "Iniciando escaneo de impresoras...")

                // Verificaciones de Bluetooth con manejo seguro de errores
                val bluetoothAdapter = try {
                    BluetoothAdapter.getDefaultAdapter()
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error obteniendo adapter", e)
                    null
                }
                if (bluetoothAdapter == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Bluetooth no soportado")
                    }
                    return@launch
                }

//                if (!bluetoothAdapter.isEnabled) {
//                    withContext(Dispatchers.Main) {
//                        _uiState.value = PdfDialogUiState.BluetoothDisabled
//                    }
//                    return@launch
//                }
                if (!isEnabled) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.BluetoothDisabled
                    }
                    return@launch
                }

                // 2. Verificar permisos
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
                        withContext(Dispatchers.Main) {
                            _uiState.value = PdfDialogUiState.Error(
                                "Se requieren permisos: ${missingPermissions.joinToString()}"
                            )
                        }
                        return@launch
                    }
                }

                // Permiso de ubicación para versiones anteriores
                if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.R) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = PdfDialogUiState.Error(
                                "Se requiere permiso de ubicación para escanear dispositivos Bluetooth"
                            )
                        }
                        return@launch
                    }
                }
                // Obtener dispositivos emparejados con manejo seguro
                val pairedDevices = try {
                    withTimeout(5000) {
                        bluetoothAdapter.bondedDevices ?: emptySet()
                    }
                } catch (e: Exception) {
                    Log.e("Bluetooth", "Error obteniendo dispositivos", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Error: ${e.message}")
                    }
                    return@launch
                }

                // 4. Filtrar impresoras y priorizar
                val printers = pairedDevices.sortedByDescending { device ->
                    val name = device.name?.lowercase() ?: ""
                    when {
                        name.contains("adv") && name.contains("7010") -> 100
                        name.contains("adv") || name.contains("advance") -> 50
                        name.contains("print") || name.contains("impres") -> 25
                        else -> 0
                    }
                }

                // 5. Actualizar la UI en el hilo principal
                withContext(Dispatchers.Main) {
                    _availablePrinters.value = printers

                    val currentState = _uiState.value
                    if (currentState is PdfDialogUiState.Success) {
                        _uiState.value = currentState.copy(printers = printers)
                    } else {
                        _uiState.value = PdfDialogUiState.PrintersFound(printers)
                    }

                    // Autoseleccionar la primera impresora si es Advance
                    printers.firstOrNull {
                        it.name?.contains("ADV", ignoreCase = true) == true ||
                                it.name?.contains("Advance", ignoreCase = true) == true
                    }?.let { advancePrinter ->
                        _selectedPrinter.value = advancePrinter
                    }
                }

            } catch (e: Exception) {
                Log.e("PrintViewModel", "Error al escanear impresoras", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = PdfDialogUiState.Error("Error: ${e.message}")
                }
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
    fun printOperation(context: Context, pdfFile: File) {
        // Actualizar estado inmediatamente
        _uiState.value = PdfDialogUiState.Printing

        viewModelScope.launch(Dispatchers.IO) {
            var socket: BluetoothSocket? = null

            try {
                val printer = _selectedPrinter.value
                val operation = currentOperation

                // Validaciones previas
                if (printer == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("No hay impresora seleccionada")
                    }
                    return@launch
                }

                if (operation == null) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("No hay operación para imprimir")
                    }
                    return@launch
                }

                // Verificar permisos
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
                    PackageManager.PERMISSION_GRANTED) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT requerido")
                    }
                    return@launch
                }

                // Conectar a la impresora con timeout
                try {
                    socket = withTimeout(10000) { // 10 segundos máximo para conectar
                        printer.createRfcommSocketToServiceRecord(
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        ).apply { connect() }
                    }
                } catch (e: TimeoutCancellationException) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Tiempo de conexión agotado")
                    }
                    return@launch
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Error conectando: ${e.message}")
                    }
                    return@launch
                }

                // Imprimir documento
                try {
                    val outputStream = socket.outputStream

                    // Reiniciar impresora y usar configuración correcta
                    outputStream.write(PrinterCommands.INIT)
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER) // Asegurar alineación central

                    // 1. Encabezado
                    outputStream.write(PrinterCommands.ESC_BOLD_ON)  // Inicio negrita
                    outputStream.write("EMPRESA DEMO\n".toByteArray())
                    outputStream.write(PrinterCommands.ESC_BOLD_OFF) // Fin negrita

                    // 2. Datos del cliente
                    outputStream.write(PrinterCommands.ESC_ALIGN_LEFT) // Cambiar a alineación izquierda
                    outputStream.write("\n".toByteArray()) // Espacio
                    outputStream.write("Cliente: ${operation.client.names}\n".toByteArray())
                    outputStream.write("Fecha: ${getCurrentDateTime()}\n".toByteArray())
                    outputStream.write("Operación #: ${operation.id}\n".toByteArray())

                    // 3. Línea separadora
                    outputStream.write("--------------------------------\n".toByteArray())

                    // 4. Detalles de la operación
                    // Asumiendo que tienes estos datos en tu objeto operation
                    outputStream.write("DETALLES DE LA OPERACIÓN:\n".toByteArray())
                    outputStream.write("Tipo: ${operation.operationType}\n".toByteArray())
                    if (operation.operationStatus?.isNotEmpty() == true) {
                        outputStream.write("Descripción: ${operation.operationStatus}\n".toByteArray())
                    }

                    // 5. Línea separadora
                    outputStream.write("--------------------------------\n".toByteArray())

                    // 6. Valores monetarios
                    // Ajustar según tu modelo de datos
                    outputStream.write("VALORES:\n".toByteArray())
                    outputStream.write("Subtotal: $${formatCurrency(operation.totalAmount)}\n".toByteArray())
                    outputStream.write("Total: $${formatCurrency(operation.totalAmount)}\n".toByteArray())

                    // 7. Línea separadora
                    outputStream.write("--------------------------------\n".toByteArray())

                    // 8. Mensaje final
                    outputStream.write(PrinterCommands.ESC_ALIGN_CENTER) // Centrar mensaje final
                    outputStream.write("Gracias por su preferencia!\n\n".toByteArray())

                    // 9. Avance de papel y corte
                    outputStream.write(PrinterCommands.ESC_FEED_PAPER_AND_CUT)

                    // Asegurar que todo se envíe
                    outputStream.flush()

                    // Pausa para asegurar que todo se envió
                    delay(500)

                    // Actualizar estado a completado
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.PrintComplete
                    }
                } catch (e: Exception) {
                    Log.e("PrintViewModel", "Error durante impresión", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Error imprimiendo: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PrintViewModel", "Error general en impresión", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = PdfDialogUiState.Error("Error: ${e.message}")
                }
            } finally {
                // Cerrar conexión con seguridad
                try {
                    socket?.close()
                } catch (e: Exception) {
                    Log.e("PrintViewModel", "Error al cerrar socket", e)
                }
            }
        }
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
}