package com.example.fibo.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.CreatePersonMutation
import com.example.fibo.GetAllPersonsBySubsidiaryAndTypeQuery
import com.example.fibo.GetPersonByIdQuery
import com.example.fibo.SntPersonMutation
import com.example.fibo.model.IPerson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getPersonById(personId: Int): IPerson {
        val query = GetPersonByIdQuery(clientId=personId.toString())
        val response = apolloClient.query(query).execute()
        // Validar errores de la respuesta
        if (response.hasErrors()) {
            val errorMessage = response.errors?.firstOrNull()?.message ?: "Error desconocido"
            throw ApolloException("Error en la consulta GraphQL: $errorMessage")
        }
        val personData = response.data?.clientById
            ?: throw ApolloException("No se encontró la persona con ID $personId")


        // Mapear datos a modelos locales
        val person = IPerson(
            id = personData.id.toInt(),
            names = personData.names,
            code = personData.code,
            shortName = personData.shortName,
            documentType = personData.documentType,
            documentNumber = personData.documentNumber,
            email = personData.email,
            phone = personData.phone,
            address = personData.address,
            country = "",
            economicActivityMain = 0,
            isEnabled = personData.isEnabled,
            isSupplier = personData.isSupplier,
            isClient = personData.isClient,
        )
        return person
    }
    suspend fun getAllPersonsBySubsidiaryAndType(subsidiaryId: Int, type: String): List<IPerson> {
        return try {
            val response = apolloClient.query(
                GetAllPersonsBySubsidiaryAndTypeQuery(subsidiaryId = subsidiaryId, type = type)
            ).execute()

            if (response.hasErrors()) {
                val errorMessage =
                    response.errors?.joinToString { it.message } ?: "Error desconocido"
                throw Exception("Error al mostrar los datos: $errorMessage")
            }

            response.data?.allPersonsBySubsidiaryAndType?.filterNotNull()?.map { person ->
                IPerson(
                    id = person.id.toInt(),
                    names = person.names,
                    code = person.code,
                    shortName = person.shortName,
                    documentType = person.documentType,
                    documentNumber = person.documentNumber,
                    email = person.email,
                    phone = person.phone,
                    address = person.address,
                    country = "",
                    economicActivityMain = 0,
                    isEnabled = person.isEnabled,
                    isSupplier = person.isSupplier,
                    isClient = person.isClient
                )
            } ?: emptyList()
        } catch (e: Exception) {
            // Log del error si es necesario
            println("Error en la consulta GraphQL: ${e.message}")
            emptyList()
        }
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
    suspend fun createPerson(
        names: String,
        shortName: String,
        code: String,
        phone: String,
        email: String,
        address: String,
        country: String,
        districtId: String,
        documentType: String,
        documentNumber: String,
        isEnabled: Boolean,
        isSupplier: Boolean,
        isClient: Boolean,
        isDriver: Boolean,
        economicActivityMain: Int,
        driverLicense: String,
        subsidiaryId: Int
    ): Result<String> {
        return try {
            val response = apolloClient.mutation(
                CreatePersonMutation(
                    names = names,
                    shortName = shortName,
                    code = Optional.present(code),
                    phone = phone,
                    email = email,
                    address = address,
                    country = country,
                    districtId = districtId,
                    documentType = documentType,
                    documentNumber = documentNumber,
                    isEnabled = isEnabled,
                    isSupplier = isSupplier,
                    isClient = isClient,
                    isDriver = isDriver,
                    economicActivityMain = economicActivityMain,
                    driverLicense = Optional.present(driverLicense),
                    subsidiaryId = Optional.present(subsidiaryId)
                )
            ).execute()

            if (response.hasErrors()) {
                val errorMessage = response.errors?.joinToString { it.message } ?: "Error desconocido"
                Result.failure(Exception("Error al crear persona: $errorMessage"))
            } else {
                val data = response.data?.createPerson
                if (data?.success == true) {
                    Result.success(data.message ?: "Persona creada exitosamente")
                } else {
                    Result.failure(Exception(data?.message ?: "Error al crear la persona"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}