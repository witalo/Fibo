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
import com.example.fibo.CreateSaleMutation
import com.example.fibo.GuideModesQuery
import com.example.fibo.GuideReasonsQuery
import com.example.fibo.SerialsQuery
import com.example.fibo.SearchClientQuery
import com.example.fibo.SearchGeographicLocationQuery
import com.example.fibo.DocumentTypesQuery
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IGeographicLocation
import com.example.fibo.model.IDocumentType
import com.example.fibo.AllGuidesQuery
import com.example.fibo.CustomRefreshTokenMutation
import com.example.fibo.model.IGuide
import com.example.fibo.model.IGuideResponse
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ICompany
import com.example.fibo.GetGuideByIdQuery
import com.example.fibo.model.IGuideData
import com.example.fibo.model.IGuideDetail
import com.example.fibo.model.IWeightMeasurementUnit
import com.example.fibo.model.IGuideLocation
import com.example.fibo.model.IDistrict
import com.example.fibo.model.IVehicle
import com.example.fibo.model.IRelatedDocument
import com.example.fibo.model.ISupplier
import com.example.fibo.GetPurchasesQuery
import com.example.fibo.GetSuppliersQuery
import com.example.fibo.SearchSupplierByParameterQuery
import com.example.fibo.CreatePurchaseMutation
import com.example.fibo.CancelPurchaseMutation
import java.text.SimpleDateFormat
import java.util.Locale


