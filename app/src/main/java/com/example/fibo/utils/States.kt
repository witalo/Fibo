package com.example.fibo.utils

import android.bluetooth.BluetoothDevice
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.IProductOperation
import java.io.File

sealed class ProductSearchState {
    object Idle : ProductSearchState()
    data class Loading(val query: String) : ProductSearchState()
    data class Empty(val query: String) : ProductSearchState()
    data class Error(val message: String, val query: String) : ProductSearchState()
    data class Success(val products: List<IProductOperation>, val query: String) : ProductSearchState()

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
sealed class PersonState {
    object WaitingForUser : PersonState()
    object Loading : PersonState()
    data class Success(val data: List<IPerson>) : PersonState()
    data class Error(val message: String) : PersonState()
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
// ... existing code ...

data class ProductUiState(
    val products: List<IProduct> = emptyList(),
    val filteredProducts: List<IProduct> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedProduct: IProduct? = null,
    val showDeleteDialog: Boolean = false,
    val productToDelete: IProduct? = null,
    val isDeleting: Boolean = false,
    val successMessage: String? = null,
    val currentPage: Int = 1,
    val hasMoreProducts: Boolean = true,
    val sortOrder: ProductSortOrder = ProductSortOrder.NAME_ASC,
    val filterCategory: String? = null,
    val filterAvailable: Boolean? = null
)

enum class ProductSortOrder {
    NAME_ASC,
    NAME_DESC,
    CODE_ASC,
    CODE_DESC,
    STOCK_ASC,
    STOCK_DESC,
    DATE_CREATED_ASC,
    DATE_CREATED_DESC
}