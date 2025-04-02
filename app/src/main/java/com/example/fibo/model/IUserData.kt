package com.example.fibo.model

data class IUserData(
    val success: Boolean,
    val message: String,
    val company: ICompany?,
    val subsidiary: ISubsidiary?,
    val user: IUser?
)