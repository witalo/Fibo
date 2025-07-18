package com.example.fibo.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.fibo.model.ISubsidiary
import com.example.fibo.navigation.Screen

@Composable
fun AppScaffold(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    onLogout: () -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryData,
        onMenuItemSelected = { option ->
            when (option) {
                "Inicio" -> navController.navigate(Screen.Home.route)
                "Cotizaciones" -> navController.navigate(Screen.Quotation.route)
                "Nota de salida" -> navController.navigate(Screen.NoteOfSale.route)
                "Perfil" -> navController.navigate(Screen.Profile.route)
                "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
                "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
                "Nueva Cotización" -> navController.navigate(Screen.NewQuotation.route)
                "Productos" -> navController.navigate(Screen.Product.route)
                "Compras" -> navController.navigate(Screen.Purchase.route)
                "Nueva Nota de salida" -> navController.navigate(Screen.NewNoteOfSale.route)
                "Nueva Guía" -> {
                    Log.d("Navigation", "Intentando navegar a Nueva Guía")
                    navController.navigate(Screen.NewGuide.route)
                }
                "Guías" -> navController.navigate(Screen.Guides.route)
                "Reporte" -> navController.navigate(Screen.Reports.route)
                "Reporte Pagos" -> navController.navigate(Screen.ReportPayment.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBarWrapper(
                        topBar = topBar,
                        onMenuClick = { isMenuOpen = !isMenuOpen }
                    )
                },
                content = content
            )
        }
    )
}

@Composable
private fun AppTopBarWrapper(
    topBar: @Composable () -> Unit,
    onMenuClick: () -> Unit
) {
    // Esta función wrapper permite inyectar la función onMenuClick a cualquier TopBar
    CompositionLocalProvider(
        LocalMenuClickHandler provides onMenuClick
    ) {
        topBar()
    }
}

// CompositionLocal para pasar la función de menú
val LocalMenuClickHandler = compositionLocalOf<() -> Unit> { 
    error("MenuClickHandler not provided") 
} 