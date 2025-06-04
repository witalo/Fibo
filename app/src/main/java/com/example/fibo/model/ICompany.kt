package com.example.fibo.model

data class ICompany(
    val id: Int,
    val doc: String,
    val businessName: String,
    val logo: String = "",
    val percentageIgv: Double = 0.18,
    val isEnabled: Boolean = false,
    val withStock: Boolean = false,
    val appMobil: Boolean = false,
)