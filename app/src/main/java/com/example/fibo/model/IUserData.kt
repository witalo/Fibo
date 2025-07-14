package com.example.fibo.model

data class IUserData(
    val success: Boolean,
    val message: String,
    val jwtToken: String? = null,
    val refreshToken: String? = null,
    val company: ICompany? = null,
    val subsidiary: ISubsidiary? = null,
    val user: IUser? = null
)