package com.example.fibo.ui.screens.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fibo.viewmodels.NewInvoiceViewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProductTariff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    onBack: () -> Unit,
    onInvoiceCreated: (String) -> Unit,
    viewModel: NewInvoiceViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var clientData by remember { mutableStateOf<IPerson?>(null) }
    var documentNumber by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }

    // Calcular totales
    val totalAmount = operationDetails.sumOf { it.totalAmount }
    val totalIgv = operationDetails.sumOf { it.totalIgv }
    val totalValue = operationDetails.sumOf { it.totalValue }

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
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // CARD CABECERA - Información del Cliente
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Información del Cliente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text("RUC / DNI") },
                            modifier = Modifier.weight(0.8f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                viewModel.fetchClientData(documentNumber) { person ->
                                    clientData = person
                                }
                            },
                            modifier = Modifier.height(45.dp)
                        ) {
                            Text("Extraer")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = clientData?.names ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
                        },
                        label = { Text("Denominación / Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = clientData?.address ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
                        },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // CARD CUERPO - Lista de productos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detalle de Productos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showAddItemDialog = true },
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar Item",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar Item")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (operationDetails.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay productos agregados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Encabezado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    "Producto",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Cant.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Precio",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(32.dp)) // Espacio para icono de eliminar
                            }

                            // Items
                            operationDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            detail.productTariff.productName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Código: ${detail.productTariff.productId}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        detail.quantity,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${detail.price}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    IconButton(
                                        onClick = {
                                            operationDetails = operationDetails.filter { it != detail }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            }

            // CARD FOOTER - Totales y botones
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Resumen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Subtotal:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalValue)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "IGV (18%):",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalIgv)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TOTAL:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                val operation = IOperation(
                                    id = 0, // El ID será asignado por el backend
                                    client = clientData ?: IPerson(),
                                    operationDetailSet = operationDetails,
                                    totalAmount = totalAmount,
                                    totalIgv = totalIgv,
                                    totalTaxed = totalValue,
                                    totalToPay = totalAmount
                                )
                                viewModel.createInvoice(operation) { operationId ->
                                    onInvoiceCreated(operationId.toString())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
                        ) {
                            Text("Emitir Factura")
                        }
                    }
                }
            }
        }

        // Diálogo para agregar producto
        if (showAddItemDialog) {
            AddProductDialog(
                onDismiss = { showAddItemDialog = false },
                onProductAdded = { newItem ->
                    operationDetails = operationDetails + newItem
                    showAddItemDialog = false
                },
                viewModel = viewModel
            )
        }

        // Indicador de carga
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Diálogo de error
        error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Aceptar")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (IOperationDetail) -> Unit,
    viewModel: NewInvoiceViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<IProductTariff?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("0.00") }
    var discount by remember { mutableStateOf("0.00") }

    val searchResults by viewModel.searchResults.collectAsState()

    // Cálculos
    val quantityValue = quantity.toDoubleOrNull() ?: 1.0
    val priceValue = price.toDoubleOrNull() ?: 0.0
    val discountValue = discount.toDoubleOrNull() ?: 0.0

    val igvPercentage = 0.18 // 18% IGV
    val unitValue = priceValue / (1 + igvPercentage) // Precio sin IGV
    val totalValue = unitValue * quantityValue // Valor total sin IGV
    val totalIgv = totalValue * igvPercentage // Total IGV
    val totalAmount = totalValue + totalIgv // Total con IGV
    val totalWithDiscount = totalAmount - discountValue // Total final con descuento

    // Efecto para actualizar búsqueda
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            viewModel.searchProducts(searchQuery)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 550.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Agregar Producto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Búsqueda de productos
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar por código o nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        },
                        singleLine = true
                    )

                    AnimatedVisibility(
                        visible = searchQuery.isNotEmpty() && searchResults.isNotEmpty() && selectedProduct == null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
                                )
                        ) {
//                            items(searchResults) { product ->
//                                Surface(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    onClick = {
//                                        selectedProduct = product
//                                        price = product.priceWithIgv.toString()
//                                        searchQuery = product.name
//                                    }
//                                ) {
//                                    Column(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(vertical = 8.dp, horizontal = 16.dp)
//                                    ) {
//                                        Text(
//                                            product.productName,
//                                            style = MaterialTheme.typography.bodyMedium
//                                        )
//                                        Text(
//                                            "Código: ${product.productId} - Precio: S/ ${String.format("%.2f", product.priceWithIgv)}",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                                        )
//                                    }
//                                }
//                                Divider()
//                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campos para detalles del producto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = discount,
                    onValueChange = { discount = it },
                    label = { Text("Descuento") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resumen
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text("S/ ${String.format("%.2f", totalValue)}")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IGV:")
                            Text("S/ ${String.format("%.2f", totalIgv)}")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Descuento:")
                            Text("S/ ${String.format("%.2f", discountValue)}")
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TOTAL:",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "S/ ${String.format("%.2f", totalWithDiscount)}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            selectedProduct?.let { product ->
                                val newDetail = IOperationDetail(
                                    id = 0, // El ID será asignado por el backend
                                    productTariff = product,
                                    quantity = quantity,
                                    price = price,
                                    unitValue = unitValue,
                                    unitPrice = priceValue,
                                    totalDiscount = discountValue,
                                    discountPercentage = if (totalAmount > 0) (discountValue / totalAmount) * 100 else 0.0,
                                    igvPercentage = igvPercentage * 100,
                                    totalIgv = totalIgv,
                                    totalValue = totalValue,
                                    totalAmount = totalAmount,
                                    totalToPay = totalWithDiscount
                                )
                                onProductAdded(newDetail)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedProduct != null
                    ) {
                        Text("Agregar")
                    }
                }
            }
        }
    }
}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NewInvoiceScreen(
//    onBack: () -> Unit,
//    onInvoiceCreated: (String) -> Unit,
//    viewModel: NewInvoiceViewModel = hiltViewModel()
//) {
//
//    var invoiceData by remember { mutableStateOf(InvoiceFormData()) }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Nueva Factura") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.Black, // Fondo negro
//                    titleContentColor = Color.White,
//                    navigationIconContentColor = Color.White,
//                    actionIconContentColor = Color.White
//                )
//            )
//        }
//    ) { padding ->
//        Column(modifier = Modifier.padding(padding)) {
//            // Formulario de factura
//            OutlinedTextField(
//                value = invoiceData.clientName, // Corregido: `clientName` en lugar de `invoiceNumber`
//                onValueChange = { invoiceData = invoiceData.copy(clientName = it) },
//                label = { Text("Nombre del cliente") }
//            )
//
//            // Más campos del formulario
//
//            Button(
//                onClick = {
//                    viewModel.createInvoice(invoiceData) { invoiceId ->
//                        onInvoiceCreated(invoiceId.toString())
//                    }
//                },
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Crear Factura")
//            }
//        }
//    }
//}
//
//data class InvoiceFormData(
//    val invoiceNumber: String = "",
//    val clientName: String = "",
//    val customerName: String = "",
//    val amount: Double = 0.0
//)