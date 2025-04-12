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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
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

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    _uiState.value = PdfDialogUiState.Error("Bluetooth no disponible en este dispositivo")
                    return@launch
                }

                if (!bluetoothAdapter.isEnabled) {
                    _uiState.value = PdfDialogUiState.BluetoothDisabled
                    return@launch
                }

                // ✅ Verificar permiso en Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        _uiState.value = PdfDialogUiState.Error("Permiso BLUETOOTH_CONNECT denegado")
                        return@launch
                    }
                }

                val pairedDevices = bluetoothAdapter.bondedDevices
                val printers = pairedDevices.toList()

                _availablePrinters.value = printers

                val currentState = _uiState.value
                if (currentState is PdfDialogUiState.Success) {
                    _uiState.value = PdfDialogUiState.Success(
                        operation = currentState.operation,
                        printers = printers
                    )
                } else {
                    _uiState.value = PdfDialogUiState.PrintersFound(printers)
                }

            } catch (e: Exception) {
                _uiState.value = PdfDialogUiState.Error("Error al buscar impresoras: ${e.message}")
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

}