package com.example.fibo.model


data class IProductTariff(
    val id: Int = 0,
    val product: IProduct? = null,
    val unit: IUnit? = null,
    val typeTrade: String = "",
    val typePrice: Int = 0, // 1: Compra, 3: Venta
    val priceWithIgv: Double = 0.0,
    val priceWithoutIgv: Double = 0.0,
    val quantityMinimum: Double = 0.0
) {
    val priceTypeLabel: String
        get() = when (typePrice) {
            1 -> "Compra"
            2 -> "Compra Mayorista"
            3 -> "Venta"
            4 -> "Venta Mayorista"
            else -> "No Aplica"
        }

    val isSalePrice: Boolean
        get() = typePrice == 3 || typePrice == 4

    val isPurchasePrice: Boolean
        get() = typePrice == 1 || typePrice == 2
}