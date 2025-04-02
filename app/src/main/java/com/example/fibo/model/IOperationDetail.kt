package com.example.fibo.model

data class IOperationDetail(
    val id: Int,
    val productTariff: IProductTariff,
    val quantity: String,
    val price: String,
    val unitValue: Double = 0.0,
    val unitPrice: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val discountPercentage: Double = 0.0,
    val igvPercentage: Double = 0.0,
    val totalIgv: Double = 0.0,
    val totalValue: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalToPay: Double = 0.0,
)