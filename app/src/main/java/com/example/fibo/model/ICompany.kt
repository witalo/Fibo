package com.example.fibo.model

data class ICompany(
    val id: Int,
    val doc: String,
    val businessName: String,
    val percentageIgv: Double = 0.18
)