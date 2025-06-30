package com.example.fibo.model

// Modelo para los métodos de pago
data class PaymentMethod(
    val id: Int,
    val name: String,
    val isCredit: Boolean = false
)
