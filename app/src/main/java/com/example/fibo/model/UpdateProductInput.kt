package com.example.fibo.model

data class UpdateProductInput(
    val productId: Int,
    val code: String? = null,
    val name: String? = null,
    val barcode: String? = null,
    val stockMin: Int? = null,
    val stockMax: Int? = null,
    val minimumUnitId: Int? = null,
    val maximumUnitId: Int? = null,
    val typeAffectationId: Int? = null,
    val observation: String? = null,
    val subjectPerception: Boolean? = null,
    val productTariffs: List<UpdateProductTariffInput>? = null
)