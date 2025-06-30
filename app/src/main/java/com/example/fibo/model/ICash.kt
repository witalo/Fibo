package com.example.fibo.model

data class ICash(
    val id: Int = 0,
    val subsidiaryId: Int? = null,
    val employeeId: Int? = null,
    val supplierId: Int? = null,
    val clientId: Int? = null,
    val name: String? = null,
    val accountNumber: String? = null,
    val accountType: String = AccountType.CASH.id,
    val currencyType: String = CurrencyType.PEN.id,
    val createdAt: String = "", // Will be formatted as ISO date string
    val isEnabled: Boolean = true
) {
    fun getAccountTypeDisplayName(): String {
        return AccountType.fromId(accountType).displayName
    }

    fun getCurrencyTypeDisplayName(): String {
        return CurrencyType.fromId(currencyType).displayName
    }
}

// WayPayChoices.kt
enum class WayPayChoices(val id: Int, val displayName: String) {
    CASH(1, "EFECTIVO [CONTADO]"),
    DEBIT_CARD(2, "TARJETA DÉBITO [CONTADO]"),
    CREDIT_CARD(3, "TARJETA CRÉDITO [CONTADO]"),
    TRANSFER(4, "TRANSFERENCIA [CONTADO]"),
    MONEY_ORDER(5, "GIRO [CONTADO]"),
    CHECK(6, "CHEQUE [CONTADO]"),
    COUPON(7, "CUPÓN [CONTADO]"),
    YAPE(8, "YAPE [CONTADO]"),
    TO_PAY(9, "POR PAGAR [CRÉDITO]"),
    OTHER(10, "OTROS [CONTADO]");

    companion object {
        fun fromId(id: Int): WayPayChoices = values().first { it.id == id }
    }
}

// DocumentTypeAttached.kt
enum class DocumentTypeAttached(val id: String, val displayName: String) {
    INVOICE("F", "Factura"),
    TICKET("B", "Boleta"),
    RECEIPT("T", "Ticket"),
    VOUCHER("V", "Vale"),
    OTHER("O", "Otro");

    companion object {
        fun fromId(id: String): DocumentTypeAttached = values().first { it.id == id }
    }
}

// TransactionType.kt
enum class TransactionType(val id: String, val displayName: String) {
    OPENING("A", "Apertura"),
    CLOSING("C", "Cierre"),
    INCOME("E", "Entrada"),
    OUTCOME("S", "Salida"),
    CASH_TO_CASH("TCC", "Transferencia de Caja a Caja"),
    CASH_TO_BANK("TCB", "Transferencia de Caja a Banco"),
    BANK_TO_BANK("TBB", "Transferencia de Banco a Banco"),
    BANK_TO_CASH("TBC", "Transferencia de Banco a Caja"),
    DEPOSIT("D", "Deposito"),
    WITHDRAWAL("R", "Retiro"),
    DEBIT("DC", "DEBE"),
    CREDIT("HC", "HABER");

    companion object {
        fun fromId(id: String): TransactionType = values().first { it.id == id }
    }
}

// AccountType.kt
enum class AccountType(val id: String, val displayName: String) {
    CASH("C", "CAJA"),
    BANK("B", "BANCO"),
    RECEIVABLE("CC", "CUENTA POR COBRAR"),
    PAYABLE("CP", "CUENTA POR PAGAR");

    companion object {
        fun fromId(id: String): AccountType = values().first { it.id == id }
    }
}

// CurrencyType.kt
enum class CurrencyType(val id: String, val displayName: String) {
    PEN("PEN", "Soles"),
    USD("USD", "Dolares"),
    EUR("EUR", "Euros");

    companion object {
        fun fromId(id: String): CurrencyType = values().first { it.id == id }
    }
}
