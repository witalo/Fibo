package com.example.fibo.ui.screens.invoice

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.IProductTariff
import com.example.fibo.model.ITariff
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.getCurrentFormattedDate
import com.example.fibo.utils.getCurrentFormattedTime
import com.example.fibo.viewmodels.ProductSearchState
import kotlinx.coroutines.delay
import kotlin.math.max



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceScreen(
    onBack: () -> Unit,
    onInvoiceCreated: (String) -> Unit,
    viewModel: NewInvoiceViewModel = hiltViewModel()
) {
    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var clientData by remember { mutableStateOf<IPerson?>(null) }
    var documentNumber by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }

    // Calcular totales basados en typeAffectationId
    val totalTaxed = operationDetails.filter { it.typeAffectationId == 1 }
        .sumOf { it.totalValue }
    val totalExonerated = operationDetails.filter { it.typeAffectationId == 2 }
        .sumOf { it.totalValue }
    val totalUnaffected = operationDetails.filter { it.typeAffectationId == 3 }
        .sumOf { it.totalValue }
    val totalFree = operationDetails.filter { it.typeAffectationId == 4 }
        .sumOf { it.totalValue }

    // Suma de descuentos por ítem
    val discountForItem = operationDetails.sumOf { it.totalDiscount }

    // Descuento global (agregamos variable para capturar esto)
    var discountGlobal by remember { mutableStateOf("0.00") }
    val discountGlobalValue = discountGlobal.toDoubleOrNull() ?: 0.0

    // Total general
    val totalIgv = operationDetails.filter { it.typeAffectationId == 1 }
        .sumOf { it.totalIgv }
    val totalAmount = totalTaxed + totalExonerated + totalUnaffected + totalIgv
    val totalToPay = totalAmount - discountGlobalValue

//    val totalAmount = operationDetails.sumOf { it.totalAmount }
//    val totalIgv = operationDetails.sumOf { it.totalIgv }
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
                                            detail.tariff.productName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Código: ${detail.tariff.productId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "${detail.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(40.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${detail.unitPrice}",
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
                                            tint = Color(0xFFFF5722),
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
                                    serial = "",
                                    correlative = 0,
                                    documentType = "01",
                                    operationType = "0101",
                                    operationStatus = "01",
                                    operationAction = "E",
                                    currencyType = "PEN",
                                    operationDate = getCurrentFormattedDate(),
                                    emitDate = getCurrentFormattedDate(),
                                    emitTime = getCurrentFormattedTime(),
                                    userId = userData?.id!!,
                                    subsidiaryId = subsidiaryData?.id!!,
                                    client = clientData ?: IPerson(),
                                    operationDetailSet = operationDetails,
                                    discountGlobal = 0.0,
                                    discountPercentageGlobal = 0.0,
                                    discountForItem = 0.0,
                                    totalTaxed = totalValue,
                                    totalUnaffected = 0.0,
                                    totalExonerated = 0.0,
                                    totalIgv = totalIgv,
                                    totalFree = 0.0,
                                    totalAmount = totalAmount,
                                    totalToPay = totalAmount,
                                    totalPayed = totalAmount
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
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    // Estados para el producto seleccionado
    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0.00") }

    // Precios con IGV y sin IGV (se actualizan cuando se selecciona un producto)
    var priceWithIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithIgv?.toString() ?: "0.00")
    }
    var priceWithoutIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithoutIgv?.toString() ?: "0.00")
    }

    // Cálculos para el resumen
    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
    val priceValue = priceWithIgv.toDoubleOrNull() ?: 0.0
    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
    val discountValue = discount.toDoubleOrNull() ?: 0.0

    val igvPercentage = 0.18
    val subtotal = priceWithoutIgvValue * qtyValue
    val igvAmount = subtotal * igvPercentage
    val total = subtotal + igvAmount - discountValue

    // Debounce para la búsqueda
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            delay(350) // Tiempo de debounce
            viewModel.searchProductsByQuery(searchQuery, subsidiaryId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false, // <- esto evita el cierre al hacer clic fuera
            usePlatformDefaultWidth = false
        )
