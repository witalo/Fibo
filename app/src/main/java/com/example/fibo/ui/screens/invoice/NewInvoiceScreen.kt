package com.example.fibo.ui.screens.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.IProductTariff
import com.example.fibo.utils.ColorGradients
import com.example.fibo.viewmodels.ProductSearchState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    onBack: () -> Unit,
    onInvoiceCreated: (String) -> Unit,
    viewModel: NewInvoiceViewModel = hiltViewModel()
) {
    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
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
                title = { Text("Nueva Factura", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
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
                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding más ajustado
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // CARD CABECERA - Información del Cliente
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp), // Espacio reducido
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp) // Bordes redondeados
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp) // Padding interno reducido
                        .fillMaxWidth()
                ) {
                    Text(
                        "Información del Cliente",
                        style = MaterialTheme.typography.titleSmall, // Texto más pequeño
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp) // Espacio reducido
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text("RUC/DNI", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .background(
                                    brush = ColorGradients.blueButtonGradient,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.fetchClientData(documentNumber) { person ->
                                        clientData = person
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Extraer",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientData?.names ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
                        },
                        label = { Text("Nombre", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientData?.address ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
                        },
                        label = { Text("Dirección", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // CARD CUERPO - Lista de productos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detalle de Productos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = MaterialTheme.colorScheme.primary
                                )
                                .background(
                                    brush = ColorGradients.blueButtonGradient,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { showAddItemDialog = true }
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Item",
                                    modifier = Modifier.size(18.dp),  // Tamaño ligeramente mayor
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))  // Espacio un poco mayor
                                Text(
                                    "Agregar",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold  // Texto en negrita
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (operationDetails.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp), // Padding reducido
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay productos agregados",
                                style = MaterialTheme.typography.bodySmall, // Texto más pequeño
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Encabezado más compacto
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    "Producto",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    "Cant.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Precio",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                            }

                            // Items más compactos
                            operationDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            detail.productTariff.productName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Código: ${detail.productTariff.productId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        detail.quantity,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(40.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${detail.price}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End
                                    )
                                    IconButton(
                                        onClick = {
                                            operationDetails = operationDetails.filter { it != detail }
                                        },
                                        modifier = Modifier.size(28.dp) // Tamaño reducido
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp) // Icono más pequeño
                                        )
                                    }
                                }
                                Divider(thickness = 0.5.dp) // Línea más fina
                            }
                        }
                    }
                }
            }

            // CARD FOOTER - Totales y botones
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Resumen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Subtotal:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalValue)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "IGV (18%):",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalIgv)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TOTAL:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = ColorGradients.goldLuxury
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones modernos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient)
                        ) {
                            Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                        }

                        Button(
                            onClick = {
                                val operation = IOperation(
                                    id = 0,
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
                        ) {
                            Text("Emitir Factura", style = MaterialTheme.typography.labelLarge)
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
                viewModel = viewModel,
                subsidiaryId = subsidiaryData?.id ?: 0
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
                title = { Text("Error", style = MaterialTheme.typography.titleSmall) },
                text = { Text(errorMessage, style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearError() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Aceptar", style = MaterialTheme.typography.labelLarge)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (IOperationDetail) -> Unit,
    viewModel: NewInvoiceViewModel,
    subsidiaryId: Int = 0
) {
    var localQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0.00") }
    val price = remember(selectedProduct) {
        selectedProduct?.priceWithIgv3?.toString() ?: "0.00"
    }

    // Cálculos optimizados
    val (totalValue, totalIgv, totalWithDiscount) = remember(quantity, price, discount) {
        val qty = quantity.toDoubleOrNull() ?: 1.0
        val priceVal = price.toDoubleOrNull() ?: 0.0
        val discountVal = discount.toDoubleOrNull() ?: 0.0
        val igvPercentage = 0.18

        val unitValue = priceVal / (1 + igvPercentage)
        val subtotal = unitValue * qty
        val igv = subtotal * igvPercentage
        val total = (subtotal + igv) - discountVal

        Triple(subtotal, igv, total)
    }

    // Búsqueda con debounce
    LaunchedEffect(localQuery) {
        if (localQuery.length >= 3) {
            delay(350) // Tiempo de debounce
            viewModel.searchProducts(localQuery, subsidiaryId)
        } else {
            viewModel.clearProductSelection()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header con título y botón de cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Agregar Producto",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de búsqueda mejorado
                SearchTextField(
                    query = localQuery,
                    onQueryChange = { localQuery = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resultados de búsqueda con animación
                AnimatedVisibility(
                    visible = localQuery.isNotEmpty() && selectedProduct == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    when (searchState) {
                        is ProductSearchState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }
                        }

                        is ProductSearchState.Success -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 250.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                items(searchResults) { product ->
                                    ProductSuggestionItem(
                                        product = product,
                                        onClick = {
                                            viewModel.selectProduct(product)
                                        }
                                    )
                                    Divider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }

                        is ProductSearchState.Empty -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No se encontraron productos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        is ProductSearchState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Error: ${(searchState as ProductSearchState.Error).message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        ProductSearchState.Idle -> {
                            if (localQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Ingrese al menos 3 caracteres",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Detalles del producto seleccionado
                selectedProduct?.let { product ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Tarjeta de producto seleccionado
                        SelectedProductCard(product)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Campos de cantidad y descuento
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = { Text("Cantidad") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )

                            OutlinedTextField(
                                value = discount,
                                onValueChange = { discount = it },
                                label = { Text("Descuento") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Resumen de compra
                        PurchaseSummary(
                            subtotal = totalValue,
                            igv = totalIgv,
                            discount = discount.toDoubleOrNull() ?: 0.0,
                            total = totalWithDiscount
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón de agregar
                        Button(
                            onClick = {
//                                val detail = IOperationDetail(
//                                    id = 0,
////                                    productId = product.id,
////                                    productCode = product.code,
////                                    productName = product.name,
//                                    quantity = quantity.toDoubleOrNull() ?: 1.0,
//                                    unitPrice = product.priceWithIgv3,
//                                    totalValue = totalValue,
//                                    totalDiscount = discount.toDoubleOrNull() ?: 0.0,
//                                    totalIgv = totalIgv,
//                                    totalAmount = totalValue + totalIgv,
//                                    totalToPay = totalWithDiscount
//                                )
//                                onProductAdded(detail)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Text(
                                "Agregar Producto",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Componente de campo de búsqueda
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = { Text("Buscar por nombre o código") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
        )
    )
}
// Componente de ítem de producto en la lista
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSuggestionItem(
    product: IProduct,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen de producto (puedes reemplazar con un icono o imagen real)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Producto",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        "Código: ${product.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Stock: ${product.remainingQuantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                "S/ ${"%.2f".format(product.priceWithIgv3)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// Componente de tarjeta de producto seleccionado
@Composable
private fun SelectedProductCard(product: IProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Producto seleccionado",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Código: ${product.code}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Stock: ${product.remainingQuantity}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Precio: S/ ${"%.2f".format(product.priceWithIgv3)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

// Componente de resumen de compra
@Composable
private fun PurchaseSummary(
    subtotal: Double,
    igv: Double,
    discount: Double,
    total: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            "Resumen",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        PriceRow("Subtotal:", subtotal)
        PriceRow("IGV (18%):", igv)
        PriceRow("Descuento:", -discount)

        Divider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        )

        PriceRow(
            "TOTAL:",
            total,
            isBold = true,
            textColor = MaterialTheme.colorScheme.primary
        )
    }
}

// Componente de fila de precio
@Composable
private fun PriceRow(
    label: String,
    value: Double,
    isBold: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        Text(
            "S/ ${"%.2f".format(value)}",
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}
//@Composable
//fun AddProductDialog(
//    onDismiss: () -> Unit,
//    onProductAdded: (IOperationDetail) -> Unit,
//    viewModel: NewInvoiceViewModel,
//    subsidiaryId: Int = 0
//) {
//    var localQuery by remember { mutableStateOf("") }
//    val searchState by viewModel.searchState.collectAsState()
//    val selectedProduct by viewModel.selectedProduct.collectAsState()
//
//    var quantity by remember { mutableStateOf("1") }
//    var price by remember { mutableStateOf("0.00") }
//    var discount by remember { mutableStateOf("0.00") }
//
//    val searchResults by viewModel.searchResults.collectAsState()
//
//    // Cálculos
//    val quantityValue = quantity.toDoubleOrNull() ?: 1.0
//    val priceValue = price.toDoubleOrNull() ?: 0.0
//    val discountValue = discount.toDoubleOrNull() ?: 0.0
//
//    val igvPercentage = 0.18
//    val unitValue = priceValue / (1 + igvPercentage)
//    val totalValue = unitValue * quantityValue
//    val totalIgv = totalValue * igvPercentage
//    val totalAmount = totalValue + totalIgv
//    val totalWithDiscount = totalAmount - discountValue
//
//    // Sincronización con debounce
//    LaunchedEffect(localQuery) {
//        delay(350) // Debounce time
//        viewModel.searchProducts(localQuery, subsidiaryId)
//    }
//
//    Dialog(onDismissRequest = onDismiss) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .heightIn(max = 500.dp), // Altura reducida
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//            shape = RoundedCornerShape(12.dp) // Bordes más redondeados
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//            ) {
//                Text(
//                    "Agregar Producto",
//                    style = MaterialTheme.typography.titleSmall,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Búsqueda de productos
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    OutlinedTextField(
//                        value = searchQuery,
//                        onValueChange = { searchQuery = it },
//                        label = { Text("Buscar producto", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.fillMaxWidth(),
//                        leadingIcon = {
//                            Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(18.dp))
//                        },
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//
//                    AnimatedVisibility(
//                        visible = searchQuery.isNotEmpty() && searchResults.isNotEmpty() && selectedProduct == null,
//                        enter = fadeIn() + expandVertically(),
//                        exit = fadeOut() + shrinkVertically()
//                    ) {
//                        LazyColumn(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .heightIn(max = 180.dp) // Altura reducida
//                                .border(
//                                    width = 0.5.dp,
//                                    color = MaterialTheme.colorScheme.outline,
//                                    shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
//                                )
//                        ) {
////                            items(searchResults) { product ->
////                                Surface(
////                                    modifier = Modifier.fillMaxWidth(),
////                                    onClick = {
////                                        selectedProduct = product
////                                        price = product.priceWithIgv.toString()
////                                        searchQuery = product.name
////                                    },
////                                    color = MaterialTheme.colorScheme.surface
////                                ) {
////                                    Column(
////                                        modifier = Modifier
////                                            .fillMaxWidth()
////                                            .padding(vertical = 6.dp, horizontal = 12.dp)
////                                    ) {
////                                        Text(
////                                            product.productName,
////                                            style = MaterialTheme.typography.bodySmall,
////                                            maxLines = 1,
////                                            overflow = TextOverflow.Ellipsis
////                                        )
////                                        Text(
////                                            "Código: ${product.productId} - S/ ${String.format("%.2f", product.priceWithIgv)}",
////                                            style = MaterialTheme.typography.labelSmall,
////                                            color = MaterialTheme.colorScheme.onSurfaceVariant
////                                        )
////                                    }
////                                }
////                                Divider(thickness = 0.5.dp)
////                            }
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Campos para detalles del producto
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    OutlinedTextField(
//                        value = quantity,
//                        onValueChange = { quantity = it },
//                        label = { Text("Cantidad", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//
//                    OutlinedTextField(
//                        value = price,
//                        onValueChange = { price = it },
//                        label = { Text("Precio", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                OutlinedTextField(
//                    value = discount,
//                    onValueChange = { discount = it },
//                    label = { Text("Descuento", style = MaterialTheme.typography.labelSmall) },
//                    textStyle = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.fillMaxWidth(),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                    singleLine = true,
//                    shape = RoundedCornerShape(8.dp)
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Resumen
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .padding(12.dp)
//                            .fillMaxWidth()
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
//                            Text("S/ ${String.format("%.2f", totalValue)}", style = MaterialTheme.typography.bodySmall)
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("IGV:", style = MaterialTheme.typography.bodySmall)
//                            Text("S/ ${String.format("%.2f", totalIgv)}", style = MaterialTheme.typography.bodySmall)
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Descuento:", style = MaterialTheme.typography.bodySmall)
//                            Text("S/ ${String.format("%.2f", discountValue)}", style = MaterialTheme.typography.bodySmall)
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Divider(thickness = 0.5.dp)
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(
//                                "TOTAL:",
//                                style = MaterialTheme.typography.bodySmall,
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                "S/ ${String.format("%.2f", totalWithDiscount)}",
//                                style = MaterialTheme.typography.bodySmall,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Botones modernos
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier
//                            .weight(1f)
//                            .height(48.dp),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.outlinedButtonColors(
//                            contentColor = MaterialTheme.colorScheme.primary
//                        ),
//                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
//                    ) {
//                        Text("Cancelar", style = MaterialTheme.typography.labelLarge)
//                    }
//
//                    Button(
//                        onClick = {
//                            selectedProduct?.let { product ->
//                                val newDetail = IOperationDetail(
//                                    id = 0,
//                                    productTariff = product,
//                                    quantity = quantity,
//                                    price = price,
//                                    unitValue = unitValue,
//                                    unitPrice = priceValue,
//                                    totalDiscount = discountValue,
//                                    discountPercentage = if (totalAmount > 0) (discountValue / totalAmount) * 100 else 0.0,
//                                    igvPercentage = igvPercentage * 100,
//                                    totalIgv = totalIgv,
//                                    totalValue = totalValue,
//                                    totalAmount = totalAmount,
//                                    totalToPay = totalWithDiscount
//                                )
//                                onProductAdded(newDetail)
//                            }
//                        },
//                        modifier = Modifier
//                            .weight(1f)
//                            .height(48.dp),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary,
//                            contentColor = MaterialTheme.colorScheme.onPrimary
//                        ),
//                        elevation = ButtonDefaults.buttonElevation(
//                            defaultElevation = 2.dp,
//                            pressedElevation = 4.dp
//                        ),
//                        enabled = selectedProduct != null
//                    ) {
//                        Text("Agregar", style = MaterialTheme.typography.labelLarge)
//                    }
//                }
//            }
//        }
//    }
//}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NewInvoiceScreen(
//    onBack: () -> Unit,
//    onInvoiceCreated: (String) -> Unit,
//    viewModel: NewInvoiceViewModel = hiltViewModel()
//) {
//    val isLoading by viewModel.isLoading.collectAsState()
//    val error by viewModel.error.collectAsState()
//
//    var clientData by remember { mutableStateOf<IPerson?>(null) }
//    var documentNumber by remember { mutableStateOf("") }
//    var showAddItemDialog by remember { mutableStateOf(false) }
//    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }
//
//    // Calcular totales
//    val totalAmount = operationDetails.sumOf { it.totalAmount }
//    val totalIgv = operationDetails.sumOf { it.totalIgv }
//    val totalValue = operationDetails.sumOf { it.totalValue }
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
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .padding(16.dp)
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//        ) {
//            // CARD CABECERA - Información del Cliente
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 16.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Información del Cliente",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 16.dp)
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        OutlinedTextField(
//                            value = documentNumber,
//                            onValueChange = { documentNumber = it },
//                            label = { Text("RUC / DNI") },
//                            modifier = Modifier.weight(0.8f),
//                            singleLine = true
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Button(
//                            onClick = {
//                                viewModel.fetchClientData(documentNumber) { person ->
//                                    clientData = person
//                                }
//                            },
//                            modifier = Modifier.height(45.dp)
//                        ) {
//                            Text("Extraer")
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.names ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
//                        },
//                        label = { Text("Denominación / Nombre") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.address ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
//                        },
//                        label = { Text("Dirección") },
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true
//                    )
//                }
//            }
//
//            // CARD CUERPO - Lista de productos
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 16.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "Detalle de Productos",
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        Button(
//                            onClick = { showAddItemDialog = true },
//                            contentPadding = PaddingValues(horizontal = 16.dp)
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Add,
//                                contentDescription = "Agregar Item",
//                                modifier = Modifier.size(16.dp)
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text("Agregar Item")
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    if (operationDetails.isEmpty()) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 32.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                "No hay productos agregados",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    } else {
//                        Column(modifier = Modifier.fillMaxWidth()) {
//                            // Encabezado
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .background(MaterialTheme.colorScheme.surfaceVariant)
//                                    .padding(vertical = 8.dp, horizontal = 8.dp)
//                            ) {
//                                Text(
//                                    "Producto",
//                                    style = MaterialTheme.typography.labelMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.weight(1f)
//                                )
//                                Text(
//                                    "Cant.",
//                                    style = MaterialTheme.typography.labelMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(60.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Precio",
//                                    style = MaterialTheme.typography.labelMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(80.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Total",
//                                    style = MaterialTheme.typography.labelMedium,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(80.dp),
//                                    textAlign = TextAlign.End
//                                )
//                                Spacer(modifier = Modifier.width(32.dp)) // Espacio para icono de eliminar
//                            }
//
//                            // Items
//                            operationDetails.forEach { detail ->
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(vertical = 12.dp, horizontal = 8.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Column(modifier = Modifier.weight(1f)) {
//                                        Text(
//                                            detail.productTariff.productName,
//                                            style = MaterialTheme.typography.bodyMedium
//                                        )
//                                        Text(
//                                            "Código: ${detail.productTariff.productId}",
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                                        )
//                                    }
//                                    Text(
//                                        detail.quantity,
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        modifier = Modifier.width(60.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${detail.price}",
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        modifier = Modifier.width(80.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        modifier = Modifier.width(80.dp),
//                                        textAlign = TextAlign.End
//                                    )
//                                    IconButton(
//                                        onClick = {
//                                            operationDetails = operationDetails.filter { it != detail }
//                                        },
//                                        modifier = Modifier.size(32.dp)
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.Delete,
//                                            contentDescription = "Eliminar",
//                                            tint = MaterialTheme.colorScheme.error
//                                        )
//                                    }
//                                }
//                                Divider()
//                            }
//                        }
//                    }
//                }
//            }
//
//            // CARD FOOTER - Totales y botones
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(16.dp)
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Resumen",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 16.dp)
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "Subtotal:",
//                            style = MaterialTheme.typography.bodyLarge
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalValue)}",
//                            style = MaterialTheme.typography.bodyLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "IGV (18%):",
//                            style = MaterialTheme.typography.bodyLarge
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalIgv)}",
//                            style = MaterialTheme.typography.bodyLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Divider()
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "TOTAL:",
//                            style = MaterialTheme.typography.bodyLarge
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalAmount)}",
//                            style = MaterialTheme.typography.titleLarge,
//                            fontWeight = FontWeight.Bold,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(16.dp)
//                    ) {
//                        OutlinedButton(
//                            onClick = onBack,
//                            modifier = Modifier.weight(1f)
//                        ) {
//                            Text("Cancelar")
//                        }
//
//                        Button(
//                            onClick = {
//                                val operation = IOperation(
//                                    id = 0, // El ID será asignado por el backend
//                                    client = clientData ?: IPerson(),
//                                    operationDetailSet = operationDetails,
//                                    totalAmount = totalAmount,
//                                    totalIgv = totalIgv,
//                                    totalTaxed = totalValue,
//                                    totalToPay = totalAmount
//                                )
//                                viewModel.createInvoice(operation) { operationId ->
//                                    onInvoiceCreated(operationId.toString())
//                                }
//                            },
//                            modifier = Modifier.weight(1f),
//                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
//                        ) {
//                            Text("Emitir Factura")
//                        }
//                    }
//                }
//            }
//        }
//
//        // Diálogo para agregar producto
//        if (showAddItemDialog) {
//            AddProductDialog(
//                onDismiss = { showAddItemDialog = false },
//                onProductAdded = { newItem ->
//                    operationDetails = operationDetails + newItem
//                    showAddItemDialog = false
//                },
//                viewModel = viewModel
//            )
//        }
//
//        // Indicador de carga
//        if (isLoading) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.5f)),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
//            }
//        }
//
//        // Diálogo de error
//        error?.let { errorMessage ->
//            AlertDialog(
//                onDismissRequest = { viewModel.clearError() },
//                title = { Text("Error") },
//                text = { Text(errorMessage) },
//                confirmButton = {
//                    TextButton(onClick = { viewModel.clearError() }) {
//                        Text("Aceptar")
//                    }
//                }
//            )
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddProductDialog(
//    onDismiss: () -> Unit,
//    onProductAdded: (IOperationDetail) -> Unit,
//    viewModel: NewInvoiceViewModel
//) {
//    var searchQuery by remember { mutableStateOf("") }
//    var selectedProduct by remember { mutableStateOf<IProductTariff?>(null) }
//    var quantity by remember { mutableStateOf("1") }
//    var price by remember { mutableStateOf("0.00") }
//    var discount by remember { mutableStateOf("0.00") }
//
//    val searchResults by viewModel.searchResults.collectAsState()
//
//    // Cálculos
//    val quantityValue = quantity.toDoubleOrNull() ?: 1.0
//    val priceValue = price.toDoubleOrNull() ?: 0.0
//    val discountValue = discount.toDoubleOrNull() ?: 0.0
//
//    val igvPercentage = 0.18 // 18% IGV
//    val unitValue = priceValue / (1 + igvPercentage) // Precio sin IGV
//    val totalValue = unitValue * quantityValue // Valor total sin IGV
//    val totalIgv = totalValue * igvPercentage // Total IGV
//    val totalAmount = totalValue + totalIgv // Total con IGV
//    val totalWithDiscount = totalAmount - discountValue // Total final con descuento
//
//    // Efecto para actualizar búsqueda
//    LaunchedEffect(searchQuery) {
//        if (searchQuery.length >= 2) {
//            viewModel.searchProducts(searchQuery)
//        }
//    }
//
//    Dialog(onDismissRequest = onDismiss) {
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .heightIn(max = 550.dp),
//            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//            ) {
//                Text(
//                    "Agregar Producto",
//                    style = MaterialTheme.typography.titleLarge,
//                    fontWeight = FontWeight.Bold
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Búsqueda de productos
//                Column(modifier = Modifier.fillMaxWidth()) {
//                    OutlinedTextField(
//                        value = searchQuery,
//                        onValueChange = { searchQuery = it },
//                        label = { Text("Buscar por código o nombre") },
//                        modifier = Modifier.fillMaxWidth(),
//                        leadingIcon = {
//                            Icon(Icons.Default.Search, contentDescription = "Buscar")
//                        },
//                        singleLine = true
//                    )
//
//                    AnimatedVisibility(
//                        visible = searchQuery.isNotEmpty() && searchResults.isNotEmpty() && selectedProduct == null,
//                        enter = fadeIn() + expandVertically(),
//                        exit = fadeOut() + shrinkVertically()
//                    ) {
//                        LazyColumn(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .heightIn(max = 200.dp)
//                                .border(
//                                    width = 1.dp,
//                                    color = MaterialTheme.colorScheme.outline,
//                                    shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
//                                )
//                        ) {
////                            items(searchResults) { product ->
////                                Surface(
////                                    modifier = Modifier.fillMaxWidth(),
////                                    onClick = {
////                                        selectedProduct = product
////                                        price = product.priceWithIgv.toString()
////                                        searchQuery = product.name
////                                    }
////                                ) {
////                                    Column(
////                                        modifier = Modifier
////                                            .fillMaxWidth()
////                                            .padding(vertical = 8.dp, horizontal = 16.dp)
////                                    ) {
////                                        Text(
////                                            product.productName,
////                                            style = MaterialTheme.typography.bodyMedium
////                                        )
////                                        Text(
////                                            "Código: ${product.productId} - Precio: S/ ${String.format("%.2f", product.priceWithIgv)}",
////                                            style = MaterialTheme.typography.bodySmall,
////                                            color = MaterialTheme.colorScheme.onSurfaceVariant
////                                        )
////                                    }
////                                }
////                                Divider()
////                            }
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Campos para detalles del producto
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    OutlinedTextField(
//                        value = quantity,
//                        onValueChange = { quantity = it },
//                        label = { Text("Cantidad") },
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        singleLine = true
//                    )
//
//                    OutlinedTextField(
//                        value = price,
//                        onValueChange = { price = it },
//                        label = { Text("Precio") },
//                        modifier = Modifier.weight(1f),
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                        singleLine = true
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                OutlinedTextField(
//                    value = discount,
//                    onValueChange = { discount = it },
//                    label = { Text("Descuento") },
//                    modifier = Modifier.fillMaxWidth(),
//                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                    singleLine = true
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Resumen
//                Surface(
//                    modifier = Modifier.fillMaxWidth(),
//                    color = MaterialTheme.colorScheme.surfaceVariant,
//                    shape = RoundedCornerShape(4.dp)
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .padding(16.dp)
//                            .fillMaxWidth()
//                    ) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Subtotal:")
//                            Text("S/ ${String.format("%.2f", totalValue)}")
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("IGV:")
//                            Text("S/ ${String.format("%.2f", totalIgv)}")
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("Descuento:")
//                            Text("S/ ${String.format("%.2f", discountValue)}")
//                        }
//
//                        Spacer(modifier = Modifier.height(4.dp))
//                        Divider()
//                        Spacer(modifier = Modifier.height(4.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(
//                                "TOTAL:",
//                                fontWeight = FontWeight.Bold
//                            )
//                            Text(
//                                "S/ ${String.format("%.2f", totalWithDiscount)}",
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Botones
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier.weight(1f)
//                    ) {
//                        Text("Cancelar")
//                    }
//
//                    Button(
//                        onClick = {
//                            selectedProduct?.let { product ->
//                                val newDetail = IOperationDetail(
//                                    id = 0, // El ID será asignado por el backend
//                                    productTariff = product,
//                                    quantity = quantity,
//                                    price = price,
//                                    unitValue = unitValue,
//                                    unitPrice = priceValue,
//                                    totalDiscount = discountValue,
//                                    discountPercentage = if (totalAmount > 0) (discountValue / totalAmount) * 100 else 0.0,
//                                    igvPercentage = igvPercentage * 100,
//                                    totalIgv = totalIgv,
//                                    totalValue = totalValue,
//                                    totalAmount = totalAmount,
//                                    totalToPay = totalWithDiscount
//                                )
//                                onProductAdded(newDetail)
//                            }
//                        },
//                        modifier = Modifier.weight(1f),
//                        enabled = selectedProduct != null
//                    ) {
//                        Text("Agregar")
//                    }
//                }
//            }
//        }
//    }
//}