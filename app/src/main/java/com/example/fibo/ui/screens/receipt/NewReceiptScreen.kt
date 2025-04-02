package com.example.fibo.ui.screens.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.ui.screens.invoice.InvoiceFormData
import com.example.fibo.viewmodels.NewInvoiceViewModel
import com.example.fibo.viewmodels.NewReceiptViewModel

// NewReceiptScreen.kt
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NewReceiptScreen(
//            onBack: () -> Unit,
//            onInvoiceCreated: (String) -> Unit,
//            viewModel: NewReceiptViewModel = hiltViewModel()
//) {
//    var receiptData by remember { mutableStateOf(InvoiceFormData()) }
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Nueva Boleta") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        // Tu formulario de boleta aquí
//        Button(
//            onClick = {
////                viewModel.saveReceipt()
////                onSaveSuccess()
//            },
//            modifier = Modifier.padding(padding)
//        ) {
//            Text("Guardar Boleta")
//        }
//    }
//}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReceiptScreen(
    onBack: () -> Unit,
    onReceiptCreated: (String) -> Unit,
    viewModel: NewReceiptViewModel = hiltViewModel()
) {

    var receiptData by remember { mutableStateOf(ReceiptFormData()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Boleta") },
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
                value = receiptData.clientName, // Corregido: `clientName` en lugar de `invoiceNumber`
                onValueChange = { receiptData = receiptData.copy(clientName = it) },
                label = { Text("Nombre del cliente") }
            )

            // Más campos del formulario

            Button(
                onClick = {
                    viewModel.createReceipt(receiptData) { receiptId ->
                        onReceiptCreated(receiptId.toString())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Boleta")
            }
        }
    }
}

data class ReceiptFormData(
    val number: String = "",
    val clientName: String = "",
    val customerName: String = "",
    val amount: Double = 0.0
)