//        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(15.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, start = 18.dp, end = 18.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Encabezado del diálogo
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Agregar Producto",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de búsqueda con autocompletado
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar producto") },
                    placeholder = { Text("Ingrese 3 caracteres") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resultados de la búsqueda
                AnimatedVisibility(
                    visible = searchQuery.length >= 3 && selectedProduct == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
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
                                        modifier = Modifier.size(35.dp),
                                        strokeWidth = 3.dp,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            is ProductSearchState.Success -> {
                                val products = (searchState as ProductSearchState.Success).products
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    )
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 350.dp, max = 350.dp)) {
                                        itemsIndexed(products) { index, product ->
                                            ProductListItem(
                                                product = product,
                                                onClick = {
                                                    viewModel.getTariff(product.id, subsidiaryId)
                                                }
                                            )

                                            if (index < products.size - 1) {
                                                Divider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    thickness = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is ProductSearchState.Empty -> {
                                EmptySearchResult()
                            }
                            is ProductSearchState.Error -> {
                                SearchError((searchState as ProductSearchState.Error).message)
                            }
                            else -> {
                                // Estado Idle
                                if (searchQuery.isNotEmpty()) {
                                    MinimumSearchInfo()
                                }
                            }
                        }
                    }
                }

                // Detalles del producto seleccionado
                AnimatedVisibility(
                    visible = selectedProduct != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedProduct?.let { product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            // Card con datos del producto seleccionado
                            SelectedProductCard(
                                product = product,
                                onClear = { viewModel.clearProductSelection() }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Sección de cantidad, precio y descuento
                            Text(
                                "Detalle de venta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Cantidad y Descuento
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { quantity = it },
                                    label = { Text("Cantidad") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = discount,
                                    onValueChange = { discount = it },
                                    label = { Text("Descuento S/") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Precios
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = priceWithoutIgv,
                                    onValueChange = {
                                        priceWithoutIgv = it
                                        // Calcular precio con IGV
                                        val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
                                        priceWithIgv = (withoutIgvValue * (1 + igvPercentage)).toString()
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio sin IGV") },
                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = priceWithIgv,
                                    onValueChange = {
                                        priceWithIgv = it
                                        // Calcular precio sin IGV
                                        val withIgvValue = it.toDoubleOrNull() ?: 0.0
                                        priceWithoutIgv = (withIgvValue / (1 + igvPercentage)).toString()
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio con IGV") },
                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(15.dp))

                            // Resumen de la venta
                            PurchaseSummary(
                                subtotal = subtotal,
                                igv = igvAmount,
                                discount = discountValue,
                                total = total
                            )

                            Spacer(modifier = Modifier.height(15.dp))

                            // Botón agregar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = MaterialTheme.colorScheme.primary
                                    )
                                    .background(
                                        brush = ColorGradients.blueButtonGradient,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        val tariff = ITariff(
                                            productId = product.productId,
                                            productCode = product.productCode,
                                            productName = product.productName,
                                            unitId = product.unitId,
                                            unitName = product.unitName,
                                            remainingQuantity = product.remainingQuantity,
                                            priceWithIgv = priceWithIgv.toDoubleOrNull() ?: 0.0,
                                            priceWithoutIgv = priceWithoutIgv.toDoubleOrNull() ?: 0.0,
                                            productTariffId = product.productTariffId,
                                            typeAffectationId = product.typeAffectationId
                                        )

                                        val operationDetail = IOperationDetail(
                                            id = 0,
                                            tariff = tariff,
                                            typeAffectationId = product.typeAffectationId,
                                            quantity = qtyValue,
                                            unitValue = priceWithoutIgvValue,
                                            unitPrice = priceValue,
                                            totalDiscount = discountValue,
                                            discountPercentage = if (subtotal > 0) (discountValue / subtotal) * 100 else 0.0,
                                            igvPercentage = igvPercentage * 100,
                                            perceptionPercentage = 0.0,
                                            totalPerception = 0.0,
                                            totalValue = when (product.typeAffectationId) {
                                                1 -> subtotal  // Operación gravada
                                                2 -> subtotal  // Operación exonerada
                                                3 -> subtotal  // Operación inafecta
                                                4 -> 0.0       // Operación gratuita (valor comercial en totalValue)
                                                else -> subtotal
                                            },
                                            totalIgv = if (product.typeAffectationId == 1) igvAmount else 0.0,
                                            totalAmount = when (product.typeAffectationId) {
                                                1 -> subtotal + igvAmount - discountValue  // Gravada: Base + IGV - Descuento
                                                2, 3 -> subtotal - discountValue           // Exonerada/Inafecta: Base - Descuento
                                                4 -> 0.0                                   // Gratuita: Se registra 0
                                                else -> subtotal + igvAmount - discountValue
                                            },
                                            totalToPay = total
                                        )

                                        onProductAdded(operationDetail)
                                    }
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Agregar Producto",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: IProduct,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono o imagen del producto
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Código: ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ícono de selección
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Seleccionar",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@Composable
private fun SelectedProductCard(
    product: ITariff,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PRODUCTO SELECCIONADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            brush = ColorGradients.blueVibrant
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cambiar selección",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Nombre del producto
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Detalles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        InfoRow(
                            label = "Código:",
                            value = product.productCode,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoRow(
                            label = "Stock:",
                            value = "${product.remainingQuantity} ${product.unitName}",
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Precio con mejor estilo
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "S/ ${"%.2f".format(product.priceWithIgv)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = ColorGradients.orangeSunset,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptySearchResult() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No se encontraron productos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchError(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error en la búsqueda",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun MinimumSearchInfo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Ingrese al menos 3 caracteres para buscar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchaseSummary(
    subtotal: Double,
    igv: Double,
    discount: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Resumen",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SummaryRow(
                label = "Subtotal:",
                value = subtotal
            )

            SummaryRow(
                label = "IGV (18%):",
                value = igv
            )

            SummaryRow(
                label = "Descuento:",
                value = -discount,
                valueColor = if (discount > 0) MaterialTheme.colorScheme.tertiary else null
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            SummaryRow(
                label = "TOTAL:",
                value = total,
                isTotal = true
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: Double,
    isTotal: Boolean = false,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = "S/ ${String.format("%.2f", value)}",
//            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            style = MaterialTheme.typography.titleMedium.copy(
                brush = if (isTotal) ColorGradients.orangeFire else ColorGradients.orangeSunset
            ),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                valueColor != null -> valueColor
                isTotal -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
//---------------------------------------------------------------------------------------------------
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NewInvoiceScreen(
//    onBack: () -> Unit,
//    onInvoiceCreated: (String) -> Unit,
//    viewModel: NewInvoiceViewModel = hiltViewModel()
//) {
//    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
//    val userData by viewModel.userData.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//    val error by viewModel.error.collectAsState()
//
//    var clientData by remember { mutableStateOf<IPerson?>(null) }
//    var documentNumber by remember { mutableStateOf("") }
//    var showAddItemDialog by remember { mutableStateOf(false) }
//    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }
//
//    // ===== [CÁLCULOS CORREGIDOS SEGÚN SUNAT] =====
//    var discountGlobal by remember { mutableStateOf("0.00") }
//    val discountGlobalValue = discountGlobal.toDoubleOrNull() ?: 0.0
//
//    // 1. Calculamos por tipo de operación
//    val totalTaxed = operationDetails.filter { it.typeAffectationId == 1 }.sumOf { it.totalValue }
//    val totalExonerated =
//        operationDetails.filter { it.typeAffectationId == 2 }.sumOf { it.totalValue }
//    val totalUnaffected =
//        operationDetails.filter { it.typeAffectationId == 3 }.sumOf { it.totalValue }
//    val totalFree = operationDetails.filter { it.typeAffectationId == 4 }.sumOf { it.totalValue }
//
//    // 2. Descuentos
//    val discountForItem = operationDetails.sumOf { it.totalDiscount }
//    val totalDiscount = discountGlobalValue + discountForItem
//
//    // 3. IGV solo para operaciones gravadas (id=1)
//    val totalIgv = operationDetails.filter { it.typeAffectationId == 1 }.sumOf { it.totalIgv }
//
//    // 4. Totales finales
//    val totalAmount = totalTaxed + totalExonerated + totalUnaffected + totalIgv
//    val totalToPay = max(totalAmount - discountGlobalValue, 0.0) // Evitar valores negativos
//
//    // ... (Scaffold y demás código anterior sin cambios hasta el Card de resumen)
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Nueva Factura", style = MaterialTheme.typography.titleSmall) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.Black,
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
//                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding más ajustado
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//        ) {
//            // CARD CABECERA - Información del Cliente
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 12.dp), // Espacio reducido
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp) // Bordes redondeados
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp) // Padding interno reducido
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Información del Cliente",
//                        style = MaterialTheme.typography.titleSmall, // Texto más pequeño
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 8.dp) // Espacio reducido
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        OutlinedTextField(
//                            value = documentNumber,
//                            onValueChange = { documentNumber = it },
//                            label = {
//                                Text(
//                                    "RUC/DNI",
//                                    style = MaterialTheme.typography.labelSmall
//                                )
//                            },
//                            textStyle = MaterialTheme.typography.bodySmall,
//                            modifier = Modifier.weight(0.8f),
//                            singleLine = true,
//                            shape = RoundedCornerShape(8.dp)
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Box(
//                            modifier = Modifier
//                                .height(40.dp)
//                                .shadow(2.dp, RoundedCornerShape(8.dp))
//                                .background(
//                                    brush = ColorGradients.blueButtonGradient,
//                                    shape = RoundedCornerShape(8.dp)
//                                )
//                                .clickable {
//                                    viewModel.fetchClientData(documentNumber) { person ->
//                                        clientData = person
//                                    }
//                                },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(horizontal = 12.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Search,
//                                    contentDescription = "Buscar",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    "Extraer",
//                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
//                                )
//                            }
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.names ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
//                        },
//                        label = { Text("Nombre", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.address ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
//                        },
//                        label = { Text("Dirección", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//                }
//            }
//
//            // CARD CUERPO - Lista de productos
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 12.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "Detalle de Productos",
//                            style = MaterialTheme.typography.titleSmall,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        Box(
//                            modifier = Modifier
//                                .height(40.dp)
//                                .shadow(
//                                    elevation = 2.dp,
//                                    shape = RoundedCornerShape(8.dp),
//                                    spotColor = MaterialTheme.colorScheme.primary
//                                )
//                                .background(
//                                    brush = ColorGradients.blueButtonGradient,
//                                    shape = RoundedCornerShape(8.dp)
//                                )
//                                .clickable { showAddItemDialog = true }
//                                .border(
//                                    width = 1.dp,
//                                    brush = Brush.linearGradient(
//                                        colors = listOf(
//                                            Color.White.copy(alpha = 0.3f),
//                                            Color.Transparent
//                                        )
//                                    ),
//                                    shape = RoundedCornerShape(8.dp)
//                                ),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(horizontal = 12.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Add,
//                                    contentDescription = "Agregar Item",
//                                    modifier = Modifier.size(18.dp),  // Tamaño ligeramente mayor
//                                    tint = Color.White
//                                )
//                                Spacer(modifier = Modifier.width(6.dp))  // Espacio un poco mayor
//                                Text(
//                                    "Agregar",
//                                    style = MaterialTheme.typography.labelMedium.copy(
//                                        color = Color.White,
//                                        fontWeight = FontWeight.SemiBold  // Texto en negrita
//                                    )
//                                )
//                            }
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    if (operationDetails.isEmpty()) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 24.dp), // Padding reducido
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                "No hay productos agregados",
//                                style = MaterialTheme.typography.bodySmall, // Texto más pequeño
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    } else {
//                        Column(modifier = Modifier.fillMaxWidth()) {
//                            // Encabezado más compacto
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .background(MaterialTheme.colorScheme.surfaceVariant)
//                                    .padding(vertical = 4.dp, horizontal = 4.dp)
//                            ) {
//                                Text(
//                                    "Producto",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.weight(1.5f)
//                                )
//                                Text(
//                                    "Cant.",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(40.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Precio",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(60.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Total",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(70.dp),
//                                    textAlign = TextAlign.End
//                                )
//                                Spacer(modifier = Modifier.width(24.dp))
//                            }
//
//                            // Items más compactos
//                            operationDetails.forEach { detail ->
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(vertical = 8.dp, horizontal = 4.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Column(modifier = Modifier.weight(1.5f)) {
//                                        Text(
//                                            detail.tariff.productName,
//                                            style = MaterialTheme.typography.bodySmall,
//                                            maxLines = 1,
//                                            overflow = TextOverflow.Ellipsis
//                                        )
//                                        Text(
//                                            "Código: ${detail.tariff.productId}",
//                                            style = MaterialTheme.typography.labelSmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                                        )
//                                    }
//                                    Text(
//                                        "${detail.quantity}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(40.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${detail.unitPrice}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(60.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(70.dp),
//                                        textAlign = TextAlign.End
//                                    )
//                                    IconButton(
//                                        onClick = {
//                                            operationDetails =
//                                                operationDetails.filter { it != detail }
//                                        },
//                                        modifier = Modifier.size(28.dp) // Tamaño reducido
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.Delete,
//                                            contentDescription = "Eliminar",
//                                            tint = Color(0xFFFF5722),
//                                            modifier = Modifier.size(18.dp) // Icono más pequeño
//                                        )
//                                    }
//                                }
//                                Divider(thickness = 0.5.dp) // Línea más fina
//                            }
//                        }
//                    }
//                }
//            }
//
//            // ===== [CARD FOOTER - Solo modificamos el resumen] =====
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Resumen",
//                        style = MaterialTheme.typography.titleSmall,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 12.dp)
//                    )
//
//                    // Agregamos campo para descuento global
//                    OutlinedTextField(
//                        value = discountGlobal,
//                        onValueChange = { discountGlobal = it },
//                        label = { Text("Descuento Global (S/)") },
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(8.dp)
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    // Mostramos los totales por tipo de operación
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Op. Gravadas:", style = MaterialTheme.typography.bodySmall)
//                        Text(
//                            "S/ ${"%.2f".format(totalTaxed)}",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }
//
//                    if (totalExonerated > 0) {
//                        Row(
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text("Op. Exoneradas:", style = MaterialTheme.typography.bodySmall)
//                            Text(
//                                "S/ ${"%.2f".format(totalExonerated)}",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }
//
//                    if (totalUnaffected > 0) {
//                        Row(
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text("Op. Inafectas:", style = MaterialTheme.typography.bodySmall)
//                            Text(
//                                "S/ ${"%.2f".format(totalUnaffected)}",
//                                style = MaterialTheme.typography.bodySmall
//                            )
//                        }
//                    }
//
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Descuento Items:", style = MaterialTheme.typography.bodySmall)
//                        Text(
//                            "S/ ${"%.2f".format(discountForItem)}",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }
//
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Descuento Global:", style = MaterialTheme.typography.bodySmall)
//                        Text(
//                            "S/ ${"%.2f".format(discountGlobalValue)}",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }
//
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("IGV (18%):", style = MaterialTheme.typography.bodySmall)
//                        Text(
//                            "S/ ${"%.2f".format(totalIgv)}",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                    }
//
//                    Divider(modifier = Modifier.padding(vertical = 8.dp))
//
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text(
//                            "TOTAL:",
//                            style = MaterialTheme.typography.bodyMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            "S/ ${"%.2f".format(totalToPay)}",
//                            style = MaterialTheme.typography.titleMedium.copy(
//                                brush = ColorGradients.goldLuxury
//                            ),
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    // ... (Botones y código posterior sin cambios)
//                }
//            }
//
//            // ... (resto del código sin cambios)
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun AddProductDialog(
//    onDismiss: () -> Unit,
//    onProductAdded: (IOperationDetail) -> Unit,
//    viewModel: NewInvoiceViewModel,
//    subsidiaryId: Int = 0
//) {
//    var searchQuery by remember { mutableStateOf("") }
//    val searchState by viewModel.searchState.collectAsState()
//    val selectedProduct by viewModel.selectedProduct.collectAsState()
//
//    // Estados para el producto seleccionado
//    var quantity by remember { mutableStateOf("1") }
//    var discount by remember { mutableStateOf("0.00") }
//
//    // Precios con IGV y sin IGV (se actualizan cuando se selecciona un producto)
//    var priceWithIgv by remember(selectedProduct) {
//        mutableStateOf(selectedProduct?.priceWithIgv?.toString() ?: "0.00")
//    }
//    var priceWithoutIgv by remember(selectedProduct) {
//        mutableStateOf(selectedProduct?.priceWithoutIgv?.toString() ?: "0.00")
//    }
//
//    // Cálculos para el resumen
//    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
//    val priceValue = priceWithIgv.toDoubleOrNull() ?: 0.0
//    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
//    val discountValue = discount.toDoubleOrNull() ?: 0.0
//
//    val igvPercentage = 0.18
//    // Cálculos corregidos según tipo de operación
//    val subtotal = priceWithoutIgvValue * qtyValue
//    val igvAmount = if (selectedProduct?.typeAffectationId == 1) subtotal * 0.18 else 0.0
//    val total = when (selectedProduct?.typeAffectationId) {
//        1 -> subtotal + igvAmount - discountValue // Gravada
//        2, 3 -> subtotal - discountValue         // Exonerada/Inafecta
//        4 -> 0.0                                // Gratuita
//        else -> subtotal + igvAmount - discountValue
//    }
//
//    // Debounce para la búsqueda
//    LaunchedEffect(searchQuery) {
//        if (searchQuery.length >= 3) {
//            delay(350) // Tiempo de debounce
//            viewModel.searchProductsByQuery(searchQuery, subsidiaryId)
//        }
//    }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(
//            dismissOnClickOutside = false, // <- esto evita el cierre al hacer clic fuera
//            usePlatformDefaultWidth = false
//        )
////        properties = DialogProperties(usePlatformDefaultWidth = false)
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(0.95f)
//                .heightIn(max = 700.dp),
//            shape = RoundedCornerShape(15.dp),
//            tonalElevation = 6.dp
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(top = 5.dp, start = 18.dp, end = 18.dp, bottom = 10.dp)
//                    .fillMaxWidth()
//                    .verticalScroll(rememberScrollState())
//            ) {
//                // Encabezado del diálogo
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(top = 14.dp, bottom = 0.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "Agregar Producto",
//                        style = MaterialTheme.typography.titleSmall.copy(
//                            fontWeight = FontWeight.Bold
//                        )
//                    )
//
//                    IconButton(
//                        onClick = onDismiss,
//                        modifier = Modifier
//                            .size(20.dp)
//                            .background(
//                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
//                                shape = CircleShape
//                            )
//                    ) {
//                        Icon(
//                            Icons.Default.Close,
//                            contentDescription = "Cerrar",
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Campo de búsqueda con autocompletado
//                OutlinedTextField(
//                    value = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    modifier = Modifier.fillMaxWidth(),
//                    label = { Text("Buscar producto") },
//                    placeholder = { Text("Ingrese 3 caracteres") },
//                    leadingIcon = {
//                        Icon(
//                            imageVector = Icons.Default.Search,
//                            contentDescription = "Buscar",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    },
//                    trailingIcon = {
//                        if (searchQuery.isNotEmpty()) {
//                            IconButton(onClick = { searchQuery = "" }) {
//                                Icon(
//                                    imageVector = Icons.Default.Close,
//                                    contentDescription = "Limpiar",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//                    },
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = MaterialTheme.colorScheme.primary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                        focusedContainerColor = MaterialTheme.colorScheme.surface,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                    ),
//                    shape = RoundedCornerShape(12.dp),
//                    singleLine = true
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Resultados de la búsqueda
//                AnimatedVisibility(
//                    visible = searchQuery.length >= 3 && selectedProduct == null,
//                    enter = fadeIn() + expandVertically(),
//                    exit = fadeOut() + shrinkVertically()
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .heightIn(max = 250.dp)
//                    ) {
//                        when (searchState) {
//                            is ProductSearchState.Loading -> {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(100.dp),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    CircularProgressIndicator(
//                                        modifier = Modifier.size(35.dp),
//                                        strokeWidth = 3.dp,
//                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
//                                    )
//                                }
//                            }
//
//                            is ProductSearchState.Success -> {
//                                val products = (searchState as ProductSearchState.Success).products
//                                Card(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    shape = RoundedCornerShape(12.dp),
//                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
//                                            2.dp
//                                        )
//                                    )
//                                ) {
//                                    LazyColumn(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .heightIn(min = 350.dp, max = 350.dp)
//                                    ) {
//                                        itemsIndexed(products) { index, product ->
//                                            ProductListItem(
//                                                product = product,
//                                                onClick = {
//                                                    viewModel.getTariff(product.id, subsidiaryId)
//                                                }
//                                            )
//
//                                            if (index < products.size - 1) {
//                                                Divider(
//                                                    modifier = Modifier.padding(horizontal = 16.dp),
//                                                    thickness = 0.5.dp,
//                                                    color = MaterialTheme.colorScheme.outlineVariant
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//
//                            is ProductSearchState.Empty -> {
//                                EmptySearchResult()
//                            }
//
//                            is ProductSearchState.Error -> {
//                                SearchError((searchState as ProductSearchState.Error).message)
//                            }
//
//                            else -> {
//                                // Estado Idle
//                                if (searchQuery.isNotEmpty()) {
//                                    MinimumSearchInfo()
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // Detalles del producto seleccionado
//                AnimatedVisibility(
//                    visible = selectedProduct != null,
//                    enter = fadeIn() + expandVertically(),
//                    exit = fadeOut() + shrinkVertically()
//                ) {
//                    selectedProduct?.let { product ->
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 8.dp)
//                        ) {
//                            // Card con datos del producto seleccionado
//                            SelectedProductCard(
//                                product = product,
//                                onClear = { viewModel.clearProductSelection() }
//                            )
//
//                            Spacer(modifier = Modifier.height(24.dp))
//
//                            // Sección de cantidad, precio y descuento
//                            Text(
//                                "Detalle de venta",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.SemiBold
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Cantidad y Descuento
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(10.dp)
//                            ) {
//                                OutlinedTextField(
//                                    value = quantity,
//                                    onValueChange = { quantity = it },
//                                    label = { Text("Cantidad") },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//
//                                OutlinedTextField(
//                                    value = discount,
//                                    onValueChange = { discount = it },
//                                    label = { Text("Descuento S/") },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Precios
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(16.dp)
//                            ) {
//                                OutlinedTextField(
//                                    value = priceWithoutIgv,
//                                    onValueChange = {
//                                        priceWithoutIgv = it
//                                        // Calcular precio con IGV
//                                        val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithIgv =
//                                            (withoutIgvValue * (1 + igvPercentage)).toString()
//                                    },
//                                    textStyle = MaterialTheme.typography.bodyMedium,
//                                    label = { Text("Precio sin IGV") },
//                                    leadingIcon = {
//                                        Text(
//                                            "S/",
//                                            modifier = Modifier.padding(start = 3.dp)
//                                        )
//                                    },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//
//                                OutlinedTextField(
//                                    value = priceWithIgv,
//                                    onValueChange = {
//                                        priceWithIgv = it
//                                        // Calcular precio sin IGV
//                                        val withIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithoutIgv =
//                                            (withIgvValue / (1 + igvPercentage)).toString()
//                                    },
//                                    textStyle = MaterialTheme.typography.bodyMedium,
//                                    label = { Text("Precio con IGV") },
//                                    leadingIcon = {
//                                        Text(
//                                            "S/",
//                                            modifier = Modifier.padding(start = 3.dp)
//                                        )
//                                    },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(15.dp))
//
//                            // Resumen de la venta
//                            PurchaseSummary(
//                                subtotal = subtotal,
//                                igv = igvAmount,
//                                discount = discountValue,
//                                total = total
//                            )
//
//                            Spacer(modifier = Modifier.height(15.dp))
//
//                            // Botón agregar
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(56.dp)
//                                    .shadow(
//                                        elevation = 4.dp,
//                                        shape = RoundedCornerShape(16.dp),
//                                        spotColor = MaterialTheme.colorScheme.primary
//                                    )
//                                    .background(
//                                        brush = ColorGradients.blueButtonGradient,
//                                        shape = RoundedCornerShape(16.dp)
//                                    )
//                                    .clickable {
//                                        val tariff = ITariff(
//                                            productId = product.productId,
//                                            productCode = product.productCode,
//                                            productName = product.productName,
//                                            unitId = product.unitId,
//                                            unitName = product.unitName,
//                                            remainingQuantity = product.remainingQuantity,
//                                            priceWithIgv = priceWithIgv.toDoubleOrNull() ?: 0.0,
//                                            priceWithoutIgv = priceWithoutIgv.toDoubleOrNull()
//                                                ?: 0.0,
//                                            productTariffId = product.productTariffId,
//                                            typeAffectationId = product.typeAffectationId
//                                        )
//
//                                        val operationDetail = IOperationDetail(
//                                            id = 0,
//                                            tariff = tariff,
//                                            typeAffectationId = product.typeAffectationId,
//                                            quantity = qtyValue,
//                                            unitValue = priceWithoutIgvValue,
//                                            unitPrice = priceValue,
//                                            discountPercentage = if (subtotal > 0) (discountValue / subtotal) * 100 else 0.0,
//                                            totalDiscount = discountValue,
//                                            igvPercentage = if (product.typeAffectationId == 1) 18.0 else 0.0,
//                                            totalValue = when (product.typeAffectationId) {
//                                                1 -> subtotal  // Gravada
//                                                2 -> subtotal  // Exonerada
//                                                3 -> subtotal  // Inafecta
//                                                4 -> 0.0       // Gratuita
//                                                else -> subtotal
//                                            },
//                                            totalIgv = if (product.typeAffectationId == 1) igvAmount else 0.0,
//                                            totalAmount = total,
//                                            totalToPay = total
//                                        )
//
//                                        onProductAdded(operationDetail)
//                                    }
//                                    .border(
//                                        width = 1.dp,
//                                        brush = Brush.linearGradient(
//                                            colors = listOf(
//                                                Color.White.copy(alpha = 0.3f),
//                                                Color.Transparent
//                                            )
//                                        ),
//                                        shape = RoundedCornerShape(16.dp)
//                                    ),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Row(
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.Center,
//                                    modifier = Modifier.padding(horizontal = 16.dp)
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Default.ShoppingCart,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(24.dp),
//                                        tint = Color.White
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
//                                    Text(
//                                        "Agregar Producto",
//                                        style = MaterialTheme.typography.labelMedium.copy(
//                                            color = Color.White,
//                                            fontWeight = FontWeight.SemiBold
//                                        )
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun ProductListItem(
//    product: IProduct,
//    onClick: () -> Unit
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier.fillMaxWidth(),
//        color = Color.Transparent
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 10.dp, vertical = 10.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Icono o imagen del producto
//            Box(
//                modifier = Modifier
//                    .size(35.dp)
//                    .background(
//                        brush = Brush.radialGradient(
//                            colors = listOf(
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
//                            )
//                        ),
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Inventory,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.surface,
//                    modifier = Modifier.size(15.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.width(14.dp))
//
//            // Información del producto
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = product.name,
//                    style = MaterialTheme.typography.bodySmall,
//                    fontWeight = FontWeight.SemiBold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Text(
//                    text = "Código: ${product.code}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            // Ícono de selección
//            Icon(
//                imageVector = Icons.Default.KeyboardArrowRight,
//                contentDescription = "Seleccionar",
//                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
//                modifier = Modifier.size(20.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun SelectedProductCard(
//    product: ITariff,
//    onClear: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = 4.dp,
//            pressedElevation = 8.dp
//        )
//    ) {
//        Box(
//            modifier = Modifier
//                .background(
//                    brush = Brush.verticalGradient(
//                        colors = listOf(
//                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
//                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
//                        )
//                    ),
//                    shape = RoundedCornerShape(16.dp)
//                )
//        ) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                // Header
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "PRODUCTO SELECCIONADO",
//                        style = MaterialTheme.typography.labelSmall.copy(
//                            brush = ColorGradients.blueVibrant
//                        ),
//                        color = MaterialTheme.colorScheme.primary
//                    )
//
//                    IconButton(
//                        onClick = onClear,
//                        modifier = Modifier
//                            .size(28.dp)
//                            .background(
//                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
//                                shape = CircleShape
//                            ),
//                        colors = IconButtonDefaults.iconButtonColors(
//                            contentColor = MaterialTheme.colorScheme.error
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Cambiar selección",
//                            modifier = Modifier.size(16.dp)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(6.dp))
//
//                // Nombre del producto
//                Text(
//                    text = product.productName,
//                    style = MaterialTheme.typography.titleSmall.copy(
//                        fontWeight = FontWeight.ExtraBold
//                    ),
//                    color = MaterialTheme.colorScheme.onSurface,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(10.dp))
//
//                // Detalles
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column {
//                        InfoRow(
//                            label = "Código:",
//                            value = product.productCode,
//                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        InfoRow(
//                            label = "Stock:",
//                            value = "${product.remainingQuantity} ${product.unitName}",
//                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//
//                    // Precio con mejor estilo
//                    Box(
//                        modifier = Modifier
//                            .background(
//                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                                shape = RoundedCornerShape(12.dp)
//                            )
//                            .padding(horizontal = 12.dp, vertical = 8.dp)
//                    ) {
//                        Text(
//                            text = "S/ ${"%.2f".format(product.priceWithIgv)}",
//                            style = MaterialTheme.typography.titleMedium.copy(
//                                brush = ColorGradients.orangeSunset,
//                                fontWeight = FontWeight.Bold,
//                            )
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun InfoRow(
//    label: String,
//    value: String,
//    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
//) {
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodySmall,
//            color = labelColor,
//            modifier = Modifier.width(60.dp)
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium.copy(
//                fontWeight = FontWeight.SemiBold
//            ),
//            color = MaterialTheme.colorScheme.onSurface
//        )
//    }
//}
//
//@Composable
//private fun PriceTag(price: Double) {
//    Card(
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
//        ),
//        shape = RoundedCornerShape(8.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "S/",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.primary
//            )
//            Spacer(modifier = Modifier.width(2.dp))
//            Text(
//                text = String.format("%.2f", price),
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.primary
//            )
//        }
//    }
//}
//
//@Composable
//private fun EmptySearchResult() {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 32.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = Icons.Default.SearchOff,
//                contentDescription = null,
//                modifier = Modifier.size(48.dp),
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "No se encontraron productos",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//@Composable
//private fun SearchError(errorMessage: String) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = Icons.Default.Error,
//                contentDescription = null,
//                modifier = Modifier.size(40.dp),
//                tint = MaterialTheme.colorScheme.error
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "Error en la búsqueda",
//                style = MaterialTheme.typography.titleMedium,
//                color = MaterialTheme.colorScheme.error
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                errorMessage,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onErrorContainer
//            )
//        }
//    }
//}
//
//@Composable
//private fun MinimumSearchInfo() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            "Ingrese al menos 3 caracteres para buscar",
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}
//
//@Composable
//private fun PurchaseSummary(
//    subtotal: Double,
//    igv: Double,
//    discount: Double,
//    total: Double
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                "Resumen",
//                style = MaterialTheme.typography.titleSmall,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SummaryRow(
//                label = "Subtotal:",
//                value = subtotal
//            )
//
//            SummaryRow(
//                label = "IGV (18%):",
//                value = igv
//            )
//
//            SummaryRow(
//                label = "Descuento:",
//                value = -discount,
//                valueColor = if (discount > 0) MaterialTheme.colorScheme.tertiary else null
//            )
//
//            Divider(
//                modifier = Modifier.padding(vertical = 10.dp),
//                thickness = 1.dp,
//                color = MaterialTheme.colorScheme.outlineVariant
//            )
//
//            SummaryRow(
//                label = "TOTAL:",
//                value = total,
//                isTotal = true
//            )
//        }
//    }
//}
//
//@Composable
//private fun SummaryRow(
//    label: String,
//    value: Double,
//    isTotal: Boolean = false,
//    valueColor: Color? = null
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // Texto de la etiqueta
//        Text(
//            text = label,
//            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
//            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
//        )
//
//        // Texto del valor
//        Text(
//            text = "S/ ${"%.2f".format(value)}",
//            style = MaterialTheme.typography.titleMedium.copy(
//                brush = if (isTotal) ColorGradients.orangeFire else ColorGradients.orangeSunset
//            ),
//            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
//            color = valueColor ?: if (isTotal) MaterialTheme.colorScheme.primary
//            else MaterialTheme.colorScheme.onSurface
//        )
//    }
//}
//-----------------------------------------------------------------------------------------------------
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NewInvoiceScreen(
//    onBack: () -> Unit,
//    onInvoiceCreated: (String) -> Unit,
//    viewModel: NewInvoiceViewModel = hiltViewModel()
//) {
//    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
//    val userData by viewModel.userData.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//    val error by viewModel.error.collectAsState()
//
//    var clientData by remember { mutableStateOf<IPerson?>(null) }
//    var documentNumber by remember { mutableStateOf("") }
//    var showAddItemDialog by remember { mutableStateOf(false) }
//    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }
//
//    // Calcular totales basados en typeAffectationId
//    val totalTaxed = operationDetails.filter { it.typeAffectationId == 1 }
//        .sumOf { it.totalValue }
//    val totalExonerated = operationDetails.filter { it.typeAffectationId == 2 }
//        .sumOf { it.totalValue }
//    val totalUnaffected = operationDetails.filter { it.typeAffectationId == 3 }
//        .sumOf { it.totalValue }
//    val totalFree = operationDetails.filter { it.typeAffectationId == 4 }
//        .sumOf { it.totalValue }
//
//    // Suma de descuentos por ítem
//    val discountForItem = operationDetails.sumOf { it.totalDiscount }
//
//    // Descuento global (agregamos variable para capturar esto)
//    var discountGlobal by remember { mutableStateOf("0.00") }
//    val discountGlobalValue = discountGlobal.toDoubleOrNull() ?: 0.0
//
//    // Total general
//    val totalIgv = operationDetails.filter { it.typeAffectationId == 1 }
//        .sumOf { it.totalIgv }
//    val totalAmount = totalTaxed + totalExonerated + totalUnaffected + totalIgv
//    val totalToPay = totalAmount - discountGlobalValue
//
////    val totalAmount = operationDetails.sumOf { it.totalAmount }
////    val totalIgv = operationDetails.sumOf { it.totalIgv }
//    val totalValue = operationDetails.sumOf { it.totalValue }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Nueva Factura", style = MaterialTheme.typography.titleSmall) },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
//                },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.Black,
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
//                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding más ajustado
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//        ) {
//            // CARD CABECERA - Información del Cliente
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 12.dp), // Espacio reducido
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp) // Bordes redondeados
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp) // Padding interno reducido
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Información del Cliente",
//                        style = MaterialTheme.typography.titleSmall, // Texto más pequeño
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 8.dp) // Espacio reducido
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        OutlinedTextField(
//                            value = documentNumber,
//                            onValueChange = { documentNumber = it },
//                            label = { Text("RUC/DNI", style = MaterialTheme.typography.labelSmall) },
//                            textStyle = MaterialTheme.typography.bodySmall,
//                            modifier = Modifier.weight(0.8f),
//                            singleLine = true,
//                            shape = RoundedCornerShape(8.dp)
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Box(
//                            modifier = Modifier
//                                .height(40.dp)
//                                .shadow(2.dp, RoundedCornerShape(8.dp))
//                                .background(
//                                    brush = ColorGradients.blueButtonGradient,
//                                    shape = RoundedCornerShape(8.dp)
//                                )
//                                .clickable {
//                                    viewModel.fetchClientData(documentNumber) { person ->
//                                        clientData = person
//                                    }
//                                },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(horizontal = 12.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Search,
//                                    contentDescription = "Buscar",
//                                    modifier = Modifier.size(20.dp),
//                                    tint = Color.White
//                                )
//                                Spacer(modifier = Modifier.width(8.dp))
//                                Text(
//                                    "Extraer",
//                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
//                                )
//                            }
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.names ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
//                        },
//                        label = { Text("Nombre", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    OutlinedTextField(
//                        value = clientData?.address ?: "",
//                        onValueChange = {
//                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
//                        },
//                        label = { Text("Dirección", style = MaterialTheme.typography.labelSmall) },
//                        textStyle = MaterialTheme.typography.bodySmall,
//                        modifier = Modifier.fillMaxWidth(),
//                        singleLine = true,
//                        shape = RoundedCornerShape(8.dp)
//                    )
//                }
//            }
//
//            // CARD CUERPO - Lista de productos
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 12.dp),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "Detalle de Productos",
//                            style = MaterialTheme.typography.titleSmall,
//                            fontWeight = FontWeight.Bold
//                        )
//
//                        Box(
//                            modifier = Modifier
//                                .height(40.dp)
//                                .shadow(
//                                    elevation = 2.dp,
//                                    shape = RoundedCornerShape(8.dp),
//                                    spotColor = MaterialTheme.colorScheme.primary
//                                )
//                                .background(
//                                    brush = ColorGradients.blueButtonGradient,
//                                    shape = RoundedCornerShape(8.dp)
//                                )
//                                .clickable { showAddItemDialog = true }
//                                .border(
//                                    width = 1.dp,
//                                    brush = Brush.linearGradient(
//                                        colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
//                                    ),
//                                    shape = RoundedCornerShape(8.dp)
//                                ),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(horizontal = 12.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Add,
//                                    contentDescription = "Agregar Item",
//                                    modifier = Modifier.size(18.dp),  // Tamaño ligeramente mayor
//                                    tint = Color.White
//                                )
//                                Spacer(modifier = Modifier.width(6.dp))  // Espacio un poco mayor
//                                Text(
//                                    "Agregar",
//                                    style = MaterialTheme.typography.labelMedium.copy(
//                                        color = Color.White,
//                                        fontWeight = FontWeight.SemiBold  // Texto en negrita
//                                    )
//                                )
//                            }
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    if (operationDetails.isEmpty()) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 24.dp), // Padding reducido
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                "No hay productos agregados",
//                                style = MaterialTheme.typography.bodySmall, // Texto más pequeño
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    } else {
//                        Column(modifier = Modifier.fillMaxWidth()) {
//                            // Encabezado más compacto
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .background(MaterialTheme.colorScheme.surfaceVariant)
//                                    .padding(vertical = 4.dp, horizontal = 4.dp)
//                            ) {
//                                Text(
//                                    "Producto",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.weight(1.5f)
//                                )
//                                Text(
//                                    "Cant.",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(40.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Precio",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(60.dp),
//                                    textAlign = TextAlign.Center
//                                )
//                                Text(
//                                    "Total",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    fontWeight = FontWeight.Bold,
//                                    modifier = Modifier.width(70.dp),
//                                    textAlign = TextAlign.End
//                                )
//                                Spacer(modifier = Modifier.width(24.dp))
//                            }
//
//                            // Items más compactos
//                            operationDetails.forEach { detail ->
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .padding(vertical = 8.dp, horizontal = 4.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Column(modifier = Modifier.weight(1.5f)) {
//                                        Text(
//                                            detail.tariff.productName,
//                                            style = MaterialTheme.typography.bodySmall,
//                                            maxLines = 1,
//                                            overflow = TextOverflow.Ellipsis
//                                        )
//                                        Text(
//                                            "Código: ${detail.tariff.productId}",
//                                            style = MaterialTheme.typography.labelSmall,
//                                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                                        )
//                                    }
//                                    Text(
//                                        "${detail.quantity}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(40.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${detail.unitPrice}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(60.dp),
//                                        textAlign = TextAlign.Center
//                                    )
//                                    Text(
//                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(70.dp),
//                                        textAlign = TextAlign.End
//                                    )
//                                    IconButton(
//                                        onClick = {
//                                            operationDetails = operationDetails.filter { it != detail }
//                                        },
//                                        modifier = Modifier.size(28.dp) // Tamaño reducido
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.Delete,
//                                            contentDescription = "Eliminar",
//                                            tint = Color(0xFFFF5722),
//                                            modifier = Modifier.size(18.dp) // Icono más pequeño
//                                        )
//                                    }
//                                }
//                                Divider(thickness = 0.5.dp) // Línea más fina
//                            }
//                        }
//                    }
//                }
//            }
//
//            // CARD FOOTER - Totales y botones
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                shape = RoundedCornerShape(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .padding(12.dp)
//                        .fillMaxWidth()
//                ) {
//                    Text(
//                        "Resumen",
//                        style = MaterialTheme.typography.titleSmall,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(bottom = 12.dp)
//                    )
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "Subtotal:",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalValue)}",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(4.dp))
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "IGV (18%):",
//                            style = MaterialTheme.typography.bodySmall
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalIgv)}",
//                            style = MaterialTheme.typography.bodySmall,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(4.dp))
//                    Divider(thickness = 0.5.dp)
//                    Spacer(modifier = Modifier.height(4.dp))
//
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text(
//                            "TOTAL:",
//                            style = MaterialTheme.typography.bodyMedium,
//                            fontWeight = FontWeight.Bold
//                        )
//                        Text(
//                            "S/ ${String.format("%.2f", totalAmount)}",
//                            style = MaterialTheme.typography.titleMedium.copy(
//                                brush = ColorGradients.goldLuxury
//                            ),
//                            fontWeight = FontWeight.Bold,
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Botones modernos
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        OutlinedButton(
//                            onClick = onBack,
//                            modifier = Modifier
//                                .weight(1f)
//                                .height(48.dp),
//                            shape = RoundedCornerShape(8.dp),
//                            colors = ButtonDefaults.outlinedButtonColors(
//                                contentColor = Color.White,
//                            ),
//                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient)
//                        ) {
//                            Text("Cancelar", style = MaterialTheme.typography.labelLarge)
//                        }
//
//                        Button(
//                            onClick = {
//                                val operation = IOperation(
//                                    id = 0,
//                                    serial = "",
//                                    correlative = 0,
//                                    documentType = "01",
//                                    operationType = "0101",
//                                    operationStatus = "01",
//                                    operationAction = "E",
//                                    currencyType = "PEN",
//                                    operationDate = getCurrentFormattedDate(),
//                                    emitDate = getCurrentFormattedDate(),
//                                    emitTime = getCurrentFormattedTime(),
//                                    userId = userData?.id!!,
//                                    subsidiaryId = subsidiaryData?.id!!,
//                                    client = clientData ?: IPerson(),
//                                    operationDetailSet = operationDetails,
//                                    discountGlobal = 0.0,
//                                    discountPercentageGlobal = 0.0,
//                                    discountForItem = 0.0,
//                                    totalTaxed = totalValue,
//                                    totalUnaffected = 0.0,
//                                    totalExonerated = 0.0,
//                                    totalIgv = totalIgv,
//                                    totalFree = 0.0,
//                                    totalAmount = totalAmount,
//                                    totalToPay = totalAmount,
//                                    totalPayed = totalAmount
//                                )
//                                viewModel.createInvoice(operation) { operationId ->
//                                    onInvoiceCreated(operationId.toString())
//                                }
//                            },
//                            modifier = Modifier
//                                .weight(1f)
//                                .height(48.dp),
//                            shape = RoundedCornerShape(8.dp),
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.inverseSurface,
//                                contentColor = Color.White
//                            ),
//                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient),
//                            elevation = ButtonDefaults.buttonElevation(
//                                defaultElevation = 2.dp,
//                                pressedElevation = 4.dp
//                            ),
//                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
//                        ) {
//                            Text("Emitir Factura", style = MaterialTheme.typography.labelLarge)
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
//                viewModel = viewModel,
//                subsidiaryId = subsidiaryData?.id ?: 0
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
//                title = { Text("Error", style = MaterialTheme.typography.titleSmall) },
//                text = { Text(errorMessage, style = MaterialTheme.typography.bodySmall) },
//                confirmButton = {
//                    TextButton(
//                        onClick = { viewModel.clearError() },
//                        shape = RoundedCornerShape(8.dp)
//                    ) {
//                        Text("Aceptar", style = MaterialTheme.typography.labelLarge)
//                    }
//                },
//                shape = RoundedCornerShape(12.dp)
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
//    viewModel: NewInvoiceViewModel,
//    subsidiaryId: Int = 0
//) {
//    var searchQuery by remember { mutableStateOf("") }
//    val searchState by viewModel.searchState.collectAsState()
//    val selectedProduct by viewModel.selectedProduct.collectAsState()
//
//    // Estados para el producto seleccionado
//    var quantity by remember { mutableStateOf("1") }
//    var discount by remember { mutableStateOf("0.00") }
//
//    // Precios con IGV y sin IGV (se actualizan cuando se selecciona un producto)
//    var priceWithIgv by remember(selectedProduct) {
//        mutableStateOf(selectedProduct?.priceWithIgv?.toString() ?: "0.00")
//    }
//    var priceWithoutIgv by remember(selectedProduct) {
//        mutableStateOf(selectedProduct?.priceWithoutIgv?.toString() ?: "0.00")
//    }
//
//    // Cálculos para el resumen
//    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
//    val priceValue = priceWithIgv.toDoubleOrNull() ?: 0.0
//    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
//    val discountValue = discount.toDoubleOrNull() ?: 0.0
//
//    val igvPercentage = 0.18
//    val subtotal = priceWithoutIgvValue * qtyValue
//    val igvAmount = subtotal * igvPercentage
//    val total = subtotal + igvAmount - discountValue
//
//    // Debounce para la búsqueda
//    LaunchedEffect(searchQuery) {
//        if (searchQuery.length >= 3) {
//            delay(350) // Tiempo de debounce
//            viewModel.searchProductsByQuery(searchQuery, subsidiaryId)
//        }
//    }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(
//            dismissOnClickOutside = false, // <- esto evita el cierre al hacer clic fuera
//            usePlatformDefaultWidth = false
//        )
////        properties = DialogProperties(usePlatformDefaultWidth = false)
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(0.95f)
//                .heightIn(max = 700.dp),
//            shape = RoundedCornerShape(15.dp),
//            tonalElevation = 6.dp
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(top = 5.dp, start = 18.dp, end = 18.dp, bottom = 10.dp)
//                    .fillMaxWidth()
//                    .verticalScroll(rememberScrollState())
//            ) {
//                // Encabezado del diálogo
//                Row(
//                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 0.dp),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "Agregar Producto",
//                        style = MaterialTheme.typography.titleSmall.copy(
//                            fontWeight = FontWeight.Bold
//                        )
//                    )
//
//                    IconButton(
//                        onClick = onDismiss,
//                        modifier = Modifier
//                            .size(20.dp)
//                            .background(
//                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
//                                shape = CircleShape
//                            )
//                    ) {
//                        Icon(
//                            Icons.Default.Close,
//                            contentDescription = "Cerrar",
//                            tint = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Campo de búsqueda con autocompletado
//                OutlinedTextField(
//                    value = searchQuery,
//                    onValueChange = { searchQuery = it },
//                    modifier = Modifier.fillMaxWidth(),
//                    label = { Text("Buscar producto") },
//                    placeholder = { Text("Ingrese 3 caracteres") },
//                    leadingIcon = {
//                        Icon(
//                            imageVector = Icons.Default.Search,
//                            contentDescription = "Buscar",
//                            tint = MaterialTheme.colorScheme.primary
//                        )
//                    },
//                    trailingIcon = {
//                        if (searchQuery.isNotEmpty()) {
//                            IconButton(onClick = { searchQuery = "" }) {
//                                Icon(
//                                    imageVector = Icons.Default.Close,
//                                    contentDescription = "Limpiar",
//                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//                    },
//                    colors = OutlinedTextFieldDefaults.colors(
//                        focusedBorderColor = MaterialTheme.colorScheme.primary,
//                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
//                        focusedContainerColor = MaterialTheme.colorScheme.surface,
//                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
//                    ),
//                    shape = RoundedCornerShape(12.dp),
//                    singleLine = true
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Resultados de la búsqueda
//                AnimatedVisibility(
//                    visible = searchQuery.length >= 3 && selectedProduct == null,
//                    enter = fadeIn() + expandVertically(),
//                    exit = fadeOut() + shrinkVertically()
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .heightIn(max = 250.dp)
//                    ) {
//                        when (searchState) {
//                            is ProductSearchState.Loading -> {
//                                Box(
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .height(100.dp),
//                                    contentAlignment = Alignment.Center
//                                ) {
//                                    CircularProgressIndicator(
//                                        modifier = Modifier.size(35.dp),
//                                        strokeWidth = 3.dp,
//                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
//                                    )
//                                }
//                            }
//                            is ProductSearchState.Success -> {
//                                val products = (searchState as ProductSearchState.Success).products
//                                Card(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    shape = RoundedCornerShape(12.dp),
//                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
//                                    colors = CardDefaults.cardColors(
//                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
//                                    )
//                                ) {
//                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(min = 350.dp, max = 350.dp)) {
//                                        itemsIndexed(products) { index, product ->
//                                            ProductListItem(
//                                                product = product,
//                                                onClick = {
//                                                    viewModel.getTariff(product.id, subsidiaryId)
//                                                }
//                                            )
//
//                                            if (index < products.size - 1) {
//                                                Divider(
//                                                    modifier = Modifier.padding(horizontal = 16.dp),
//                                                    thickness = 0.5.dp,
//                                                    color = MaterialTheme.colorScheme.outlineVariant
//                                                )
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            is ProductSearchState.Empty -> {
//                                EmptySearchResult()
//                            }
//                            is ProductSearchState.Error -> {
//                                SearchError((searchState as ProductSearchState.Error).message)
//                            }
//                            else -> {
//                                // Estado Idle
//                                if (searchQuery.isNotEmpty()) {
//                                    MinimumSearchInfo()
//                                }
//                            }
//                        }
//                    }
//                }
//
//                // Detalles del producto seleccionado
//                AnimatedVisibility(
//                    visible = selectedProduct != null,
//                    enter = fadeIn() + expandVertically(),
//                    exit = fadeOut() + shrinkVertically()
//                ) {
//                    selectedProduct?.let { product ->
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(top = 8.dp)
//                        ) {
//                            // Card con datos del producto seleccionado
//                            SelectedProductCard(
//                                product = product,
//                                onClear = { viewModel.clearProductSelection() }
//                            )
//
//                            Spacer(modifier = Modifier.height(24.dp))
//
//                            // Sección de cantidad, precio y descuento
//                            Text(
//                                "Detalle de venta",
//                                style = MaterialTheme.typography.titleMedium,
//                                fontWeight = FontWeight.SemiBold
//                            )
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Cantidad y Descuento
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(10.dp)
//                            ) {
//                                OutlinedTextField(
//                                    value = quantity,
//                                    onValueChange = { quantity = it },
//                                    label = { Text("Cantidad") },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//
//                                OutlinedTextField(
//                                    value = discount,
//                                    onValueChange = { discount = it },
//                                    label = { Text("Descuento S/") },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(8.dp))
//
//                            // Precios
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.spacedBy(16.dp)
//                            ) {
//                                OutlinedTextField(
//                                    value = priceWithoutIgv,
//                                    onValueChange = {
//                                        priceWithoutIgv = it
//                                        // Calcular precio con IGV
//                                        val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithIgv = (withoutIgvValue * (1 + igvPercentage)).toString()
//                                    },
//                                    textStyle = MaterialTheme.typography.bodyMedium,
//                                    label = { Text("Precio sin IGV") },
//                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//
//                                OutlinedTextField(
//                                    value = priceWithIgv,
//                                    onValueChange = {
//                                        priceWithIgv = it
//                                        // Calcular precio sin IGV
//                                        val withIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithoutIgv = (withIgvValue / (1 + igvPercentage)).toString()
//                                    },
//                                    textStyle = MaterialTheme.typography.bodyMedium,
//                                    label = { Text("Precio con IGV") },
//                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
//                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                    modifier = Modifier.weight(1f),
//                                    shape = RoundedCornerShape(12.dp)
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(15.dp))
//
//                            // Resumen de la venta
//                            PurchaseSummary(
//                                subtotal = subtotal,
//                                igv = igvAmount,
//                                discount = discountValue,
//                                total = total
//                            )
//
//                            Spacer(modifier = Modifier.height(15.dp))
//
//                            // Botón agregar
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .height(56.dp)
//                                    .shadow(
//                                        elevation = 4.dp,
//                                        shape = RoundedCornerShape(16.dp),
//                                        spotColor = MaterialTheme.colorScheme.primary
//                                    )
//                                    .background(
//                                        brush = ColorGradients.blueButtonGradient,
//                                        shape = RoundedCornerShape(16.dp)
//                                    )
//                                    .clickable {
//                                        val tariff = ITariff(
//                                            productId = product.productId,
//                                            productCode = product.productCode,
//                                            productName = product.productName,
//                                            unitId = product.unitId,
//                                            unitName = product.unitName,
//                                            remainingQuantity = product.remainingQuantity,
//                                            priceWithIgv = priceWithIgv.toDoubleOrNull() ?: 0.0,
//                                            priceWithoutIgv = priceWithoutIgv.toDoubleOrNull() ?: 0.0,
//                                            productTariffId = product.productTariffId,
//                                            typeAffectationId = product.typeAffectationId
//                                        )
//
//                                        val operationDetail = IOperationDetail(
//                                            id = 0,
//                                            tariff = tariff,
//                                            typeAffectationId = product.typeAffectationId,
//                                            quantity = qtyValue,
//                                            unitValue = priceWithoutIgvValue,
//                                            unitPrice = priceValue,
//                                            totalDiscount = discountValue,
//                                            discountPercentage = if (subtotal > 0) (discountValue / subtotal) * 100 else 0.0,
//                                            igvPercentage = igvPercentage * 100,
//                                            perceptionPercentage = 0.0,
//                                            totalPerception = 0.0,
//                                            totalValue = when (product.typeAffectationId) {
//                                                1 -> subtotal  // Operación gravada
//                                                2 -> subtotal  // Operación exonerada
//                                                3 -> subtotal  // Operación inafecta
//                                                4 -> 0.0       // Operación gratuita (valor comercial en totalValue)
//                                                else -> subtotal
//                                            },
//                                            totalIgv = if (product.typeAffectationId == 1) igvAmount else 0.0,
//                                            totalAmount = when (product.typeAffectationId) {
//                                                1 -> subtotal + igvAmount - discountValue  // Gravada: Base + IGV - Descuento
//                                                2, 3 -> subtotal - discountValue           // Exonerada/Inafecta: Base - Descuento
//                                                4 -> 0.0                                   // Gratuita: Se registra 0
//                                                else -> subtotal + igvAmount - discountValue
//                                            },
//                                            totalToPay = total
//                                        )
//
//                                        onProductAdded(operationDetail)
//                                    }
//                                    .border(
//                                        width = 1.dp,
//                                        brush = Brush.linearGradient(
//                                            colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
//                                        ),
//                                        shape = RoundedCornerShape(16.dp)
//                                    ),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Row(
//                                    verticalAlignment = Alignment.CenterVertically,
//                                    horizontalArrangement = Arrangement.Center,
//                                    modifier = Modifier.padding(horizontal = 16.dp)
//                                ) {
//                                    Icon(
//                                        imageVector = Icons.Default.ShoppingCart,
//                                        contentDescription = null,
//                                        modifier = Modifier.size(24.dp),
//                                        tint = Color.White
//                                    )
//                                    Spacer(modifier = Modifier.width(8.dp))
//                                    Text(
//                                        "Agregar Producto",
//                                        style = MaterialTheme.typography.labelMedium.copy(
//                                            color = Color.White,
//                                            fontWeight = FontWeight.SemiBold
//                                        )
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun ProductListItem(
//    product: IProduct,
//    onClick: () -> Unit
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier.fillMaxWidth(),
//        color = Color.Transparent
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 10.dp, vertical = 10.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Icono o imagen del producto
//            Box(
//                modifier = Modifier
//                    .size(35.dp)
//                    .background(
//                        brush = Brush.radialGradient(
//                            colors = listOf(
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
//                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
//                            )
//                        ),
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Inventory,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.surface,
//                    modifier = Modifier.size(15.dp)
//                )
//            }
//
//            Spacer(modifier = Modifier.width(14.dp))
//
//            // Información del producto
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = product.name,
//                    style = MaterialTheme.typography.bodySmall,
//                    fontWeight = FontWeight.SemiBold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Text(
//                    text = "Código: ${product.code}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Spacer(modifier = Modifier.width(8.dp))
//
//            // Ícono de selección
//            Icon(
//                imageVector = Icons.Default.KeyboardArrowRight,
//                contentDescription = "Seleccionar",
//                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
//                modifier = Modifier.size(20.dp)
//            )
//        }
//    }
//}
//@Composable
//private fun SelectedProductCard(
//    product: ITariff,
//    onClear: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = 4.dp,
//            pressedElevation = 8.dp
//        )
//    ) {
//        Box(
//            modifier = Modifier
//                .background(
//                    brush = Brush.verticalGradient(
//                        colors = listOf(
//                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
//                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
//                        )
//                    ),
//                    shape = RoundedCornerShape(16.dp)
//                )
//        ) {
//            Column(modifier = Modifier.padding(16.dp)) {
//                // Header
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        "PRODUCTO SELECCIONADO",
//                        style = MaterialTheme.typography.labelSmall.copy(
//                            brush = ColorGradients.blueVibrant
//                        ),
//                        color = MaterialTheme.colorScheme.primary
//                    )
//
//                    IconButton(
//                        onClick = onClear,
//                        modifier = Modifier
//                            .size(28.dp)
//                            .background(
//                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
//                                shape = CircleShape
//                            ),
//                        colors = IconButtonDefaults.iconButtonColors(
//                            contentColor = MaterialTheme.colorScheme.error
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Close,
//                            contentDescription = "Cambiar selección",
//                            modifier = Modifier.size(16.dp)
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(6.dp))
//
//                // Nombre del producto
//                Text(
//                    text = product.productName,
//                    style = MaterialTheme.typography.titleSmall.copy(
//                        fontWeight = FontWeight.ExtraBold
//                    ),
//                    color = MaterialTheme.colorScheme.onSurface,
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(modifier = Modifier.height(10.dp))
//
//                // Detalles
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Column {
//                        InfoRow(
//                            label = "Código:",
//                            value = product.productCode,
//                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(modifier = Modifier.height(4.dp))
//                        InfoRow(
//                            label = "Stock:",
//                            value = "${product.remainingQuantity} ${product.unitName}",
//                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//
//                    // Precio con mejor estilo
//                    Box(
//                        modifier = Modifier
//                            .background(
//                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                                shape = RoundedCornerShape(12.dp)
//                            )
//                            .padding(horizontal = 12.dp, vertical = 8.dp)
//                    ) {
//                        Text(
//                            text = "S/ ${"%.2f".format(product.priceWithIgv)}",
//                            style = MaterialTheme.typography.titleMedium.copy(
//                                    brush = ColorGradients.orangeSunset,
//                                    fontWeight = FontWeight.Bold,
//                                    )
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun InfoRow(
//    label: String,
//    value: String,
//    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
//) {
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        Text(
//            text = label,
//            style = MaterialTheme.typography.bodySmall,
//            color = labelColor,
//            modifier = Modifier.width(60.dp)
//        )
//        Text(
//            text = value,
//            style = MaterialTheme.typography.bodyMedium.copy(
//                fontWeight = FontWeight.SemiBold
//            ),
//            color = MaterialTheme.colorScheme.onSurface
//        )
//    }
//}
//
//@Composable
//private fun PriceTag(price: Double) {
//    Card(
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
//        ),
//        shape = RoundedCornerShape(8.dp)
//    ) {
//        Row(
//            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "S/",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.primary
//            )
//            Spacer(modifier = Modifier.width(2.dp))
//            Text(
//                text = String.format("%.2f", price),
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.primary
//            )
//        }
//    }
//}
//
//@Composable
//private fun EmptySearchResult() {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 32.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = Icons.Default.SearchOff,
//                contentDescription = null,
//                modifier = Modifier.size(48.dp),
//                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "No se encontraron productos",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//@Composable
//private fun SearchError(errorMessage: String) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = Icons.Default.Error,
//                contentDescription = null,
//                modifier = Modifier.size(40.dp),
//                tint = MaterialTheme.colorScheme.error
//            )
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "Error en la búsqueda",
//                style = MaterialTheme.typography.titleMedium,
//                color = MaterialTheme.colorScheme.error
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                errorMessage,
//                style = MaterialTheme.typography.bodyMedium,
//                color = MaterialTheme.colorScheme.onErrorContainer
//            )
//        }
//    }
//}
//
//@Composable
//private fun MinimumSearchInfo() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 16.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            "Ingrese al menos 3 caracteres para buscar",
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant
//        )
//    }
//}
//
//@Composable
//private fun PurchaseSummary(
//    subtotal: Double,
//    igv: Double,
//    discount: Double,
//    total: Double
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                "Resumen",
//                style = MaterialTheme.typography.titleSmall,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SummaryRow(
//                label = "Subtotal:",
//                value = subtotal
//            )
//
//            SummaryRow(
//                label = "IGV (18%):",
//                value = igv
//            )
//
//            SummaryRow(
//                label = "Descuento:",
//                value = -discount,
//                valueColor = if (discount > 0) MaterialTheme.colorScheme.tertiary else null
//            )
//
//            Divider(
//                modifier = Modifier.padding(vertical = 10.dp),
//                thickness = 1.dp,
//                color = MaterialTheme.colorScheme.outlineVariant
//            )
//
//            SummaryRow(
//                label = "TOTAL:",
//                value = total,
//                isTotal = true
//            )
//        }
//    }
//}
//
//@Composable
//private fun SummaryRow(
//    label: String,
//    value: Double,
//    isTotal: Boolean = false,
//    valueColor: Color? = null
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = label,
//            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
//            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
//        )
//
//        Text(
//            text = "S/ ${String.format("%.2f", value)}",
////            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
//            style = MaterialTheme.typography.titleMedium.copy(
//                brush = if (isTotal) ColorGradients.orangeFire else ColorGradients.orangeSunset
//            ),
//            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
//            color = when {
//                valueColor != null -> valueColor
//                isTotal -> MaterialTheme.colorScheme.primary
//                else -> MaterialTheme.colorScheme.onSurface
//            }
//        )
//    }
//}
