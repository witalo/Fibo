package com.example.fibo.ui.screens.reportpayment

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.*
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.repository.OperationRepository
import com.example.fibo.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class ReportPaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    val preferencesManager: PreferencesManager
) : ViewModel() {

    // Estados
    private val _uiState = MutableStateFlow(PaymentReportUiState())
    val uiState: StateFlow<PaymentReportUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val _startDate = MutableStateFlow(LocalDate.now().minusDays(7))
    @RequiresApi(Build.VERSION_CODES.O)
    val startDate: StateFlow<LocalDate> = _startDate.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    private val _endDate = MutableStateFlow(LocalDate.now())
    @RequiresApi(Build.VERSION_CODES.O)
    val endDate: StateFlow<LocalDate> = _endDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Definir los métodos de pago disponibles
    private val paymentMethods = listOf(
        PaymentMethod(1, "EFECTIVO", icon = "💵"),
        PaymentMethod(2, "TARJETA DÉBITO", icon = "💳"),
        PaymentMethod(3, "TARJETA CRÉDITO", icon = "💳"),
        PaymentMethod(4, "TRANSFERENCIA", icon = "🏦"),
        PaymentMethod(5, "GIRO", icon = "📨"),
        PaymentMethod(6, "CHEQUE", icon = "📄"),
        PaymentMethod(7, "CUPÓN", icon = "🎫"),
        PaymentMethod(8, "YAPE", icon = "📱"),
        PaymentMethod(9, "POR PAGAR", icon = "📋", isCredit = true),
        PaymentMethod(10, "OTROS", icon = "📌")
    )

    init {
        // Cargar datos iniciales al iniciar el ViewModel
        loadPaymentReport()
    }

    fun updateStartDate(date: LocalDate) {
        _startDate.value = date
        loadPaymentReport()
    }

    fun updateEndDate(date: LocalDate) {
        _endDate.value = date
        loadPaymentReport()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadPaymentReport() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val subsidiaryId = preferencesManager.subsidiaryData.value?.id ?: 0
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                paymentRepository.getPaymentReport(
                    startDate = _startDate.value.format(dateFormatter),
                    endDate = _endDate.value.format(dateFormatter),
                    subsidiaryId = subsidiaryId
                ).collect { response ->
                    when {
                        response.hasErrors() -> {
                            _uiState.value = _uiState.value.copy(
                                error = response.errors?.firstOrNull()?.message ?: "Error desconocido"
                            )
                        }
                        response.data != null -> {
                            val data = response.data!!.allSalesPayments

                            // Mapear las ventas
                            val salesWithPayments = data?.salesWithPayments?.map { sale ->
                                paymentRepository.mapToSaleWithPayments(sale!!)
                            } ?: emptyList()

                            // Procesar los datos del reporte
                            val paymentSummary = mutableMapOf<Int, PaymentSummaryItem>()

                            // Procesar cada venta y sus pagos
                            salesWithPayments.forEach { sale ->
                                paymentMethods.forEach { method ->
                                    val amount = when(method.id) {
                                        1 -> sale.totalCash
                                        2 -> sale.totalDebitCard
                                        3 -> sale.totalCreditCard
                                        4 -> sale.totalTransfer
                                        5 -> sale.totalMonue
                                        6 -> sale.totalCheck
                                        7 -> sale.totalCoupon
                                        8 -> sale.totalYape
                                        9 -> sale.totalDue
                                        10 -> sale.totalOther
                                        else -> 0.0
                                    }

                                    if (amount > 0) {
                                        val currentSummary = paymentSummary[method.id] ?: PaymentSummaryItem(
                                            paymentMethod = method,
                                            totalAmount = 0.0,
                                            transactionCount = 0,
                                            percentage = 0.0
                                        )

                                        paymentSummary[method.id] = currentSummary.copy(
                                            totalAmount = currentSummary.totalAmount + amount,
                                            transactionCount = currentSummary.transactionCount + 1
                                        )
                                    }
                                }
                            }

                            // Calcular el total general
                            val grandTotal = paymentSummary.values.sumOf { it.totalAmount }

                            // Calcular porcentajes
                            val summaryList = paymentSummary.values.map { item ->
                                item.copy(
                                    percentage = if (grandTotal > 0) (item.totalAmount / grandTotal) * 100 else 0.0
                                )
                            }.sortedByDescending { it.totalAmount }

                            // Preparar datos para el gráfico circular
                            val chartData = summaryList.map {
                                ChartDataItem(
                                    label = it.paymentMethod.name,
                                    value = it.totalAmount.toFloat(),
                                    color = getColorForPaymentMethod(it.paymentMethod.id)
                                )
                            }

                            _uiState.value = PaymentReportUiState(
                                salesWithPayments = salesWithPayments,
                                paymentSummary = summaryList,
                                totalAmount = grandTotal,
                                chartData = chartData,
                                availablePaymentMethods = paymentSummary.keys.map { id ->
                                    paymentMethods.first { it.id == id }
                                }
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                error = "No se recibieron datos del servidor"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error desconocido"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun exportToExcel() {
        viewModelScope.launch {
            try {
                _exportState.value = ExportState.Loading

                val userId = preferencesManager.userData.value?.id ?: run {
                    _exportState.value = ExportState.Error("Usuario no autenticado")
                    return@launch
                }

                val subsidiaryId = preferencesManager.subsidiaryData.value?.id ?: 0
                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val startDateStr = _startDate.value.format(dateFormatter)
                val endDateStr = _endDate.value.format(dateFormatter)

                // Construir URL para descargar el Excel
                val baseUrl = "https://ng.tuf4ctur4.net.pe/logistics"
                val endpoint = "export-sales-payments-excel/"

                val url = "$baseUrl/$endpoint?subsidiary_id=$subsidiaryId&start_date=$startDateStr&end_date=$endDateStr"

                // Nombre del archivo
                val fileName = "Reporte_Pagos_${LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))}.xlsx"

                _exportState.value = ExportState.Success(url, fileName)

            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Error al exportar: ${e.message}")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    private fun getColorForPaymentMethod(methodId: Int): Long {
        return when (methodId) {
            1 -> 0xFF4CAF50 // Verde para efectivo
            2 -> 0xFF2196F3 // Azul para débito
            3 -> 0xFF3F51B5 // Azul oscuro para crédito
            4 -> 0xFF00BCD4 // Cyan para transferencia
            5 -> 0xFF009688 // Teal para giro
            6 -> 0xFF607D8B // Blue Grey para cheque
            7 -> 0xFFFF9800 // Orange para cupón
            8 -> 0xFF9C27B0 // Purple para Yape
            9 -> 0xFFF44336 // Rojo para crédito
            10 -> 0xFF795548 // Brown para otros
            else -> 0xFF9E9E9E // Grey por defecto
        }
    }
}

// Data classes para el estado
data class PaymentReportUiState(
    val salesWithPayments: List<SaleWithPayments> = emptyList(),
    val paymentSummary: List<PaymentSummaryItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val chartData: List<ChartDataItem> = emptyList(),
    val availablePaymentMethods: List<PaymentMethod> = emptyList(),
    val error: String? = null
)

data class SaleWithPayments(
    val id: Int,
    val serial: String,
    val correlative: Int,
    val documentType: String,
    val clientName: String,
    val emitDate: String,
    val totalAmount: Double,
    val totalCash: Double = 0.0,
    val totalDebitCard: Double = 0.0,
    val totalCreditCard: Double = 0.0,
    val totalTransfer: Double = 0.0,
    val totalMonue: Double = 0.0,
    val totalCheck: Double = 0.0,
    val totalCoupon: Double = 0.0,
    val totalYape: Double = 0.0,
    val totalDue: Double = 0.0,
    val totalOther: Double = 0.0
)

data class PaymentSummaryItem(
    val paymentMethod: PaymentMethod,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Double
)

data class PaymentMethod(
    val id: Int,
    val name: String,
    val icon: String = "",
    val isCredit: Boolean = false
)

data class ChartDataItem(
    val label: String,
    val value: Float,
    val color: Long
)

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val downloadUrl: String, val fileName: String) : ExportState()
    data class Error(val message: String) : ExportState()
}