@Singleton
class OperationRepository @Inject constructor(
    private val apolloClient: ApolloClient,
    private val preferencesManager: PreferencesManager
) {
    
    /**
     * Método para renovar tokens automáticamente cuando expiren
     */
    private suspend fun refreshTokenIfNeeded(): String? {
        return try {
            val refreshToken = preferencesManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                Log.e("OperationRepository", "No hay refresh token disponible")
                return null
            }
            
            val response = apolloClient.mutation(
                CustomRefreshTokenMutation(refreshToken = refreshToken)
            ).execute()
            
            if (response.hasErrors()) {
                Log.e("OperationRepository", "Error al renovar token: ${response.errors?.first()?.message}")
                // Si hay error, probablemente el refresh token ha expirado
                preferencesManager.clearUserData()
                return null
            }
            
            val data = response.data?.refreshToken
            if (data?.token != null && data.refreshToken != null) {
                // Actualizar tokens en preferencias
                preferencesManager.updateTokens(data.token, data.refreshToken)
                Log.d("OperationRepository", "Tokens renovados exitosamente")
                return data.token
            } else {
                Log.e("OperationRepository", "No se pudieron obtener nuevos tokens")
                preferencesManager.clearUserData()
                return null
            }
        } catch (e: Exception) {
            Log.e("OperationRepository", "Excepción al renovar token: ${e.message}")
            preferencesManager.clearUserData()
            return null
        }
    }
    
    /**
     * Método helper para ejecutar operaciones con manejo automático de tokens
     */
    private suspend fun <T> executeWithTokenRefresh(
        operation: suspend (token: String) -> T
    ): T? {
        var token = preferencesManager.getAuthToken()
        if (token.isNullOrEmpty()) {
            Log.e("OperationRepository", "No hay token de autenticación")
            return null
        }
        
        return try {
            // Intentar la operación con el token actual
            operation(token)
        } catch (e: Exception) {
            // Si el error indica que el token ha expirado, intentar renovarlo
            val errorMessage = e.message ?: ""
            if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                Log.d("OperationRepository", "Token expirado, intentando renovar...")
                val newToken = refreshTokenIfNeeded()
                if (newToken != null) {
                    // Reintentar la operación con el nuevo token
                    try {
                        operation(newToken)
                    } catch (retryException: Exception) {
                        Log.e("OperationRepository", "Error después de renovar token: ${retryException.message}")
                        throw retryException
                    }
                } else {
                    Log.e("OperationRepository", "No se pudo renovar el token")
                    throw e
                }
            } else {
                throw e
            }
        }
    }

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
            val message = response.data?.sntPerson?.message ?: ""
            
            // Si no hay datos de persona, lanzar error
            if (personData == null) {
                throw RuntimeException(message.ifEmpty { "No se encontraron datos del cliente" })
            }

            // Aunque el success sea false (cliente ya registrado), si hay datos de persona, los devolvemos
            IPerson(
                id = 0,
                names = personData.sntNames.orEmpty(),
                documentNumber = document,
                phone = "",
                email = "",
                address = personData.sntAddress.orEmpty(),
                driverLicense = personData.sntDriverLicense.orEmpty()
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
                    parentOperation = Optional.present((operation.parentOperation)),
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
            Log.d("OperationRepository", "Ejecutando getAllProductsBySubsidiaryId - subsidiaryId: $subsidiaryId, available: $available")
            
            // Probar primero sin executeWithTokenRefresh
            val token = preferencesManager.getAuthToken()
            Log.d("OperationRepository", "Token obtenido: ${!token.isNullOrEmpty()}")
            
            if (token.isNullOrEmpty()) {
                Log.e("OperationRepository", "No hay token disponible")
                return emptyList()
            }
            
            Log.d("OperationRepository", "Ejecutando query con token directo...")
            val response = apolloClient.query(
                GetAllProductsBySubsidiaryIdQuery(
                    subsidiaryId = Optional.Present(subsidiaryId),
                    available = Optional.Present(available)
                )
            ).addHttpHeader("Authorization", "JWT $token").execute()

            Log.d("OperationRepository", "Respuesta GraphQL recibida - hasErrors: ${response.hasErrors()}")

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                Log.e("OperationRepository", "Error en GraphQL: $errorMessage")
                return emptyList()
            }

            val rawProducts = response.data?.allProducts
            Log.d("OperationRepository", "Productos raw de GraphQL: ${rawProducts?.size ?: 0}")
            
            if (rawProducts != null && rawProducts.isNotEmpty()) {
                Log.d("OperationRepository", "Primer producto raw: ${rawProducts.first()}")
            }

            val mappedProducts = rawProducts?.filterNotNull()?.map { product ->
                Log.d("OperationRepository", "Mapeando producto - ID: ${product.id}, code: ${product.code}, name: ${product.name}")
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
            
            Log.d("OperationRepository", "Productos mapeados: ${mappedProducts.size}")
            mappedProducts
            
        } catch (e: Exception) {
            Log.e("OperationRepository", "Error en getAllProductsBySubsidiaryId: ${e.message}", e)
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

    suspend fun createSale(input: CreateSaleMutation): Result<CreateSaleMutation.Data> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.mutation(input).addHttpHeader("Authorization", "JWT $token").execute()
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data!!
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuideModes(): Result<GuideModesQuery.Data> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(GuideModesQuery())
                    .addHttpHeader("Authorization", "JWT $token")
                    .execute()
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data!!
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuideReasons(): Result<GuideReasonsQuery.Data> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(GuideReasonsQuery())
                    .addHttpHeader("Authorization", "JWT $token")
                    .execute()
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data!!
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSerials(subsidiaryId: Int): Result<SerialsQuery.Data> {
        return try {
            val response = apolloClient.query(SerialsQuery(Optional.present(subsidiaryId)))
                .execute()
            if (response.hasErrors()) {
                Result.failure(Exception(response.errors?.first()?.message))
            } else {
                Result.success(response.data!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchClientByParameter(
        search: String,
        isClient: Boolean = false,
        documentType: String? = null,
        operationDocumentType: String? = null,
        isDriver: Boolean = false,
        isSupplier: Boolean = false,
        isReceiver: Boolean = false
    ): Result<SearchClientQuery.Data> {
        Log.d("SearchClientByParameter", "search: $search, isClient: $isClient, documentType: $documentType, operationDocumentType: $operationDocumentType, isDriver: $isDriver, isSupplier: $isSupplier, isReceiver: $isReceiver")
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(
                    SearchClientQuery(
                        search = search,
                        isClient = Optional.present(isClient),
                        documentType = Optional.presentIfNotNull(documentType),
                        operationDocumentType = Optional.presentIfNotNull(operationDocumentType),
                        isDriver = Optional.present(isDriver),
                        isSupplier = Optional.present(isSupplier),
                        isReceiver = Optional.present(isReceiver)
                    )
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data!!
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchGeographicLocation(search: String): Result<List<IGeographicLocation>> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(
                    SearchGeographicLocationQuery(search = search)
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data?.searchGeographicLocationCode?.map { location ->
                        IGeographicLocation(
                            districtId = location?.districtId ?: "",
                            districtDescription = location?.districtDescription ?: "",
                            provinceDescription = location?.provinceDescription ?: "",
                            departmentDescription = location?.departmentDescription ?: ""
                        )
                    } ?: emptyList()
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDocumentTypes(): Result<DocumentTypesQuery.Data> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(DocumentTypesQuery())
                    .addHttpHeader("Authorization", "JWT $token")
                    .execute()
                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data!!
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllGuides(
        subsidiaryId: Int,
        startDate: String,
        endDate: String,
        documentType: String,
        page: Int,
        pageSize: Int
    ): Result<IGuideResponse> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(
                    AllGuidesQuery(
                        subsidiaryId = subsidiaryId,
                        startDate = startDate,
                        endDate = endDate,
                        documentType = documentType,
                        page = page,
                        pageSize = pageSize
                    )
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    val data = response.data?.allGuides
                    val guides = data?.guides?.map { guide ->
                        IGuide(
                            id = guide?.id?.toInt() ?: 0,
                            emitDate = guide?.emitDate?.toString() ?: "",
                            emitTime = guide?.emitTime?.toString() ?: "",
                            documentType = guide?.documentType?.toString()?.replace("A_", "") ?: "",
                            serial = guide?.serial ?: "",
                            correlative = guide?.correlative ?: 0,
                            subsidiary = guide?.subsidiary?.let { sub ->
                                ISubsidiary(
                                    id = 0,
                                    serial = "",
                                    name = sub.companyName ?: "",
                                    geographicLocationByDistrict = "",
                                    address = "",
                                    token = ""
                                )
                            },
                            client = guide?.client?.let { client ->
                                IPerson(
                                    id = 0,
                                    names = client.names ?: "",
                                    documentType = client.documentType?.toString() ?: "",
                                    documentNumber = "",
                                    phone = "",
                                    email = "",
                                    address = ""
                                )
                            },
                            sendWhatsapp = guide?.sendWhatsapp ?: false,
                            sendClient = guide?.sendClient ?: false,
                            linkXml = guide?.linkXml,
                            linkCdr = guide?.linkCdr,
                            sunatStatus = guide?.sunatStatus?.toString(),
                            sunatDescription = guide?.sunatDescription?.toString(),
                            operationStatus = guide?.operationStatus?.toString() ?: "",
                            operationStatusReadable = guide?.operationStatusReadable ?: ""
                        )
                    } ?: emptyList()

                    IGuideResponse(
                        guides = guides,
                        totalNumberOfPages = data?.totalNumberOfPages ?: 0,
                        totalNumberOfSales = data?.totalNumberOfSales ?: 0
                    )
                }
            }
            
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("No se pudo ejecutar la operación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGuideById(guideId: Int): IGuideData {
        val query = GetGuideByIdQuery(id = Optional.present(guideId))
        val response = apolloClient.query(query).execute()
        
        if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Error desconocido"
            throw ApolloException("Error en la consulta GraphQL: $errorMessage")
        }
        
        val operationData = response.data?.operationById
            ?: throw ApolloException("No se encontró la operación con ID $guideId")
        
        return IGuideData(
            id = operationData.id.toInt(),
            subsidiary = operationData.subsidiary?.let { sub ->
                ISubsidiary(
                    id = 0, // No disponible en la query
                    serial = "",
                    name = sub.company?.businessName.orEmpty(),
                    address = sub.address.orEmpty(),
                    geographicLocationByDistrict = sub.geographicLocationByDistrict.orEmpty(),
                    token = "",
                    company = sub.company?.let { comp ->
                        ICompany(
                            id = 0,
                            businessName = comp.businessName.orEmpty(),
                            doc = comp.doc.orEmpty(),
                            address = comp.address.orEmpty(),
                            logo = ""
                        )
                    }
                )
            } ?: throw ApolloException("Los datos de la subsidiaria están vacíos"),
            client = operationData.client?.let { client ->
                IPerson(
                    id = 0,
                    names = client.names.orEmpty(),
                    documentType = client.documentTypeReadable,
                    documentNumber = client.documentNumber.orEmpty(),
                    phone = "",
                    email = "",
                    address = ""
                )
            } ?: throw ApolloException("Los datos del cliente están vacíos"),
            documentType = operationData.documentType.toString(),
            documentTypeReadable = operationData.documentTypeReadable.toString(),
            serial = operationData.serial.orEmpty(),
            correlative = operationData.correlative ?: 0,
            emitDate = operationData.emitDate.toString(),
            guideModeTransferReadable = operationData.guideModeTransferReadable,
            guideReasonTransferReadable = operationData.guideReasonTransferReadable,
            operationDetailSet = operationData.operationdetailSet.map { detail ->
                IGuideDetail(
                    productName = detail.productName.orEmpty(),
                    quantity = detail.quantity.toString(),
                    description = detail.description.orEmpty()
                )
            },
            relatedDocuments = operationData.relatedDocuments.map { doc ->
                IRelatedDocument(
                    serial = doc.serial,
                    correlative = doc.correlative,
                    documentType = doc.documentType.toString()
                )
            },
            transferDate = operationData.transferDate?.toString(),
            totalWeight = operationData.totalWeight?.toString(),
            weightMeasurementUnit = operationData.weightMeasurementUnit?.let { unit ->
                IWeightMeasurementUnit(shortName = unit.shortName.orEmpty())
            },
            quantityPackages = operationData.quantityPackages?.toString(),
            transportationCompany = operationData.transportationCompany?.let { company ->
                IPerson(
                    id = 0,
                    names = company.names.orEmpty(),
                    documentType = company.documentTypeReadable,
                    documentNumber = company.documentNumber.orEmpty(),
                    phone = "",
                    email = "",
                    address = "",
                    mtcRegistrationNumber = company.mtcRegistrationNumber
                )
            },
            mainDriver = operationData.mainDriver?.let { driver ->
                IPerson(
                    id = 0,
                    names = driver.names.orEmpty(),
                    documentType = driver.documentTypeReadable,
                    documentNumber = driver.documentNumber.orEmpty(),
                    phone = "",
                    email = "",
                    address = "",
                    driverLicense = driver.driverLicense.orEmpty()
                )
            },
            mainVehicle = operationData.mainVehicle?.let { vehicle ->
                IVehicle(
                    id = 0,
                    licensePlate = vehicle.licensePlate.orEmpty()
                )
            },
            othersDrivers = operationData.othersDrivers.map { driver ->
                IPerson(
                    id = 0,
                    names = driver.names.orEmpty(),
                    documentType = driver.documentTypeReadable,
                    documentNumber = driver.documentNumber.orEmpty(),
                    phone = "",
                    email = "",
                    address = "",
                    driverLicense = driver.driverLicense.orEmpty()
                )
            },
            othersVehicles = operationData.othersVehicles.map { vehicle ->
                IVehicle(
                    id = 0,
                    licensePlate = vehicle.licensePlate.orEmpty()
                )
            },
            receiver = operationData.receiver?.let { receiver ->
                IPerson(
                    id = 0,
                    names = receiver.names.orEmpty(),
                    documentType = receiver.documentTypeReadable,
                    documentNumber = receiver.documentNumber.orEmpty(),
                    phone = "",
                    email = "",
                    address = ""
                )
            },
            guideOrigin = operationData.guideOrigin?.let { origin ->
                IGuideLocation(
                    district = IDistrict(
                        id = origin.district?.id.orEmpty(),
                        description = origin.district?.description.orEmpty()
                    ),
                    address = origin.address.orEmpty(),
                    serial = origin.serial.orEmpty()
                )
            },
            guideArrival = operationData.guideArrival?.let { arrival ->
                IGuideLocation(
                    district = IDistrict(
                        id = arrival.district?.id.orEmpty(),
                        description = arrival.district?.description.orEmpty()
                    ),
                    address = arrival.address.orEmpty(),
                    serial = arrival.serial.orEmpty()
                )
            }
        )
    }

    // ========== MÉTODOS PARA COMPRAS ==========
    
    /**
     * Obtener compras por fecha y filtros
     */
    suspend fun getPurchases(
        subsidiaryId: Int,
        supplierId: Int,
        startDate: String,
        endDate: String,
        documentType: String,
        page: Int,
        pageSize: Int,
        serial: String? = null,
        correlative: Int? = null
    ): List<IOperation> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(
                    GetPurchasesQuery(
                        subsidiaryId = subsidiaryId,
                        supplierId = supplierId,
                        startDate = startDate,
                        endDate = endDate,
                        documentType = documentType,
                        page = page,
                        pageSize = pageSize,
                        serial = Optional.presentIfNotNull(serial),
                        correlative = Optional.presentIfNotNull(correlative)
                    )
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data?.allPurchases?.purchases?.map { purchase ->
                        IOperation(
                            id = purchase?.id?.toInt() ?: 0,
                            documentType = purchase?.documentType?.toString() ?: "",
                            documentTypeReadable = when (purchase?.documentType?.toString()?.replace("A_", "")) {
                                "01" -> "FACTURA"
                                "03" -> "BOLETA"
                                "07" -> "NOTA DE CRÉDITO"
                                else -> "COMPROBANTE"
                            },
                            emitDate = purchase?.emitDate?.toString() ?: "",
                            serial = purchase?.serial ?: "",
                            correlative = purchase?.correlative ?: 0,
                            totalAmount = purchase?.totalAmount?.toSafeDouble() ?: 0.0,
                            totalTaxed = purchase?.totalTaxed?.toSafeDouble() ?: 0.0,
                            totalDiscount = purchase?.totalDiscount?.toSafeDouble() ?: 0.0,
                            totalExonerated = purchase?.totalExonerated?.toSafeDouble() ?: 0.0,
                            totalUnaffected = purchase?.totalUnaffected?.toSafeDouble() ?: 0.0,
                            totalFree = purchase?.totalFree?.toSafeDouble() ?: 0.0,
                            totalIgv = purchase?.totalIgv?.toSafeDouble() ?: 0.0,
                            totalToPay = purchase?.totalToPay?.toSafeDouble() ?: 0.0,
                            totalPayed = purchase?.totalPayed?.toSafeDouble() ?: 0.0,
                            operationStatus = purchase?.operationStatus?.toString() ?: "",
                            subsidiaryId = subsidiaryId,
                            supplier = purchase?.supplier?.let { supplier ->
                                ISupplier(
                                    id = 0,
                                    names = supplier.names ?: "",
                                    documentNumber = supplier.documentNumber ?: "",
                                    address = null,
                                    documentType = null,
                                    phone = null,
                                    email = null
                                )
                            },
                            // Para compras, el cliente será nulo ya que se usa supplier
                            client = IPerson(id = 0, names = "", documentNumber = "", phone = "", email = "")
                        )
                    } ?: emptyList()
                }
            }
            
            result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OperationRepository", "Error en getPurchases: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Buscar proveedores por parámetro
     */
    suspend fun searchSuppliers(query: String): List<ISupplier> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(
                    SearchSupplierByParameterQuery(
                        search = query,
                        isSupplier = true
                    )
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data?.searchClientByParameter?.map { supplier ->
                        ISupplier(
                            id = supplier?.id?.toInt() ?: 0,
                            names = supplier?.names ?: "",
                            documentNumber = supplier?.documentNumber ?: "",
                            documentType = supplier?.documentType?.toString(),
                            address = null,
                            phone = null,
                            email = null
                        )
                    } ?: emptyList()
                }
            }
            
            result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OperationRepository", "Error en searchSuppliers: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtener todos los proveedores
     */
    suspend fun getAllSuppliers(): List<ISupplier> {
        return try {
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.query(GetSuppliersQuery())
                    .addHttpHeader("Authorization", "JWT $token")
                    .execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data?.allSuppliers?.map { supplier ->
                        ISupplier(
                            id = supplier?.id?.toInt() ?: 0,
                            names = supplier?.names ?: "",
                            documentNumber = supplier?.documentNumber ?: "",
                            documentType = supplier?.documentType?.toString(),
                            address = supplier?.address,
                            phone = supplier?.phone,
                            email = supplier?.email
                        )
                    } ?: emptyList()
                }
            }
            
            result ?: emptyList()
        } catch (e: Exception) {
            Log.e("OperationRepository", "Error en getAllSuppliers: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Cancelar una operación (compra o venta)
     */
    suspend fun cancelOperation(operationId: Int): Boolean {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
            
            val result = executeWithTokenRefresh { token ->
                val response = apolloClient.mutation(
                    CancelPurchaseMutation(
                        operationId = operationId,
                        lowDate = today
                    )
                ).addHttpHeader("Authorization", "JWT $token").execute()

                if (response.hasErrors()) {
                    val errorMessage = response.errors?.first()?.message ?: "Error desconocido"
                    if (errorMessage.contains("Signature has expired") || errorMessage.contains("token_expired")) {
                        throw Exception(errorMessage)
                    }
                    throw Exception(errorMessage)
                } else {
                    response.data?.cancelInvoice?.success ?: false
                }
            }
            
            result ?: false
        } catch (e: Exception) {
            Log.e("OperationRepository", "Error en cancelOperation: ${e.message}", e)
            false
        }
    }

//    suspend fun createPurchase(operation: IOperation): Result<String> {
//        return try {
//            // Convertir los detalles de la operación a los formatos requeridos
//            val productTariffIdSet = operation.operationDetailSet.map { it.productTariffId }
//            val typeAffectationIdSet = operation.operationDetailSet.map { it.typeAffectationId }
//            val quantitySet = operation.operationDetailSet.map { it.quantity }
//            val unitValueSet = operation.operationDetailSet.map { it.unitValue }
//            val unitPriceSet = operation.operationDetailSet.map { it.unitPrice }
//            val discountPercentageSet = operation.operationDetailSet.map { it.discountPercentage }
//            val igvPercentageSet = operation.operationDetailSet.map { it.igvPercentage }
//            val perceptionPercentageSet = operation.operationDetailSet.map { it.perceptionPercentage }
//            val commentSet = operation.operationDetailSet.map { it.comment ?: "" }
//            val totalDiscountSet = operation.operationDetailSet.map { it.totalDiscount }
//            val totalValueSet = operation.operationDetailSet.map { it.totalValue }
//            val totalIgvSet = operation.operationDetailSet.map { it.totalIgv }
//            val totalAmountSet = operation.operationDetailSet.map { it.totalAmount }
//            val totalPerceptionSet = operation.operationDetailSet.map { it.totalPerception }
//            val totalToPaySet = operation.operationDetailSet.map { it.totalToPay }
//            val wayPaySet = operation.payments?.map { it.wayPay } ?: emptyList()
//            val totalSet = operation.payments?.map { it.amount } ?: emptyList()
//            val descriptionSet = operation.payments?.map { it.description ?: "" } ?: emptyList()
//            val transactionDateSet = operation.payments?.map { it.paymentDate } ?: emptyList()
//
//            val response = apolloClient.mutation(
//                CreatePurchaseMutation(
//                    serial = operation.serial,
//                    correlative = operation.correlative,
//                    operationType = operation.operationType,
//                    documentType = operation.documentType,
//                    currencyType = operation.currencyType,
//                    saleExchangeRate = operation.saleExchangeRate,
//                    emitDate = operation.emitDate,
//                    dueDate = operation.dueDate,
//                    supplierId = operation.supplier?.id ?: throw IllegalArgumentException("Supplier ID is required"),
//                    productTariffIdSet = productTariffIdSet,
//                    typeAffectationIdSet = typeAffectationIdSet,
//                    quantitySet = quantitySet,
//                    unitValueSet = unitValueSet,
//                    unitPriceSet = unitPriceSet,
//                    discountPercentageSet = discountPercentageSet,
//                    igvPercentageSet = igvPercentageSet,
//                    perceptionPercentageSet = perceptionPercentageSet,
//                    commentSet = commentSet,
//                    totalDiscountSet = totalDiscountSet,
//                    totalValueSet = totalValueSet,
//                    totalIgvSet = totalIgvSet,
//                    totalAmountSet = totalAmountSet,
//                    totalPerceptionSet = totalPerceptionSet,
//                    totalToPaySet = totalToPaySet,
//                    wayPaySet = wayPaySet,
//                    totalSet = totalSet,
//                    descriptionSet = descriptionSet,
//                    transactionDateSet = transactionDateSet,
//                    discountForItem = operation.discountForItem,
//                    discountGlobal = operation.discountGlobal,
//                    discountPercentageGlobal = operation.discountPercentageGlobal,
//                    igvType = operation.igvType,
//                    totalDiscount = operation.totalDiscount,
//                    totalTaxed = operation.totalTaxed,
//                    totalUnaffected = operation.totalUnaffected,
//                    totalExonerated = operation.totalExonerated,
//                    totalIgv = operation.totalIgv,
//                    totalFree = operation.totalFree,
//                    totalAmount = operation.totalAmount,
//                    totalPerception = operation.totalPerception,
//                    totalToPay = operation.totalToPay,
//                    totalPayed = operation.totalPayed,
//                    totalTurned = operation.totalTurned,
//                    observation = operation.observation ?: "",
//                    hasPerception = operation.hasPerception,
//                    hasRetention = operation.hasRetention,
//                    hasDetraction = operation.hasDetraction,
//                    perceptionType = operation.perceptionType,
//                    perceptionPercentage = operation.perceptionPercentage,
//                    retentionType = operation.retentionType,
//                    totalRetention = operation.totalRetention,
//                    retentionPercentage = operation.retentionPercentage,
//                    detractionType = operation.detractionType,
//                    detractionPaymentMethod = operation.detractionPaymentMethod,
//                    totalDetraction = operation.totalDetraction,
//                    detractionPercentage = operation.detractionPercentage
//                )
//            ).execute()
//
//            if (response.hasErrors()) {
//                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
//                Result.failure(Exception(errorMessage))
//            } else {
//                val data = response.data?.createPurchase
//                if (data?.error.isNullOrEmpty()) {
//                    Result.success(data?.message ?: "Compra registrada exitosamente")
//                } else {
//                    Result.failure(Exception(data?.error ?: "Error al crear la compra"))
//                }
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
}
