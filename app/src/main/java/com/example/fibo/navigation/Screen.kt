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
    object NoteOfSale : Screen("note_of_sale")
    object Profile : Screen("new_profile")
//    object NewInvoice : Screen("new_invoice")
//    object NewReceipt : Screen("new_receipt")
    object NewQuotation : Screen("new_quotation")
    object NewNoteOfSale : Screen("new_note_of_sale")
    object Product : Screen("product")
    object NewInvoice : Screen(
        route = "new_invoice",
        routeWithArgs = "new_invoice/{quotationId}",
        arguments = listOf(
            navArgument("quotationId") {
                type = NavType.StringType
            }
        ) // <-- Este paréntesis estaba faltando
    ) { // <-- Ahora sí se cierra correctamente
        fun createRoute(quotationId: Int) = "new_invoice/$quotationId"
        const val quotationIdArg = "quotationId"
    }
    object NewReceipt : Screen(
        route = "new_receipt",
        routeWithArgs = "new_receipt/{quotationId}",
        arguments = listOf(
            navArgument("quotationId") {
                type = NavType.StringType
            }
        ) // <-- Este paréntesis estaba faltando
    ) { // <-- Ahora sí se cierra correctamente
        fun createRoute(quotationId: Int) = "new_receipt/$quotationId"
        const val quotationIdArg = "quotationId"
    }




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

    object QuotationDetail : Screen(
        route = "quotation",
        routeWithArgs = "quotation/{quotationId}",
        arguments = listOf(
            navArgument("quotationId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(quotationId: Int) = "quotation/$quotationId"
        const val quotationIdArg = "quotationId"
    }
    object NoteOfSaleDetail : Screen(
        route = "noteOfSale",
        routeWithArgs = "noteOfSale/{noteOfSaleId}",
        arguments = listOf(
            navArgument("noteOfSaleId") {
                type = NavType.StringType
            }
        )
    ) {
        fun createRoute(noteOfSaleId: Int) = "noteOfSale/$noteOfSaleId"
        const val noteOfSaleIdArg = "noteOfSaleId"
    }
}