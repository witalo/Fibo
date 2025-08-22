package com.example.fibo.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.example.fibo.CreateProductMovilMutation
import com.example.fibo.GetProductMovilByIdQuery
import com.example.fibo.GetProductsBySubsidiaryQuery
import com.example.fibo.GetTypeAffectationsQuery
import com.example.fibo.GetUnitsQuery
import com.example.fibo.UpdateProductMovilMutation
import com.example.fibo.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

@Singleton
class ProductRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getProductsBySubsidiary(subsidiaryId: Int): List<IProduct> {
        return try {
            val response = apolloClient.query(
                GetProductsBySubsidiaryQuery(subsidiaryId = subsidiaryId)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al cargar productos: $errorMessage")
            }

            response.data?.productsBySubsidiary?.filterNotNull()?.map { product ->
                IProduct(
                    id = product.id!!.toInt(),
                    code = product.code ?: "",
                    barcode = product.barcode ?: "",
                    codeSnt = product.codeSnt ?: "",
                    name = product.name ?: "",
                    stockMin = product.stockMin ?: 0,
                    stockMax = product.stockMax ?: 0,
                    path = product.path ?: "",
                    available = product.available ?: true,
                    activeType = (product.activeType).toString(),
                    ean = product.ean ?: "",
                    weightInKilograms = product.weightInKilograms?.toDouble() ?: 0.0,
                    maximumFactor = product.maximumFactor?.toDouble() ?: 0.0,
                    minimumFactor = product.minimumFactor?.toDouble() ?: 0.0,
                    minimumUnit = product.minimumUnit?.let { unit ->
                        IUnit(
                            id = unit.id.toInt(),
                            shortName = unit.shortName ?: "",
                            description = unit.description ?: "",
                            code = unit.code ?: ""
                        )
                    },
                    maximumUnit = product.maximumUnit?.let { unit ->
                        IUnit(
                            id = unit.id.toInt(),
                            shortName = unit.shortName ?: "",
                            description = unit.description ?: "",
                            code = unit.code ?: ""
                        )
                    },
                    typeAffectation = product.typeAffectation?.let { affectation ->
                        ITypeAffectation(
                            id = affectation.id!!.toInt(),
                            code = affectation.code ?: "",
                            name = affectation.name ?: "",
                            affectCode = affectation.affectCode ?: "",
                            affectName = affectation.affectName ?: "",
                            affectType = affectation.affectType ?: ""
                        )
                    },
                    subsidiary = product.subsidiary?.let { subsidiary ->
                        ISubsidiary(
                            id = subsidiary.id.toInt(),
                            serial = subsidiary.serial ?: "",
                            name = subsidiary.name ?: "",
                            address = subsidiary.address ?: "",
                            geographicLocationByDistrict = subsidiary.geographicLocationByDistrict ?: "",
                            token = subsidiary.token ?: ""
                        )
                    },
                    productTariffs = product.productTariffs?.filterNotNull()?.map { tariff ->
                        IProductTariff(
                            id = tariff.id.toInt(),
                            unit = tariff.unit?.let { unit ->
                                IUnit(
                                    id = unit.id.toInt(),
                                    shortName = unit.shortName ?: "",
                                    description = unit.description ?: "",
                                    code = unit.code ?: ""
                                )
                            },
                            typeTrade = tariff.typeTrade?.let { trade ->
                                ITypeTrade(
                                    id = trade.id.toInt(),
                                    name = trade.name ?: "",
                                    creditSuspension = trade.creditSuspension ?: 0,
                                    cashAndCreditBlock = trade.cashAndCreditBlock ?: 0
                                )
                            }.toString(),
                            typePrice = (tariff.typePrice as? Int) ?: 0, // Convertir a Int de forma segura
                            priceWithIgv = (tariff.priceWithIgv as? Double) ?: 0.0, // Convertir a Double
                            priceWithoutIgv = (tariff.priceWithoutIgv as? Double) ?: 0.0, // Convertir a Double
                            quantityMinimum = (tariff.quantityMinimum as? Double) ?: 0.0 // Convertir a Double
                        )
                    } ?: emptyList(),
                    productStores = product.productStores?.filterNotNull()?.map { store ->
                        IProductStore(
                            id = store.id.toInt(),
                            warehouse = store.warehouse?.let { warehouse ->
                                IWarehouse(
                                    id = warehouse.id.toInt(),
                                    name = warehouse.name ?: "",
                                    category = (warehouse.category ?: "NA").toString()
                                )
                            },
                            stock = (store.stock as? Double) ?: 0.0
                        )
                    } ?: emptyList()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            throw Exception("Error al cargar productos: ${e.message}")
        }
    }

    suspend fun getProductById(productId: Int): IProduct? {
        return try {
            val response = apolloClient.query(
                GetProductMovilByIdQuery(productId = productId)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al cargar producto: $errorMessage")
            }

            val product = response.data?.productMovilById ?: return null

            IProduct(
                id = product.id!!.toInt(),
                code = product.code ?: "",
                barcode = product.barcode ?: "",
                codeSnt = product.codeSnt ?: "",
                name = product.name ?: "",
                stockMin = product.stockMin ?: 0,
                stockMax = product.stockMax ?: 0,
                path = product.path ?: "",
                available = product.available ?: true,
                activeType = (product.activeType ?: "01").toString(),
                ean = product.ean ?: "",
                weightInKilograms = product.weightInKilograms?.toDouble() ?: 0.0,
                maximumFactor = product.maximumFactor?.toDouble() ?: 0.0,
                minimumFactor = product.minimumFactor?.toDouble() ?: 0.0,
                minimumUnit = product.minimumUnit?.let { unit ->
                    IUnit(
                        id = unit.id.toInt(),
                        shortName = unit.shortName ?: "",
                        description = unit.description ?: "",
                        code = unit.code ?: ""
                    )
                },
                maximumUnit = product.maximumUnit?.let { unit ->
                    IUnit(
                        id = unit.id.toInt(),
                        shortName = unit.shortName ?: "",
                        description = unit.description ?: "",
                        code = unit.code ?: ""
                    )
                },
                typeAffectation = product.typeAffectation?.let { affectation ->
                    ITypeAffectation(
                        id = affectation.id!!.toInt(),
                        code = affectation.code ?: "",
                        name = affectation.name ?: "",
                        affectCode = affectation.affectCode ?: "",
                        affectName = affectation.affectName ?: "",
                        affectType = affectation.affectType ?: ""
                    )
                },
                subsidiary = product.subsidiary?.let { subsidiary ->
                    ISubsidiary(
                        id = subsidiary.id.toInt(),
                        serial = subsidiary.serial ?: "",
                        name = subsidiary.name ?: "",
                        address = subsidiary.address ?: "",
                        geographicLocationByDistrict = subsidiary.geographicLocationByDistrict ?: "",
                        token = subsidiary.token ?: ""
                    )
                },
                productTariffs = product.productTariffs?.filterNotNull()?.map { tariff ->
                    IProductTariff(
                        id = tariff.id.toInt(),
                        unit = tariff.unit?.let { unit ->
                            IUnit(
                                id = unit.id.toInt(),
                                shortName = unit.shortName ?: "",
                                description = unit.description ?: "",
                                code = unit.code ?: ""
                            )
                        },
                        typeTrade = tariff.typeTrade?.let { trade ->
                            ITypeTrade(
                                id = trade.id.toInt(),
                                name = trade.name ?: "",
                                creditSuspension = trade.creditSuspension ?: 0,
                                cashAndCreditBlock = trade.cashAndCreditBlock ?: 0
                            )
                        }.toString(),
                        typePrice = (tariff.typePrice as? Int) ?: 0, // Convertir a Int de forma segura
                        priceWithIgv = (tariff.priceWithIgv as? Double) ?: 0.0, // Convertir a Double
                        priceWithoutIgv = (tariff.priceWithoutIgv as? Double) ?: 0.0, // Convertir a Double
                        quantityMinimum = (tariff.quantityMinimum as? Double) ?: 0.0 // Convertir a Double
                    )
                } ?: emptyList(),
                productStores = product.productStores?.filterNotNull()?.map { store ->
                    IProductStore(
                        id = store.id.toInt(),
                        warehouse = store.warehouse?.let { warehouse ->
                            IWarehouse(
                                id = warehouse.id.toInt(),
                                name = warehouse.name ?: "",
                                category = (warehouse.category ?: "NA").toString()
                            )
                        },
                        stock = (store.stock as? Double) ?: 0.0
                    )
                } ?: emptyList()
            )
        } catch (e: Exception) {
            throw Exception("Error al cargar producto: ${e.message}")
        }
    }

    suspend fun createProduct(
        name: String, // ✅ REQUERIDO
        code: String? = null, // ✅ OPCIONAL
        barcode: String? = null, // ✅ OPCIONAL
        observation: String? = null, // ✅ OPCIONAL
        stockMin: Int? = null, // ✅ OPCIONAL
        stockMax: Int? = null, // ✅ OPCIONAL
        minimumUnit: IUnit? = null, // ✅ OPCIONAL
        maximumUnit: IUnit? = null, // ✅ OPCIONAL
        typeAffectation: ITypeAffectation? = null, // ✅ OPCIONAL
        subjectPerception: Boolean? = null, // ✅ OPCIONAL
        productTariffs: List<IProductTariff> = emptyList(), // ✅ OPCIONAL
        subsidiaryId: Int // ✅ REQUERIDO
    ): Result<String> {
        return try {
            // Convertir IProductTariff a ProductTariffInput
            val tariffInputs = productTariffs.map { tariff ->
                com.example.fibo.type.ProductTariffInput(
                    unitId = Optional.present(tariff.unit?.id), // ✅ OPCIONAL
                    typePrice = tariff.typePrice, // ✅ REQUERIDO
                    priceWithIgv = tariff.priceWithIgv, // ✅ REQUERIDO
                    priceWithoutIgv = tariff.priceWithoutIgv, // ✅ REQUERIDO
                    quantityMinimum = tariff.quantityMinimum // ✅ REQUERIDO
                )
            }

            val response = apolloClient.mutation(
                CreateProductMovilMutation(
                    input = com.example.fibo.type.CreateProductInput(
                        name = name, // ✅ REQUERIDO
                        code = Optional.present(code), // ✅ OPCIONAL
                        barcode = Optional.present(barcode), // ✅ OPCIONAL
                        observation = Optional.present(observation), // ✅ OPCIONAL
                        stockMin = Optional.present(stockMin), // ✅ OPCIONAL
                        stockMax = Optional.present(stockMax), // ✅ OPCIONAL
                        minimumUnitId = Optional.present(minimumUnit?.id), // ✅ OPCIONAL
                        maximumUnitId = Optional.present(maximumUnit?.id), // ✅ OPCIONAL
                        typeAffectationId = Optional.present(typeAffectation?.id), // ✅ OPCIONAL
                        subjectPerception = Optional.present(subjectPerception), // ✅ OPCIONAL
                        productTariffs = Optional.present(tariffInputs), // ✅ OPCIONAL
                        subsidiaryId = subsidiaryId // ✅ REQUERIDO
                    )
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                return Result.failure(Exception("Error al crear producto: $errorMessage"))
            }

            val success = response.data?.createProductMovil?.success
            if (success == true) {
                val message = response.data?.createProductMovil?.message ?: "Producto creado exitosamente"
                Result.success(message)
            } else {
                val message = response.data?.createProductMovil?.message ?: "Error desconocido al crear"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al crear producto: ${e.message}"))
        }
    }

    suspend fun updateProduct(
        productId: Int, // ✅ REQUERIDO
        name: String? = null, // ✅ OPCIONAL
        code: String? = null, // ✅ OPCIONAL
        barcode: String? = null, // ✅ OPCIONAL
        observation: String? = null, // ✅ OPCIONAL
        stockMin: Int? = null, // ✅ OPCIONAL
        stockMax: Int? = null, // ✅ OPCIONAL
        minimumUnit: IUnit? = null, // ✅ OPCIONAL
        maximumUnit: IUnit? = null, // ✅ OPCIONAL
        typeAffectation: ITypeAffectation? = null, // ✅ OPCIONAL
        subjectPerception: Boolean? = null, // ✅ OPCIONAL
        productTariffs: List<IProductTariff>? = null // ✅ OPCIONAL
    ): Result<String> {
        return try {
            // Convertir IProductTariff a ProductTariffInput
            val tariffInputs = productTariffs?.map { tariff ->
                com.example.fibo.type.ProductTariffInput(
                    unitId = Optional.present(tariff.unit?.id), // ✅ OPCIONAL
                    typePrice = tariff.typePrice, // ✅ REQUERIDO
                    priceWithIgv = tariff.priceWithIgv, // ✅ REQUERIDO
                    priceWithoutIgv = tariff.priceWithoutIgv, // ✅ REQUERIDO
                    quantityMinimum = tariff.quantityMinimum // ✅ REQUERIDO
                )
            }

            val response = apolloClient.mutation(
                UpdateProductMovilMutation(
                    input = com.example.fibo.type.UpdateProductInput(
                        productId = productId, // ✅ REQUERIDO
                        name = name?:"", // ✅ REQUERIDO (pero puede ser null en la función)
                        code = Optional.present(code), // ✅ OPCIONAL
                        barcode = Optional.present(barcode), // ✅ OPCIONAL
                        observation = Optional.present(observation), // ✅ OPCIONAL
                        stockMin = Optional.present(stockMin), // ✅ OPCIONAL
                        stockMax = Optional.present(stockMax), // ✅ OPCIONAL
                        minimumUnitId = Optional.present(minimumUnit?.id), // ✅ OPCIONAL
                        maximumUnitId = Optional.present(maximumUnit?.id), // ✅ OPCIONAL
                        typeAffectationId = Optional.present(typeAffectation?.id), // ✅ OPCIONAL
                        subjectPerception = Optional.present(subjectPerception), // ✅ OPCIONAL
                        productTariffs = Optional.present(tariffInputs) // ✅ OPCIONAL
                    )
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                return Result.failure(Exception("Error al actualizar producto: $errorMessage"))
            }

            val success = response.data?.updateProductMovil?.success
            if (success == true) {
                val message = response.data?.updateProductMovil?.message ?: "Producto actualizado exitosamente"
                Result.success(message)
            } else {
                val message = response.data?.updateProductMovil?.message ?: "Error desconocido al actualizar"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar producto: ${e.message}"))
        }
    }
    suspend fun getUnits(): List<IUnit> {
        return try {
            val response = apolloClient.query(GetUnitsQuery()).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al cargar unidades: $errorMessage")
            }

            response.data?.allUnits?.filterNotNull()?.map { unit ->
                IUnit(
                    id = unit.id.toInt(),
                    shortName = unit.shortName ?: "",
                    description = unit.description ?: "",
                    code = unit.code ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) {
            throw Exception("Error al cargar unidades: ${e.message}")
        }
    }

    suspend fun getTypeAffectations(): List<ITypeAffectation> {
        return try {
            val response = apolloClient.query(GetTypeAffectationsQuery()).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al cargar tipos de afectación: $errorMessage")
            }

            response.data?.allTypeAffectations?.filterNotNull()?.map { affectation ->
                ITypeAffectation(
                    id = affectation.id!!.toInt(),
                    code = affectation.code ?: "",
                    name = affectation.name ?: "",
                    affectCode = affectation.affectCode ?: "",
                    affectName = affectation.affectName ?: "",
                    affectType = affectation.affectType ?: ""
                )
            } ?: emptyList()
        } catch (e: Exception) {
            throw Exception("Error al cargar tipos de afectación: ${e.message}")
        }
    }
}