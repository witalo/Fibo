package com.example.fibo.model

data class CreateProductInput(
    val code: String,
    val name: String,
    val barcode: String = "",
    val stockMin: Int = 0,
    val stockMax: Int = 0,
    val minimumUnitId: Int? = null,
    val maximumUnitId: Int? = null,
    val typeAffectationId: Int? = null,
    val subsidiaryId: Int,
    val observation: String = "-",
    val subjectPerception: Boolean = false,
    val productTariffs: List<CreateProductTariffInput> = emptyList()
)