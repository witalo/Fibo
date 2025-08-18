package com.example.fibo.repository

import android.annotation.SuppressLint
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.AllSalesPaymentsQuery
import com.example.fibo.GetAllPersonBySubsidiaryQuery
import com.example.fibo.GetAllPersonsBySubsidiaryAndTypeQuery
import com.example.fibo.GetOperationByIdQuery
import com.example.fibo.GetPersonByIdQuery
import com.example.fibo.SearchProductsQuery
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ITariff
import com.example.fibo.ui.screens.reportpayment.SaleWithPayments
import kotlinx.coroutines.flow.Flow
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
}