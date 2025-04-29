package com.example.fibo.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(
    val route: String,
    val routeWithArgs: String? = null,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    // Autenticación
    object QrScanner : Screen("qr_scanner")
    // Principal
    object Home : Screen("home")
    object Quotation : Screen("quotation")
    object NotesOfSale : Screen("notes_of_sale")
    object Profile : Screen("new_profile")
    object NewInvoice : Screen("new_invoice")
    object NewReceipt : Screen("new_receipt")
    object NewQuotation : Screen("new_quotation")
    object NewNotesOfSale : Screen("new_notes_of_sale")

    // Detalles
    object InvoiceDetail : Screen(
        route = "invoice",
        routeWithArgs = "invoice/{invoiceId}",
        arguments = listOf(
            navArgument("invoiceId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(invoiceId: Int) = "invoice/$invoiceId"
        const val invoiceIdArg = "invoiceId"
    }

    object ReceiptDetail : Screen(
        route = "receipt",
        routeWithArgs = "receipt/{receiptId}",
        arguments = listOf(
            navArgument("receiptId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(receiptId: Int) = "receipt/$receiptId"
        const val receiptIdArg = "receiptId"
    }
}