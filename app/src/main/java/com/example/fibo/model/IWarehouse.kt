package com.example.fibo.model

data class IWarehouse(
    val id: Int = 0,
    val name: String = "",
    val subsidiary: ISubsidiary? = null,
    val category: String = "NA"
) {
    val categoryLabel: String
        get() = when (category) {
            "01" -> "Venta"
            "02" -> "Vehículo"
            "03" -> "Reserva"
            else -> "No Aplica"
        }
}