package com.example.fibo.model

data class ISerialAssigned(
    val id: Int = 0,
    val serial: String = "",
    val documentType: String = "",
    val documentTypeReadable: String = "",
    val isGeneratedViaApi: Boolean = false
)