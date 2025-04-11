package com.example.fibo.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.fibo.model.IOperation
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.toInput
import com.example.fibo.CreateOperationMutation
import com.example.fibo.GetOperationByDateAndUserIdQuery
import com.example.fibo.GetTariffByProductIdQuery
import com.example.fibo.SearchProductsQuery
import com.example.fibo.SntPersonMutation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ITariff
import com.example.fibo.type.OperationDetailInput
import com.example.fibo.type.PersonInput
import com.example.fibo.type.TariffInput
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
                subsidiaryId = o.subsidiaryId!!,
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

    suspend fun createInvoice(operation: IOperation): Result<Int> {
        return try {
            // Convert operation details to input type
            val operationDetailInputs = operation.operationDetailSet.map { detail ->
                val tariffInput = TariffInput(
                    productId = Optional.present(detail.tariff.productId),
                    productCode = Optional.present(detail.tariff.productCode),
                    productName = Optional.present(detail.tariff.productName),
                    unitId = Optional.present(detail.tariff.unitId),
                    unitName = Optional.present(detail.tariff.unitName),
                    remainingQuantity = Optional.present(detail.tariff.remainingQuantity),
                    priceWithIgv = Optional.present(detail.tariff.priceWithIgv),
                    priceWithoutIgv = Optional.present(detail.tariff.priceWithoutIgv),
                    productTariffId = Optional.present(detail.tariff.productTariffId),
                    typeAffectationId = Optional.present(detail.tariff.typeAffectationId)
                )
                OperationDetailInput(
                    id = Optional.present(detail.id),
                    tariff = Optional.present(tariffInput),
                    typeAffectationId = Optional.present(detail.typeAffectationId),
                    quantity = Optional.present(detail.quantity),
                    unitValue = Optional.present(detail.unitValue),
                    unitPrice = Optional.present(detail.unitPrice),
                    discountPercentage = Optional.present(detail.discountPercentage),
                    totalDiscount = Optional.present(detail.totalDiscount),
                    perceptionPercentage = Optional.present(detail.perceptionPercentage),
                    totalPerception = Optional.present(detail.totalPerception),
                    igvPercentage = Optional.present(detail.igvPercentage),
                    totalValue = Optional.present(detail.totalValue),
                    totalIgv = Optional.present(detail.totalIgv),
                    totalAmount = Optional.present(detail.totalAmount),
                    totalToPay = Optional.present(detail.totalToPay)
                )
            }

            // Create person input
            val clientInput = PersonInput(
                id = Optional.present(operation.client.id as Int?),
                names = Optional.present(operation.client.names),
                documentNumber = Optional.present(operation.client.documentNumber),
                email = Optional.present(operation.client.email),
                phone = Optional.present(operation.client.phone),
                address = Optional.present(operation.client.address)
            )

            // Execute the mutation
            val response = apolloClient.mutation(
                CreateOperationMutation(
                    id = Optional.present(operation.id),
                    serial = Optional.present(operation.serial),
                    correlative = Optional.present(operation.correlative),
                    documentType = operation.documentType,
                    operationType = operation.operationType,
                    operationStatus = operation.operationStatus,
                    operationAction = operation.operationAction,
                    currencyType = operation.currencyType,
                    operationDate = operation.operationDate,
                    emitDate = operation.emitDate,
                    emitTime = operation.emitTime,
                    userId = operation.userId,
                    subsidiaryId = operation.subsidiaryId,
                    client = clientInput,
                    discountGlobal = Optional.present(operation.discountGlobal),
                    discountPercentageGlobal = Optional.present(operation.discountPercentageGlobal),
                    discountForItem = Optional.present(operation.discountForItem),
                    totalDiscount = Optional.present(operation.totalDiscount),
                    totalTaxed = Optional.present(operation.totalTaxed),
                    totalUnaffected = Optional.present(operation.totalUnaffected),
                    totalExonerated = Optional.present(operation.totalExonerated),
                    totalIgv = Optional.present(operation.totalIgv),
                    totalFree = Optional.present(operation.totalFree),
                    totalAmount = Optional.present(operation.totalAmount),
                    totalToPay = Optional.present(operation.totalToPay),
                    totalPayed = Optional.present(operation.totalPayed),
                    operationDetailSet = operationDetailInputs
                )
            ).execute()

            val data = response.data
            if (data != null && data.createOperation?.success == true) {
                val operationIdStr = data.createOperation.operation?.id
                val operationIdInt = operationIdStr?.toIntOrNull() ?: -1
                Result.success(operationIdInt)
            } else {
                Result.failure(Exception(data?.createOperation?.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun createInvoice(operation: IOperation, callback: (Int) -> Unit) {
//        try {
//            // 1. Preparar los detalles de la operación
//            val operationDetailsInput = operation.operationDetailSet.map { detail ->
//                mapOf(
//                    "type_affectation_id" to detail.typeAffectationId,
//                    "quantity" to detail.quantity,
//                    "unit_value" to detail.unitValue,
//                    "unit_price" to detail.unitPrice,
//                    "total_discount" to detail.totalDiscount,
//                    "discount_percentage" to detail.discountPercentage,
//                    "igv_percentage" to detail.igvPercentage,
//                    "total_value" to detail.totalValue,
//                    "total_igv" to detail.totalIgv,
//                    "total_amount" to detail.totalAmount,
//                    "total_to_pay" to detail.totalToPay,
//                    "tariff" to mapOf(
//                        "product_tariff_id" to detail.tariff.productTariffId,
//                        "product_id" to detail.tariff.productId,
//                        "product_code" to detail.tariff.productCode,
//                        "product_name" to detail.tariff.productName,
//                        "unit_id" to detail.tariff.unitId,
//                        "unit_name" to detail.tariff.unitName,
//                        "price_with_igv" to detail.tariff.priceWithIgv,
//                        "price_without_igv" to detail.tariff.priceWithoutIgv,
//                        "type_affectation_id" to detail.tariff.typeAffectationId
//                    )
//                )
//            }
//
//            // 2. Preparar el cliente
//            val clientInput = mapOf(
//                "id" to operation.client.id,
//                "document_number" to operation.client.documentNumber,
//                "names" to operation.client.names,
//                "email" to operation.client.email,
//                "phone" to operation.client.phone,
//                "address" to operation.client.address,
//            )
//
//            // 3. Definir la mutación GraphQL
//            val mutation = CreateOperationMutation(
//                id = operation.id,
//                serial = operation.serial ?: "",
//                correlative = operation.correlative,
//                documentType = operation.documentType,
//                operationType = operation.operationType,
//                operationStatus = operation.operationStatus,
//                operationAction = operation.operationAction,
//                currencyType = operation.currencyType,
//                operationDate = operation.operationDate,
//                emitDate = operation.emitDate,
//                emitTime = operation.emitTime,
//                userId = operation.userId,
//                subsidiaryId = operation.subsidiaryId,
//                client = PersonInput(
//                    documentNumber = operation.client.documentNumber,
//                    names = operation.client.names,
//                    address = operation.client.address
//                ),
//                discountGlobal = operation.discountGlobal,
//                discountPercentageGlobal = operation.discountPercentageGlobal,
//                discountForItem = operation.discountForItem,
//                totalDiscount = operation.totalDiscount,
//                totalTaxed = operation.totalTaxed,
//                totalUnaffected = operation.totalUnaffected,
//                totalExonerated = operation.totalExonerated,
//                totalIgv = operation.totalIgv,
//                totalFree = operation.totalFree,
//                totalAmount = operation.totalAmount,
//                totalToPay = operation.totalToPay,
//                totalPayed = operation.totalPayed,
//                operationDetailSet = operationDetailsInput.map { detail ->
//                    OperationDetailInput(
//                        typeAffectationId = detail["type_affectation_id"] as Int,
//                        quantity = detail["quantity"] as Double,
//                        unitValue = detail["unit_value"] as Double,
//                        unitPrice = detail["unit_price"] as Double,
//                        totalDiscount = detail["total_discount"] as Double,
//                        discountPercentage = detail["discount_percentage"] as Double,
//                        igvPercentage = detail["igv_percentage"] as Double,
//                        totalValue = detail["total_value"] as Double,
//                        totalIgv = detail["total_igv"] as Double,
//                        totalAmount = detail["total_amount"] as Double,
//                        totalToPay = detail["total_to_pay"] as Double,
//                        tariff = TariffInput(
//                            productId = (detail["tariff"] as Map<String, Any>)["product_id"] as Int,
//                            productCode = (detail["tariff"] as Map<String, Any>)["product_code"] as? String,
//                            productName = (detail["tariff"] as Map<String, Any>)["product_name"] as? String,
//                            unitId = (detail["tariff"] as Map<String, Any>)["unit_id"] as? Int,
//                            unitName = (detail["tariff"] as Map<String, Any>)["unit_name"] as? String,
//                            priceWithIgv = (detail["tariff"] as Map<String, Any>)["price_with_igv"] as Double,
//                            priceWithoutIgv = (detail["tariff"] as Map<String, Any>)["price_without_igv"] as Double,
//                            typeAffectationId = (detail["tariff"] as Map<String, Any>)["type_affectation_id"] as Int
//                        )
//                    )
//                }
//            )
//
//            // 4. Ejecutar la mutación
//            val response = apolloClient.mutation(mutation).execute()
//
//            if (response.hasErrors()) {
//                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
//                throw Exception("Error al crear factura: $errorMessage")
//            }
//
//            // 5. Obtener el resultado
//            val operationId = response.data?.createOperation?.operation?.id?.toInt()
//                ?: throw Exception("No se pudo obtener el ID de la operación creada")
//
//            // 6. Llamar al callback con el ID de la operación
//            callback(operationId)
//
//        } catch (e: Exception) {
//            // Manejar errores y llamar al callback con 0 (o podrías usar un sealed class/resultado)
//            println("Error al crear factura: ${e.message}")
//            callback(0)
//        }
//    }

}