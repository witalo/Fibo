package com.example.fibo.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
