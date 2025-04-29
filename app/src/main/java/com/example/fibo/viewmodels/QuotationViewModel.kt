package com.example.fibo.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.QuotationState
import com.example.fibo.viewmodels.HomeViewModel.InvoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class QuotationViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _quotationState = MutableStateFlow<QuotationState>(QuotationState.WaitingForUser)
    val quotationState: StateFlow<QuotationState> = _quotationState.asStateFlow()
    val userData = preferencesManager.userData
    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData

    init {
        // Cargar cotizaciones cuando se inicializa el ViewModel y hay un usuario válido
        viewModelScope.launch {
            // Observar cambios en userData y cargar cotizaciones cuando tenemos un usuario
            preferencesManager.userData.collect { user ->
                if (user != null) {
                    // Aquí pasamos el ID del usuario junto con la fecha
                    loadQuotation(_selectedDate.value, user.id)
                } else {
                    _quotationState.value = QuotationState.WaitingForUser
                }
            }
        }
    }
    fun loadQuotation(date: String, userId: Int) {
        viewModelScope.launch {
            _quotationState.value = QuotationState.Loading
            val types = listOf("48")
            try {
                val quotation = operationRepository.getOperationsByDateAndUserId(date, userId, types)
                _quotationState.value = QuotationState.Success(quotation)
            } catch (e: Exception) {
                _quotationState.value = QuotationState.Error(
                    e.message ?: "Error al cargar las cotizaciones"
                )
            }
        }
    }
    // Método corregido para pasar también el userId
    fun updateDate(date: String) {
        _selectedDate.value = date
        // Obtenemos el usuario actual
        val user = userData.value

        // Si tenemos un usuario, cargamos las cotizaciones con la nueva fecha
        if (user != null) {
            loadQuotation(date, user.id)
        } else {
            _quotationState.value = QuotationState.WaitingForUser
        }
    }
}