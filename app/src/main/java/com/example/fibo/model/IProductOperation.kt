package com.example.fibo.model

data class IProductOperation(
    val id: Int = 0,
    val code: String = "",
    val name: String = "",
    val priceWithIgv3: Double = 0.0,
    val priceWithoutIgv3: Double = 0.0,
    val stock: Double = 0.0,
    val minimumUnitName: String = "NIU",
)