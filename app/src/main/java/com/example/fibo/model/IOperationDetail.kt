package com.example.fibo.model

data class IOperationDetail(
    val id: Int,
    val tariff: ITariff,
    val typeAffectationId: Int=0, //ID tipo de afectacion
    val quantity: Double = 0.0, //Cantidad
    val unitValue: Double = 0.0, //Precio sin IGV
    val unitPrice: Double = 0.0, // Precio con IGV
    val discountPercentage: Double = 0.0, //Porcentaje de descuento
    val totalDiscount: Double = 0.0, // Total Descuento por Item
    val perceptionPercentage: Double = 0.0, // Porcentega Percepcion
    val totalPerception: Double = 0.0, //Total Percepcion
    val igvPercentage: Double = 0.0, // Porcentaje IGV
    val totalValue: Double = 0.0, // Subtotal Item
    val totalIgv: Double = 0.0, // Total IGV Item
    val totalAmount: Double = 0.0, // Total Item
    val totalToPay: Double = 0.0, // Total a pagar Item
)