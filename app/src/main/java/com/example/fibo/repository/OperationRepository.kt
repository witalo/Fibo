package com.example.fibo.repository

import android.annotation.SuppressLint
import android.util.Log
import com.example.fibo.model.IOperation
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.CreateOperationMutation
import com.example.fibo.GetAllSerialsByIdQuery
import com.example.fibo.GetOperationByDateAndUserIdQuery
import com.example.fibo.GetTariffByProductIdQuery
import com.example.fibo.SearchProductsQuery
import com.example.fibo.SntPersonMutation
import com.example.fibo.GetOperationByIdQuery
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ISerialAssigned
import com.example.fibo.model.ITariff
import com.example.fibo.type.OperationDetailInput
import com.example.fibo.type.PersonInput
import com.example.fibo.type.TariffInput
import javax.inject.Inject
import javax.inject.Singleton
import com.example.fibo.CancelInvoiceMutation
import com.example.fibo.GetAllProductsBySubsidiaryIdQuery
import com.example.fibo.GetOperationsByDateAndUserIdQuery
import com.example.fibo.GetOperationsByDateRangeQuery
import com.example.fibo.GetOperationsByPersonAndUserQuery
import com.example.fibo.SearchPersonsQuery
import com.example.fibo.model.IPayment
import com.example.fibo.type.PaymentInput


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
                        id = it.id.toInt(),
                        names = it.names.orEmpty(),
                        documentNumber = it.documentNumber.orEmpty(),
                        phone = it.phone.orEmpty(),
                        email = it.email.orEmpty()
                    )
                } ?: IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
            )
        } ?: emptyList()

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
                    stock = product.stock ?: 0.0
                )
            } ?: emptyList()
        } catch (e: Exception) {
            // Log del error si es necesario
            println("Error en searchProducts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTariffByProductID(productId: Int): ITariff {
        return try {
            val response = apolloClient.query(
                GetTariffByProductIdQuery(
                    productId = productId
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
                stock = tariffData.stock ?: 0.0,
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

    suspend fun createInvoice(operation: IOperation, payments: List<IPayment> = emptyList()): Result<Pair<Int, String>>{
        return try {
            // Convert operation details to input type (código existente)
            val operationDetailInputs = operation.operationDetailSet.map { detail ->
                val tariffInput = TariffInput(
                    productId = Optional.present(detail.tariff.productId),
                    productCode = Optional.present(detail.tariff.productCode),
                    productName = Optional.present(detail.tariff.productName),
                    unitId = Optional.present(detail.tariff.unitId),
                    unitName = Optional.present(detail.tariff.unitName),
                    stock = Optional.present(detail.tariff.stock),
                    priceWithIgv = Optional.present(detail.tariff.priceWithIgv),
                    priceWithoutIgv = Optional.present(detail.tariff.priceWithoutIgv),
                    productTariffId = Optional.present(detail.tariff.productTariffId),
                    typeAffectationId = Optional.present(detail.tariff.typeAffectationId)
                )
                OperationDetailInput(
                    id = Optional.present(detail.id),
                    tariff = Optional.present(tariffInput),
                    description = Optional.present(detail.description),
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

            // Create person input (código existente)
            val clientInput = PersonInput(
                id = Optional.present(operation.client.id as Int?),
                names = Optional.present(operation.client.names),
                documentType = Optional.present(operation.client.documentType),
                documentNumber = Optional.present(operation.client.documentNumber),
                email = Optional.present(operation.client.email),
                phone = Optional.present(operation.client.phone),
                address = Optional.present(operation.client.address)
            )

            // NUEVO: Convert payments to input type
            val paymentInputs = payments.map { payment ->
                PaymentInput(
                    wayPay = payment.wayPay,
                    amount = payment.amount,
                    note = Optional.present(payment.note),
                    paymentDate = payment.paymentDate // NUEVO: Incluir fecha
                )
            }

            // Execute the mutation con pagos
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
                    operationDetailSet = operationDetailInputs,
                    payments = Optional.present(paymentInputs) // NUEVO: Agregar pagos
                )
            ).execute()

            val data = response.data
            if (data != null && data.createOperation?.success == true) {
                val operationIdStr = data.createOperation.operation?.id
                val operationIdInt = operationIdStr?.toIntOrNull() ?: -1
                val serial = data.createOperation.operation?.serial ?: ""
                val correlative = data.createOperation.operation?.correlative ?: 0

                val successMessage = if (serial.isNotEmpty()) {
                    "${data.createOperation.message ?: "Comprobante creada exitosamente"} (${serial}-${correlative})"
                } else {
                    data.createOperation.message ?: "Comprobante creada exitosamente"
                }
                Result.success(Pair(operationIdInt, successMessage))
            } else {
                val errorMessage = data?.createOperation?.message
                    ?: response.errors?.firstOrNull()?.message
                    ?: "Error desconocido al crear el comprobante"

                Log.e("CreateOperation", "Error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("CreateOperation", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getOperationById(operationId: Int): IOperation {
        val query = GetOperationByIdQuery(OperationId = operationId)
        val response = apolloClient.query(query).execute()
        // Validar errores de la respuesta
        if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Error desconocido"
            throw ApolloException("Error en la consulta GraphQL: $errorMessage")
        }
        val operationData = response.data?.operationById
            ?: throw ApolloException("No se encontró la operación con ID $operationId")

        val clientData = operationData.client
            ?: throw ApolloException("Los datos del cliente están vacíos")

        // Mapear datos a modelos locales
        val client = IPerson(
            id = clientData.id.toInt(),
            names = clientData.names,
            documentType = try {
                clientData.documentType.toString()
            } catch (e: Exception) {
                // Si hay un error al convertir el enum, usar el valor directamente como string
                clientData.documentType?.toString() ?: ""
            },
            documentNumber = clientData.documentNumber,
            email = clientData.email,
            phone = clientData.phone,
            address = clientData.address
        )

        val details = operationData.operationdetailSet.map { detail ->
            IOperationDetail(
                id = detail.id!!,
                tariff = ITariff(
                    productTariffId = detail.productTariff?.id!!.toInt(),
                    productId = detail.productTariff.productId!!.toInt(),
                    productCode = detail.productTariff.productCode.toString(),
                    productName = detail.productTariff.productName.toString(),
                    unitId = detail.productTariff.unitId!!.toInt(),
                    unitName = detail.productTariff.unitName.toString(),
                    typeAffectationId = detail.typeAffectationId!!.toInt()
                ),
                typeAffectationId = detail.typeAffectationId,
                description = detail.description.toString(),
                quantity = detail.quantity.toSafeDouble(),
                unitValue = detail.unitValue.toSafeDouble(),
                unitPrice = detail.unitPrice.toSafeDouble(),
                discountPercentage = detail.discountPercentage.toSafeDouble(),
                totalDiscount = detail.totalDiscount.toSafeDouble(),
                perceptionPercentage = detail.perceptionPercentage.toSafeDouble(),
                totalPerception = detail.totalPerception.toSafeDouble(),
                igvPercentage = detail.igvPercentage.toSafeDouble(),
                totalValue = detail.totalValue.toSafeDouble(),
                totalIgv = detail.totalIgv.toSafeDouble(),
                totalAmount = detail.totalAmount.toSafeDouble(),
                totalToPay = detail.totalToPay.toSafeDouble()
            )
        }

        val operation = IOperation(
            id = operationData.id.toInt(),
            serial = operationData.serial ?: "",
            correlative = operationData.correlative ?: 0,
            operationType = operationData.operationType.toString(),
            documentTypeReadable = operationData.documentTypeReadable.toString(),
            operationStatus = operationData.operationStatus.toString(),
            operationAction = operationData.operationAction.toString(),
            documentType = operationData.documentType.toString(),
            currencyType = operationData.currencyType.toString(),
            operationDate = operationData.operationDate.toString(),
            emitDate = operationData.emitDate.toString(),
            emitTime = operationData.emitTime.toString(),
            userId = operationData.userId!!,
            client = client,
            subsidiaryId = operationData.subsidiaryId!!,
            discountGlobal = operationData.discountGlobal.toSafeDouble(),
            discountPercentageGlobal = operationData.discountPercentageGlobal.toSafeDouble(),
            discountForItem = operationData.discountForItem.toSafeDouble(),
            totalDiscount = operationData.totalDiscount.toSafeDouble(),
            totalTaxed = operationData.totalTaxed.toSafeDouble(),
            totalUnaffected = operationData.totalUnaffected.toSafeDouble(),
            totalExonerated = operationData.totalExonerated.toSafeDouble(),
            totalIgv = operationData.totalIgv.toSafeDouble(),
            totalFree = operationData.totalFree.toSafeDouble(),
            totalAmount = operationData.totalAmount.toSafeDouble(),
            totalToPay = operationData.totalToPay.toSafeDouble(),
            totalPayed = operationData.totalPayed.toSafeDouble(),
            operationDetailSet = details
        )
        return operation
    }

    suspend fun getAllSerialsByIdSubsidiary(
        subsidiaryId: Int,
        documentType: String
    ): List<ISerialAssigned> {
        return try {
            val response = apolloClient.query(
                GetAllSerialsByIdQuery(subsidiaryId = subsidiaryId, documentType = documentType)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error no se encontraron series: $errorMessage")
            }

            response.data?.allSerialsByType?.filterNotNull()?.map { sa ->
                ISerialAssigned(
                    id = sa.id!!,
                    serial = sa.serial.orEmpty()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            println("Error en series: ${e.message}")
            emptyList()
        }
    }

    suspend fun cancelInvoice(operationId: Int, date: String): Result<String> {
        return try {
            // Execute the mutation
            val mutation = CancelInvoiceMutation(operationId = operationId, lowDate = date)
            val response = apolloClient.mutation(mutation).execute()

            val data = response.data
            if (data != null && data.cancelInvoice?.success == true) {
                val successMessage =
                    data.cancelInvoice.message ?: "Comprobante anulado exitosamente"
                Result.success(successMessage)
            } else {
                Result.failure(
                    Exception(
                        data?.cancelInvoice?.message ?: "Error desconocido al anular el comprobante"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOperationsByDateAndUserId(
        date: String,
        userId: Int,
        types: List<String>
    ): List<IOperation> {
        val query = GetOperationsByDateAndUserIdQuery(
            date = date,
            userId = userId,
            types = Optional.Present(types)
        )
        val response = apolloClient.query(query).execute()

        return response.data?.operationsByDateAndUserId?.filterNotNull()?.map { o ->
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
                        id = it.id.toInt(),
                        names = it.names.orEmpty(),
                        documentNumber = it.documentNumber.orEmpty(),
                        phone = it.phone.orEmpty(),
                        email = it.email.orEmpty()
                    )
                } ?: IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
            )
        } ?: emptyList()

    }

    suspend fun searchPersons(query: String): List<IPerson> {
        return try {
            val response = apolloClient.query(
                SearchPersonsQuery(query = query)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al buscar clientes: $errorMessage")
            }

            response.data?.searchPersons?.filterNotNull()?.map { person ->
                IPerson(
                    id = person.id.toInt(),
                    names = person.names,
                    documentType = person.documentType?.toString(),
                    documentNumber = person.documentNumber,
                    phone = person.phone,
                    email = person.email,
                    address = person.address
                )
            } ?: emptyList()
        } catch (e: Exception) {
            println("Error en searchPersons: ${e.message}")
            emptyList()
        }
    }

    suspend fun getOperationsByPersonAndUser(
        personId: Int,
        userId: Int,
        types: List<String>
    ): List<IOperation> {
        return try {
            val response = apolloClient.query(
                GetOperationsByPersonAndUserQuery(
                    personId = personId,
                    userId = userId,
                    types = Optional.Present(types)
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al buscar operaciones: $errorMessage")
            }

            response.data?.operationsByPersonAndUser?.filterNotNull()?.map { operation ->
                IOperation(
                    id = operation.id.toInt(),
                    documentType = operation.documentType.toString(),
                    documentTypeReadable = operation.documentTypeReadable.toString(),
                    emitDate = operation.emitDate.toString(),
                    serial = operation.serial ?: "",
                    correlative = operation.correlative ?: 0,
                    totalAmount = operation.totalAmount.toSafeDouble(),
                    totalTaxed = operation.totalTaxed.toSafeDouble(),
                    totalDiscount = operation.totalDiscount.toSafeDouble(),
                    totalExonerated = operation.totalExonerated.toSafeDouble(),
                    totalUnaffected = operation.totalUnaffected.toSafeDouble(),
                    totalFree = operation.totalFree.toSafeDouble(),
                    totalIgv = operation.totalIgv.toSafeDouble(),
                    totalToPay = operation.totalToPay.toSafeDouble(),
                    totalPayed = operation.totalPayed.toSafeDouble(),
                    operationStatus = operation.operationStatus.toString(),
                    subsidiaryId = operation.subsidiaryId!!,
                    client = operation.client?.let { client ->
                        IPerson(
                            id = client.id.toInt(),
                            names = client.names,
                            documentNumber = client.documentNumber,
                            phone = client.phone,
                            email = client.email
                        )
                    } ?: IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
                )
            } ?: emptyList()
        } catch (e: Exception) {
            println("Error en getOperationsByPersonAndUser: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllProductsBySubsidiaryId(
        subsidiaryId: Int,
        available: Boolean
    ): List<IProduct> {
        return try {
            val response = apolloClient.query(
                GetAllProductsBySubsidiaryIdQuery(
                    subsidiaryId = Optional.Present(subsidiaryId),
                    available = Optional.Present(available)
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al mostrar listado de productos: $errorMessage")
            }

            response.data?.allProducts?.filterNotNull()?.map { product ->
                IProduct(
                    id = product.id!!,
                    code = product.code.orEmpty(),
                    name = product.name.orEmpty(),
                    priceWithIgv3 = product.priceWithIgv3.toSafeDouble(),
                    priceWithoutIgv3 = product.priceWithoutIgv3.toSafeDouble(),
                    stock = product.stock.toSafeDouble(),
                    minimumUnitName = product.minimumUnitName.orEmpty()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            // Log del error si es necesario
            println("Error en mostrar la lista de productos: ${e.message}")
            emptyList()
        }
    }

    suspend fun getOperationsByDateRange(
        startDate: String,
        endDate: String,
        userId: Int,
        types: List<String>
    ): List<IOperation> {
        return try {
            val query = GetOperationsByDateRangeQuery(
                startDate = startDate,
                endDate = endDate,
                userId = userId,
                types = types
            )
            val response = apolloClient.query(query).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al obtener operaciones: $errorMessage")
            }

            response.data?.operationsByDateRange?.filterNotNull()?.map { o ->
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
                    operationType = o.operationType.toString(),
                    operationAction = o.operationAction.toString(),
                    currencyType = o.currencyType.toString(),
                    operationDate = o.operationDate.toString(),
                    emitTime = o.emitTime.toString(),
                    userId = o.userId!!,
                    discountGlobal = o.discountGlobal.toSafeDouble(),
                    discountPercentageGlobal = o.discountPercentageGlobal.toSafeDouble(),
                    discountForItem = o.discountForItem.toSafeDouble(),
                    client = o.client?.let {
                        IPerson(
                            id = it.id.toInt(),
                            names = it.names.orEmpty(),
                            documentNumber = it.documentNumber.orEmpty(),
                            phone = it.phone.orEmpty(),
                            email = it.email.orEmpty(),
                            documentType = it.documentType?.toString(),
                            address = it.address.orEmpty()
                        )
                    } ?: IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
                )
            } ?: emptyList()
        } catch (e: Exception) {
            println("Error en getOperationsByDateRange: ${e.message}")
            emptyList()
        }
    }
}
