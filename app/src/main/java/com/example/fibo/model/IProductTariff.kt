package com.example.fibo.model

data class IProductTariff(
    val id: Int,
    val unitId: Int,
    val unitName: String,
    val priceWithIgv: Double = 0.0,
    val priceWithoutIgv: Double = 0.0,
    val quantityMinimum: Double = 0.0,
    val productId: Int,
    val productName: String,
    val typePrice: String
)