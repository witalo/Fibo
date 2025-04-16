package com.example.fibo.reports

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    fun scanForPrinters(context: Context) {
        viewModelScope.launch {
            try {
                Log.d("PrintViewModel", "Starting printer scan...")
                _uiState.value = PdfDialogUiState.ScanningPrinters

                // 1. Verificar si el dispositivo soporta Bluetooth
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    _uiState.value =
                        PdfDialogUiState.Error("Bluetooth no soportado en este dispositivo")
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
                        _uiState.value =
                            PdfDialogUiState.Error("Se requiere permiso de ubicación para escanear dispositivos Bluetooth")
                        return@launch
                    }
                }

                // 5. Obtener dispositivos emparejados
                val pairedDevices: Set<BluetoothDevice> = try {
                    bluetoothAdapter.bondedDevices
                } catch (e: SecurityException) {
                    _uiState.value =
                        PdfDialogUiState.Error("Permisos insuficientes para acceder a dispositivos Bluetooth")
                    return@launch
                } catch (e: Exception) {
                    _uiState.value =
                        PdfDialogUiState.Error("Error al obtener dispositivos Bluetooth: ${e.message}")
                    return@launch
                }

                // 6. Filtrar impresoras y priorizar la impresora Advance
                val printers = pairedDevices.sortedByDescending { device ->
                    // Priorizar la Advance ADV 7010n
                    val isAdvancePrinter =
                        device.name?.contains("ADV", ignoreCase = true) == true ||
                                device.name?.contains("Advance", ignoreCase = true) == true

                    // Si se encuentra específicamente la ADV 7010n, darle prioridad alta
                    val isTargetModel = device.name?.contains("7010", ignoreCase = true) == true

                    when {
                        isTargetModel -> 100  // Prioridad máxima para la impresora objetivo
                        isAdvancePrinter -> 50  // Prioridad alta para cualquier Advance
                        device.name?.contains(
                            "print",
                            ignoreCase = true
                        ) == true -> 25  // Prioridad para otras impresoras
                        else -> 0  // Baja prioridad para otros dispositivos
                    }
                }

                // 7. Actualizar estado con los resultados
                _availablePrinters.value = printers

                // 8. Mantener los datos de la operación si estaban en Success y seleccionar automáticamente la primera impresora si es Advance
                //Cambiar al hilo principal para actualizaciones de la interfaz de usuario después del escaneo
                withContext(Dispatchers.Main) {
                    _availablePrinters.value = printers

                    val currentState = _uiState.value
                    if (currentState is PdfDialogUiState.Success) {
                        _uiState.value = currentState.copy(printers = printers)

                        // Auto-select printer
                        printers.firstOrNull {
                            it.name?.contains("ADV", ignoreCase = true) == true ||
                                    it.name?.contains("Advance", ignoreCase = true) == true
                        }?.let { advancePrinter ->
                            _selectedPrinter.value = advancePrinter
                        }
                    } else {
                        _uiState.value = PdfDialogUiState.PrintersFound(printers)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("PrintViewModel", "Error scanning printers", e)
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
        // Set printing state immediately
        _uiState.value = PdfDialogUiState.Printing
        viewModelScope.launch(Dispatchers.IO) {
            val printer = _selectedPrinter.value
            val operation = currentOperation
            try {
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    _uiState.value = PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT requerido")
                    return@launch
                }

                _uiState.value = PdfDialogUiState.Printing

                // Conectarse a la impresora con tiempo de espera
                val socket = try {
                    val socket = printer.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )

                    // Set connection timeout
                    socket.connect()
                    socket
                } catch (e: IOException) {
                    Log.e("PrintOperation", "Error conectando", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Error conectando a impresora: ${e.message}")
                    }
                    return@launch
                }

                try {
                    val outputStream = socket.outputStream

                    // 1. Inicializar impresora
                    outputStream.write(PrinterCommands.INIT)
                    outputStream.write(PrinterCommands.TEXT_ALIGN_CENTER)

                    // 2. Encabezado
                    outputStream.write(PrinterCommands.TEXT_BOLD_ON)
                    outputStream.write("EMPRESA DEMO\n".toByteArray())
                    outputStream.write(PrinterCommands.TEXT_BOLD_OFF)
                    // Establecer la alineación izquierda para más detalles
                    outputStream.write(PrinterCommands.TEXT_ALIGN_LEFT)
                    // 3. Detalles de la operación (usar operation)
                    outputStream.write("Cliente: ${operation.client.names}\n".toByteArray())
                    // ... resto de tu lógica de impresión
                    outputStream.write("Fecha: ${getCurrentDate()}\n".toByteArray())
                    outputStream.write("Operación: ${operation.id}\n".toByteArray())
                    outputStream.write("---------------------------\n".toByteArray())

                    // 4. Finalizar
                    outputStream.write(PrinterCommands.FEED_PAPER_AND_CUT)
                    outputStream.flush()

                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.PrintComplete
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PdfDialogUiState.Error("Error imprimiendo: ${e.message}")
                    }
                } finally {
                    try {
                        socket.close()
                    } catch (e: Exception) {
                        Log.e("PrintOperation", "Error cerrando socket", e)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = PdfDialogUiState.Error("Error: ${e.message}")
                }
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