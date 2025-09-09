package com.example.fibo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.*
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonthlyReportViewModel @Inject constructor(
    private val operationRepository: OperationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyReportUiState())
    val uiState: StateFlow<MonthlyReportUiState> = _uiState.asStateFlow()

    fun loadMonthlyReport(subsidiaryId: Int, year: Int, month: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Cargar datos de ventas
                val salesData = operationRepository.getMonthlySalesReport(subsidiaryId, year, month)
                
                // Cargar datos de compras
                val purchasesData = operationRepository.getMonthlyPurchasesReport(subsidiaryId, year, month)
                
                // Cargar top productos
                val topProducts = operationRepository.getTopProductsReport(subsidiaryId, year, month, 10)

                val reportData = MonthlyReportData(
                    sales = salesData,
                    purchases = purchasesData,
                    topProducts = topProducts
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    reportData = reportData
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar el reporte"
                )
            }
        }
    }

    fun updateSelectedDate(year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(
            selectedYear = year,
            selectedMonth = month
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    suspend fun exportToExcel(subsidiaryId: Int): ByteArray? {
        return try {
            operationRepository.exportMonthlyReportToExcel(
                subsidiaryId = subsidiaryId,
                year = uiState.value.selectedYear,
                month = uiState.value.selectedMonth
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Error al exportar Excel: ${e.message}"
            )
            null
        }
    }
}
