package com.example.fibo.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.ui.screens.HomeScreen
import com.example.fibo.ui.screens.QrScannerScreen
import com.example.fibo.ui.screens.invoice.NewInvoiceScreen
import com.example.fibo.ui.screens.receipt.NewReceiptScreen
import com.example.fibo.viewmodels.AuthViewModel


@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val authViewModel: AuthViewModel = hiltViewModel()

    // Observar cambios en el estado de autenticación
    val isLoggedIn by preferencesManager.isLoggedIn.collectAsState(initial = false)

    // Determinar destino inicial
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.QrScanner.route

    // Usar NavGraph para configurar la navegación
    NavGraph(
        navController = navController,
        startDestination = startDestination,
        authViewModel = authViewModel
    )
}
//@Composable
//@RequiresApi(Build.VERSION_CODES.O)
//fun AppNavigation() {
//    val navController = rememberNavController()
//    val context = LocalContext.current
//    val preferencesManager = remember { PreferencesManager(context) }
//    val authViewModel: AuthViewModel = hiltViewModel()
//
//    // Observar cambios en el estado de autenticación
//    val isLoggedIn by preferencesManager.isLoggedIn.collectAsState(initial = false)
//
//    NavHost(
//        navController = navController,
//        startDestination = if (isLoggedIn) Screen.Home.route else Screen.QrScanner.route
//    ) {
//        // Pantallas de autenticación
//        composable(Screen.QrScanner.route) {
//            QrScannerScreen(
//                onScanSuccess = {
//                    navController.navigate(Screen.Home.route) {
//                        popUpTo(Screen.QrScanner.route) { inclusive = true }
//                    }
//                },
//                authViewModel = authViewModel
//            )
//        }
//
//        // Pantallas principales
//        composable(Screen.Home.route) {
//            HomeScreen(
//                navController = navController,
//                onLogout = {
//                    authViewModel.logout()
//                    navController.navigate(Screen.QrScanner.route) {
//                        popUpTo(navController.graph.id) { inclusive = true }
//                    }
//                }
//            )
//        }
//
//        // Pantallas de facturas y recibos
//        composable(Screen.NewInvoice.route) {
//            NewInvoiceScreen(
//                onBack = { navController.popBackStack() },
//                onInvoiceCreated = { invoiceId ->
//                    navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId.toInt()))
//                }
//            )
//        }
//
//        composable(Screen.NewReceipt.route) {
////            NewReceiptScreen(
////                onBack = { navController.popBackStack() },
////                onReceiptCreated = { receiptId ->
////                    navController.navigate(Screen.ReceiptDetail.createRoute(receiptId))
////                }
////            )
//        }
//
//        // Pantallas de detalle
//        Screen.InvoiceDetail.routeWithArgs?.let {
//            composable(
//                route = it,
//                arguments = Screen.InvoiceDetail.arguments
//            ) { backStackEntry ->
//                val invoiceId = backStackEntry.arguments?.getString(Screen.InvoiceDetail.invoiceIdArg)?.toIntOrNull() ?: 0
//                // Pantalla de detalle de factura
//            }
//        }
//
//        Screen.ReceiptDetail.routeWithArgs?.let {
//            composable(
//                route = it,
//                arguments = Screen.ReceiptDetail.arguments
//            ) { backStackEntry ->
//                val receiptId = backStackEntry.arguments?.getString(Screen.ReceiptDetail.receiptIdArg)?.toIntOrNull() ?: 0
//                // Pantalla de detalle de recibo
//            }
//        }
//    }
//}

