package com.example.fibo.repository

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloException
import com.example.fibo.GetPersonByIdQuery
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProductOperation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val apolloClient: ApolloClient
) {
}