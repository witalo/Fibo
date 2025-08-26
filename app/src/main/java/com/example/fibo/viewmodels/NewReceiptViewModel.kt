package com.example.fibo.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ICompany
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPayment
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProductOperation
import com.example.fibo.model.ISerialAssigned
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ITariff
import com.example.fibo.model.IUser
import com.example.fibo.model.PaymentSummary
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.ProductSearchState
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
class NewReceiptViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    // Esto es para la cotización cuando la cotizacion se convierte en factura
    private val _quotationID = MutableStateFlow<Int?>(null)
    val quotationID: StateFlow<Int?> = _quotationID.asStateFlow()

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

    private val _searchResults = MutableStateFlow<List<IProductOperation>>(emptyList())
    val searchResults: StateFlow<List<IProductOperation>> = _searchResults.asStateFlow()
    // Estado para las series
    private val _serials = MutableStateFlow<List<ISerialAssigned>>(emptyList())
    val serials: StateFlow<List<ISerialAssigned>> = _serials.asStateFlow()
    // Estado para la serie seleccionada
    private val _selectedSerial = MutableStateFlow<ISerialAssigned?>(null)
    val selectedSerial: StateFlow<ISerialAssigned?> = _selectedSerial.asStateFlow()
    val withStock: StateFlow<Boolean> = preferencesManager.companyData
        .map { it?.withStock ?: false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    // NUEVOS ESTADOS PARA PAGOS
    private val _payments = MutableStateFlow<List<IPayment>>(emptyList())
    val payments: StateFlow<List<IPayment>> = _payments.asStateFlow()

    private val _showPaymentDialog = MutableStateFlow(false)
    val showPaymentDialog: StateFlow<Boolean> = _showPaymentDialog.asStateFlow()

    private val _paymentSummary = MutableStateFlow(PaymentSummary(0.0, 0.0, 0.0))
    val paymentSummary: StateFlow<PaymentSummary> = _paymentSummary.asStateFlow()

    // Estado combinado para saber si los pagos están habilitados
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

    // NUEVO: Estado para rastrear si la búsqueda viene del escáner
    private val _isFromBarcodeScan = MutableStateFlow(false)
    val isFromBarcodeScan: StateFlow<Boolean> = _isFromBarcodeScan.asStateFlow()

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
//    fun searchProductsByQuery(query: String, subsidiaryId: Int) {
//        if (query.length < 3) {
//            _searchState.value = ProductSearchState.Idle
//            return
//        }
//
//        _searchState.value = ProductSearchState.Loading(query)
//        viewModelScope.launch {
//            try {
//                // Consulta básica que devuelve sólo id, code, name
//                val products = operationRepository.searchProducts(query, subsidiaryId)
//                _searchResults.value = products
//
//                _searchState.value = if (products.isEmpty()) {
//                    ProductSearchState.Empty(query)
//                } else {
//                    ProductSearchState.Success(products, query)
//                }
//            } catch (e: Exception) {
//                _searchState.value = ProductSearchState.Error(
//                    message = e.message ?: "Error al buscar productos",
//                    query = query
//                )
//            }
//        }
//    }
    fun searchProductsByQuery(query: String, subsidiaryId: Int, isFromScan: Boolean = false) {
        if (query.length < 3) {
            _searchState.value = ProductSearchState.Idle
            return
        }

        // NUEVO: Establecer el origen de la búsqueda
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

                    // NUEVO: Si es del escáner y solo hay un producto, seleccionarlo automáticamente
                    if (isFromScan && products.size == 1) {
                        getTariff(products.first().id)
                    }
                }
            } catch (e: Exception) {
                _searchState.value = ProductSearchState.Error(
                    message = e.message ?: "Error al buscar productos",
                    query = query
                )
            } finally {
                // NUEVO: Resetear el flag después de procesar
                _isFromBarcodeScan.value = false
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

    fun createInvoice(operation: IOperation, payments: List<IPayment>, onSuccess: (Int, String) -> Unit) {
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
                val result = operationRepository.createInvoice(operation, payments)
                result.fold(
                    onSuccess = { pair ->
                        _isLoading.value = false
                        onSuccess(pair.first, pair.second)
                        Log.e("Italo", "Receipt created successfully")
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        _error.value = error.message ?: "Error al crear la boleta"
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
        _quotationID.value = quotationId // Guardar el ID
        viewModelScope.launch {
            try {
                val result = operationRepository.getOperationById(quotationId)
                callback(result)
            } catch (e: Exception) {
                _error.value = "Error al cargar cotización: ${e.message}"
                callback(null)
            }
        }
        // Función para limpiar el quotationId si es necesario
//        fun clearQuotationId() {
//            _quotationID.value = null
//        }
    }
    // NUEVAS FUNCIONES PARA MANEJAR PAGOS

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
    }

    fun removePayment(paymentId: Int) {
        _payments.value = _payments.value.filter { it.id != paymentId }
        updatePaymentSummary()
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
    }
}
