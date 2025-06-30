package com.example.fibo.model

// Modelo para un pago individual
data class IPayment(
    val id: Int = 0,
    val wayPay: Int, // ID del método de pago (1-10 según tus WAY_PAY_CHOICES)
    val amount: Double,
    val note: String = "",
    val paymentDate: String = "", // Fecha del pago (para créditos es la fecha que selecciona el vendedor)
    val operationId: Int = 0
)
