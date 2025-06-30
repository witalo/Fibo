package com.example.fibo.model

data class IQuota(
    val id: Int = 0,
    val paymentDate: String = "", // ISO date string
    val number: Int = 1,
    val total: Double = 0.0
)