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
import com.example.fibo.ui.screens.reportpayment.ReportPaymentScreen
import com.example.fibo.ui.screens.reports.ReportScreen
import com.example.fibo.utils.DeviceUtils
import com.example.fibo.ui.screens.GuideScreen
import com.example.fibo.ui.screens.guide.NewGuideScreen
import com.example.fibo.ui.screens.GuideListScreen
import com.example.fibo.ui.screens.purchase.PurchaseScreen
import com.example.fibo.ui.screens.purchase.NewPurchaseScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.widget.Toast
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.fibo.ui.screens.person.NewPersonScreen
import com.example.fibo.ui.screens.person.PersonScreen
import com.example.fibo.ui.screens.product.NewProductScreen

@Composable
@RequiresApi(Build.VERSION_CODES.O)
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel
) {
    // Obtener subsidiaryData una vez para todas las pantallas
    val preferencesManager = PreferencesManager(LocalContext.current)
    val subsidiaryData by preferencesManager.subsidiaryData.collectAsState()
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Pantallas de autenticación
        composable(Screen.QrScanner.route) {
            val context = LocalContext.current
            val scanResult by authViewModel.scanResult.collectAsState()
            val isLoading by authViewModel.isLoading.collectAsState()
            val error by authViewModel.error.collectAsState()
            
            // Limpiar estados al cargar la pantalla
            LaunchedEffect(Unit) {
                authViewModel.clearStates()
            }
            
            // Observar el resultado de la autenticación
            LaunchedEffect(scanResult) {
                if (scanResult?.data?.qrScan?.success == true) {
                    // Solo navegar cuando la autenticación sea exitosa
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.QrScanner.route) { inclusive = true }
                    }
                }
            }
            
            // Mostrar errores si los hay
            LaunchedEffect(error) {
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
            }
            
            QrScannerScreen(
                onQrCodeScanned = { qrCode ->
                    // Solo iniciar el proceso de autenticación
                    authViewModel.scanQr(
                        qrCode,
                        DeviceUtils.getDeviceId(context),
                        DeviceUtils.getDeviceDescription(context)
                    )
                }
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
                subsidiaryData = subsidiaryData,
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
                subsidiaryData = subsidiaryData,
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
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                preferencesManager = preferencesManager
            )
        }
        // Productos
//        composable(Screen.Product.route) {
//            ProductScreen(
//                navController = navController,
//                onBack = { navController.popBackStack() },
//                onAddProduct = {
//                    // TODO: Navegar a pantalla de agregar producto
//                    // navController.navigate(Screen.AddProduct.route)
//                },
//                subsidiaryData = subsidiaryData,
//                onLogout = {
//                    authViewModel.logout()
//                    navController.navigate(Screen.QrScanner.route) {
//                        popUpTo(navController.graph.id) { inclusive = true }
//                    }
//                }
//            )
//        }

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
        composable(
            route = Screen.NewNoteOfSale.routeWithArgs ?: Screen.NewNoteOfSale.route,
            arguments = Screen.NewNoteOfSale.arguments
        ) { backStackEntry ->
            val quotationId = backStackEntry.arguments?.getString(Screen.NewNoteOfSale.quotationIdArg)?.toIntOrNull()

            NewNoteOfSaleScreen(
                onBack = { navController.popBackStack() },
                onNoteOfSaleCreated = { receiptId ->
                    navController.navigate(Screen.NoteOfSaleDetail.createRoute(receiptId.toInt()))
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
        composable(Screen.NewGuide.route) {
            NewGuideScreen(navController = navController)
        }
        composable(Screen.NewPerson.route) {
            NewPersonScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                personId = null // null = crear nueva persona
            )
        }

        // ✅ Ruta para editar persona existente
        composable(
            route = Screen.EditPerson.route,
            arguments = listOf(
                navArgument("personId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getInt("personId")
            NewPersonScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                personId = personId
            )
        }

        // ✅ Ruta para detalle de persona (opcional)
        composable(
            route = Screen.PersonDetail.route,
            arguments = listOf(
                navArgument("personId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getInt("personId")
            // PersonDetailScreen(personId = personId, navController = navController)
        }
        // Guías
        composable(Screen.Guide.route) {
            GuideScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Guides.route) {
            GuideListScreen(navController = navController)
        }
        // Ruta para la lista de productos
        composable(
            route = "product"
        ) {
            ProductScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        // Ruta para crear nuevo producto
        composable(
            route = "newProduct"
        ) {
            NewProductScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                productId = null // null = crear nuevo producto
            )
        }

        // Ruta para editar producto existente
        composable(
            route = "editProduct/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: 0
            NewProductScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                productId = productId // ID del producto a editar
            )
        }

        // Pantallas de compras
        composable(Screen.Purchase.route) {
            PurchaseScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.NewPurchase.route) {
            NewPurchaseScreen(
                onBack = { navController.popBackStack() },
                onPurchaseCreated = { purchaseId ->
                    navController.navigate(Screen.PurchaseDetail.createRoute(purchaseId.toInt()))
                }
            )
        }
        composable(Screen.Reports.route) {
            ReportScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ReportPayment.route) {
            ReportPaymentScreen(
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Person.route) {
            PersonScreen(
                navController = navController,
                subsidiaryData = subsidiaryData,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.QrScanner.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
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

        Screen.PurchaseDetail.routeWithArgs?.let {
            composable(
                route = it,
                arguments = Screen.PurchaseDetail.arguments
            ) { backStackEntry ->
                val purchaseId = backStackEntry.arguments?.getString(Screen.PurchaseDetail.purchaseIdArg)?.toIntOrNull() ?: 0
                // PurchaseDetailScreen(
                //     purchaseId = purchaseId,
                //     onBack = { navController.popBackStack() }
                // )
            }
        }
    }
}
