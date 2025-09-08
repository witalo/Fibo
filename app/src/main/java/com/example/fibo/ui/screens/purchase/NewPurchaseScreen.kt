package com.example.fibo.ui.screens.purchase

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
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
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.utils.getCurrentFormattedTime
import com.example.fibo.viewmodels.NewPurchaseViewModel
import com.example.fibo.viewmodels.ProductSearchState
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.O)
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
    var dueDate by remember { mutableStateOf(getCurrentFormattedDate()) } // Fecha de vencimiento
    var observation by remember { mutableStateOf("") } // Observación opcional
    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showSupplierDialog by remember { mutableStateOf(false) }
    var showDocumentTypeDialog by remember { mutableStateOf(false) } // ✅ MOVER AQUÍ
    var supplierSearchQuery by remember { mutableStateOf("") } // Para buscar proveedor por RUC/DNI
    var showProductDialog by remember { mutableStateOf(false) } // Para buscar productos
    var productSearchQuery by remember { mutableStateOf("") } // Para buscar productos
    var showProductEditDialog by remember { mutableStateOf(false) } // Para editar productos
    var selectedProductForEdit by remember { mutableStateOf<ITariff?>(null) } // Producto seleccionado para editar

    // ✅ MOVER AQUÍ: Declarar paymentsEnabled y currentPayments al nivel del composable principal
    val paymentsEnabled by viewModel.paymentsEnabled.collectAsState()
    val currentPayments by viewModel.payments.collectAsState()

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        viewModel.loadInitialData(subsidiaryData?.id ?: 0)
    }

    // ✅ Escuchar cuando se obtiene el ITariff y abrir el diálogo de edición
    LaunchedEffect(viewModel.selectedProduct.collectAsState().value) {
        val tariff = viewModel.selectedProduct.value
        if (tariff != null) {
            selectedProductForEdit = tariff
            showProductEditDialog = true
            viewModel.clearProductSelection() // Limpiar para evitar re-apertura
        }
    }

    // ✅ COMENTAR LaunchedEffect porque usamos callbacks directos
    // LaunchedEffect(uiState.purchaseResult) {
    //     println("DEBUG: LaunchedEffect ejecutándose, purchaseResult = ${uiState.purchaseResult}")
    //     uiState.purchaseResult?.let { result ->
    //         println("DEBUG: Procesando resultado de compra: $result")
    //         result.fold(
    //             onSuccess = { message ->
    //                 println("DEBUG: Compra exitosa, navegando a PurchaseScreen")
    //                 Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    //                 // Limpiar el resultado antes de navegar
    //                 viewModel.clearPurchaseResult()
    //                 // ✅ Navegar específicamente a PurchaseScreen en lugar de popBackStack
    //                 navController.navigate(Screen.Purchase.route) {
    //                     popUpTo(Screen.Purchase.route) { inclusive = false }
    //                 }
    //             },
    //             onFailure = { error ->
    //                 println("DEBUG: Error en compra: ${error.message}")
    //                 Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
    //                 // Limpiar el resultado después de mostrar el error
    //                 viewModel.clearPurchaseResult()
    //             }
    //         )
    //     }
    // }



    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            TopAppBar(
                title = { Text("Nueva Compra", style = MaterialTheme.typography.titleSmall) },
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
                    onDateClick = { showDatePicker = true },
                    dueDate = dueDate,
                    onDueDateClick = { showDueDatePicker = true },
                    observation = observation,
                    onObservationChange = { observation = it }
                )
            }

            // Sección de Datos del Proveedor
            item {
                SupplierDataSection(
                    supplier = uiState.supplier,
                    supplierSearchQuery = supplierSearchQuery,
                    onSupplierSearchQueryChange = { newValue ->
                        // ✅ Validar según tipo de documento
                        val isValid = when (selectedDocumentType) {
                            "01" -> { // Factura: Solo RUC (11 dígitos)
                                newValue.matches(Regex("^\\d{0,11}$"))
                            }
                            "03" -> { // Boleta: RUC (11 dígitos) o DNI (8 dígitos)
                                newValue.matches(Regex("^\\d{0,11}$"))
                            }
                            else -> true
                        }
                        
                        if (isValid) {
                            supplierSearchQuery = newValue
                            // Buscar solo si tiene la longitud mínima requerida
                            val minLength = if (selectedDocumentType == "01") 11 else 8
                            if (newValue.length >= minLength) {
                                viewModel.searchSupplierByDocument(newValue)
                            }
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
                            val totalAmount = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 }
                            viewModel.showPaymentDialog(totalAmount)
                        },
                        onRemovePayment = { paymentId -> viewModel.removePayment(paymentId) },
                        totalAmount = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 }, // ✅ Usar productos
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
                        if (paymentsEnabled) {
                            // ✅ Si los pagos están habilitados, mostrar el diálogo de pagos (igual que NoteOfSale)
                            val totalAmount = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 }
                            viewModel.showPaymentDialog(totalAmount)
                        } else {
                            // ✅ Si los pagos están deshabilitados, proceder directamente con la creación
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
                                dueDate = dueDate,
                                emitTime = getCurrentFormattedTime(), // Hora actual
                                userId = 1, // TODO: Obtener del usuario logueado desde PreferencesManager
                                subsidiaryId = subsidiaryData?.id ?: 0, // Sucursal
                                client = null, // No hay cliente en compras
                                supplier = uiState.supplier?.copy( // Solo cambiar esto: cliente por proveedor
                                    id = uiState.supplier!!.id, // ✅ Agregar ID del proveedor
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
                                        description = product.productName ?: "Producto",
                                        tariff = ITariff(
                                            productId = product.productId,
                                            productCode = product.productCode,
                                            productName = product.productName,
                                            unitId = product.unitId,
                                            unitName = product.unitName,
                                            stock = product.stock,
                                            priceWithIgv = product.priceBuyWithIgv ?: 0.0,
                                            priceWithoutIgv = product.priceBuyWithoutIgv ?: 0.0,
                                            productTariffId = product.productTariffId ?: 0, // ✅ Usar el ID real de la tarifa
                                            typeAffectationId = product.typeAffectationId
                                        ),
                                        quantity = 1.0, // TODO: Obtener cantidad del producto
                                        unitValue = product.priceBuyWithoutIgv ?: 0.0,
                                        unitPrice = product.priceBuyWithIgv ?: 0.0,
                                        discountPercentage = 0.0,
                                        totalDiscount = 0.0,
                                        perceptionPercentage = 0.0,
                                        totalPerception = 0.0,
                                        igvPercentage = 18.0,
                                        totalValue = product.priceBuyWithoutIgv ?: 0.0,
                                        totalIgv = (product.priceBuyWithIgv ?: 0.0) - (product.priceBuyWithoutIgv ?: 0.0),
                                        totalAmount = product.priceBuyWithIgv ?: 0.0,
                                        totalToPay = product.priceBuyWithIgv ?: 0.0
                                    )
                                },
                                discountGlobal = 0.0,
                                discountPercentageGlobal = 0.0,
                                discountForItem = 0.0,
                                totalDiscount = 0.0,
                                totalTaxed = uiState.products.sumOf { it.priceBuyWithoutIgv ?: 0.0 },
                                totalUnaffected = 0.0,
                                totalExonerated = 0.0,
                                totalIgv = uiState.products.sumOf { (it.priceBuyWithIgv ?: 0.0) - (it.priceBuyWithoutIgv ?: 0.0) },
                                totalFree = 0.0,
                                totalAmount = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                                totalToPay = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                                totalPayed = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                                observation = observation
                            )

                            // ✅ ENVIAR PAGOS SEGÚN CONFIGURACIÓN (exactamente como en NoteOfSale):
                            viewModel.createPurchase(
                                operation = operation,
                                payments = emptyList(),
                                onSuccess = { operationId, message ->
                                    println("DEBUG: Callback onSuccess ejecutándose - operación sin pagos")
                                    Toast.makeText(
                                        context,
                                        "Compra $message creada",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // ✅ Navegar específicamente a PurchaseScreen
                                    try {
                                        navController.navigate(Screen.Purchase.route) {
                                            popUpTo(0) { inclusive = false }
                                        }
                                        println("DEBUG: Navegación exitosa a PurchaseScreen")
                                    } catch (e: Exception) {
                                        println("DEBUG: Error en navegación: ${e.message}")
                                        navController.popBackStack()
                                    }
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

        // Diálogos de fecha - CON MÁS LOGS
        println("DEBUG: ===== EVALUANDO DIÁLOGOS DE FECHA =====")
        println("DEBUG: showDatePicker = $showDatePicker")
        println("DEBUG: showDueDatePicker = $showDueDatePicker")

        if (showDatePicker) {
            println("DEBUG: ===== MOSTRANDO DatePickerDialog (EMISIÓN) =====")
            DatePickerDialog(
                currentDate = invoiceDate,
                onDateSelected = {
                    println("DEBUG: Fecha emisión seleccionada: $it")
                    invoiceDate = it
                    showDatePicker = false
                },
                onDismiss = { 
                    println("DEBUG: Cerrando DatePickerDialog (emisión)")
                    showDatePicker = false 
                }
            )
        } else {
            println("DEBUG: NO se muestra DatePickerDialog (emisión)")
        }

        if (showDueDatePicker) {
            println("DEBUG: ===== MOSTRANDO DatePickerDialog (VENCIMIENTO) =====")
            DatePickerDialog(
                currentDate = dueDate,
                onDateSelected = {
                    println("DEBUG: Fecha vencimiento seleccionada: $it")
                    dueDate = it
                    showDueDatePicker = false
                },
                onDismiss = { 
                    println("DEBUG: Cerrando DatePickerDialog (vencimiento)")
                    showDueDatePicker = false 
                }
            )
        } else {
            println("DEBUG: NO se muestra DatePickerDialog (vencimiento)")
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
                paymentSummary = viewModel.paymentSummary.collectAsState().value,
                onConfirmPayments = {
                    // ✅ Cuando se confirman los pagos, crear la compra
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
                        dueDate = dueDate, // Fecha de vencimiento
                        emitTime = getCurrentFormattedTime(), // Hora actual
                        userId = 1, // TODO: Obtener del usuario logueado desde PreferencesManager
                        subsidiaryId = subsidiaryData?.id ?: 0, // Sucursal
                        client = null, // No hay cliente en compras
                        supplier = uiState.supplier?.copy( // Solo cambiar esto: cliente por proveedor
                            id = uiState.supplier!!.id, // ✅ Agregar ID del proveedor
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
                            return@PaymentDialog
                        },
                        operationDetailSet = uiState.products.map { product ->
                            IOperationDetail(
                                id = 0,
                                typeAffectationId = 1, // Por defecto gravada
                                description = product.productName ?: "Producto",
                                tariff = ITariff(
                                    productId = product.productId,
                                    productCode = product.productCode,
                                    productName = product.productName,
                                    unitId = product.unitId,
                                    unitName = product.unitName,
                                    stock = product.stock,
                                    priceWithIgv = product.priceBuyWithIgv ?: 0.0,
                                    priceWithoutIgv = product.priceBuyWithoutIgv ?: 0.0,
                                    productTariffId = product.productTariffId ?: 0, // ✅ Usar el ID real de la tarifa
                                    typeAffectationId = product.typeAffectationId
                                ),
                                quantity = 1.0, // TODO: Obtener cantidad del producto
                                unitValue = product.priceBuyWithoutIgv ?: 0.0,
                                unitPrice = product.priceBuyWithIgv ?: 0.0,
                                discountPercentage = 0.0,
                                totalDiscount = 0.0,
                                perceptionPercentage = 0.0,
                                totalPerception = 0.0,
                                igvPercentage = 18.0,
                                totalValue = product.priceBuyWithoutIgv ?: 0.0,
                                totalIgv = (product.priceBuyWithIgv ?: 0.0) - (product.priceBuyWithoutIgv ?: 0.0),
                                totalAmount = product.priceBuyWithIgv ?: 0.0,
                                totalToPay = product.priceBuyWithIgv ?: 0.0
                            )
                        },
                        discountGlobal = 0.0,
                        discountPercentageGlobal = 0.0,
                        discountForItem = 0.0,
                        totalDiscount = 0.0,
                        totalTaxed = uiState.products.sumOf { it.priceBuyWithoutIgv ?: 0.0 },
                        totalUnaffected = 0.0,
                        totalExonerated = 0.0,
                        totalIgv = uiState.products.sumOf { (it.priceBuyWithIgv ?: 0.0) - (it.priceBuyWithoutIgv ?: 0.0) },
                        totalFree = 0.0,
                        totalAmount = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                        totalToPay = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                        totalPayed = uiState.products.sumOf { it.priceBuyWithIgv ?: 0.0 },
                        observation = observation
                    )

                    // ✅ Crear la compra con los pagos registrados
                    viewModel.createPurchase(
                        operation = operation,
                        payments = currentPayments,
                        onSuccess = { operationId, message ->
                            println("DEBUG: Callback onSuccess ejecutándose - operación con pagos")
                            Toast.makeText(
                                context,
                                "Compra $message creada",
                                Toast.LENGTH_SHORT
                            ).show()
                            // ✅ Navegar específicamente a PurchaseScreen
                            try {
                                navController.navigate(Screen.Purchase.route) {
                                    popUpTo(0) { inclusive = false }
                                }
                                println("DEBUG: Navegación exitosa a PurchaseScreen (con pagos)")
                            } catch (e: Exception) {
                                println("DEBUG: Error en navegación: ${e.message}")
                                navController.popBackStack()
                            }
                        }
                    )
                }
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
                    // ✅ Obtener ITariff completo con precios de compra usando getTariffByProductID
                    viewModel.getTariff(product.id ?: 0)
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
    onDateClick: () -> Unit,
    dueDate: String,
    onDueDateClick: () -> Unit,
    observation: String,
    onObservationChange: (String) -> Unit
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
                value = formatDateForDisplay(invoiceDate),
                onValueChange = { },
                label = { Text("Fecha emisión") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        println("DEBUG: ===== CLICK EN FECHA EMISIÓN =====")
                        onDateClick() 
                        println("DEBUG: ===== FIN CLICK FECHA EMISIÓN =====")
                    },
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            println("DEBUG: ===== CLICK EN ICONO FECHA EMISIÓN =====")
                            onDateClick()
                            println("DEBUG: ===== FIN CLICK ICONO FECHA EMISIÓN =====")
                        }
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Seleccionar fecha"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha vencimiento (editable)
            OutlinedTextField(
                value = formatDateForDisplay(dueDate),
                onValueChange = { },
                label = { Text("Fecha vencimiento") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        println("DEBUG: ===== CLICK EN FECHA VENCIMIENTO =====")
                        onDueDateClick() 
                        println("DEBUG: ===== FIN CLICK FECHA VENCIMIENTO =====")
                    },
                readOnly = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            println("DEBUG: ===== CLICK EN ICONO FECHA VENCIMIENTO =====")
                            onDueDateClick()
                            println("DEBUG: ===== FIN CLICK ICONO FECHA VENCIMIENTO =====")
                        }
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Seleccionar fecha de vencimiento"
                        )
                    }
                },
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
                // Serie - INGRESO MANUAL (máximo 4 caracteres)
                OutlinedTextField(
                    value = manualSerial,
                    onValueChange = { newValue ->
                        if (newValue.length <= 4) { // ✅ Máximo 4 caracteres
                            onSerialChange(newValue)
                        }
                    },
                    label = { Text("Serie") },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ej: F001") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${manualSerial.length}/4 caracteres",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (manualSerial.length > 4) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    isError = manualSerial.length > 4
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

            Spacer(modifier = Modifier.height(12.dp))

            // Observación (opcional)
            OutlinedTextField(
                value = observation,
                onValueChange = onObservationChange,
                label = { Text("Observación (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej: Compra urgente, entrega especial, etc.") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
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
                                "01" -> "Ingrese RUC del proveedor (11 dígitos)"
                                "03" -> "Ingrese RUC (11) o DNI (8) del proveedor"
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
                    singleLine = true,
                    supportingText = {
                        when (documentType) {
                            "01" -> {
                                val isValid = supplierSearchQuery.matches(Regex("^\\d{11}$"))
                                Text(
                                    text = if (supplierSearchQuery.isBlank()) {
                                        "RUC: 11 dígitos requeridos"
                                    } else if (isValid) {
                                        "RUC válido ✓"
                                    } else {
                                        "RUC: ${supplierSearchQuery.length}/11 dígitos"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (supplierSearchQuery.isNotBlank() && !isValid) {
                                        MaterialTheme.colorScheme.error
                                    } else if (isValid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            "03" -> {
                                val isRuc = supplierSearchQuery.matches(Regex("^\\d{11}$"))
                                val isDni = supplierSearchQuery.matches(Regex("^\\d{8}$"))
                                val isValid = isRuc || isDni
                                Text(
                                    text = if (supplierSearchQuery.isBlank()) {
                                        "RUC: 11 dígitos o DNI: 8 dígitos"
                                    } else if (isValid) {
                                        if (isRuc) "RUC válido ✓" else "DNI válido ✓"
                                    } else {
                                        "RUC: ${supplierSearchQuery.length}/11 o DNI: ${supplierSearchQuery.length}/8"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (supplierSearchQuery.isNotBlank() && !isValid) {
                                        MaterialTheme.colorScheme.error
                                    } else if (isValid) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            else -> null
                        }
                    },
                    isError = when (documentType) {
                        "01" -> supplierSearchQuery.isNotBlank() && !supplierSearchQuery.matches(Regex("^\\d{11}$"))
                        "03" -> supplierSearchQuery.isNotBlank() && !supplierSearchQuery.matches(Regex("^\\d{8}$|^\\d{11}$"))
                        else -> false
                    }
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (documentType) {
                            "01" -> MaterialTheme.colorScheme.primaryContainer
                            "03" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = when (documentType) {
                                "01" -> "📋 FACTURA ELECTRÓNICA"
                                "03" -> "📄 BOLETA ELECTRÓNICA"
                                else -> "📝 DOCUMENTO"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (documentType) {
                                "01" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "03" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (documentType) {
                                "01" -> "• Solo RUC de 11 dígitos\n• Para empresas y proveedores formales"
                                "03" -> "• RUC de 11 dígitos (empresas)\n• DNI de 8 dígitos (personas naturales)"
                                else -> "• Documento de identificación válido"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (documentType) {
                                "01" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "03" -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
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
    var day by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }

    // Parsear la fecha actual
    LaunchedEffect(currentDate) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(currentDate)
            if (date != null) {
                val calendar = Calendar.getInstance()
                calendar.time = date
                day = calendar.get(Calendar.DAY_OF_MONTH).toString()
                month = (calendar.get(Calendar.MONTH) + 1).toString()
                year = calendar.get(Calendar.YEAR).toString()
            }
        } catch (e: Exception) {
            val today = Calendar.getInstance()
            day = today.get(Calendar.DAY_OF_MONTH).toString()
            month = (today.get(Calendar.MONTH) + 1).toString()
            year = today.get(Calendar.YEAR).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Seleccionar Fecha",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información de la fecha actual
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Fecha actual:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatDateForDisplay(currentDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Campos de entrada para día, mes y año
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Día
                    OutlinedTextField(
                        value = day,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d{0,2}$"))) {
                                day = newValue
                            }
                        },
                        label = { Text("Día") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("DD") },
                        supportingText = { Text("1-31") }
                    )

                    // Mes
                    OutlinedTextField(
                        value = month,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d{0,2}$"))) {
                                month = newValue
                            }
                        },
                        label = { Text("Mes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("MM") },
                        supportingText = { Text("1-12") }
                    )

                    // Año
                    OutlinedTextField(
                        value = year,
                        onValueChange = { newValue ->
                            if (newValue.matches(Regex("^\\d{0,4}$"))) {
                                year = newValue
                            }
                        },
                        label = { Text("Año") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("YYYY") },
                        supportingText = { Text("2024-2026") }
                    )
                }

                // Botones de fecha rápida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val today = Calendar.getInstance()
                            day = today.get(Calendar.DAY_OF_MONTH).toString()
                            month = (today.get(Calendar.MONTH) + 1).toString()
                            year = today.get(Calendar.YEAR).toString()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Hoy", style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            val tomorrow = Calendar.getInstance()
                            tomorrow.add(Calendar.DAY_OF_MONTH, 1)
                            day = tomorrow.get(Calendar.DAY_OF_MONTH).toString()
                            month = (tomorrow.get(Calendar.MONTH) + 1).toString()
                            year = tomorrow.get(Calendar.YEAR).toString()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Text("Mañana", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Vista previa de la fecha seleccionada
                if (day.isNotBlank() && month.isNotBlank() && year.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Vista previa:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = try {
                                    val dayInt = day.toInt()
                                    val monthInt = month.toInt()
                                    val yearInt = year.toInt()
                                    val calendar = Calendar.getInstance()
                                    calendar.set(yearInt, monthInt - 1, dayInt)
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    dateFormat.format(calendar.time)
                                } catch (e: Exception) {
                                    "Fecha inválida"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isValidDate(day, month, year)) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidDate(day, month, year)) {
                        try {
                            val dayInt = day.toInt()
                            val monthInt = month.toInt()
                            val yearInt = year.toInt()
                            val calendar = Calendar.getInstance()
                            calendar.set(yearInt, monthInt - 1, dayInt)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val formattedDate = dateFormat.format(calendar.time)
                            onDateSelected(formattedDate)
                        } catch (e: Exception) {
                            // Error al formatear fecha
                        }
                    }
                },
                enabled = isValidDate(day, month, year)
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

// Función para validar si la fecha es válida
private fun isValidDate(day: String, month: String, year: String): Boolean {
    return try {
        val dayInt = day.toInt()
        val monthInt = month.toInt()
        val yearInt = year.toInt()
        
        if (dayInt < 1 || dayInt > 31) return false
        if (monthInt < 1 || monthInt > 12) return false
        if (yearInt < 2020 || yearInt > 2030) return false
        
        val calendar = Calendar.getInstance()
        calendar.set(yearInt, monthInt - 1, dayInt)
        calendar.get(Calendar.DAY_OF_MONTH) == dayInt &&
        calendar.get(Calendar.MONTH) == monthInt - 1 &&
        calendar.get(Calendar.YEAR) == yearInt
    } catch (e: Exception) {
        false
    }
}

@Composable
fun ProductsSection(
    products: List<ITariff>, // ✅ Ya está correcto
    onAddProduct: () -> Unit,
    viewModel: NewPurchaseViewModel,
    onEditProduct: (ITariff) -> Unit // ✅ Ya está correcto
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
                            onRemove = { viewModel.removeProduct(product.productId) },
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
    product: ITariff, // ✅ Ya está correcto
    onRemove: () -> Unit,
    onEdit: (ITariff) -> Unit // ✅ Ya está correcto
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
                    text = product.productName.ifBlank { "Producto sin nombre" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Código: ${product.productCode} - Stock: ${product.stock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Precio sin IGV: S/. ${String.format("%.2f", product.priceBuyWithoutIgv)}", // ✅ Ya está correcto
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Total: S/. ${String.format("%.2f", product.priceBuyWithIgv ?: 0.0)}", // ✅ Ya está correcto
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

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PaymentDialog(
    onDismiss: () -> Unit,
    onPaymentAdded: (IPayment) -> Unit,
    paymentSummary: PaymentSummary,
    onConfirmPayments: () -> Unit = {} // ✅ Agregar parámetro opcional
) {
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showPaymentMethodDialog by remember { mutableStateOf(false) }
    var paymentDate by remember { mutableStateOf(getCurrentFormattedDate()) } // ✅ Fecha de pago para crédito
    var showDatePicker by remember { mutableStateOf(false) } // ✅ Para mostrar selector de fecha

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Agregar Pago",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen de pagos actual
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Resumen de Pagos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "S/. ${String.format("%.2f", paymentSummary.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pagado:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "S/. ${String.format("%.2f", paymentSummary.totalPaid)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pendiente:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "S/. ${String.format("%.2f", paymentSummary.remaining)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentSummary.remaining > 0.01) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Método de pago - MEJORADO
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentMethodDialog = true },
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
                                text = "Método de Pago",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedPaymentMethod?.name ?: "Seleccionar método",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Seleccionar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botones de monto rápido con iconos (más compactos)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón "Pago Completo" con icono
                    IconButton(
                        onClick = { 
                            // ✅ Usar el monto exacto restante, redondeado a 2 decimales
                            amount = String.format("%.2f", paymentSummary.remaining)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Pago completo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Botón "Mitad" con icono
                    IconButton(
                        onClick = { 
                            amount = String.format("%.2f", paymentSummary.remaining / 2)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.secondary,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.BrightnessMedium,
                            contentDescription = "Mitad del pago",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Botón "Limpiar" con icono
                    IconButton(
                        onClick = { amount = "" },
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Limpiar monto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Monto con mejor control y precisión decimal
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Validar que sea un número válido y no exceda el monto restante
                        if (newValue.matches(Regex("^\\d*\\.?\\d{0,2}\$")) || newValue.isEmpty()) {
                            val amountValue = newValue.toDoubleOrNull() ?: 0.0
                            // ✅ Usar tolerancia de 0.01 para manejar errores de precisión decimal
                            if (amountValue <= (paymentSummary.remaining + 0.01) || newValue.isEmpty()) {
                                amount = newValue
                            }
                        }
                    },
                    label = { Text("Monto (S/.)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Máximo: ${String.format("%.2f", paymentSummary.remaining)}") },
                    isError = amount.toDoubleOrNull()?.let { it > (paymentSummary.remaining + 0.01) } ?: false,
                    supportingText = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        val remainingAfter = paymentSummary.remaining - amountValue
                        when {
                            amount.isEmpty() -> Text("Ingrese el monto a pagar")
                            amountValue > (paymentSummary.remaining + 0.01) -> Text(
                                "Monto excede lo pendiente",
                                color = MaterialTheme.colorScheme.error
                            )
                            remainingAfter > 0.01 -> Text(
                                "Restante después: S/. ${String.format("%.2f", remainingAfter)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> Text(
                                "Pago completo ✓",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = {
                        if (amount.isNotBlank()) {
                            IconButton(onClick = { amount = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    }
                )

                // Nota (opcional)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej: Transferencia, Cheque N°") }
                )

                // ✅ Fecha de pago (automática para contado, selector para crédito) - MEJORADO
                if (selectedPaymentMethod != null) {
                    OutlinedTextField(
                        value = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                            paymentDate // Fecha seleccionada para crédito
                        } else {
                            getCurrentFormattedDate() // Fecha actual para contado
                        },
                        onValueChange = { },
                        label = { Text("Fecha de Pago") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                // Solo permitir editar fecha si es crédito
                                if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                    showDatePicker = true
                                }
                            },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Fecha de pago",
                                tint = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                    MaterialTheme.colorScheme.primary // Clickable para crédito
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // No clickable para contado
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                text = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                    "Click para seleccionar fecha futura"
                                } else {
                                    "Fecha actual (automática)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón para agregar pago individual
                Button(
                    onClick = {
                        val amountValue = amount.toDoubleOrNull() ?: 0.0
                        // ✅ Usar tolerancia de 0.01 para manejar errores de precisión decimal
                        if (selectedPaymentMethod != null && amountValue > 0 && amountValue <= (paymentSummary.remaining + 0.01)) {
                            // ✅ Usar fecha correcta según el tipo de pago
                            val finalPaymentDate = if (selectedPaymentMethod?.name?.contains("Crédito", ignoreCase = true) == true) {
                                paymentDate // Fecha seleccionada para crédito
                            } else {
                                getCurrentFormattedDate() // Fecha actual para contado
                            }
                            
                            val payment = IPayment(
                                id = 0,
                                wayPay = selectedPaymentMethod!!.id,
                                amount = amountValue,
                                note = note.trim(),
                                paymentDate = finalPaymentDate, // ✅ Fecha correcta según tipo
                                operationId = 0
                            )
                            onPaymentAdded(payment)
                            
                            // Limpiar campos después de agregar
                            selectedPaymentMethod = null
                            amount = ""
                            note = ""
                            paymentDate = getCurrentFormattedDate() // Resetear fecha para próximo pago
                        }
                    },
                    enabled = selectedPaymentMethod != null &&
                            amount.isNotBlank() &&
                            (amount.toDoubleOrNull() ?: 0.0) > 0 &&
                            (amount.toDoubleOrNull() ?: 0.0) <= (paymentSummary.remaining + 0.01)
                ) {
                    Text("Agregar Pago")
                }
                
                // Botón para confirmar todos los pagos (solo si el pago está completo)
                if (paymentSummary.remaining <= 0.01) {
                    Button(
                        onClick = {
                            onConfirmPayments()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Confirmar Pagos")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    // Diálogo para seleccionar método de pago - MEJORADO
    if (showPaymentMethodDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentMethodDialog = false },
            title = { 
                Text(
                    "Seleccionar Método de Pago",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(PaymentMethods.AVAILABLE_METHODS) { method ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPaymentMethod = method
                                    showPaymentMethodDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod?.id == method.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (selectedPaymentMethod?.id == method.id) 4.dp else 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod?.id == method.id,
                                    onClick = {
                                        selectedPaymentMethod = method
                                        showPaymentMethodDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = method.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedPaymentMethod?.id == method.id) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedPaymentMethod?.id == method.id) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
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

    // ✅ Diálogo para seleccionar fecha de pago (solo para crédito) - USANDO DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // Mañana
            yearRange = IntRange(
                java.time.LocalDate.now().year,
                java.time.LocalDate.now().year + 2
            )
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(dateMillis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            
                            // Validar que la fecha sea futura
                            if (selectedDate.isAfter(java.time.LocalDate.now())) {
                                paymentDate = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                showDatePicker = false
                            }
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                    subheadContentColor = MaterialTheme.colorScheme.onSurface,
                    yearContentColor = MaterialTheme.colorScheme.onSurface,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    disabledSelectedDayContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// Funciones de utilidad
fun getCurrentFormattedDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // ✅ Formato ISO para Django
    return dateFormat.format(Date())
}

fun getCurrentFormattedTime(): String {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return timeFormat.format(Date())
}

// ✅ Función para formatear fecha ISO a formato legible para el usuario
fun formatDateForDisplay(isoDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(isoDate)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        isoDate // Si hay error, devolver la fecha original
    }
}
@Composable
fun ProductEditDialog(
    product: ITariff,
    onDismiss: () -> Unit,
    onSave: (ITariff) -> Unit
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
        val productPrice = product.priceBuyWithIgv ?: 0.0 // ✅ ITariff usa 'priceBuyWithIgv' para compras
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
                            text = product.productName ?: "Sin nombre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Código: ${product.productCode}",
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
                        priceBuyWithIgv = totalAmount, // Total calculado (precio de compra con IGV)
                        priceBuyWithoutIgv = subtotal // Subtotal sin descuentos (precio de compra sin IGV)
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
    onProductSelected: (IProductOperation) -> Unit // ✅ Cambiar a IProductOperation
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
                                                text = product.name ?: "Sin nombre", // ✅ IProductOperation usa 'name'
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Código: ${product.code}", // ✅ IProductOperation usa 'code'
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Stock: ${product.stock} - Precio: S/. ${String.format("%.2f", product.priceWithIgv3)}", // ✅ IProductOperation usa 'priceWithIgv3'
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