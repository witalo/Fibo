package com.example.fibo.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ISupplier
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.PurchaseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _subsidiaryData = MutableStateFlow<ISubsidiary?>(null)
    val subsidiaryData: StateFlow<ISubsidiary?> = _subsidiaryData.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    @RequiresApi(Build.VERSION_CODES.O)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Loading)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    // Estados para búsqueda de proveedores
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ISupplier>>(emptyList())
    val searchResults: StateFlow<List<ISupplier>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<ISupplier?>(null)
    val selectedSupplier: StateFlow<ISupplier?> = _selectedSupplier.asStateFlow()

    // Estados para diálogos
    private val _showCancelDialog = MutableStateFlow(false)
    val showCancelDialog: StateFlow<Boolean> = _showCancelDialog.asStateFlow()

    private val _currentOperationId = MutableStateFlow(0)
    val currentOperationId: StateFlow<Int> = _currentOperationId.asStateFlow()

    init {
        loadSubsidiaryData()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadSubsidiaryData() {
        viewModelScope.launch {
            try {
                val subsidiary = preferencesManager.subsidiaryData.value
                _subsidiaryData.value = subsidiary
                if (subsidiary != null) {
                    loadPurchases(_selectedDate.value)
                }
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error loading subsidiary data", e)
                _purchaseState.value = PurchaseState.Error("Error al cargar datos de la empresa")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateSelectedDate(newDate: String) {
        _selectedDate.value = newDate
        loadPurchases(newDate)
    }

    fun loadPurchases(date: String) {
        viewModelScope.launch {
            try {
                _purchaseState.value = PurchaseState.Loading
                
                val subsidiary = _subsidiaryData.value
                if (subsidiary == null) {
                    _purchaseState.value = PurchaseState.WaitingForUser
                    return@launch
                }

                val supplierId = _selectedSupplier.value?.id ?: 0

                val purchases = operationRepository.getPurchases(
                    subsidiaryId = subsidiary.id,
                    supplierId = supplierId,
                    startDate = date,
                    endDate = date,
                    documentType = "NA", // Todos los tipos por defecto
                    page = 1,
                    pageSize = 50
                )

                _purchaseState.value = PurchaseState.Success(purchases)
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error loading purchases", e)
                _purchaseState.value = PurchaseState.Error("Error al cargar compras: ${e.localizedMessage}")
            }
        }
    }

    fun searchSuppliers(query: String) {
        _searchQuery.value = query
        
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _isSearching.value = true
                val suppliers = operationRepository.searchSuppliers(query)
                _searchResults.value = suppliers
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error searching suppliers", e)
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun selectSupplier(supplier: ISupplier) {
        _selectedSupplier.value = supplier
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        // Recargar compras con el proveedor seleccionado
        loadPurchases(_selectedDate.value)
    }

    fun clearSupplierSearch() {
        _selectedSupplier.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        // Recargar compras sin filtro de proveedor
        loadPurchases(_selectedDate.value)
    }

    fun showCancelDialog(operationId: Int) {
        _currentOperationId.value = operationId
        _showCancelDialog.value = true
    }

    fun closeCancelDialog() {
        _showCancelDialog.value = false
        _currentOperationId.value = 0
    }

    fun cancelOperation(operationId: Int, operationType: String, emitDate: String) {
        viewModelScope.launch {
            try {
                closeCancelDialog()
                
                // Verificar si la operación puede ser anulada (dentro del período permitido)
                val canCancel = when (operationType) {
                    "01" -> isWithinDays(emitDate, 3) // Facturas: 3 días
                    "03" -> isWithinDays(emitDate, 5) // Boletas: 5 días
                    else -> false
                }
                
                if (!canCancel) {
                    _purchaseState.value = PurchaseState.Error("El documento no puede ser anulado fuera del período permitido")
                    return@launch
                }

                val success = operationRepository.cancelOperation(operationId)
                if (success) {
                    // Recargar las compras después de cancelar
                    loadPurchases(_selectedDate.value)
                } else {
                    _purchaseState.value = PurchaseState.Error("Error al anular la compra")
                }
            } catch (e: Exception) {
                Log.e("PurchaseViewModel", "Error canceling operation", e)
                _purchaseState.value = PurchaseState.Error("Error al anular: ${e.localizedMessage}")
            }
        }
    }

    private fun isWithinDays(emitDate: String, maxDays: Int): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val emitDateObj = dateFormat.parse(emitDate) ?: return false
            val currentDate = Date()

            val diffInMillis = currentDate.time - emitDateObj.time
            val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

            diffInDays <= maxDays
        } catch (e: Exception) {
            false
        }
    }
}