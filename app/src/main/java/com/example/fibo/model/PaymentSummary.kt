package com.example.fibo.model

// Modelo para el resumen de pagos
data class PaymentSummary(
    val totalAmount: Double,
    val totalPaid: Double,
    val remaining: Double
) {
    val isComplete: Boolean get() = remaining <= 0.01 // Margen para errores de redondeo
}

// Lista de métodos de pago disponibles (según tu backend)
object PaymentMethods {
    val AVAILABLE_METHODS = listOf(
        PaymentMethod(1, "EFECTIVO [CONTADO]"),
        PaymentMethod(2, "TARJETA DÉBITO [CONTADO]"),
        PaymentMethod(3, "TARJETA CRÉDITO [CONTADO]"),
        PaymentMethod(4, "TRANSFERENCIA [CONTADO]"),
        PaymentMethod(5, "GIRO [CONTADO]"),
        PaymentMethod(6, "CHEQUE [CONTADO]"),
        PaymentMethod(7, "CUPÓN [CONTADO]"),
        PaymentMethod(8, "YAPE [CONTADO]"),
        PaymentMethod(9, "POR PAGAR [CRÉDITO]", isCredit = true),
        PaymentMethod(10, "OTROS [CONTADO]")
    )

    fun getMethodById(id: Int): PaymentMethod? {
        return AVAILABLE_METHODS.find { it.id == id }
    }
}