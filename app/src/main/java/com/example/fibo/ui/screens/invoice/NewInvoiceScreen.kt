package com.example.fibo.ui.screens.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.viewmodels.NewInvoiceViewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    onBack: () -> Unit,
    onInvoiceCreated: (String) -> Unit,
    viewModel: NewInvoiceViewModel = hiltViewModel()
) { 

    var invoiceData by remember { mutableStateOf(InvoiceFormData()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Factura") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black, // Fondo negro
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Formulario de factura
            OutlinedTextField(
                value = invoiceData.clientName, // Corregido: `clientName` en lugar de `invoiceNumber`
                onValueChange = { invoiceData = invoiceData.copy(clientName = it) },
                label = { Text("Nombre del cliente") }
            )

            // Más campos del formulario

            Button(
                onClick = {
                    viewModel.createInvoice(invoiceData) { invoiceId ->
                        onInvoiceCreated(invoiceId.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Factura")
            }
        }
    }
}

data class InvoiceFormData(
    val invoiceNumber: String = "",
    val clientName: String = "",
    val customerName: String = "",
    val amount: Double = 0.0
)