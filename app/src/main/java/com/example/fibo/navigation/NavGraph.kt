package com.example.fibo.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fibo.ui.screens.HomeScreen
import com.example.fibo.ui.screens.QrScannerScreen
import com.example.fibo.viewmodels.AuthViewModel
import com.example.fibo.ui.screens.invoice.NewInvoiceScreen
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

        // Pantallas de facturas y recibos
        composable(Screen.NewInvoice.route) {
            NewInvoiceScreen(
                onBack = { navController.popBackStack() },
                onInvoiceCreated = { invoiceId ->
                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId.toInt()))
                }
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
    }
}
