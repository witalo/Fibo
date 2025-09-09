package com.example.fibo.model

data class DocumentTypeSummary(
    val documentType: String,
    val documentTypeName: String,
    val count: Int,
    val totalAmount: Double
)

data class MonthlySalesData(
    val currentMonth: List<DocumentTypeSummary>,
    val previousMonth: List<DocumentTypeSummary>,
    val totalCurrentMonth: Double,
    val totalPreviousMonth: Double,
    val growthPercentage: Double
)

data class MonthlyPurchasesData(
    val currentMonth: List<DocumentTypeSummary>,
    val totalCurrentMonth: Double
)

data class TopProduct(
    val productId: Int,
    val productName: String,
    val productCode: String,
    val totalQuantity: Double,
    val totalAmount: Double,
    val percentage: Double
)

data class MonthlyReportData(
    val sales: MonthlySalesData,
    val purchases: MonthlyPurchasesData,
    val topProducts: List<TopProduct>
)

data class MonthlyReportUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val reportData: MonthlyReportData? = null,
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val selectedMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
)
