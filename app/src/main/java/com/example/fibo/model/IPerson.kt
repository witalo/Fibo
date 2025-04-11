package com.example.fibo.model

data class IPerson(
    val id: Int = 0,
    val names: String? = null,
    val documentNumber: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
) {
    // Puedes añadir funciones de ayuda si lo necesitas
    fun getFullIdentification(): String {
        return "$names - $documentNumber"
    }
}