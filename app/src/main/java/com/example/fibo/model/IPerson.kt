package com.example.fibo.model

data class IPerson(
    val id: Int = 0,
    val names: String? = null,
    val code: String? = "",
    val shortName: String? = "",
    val documentType: String? = null,
    val documentNumber: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val country: String? = "PE",
    val fullNames: String = "",
    val economicActivityMain: Int = 0,
    val isEnabled: Boolean = true,
    val isSupplier: Boolean = false,
    val isClient: Boolean = false,
    val isDriver: Boolean = false,
    val driverLicense: String = "",
    val mtcRegistrationNumber: String? = null,
) {
    constructor(id: Int, names: String, documentType: String, documentNumber: String, email: String, phone: String, address: String, fullNames: String, driverLicense: String) : this(
        id = id,
        names = names,
        documentType = documentType,
        documentNumber = documentNumber,
        email = email,
        phone = phone,
        address = address,
        fullNames = fullNames,
        driverLicense = driverLicense,
        economicActivityMain = 0,
        isEnabled = true,
        isSupplier = false,
        isClient = false,
        mtcRegistrationNumber = null,
    )
    // Puedes añadir funciones de ayuda si lo necesitas
    fun getFullIdentification(): String {
        return "$names - $documentNumber"
    }
}