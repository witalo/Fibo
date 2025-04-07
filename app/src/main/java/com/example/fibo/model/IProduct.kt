package com.example.fibo.model

data class IProduct(
    val id: Int,
    val code: String,
    val name: String,
    // Tarifa tipo 3 (ej. precio de cobertura crédito)
    val priceWithIgv3: Double,
    val priceWithoutIgv3: Double,
    val productTariffId3: Int,
    // Cantidad disponible en el almacén principal
    val remainingQuantity: Double,
    // Afectación al IGV
    val typeAffectationId: Int?
)