package com.example.fibo.model

data class IProductTariffOperation(
    val id: Int,
//    val unitId: Int,
//    val unitName: String,
    val priceWithIgv: Double = 0.0,
    val priceWithoutIgv: Double = 0.0,
//    val remainingQuantity: Double = 0.0,
    val productId: Int,
    val productCode: String,
    val productName: String,
    val typeAffectationId: Int,
//    val typeAffectationName: String
//    val typePrice: String
)