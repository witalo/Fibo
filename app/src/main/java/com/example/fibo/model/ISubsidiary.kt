package com.example.fibo.model

data class ISubsidiary(
    val id: Int,
    val serial: String,
    val name: String,
    val address: String,
    val geographicLocationByDistrict: String,
    val token: String,
    val company: ICompany? = null
) {
    // Constructor secundario para compatibilidad con versiones anteriores
    constructor(id: Int, serial: String, name: String) : this(
        id = id,
        serial = serial,
        name = name,
        address = "",
        geographicLocationByDistrict = "",
        token = ""
    )
    constructor(id: Int, serial: String, name: String, address: String, token: String) : this(
        id = id,
        serial = serial,
        name = name,
        address = address,
        geographicLocationByDistrict = "",
        token = token
    )
}