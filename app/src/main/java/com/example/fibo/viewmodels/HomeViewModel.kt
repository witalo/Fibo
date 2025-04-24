package com.example.fibo.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.fibo.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _invoiceState = MutableStateFlow<InvoiceState>(InvoiceState.Loading)
    val invoiceState: StateFlow<InvoiceState> = _invoiceState.asStateFlow()

    // Store the userId when available
    private var currentUserId: Int? = null
    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData

    // Estado para el diálogo de PDF
    private val _showPdfDialog = MutableStateFlow(false)
    val showPdfDialog: StateFlow<Boolean> = _showPdfDialog
    private val _currentInvoiceId = MutableStateFlow(0)
    val currentInvoiceId: StateFlow<Int> = _currentInvoiceId

    // Añade esto para manejar eventos de refresco
    private val _refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun triggerRefresh() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
        }
    }

    init {
        // 1. Collector para datos de usuario
        viewModelScope.launch {
            preferencesManager.userData.collect { user ->
                currentUserId = user?.id
                user?.id?.let {
                    // Carga facturas cuando tenemos un usuario válido
                    loadInvoices(_selectedDate.value)
                }
            }
        }

        // 2. Collector independiente para eventos de refresco
        viewModelScope.launch {
            _refreshTrigger.collect {
                currentUserId?.let {
                    loadInvoices(_selectedDate.value)
                }
            }
        }
    }

    fun updateSelectedDate(date: String) {
        Log.d("italo", "Fecha: $date")
        _selectedDate.value = date
        loadInvoices(date)
    }

    fun loadInvoices(date: String) {
        viewModelScope.launch {
            _invoiceState.value = InvoiceState.Loading

            val userId = currentUserId
            if (userId == null) {
                _invoiceState.value = InvoiceState.Error("Usuario no autenticado")
                return@launch
            }

            try {
                Log.d("italo", "Cargando operaciones para userId: $userId y fecha: $date")
                val invoices = operationRepository.getOperationByDate(date, userId)
                Log.d("italo", "Operaciones cargadas: $invoices")
                _invoiceState.value = InvoiceState.Success(invoices)
            } catch (e: Exception) {
                Log.e("italo", "Error al cargar operaciones", e)
                _invoiceState.value = InvoiceState.Error(
                    e.message ?: "Error al cargar las operaciones"
                )
            }
        }
    }

    sealed class InvoiceState {
        object Loading : InvoiceState()
        data class Success(val data: List<IOperation>) : InvoiceState()
        data class Error(val message: String) : InvoiceState()
    }
    // Función para mostrar el diálogo de PDF
    fun showPdfDialog(invoiceId: Int) {
        _currentInvoiceId.value = invoiceId
        _showPdfDialog.value = true
    }

    // Función para cerrar el diálogo
    fun closePdfDialog() {
        _showPdfDialog.value = false
    }
}
//@HiltViewModel
//class HomeViewModel @Inject constructor(
//    private val invoiceRepository: OperationRepository
//) : ViewModel() {
//
//    private val _invoiceState = MutableStateFlow<InvoiceState>(InvoiceState.Loading)
//    val invoiceState: StateFlow<InvoiceState> = _invoiceState.asStateFlow()
//
//    private val _selectedDate = MutableStateFlow(
//        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//    )
//    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
//
//    init {
//        loadInvoices(_selectedDate.value)
//    }
//
//    fun updateSelectedDate(date: String) {
//        _selectedDate.value = date
//        loadInvoices(date)
//    }
//
//    fun loadInvoices(date: String) {
//        viewModelScope.launch {
//            _invoiceState.value = InvoiceState.Loading
//            try {
//                val invoices = invoiceRepository.getInvoicesByDate(date)
//                _invoiceState.value = InvoiceState.Success(invoices)
//            } catch (e: Exception) {
//                _invoiceState.value = InvoiceState.Error(e.message ?: "Error desconocido")
//            }
//        }
//    }
//
//    sealed class InvoiceState {
//        object Loading : InvoiceState()
//        data class Success(val data: List<IOperation>) : InvoiceState()
//        data class Error(val message: String) : InvoiceState()
//    }
//}