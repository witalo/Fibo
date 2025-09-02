package com.example.fibo.ui.screens.purchase

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
    // ✅ Agregar operationDetails como en NoteOfSale
    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }
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
                    onAddProduct = { /* TODO: Implementar selección de productos */ }
                )
            }

            // Sección de Pagos (si están habilitados)
            item {
                if (paymentsEnabled) {
                    PaymentsSection(
                        payments = uiState.payments,
                        onAddPayment = {
                            // ✅ Usar operationDetails en lugar de uiState.products
                            val totalAmount = operationDetails.sumOf { it.totalToPay }
                            viewModel.showPaymentDialog(totalAmount)
                        },
                        onRemovePayment = { paymentId -> viewModel.removePayment(paymentId) },
                        totalAmount = operationDetails.sumOf { it.totalToPay }, // ✅ Usar operationDetails
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
                            operationDetailSet = operationDetails.map { detail ->
                                detail.copy(
                                    // Asegurar valores positivos
                                    id = 0,
                                    typeAffectationId = max(1, detail.typeAffectationId),
                                    description = detail.description.trim().uppercase(),
                                    tariff = detail.tariff,
                                    quantity = max(0.0, detail.quantity),
                                    unitValue = max(0.0, detail.unitValue),
                                    unitPrice = max(0.0, detail.unitPrice),
                                    discountPercentage = max(0.0, detail.discountPercentage),
                                    totalDiscount = max(0.0, detail.totalDiscount),
                                    perceptionPercentage = max(0.0, detail.perceptionPercentage),
                                    totalPerception = max(0.0, detail.totalPerception),
                                    igvPercentage = max(0.0, detail.igvPercentage),
                                    totalValue = max(0.0, detail.totalValue),
                                    totalIgv = max(0.0, detail.totalIgv),
                                    totalAmount = max(0.0, detail.totalAmount),
                                    totalToPay = max(0.0, detail.totalToPay)
                                )
                            },
                            discountGlobal = max(0.0, 0.0), // TODO: Implementar descuento global
                            discountPercentageGlobal = max(0.0, min(0.0, 100.0)), // TODO: Implementar
                            discountForItem = max(0.0, 0.0), // TODO: Implementar
                            totalDiscount = max(0.0, operationDetails.sumOf { it.totalDiscount }),
                            totalTaxed = max(0.0, operationDetails.sumOf { it.totalValue }), // ✅ Usar operationDetails
                            totalUnaffected = max(0.0, 0.0), // TODO: Implementar
                            totalExonerated = max(0.0, 0.0), // TODO: Implementar
                            totalIgv = max(0.0, operationDetails.sumOf { it.totalIgv }), // ✅ Usar operationDetails
                            totalFree = max(0.0, 0.0), // TODO: Implementar
                            totalAmount = max(0.0, operationDetails.sumOf { it.totalAmount }), // ✅ Usar operationDetails
                            totalToPay = max(0.0, operationDetails.sumOf { it.totalToPay }), // ✅ Usar operationDetails
                            totalPayed = max(0.0, operationDetails.sumOf { it.totalToPay }) // ✅ Usar operationDetails
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
                            IconButton(onClick = onSupplierClick) {
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
    onAddProduct: () -> Unit
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
                            onRemove = { /* TODO: Implementar eliminación */ }
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
                    text = product.name.ifBlank { "Producto sin nombre" }, // ✅ Usar name que sí existe
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Código: ${product.code} - Stock: ${product.stock}", // ✅ Usar propiedades que existen
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Precio: S/. ${String.format("%.2f", product.priceWithIgv3)}", // ✅ Usar priceWithIgv3 que existe
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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