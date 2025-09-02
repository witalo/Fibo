package com.example.fibo.ui.screens.purchase

import android.widget.Toast
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.*
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.utils.getCurrentFormattedTime
import com.example.fibo.viewmodels.NewPurchaseViewModel
import com.example.fibo.viewmodels.ProductSearchState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPurchaseScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    onLogout: () -> Unit,
    viewModel: NewPurchaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Estados locales para el formulario
    var selectedDocumentType by remember { mutableStateOf("01") } // 01 = Factura, 03 = Boleta
    var manualSerial by remember { mutableStateOf("") } // Serie ingresada manualmente
    var manualCorrelative by remember { mutableStateOf("") } // Correlativo ingresado manualmente
    var invoiceDate by remember { mutableStateOf(getCurrentFormattedDate()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var showDocumentTypeDialog by remember { mutableStateOf(false) } // ✅ MOVER AQUÍ
    var supplierSearchQuery by remember { mutableStateOf("") } // Para buscar proveedor por RUC/DNI
    var showProductDialog by remember { mutableStateOf(false) } // Para buscar productos
    var productSearchQuery by remember { mutableStateOf("") } // Para buscar productos
    var showProductEditDialog by remember { mutableStateOf(false) } // Para editar productos
    var selectedProductForEdit by remember { mutableStateOf<IProductOperation?>(null) } // Producto seleccionado para editar

    // ✅ MOVER AQUÍ: Declarar paymentsEnabled y currentPayments al nivel del composable principal
    val paymentsEnabled by viewModel.paymentsEnabled.collectAsState()
    val currentPayments by viewModel.payments.collectAsState()

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        viewModel.loadInitialData(subsidiaryData?.id ?: 0)
    }

    // Manejar resultado de la operación
    LaunchedEffect(uiState.purchaseResult) {
        uiState.purchaseResult?.let { result ->
            result.fold(
                onSuccess = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                },
                onFailure = { error ->
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }



    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
//                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Información General
            item {
                GeneralInformationSection(
                    selectedDocumentType = selectedDocumentType,
                    onDocumentTypeClick = { showDocumentTypeDialog = true }, // ✅ Pasar la función
                    manualSerial = manualSerial,
                    onSerialChange = { manualSerial = it },
                    manualCorrelative = manualCorrelative,
                    onCorrelativeChange = { manualCorrelative = it },
                    invoiceDate = invoiceDate,
                    onDateClick = { showDatePicker = true }
                )
            }

            // Sección de Datos del Proveedor
            item {
                SupplierDataSection(
                    supplier = uiState.supplier,
                    supplierSearchQuery = supplierSearchQuery,
                    onSupplierSearchQueryChange = {
                        supplierSearchQuery = it
                        if (it.length >= 8) {
                            viewModel.searchSupplierByDocument(it)
                        }
                    },
                    onSupplierClick = { showSupplierDialog = true },
                    documentType = selectedDocumentType,
                    searchResults = viewModel.supplierSearchResults.collectAsState().value,
                    isSearching = viewModel.isSearchingSupplier.collectAsState().value,
                    viewModel = viewModel // ✅ Agregar viewModel como parámetro
                )
            }

            // Sección de Productos
            item {
                ProductsSection(
                    products = uiState.products,
                    onAddProduct = { showProductDialog = true },
                    viewModel = viewModel,
                    onEditProduct = { product ->
                        selectedProductForEdit = product
                        showProductEditDialog = true
                    }
                )
            }

            // Sección de Pagos (si están habilitados)
            item {
                if (paymentsEnabled) {
                    PaymentsSection(
                        payments = uiState.payments,
                        onAddPayment = {
                            // ✅ Calcular total de productos
                            val totalAmount = uiState.products.sumOf { it.priceWithIgv3 ?: 0.0 }
                            viewModel.showPaymentDialog(totalAmount)
                        },
                        onRemovePayment = { paymentId -> viewModel.removePayment(paymentId) },
                        totalAmount = uiState.products.sumOf { it.priceWithIgv3 ?: 0.0 }, // ✅ Usar productos
                        paymentSummary = viewModel.paymentSummary.collectAsState().value
                    )
                }
            }


            // Botón de Crear Compra
            item {
                CreatePurchaseButton(
                    isEnabled = viewModel.isFormValid(),
                    isLoading = uiState.isLoading,
                    onClick = {
                        // ✅ Construir la operación EXACTAMENTE como en NoteOfSale
                        val operation = IOperation(
                            id = 0, // ID se generará en el backend
                            serial = manualSerial, // Serie ingresada manualmente
                            correlative = manualCorrelative.toIntOrNull() ?: 0, // Correlativo ingresado manualmente
                            documentType = selectedDocumentType, // 01 = Factura, 03 = Boleta
                            operationType = when (selectedDocumentType) {
                                "01" -> "0501" // Factura de proveedor
                                "03" -> "0501" // Boleta de proveedor
                                else -> "0501"
                            },
                            operationStatus = "01", // Pendiente de envío a SUNAT
                            operationAction = "E", // Emitir
                            currencyType = "PEN", // Soles peruanos
                            operationDate = getCurrentFormattedDate(), // Fecha actual
                            emitDate = invoiceDate, // Fecha de emisión
                            emitTime = getCurrentFormattedTime(), // Hora actual
                            userId = 0, // TODO: Obtener del usuario logueado
                            subsidiaryId = subsidiaryData?.id ?: 0, // Sucursal
                            client = null, // No hay cliente en compras
                            supplier = uiState.supplier?.copy( // Solo cambiar esto: cliente por proveedor
                                documentType = uiState.supplier!!.documentType,
                                documentNumber = uiState.supplier!!.documentNumber?.trim() ?: "", // ✅ Agregar ?: "" para evitar null
                                names = uiState.supplier!!.names?.trim()?.uppercase() ?: "", // ✅ Agregar ?: "" para evitar null
                                address = uiState.supplier!!.address?.trim() ?: "", // ✅ Agregar ?: "" para evitar null
                                email = uiState.supplier!!.email ?: "", // ✅ Agregar ?: "" para evitar null
                                phone = uiState.supplier!!.phone ?: "" // ✅ Agregar ?: "" para evitar null
                            ) ?: run {
                                Toast.makeText(
                                    context,
                                    "Complete datos del proveedor",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@CreatePurchaseButton
                            },
                            operationDetailSet = uiState.products.map { product ->
                                IOperationDetail(
                                    id = 0,
                                    typeAffectationId = 1, // Por defecto gravada
                                    description = product.name ?: "Producto",
                                    tariff = ITariff(
                                        productId = product.id,
                                        productCode = product.code,
                                        productName = product.name,
                                        unitId = 0,
                                        unitName = product.minimumUnitName,
                                        stock = product.stock,
                                        priceWithIgv = product.priceWithIgv3 ?: 0.0,
                                        priceWithoutIgv = product.priceWithoutIgv3 ?: 0.0,
                                        productTariffId = 0,
                                        typeAffectationId = 1
                                    ),
                                    quantity = 1.0, // TODO: Obtener cantidad del producto
                                    unitValue = product.priceWithoutIgv3 ?: 0.0,
                                    unitPrice = product.priceWithIgv3 ?: 0.0,
                                    discountPercentage = 0.0,
                                    totalDiscount = 0.0,
                                    perceptionPercentage = 0.0,
                                    totalPerception = 0.0,
                                    igvPercentage = 18.0,
                                    totalValue = product.priceWithoutIgv3 ?: 0.0,
                                    totalIgv = (product.priceWithIgv3 ?: 0.0) - (product.priceWithoutIgv3 ?: 0.0),
                                    totalAmount = product.priceWithIgv3 ?: 0.0,
                                    totalToPay = product.priceWithIgv3 ?: 0.0
                                )
                            },
                            discountGlobal = 0.0,
                            discountPercentageGlobal = 0.0,
                            discountForItem = 0.0,
                            totalDiscount = 0.0,
                            totalTaxed = uiState.products.sumOf { it.priceWithoutIgv3 ?: 0.0 },
                            totalUnaffected = 0.0,
                            totalExonerated = 0.0,
                            totalIgv = uiState.products.sumOf { (it.priceWithIgv3 ?: 0.0) - (it.priceWithoutIgv3 ?: 0.0) },
                            totalFree = 0.0,
                            totalAmount = uiState.products.sumOf { it.priceWithIgv3 ?: 0.0 },
                            totalToPay = uiState.products.sumOf { it.priceWithIgv3 ?: 0.0 },
                            totalPayed = uiState.products.sumOf { it.priceWithIgv3 ?: 0.0 }
                        )

                        if (paymentsEnabled) { // ✅ Usar paymentsEnabled que ya está declarado arriba
                            // disableContinuePay = false → Enviar pagos registrados
                            viewModel.createPurchase(
                                operation = operation,
                                payments = currentPayments, // ✅ Usar currentPayments que ya está declarado arriba
                                onSuccess = { operationId, message ->
                                    Toast.makeText(
                                        context,
                                        "Compra $message creada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                            )
                        } else {
                            // disableContinuePay = true → Backend maneja pago automático
                            viewModel.createPurchase(
                                operation = operation,
                                payments = emptyList(),
                                onSuccess = { operationId, message ->
                                    Toast.makeText(
                                        context,
                                        "Compra $message creada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                )
            }
        }
        // Diálogos - CON MÁS LOGS
        println("DEBUG: ===== EVALUANDO DIÁLOGOS =====")
        println("DEBUG: showDocumentTypeDialog = $showDocumentTypeDialog")

        if (showDocumentTypeDialog) {
            println("DEBUG: ===== MOSTRANDO DocumentTypeDialog =====")
            DocumentTypeDialog(
                selectedType = selectedDocumentType,
                onTypeSelected = { newType ->
                    println("DEBUG: Tipo seleccionado: $newType")
                    selectedDocumentType = newType
                    showDocumentTypeDialog = false
                },
                onDismiss = {
                    println("DEBUG: Cerrando diálogo")
                    showDocumentTypeDialog = false
                }
            )
        } else {
            println("DEBUG: NO se muestra DocumentTypeDialog")
        }

        if (showDatePicker) {
            DatePickerDialog(
                currentDate = invoiceDate,
                onDateSelected = {
                    invoiceDate = it
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        if (showSupplierDialog) {
            SupplierSelectionDialog(
                suppliers = uiState.supplierSearchResults,
                onSupplierSelected = {
                    viewModel.selectSupplier(it)
                    // ✅ Usar la variable local directamente en lugar de onSupplierSearchQueryChange
                    supplierSearchQuery = ""
                    showSupplierDialog = false
                },
                onDismiss = { showSupplierDialog = false }
            )
        }

        // Diálogo de pagos
        val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
        if (showPaymentDialog) {
            PaymentDialog(
                onDismiss = { viewModel.hidePaymentDialog() },
                onPaymentAdded = { payment ->
                    viewModel.addPayment(payment)
                },
                paymentSummary = viewModel.paymentSummary.collectAsState().value
            )
        }

        // Diálogo de búsqueda de productos
        if (showProductDialog) {
            ProductSearchDialog(
                onDismiss = { showProductDialog = false },
                searchQuery = productSearchQuery,
                onSearchQueryChange = { query ->
                    productSearchQuery = query
                    if (query.length >= 3) {
                        viewModel.searchProductsByQuery(query, subsidiaryData?.id ?: 0)
                    }
                },
                searchResults = viewModel.searchResults.collectAsState().value,
                searchState = viewModel.searchState.collectAsState().value,
                onProductSelected = { product ->
                    selectedProductForEdit = product
                    showProductEditDialog = true
                    showProductDialog = false
                    productSearchQuery = ""
                }
            )
        }

        // Diálogo de edición de productos
        if (showProductEditDialog && selectedProductForEdit != null) {
            ProductEditDialog(
                product = selectedProductForEdit!!,
                onDismiss = { 
                    showProductEditDialog = false
                    selectedProductForEdit = null
                },
                onSave = { configuredProduct ->
                    // Agregar el producto configurado
                    viewModel.addProduct(configuredProduct)
                    showProductEditDialog = false
                    selectedProductForEdit = null
                }
            )
        }
    }
}

@Composable
fun GeneralInformationSection(
    selectedDocumentType: String,
    onDocumentTypeClick: () -> Unit, // ✅ Agregar parámetro
    manualSerial: String,
    onSerialChange: (String) -> Unit,
    manualCorrelative: String,
    onCorrelativeChange: (String) -> Unit,
    invoiceDate: String,
    onDateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Información General",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // IGV %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IGV %",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "18%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tipo de documento - VERSIÓN SIMPLIFICADA PARA DEBUG
            OutlinedTextField(
                value = when (selectedDocumentType) {
                    "01" -> "FACTURA ELECTRÓNICA"
                    "03" -> "BOLETA ELECTRÓNICA"
                    else -> "Seleccionar tipo"
                },
                onValueChange = { },
                label = { Text("Tipo documento") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        println("DEBUG: ===== CLICK EN TIPO DOCUMENTO =====")
                        onDocumentTypeClick() // ✅ Usar el parámetro
                        println("DEBUG: ===== FIN CLICK =====")
                    },
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            println("DEBUG: ===== CLICK EN ICONO =====")
                            onDocumentTypeClick() // ✅ Usar el parámetro
                            println("DEBUG: ===== FIN CLICK ICONO =====")
                        }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar tipo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha emisión
            OutlinedTextField(
                value = invoiceDate,
                onValueChange = { },
                label = { Text("Fecha emisión") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick() },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Seleccionar fecha"
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha vencimiento (igual a emisión para compras)
            OutlinedTextField(
                value = invoiceDate,
                onValueChange = { },
                label = { Text("Fecha vencimiento") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Moneda
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moneda",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "S/ PEN - SOLES",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Serie y Número (INGRESO MANUAL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Serie - INGRESO MANUAL
                OutlinedTextField(
                    value = manualSerial,
                    onValueChange = onSerialChange,
                    label = { Text("Serie") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: F001") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                // Número - INGRESO MANUAL
                OutlinedTextField(
                    value = manualCorrelative,
                    onValueChange = onCorrelativeChange,
                    label = { Text("Número") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: 0001") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun SupplierDataSection(
    supplier: ISupplier?,
    supplierSearchQuery: String,
    onSupplierSearchQueryChange: (String) -> Unit,
    onSupplierClick: () -> Unit,
    documentType: String,
    searchResults: List<ISupplier>,
    isSearching: Boolean,
    viewModel: NewPurchaseViewModel // ✅ Agregar viewModel como parámetro
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Datos del Proveedor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Buscar por RUC/DNI o Nombre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (supplier == null) {
                // Campo de búsqueda de proveedor
                OutlinedTextField(
                    value = supplierSearchQuery,
                    onValueChange = onSupplierSearchQueryChange,
                    label = { Text("Buscar proveedor...") },
                    placeholder = {
                        Text(
                            when (documentType) {
                                "01" -> "Ingrese RUC del proveedor"
                                "03" -> "Ingrese RUC o DNI del proveedor"
                                else -> "Ingrese RUC/DNI o nombre"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (supplierSearchQuery.isNotBlank()) {
                            IconButton(onClick = { onSupplierSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )

                // Mostrar resultados de búsqueda
                if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(searchResults) { searchResult ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // ✅ Seleccionar proveedor directamente
                                        viewModel.selectSupplier(searchResult)
                                        // ✅ Usar onSupplierSearchQueryChange para limpiar
                                        onSupplierSearchQueryChange("")
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = searchResult.names ?: "Sin nombre", // ✅ Agregar null safety
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${getDocumentTypeName(searchResult.documentType)}: ${searchResult.documentNumber ?: ""}", // ✅ Usar función helper
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Información adicional según tipo de documento
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (documentType) {
                        "01" -> "Para facturas: Solo se permiten proveedores con RUC"
                        "03" -> "Para boletas: Se permiten RUC o DNI"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Mostrar información del proveedor seleccionado
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = supplier.names,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${getDocumentTypeName(supplier.documentType)}: ${supplier.documentNumber ?: ""}", // ✅ Usar función helper
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (supplier.address!!.isNotBlank()) {
                                    Text(
                                        text = supplier.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                // Limpiar el proveedor actual para permitir buscar uno nuevo
                                viewModel.clearSupplier()
                                onSupplierClick()
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Cambiar proveedor",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerDialog(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf(currentDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Fecha") },
        text = {
            Column {
                // Aquí podrías implementar un DatePicker real
                // Por ahora usamos un campo de texto simple
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { selectedDate = it },
                    label = { Text("Fecha (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("01/09/2025") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedDate)
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ProductsSection(
    products: List<IProductOperation>,
    onAddProduct: () -> Unit,
    viewModel: NewPurchaseViewModel,
    onEditProduct: (IProductOperation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Productos",
                style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (products.isEmpty()) {
        Box(
            modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                Text(
                            text = "No hay productos agregados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Lista de productos
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { product ->
                        ProductOperationCard(
                            product = product,
                            onRemove = { viewModel.removeProduct(product.id) },
                            onEdit = { onEditProduct(product) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para agregar productos
            OutlinedButton(
                onClick = onAddProduct,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Producto")
            }
        }
    }
}

@Composable
fun ProductOperationCard(
    product: IProductOperation,
    onRemove: () -> Unit,
    onEdit: (IProductOperation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name.ifBlank { "Producto sin nombre" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Código: ${product.code} - Stock: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Precio sin IGV: S/. ${String.format("%.2f", product.priceWithoutIgv3)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: S/. ${String.format("%.2f", product.priceWithIgv3 ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                IconButton(onClick = { onEdit(product) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar producto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar producto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


@Composable
fun CreatePurchaseButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit // ✅ Cambiar de @Composable () -> Unit a () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear Compra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// Diálogos
@Composable
fun DocumentTypeDialog(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    println("DEBUG: ===== DocumentTypeDialog COMPOSABLE EJECUTÁNDOSE =====")

    AlertDialog(
        onDismissRequest = {
            println("DEBUG: onDismissRequest llamado")
            onDismiss()
        },
        title = {
            Text(
                "Seleccionar Tipo de Documento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "01" to "FACTURA ELECTRÓNICA",
                    "03" to "BOLETA DE VENTA ELECTRÓNICA"
                ).forEach { (type, name) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                println("DEBUG: ===== CLICK EN CARD: $type =====")
                                onTypeSelected(type)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedType == type) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedType == type) 4.dp else 2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = {
                                    println("DEBUG: ===== CLICK EN RADIOBUTTON: $type =====")
                                    onTypeSelected(type)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedType == type) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = when (type) {
                                        "01" -> "Para proveedores con RUC"
                                        "03" -> "Para proveedores con RUC o DNI"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selectedType == type) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    println("DEBUG: Botón Cancelar clickeado")
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Cancelar",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp)
    )
}


@Composable
fun SupplierSelectionDialog(
    suppliers: List<ISupplier>,
    onSupplierSelected: (ISupplier) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Proveedor") },
        text = {
            if (suppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay proveedores disponibles")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(suppliers) { supplier ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSupplierSelected(supplier) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = supplier.names ?: "Sin nombre", // ✅ Agregar null safety
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${getDocumentTypeName(supplier.documentType)}: ${supplier.documentNumber}", // ✅ Usar función helper
                                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun PaymentsSection(
    payments: List<IPayment>,
    onAddPayment: () -> Unit,
    onRemovePayment: (Int) -> Unit,
    totalAmount: Double,
    paymentSummary: PaymentSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
                Text(
                text = "Pagos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Resumen de pagos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Compra:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "S/. ${String.format("%.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Pagado:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "S/. ${String.format("%.2f", paymentSummary.totalPaid)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Saldo pendiente o a favor
            if (paymentSummary.remaining > 0.01) {
                Text(
                    text = "Saldo Pendiente: S/. ${String.format("%.2f", paymentSummary.remaining)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            } else if (paymentSummary.totalPaid > totalAmount) {
                val overpayment = paymentSummary.totalPaid - totalAmount
                Text(
                    text = "Saldo a Favor: S/. ${String.format("%.2f", overpayment)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "Pago Completo ✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de pagos
            if (payments.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.height(120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(payments) { payment ->
                        PaymentCard(
                            payment = payment,
                            onRemove = { onRemovePayment(payment.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Botón para agregar pago
            OutlinedButton(
                onClick = onAddPayment,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                enabled = paymentSummary.remaining > 0.01
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Pago")
            }
        }
    }
}
// ... existing code hasta línea 1057 ...

@Composable
fun PaymentCard(
    payment: IPayment,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PaymentMethods.getMethodById(payment.wayPay)?.name ?: "Método no especificado",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "S/. ${String.format("%.2f", payment.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        text = "Nota: ${payment.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar pago",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun PaymentDialog(
    onDismiss: () -> Unit,
    onPaymentAdded: (IPayment) -> Unit,
    paymentSummary: PaymentSummary
) {
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Pago") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Método de pago
                OutlinedTextField(
                    value = selectedPaymentMethod?.name ?: "Seleccionar método",
                    onValueChange = { },
                    label = { Text("Método de Pago") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentMethodDialog = true },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar"
                        )
                    }
                )

                // Monto
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto (S/.)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Máximo: ${String.format("%.2f", paymentSummary.remaining)}") }
                )

                // Nota (opcional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Transferencia, Cheque N°") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (selectedPaymentMethod != null && amountValue > 0 && amountValue <= paymentSummary.remaining) {
                        val payment = IPayment(
                            id = 0,
                            wayPay = selectedPaymentMethod!!.id,
                            amount = amountValue,
                            note = note.trim(),
                            paymentDate = getCurrentFormattedDate(),
                            operationId = 0
                        )
                        onPaymentAdded(payment)
                        onDismiss() // Cerrar diálogo después de agregar
                    }
                },
                enabled = selectedPaymentMethod != null &&
                        amount.isNotBlank() &&
                        (amount.toDoubleOrNull() ?: 0.0) > 0 &&
                        (amount.toDoubleOrNull() ?: 0.0) <= paymentSummary.remaining
            ) {
                Text("Agregar Pago")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Diálogo para seleccionar método de pago
    if (showPaymentMethodDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentMethodDialog = false },
            title = { Text("Seleccionar Método de Pago") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(PaymentMethods.AVAILABLE_METHODS) { method ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedPaymentMethod = method
                                    showPaymentMethodDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = method.name,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentMethodDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// Funciones de utilidad
fun getCurrentFormattedDate(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date())
}

fun getCurrentFormattedTime(): String {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return timeFormat.format(Date())
}
@Composable
fun ProductEditDialog(
    product: IProductOperation,
    onDismiss: () -> Unit,
    onSave: (IProductOperation) -> Unit
) {
    // Estados para edición - inicializar con valores del producto
    var quantity by remember { mutableStateOf("1.0") }
    var priceWithIgv by remember { mutableStateOf("0.00") }
    var priceWithoutIgv by remember { mutableStateOf("0.00") }
    var selectedAffectationType by remember { mutableStateOf(1) } // 1 = Gravada por defecto
    var showAffectationDialog by remember { mutableStateOf(false) }

    // IGV de la empresa (por defecto 18%)
    val igvPercentage = 18.0 // TODO: Obtener de PreferencesManager
    val igvFactor = igvPercentage / 100.0

    // Inicializar precios del producto
    LaunchedEffect(product) {
        val productPrice = product.priceWithIgv3 ?: 0.0
        priceWithIgv = String.format("%.2f", productPrice)
        
        // Calcular precio sin IGV inicial
        priceWithoutIgv = when (selectedAffectationType) {
            1 -> String.format("%.2f", productPrice / (1 + igvFactor)) // Gravado: quitar IGV
            else -> String.format("%.2f", productPrice) // Exonerado/Inafecto: mismo valor
        }
    }

    // Recalcular precio sin IGV cuando cambie el precio con IGV o el tipo de afectación
    LaunchedEffect(priceWithIgv, selectedAffectationType) {
        val withIgvValue = priceWithIgv.toDoubleOrNull() ?: 0.0
        priceWithoutIgv = when (selectedAffectationType) {
            1 -> String.format("%.2f", withIgvValue / (1 + igvFactor)) // Gravado: quitar IGV
            else -> String.format("%.2f", withIgvValue) // Exonerado/Inafecto: mismo valor
        }
    }

    // Convertir valores a números
    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
    val priceWithIgvValue = priceWithIgv.toDoubleOrNull() ?: 0.0

    // CÁLCULOS SEGÚN SUNAT (SIN DESCUENTOS):
    // 1. Calcular subtotal (cantidad × precio sin IGV)
    val subtotal = priceWithoutIgvValue * qtyValue

    // 2. Calcular IGV solo para operaciones gravadas (tipo 1)
    val igvAmount = if (selectedAffectationType == 1) {
        subtotal * igvFactor
    } else {
        0.0
    }

    // 3. Calcular total según tipo de operación
    val totalAmount = when (selectedAffectationType) {
        1 -> subtotal + igvAmount  // Gravada: Subtotal + IGV
        2, 3, 4 -> subtotal           // Exonerada/Inafecta: Solo subtotal
        else -> subtotal + igvAmount
    }

    // Debug temporal
    println("DEBUG ProductEditDialog:")
    println("  qtyValue: $qtyValue")
    println("  priceWithoutIgvValue: $priceWithoutIgvValue")
    println("  priceWithIgvValue: $priceWithIgvValue")
    println("  selectedAffectationType: $selectedAffectationType")
    println("  subtotal: $subtotal")
    println("  igvAmount: $igvAmount")
    println("  totalAmount: $totalAmount")



    // Lista de tipos de afectación
    val affectationTypes = listOf(
        "1" to "Gravada",
        "2" to "Exonerada", 
        "3" to "Inafecta",
        "4" to "Gratuita"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Configurar Producto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // ✅ HACER SCROLLABLE
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Información del producto
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = product.name ?: "Sin nombre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Código: ${product.code}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Stock disponible: ${product.stock}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cantidad
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d{0,2}\$")) || newValue.isEmpty()) {
                            quantity = newValue
                        }
                    },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("1.0") }
                )

                // Precio con IGV (editable)
                OutlinedTextField(
                    value = priceWithIgv,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d{0,4}\$")) || newValue.isEmpty()) {
                            priceWithIgv = newValue
                        }
                    },
                    label = { Text("Precio con IGV (S/.)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0.00") }
                )

                // Precio sin IGV (calculado automáticamente)
                OutlinedTextField(
                    value = priceWithoutIgv,
                    onValueChange = { },
                    label = { Text("Precio sin IGV (S/.)") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Tipo de afectación
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAffectationDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tipo de Afectación",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = affectationTypes.find { it.first == selectedAffectationType.toString() }?.second ?: "Gravada",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar tipo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }



                // Resumen de cálculos
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // SUBTOTAL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "S/. ${String.format("%.2f", subtotal)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        // IGV (solo para gravadas)
                        if (selectedAffectationType == 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "IGV (18%):",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "S/. ${String.format("%.2f", igvAmount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else {
                            // Mostrar que no hay IGV para operaciones exoneradas/inafectas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "IGV:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "S/. 0.00",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                        )
                        
                        // TOTAL - FORZAR VISIBILIDAD CON COLOR DIFERENTE
                        Text(
                            text = "TOTAL: S/. ${String.format("%.2f", totalAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red, // Color rojo para que sea visible
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Crear el producto actualizado con los nuevos valores
                    val updatedProduct = product.copy(
                        priceWithIgv3 = totalAmount, // Total calculado
                        priceWithoutIgv3 = subtotal // Subtotal sin descuentos
                    )
                    onSave(updatedProduct)
                },
                enabled = qtyValue > 0 && priceWithIgvValue > 0
            ) {
                Text("Agregar Producto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Diálogo para seleccionar tipo de afectación
    if (showAffectationDialog) {
        AlertDialog(
            onDismissRequest = { showAffectationDialog = false },
            title = { 
                Text(
                    "Seleccionar Tipo de Afectación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    affectationTypes.forEach { (type, name) ->
                        val isSelected = selectedAffectationType == type.toInt()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAffectationType = type.toInt()
                                    showAffectationDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAffectationType = type.toInt()
                                        showAffectationDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = when (type) {
                                            "1" -> "Aplica IGV 18%"
                                            "2" -> "Exonerado de IGV"
                                            "3" -> "Inafecto al IGV"
                                            "4" -> "Operación gratuita"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAffectationDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Cerrar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}

@Composable
fun ProductSearchDialog(
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<IProductOperation>,
    searchState: ProductSearchState,
    onProductSelected: (IProductOperation) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar Producto") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Nombre o código del producto") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Coca Cola, 12345") },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (searchState) {
                    is ProductSearchState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProductSearchState.Success -> {
                        if (searchResults.isEmpty()) {
                            Text(
                                text = "No se encontraron productos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(200.dp)
                            ) {
                                items(searchResults) { product ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clickable { onProductSelected(product) },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = product.name ?: "Sin nombre",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Código: ${product.code}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Stock: ${product.stock} - Precio: S/. ${String.format("%.2f", product.priceWithIgv3)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ProductSearchState.Empty -> {
                        Text(
                            text = "No se encontraron productos para '${searchState.query}'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ProductSearchState.Error -> {
                        Text(
                            text = "Error: ${searchState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is ProductSearchState.Idle -> {
                        Text(
                            text = "Ingrese al menos 3 caracteres para buscar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

// ✅ Función helper para convertir código de documento a nombre legible
private fun getDocumentTypeName(documentType: String?): String {
    return when (documentType) {
        "1" -> "DNI"
        "6" -> "RUC"
        "4" -> "CARNET DE EXTRANJERIA"
        "7" -> "PASAPORTE"
        "A" -> "CED.DIPLOMÁTICA DE IDENTIDAD"
        "B" -> "DOCUMENTO IDENTIDAD PAÍS RESIDENCIA"
        "C" -> "TAX IDENTIFICACIÓN NUMBER - TIN"
        "D" -> "IDENTIFICATION NUMBER - IN"
        "E" -> "TAM - TARJETA ANDINA DE MIGRACIÓN"
        "F" -> "PERMISO TEMPORAL DE PERMANENCIA PTP"
        "-" -> "VARIOS - VENTAS MENORES A S/.700.00"
        else -> "DOC"
    }
} 