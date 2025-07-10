package com.example.fibo.model

data class ISubsidiary(
    val id: Int,
    val serial: String,
    val name: String,
    val address: String,
    val token: String
) {
    // Constructor secundario para compatibilidad con versiones anteriores
    constructor(id: Int, serial: String, name: String) : this(
        id = id,
        serial = serial,
        name = name,
        address = "",
        token = ""
    )
}