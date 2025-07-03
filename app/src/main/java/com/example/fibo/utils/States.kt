package com.example.fibo.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.example.fibo.model.IOperation
import com.example.fibo.model.IProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed class ProductSearchState {
    object Idle : ProductSearchState()
    data class Loading(val query: String) : ProductSearchState()
    data class Empty(val query: String) : ProductSearchState()
    data class Error(val message: String, val query: String) : ProductSearchState()
    data class Success(val products: List<IProduct>, val query: String) : ProductSearchState()

    val currentQuery: String? get() = when (this) {
        is Idle -> null
        is Loading -> query
        is Empty -> query
        is Error -> query
        is Success -> query
    }
}
// 5. Definir los estados UI para el diálogo del PDF
sealed class PdfDialogUiState {
    object Initial : PdfDialogUiState()
    object Loading : PdfDialogUiState()
    data class Success(
        val operation: IOperation,
        val printers: List<BluetoothDevice> = emptyList(),
        val selectedPrinter: BluetoothDevice? = null
    ) : PdfDialogUiState()
    data class Error(val message: String) : PdfDialogUiState()
    object ScanningPrinters : PdfDialogUiState()
    data class PrintersFound(val printers: List<BluetoothDevice>) : PdfDialogUiState()
    object BluetoothDisabled : PdfDialogUiState()
    object Printing : PdfDialogUiState()
    object PrintComplete : PdfDialogUiState()
}
// Estado adicional para manejar la generación del PDF
sealed class PdfGenerationState {
    object Loading : PdfGenerationState()
    object Success : PdfGenerationState()
    class Error(val message: String) : PdfGenerationState()
}

sealed class QuotationState {
    object WaitingForUser : QuotationState()
    object Loading : QuotationState()
    data class Success(val data: List<IOperation>) : QuotationState()
    data class Error(val message: String) : QuotationState()
}
sealed class NoteOfSaleState {
    object WaitingForUser : NoteOfSaleState()
    object Loading : NoteOfSaleState()
    data class Success(val data: List<IOperation>) : NoteOfSaleState()
    data class Error(val message: String) : NoteOfSaleState()
}
sealed class OperationState {
    object Loading : OperationState()
    data class Success(val operation: IOperation) : OperationState()
    data class Error(val message: String) : OperationState()
}
// Define Bluetooth states
sealed class BluetoothState {
    object Disabled : BluetoothState()
    object Enabled : BluetoothState()
    object Scanning : BluetoothState()
    object DevicesFound : BluetoothState()
    object Connected : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}
sealed class ReportState {
    object Initial : ReportState()
    object Loading : ReportState()
    data class Success(val operations: List<IOperation>) : ReportState()
    data class Error(val message: String) : ReportState()
    object Empty : ReportState()
}
// Define PDF states
sealed class PdfState {
    object Loading : PdfState()
    data class Success(val file: File) : PdfState()
    data class Error(val message: String) : PdfState()
}