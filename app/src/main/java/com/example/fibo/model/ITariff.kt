package com.example.fibo.model

data class ITariff(
    val productId: Int,
    val productCode: String,
    val productName: String,
    val unitId: Int,
    val unitName: String,
    val remainingQuantity: Double = 0.0,
    val priceWithIgv: Double = 0.0,
    val priceWithoutIgv: Double = 0.0,
    val productTariffId: Int,
    val typeAffectationId: Int,
)