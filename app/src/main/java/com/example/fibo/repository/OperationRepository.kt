package com.example.fibo.repository

import android.util.Log
import com.example.fibo.model.IOperation
import com.apollographql.apollo3.ApolloClient
import com.example.fibo.GetOperationByDateAndUserIdQuery
import com.example.fibo.model.IPerson
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OperationRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getOperationByDate(date: String, userId: Int): List<IOperation> {
        val query = GetOperationByDateAndUserIdQuery(date = date, userId = userId)
        Log.d("italo", "Consulta GraphQL exacta: ${query}")

        val response = apolloClient.query(query).execute()

        // Imprimir respuesta RAW
        Log.d("italo", "Respuesta RAW: ${response.data}")
        Log.d("italo", "Errores: ${response.errors}")
//        val response = apolloClient.query(
//            GetOperationByDateAndUserIdQuery(date = date, userId = userId)
//        ).execute()
//        Log.d("italo", "Response: ${response.data}")

        return response.data?.operationsApp?.filterNotNull()?.mapNotNull  { o ->
            o?.let {
                IOperation(
                    id = o.id.toInt(),
                    documentType = o.documentType.toString(),
                    documentTypeReadable = o.documentTypeReadable.toString(),
                    emitDate = o.emitDate!!.toString(),
                    serial = o.serial?: "",
                    correlative = o.correlative ?: 0,
                    totalAmount = o.totalAmount.toSafeDouble(),
                    totalTaxed = o.totalTaxed.toSafeDouble(),
                    totalDiscount = o.totalDiscount.toSafeDouble(),
                    totalExonerated = o.totalExonerated.toSafeDouble(),
                    totalUnaffected = o.totalUnaffected.toSafeDouble(),
                    totalFree = o.totalFree.toSafeDouble(),
                    totalIgv = o.totalIgv.toSafeDouble(),
                    totalToPay = o.totalToPay.toSafeDouble(),
                    totalPayed = o.totalPayed.toSafeDouble(),
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
    // Funciones de extensión para conversión segura
    fun Any?.toSafeDouble(): Double {
        return when (this) {
            is Double -> this
            is Float -> this.toDouble()
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is String -> this.toDoubleOrNull() ?: 0.0
            is Number -> this.toDouble()
            else -> 0.0
        }
    }
}