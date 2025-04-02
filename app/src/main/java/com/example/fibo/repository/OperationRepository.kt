package com.example.fibo.repository

import com.example.fibo.model.IOperation
import com.apollographql.apollo3.ApolloClient
import com.example.fibo.GetInvoicesByDateQuery
import com.example.fibo.model.IPerson
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OperationRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getOperationByDate(date: String): List<IOperation> {
        val response = apolloClient.query(
            GetInvoicesByDateQuery(date = date)
        ).execute()

        return response.data?.operations?.filterNotNull()?.mapNotNull  { o ->
            o?.let {
                IOperation(
                    id = o.id.toInt(),
                    documentType = o.documentType.toString(),
                    emitDate = o.emitDate!!.toString(),
                    serial = o.serial?: "",
                    correlative = o.correlative ?: 0,
                    totalAmount = (o.totalAmount as? Double) ?: 0.0,
                    totalTaxed = (o.totalTaxed as? Double) ?: 0.0,
                    totalDiscount = (o.totalDiscount as? Double) ?: 0.0,
                    totalExonerated = (o.totalExonerated as? Double) ?: 0.0,
                    totalUnaffected = (o.totalUnaffected as? Double) ?: 0.0,
                    totalFree = (o.totalFree as? Double) ?: 0.0,
                    totalIgv = (o.totalIgv as? Double) ?: 0.0,
                    totalToPay = (o.totalToPay as? Double) ?: 0.0,
                    totalPayed = (o.totalPayed as? Double) ?: 0.0,
                    operationStatus = o.operationStatus.toString(),
                    client = o.client?.let {
                        IPerson(
                            id = it.id,
                            names = it.names.orEmpty(),
                            documentNumber = it.documentNumber.orEmpty(),
                            phone = it.phone.orEmpty(),
                            email = it.email.orEmpty()
                        )
                    } ?: IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
                )
            }
        } ?: emptyList()
    }
}