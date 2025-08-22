package com.example.fibo.model

data class UpdateProductTariffInput(
    val id: Int,
    val unitId: Int,
    val typePrice: Int,
    val priceWithIgv: Double,
    val priceWithoutIgv: Double,
    val quantityMinimum: Double
)