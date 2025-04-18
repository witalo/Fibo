package com.example.fibo.utils

import android.bluetooth.BluetoothDevice
import com.example.fibo.model.IOperation
import com.example.fibo.model.IProduct

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