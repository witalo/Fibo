package com.example.fibo.repository

import android.annotation.SuppressLint
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.example.fibo.AllSalesPaymentsQuery
import com.example.fibo.ui.screens.reportpayment.SaleWithPayments
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getPaymentReport(
        startDate: String,
        endDate: String,
        subsidiaryId: Int
    ): Flow<ApolloResponse<AllSalesPaymentsQuery.Data>> {
        return apolloClient.query(
            AllSalesPaymentsQuery(
                startDate = startDate,
                endDate = endDate,
                subsidiaryId = subsidiaryId
            )
        ).toFlow()
    }

    fun mapToSaleWithPayments(
        sale: AllSalesPaymentsQuery.SalesWithPayment
    ): SaleWithPayments {
        return SaleWithPayments(
            id = sale.id.toInt(),
            serial = sale.serial.toString(),
            correlative = sale.correlative?:0,
            documentType = sale.documentType.toString(),
            clientName = sale.client?.names ?: "Cliente General",
            emitDate = sale.emitDate.toString(),
            totalAmount = sale.totalAmount.toSafeDouble(),
            totalCash = sale.totalCash ?: 0.0,
            totalDebitCard = sale.totalDebitCard ?: 0.0,
            totalCreditCard = sale.totalCreditCard ?: 0.0,
            totalTransfer = sale.totalTransfer ?: 0.0,
            totalMonue = sale.totalMonue ?: 0.0,
            totalCheck = sale.totalCheck ?: 0.0,
            totalCoupon = sale.totalCoupon ?: 0.0,
            totalYape = sale.totalYape ?: 0.0,
            totalDue = sale.totalDue ?: 0.0,
            totalOther = sale.totalOther ?: 0.0
        )
    }
    @SuppressLint("DefaultLocale")
    private fun Any?.toSafeDouble(): Double {
        val value = when (this) {
            is Double -> this
            is Float -> this.toDouble()
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is String -> {
                // Reemplaza coma por punto para manejar formato "264,00"
                val cleaned = this.replace(",", ".")
                cleaned.toDoubleOrNull() ?: 0.0
            }

            is Number -> this.toDouble()
            else -> 0.0
        }
        return String.format("%.2f", value).toDouble()
    }
}


data class PaymentReportData(
    val salesWithPayments: List<SaleWithPayments>,
    val totalCash: Double,
    val totalDebitCard: Double,
    val totalCreditCard: Double,
    val totalTransfer: Double,
    val totalMonue: Double,
    val totalCheck: Double,
    val totalCoupon: Double,
    val totalYape: Double,
    val totalDue: Double,
    val totalOther: Double
)