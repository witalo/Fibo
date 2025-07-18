package com.example.fibo.model

data class ISupplier(
    val id: Int,
    val names: String,
    val address: String?,
    val documentNumber: String,
    val documentType: String?,
    val phone: String?,
    val email: String?
) 