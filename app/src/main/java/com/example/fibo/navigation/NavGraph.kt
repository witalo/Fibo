package com.example.fibo.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.ui.screens.HomeScreen
import com.example.fibo.ui.screens.NoteOfSaleScreen
import com.example.fibo.ui.screens.QrScannerScreen
import com.example.fibo.ui.screens.QuotationScreen
import com.example.fibo.viewmodels.AuthViewModel
import com.example.fibo.ui.screens.invoice.NewInvoiceScreen
import com.example.fibo.ui.screens.noteofsale.NewNoteOfSaleScreen
import com.example.fibo.ui.screens.product.ProductScreen
import com.example.fibo.ui.screens.profile.ProfileScreen
import com.example.fibo.ui.screens.quotation.NewQuotationScreen
import com.example.fibo.ui.screens.receipt.NewReceiptScreen

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pantallas de autenticación
        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onScanSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.QrScanner.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        // Pantallas principales
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // Pantallas de cotizaciones
        composable(Screen.Quotation.route) {
            QuotationScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        // Pantallas de nota de salida
        composable(Screen.NoteOfSale.route) {
            NoteOfSaleScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // Perfil
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                preferencesManager = PreferencesManager(LocalContext.current)
            )
        }
        // Productos
        composable(Screen.Product.route) {
            ProductScreen(
                onBack  = { navController.popBackStack() },
                onAddProduct = {
                    // TODO: Navegar a pantalla de agregar producto
                    // navController.navigate(Screen.AddProduct.route)
                }
            )
        }

        // Pantallas de facturas y recibos
        composable(Screen.NewInvoice.route) {
            NewInvoiceScreen(
                onBack = { navController.popBackStack() },
                onInvoiceCreated = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId.toInt()))
                }
            )
        }
        composable(
            route = Screen.NewInvoice.routeWithArgs ?: Screen.NewInvoice.route,
            arguments = Screen.NewInvoice.arguments
        ) { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString(Screen.NewInvoice.quotationIdArg)?.toIntOrNull()

            NewInvoiceScreen(
                onBack = { navController.popBackStack() },
                onInvoiceCreated = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId.toInt()))
                },
                quotationId = quotationId // Pasamos el ID de la cotización
            )
        }

        composable(Screen.NewReceipt.route) {
            NewReceiptScreen(
                onBack = { navController.popBackStack() },
                onReceiptCreated = { receiptId ->
                    navController.navigate(Screen.ReceiptDetail.createRoute(receiptId.toInt()))
                }
            )
        }
        composable(
            route = Screen.NewReceipt.routeWithArgs ?: Screen.NewReceipt.route,
            arguments = Screen.NewReceipt.arguments
        ) { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString(Screen.NewReceipt.quotationIdArg)?.toIntOrNull()

            NewReceiptScreen(
                onBack = { navController.popBackStack() },
                onReceiptCreated = { receiptId ->
                    navController.navigate(Screen.ReceiptDetail.createRoute(receiptId.toInt()))
                },
                quotationId = quotationId // Pasamos el ID de la cotización
            )
        }
        // Pantallas de facturas y recibos
        composable(Screen.NewQuotation.route) {
            NewQuotationScreen(
                onBack = { navController.popBackStack() },
                onQuotationCreated = { quotationId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(quotationId.toInt()))
                }
            )
        }
        // Pantallas de venta interna
        composable(Screen.NewNoteOfSale.route) {
            NewNoteOfSaleScreen(
                onBack = { navController.popBackStack() },
                onNoteOfSaleCreated = { noteId ->
                    navController.navigate(Screen.NoteOfSaleDetail.createRoute(noteId.toInt()))
                }
            )
        }

        // Pantallas de detalle
        Screen.InvoiceDetail.routeWithArgs?.let {
            composable(
                route = it,
                arguments = Screen.InvoiceDetail.arguments
            ) { backStackEntry ->
                val invoiceId = backStackEntry.arguments?.getString(Screen.InvoiceDetail.invoiceIdArg)?.toIntOrNull() ?: 0
                // InvoiceDetailScreen(
                //     invoiceId = invoiceId,
                //     onBack = { navController.popBackStack() }
                // )
            }
        }

        Screen.ReceiptDetail.routeWithArgs?.let {
            composable(
                route = it,
                arguments = Screen.ReceiptDetail.arguments
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getString(Screen.ReceiptDetail.receiptIdArg)?.toIntOrNull() ?: 0
                // ReceiptDetailScreen(
                //     receiptId = receiptId,
                //     onBack = { navController.popBackStack() }
                // )
            }
        }
        // Pantallas de detalle
        Screen.QuotationDetail.routeWithArgs?.let {
            composable(
                route = it,
                arguments = Screen.QuotationDetail.arguments
            ) { backStackEntry ->
                val quotationId = backStackEntry.arguments?.getString(Screen.QuotationDetail.quotationIdArg)?.toIntOrNull() ?: 0
                // InvoiceDetailScreen(
                //     invoiceId = invoiceId,
                //     onBack = { navController.popBackStack() }
                // )
            }
        }
        // Pantallas de detalle
        Screen.NoteOfSaleDetail.routeWithArgs?.let {
            composable(
                route = it,
                arguments = Screen.NoteOfSaleDetail.arguments
            ) { backStackEntry ->
                val noteOfSaleId = backStackEntry.arguments?.getString(Screen.NoteOfSaleDetail.noteOfSaleIdArg)?.toIntOrNull() ?: 0
                // InvoiceDetailScreen(
                //     invoiceId = invoiceId,
                //     onBack = { navController.popBackStack() }
                // )
            }
        }
    }
}
