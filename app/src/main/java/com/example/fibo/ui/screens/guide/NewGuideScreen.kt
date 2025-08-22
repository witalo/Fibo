package com.example.fibo.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.viewmodels.GuideViewModel
import com.example.fibo.ui.components.LoadingDialog
import com.example.fibo.ui.components.ErrorDialog
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.ITariff
import com.example.fibo.model.IGuideModeType
import com.example.fibo.model.IGuideReasonType
import com.example.fibo.model.ISerialAssigned
import com.example.fibo.ui.components.ClientSearchDialog
import android.util.Log
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.focus.onFocusChanged
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone
import com.example.fibo.model.IRelatedDocument
import com.example.fibo.model.IProductOperation
import com.example.fibo.model.IGeographicLocation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.fibo.model.IPerson
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGuideScreen(
    navController: NavController,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val guideState by viewModel.guideState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Collect individual states for DocumentSection
    val guideModeTypes by remember { viewModel.guideModeTypes }.collectAsState()
    val guideReasonTypes by remember { viewModel.guideReasonTypes }.collectAsState()
    val serialAssigneds by remember { viewModel.serialAssigneds }.collectAsState()
    val isLoadingData by remember { viewModel.isLoadingData }.collectAsState()

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Estados para controlar qué secciones están expandidas
    var expandedSections by remember { 
        mutableStateOf(
            setOf(
                "document", // Expandir por defecto la sección de documento
                "transfer",
                "products"
            )
        )
    }

    // Debug logging
    LaunchedEffect(isLoadingData) {
        Log.d("NewGuideScreen", "isLoadingData changed: $isLoadingData")
    }

    LaunchedEffect(Unit) {
        Log.d("NewGuideScreen", "Initial load")
    }

    LaunchedEffect(error) {
        error?.let {
            showError = true
            errorMessage = it
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            // Navegar a la lista de guías cuando se guarde exitosamente
            navController.navigate("guides") {
                popUpTo("new_guide") { inclusive = true }
            }
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Guía de Remisión") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // Sección de Documento
            AccordionSection(
                title = "Datos del Documento",
                isExpanded = expandedSections.contains("document"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("document")) {
                        expandedSections - "document"
                    } else {
                        expandedSections + "document"
                    }
                }
            ) {
                DocumentSectionContent(
                    documentType = guideState.documentType,
                    guideModeTransfer = guideState.guideModeTransfer,
                    guideReasonTransfer = guideState.guideReasonTransfer,
                    serial = guideState.serial,
                    correlative = guideState.correlative,
                    emitDate = guideState.emitDate,
                    clientId = guideState.clientId,
                    clientName = guideState.clientName,
                    onDocumentTypeChanged = { viewModel.updateField("documentType", it) },
                    onGuideModeChanged = { viewModel.updateField("guideModeTransfer", it) },
                    onGuideReasonChanged = { viewModel.updateField("guideReasonTransfer", it) },
                    onSerialChanged = { viewModel.updateField("serial", it) },
                    onCorrelativeChanged = { viewModel.updateField("correlative", it ?: 0) },
                    onClientSelected = { clientId, clientName ->
                        viewModel.updateField("clientId", clientId)
                        viewModel.updateField("clientName", clientName)
                    },
                    guideModes = guideModeTypes,
                    guideReasons = guideReasonTypes,
                    serials = serialAssigneds,
                    isLoadingData = isLoadingData,
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Datos del Traslado
            AccordionSection(
                title = "Datos del Traslado",
                isExpanded = expandedSections.contains("transfer"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("transfer")) {
                        expandedSections - "transfer"
                    } else {
                        expandedSections + "transfer"
                    }
                }
            ) {
                TransferDataSectionContent(
                    transferData = guideState,
                    onTransferDataChanged = { field, value -> viewModel.updateField(field, value) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Productos
            AccordionSection(
                title = "Productos",
                isExpanded = expandedSections.contains("products"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("products")) {
                        expandedSections - "products"
                    } else {
                        expandedSections + "products"
                    }
                }
            ) {
                ProductsSectionContent(
                    details = guideState.operationDetailSet,
                    onAddProduct = { viewModel.addProduct(it) },
                    onRemoveProduct = { viewModel.removeProduct(it) },
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Documentos Relacionados
            AccordionSection(
                title = "Documentos Relacionados",
                isExpanded = expandedSections.contains("related"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("related")) {
                        expandedSections - "related"
                    } else {
                        expandedSections + "related"
                    }
                }
            ) {
                RelatedDocumentsSectionContent(
                    documents = guideState.relatedDocuments,
                    onAddDocument = { viewModel.addRelatedDocument() },
                    onRemoveDocument = { viewModel.removeRelatedDocument(it) },
                    onUpdateDocument = { index, field, value -> 
                        viewModel.updateRelatedDocument(index, field, value) 
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Transporte
            AccordionSection(
                title = "Datos del Transportista",
                isExpanded = expandedSections.contains("transport"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("transport")) {
                        expandedSections - "transport"
                    } else {
                        expandedSections + "transport"
                    }
                }
            ) {
                TransportSectionContent(
                    transportData = guideState,
                    onTransportDataChanged = { field, value -> viewModel.updateField(field, value) },
                    onAddVehicle = { viewModel.addOtherVehicle() },
                    onRemoveVehicle = { index -> viewModel.removeOtherVehicle(index) },
                    onUpdateVehicle = { index, licensePlate -> viewModel.updateOtherVehicle(index, licensePlate) },
                    viewModel = viewModel
                )
            }

            // Sección de Conductor (solo para transporte privado)
            if (guideState.guideModeTransfer == "02") {
                Spacer(modifier = Modifier.height(8.dp))

                AccordionSection(
                    title = "Datos del Conductor",
                    isExpanded = expandedSections.contains("driver"),
                    onToggle = { 
                        expandedSections = if (expandedSections.contains("driver")) {
                            expandedSections - "driver"
                        } else {
                            expandedSections + "driver"
                        }
                    }
                ) {
                    DriverSectionContent(
                        driverData = guideState,
                        onDriverDataChanged = { field, value -> viewModel.updateField(field, value) },
                        onAddDriver = { viewModel.addOtherDriver() },
                        onRemoveDriver = { index -> viewModel.removeOtherDriver(index) },
                        onUpdateDriver = { index, field, value -> viewModel.updateOtherDriver(index, field, value) },
                        viewModel = viewModel
                    )
                }
            }

            // Sección de Destinatario (solo para guía de transportista)
            if (guideState.documentType == "31") {
                Spacer(modifier = Modifier.height(8.dp))

                AccordionSection(
                    title = "Datos del Destinatario",
                    isExpanded = expandedSections.contains("receiver"),
                    onToggle = { 
                        expandedSections = if (expandedSections.contains("receiver")) {
                            expandedSections - "receiver"
                        } else {
                            expandedSections + "receiver"
                        }
                    }
                ) {
                    ReceiverSectionContent(
                        receiverData = guideState,
                        onReceiverDataChanged = { field, value -> viewModel.updateField(field, value) },
                        viewModel = viewModel
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Direcciones
            AccordionSection(
                title = "Direcciones",
                isExpanded = expandedSections.contains("address"),
                onToggle = { 
                    expandedSections = if (expandedSections.contains("address")) {
                        expandedSections - "address"
                    } else {
                        expandedSections + "address"
                    }
                }
            ) {
                AddressSectionContent(
                    addressData = guideState,
                    onAddressDataChanged = { field, value -> viewModel.updateField(field, value) },
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Guardar
            Button(
                onClick = { viewModel.saveGuide() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar Guía")
            }
        }

        // Diálogo de carga
        if (isSaving) {
            LoadingDialog()
        }

        // Diálogo de error
        if (showError) {
            ErrorDialog(
                message = errorMessage,
                onDismiss = { 
                    showError = false
                    viewModel.clearError()
                }
            )
        }
    }
}

@Composable
private fun AccordionSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(durationMillis = 300)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header del acordeón
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Contenido del acordeón
            if (isExpanded) {
                Divider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    thickness = 1.dp
                )
                
                Box(
                    modifier = Modifier.padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DocumentSectionContent(
    documentType: String,
    guideModeTransfer: String,
    guideReasonTransfer: String,
    serial: String,
    correlative: Int,
    emitDate: String,
    clientId: Int,
    clientName: String,
    onDocumentTypeChanged: (String) -> Unit,
    onGuideModeChanged: (String) -> Unit,
    onGuideReasonChanged: (String) -> Unit,
    onSerialChanged: (String) -> Unit,
    onCorrelativeChanged: (Int) -> Unit,
    onClientSelected: (Int, String) -> Unit,
    guideModes: List<IGuideModeType> = emptyList(),
    guideReasons: List<IGuideReasonType> = emptyList(),
    serials: List<ISerialAssigned> = emptyList(),
    isLoadingData: Boolean = false,
    viewModel: GuideViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tipo de documento
        ExposedDropdownMenuBox(
            label = "Tipo documento",
            value = when(documentType) {
                "09" -> "GUIA DE REMISIÓN REMITENTE ELECTRÓNICA"
                "31" -> "GUÍA DE REMISIÓN TRANSPORTISTA"
                else -> ""
            },
            options = listOf(
                "09" to "GUIA DE REMISIÓN REMITENTE ELECTRÓNICA",
                "31" to "GUÍA DE REMISIÓN TRANSPORTISTA"
            ),
            onOptionSelected = { code, _ -> onDocumentTypeChanged(code) }
        )

        if (documentType == "09") {
            // Tipo de transporte
            if (isLoadingData) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                ExposedDropdownMenuBox(
                    label = "Tipo de transporte",
                    value = guideModes.find { it.code == guideModeTransfer }?.name ?: "",
                    options = guideModes.map { it.code to it.name },
                    onOptionSelected = { code, _ -> onGuideModeChanged(code) }
                )
            }

            // Motivo de traslado
            if (isLoadingData) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                ExposedDropdownMenuBox(
                    label = "Motivo de traslado",
                    value = guideReasons.find { it.code == guideReasonTransfer }?.name ?: "",
                    options = guideReasons.map { it.code to it.name },
                    onOptionSelected = { code, _ -> onGuideReasonChanged(code) }
                )
            }
        }

        // Cliente
        ClientSearchField(
            value = clientName,
            onClientSelected = { clientId, clientName ->
                // Guardar en mayúsculas con maxLength = 200
                val processedName = clientName.uppercase().take(200)
                onClientSelected(clientId, processedName)
            },
            documentType = documentType,
            viewModel = viewModel
        )

        // Serie
        if (isLoadingData) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
            val availableSerials = serials.filter { 
                it.documentType == "A_$documentType" && !it.isGeneratedViaApi 
            }
            
            ExposedDropdownMenuBox(
                label = "Serie",
                value = serial,
                options = availableSerials.map { it.serial to it.serial },
                onOptionSelected = { code, _ -> 
                    // Guardar en mayúsculas con maxLength = 4
                    val processedSerial = code.uppercase().take(4)
                    onSerialChanged(processedSerial)
                },
                enabled = availableSerials.isNotEmpty()
            )

            if (availableSerials.isEmpty()) {
                Text(
                    text = "No hay series asignadas para este tipo de documento",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Fecha de emisión
        OutlinedTextField(
            value = emitDate,
            onValueChange = { },
            label = { Text("Fecha de emisión") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBox(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String, String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onOptionSelected(code, name)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ClientSearchField(
    value: String,
    onClientSelected: (Int, String) -> Unit,
    documentType: String,
    viewModel: GuideViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text("Cliente") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Row {
                    // Mostrar ícono de borrar si hay un cliente seleccionado
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = { onClientSelected(0, "") }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar selección",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Ícono de búsqueda
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar cliente",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (documentType == "31") 
                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else 
                    MaterialTheme.colorScheme.outline
            ),
            supportingText = if (documentType == "31") {
                { Text("No se requiere cliente para guía de transportista") }
            } else null
        )
    }

    if (showDialog) {
        ClientSearchDialog(
            isVisible = true,
            onDismiss = { 
                showDialog = false
                viewModel.clearSearchResults()
            },
            onClientSelected = { client ->
                client.names?.let { 
                    onClientSelected(client.id, it)
                    viewModel.clearSearchResults()
                }
                showDialog = false
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                searchQuery = query
                viewModel.searchClients(query)
            },
            searchResults = searchResults,
            isLoading = isSearching,
            error = searchError // Pasamos el error al diálogo
        )
    }
}

@Composable
private fun ProductsSectionContent(
    details: List<IOperationDetail>,
    onAddProduct: (IOperationDetail) -> Unit,
    onRemoveProduct: (Int) -> Unit,
    viewModel: GuideViewModel
) {
    val products by viewModel.products.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Productos",
                style = MaterialTheme.typography.titleMedium
            )
            
            IconButton(onClick = {
                onAddProduct(
                    IOperationDetail(
                        id = 0,
                        tariff = ITariff(
                            productId = 0,
                            productCode = "",
                            productName = "",
                            unitId = 0,
                            unitName = "",
                            stock = 0.0,
                            priceWithIgv = 0.0,
                            priceWithoutIgv = 0.0,
                            productTariffId = 0,
                            typeAffectationId = 0
                        ),
                        quantity = 0.0,
                        description = "",
                        typeAffectationId = 0,
                        unitValue = 0.0,
                        unitPrice = 0.0,
                        discountPercentage = 0.0,
                        totalDiscount = 0.0,
                        perceptionPercentage = 0.0,
                        totalPerception = 0.0,
                        igvPercentage = 0.0,
                        totalValue = 0.0,
                        totalIgv = 0.0,
                        totalAmount = 0.0,
                        totalToPay = 0.0
                    )
                )
            }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isLoadingProducts) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (details.isEmpty()) {
            Text(
                text = "No hay productos agregados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                details.forEachIndexed { index, detail ->
                    key(detail.hashCode()) {
                        Column {
                            ProductItem(
                                detail = detail,
                                onRemove = { onRemoveProduct(index) },
                                onUpdate = { field, value -> viewModel.updateProduct(index, field, value) },
                                products = products
                            )
                            if (index < details.size - 1) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductItem(
    detail: IOperationDetail,
    onRemove: () -> Unit,
    onUpdate: (String, Any) -> Unit,
    products: List<IProductOperation>
) {
    // Estados locales para mantener el foco
    var productSearchText by remember { mutableStateOf(detail.tariff.productName) }
    var descriptionText by remember { mutableStateOf(detail.description) }
    var quantityText by remember { mutableStateOf(detail.quantity.toString()) }
    var expanded by remember { mutableStateOf(false) }
    
    // Variables para controlar cuándo actualizar el ViewModel
    var shouldUpdateProduct by remember { mutableStateOf(false) }
    var shouldUpdateDescription by remember { mutableStateOf(false) }
    var shouldUpdateQuantity by remember { mutableStateOf(false) }

    // Sincronizar estados locales con el ViewModel solo cuando sea necesario
    LaunchedEffect(detail.tariff.productName) {
        if (detail.tariff.productName != productSearchText && !shouldUpdateProduct) {
            productSearchText = detail.tariff.productName
        }
        shouldUpdateProduct = false
    }
    
    LaunchedEffect(detail.description) {
        if (detail.description != descriptionText && !shouldUpdateDescription) {
            descriptionText = detail.description
        }
        shouldUpdateDescription = false
    }
    
    LaunchedEffect(detail.quantity) {
        if (detail.quantity.toString() != quantityText && !shouldUpdateQuantity) {
            quantityText = detail.quantity.toString()
        }
        shouldUpdateQuantity = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Producto - Servicio
        Column {
            Text(
                text = "Producto - Servicio",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = productSearchText,
                    onValueChange = { newValue ->
                        productSearchText = newValue
                        shouldUpdateProduct = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                if (productSearchText.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.filter { product ->
                            val productString = "${product.code?.let { "$it " } ?: ""}${product.name.trim()} ${product.minimumUnitName}".trim()
                            productString.contains(productSearchText, ignoreCase = true)
                        }.forEach { product ->
                            val productString = "${product.code?.let { "$it " } ?: ""}${product.name.trim()} ${product.minimumUnitName}".trim()
                            DropdownMenuItem(
                                text = { Text(productString) },
                                onClick = {
                                    productSearchText = productString
                                    shouldUpdateProduct = true
                                    onUpdate("productName", productString)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Detalle adicional
        Column {
            Text(
                text = "Detalle adicional",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { newValue ->
                    descriptionText = newValue
                    shouldUpdateDescription = true
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cantidad
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cantidad",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { value ->
                        // Validar el formato del número
                        val parts = value.split(".")
                        if (parts.size <= 2 && // Solo un punto decimal
                            (parts.getOrNull(0)?.length ?: 0) <= 6 && // Máximo 6 dígitos antes del punto
                            (parts.getOrNull(1)?.length ?: 0) <= 4 && // Máximo 4 dígitos después del punto
                            value.matches(Regex("^\\d*\\.?\\d*$")) // Solo números y un punto
                        ) {
                            quantityText = value
                            shouldUpdateQuantity = true
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón eliminar
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.Bottom)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar Producto",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Actualizar el ViewModel con debounce para evitar demasiadas actualizaciones
    LaunchedEffect(productSearchText) {
        if (shouldUpdateProduct) {
            kotlinx.coroutines.delay(1500) // Debounce de 300ms
            if (shouldUpdateProduct) { // Verificar que aún necesitamos actualizar
                onUpdate("productName", productSearchText)
            }
        }
    }
    
    LaunchedEffect(descriptionText) {
        if (shouldUpdateDescription) {
            kotlinx.coroutines.delay(1500) // Debounce de 300ms
            if (shouldUpdateDescription) { // Verificar que aún necesitamos actualizar
                onUpdate("description", descriptionText)
            }
        }
    }
    
    LaunchedEffect(quantityText) {
        if (shouldUpdateQuantity) {
            kotlinx.coroutines.delay(1500) // Debounce de 300ms
            if (shouldUpdateQuantity) { // Verificar que aún necesitamos actualizar
                onUpdate("quantity", quantityText)
            }
        }
    }
}

@Composable
private fun RelatedDocumentsSectionContent(
    documents: List<IRelatedDocument>,
    onAddDocument: () -> Unit,
    onRemoveDocument: (Int) -> Unit,
    onUpdateDocument: (Int, String, Any) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Documentos Relacionados",
                style = MaterialTheme.typography.titleMedium
            )
            
            IconButton(onClick = onAddDocument) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Documento")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (documents.isEmpty()) {
            Text(
                text = "No hay documentos relacionados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                documents.forEachIndexed { index, document ->
                    key(index) {  // Agregamos key para estabilidad
                        RelatedDocumentItem(
                            document = document,
                            onRemove = { onRemoveDocument(index) },
                            onUpdate = { field, value -> onUpdateDocument(index, field, value) }
                        )
                        if (index < documents.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelatedDocumentItem(
    document: IRelatedDocument,
    onRemove: () -> Unit,
    onUpdate: (String, Any) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Tipo de documento
        Column {
            Text(
                text = "Tipo documento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = when(document.documentType) {
                        "01" -> "FACTURA"
                        "03" -> "BOLETA DE VENTA"
                        "07" -> "NOTA DE CRÉDITO ELECTRÓNICA"
                        "09" -> "GUÍA DE REMISIÓN REMITENTE"
                        "31" -> "GUÍA DE REMISIÓN TRANSPORTISTA"
                        else -> ""
                    },
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        "01" to "FACTURA",
                        "03" to "BOLETA DE VENTA",
                        "07" to "NOTA DE CRÉDITO ELECTRÓNICA",
                        "09" to "GUÍA DE REMISIÓN REMITENTE",
                        "31" to "GUÍA DE REMISIÓN TRANSPORTISTA"
                    ).forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onUpdate("documentType", code)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Serie
        Column {
            Text(
                text = "Serie",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = document.serial ?: "",
                onValueChange = { value -> 
                    // Guardar en mayúsculas con maxLength = 4
                    val processedValue = value.uppercase().take(4)
                    onUpdate("serial", processedValue)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Número (Correlativo)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Número",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = (document.correlative ?: 0).toString(),
                    onValueChange = { value ->
                        // Guardar en mayúsculas con maxLength = 8 dígitos
                        val processedValue = value.uppercase().take(8)
                        processedValue.toIntOrNull()?.let { 
                            onUpdate("correlative", it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón eliminar
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.Bottom)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar Documento",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransportSectionContent(
    transportData: GuideViewModel.GuideState,
    onTransportDataChanged: (String, Any) -> Unit,
    onAddVehicle: () -> Unit,
    onRemoveVehicle: (Int) -> Unit,
    onUpdateVehicle: (Int, String) -> Unit,
    viewModel: GuideViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Datos del Transportista",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mostrar según el tipo de transporte
        when (transportData.guideModeTransfer) {
            "01" -> {
                // TRANSPORTE PÚBLICO
                Text(
                    text = "Datos de la Empresa Encargada del Traslado",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tipo de documento del transportista
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when(transportData.transportationCompanyDocumentType) {
                            "6" -> "RUC"
                            else -> "RUC"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de documento del transportista") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("RUC") },
                            onClick = {
                                onTransportDataChanged("transportationCompanyDocumentType", "6")
                                expanded = false
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Número de documento con búsqueda
                DocumentFieldWithSearch(
                    label = "Documento número",
                    value = transportData.transportationCompanyDocumentNumber,
                    onValueChange = { value ->
                        // Guardar en mayúsculas con maxLength = 11
                        val processedValue = value.uppercase().take(11)
                        onTransportDataChanged("transportationCompanyDocumentNumber", processedValue)
                    },
                    onPersonFound = { person ->
                        // Guardar en mayúsculas con maxLength = 200
                        val processedName = (person.names ?: "").uppercase().take(200)
                        onTransportDataChanged("transportationCompanyNames", processedName)
                    },
                    viewModel = viewModel,
                    documentType = "RUC",
                    maxLength = 11
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Denominación del transportista
                OutlinedTextField(
                    value = transportData.transportationCompanyNames,
                    onValueChange = { value ->
                        // Guardar en mayúsculas con maxLength = 200
                        val processedValue = value.uppercase().take(200)
                        onTransportDataChanged("transportationCompanyNames", processedValue)
                    },
                    label = { Text("Transportista denominación") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            "02" -> {
                // TRANSPORTE PRIVADO
                Log.d("NewGuideScreen", "Transport mode 02 - vehicles count: ${transportData.othersVehicles.size}")
                Text(
                    text = "Datos del Vehículo Principal",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Placa del vehículo principal
                OutlinedTextField(
                    value = transportData.mainVehicleLicensePlate,
                    onValueChange = { value ->
                        // Guardar en mayúsculas sin guiones ni espacios, maxLength = 6
                        val processedValue = value.replace("[\\s-]".toRegex(), "").uppercase().take(6)
                        onTransportDataChanged("mainVehicleLicensePlate", processedValue)
                    },
                    label = { Text("Transportista placa número") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Vehículos Secundarios
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Datos de los Vehículos Secundarios (Máximo 2 vehículos)",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (transportData.othersVehicles.size < 2) {
                        Button(
                            onClick = {
                                Log.d("NewGuideScreen", "Add vehicle button clicked")
                                onAddVehicle()
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Agregar Vehículo",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (transportData.othersVehicles.isEmpty()) {
                    Text(
                        text = "No hay vehículos secundarios agregados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    transportData.othersVehicles.forEachIndexed { index, vehicle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = vehicle.licensePlate ?: "",
                                onValueChange = { value ->
                                    // Guardar en mayúsculas sin guiones ni espacios, maxLength = 6
                                    val processedValue = value.replace("[\\s-]".toRegex(), "").uppercase().take(6)
                                    onUpdateVehicle(index, processedValue)
                                },
                                label = { Text("Placa del vehículo ${index + 1}") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { onRemoveVehicle(index) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar Vehículo",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        if (index < transportData.othersVehicles.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverSectionContent(
    driverData: GuideViewModel.GuideState,
    onDriverDataChanged: (String, Any) -> Unit,
    onAddDriver: () -> Unit,
    onRemoveDriver: (Int) -> Unit,
    onUpdateDriver: (Int, String, Any) -> Unit,
    viewModel: GuideViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Datos del Conductor",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Datos del Conductor Principal
        Text(
            text = "Datos del Conductor Principal",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tipo de documento del conductor
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = when(driverData.mainDriverDocumentType) {
                    "1" -> "DNI"
                    "6" -> "RUC"
                    else -> "DNI"
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Tipo de documento") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = false
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }

            ) {
                DropdownMenuItem(
                    text = { Text("DNI") },
                    onClick = {
                        onDriverDataChanged("mainDriverDocumentType", "1")
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("RUC") },
                    onClick = {
                        onDriverDataChanged("mainDriverDocumentType", "6")
                        expanded = false
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Número de documento del conductor con búsqueda
        DocumentFieldWithSearch(
            label = "Documento número",
            value = driverData.mainDriverDocumentNumber,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 8
                val processedValue = value.uppercase().take(8)
                onDriverDataChanged("mainDriverDocumentNumber", processedValue)
            },
            onPersonFound = { person ->
                // Guardar en mayúsculas con maxLength = 200
                val processedName = (person.names ?: "").uppercase().take(200)
                onDriverDataChanged("mainDriverNames", processedName)
                // Guardar en mayúsculas sin guiones ni espacios, maxLength = 9
                val processedLicense = (person.driverLicense ?: "").replace("[\\s-]".toRegex(), "").uppercase().take(9)
                onDriverDataChanged("mainDriverDriverLicense", processedLicense)
            },
            viewModel = viewModel,
            documentType = "DNI",
            maxLength = 8
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Nombres y apellidos del conductor con búsqueda
        DriverNameSearchField(
            label = "Nombres y Apellidos del conductor",
            value = driverData.mainDriverNames,
            onValueChange = { value ->
                onDriverDataChanged("mainDriverNames", value)
            },
            onDriverSelected = { driver ->
                // Autocompletar datos del conductor seleccionado
                onDriverDataChanged("mainDriverNames", driver.names ?: "")
                onDriverDataChanged("mainDriverDocumentNumber", driver.documentNumber ?: "")
                onDriverDataChanged("mainDriverDocumentType", driver.documentType ?: "1")
                // Si tiene licencia, buscar datos completos por documento
                if (driver.documentNumber?.isNotEmpty() == true) {
                    viewModel.searchPersonData(
                        document = driver.documentNumber!!,
                        onSuccess = { person ->
                            onDriverDataChanged("mainDriverDriverLicense", person.driverLicense ?: "")
                        },
                        onError = { /* Ignorar error de licencia */ }
                    )
                }
            },
            viewModel = viewModel,
            documentType = driverData.mainDriverDocumentType
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Licencia de conducir
        OutlinedTextField(
            value = driverData.mainDriverDriverLicense,
            onValueChange = { value ->
                // Guardar en mayúsculas sin guiones ni espacios, maxLength = 9
                val processedValue = value.replace("[\\s-]".toRegex(), "").uppercase().take(9)
                onDriverDataChanged("mainDriverDriverLicense", processedValue)
            },
            label = { Text("Licencia de conducir") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Conductores Secundarios
        Log.d("NewGuideScreen", "Drivers count: ${driverData.othersDrivers.size}")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Datos de los Conductores Secundarios (Máximo 2 conductores)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            
            if (driverData.othersDrivers.size < 2) {
                Button(
                    onClick = {
                        Log.d("NewGuideScreen", "Add driver button clicked")
                        onAddDriver()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Agregar Conductor",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Agregar")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (driverData.othersDrivers.isEmpty()) {
            Text(
                text = "No hay conductores secundarios agregados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            driverData.othersDrivers.forEachIndexed { index, driver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Conductor ${index + 1}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            
                            IconButton(
                                onClick = { onRemoveDriver(index) }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar Conductor",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Tipo de documento
                        var driverExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = driverExpanded,
                            onExpandedChange = { driverExpanded = !driverExpanded }
                        ) {
                            OutlinedTextField(
                                value = when(driver.documentType) {
                                    "1" -> "DNI"
                                    "6" -> "RUC"
                                    else -> "DNI"
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de documento") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = driverExpanded,
                                onDismissRequest = { driverExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("DNI") },
                                    onClick = {
                                        onUpdateDriver(index, "documentType", "1")
                                        driverExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("RUC") },
                                    onClick = {
                                        onUpdateDriver(index, "documentType", "6")
                                        driverExpanded = false
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Número de documento con búsqueda
                        DocumentFieldWithSearch(
                            label = "Documento número",
                            value = driver.documentNumber ?: "",
                            onValueChange = { value ->
                                // Guardar en mayúsculas con maxLength = 8
                                val processedValue = value.uppercase().take(8)
                                onUpdateDriver(index, "documentNumber", processedValue)
                            },
                            onPersonFound = { person ->
                                // Guardar en mayúsculas con maxLength = 200
                                val processedName = (person.names ?: "").uppercase().take(200)
                                onUpdateDriver(index, "names", processedName)
                                // Guardar en mayúsculas sin guiones ni espacios, maxLength = 9
                                val processedLicense = (person.driverLicense ?: "").replace("[\\s-]".toRegex(), "").uppercase().take(9)
                                onUpdateDriver(index, "driverLicense", processedLicense)
                            },
                            viewModel = viewModel,
                            documentType = "DNI",
                            maxLength = 8
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Nombres y apellidos con búsqueda
                        DriverNameSearchField(
                            label = "Nombres y Apellidos del conductor",
                            value = driver.names ?: "",
                            onValueChange = { value ->
                                onUpdateDriver(index, "names", value)
                            },
                            onDriverSelected = { selectedDriver ->
                                // Autocompletar datos del conductor seleccionado
                                onUpdateDriver(index, "names", selectedDriver.names ?: "")
                                onUpdateDriver(index, "documentNumber", selectedDriver.documentNumber ?: "")
                                onUpdateDriver(index, "documentType", selectedDriver.documentType ?: "1")
                                // Si tiene licencia, buscar datos completos por documento
                                if (selectedDriver.documentNumber?.isNotEmpty() == true) {
                                    viewModel.searchPersonData(
                                        document = selectedDriver.documentNumber!!,
                                        onSuccess = { person ->
                                            onUpdateDriver(index, "driverLicense", person.driverLicense ?: "")
                                        },
                                        onError = { /* Ignorar error de licencia */ }
                                    )
                                }
                            },
                            viewModel = viewModel,
                            documentType = driver.documentType ?: "1"
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Licencia de conducir
                        OutlinedTextField(
                            value = driver.driverLicense ?: "",
                            onValueChange = { value ->
                                // Guardar en mayúsculas sin guiones ni espacios, maxLength = 9
                                val processedValue = value.replace("[\\s-]".toRegex(), "").uppercase().take(9)
                                onUpdateDriver(index, "driverLicense", processedValue)
                            },
                            label = { Text("Licencia de conducir") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                if (index < driverData.othersDrivers.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ReceiverSectionContent(
    receiverData: GuideViewModel.GuideState,
    onReceiverDataChanged: (String, Any) -> Unit,
    viewModel: GuideViewModel
) {
    val documentTypes by viewModel.documentTypes.collectAsState()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Datos del Destinatario",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tipo de documento del destinatario
        ExposedDropdownMenuBox(
            label = "Tipo de Documento",
            value = documentTypes.find { it.code == receiverData.receiverDocumentType }?.name ?: "",
            options = documentTypes.map { it.code to it.name },
            onOptionSelected = { code, _ -> onReceiverDataChanged("receiverDocumentType", code) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Número de documento del destinatario con búsqueda
        DocumentFieldWithSearch(
            label = "Número de Documento",
            value = receiverData.receiverDocumentNumber,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 25
                val processedValue = value.uppercase().take(25)
                onReceiverDataChanged("receiverDocumentNumber", processedValue)
            },
            onPersonFound = { person ->
                // Guardar en mayúsculas con maxLength = 200
                val processedName = (person.names ?: "").uppercase().take(200)
                onReceiverDataChanged("receiverNames", processedName)
            },
            viewModel = viewModel,
            documentType = "DNI",
            maxLength = 25
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Nombre del destinatario
        OutlinedTextField(
            value = receiverData.receiverNames,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 200
                val processedValue = value.uppercase().take(200)
                onReceiverDataChanged("receiverNames", processedValue)
            },
            label = { Text("Nombre del Destinatario") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSectionContent(
    addressData: GuideViewModel.GuideState,
    onAddressDataChanged: (String, Any) -> Unit,
    viewModel: GuideViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Direcciones",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dirección de Origen
        Text(
            text = "Punto de Partida",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Autocompletado para origen
        LocationAutocomplete(
            label = "Distrito de origen",
            onLocationSelected = { location ->
                onAddressDataChanged("guideOriginDistrictId", location.districtId)
            },
            onSearch = { query -> viewModel.searchOriginLocation(query) },
            searchResults = viewModel.originSearchResults.collectAsState().value,
            isLoading = viewModel.isSearchingOrigin.collectAsState().value,
            onClearResults = { viewModel.clearOriginSearchResults() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = addressData.guideOriginAddress,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 200
                val processedValue = value.uppercase().take(200)
                onAddressDataChanged("guideOriginAddress", processedValue)
            },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Serie de origen - solo mostrar si guideReasonTransfer == "04"
        if (addressData.guideReasonTransfer == "04") {
            OutlinedTextField(
                value = addressData.guideOriginSerial,
                onValueChange = { value ->
                    // Guardar en mayúsculas con maxLength = 4
                    val processedValue = value.uppercase().take(4)
                    onAddressDataChanged("guideOriginSerial", processedValue)
                },
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Dirección de Destino
        Text(
            text = "Punto de Llegada",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Autocompletado para destino
        LocationAutocomplete(
            label = "Distrito de destino",
            onLocationSelected = { location ->
                onAddressDataChanged("guideArrivalDistrictId", location.districtId)
            },
            onSearch = { query -> viewModel.searchArrivalLocation(query) },
            searchResults = viewModel.arrivalSearchResults.collectAsState().value,
            isLoading = viewModel.isSearchingArrival.collectAsState().value,
            onClearResults = { viewModel.clearArrivalSearchResults() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = addressData.guideArrivalAddress,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 200
                val processedValue = value.uppercase().take(200)
                onAddressDataChanged("guideArrivalAddress", processedValue)
            },
            label = { Text("Dirección") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Serie de destino - solo mostrar si guideReasonTransfer == "04"
        if (addressData.guideReasonTransfer == "04") {
            OutlinedTextField(
                value = addressData.guideArrivalSerial,
                onValueChange = { value ->
                    // Guardar en mayúsculas con maxLength = 4
                    val processedValue = value.uppercase().take(4)
                    onAddressDataChanged("guideArrivalSerial", processedValue)
                },
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Campo de observaciones
        OutlinedTextField(
            value = addressData.observation,
            onValueChange = { value ->
                // Guardar en mayúsculas
                val processedValue = value.uppercase()
                onAddressDataChanged("observation", processedValue)
            },
            label = { Text("Observaciones") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Crear un Calendar en la zona horaria local
                    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    calendar.timeInMillis = millis
                    
                    // Establecer la hora a mediodía para evitar problemas de zona horaria
                    calendar.set(Calendar.HOUR_OF_DAY, 12)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    
                    // Formatear la fecha usando el calendario ajustado
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                        timeZone = calendar.timeZone
                    }.format(calendar.time)
                    
                    onDateSelected(date)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferDataSectionContent(
    transferData: GuideViewModel.GuideState,
    onTransferDataChanged: (String, Any) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Mantener el estado local de la fecha
    var selectedDate by remember { mutableStateOf(transferData.transferDate) }

    // Efecto para actualizar el estado local cuando cambia la fecha en el ViewModel
    LaunchedEffect(transferData.transferDate) {
        selectedDate = transferData.transferDate
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                selectedDate = date
                onTransferDataChanged("transferDate", date)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fecha de inicio de traslado
        Column {
            Text(
                text = "Fecha de inicio de traslado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Seleccionar fecha"
                        )
                    }
                }
            )
        }

        // Peso bruto total
        Column {
            Text(
                text = "Peso bruto total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var textFieldValue by remember { mutableStateOf("") }
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Permitir solo números y un punto decimal
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        textFieldValue = newValue
                        newValue.toDoubleOrNull()?.let {
                            onTransferDataChanged("totalWeight", it)
                        } ?: onTransferDataChanged("totalWeight", 0.0)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && textFieldValue == "0.0") {
                            textFieldValue = ""
                        }
                    },
                placeholder = { Text("0.0") }
            )
        }

        // Unidad de medida de peso
        Column {
            Text(
                text = "Peso - unidad de medida",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = when(transferData.weightMeasurementUnitCode) {
                        "KGM" -> "KGM - KILOGRAMO"
                        "TNE" -> "TNE - TONELADA (TONELADA MÉTRICA)"
                        else -> "KGM - KILOGRAMO"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("KGM - KILOGRAMO") },
                        onClick = {
                            onTransferDataChanged("weightMeasurementUnitCode", "KGM")
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("TNE - TONELADA (TONELADA MÉTRICA)") },
                        onClick = {
                            onTransferDataChanged("weightMeasurementUnitCode", "TNE")
                            expanded = false
                        }
                    )
                }
            }
        }

        // Número de bultos
        Column {
            Text(
                text = "Número de bultos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            var textFieldValue by remember { mutableStateOf("") }
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    // Permitir solo números y un punto decimal
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        textFieldValue = newValue
                        newValue.toDoubleOrNull()?.let {
                            onTransferDataChanged("quantityPackages", it)
                        } ?: onTransferDataChanged("quantityPackages", 0.0)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && textFieldValue == "0.0") {
                            textFieldValue = ""
                        }
                    },
                placeholder = { Text("0.0") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationAutocomplete(
    label: String,
    onLocationSelected: (IGeographicLocation) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<IGeographicLocation>,
    isLoading: Boolean,
    onClearResults: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<IGeographicLocation?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedLocation?.getDisplayText() ?: searchText,
                onValueChange = { newValue ->
                    if (selectedLocation != null) {
                        // Si había una selección, limpiarla al empezar a escribir
                        selectedLocation = null
                        onClearResults()
                    }
                    searchText = newValue
                    onSearch(newValue)
                    expanded = newValue.isNotEmpty()
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        if (selectedLocation != null || searchText.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    selectedLocation = null
                                    searchText = ""
                                    onClearResults()
                                    expanded = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )

            if (searchResults.isNotEmpty() && expanded) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    searchResults.forEach { location ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = location.getDisplayText(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            onClick = {
                                selectedLocation = location
                                searchText = location.getDisplayText()
                                onLocationSelected(location)
                                expanded = false
                                onClearResults()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverNameSearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDriverSelected: (IPerson) -> Unit,
    viewModel: GuideViewModel,
    documentType: String
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val searchResults by viewModel.driverSearchResults.collectAsState()
    val isSearching by viewModel.isSearchingDrivers.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { value ->
                // Guardar en mayúsculas con maxLength = 200
                val processedValue = value.uppercase().take(200)
                onValueChange(processedValue)
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    // Mostrar ícono de borrar si hay un nombre seleccionado
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = { onValueChange("") }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar selección",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    // Ícono de búsqueda
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar conductor",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }

    if (showDialog) {
        DriverSearchDialog(
            isVisible = true,
            onDismiss = { 
                showDialog = false
                viewModel.clearDriverSearchResults()
            },
            onDriverSelected = { driver ->
                onDriverSelected(driver)
                viewModel.clearDriverSearchResults()
                showDialog = false
            },
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                searchQuery = query
                viewModel.searchDriversByName(query, documentType)
            },
            searchResults = searchResults,
            isLoading = isSearching
        )
    }
}

@Composable
private fun DriverSearchDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onDriverSelected: (IPerson) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<IPerson>,
    isLoading: Boolean
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Buscar Conductor") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = { Text("Nombre del conductor") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchResults.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            itemsIndexed(searchResults) { _, driver ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    onClick = { onDriverSelected(driver) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = driver.names ?: "",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${driver.documentType}: ${driver.documentNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else if (searchQuery.length >= 3) {
                        Text(
                            text = "No se encontraron conductores",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
}

@Composable
private fun DocumentFieldWithSearch(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onPersonFound: (IPerson) -> Unit,
    viewModel: GuideViewModel,
    documentType: String = "DNI", // DNI o RUC
    maxLength: Int = 8
) {
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                // Filtrar solo números, convertir a mayúsculas y respetar la longitud máxima
                val filteredValue = newValue.filter { it.isDigit() }.uppercase().take(maxLength)
                onValueChange(filteredValue)
            },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = showError,
            supportingText = if (showError) {
                { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            } else null,
            trailingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        // Solo mostrar botón de búsqueda para DNI y RUC
        if (documentType == "DNI" || documentType == "RUC") {
            Button(
                onClick = {
                    if (value.isNotEmpty()) {
                        val expectedLength = if (documentType == "RUC") 11 else 8
                        if (value.length == expectedLength) {
                            isSearching = true
                            showError = false
                            viewModel.searchPersonData(
                                document = value,
                                onSuccess = { person ->
                                    isSearching = false
                                    onPersonFound(person)
                                },
                                onError = { error ->
                                    isSearching = false
                                    showError = true
                                    errorMessage = error
                                }
                            )
                        } else {
                            showError = true
                            errorMessage = "El ${documentType.lowercase()} debe tener $expectedLength dígitos"
                        }
                    }
                },
                enabled = value.isNotEmpty() && !isSearching,
                modifier = Modifier.height(56.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buscar")
            }
        }
    }
}