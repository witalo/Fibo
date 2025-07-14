package com.example.fibo.model

data class IPerson(
    val id: Int = 0,
    val names: String? = null,
    val documentType: String? = null,
    val documentNumber: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val fullNames: String = "",
    val driverLicense: String = "",
) {
    constructor(id: Int, names: String, documentType: String, documentNumber: String, email: String, phone: String, address: String) : this(
        id = id,
        names = names,
        documentType = documentType,
        documentNumber = documentNumber,
        email = email,
        phone = phone,
        address = address,
        fullNames = "",
        driverLicense = ""
    )
    // Puedes añadir funciones de ayuda si lo necesitas
    fun getFullIdentification(): String {
        return "$names - $documentNumber"
    }
}