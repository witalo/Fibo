package com.example.fibo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.*
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class NewPurchaseViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPurchaseUiState())
    val uiState: StateFlow<NewPurchaseUiState> = _uiState.asStateFlow()

    // Estados para búsqueda de proveedores
    private val _supplierSearchResults = MutableStateFlow<List<ISupplier>>(emptyList())
    val supplierSearchResults: StateFlow<List<ISupplier>> = _supplierSearchResults.asStateFlow()

    private val _isSearchingSupplier = MutableStateFlow(false)
    val isSearchingSupplier: StateFlow<Boolean> = _isSearchingSupplier.asStateFlow()

    // Estados para búsqueda de productos (igual que NoteOfSale)
    private val _searchResults = MutableStateFlow<List<IProductOperation>>(emptyList())
    val searchResults: StateFlow<List<IProductOperation>> = _searchResults.asStateFlow()

    private val _searchState = MutableStateFlow<ProductSearchState>(ProductSearchState.Idle)
    val searchState: StateFlow<ProductSearchState> = _searchState.asStateFlow()

    private val _isFromBarcodeScan = MutableStateFlow(false)
    val isFromBarcodeScan: StateFlow<Boolean> = _isFromBarcodeScan.asStateFlow()

    // Estado para el producto seleccionado con tarifa
    private val _selectedProduct = MutableStateFlow<ITariff?>(null)
    val selectedProduct: StateFlow<ITariff?> = _selectedProduct.asStateFlow()

    // Estados para pagos (EXACTAMENTE igual que NoteOfSale)
    private val _payments = MutableStateFlow<List<IPayment>>(emptyList())
    val payments: StateFlow<List<IPayment>> = _payments.asStateFlow()

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _paymentSummary = MutableStateFlow(PaymentSummary(0.0, 0.0, 0.0))
    val paymentSummary: StateFlow<PaymentSummary> = _paymentSummary.asStateFlow()

    // ✅ Configurar paymentsEnabled basado en PreferencesManager (igual que NoteOfSale)
    val paymentsEnabled: StateFlow<Boolean> = preferencesManager.companyData
        .map { company ->
            // Si disableContinuePay es false, los pagos SÍ están habilitados
            !(company?.disableContinuePay ?: false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Por defecto habilitado
        )

    fun loadInitialData(subsidiaryId: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Ya no necesitamos cargar series, se ingresan manualmente
                // Ya no necesitamos cargar proveedores, se buscan por RUC/DNI

                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar datos iniciales"
                )
            }
        }
    }

    // Buscar proveedor por documento (RUC/DNI)
    fun searchSupplierByDocument(documentNumber: String) {
        viewModelScope.launch {
            try {
                _isSearchingSupplier.value = true

                val result = operationRepository.getSntSupplier(documentNumber)
                // ✅ Manejar el Result correctamente
                val suppliers = result.fold(
                    onSuccess = { supplier -> listOf(supplier) }, // Convertir ISupplier a List<ISupplier>
                    onFailure = { emptyList<ISupplier>() }
                )
                _supplierSearchResults.value = suppliers

            } catch (e: Exception) {
                _supplierSearchResults.value = emptyList()
            } finally {
                _isSearchingSupplier.value = false
            }
        }
    }

    // Usar exactamente la misma función que NoteOfSale
    fun searchProductsByQuery(query: String, subsidiaryId: Int, isFromScan: Boolean = false) {
        if (query.length < 3) {
            _searchState.value = ProductSearchState.Idle
            return
        }

        // Establecer el origen de la búsqueda
        _isFromBarcodeScan.value = isFromScan

        _searchState.value = ProductSearchState.Loading(query)
        viewModelScope.launch {
            try {
                // Consulta básica que devuelve sólo id, code, name
                val products = operationRepository.searchProducts(query, subsidiaryId)
                _searchResults.value = products

                if (products.isEmpty()) {
                    _searchState.value = ProductSearchState.Empty(query)
                } else {
                    _searchState.value = ProductSearchState.Success(products, query)

                    // Si es del escáner y solo hay un producto, seleccionarlo automáticamente
                    if (isFromScan && products.size == 1) {
                        // TODO: Implementar getTariff si es necesario para compras
                        // getTariff(products.first().id)
                    }
                }
            } catch (e: Exception) {
                _searchState.value = ProductSearchState.Error(
                    message = e.message ?: "Error al buscar productos",
                    query = query
                )
            } finally {
                // Resetear el flag después de procesar
                _isFromBarcodeScan.value = false
            }
        }
    }

    fun selectSupplier(supplier: ISupplier) {
        _uiState.value = _uiState.value.copy(
            supplier = supplier,
            error = null
        )
        // ✅ También limpiar los resultados de búsqueda
        _supplierSearchResults.value = emptyList()
        _isSearchingSupplier.value = false
    }

    fun clearSupplier() {
        _uiState.value = _uiState.value.copy(
            supplier = null,
            error = null
        )
        // ✅ También limpiar los resultados de búsqueda
        _supplierSearchResults.value = emptyList()
        _isSearchingSupplier.value = false
    }

    fun getTariff(productId: Int) {
        viewModelScope.launch {
            try {
                // Consulta completa que devuelve todos los datos del producto
                val tariff = operationRepository.getTariffByProductID(productId = productId)
                _selectedProduct.value = tariff
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error al obtener tarifas del producto: ${e.message}"
                )
            }
        }
    }

    fun clearProductSelection() {
        _selectedProduct.value = null
    }

    fun addProduct(product: ITariff) {
        val currentProducts = _uiState.value.products.toMutableList()
        // Verificar si el producto ya existe
        val existingIndex = currentProducts.indexOfFirst { it.productId == product.productId }
        if (existingIndex != -1) {
            // Si ya existe, incrementar la cantidad
            val existingProduct = currentProducts[existingIndex]
            currentProducts[existingIndex] = existingProduct.copy(
                // TODO: Implementar lógica de cantidad si es necesario
            )
        } else {
            // Si no existe, agregarlo
            currentProducts.add(product)
        }
        
        _uiState.value = _uiState.value.copy(
            products = currentProducts,
            error = null
        )
    }

    fun removeProduct(productId: Int) {
        val currentProducts = _uiState.value.products.toMutableList()
        currentProducts.removeAll { it.productId == productId }
        
        _uiState.value = _uiState.value.copy(
            products = currentProducts,
            error = null
        )
    }

    fun updateProduct(updatedProduct: ITariff) {
        val currentProducts = _uiState.value.products.toMutableList()
        val index = currentProducts.indexOfFirst { it.productId == updatedProduct.productId }
        if (index != -1) {
            currentProducts[index] = updatedProduct
            _uiState.value = _uiState.value.copy(
                products = currentProducts,
                error = null
            )
        }
    }

    // FUNCIONES DE PAGOS EXACTAMENTE IGUAL QUE NoteOfSale
    fun showPaymentDialog(totalAmount: Double) {
        _paymentSummary.value = PaymentSummary(
            totalAmount = totalAmount,
            totalPaid = _payments.value.sumOf { it.amount },
            remaining = totalAmount - _payments.value.sumOf { it.amount }
        )
        _showPaymentDialog.value = true
    }

    fun hidePaymentDialog() {
        _showPaymentDialog.value = false
    }

    fun addPayment(payment: IPayment) {
        val currentPayments = _payments.value.toMutableList()
        currentPayments.add(payment.copy(id = Random.nextInt(1, Int.MAX_VALUE)))
        _payments.value = currentPayments

        // Actualizar resumen
        updatePaymentSummary()

        // Actualizar el estado de la UI
        _uiState.value = _uiState.value.copy(
            payments = _payments.value,
            error = null
        )
    }

    fun removePayment(paymentId: Int) {
        _payments.value = _payments.value.filter { it.id != paymentId }
        updatePaymentSummary()

        // Actualizar el estado de la UI
        _uiState.value = _uiState.value.copy(
            payments = _payments.value,
            error = null
        )
    }

    private fun updatePaymentSummary() {
        val currentSummary = _paymentSummary.value
        val totalPaid = _payments.value.sumOf { it.amount }
        _paymentSummary.value = currentSummary.copy(
            totalPaid = totalPaid,
            remaining = currentSummary.totalAmount - totalPaid
        )
    }

    fun clearPayments() {
        _payments.value = emptyList()
        _paymentSummary.value = PaymentSummary(0.0, 0.0, 0.0)

        // Actualizar el estado de la UI
        _uiState.value = _uiState.value.copy(
            payments = _payments.value,
            error = null
        )
    }

    // FUNCIÓN EXACTAMENTE IGUAL QUE NoteOfSale, solo cambia validación de cliente por proveedor
    fun createPurchase(
        operation: IOperation,
        payments: List<IPayment>,
        onSuccess: (Int, String) -> Unit
    ) {
        // Validaciones igual que NoteOfSale, pero para proveedor
        if (operation.supplier?.names.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Ingrese el nombre del proveedor"
            )
            return
        }
        if (operation.operationDetailSet.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Agregue al menos un producto"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Llamar al repositorio con el nuevo método para compras móviles
                val result = operationRepository.createMobilePurchase(operation, payments)

                // Manejar el resultado igual que NoteOfSale
                result.fold(
                    onSuccess = { pair ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            purchaseResult = Result.success(pair.second)
                        )
                        clearPayments() // Limpiar pagos después del éxito
                        onSuccess(pair.first, pair.second)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            purchaseResult = Result.failure(error),
                            error = error.message ?: "Error al crear la compra"
                        )
                    }
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    purchaseResult = Result.failure(e),
                    error = e.message ?: "Error al crear la compra"
                )
            }
        }
    }

    // Actualizar isFormValid para incluir validación de pagos (igual que NoteOfSale)
    fun isFormValid(): Boolean {
        val state = _uiState.value
        val paymentsValid = if (paymentsEnabled.value) { // ✅ Usar paymentsEnabled en lugar de _paymentsEnabled
            _paymentSummary.value.isComplete
        } else {
            true
        }

        return state.supplier != null &&
                state.products.isNotEmpty() &&
                paymentsValid &&
                // TODO: Agregar validaciones adicionales para serie y correlativo
                true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPurchaseResult() {
        _uiState.value = _uiState.value.copy(purchaseResult = null)
    }

    // Funciones de utilidad
    private fun getCurrentFormattedDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) // ✅ Formato ISO para Django
        return dateFormat.format(java.util.Date())
    }

    private fun getCurrentFormattedTime(): String {
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return timeFormat.format(java.util.Date())
    }
}

// Estados de búsqueda igual que NoteOfSale
sealed class ProductSearchState {
    object Idle : ProductSearchState()
    data class Loading(val query: String) : ProductSearchState()
    data class Success(val products: List<IProductOperation>, val query: String) : ProductSearchState()
    data class Empty(val query: String) : ProductSearchState()
    data class Error(val message: String, val query: String) : ProductSearchState()
}

data class NewPurchaseUiState(
    val supplier: ISupplier? = null,
    val products: List<ITariff> = emptyList(),
    val supplierSearchResults: List<ISupplier> = emptyList(),
    val payments: List<IPayment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val purchaseResult: Result<String>? = null
)