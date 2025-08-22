package com.example.fibo.model

data class IProductStore(
    val id: Int = 0,
    val product: IProduct? = null,
    val warehouse: IWarehouse? = null,
    val stock: Double = 0.0
)