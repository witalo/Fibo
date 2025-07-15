package com.example.fibo.model

data class ICompany(
    val id: Int,
    val doc: String,
    val businessName: String,
    val address: String = "",
    val logo: String = "",
    val percentageIgv: Double = 0.18,
    val isEnabled: Boolean = false,
    val withStock: Boolean = false,
    val appMobil: Boolean = false,
    val disableContinuePay: Boolean = false,
) {
    // Constructor secundario para compatibilidad con versiones anteriores
    constructor(id: Int, doc: String, businessName: String) : this(
        id = id,
        doc = doc,
        businessName = businessName,
        logo = "",
        percentageIgv = 0.0,
        isEnabled = true
    )
}