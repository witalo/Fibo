package com.example.fibo.model

data class IUser(
    val id: Int,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val isActive: Boolean,
    val mobileDescription: String,
    val jwtToken: String,
    val refreshToken: String
) {

    // Constructor secundario para compatibilidad con versiones anteriores
    constructor(id: Int) : this(
        id = id,
        username = "",
        email = "",
        firstName = "",
        lastName = "",
        role = "",
        isActive = true,
        mobileDescription = "",
        jwtToken = "",
        refreshToken = ""
    )
    constructor(id: Int, jwtToken: String, refreshToken: String) : this(
        id = id,
        username = "",
        email = "",
        firstName = "",
        lastName = "",
        role = "",
        isActive = true,
        mobileDescription = "",
        jwtToken = jwtToken,
        refreshToken = refreshToken
    )
}