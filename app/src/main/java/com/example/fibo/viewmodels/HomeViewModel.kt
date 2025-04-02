package com.example.fibo.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.fibo.model.IOperation
import com.example.fibo.repository.OperationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val operationRepository: OperationRepository
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _invoiceState = MutableStateFlow<InvoiceState>(InvoiceState.Loading)
    val invoiceState: StateFlow<InvoiceState> = _invoiceState.asStateFlow()

    init {
        loadInvoices(_selectedDate.value)
    }

    fun updateSelectedDate(date: String) {
        Log.d("italo", "Fecha: $date")
        _selectedDate.value = date
        loadInvoices(date)
    }

    fun loadInvoices(date: String) {
        viewModelScope.launch {
            _invoiceState.value = InvoiceState.Loading
            try {
                val invoices = operationRepository.getOperationByDate(date)
                Log.d("italo", "Lista: $invoices")
                _invoiceState.value = InvoiceState.Success(invoices)
            } catch (e: Exception) {
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