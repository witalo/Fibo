package com.example.fibo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.QrScanMutation
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.IProductTariff
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
// Extensiones para el ViewModel
@HiltViewModel
class NewInvoiceViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData
    // Estados para la búsqueda de productos
    private val _searchState = MutableStateFlow<ProductSearchState>(ProductSearchState.Idle)
    val searchState: StateFlow<ProductSearchState> = _searchState.asStateFlow()

    private val _selectedProduct = MutableStateFlow<IProduct?>(null)
    val selectedProduct: StateFlow<IProduct?> = _selectedProduct.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IProduct>>(emptyList())
    val searchResults: StateFlow<List<IProduct>> = _searchResults.asStateFlow()

    fun fetchClientData(document: String, onSuccess: (IPerson) -> Unit) {
        if (document.isBlank()) {
            _error.value = "Ingrese un número de documento válido"
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            operationRepository.getSntPerson(document)
                .onSuccess { person ->
                    onSuccess(person)
                    _error.value = null // Limpiar errores previos
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Error al obtener datos"
                }
            _isLoading.value = false
        }
    }

    fun searchProducts(query: String, subsidiaryId: Int) {
        when {
            query.length < 3 -> {
                _searchState.value = ProductSearchState.Idle
                _selectedProduct.value = null
            }
            query == _searchState.value.currentQuery -> return // Ya está buscando esta query
            else -> {
                viewModelScope.launch {
                    _searchState.value = ProductSearchState.Loading(query)
                    try {
//                        val subsidiaryId = subsidiaryData.value?.id ?: 0
                        val results = operationRepository.searchProducts(query, subsidiaryId)

                        _searchState.value = if (results.isEmpty()) {
                            ProductSearchState.Empty(query)
                        } else {
                            ProductSearchState.Success(results, query)
                        }
                    } catch (e: Exception) {
                        _searchState.value = ProductSearchState.Error(
                            message = e.message ?: "Error desconocido",
                            query = query
                        )
                    }
                }
            }
        }
    }
    // Agregar métodos para búsqueda y detalles de producto
    fun searchProductsByQuery(query: String, subsidiaryId: Int) {
        if (query.length < 3) {
            _searchState.value = ProductSearchState.Idle
            return
        }

        _searchState.value = ProductSearchState.Loading(query)
        viewModelScope.launch {
            try {
                // Consulta básica que devuelve sólo id, code, name
                val products = operationRepository.searchProducts(query, subsidiaryId)
                _searchResults.value = products

                _searchState.value = if (products.isEmpty()) {
                    ProductSearchState.Empty(query)
                } else {
                    ProductSearchState.Success(products, query)
                }
            } catch (e: Exception) {
                _searchState.value = ProductSearchState.Error(
                    message = e.message ?: "Error al buscar productos",
                    query = query
                )
            }
        }
    }

    fun fetchProductDetails(productId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Consulta completa que devuelve todos los datos del producto
//                val product = operationRepository.getProductDetails(productId)
//                _selectedProduct.value = product
            } catch (e: Exception) {
                _error.value = "Error al obtener detalles del producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createInvoice(operation: IOperation, onSuccess: (Int) -> Unit) {
        if (operation.client.names.isNullOrBlank()) {
            _error.value = "Ingrese el nombre del cliente"
            return
        }

        if (operation.operationDetailSet.isEmpty()) {
            _error.value = "Agregue al menos un producto"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Llama al repositorio para crear la factura
                val invoiceId = operationRepository.createInvoice(operation)
                onSuccess(invoiceId)
            } catch (e: Exception) {
                _error.value = "Error al crear la factura: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
    fun selectProduct(product: IProduct) {
        _selectedProduct.value = product
    }

    fun clearProductSelection() {
        _selectedProduct.value = null
    }
}
// Definición del estado de búsqueda
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