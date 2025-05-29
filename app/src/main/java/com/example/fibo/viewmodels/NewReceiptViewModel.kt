package com.example.fibo.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ICompany
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ISerialAssigned
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ITariff
import com.example.fibo.model.IUser
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.ProductSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReceiptViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val companyData: StateFlow<ICompany?> = preferencesManager.companyData
    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData
    val userData: StateFlow<IUser?> = preferencesManager.userData
    // Estados para la búsqueda de productos
    private val _searchState = MutableStateFlow<ProductSearchState>(ProductSearchState.Idle)
    val searchState: StateFlow<ProductSearchState> = _searchState.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ITariff?>(null)
    val selectedProduct: StateFlow<ITariff?> = _selectedProduct.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IProduct>>(emptyList())
    val searchResults: StateFlow<List<IProduct>> = _searchResults.asStateFlow()
    // Estado para las series
    private val _serials = MutableStateFlow<List<ISerialAssigned>>(emptyList())
    val serials: StateFlow<List<ISerialAssigned>> = _serials.asStateFlow()
    // Estado para la serie seleccionada
    private val _selectedSerial = MutableStateFlow<ISerialAssigned?>(null)
    val selectedSerial: StateFlow<ISerialAssigned?> = _selectedSerial.asStateFlow()

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

    fun getTariff(productId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Consulta completa que devuelve todos los datos del producto
                val tariff = operationRepository.getTariffByProductID(productId=productId)
                _selectedProduct.value = tariff
            } catch (e: Exception) {
                _error.value = "Error al obtener tarifas del producto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createInvoice(operation: IOperation, onSuccess: (Int, String) -> Unit) {
        if (operation.client.names.isNullOrBlank()) {
            _error.value = "Ingrese el nombre del cliente"
            return
        }
        if (operation.operationDetailSet.isEmpty()) {
            _error.value = "Agregue al menos un producto"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = operationRepository.createInvoice(operation)
                result.fold(
                    onSuccess = { pair ->
                        _isLoading.value = false
                        onSuccess(pair.first, pair.second)
                        Log.e("Italo", "Receipt created successfully")
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        _error.value = error.message ?: "Error al crear la factura"
                        Log.e("Italo", "Error creating receipt", error)
                    }
                )
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "Error al crear la boleta"
                Log.e("Italo", "Exception creating receipt", e)
            }
        }
    }
    // Método para cargar las series
    fun loadSerials(subsidiaryId: Int) {
        if (subsidiaryId == 0) {
            _error.value = "No se ha seleccionado una sucursal válida"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val serials = operationRepository.getAllSerialsByIdSubsidiary(
                    subsidiaryId = subsidiaryId,
                    documentType = "03" // "01" para boletas
                )
                _serials.value = serials
                _selectedSerial.value = serials.firstOrNull()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Error al cargar series: ${e.message}"
                _serials.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para cambiar la serie seleccionada
    fun selectSerial(serial: ISerialAssigned) {
        _selectedSerial.value = serial
    }

    fun clearError() {
        _error.value = null
    }
    fun selectProduct(product: ITariff) {
        _selectedProduct.value = product
    }

    fun clearProductSelection() {
        _selectedProduct.value = null
    }
    fun loadQuotationData(quotationId: Int, callback: (IOperation?) -> Unit) {
        viewModelScope.launch {
            try {
                val result = operationRepository.getOperationById(quotationId)
                callback(result)
            } catch (e: Exception) {
                _error.value = "Error al cargar cotización: ${e.message}"
                callback(null)
            }
        }
    }
}
