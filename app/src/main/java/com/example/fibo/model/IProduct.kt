package com.example.fibo.model

data class IProduct(
    val id: Int = 0,
    val code: String = "",
    val barcode: String = "",
    val codeSnt: String = "",
    val name: String = "",
    val stockMin: Int = 0,
    val stockMax: Int = 0,
    val path: String = "",
    val available: Boolean = true,
    val activeType: String = "01", // PRODUCTO por defecto
    val ean: String = "",
    val weightInKilograms: Double = 0.0,
    val maximumFactor: Double = 0.0,
    val minimumFactor: Double = 0.0,
    val minimumUnit: IUnit? = null,
    val maximumUnit: IUnit? = null,
    val typeAffectation: ITypeAffectation? = null,
    val subjectPerception: Boolean = false,
    val observation: String = "-",
    val subsidiary: ISubsidiary? = null,
    val productTariffs: List<IProductTariff> = emptyList(),
    val productStores: List<IProductStore> = emptyList()
) {
    val hasPricing: Boolean
        get() = productTariffs.isNotEmpty()

    val salePrice: Double
        get() = productTariffs.find { it.typePrice == 3 }?.priceWithIgv ?: 0.0

    val salePriceWithoutIgv: Double
        get() = productTariffs.find { it.typePrice == 3 }?.priceWithoutIgv ?: 0.0

    val purchasePrice: Double
        get() = productTariffs.find { it.typePrice == 1 }?.priceWithIgv ?: 0.0

    val purchasePriceWithoutIgv: Double
        get() = productTariffs.find { it.typePrice == 1 }?.priceWithoutIgv ?: 0.0

    val mainUnit: String
        get() = productTariffs.firstOrNull()?.unit?.shortName ?: ""

    val totalStock: Double
        get() = productStores.sumOf { it.stock }
}