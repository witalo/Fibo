package com.example.fibo.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.fibo.model.IOperation
import com.apollographql.apollo3.ApolloClient
import com.example.fibo.GetOperationByDateAndUserIdQuery
import com.example.fibo.GetTariffByProductIdQuery
import com.example.fibo.SearchProductsQuery
import com.example.fibo.SntPersonMutation
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ITariff
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class OperationRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getOperationByDate(date: String, userId: Int): List<IOperation> {
        val query = GetOperationByDateAndUserIdQuery(date = date, userId = userId)
        val response = apolloClient.query(query).execute()

        return response.data?.operationsApp?.filterNotNull()?.map { o ->
            IOperation(
                id = o.id.toInt(),
                documentType = o.documentType.toString(),
                documentTypeReadable = o.documentTypeReadable.toString(),
                emitDate = o.emitDate!!.toString(),
                serial = o.serial ?: "",
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
        } ?: emptyList()

    }

    // Funciones de extensión para conversión segura
    @SuppressLint("DefaultLocale")
    private fun Any?.toSafeDouble(): Double {
        val value = when (this) {
            is Double -> this
            is Float -> this.toDouble()
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is String -> this.toDoubleOrNull() ?: 0.0
            is Number -> this.toDouble()
            else -> 0.0
        }
        return String.format("%.2f", value).toDouble()
    }

    suspend fun getSntPerson(document: String): Result<IPerson> {
        return runCatching {
            val mutation = SntPersonMutation(document = document)
            val response = apolloClient.mutation(mutation).execute()

            if (response.hasErrors()) {
                val errorMessages =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw RuntimeException("Error en la mutación: $errorMessages")
            }

            val personData = response.data?.sntPerson?.person
            if (personData == null || response.data?.sntPerson?.status != true) {
                val errorMessage =
                    response.data?.sntPerson?.message ?: "No se encontraron datos del cliente"
                throw RuntimeException(errorMessage)
            }

            IPerson(
                id = 0,
                names = personData.sntNames.orEmpty(),
                documentNumber = document,
                phone = "",
                email = "",
                address = personData.sntAddress.orEmpty()
            )
        }
    }

    suspend fun searchProducts(query: String, subsidiaryId: Int): List<IProduct> {
        return try {
            val response = apolloClient.query(
                SearchProductsQuery(query = query, subsidiaryId = subsidiaryId)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al buscar productos: $errorMessage")
            }

            response.data?.searchProduct?.filterNotNull()?.map { product ->
                IProduct(
                    id = product.id!!,
                    code = product.code.orEmpty(),
                    name = product.name.orEmpty(),
                )
            } ?: emptyList()
        } catch (e: Exception) {
            // Log del error si es necesario
            println("Error en searchProducts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTariffByProductID(productId: Int, subsidiaryId: Int): ITariff {
        return try {
            val response = apolloClient.query(
                GetTariffByProductIdQuery(
                    productId = productId,
                    subsidiaryId = subsidiaryId
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessages =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw RuntimeException("Error en la consulta: $errorMessages")
            }

            val tariffData = response.data?.tariffByProductId
                ?: throw RuntimeException("No se encontraron precios del producto")

            ITariff(
                productId = tariffData.productId ?: 0,
                productCode = tariffData.productCode.orEmpty(),
                productName = tariffData.productName.orEmpty(),
                unitId = tariffData.unitId ?: 0,
                unitName = tariffData.unitName.orEmpty(),
                remainingQuantity = tariffData.remainingQuantity ?: 0.0,
                priceWithIgv = tariffData.priceWithIgv ?: 0.0,
                priceWithoutIgv = tariffData.priceWithoutIgv ?: 0.0,
                productTariffId = tariffData.productTariffId ?: 0,
                typeAffectationId = tariffData.typeAffectationId ?: 0
            )
        } catch (e: Exception) {
            println("Error en GetTariffByProductIdQuery: ${e.message}")
            throw e // o puedes retornar un IProduct default si lo prefieres
        }
    }

    suspend fun createInvoice(operation: IOperation): Int {
        // 1. Convertir IOperation a un input de GraphQL (depende de tu esquema)
//        val createInvoiceInput = buildCreateInvoiceInput(operation)
//
//        // 2. Ejecutar la mutación GraphQL
//        val mutation = CreateInvoiceMutation(createInvoiceInput)
//        val response = apolloClient.mutation(mutation).execute()
//
//        // 3. Verificar errores
//        if (response.hasErrors()) {
//            throw Exception("Error al crear factura: ${response.errors?.firstOrNull()?.message}")
//        }
//
//        // 4. Retornar el ID de la factura creada (ajusta según tu esquema GraphQL)
//        return response.data?.createInvoice?.id?.toInt()
//            ?: throw Exception("No se pudo obtener el ID de la factura")
        return 0
    }

//    private fun buildCreateInvoiceInput(operation: IOperation): CreateInvoiceInput {
    // Convierte IOperation al formato que espera tu API GraphQL
//        return CreateInvoiceInput(
//            clientId = operation.client.id,
//            products = operation.operationDetailSet.map { detail ->
//                ProductInput(
//                    id = detail.productId,
//                    quantity = detail.quantity,
//                    unitPrice = detail.unitPrice
//                )
//            }
//            // ... otros campos necesarios
//        )
//    }
}