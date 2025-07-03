package com.example.fibo.ui.screens.reports

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.ReportState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {



    data class DocumentType(val code: String, val name: String)

    companion object {
        val DOCUMENT_TYPE_CHOICES = listOf(
            DocumentType("01", "FACTURA"),
            DocumentType("03", "BOLETA"),
            DocumentType("07", "NOTA DE CRÉDITO"),
            DocumentType("08", "NOTA DE DÉBITO"),
            DocumentType("NS", "NOTA DE SALIDA")
        )
    }

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Initial)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val _operationDetails = MutableStateFlow<Map<Int, List<IOperationDetail>>>(emptyMap())
    val operationDetails: StateFlow<Map<Int, List<IOperationDetail>>> = _operationDetails.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val _startDate = MutableStateFlow(LocalDate.now().minusDays(7))
    @RequiresApi(Build.VERSION_CODES.O)
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val _endDate = MutableStateFlow(LocalDate.now())
    @RequiresApi(Build.VERSION_CODES.O)
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _selectedDocumentTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocumentTypes: StateFlow<Set<String>> = _selectedDocumentTypes.asStateFlow()

    private val _isFilterDialogOpen = MutableStateFlow(false)
    val isFilterDialogOpen: StateFlow<Boolean> = _isFilterDialogOpen.asStateFlow()

    private val _isLoadingDetails = MutableStateFlow<Set<Int>>(emptySet())
    val isLoadingDetails: StateFlow<Set<Int>> = _isLoadingDetails.asStateFlow()

    init {
        // Esperar a que el usuario esté autenticado antes de cargar
        viewModelScope.launch {
            preferencesManager.userData.collect { userData ->
                if (userData != null) {
                    loadReports()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStartDate(date: LocalDate) {
        _startDate.value = date
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateEndDate(date: LocalDate) {
        _endDate.value = date
    }

    fun toggleDocumentType(documentType: String) {
        val currentTypes = _selectedDocumentTypes.value.toMutableSet()
        if (currentTypes.contains(documentType)) {
            currentTypes.remove(documentType)
        } else {
            currentTypes.add(documentType)
        }
        _selectedDocumentTypes.value = currentTypes
    }

    fun selectAllDocumentTypes() {
        _selectedDocumentTypes.value = DOCUMENT_TYPE_CHOICES.map { it.code }.toSet()
    }

    fun clearDocumentTypeSelection() {
        _selectedDocumentTypes.value = emptySet()
    }

    fun openFilterDialog() {
        _isFilterDialogOpen.value = true
    }

    fun closeFilterDialog() {
        _isFilterDialogOpen.value = false
    }

    fun applyFilters() {
        closeFilterDialog()
        loadReports()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun clearFilters() {
        _selectedDocumentTypes.value = emptySet()
        _startDate.value = LocalDate.now().minusDays(7)
        _endDate.value = LocalDate.now()
    }
    private fun getCurrentUserId(): Int? {
        return preferencesManager.userData.value?.id
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadReports() {
        viewModelScope.launch {
            try {
                _reportState.value = ReportState.Loading

                // Obtener el userId usando la función helper
                val userId = getCurrentUserId() ?: run {
                    _reportState.value = ReportState.Error("Usuario no autenticado")
                    return@launch
                }

                val typesToFilter = if (_selectedDocumentTypes.value.isEmpty()) {
                    DOCUMENT_TYPE_CHOICES.map { it.code }
                } else {
                    _selectedDocumentTypes.value.toList()
                }

                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val operations = operationRepository.getOperationsByDateRange(
                    startDate = _startDate.value.format(dateFormatter),
                    endDate = _endDate.value.format(dateFormatter),
                    userId = userId,
                    types = typesToFilter
                )

                _reportState.value = if (operations.isEmpty()) {
                    ReportState.Empty
                } else {
                    ReportState.Success(operations.sortedByDescending { it.emitDate })
                }

            } catch (e: Exception) {
                _reportState.value = ReportState.Error(
                    e.message ?: "Error al cargar los reportes"
                )
            }
        }
    }

    fun loadOperationDetails(operationId: Int) {
        if (_operationDetails.value.containsKey(operationId)) return

        viewModelScope.launch {
            try {
                _isLoadingDetails.value = _isLoadingDetails.value + operationId

                val operation = operationRepository.getOperationById(operationId)
                val currentDetails = _operationDetails.value.toMutableMap()
                currentDetails[operationId] = operation.operationDetailSet
                _operationDetails.value = currentDetails

            } catch (e: Exception) {
                // Error silencioso, mantener UI estable
            } finally {
                _isLoadingDetails.value = _isLoadingDetails.value - operationId
            }
        }
    }

    fun refreshReports() {
        loadReports()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedDateRange(): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        return "${_startDate.value.format(formatter)} - ${_endDate.value.format(formatter)}"
    }

    fun getSelectedDocumentTypesText(): String {
        return when {
            _selectedDocumentTypes.value.isEmpty() -> "Todos los tipos"
            _selectedDocumentTypes.value.size == DOCUMENT_TYPE_CHOICES.size -> "Todos los tipos"
            _selectedDocumentTypes.value.size == 1 -> {
                val code = _selectedDocumentTypes.value.first()
                DOCUMENT_TYPE_CHOICES.find { it.code == code }?.name ?: code
            }
            _selectedDocumentTypes.value.size <= 3 -> {
                val names = _selectedDocumentTypes.value.mapNotNull { code ->
                    DOCUMENT_TYPE_CHOICES.find { it.code == code }?.name
                }
                names.joinToString(", ")
            }
            else -> "${_selectedDocumentTypes.value.size} tipos seleccionados"
        }
    }

    fun getExportData(): List<Map<String, Any>> {
        val currentState = _reportState.value
        if (currentState !is ReportState.Success) return emptyList()

        return currentState.operations.flatMap { operation ->
            val details = _operationDetails.value[operation.id] ?: emptyList()

            if (details.isEmpty()) {
                listOf(
                    mapOf<String, Any>(
                        "Fecha" to (operation.emitDate ?: ""),
                        "Tipo" to (operation.documentTypeReadable ?: ""),
                        "Serie" to (operation.serial ?: ""),
                        "Número" to operation.correlative,
                        "Cliente" to (operation.client.names ?: ""),
                        "RUC/DNI" to (operation.client.documentNumber ?: ""),
                        "Estado" to (operation.operationStatus ?: ""),
                        "Subtotal" to operation.totalTaxed,
                        "IGV" to operation.totalIgv,
                        "Total" to operation.totalToPay,
                        "Producto" to "",
                        "Cantidad" to "",
                        "Precio Unit." to "",
                        "Importe" to ""
                    )
                )
            } else {
                details.map { detail ->
                    mapOf<String, Any>(
                        "Fecha" to (operation.emitDate ?: ""),
                        "Tipo" to (operation.documentTypeReadable ?: ""),
                        "Serie" to (operation.serial ?: ""),
                        "Número" to operation.correlative,
                        "Cliente" to (operation.client.names ?: ""),
                        "RUC/DNI" to (operation.client.documentNumber ?: ""),
                        "Estado" to (operation.operationStatus ?: ""),
                        "Subtotal" to if (detail == details.first()) operation.totalTaxed else 0.0,
                        "IGV" to if (detail == details.first()) operation.totalIgv else 0.0,
                        "Total" to if (detail == details.first()) operation.totalToPay else 0.0,
                        "Producto" to (detail.tariff.productName ?: ""),
                        "Cantidad" to detail.quantity,
                        "Precio Unit." to detail.unitPrice,
                        "Importe" to detail.totalAmount
                    )
                }
            }
        }
    }
}