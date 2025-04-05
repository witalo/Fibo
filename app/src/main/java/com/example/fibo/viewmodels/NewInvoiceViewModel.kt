package com.example.fibo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProductTariff
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
    private val operationRepository: OperationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IProductTariff>>(emptyList())
    val searchResults: StateFlow<List<IProductTariff>> = _searchResults.asStateFlow()

    fun fetchClientData(documentNumber: String, onSuccess: (IPerson) -> Unit) {
        if (documentNumber.isBlank()) {
            _error.value = "Ingrese un número de documento válido"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Aquí iría la llamada a tu API GraphQL para obtener los datos del cliente
                // Ejemplo: val clientData = apolloClient.query(GetClientByDocumentQuery(documentNumber)).execute()

                // Por ahora, simulamos con datos de ejemplo
                delay(500) // Simulando retardo de red

                val dummyClient = IPerson(
                    id = 1,
                    names = "Cliente $documentNumber",
                    documentNumber = documentNumber,
                    email = "cliente@example.com",
                    phone = "987654321",
                    address = "Av. Principal 123"
                )

                onSuccess(dummyClient)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error al obtener datos del cliente: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun searchProducts(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Aquí iría la llamada a tu API GraphQL para buscar productos
                // Ejemplo: val result = apolloClient.query(SearchProductsQuery(query)).execute()

                // Por ahora, simulamos con datos de ejemplo
                delay(300) // Simulando retardo de red

                val dummyProducts = listOf(
                    IProductTariff(
                        id = 1001,
                        unitId = 1,
                        unitName = "UND",
                        priceWithIgv = 19.90,
                        priceWithoutIgv = 16.86,
                        quantityMinimum = 1.0,
                        productId = 1,
                        productName = "Laptop Dell Inspiron",
                        typePrice = "NORMAL"
                    ),
                    IProductTariff(
                        id = 1002,
                        unitId = 1,
                        unitName = "UND",
                        priceWithIgv = 9.90,
                        priceWithoutIgv = 8.39,
                        quantityMinimum = 1.0,
                        productId = 2,
                        productName = "Mouse Logitech",
                        typePrice = "NORMAL"
                    ),
                    IProductTariff(
                        id = 1003,
                        unitId = 1,
                        unitName = "UND",
                        priceWithIgv = 24.50,
                        priceWithoutIgv = 20.76,
                        quantityMinimum = 1.0,
                        productId = 3,
                        productName = "Teclado Mecánico",
                        typePrice = "NORMAL"
                    )
                )

                _searchResults.value = dummyProducts.filter {
                    it.productName.contains(query, ignoreCase = true) ||
                            it.productId.toString().contains(query)
                }

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error al buscar productos: ${e.message}"
                _searchResults.value = emptyList()
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
}

//@HiltViewModel
//class NewInvoiceViewModel @Inject constructor(
//    // Aquí inyectamos los repositorios necesarios
//) : ViewModel() {
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//
//    private val _error = MutableStateFlow<String?>(null)
//    val error: StateFlow<String?> = _error.asStateFlow()
//
//    fun createInvoice(invoiceData: InvoiceFormData, param: (Any) -> Unit) {
//        _isLoading.value = true
//
//        viewModelScope.launch {
//            try {
//                // Lógica para crear factura
//
//                _isLoading.value = false
//            } catch (e: Exception) {
//                _error.value = e.message
//                _isLoading.value = false
//            }
//        }
//    }
//}