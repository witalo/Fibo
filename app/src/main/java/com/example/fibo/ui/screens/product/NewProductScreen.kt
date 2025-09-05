package com.example.fibo.ui.screens.product

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IProductTariff
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ITypeAffectation
import com.example.fibo.model.IUnit

// ✅ Función helper simplificada para campos numéricos
@Composable
fun createNumericTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    isPrice: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (isRequired) "$label *" else label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPrice) KeyboardType.Decimal else KeyboardType.Number
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProductScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    productId: Int? = null, // ID del producto si se está editando
    viewModel: NewProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val units by viewModel.units.collectAsState()
    val typeAffectations by viewModel.typeAffectations.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Cargar datos del producto si se está editando
    LaunchedEffect(productId) {
        productId?.let { id ->
            println("🔍 LaunchedEffect: Cargando producto $id")
            viewModel.loadProductForEdit(id)
        }
    }
// ✅ Debug: Mostrar estado actual
    LaunchedEffect(uiState) {
        println("�� Estado actualizado:")
        println("   - Nombre: ${uiState.name}")
        println("   - Tipo: ${uiState.activeType}")
        println("   - Tarifas: ${uiState.productTariffs.size}")
        println("   - Tipo de afectación: ${uiState.typeAffectation?.name}")
    }

    // ✅ Manejar resultado de crear/actualizar producto
    LaunchedEffect(uiState.productResult) {
        uiState.productResult?.let { result ->
            result.onSuccess { message ->
                // ✅ Mostrar mensaje y navegar inmediatamente
                snackbarHostState.showSnackbar(message)

                // ✅ Navegar de vuelta inmediatamente
                navController.popBackStack()

                // ✅ Limpiar el resultado después de navegar
                viewModel.resetProductResult()
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    "Error al ${if (productId != null) "actualizar" else "crear"} producto: ${error.message}"
                )
                viewModel.resetProductResult()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (productId != null) "Editar Producto" else "Nuevo Producto") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ProductBasicInfoSection(
                    uiState = uiState,
                    productTypes = viewModel.getProductTypes(),
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onActiveTypeChanged = { viewModel.onActiveTypeChanged(it) },
                    onCodeChanged = { viewModel.onCodeChanged(it) },
                    onBarcodeChanged = { viewModel.onBarcodeChanged(it) },
                    onObservationChanged = { viewModel.onObservationChanged(it) }
                )
            }

            // Configuración de precios
            item {
                ProductPricingSection(
                    uiState = uiState,
                    units = units,
                    availablePriceTypes = viewModel.getAvailablePriceTypes(),
                    onAddTariff = { priceType, unit -> viewModel.addProductTariff(priceType.id, unit) },
                    onRemoveTariff = { index -> viewModel.removeProductTariff(index) },
                    onTariffChanged = { index, field, value ->
                        when (field) {
                            "priceWithIgv" -> {
                                // ✅ Convertir String a Double antes de enviar
                                val priceWithIgv = if (value is String) {
                                    if (value.isEmpty()) 0.0 else value.toDoubleOrNull() ?: 0.0
                                } else {
                                    value as Double
                                }
                                viewModel.onPriceWithIgvChanged(index, priceWithIgv.toString())
                            }
                            "priceWithoutIgv" -> {
                                // ✅ Convertir String a Double antes de enviar
                                val priceWithoutIgv = if (value is String) {
                                    if (value.isEmpty()) 0.0 else value.toDoubleOrNull() ?: 0.0
                                } else {
                                    value as Double
                                }
                                viewModel.onPriceWithoutIgvChanged(index, priceWithoutIgv.toString())
                            }
                            else -> viewModel.updateProductTariff(index, field, value)
                        }
                    },
                    onTariffUnitChanged = { index, unit -> viewModel.updateTariffUnit(index, unit) }
                )
            }

            // Configuración de stock (opcional)
            item {
                ProductStockSection(
                    uiState = uiState,
                    onStockMinChanged = { viewModel.onStockMinChanged(it) },
                    onStockMaxChanged = { viewModel.onStockMaxChanged(it) }
                )
            }

            // Configuración de afectación
            item {
                ProductAffectationSection(
                    uiState = uiState,
                    typeAffectations = typeAffectations,
                    onTypeAffectationChanged = { viewModel.onTypeAffectationChanged(it) },
                    onSubjectPerceptionChanged = { viewModel.onSubjectPerceptionChanged(it) },
                    onActiveChanged = { viewModel.onActiveChanged(it) },
                    onTypeAffectationExpandedChange = { viewModel.onTypeAffectationExpandedChange(it) }
                )
            }


            // Botones de acción
            item {
                ProductActionButtons(
                    isLoading = uiState.isLoading,
                    isValid = uiState.isFormValid,
                    isEditing = productId != null,
                    onCreateClick = {
                        if (productId != null) {
                            viewModel.updateProduct(productId, subsidiaryData?.id)
                        } else {
                            viewModel.createProduct(subsidiaryData?.id)
                        }
                    },
                    onCancelClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBasicInfoSection(
    uiState: NewProductUiState,
    productTypes: List<ProductType>,
    onNameChanged: (String) -> Unit,
    onActiveTypeChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onObservationChanged: (String) -> Unit
) {
    var isProductTypeExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Información Básica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Nombre del producto (OBLIGATORIO)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChanged,
                label = { Text("Nombre del Producto *") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.nameError.isNotEmpty(),
                singleLine = true
            )

            if (uiState.nameError.isNotEmpty()) {
                Text(
                    text = uiState.nameError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Selector de tipo de producto (OBLIGATORIO)
            ExposedDropdownMenuBox(
                expanded = isProductTypeExpanded,
                onExpandedChange = { isProductTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = productTypes.find { it.id == uiState.activeType }?.name ?: "Seleccionar tipo",
                    onValueChange = { },
                    label = { Text("Tipo de Producto *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductTypeExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = isProductTypeExpanded,
                    onDismissRequest = { isProductTypeExpanded = false }
                ) {
                    productTypes.forEach { productType ->
                        DropdownMenuItem(
                            text = { Text(productType.name) },
                            onClick = {
                                println("🔍 Tipo de producto seleccionado: ${productType.id}")
                                onActiveTypeChanged(productType.id)
                                isProductTypeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.code,
                    onValueChange = onCodeChanged,
                    label = { Text("Código (Opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = onBarcodeChanged,
                    label = { Text("Código de Barras (Opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.observation,
                onValueChange = onObservationChanged,
                label = { Text("Observaciones (Opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPricingSection(
    uiState: NewProductUiState,
    units: List<IUnit>,
    availablePriceTypes: List<PriceType>,
    onAddTariff: (PriceType, IUnit?) -> Unit,
    onRemoveTariff: (Int) -> Unit,
    onTariffChanged: (Int, String, Any) -> Unit,
    onTariffUnitChanged: (Int, IUnit?) -> Unit
) {
    var showAddPriceDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ✅ Header más compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Configuración de Precios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Máximo 4 tipos de precio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ✅ Botón circular corregido y más compacto
                if (availablePriceTypes.isNotEmpty() && uiState.productTariffs.size < 4) {
                    FloatingActionButton(
                        onClick = { showAddPriceDialog = true },
                        modifier = Modifier.size(48.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Agregar Precio",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Estado vacío más compacto
            if (uiState.productTariffs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No hay precios configurados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Agrega el primer precio para comenzar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // ✅ Cambiar LazyColumn por Column normal
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.productTariffs.forEachIndexed { index, tariff ->
                        ProductTariffCard(
                            tariff = tariff,
                            index = index,
                            units = units,
                            onRemove = { onRemoveTariff(index) },
                            onChange = { field, value -> onTariffChanged(index, field, value) },
                            onUnitChanged = { unit -> onTariffUnitChanged(index, unit) },
                            // ✅ Usar las funciones existentes en lugar de viewModel
                            onPriceWithIgvChanged = { priceString ->
                                onTariffChanged(index, "priceWithIgv", priceString)
                            },
                            onPriceWithoutIgvChanged = { priceString ->
                                onTariffChanged(index, "priceWithoutIgv", priceString)
                            },
                            onQuantityMinimumChanged = { quantityString ->
                                onTariffChanged(index, "quantityMinimum", quantityString)
                            }
                        )
                    }
                }
            }
        }
    }

    // ✅ Diálogo más compacto con manejo de errores
    if (showAddPriceDialog) {
        AddPriceDialog(
            availablePriceTypes = availablePriceTypes,
            units = units,
            onAddPrice = { priceType, unit ->
                try {
                    onAddTariff(priceType, unit)
                    showAddPriceDialog = false
                } catch (e: Exception) {
                    // ✅ Mostrar error en snackbar
                    println("Error al agregar precio: ${e.message}")
                    Log.e("NewProductScreen", "Error al agregar precio", e)
                    e.printStackTrace()
                }
            },
            onDismiss = { showAddPriceDialog = false }
        )
    }
}
// ✅ Diálogo más compacto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPriceDialog(
    availablePriceTypes: List<PriceType>,
    units: List<IUnit>,
    onAddPrice: (PriceType, IUnit?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPriceType by remember { mutableStateOf<PriceType?>(null) }
    var selectedUnit by remember { mutableStateOf<IUnit?>(null) }
    var isPriceTypeExpanded by remember { mutableStateOf(false) }
    var isUnitExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Agregar Nuevo Precio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // ✅ Selector de tipo de precio más compacto
                ExposedDropdownMenuBox(
                    expanded = isPriceTypeExpanded,
                    onExpandedChange = { isPriceTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPriceType?.name ?: "Seleccionar tipo de precio",
                        onValueChange = { },
                        label = { Text("Tipo de Precio *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPriceTypeExpanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = isPriceTypeExpanded,
                        onDismissRequest = { isPriceTypeExpanded = false }
                    ) {
                        availablePriceTypes.forEach { priceType ->
                            DropdownMenuItem(
                                text = { Text(priceType.name) },
                                onClick = {
                                    selectedPriceType = priceType
                                    isPriceTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                // ✅ Selector de unidad más compacto
                ExposedDropdownMenuBox(
                    expanded = isUnitExpanded,
                    onExpandedChange = { isUnitExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedUnit?.shortName ?: "Seleccionar unidad",
                        onValueChange = { },
                        label = { Text("Unidad de Medida *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = isUnitExpanded,
                        onDismissRequest = { isUnitExpanded = false }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.shortName} - ${unit.description}") },
                                onClick = {
                                    selectedUnit = unit
                                    isUnitExpanded = false
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
                    if (selectedPriceType != null && selectedUnit != null) {
                        onAddPrice(selectedPriceType!!, selectedUnit!!)
                    }
                },
                enabled = selectedPriceType != null && selectedUnit != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Precio")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("Cancelar")
            }
        }
    )
}
// ✅ Tarjeta de precio más compacta
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTariffCard(
    tariff: IProductTariff,
    index: Int,
    units: List<IUnit>,
    onRemove: () -> Unit,
    onChange: (String, Any) -> Unit,
    onUnitChanged: (IUnit?) -> Unit,
     // ✅ Nuevos parámetros para las funciones del ViewModel
    onPriceWithIgvChanged: (String) -> Unit,
    onPriceWithoutIgvChanged: (String) -> Unit,
    onQuantityMinimumChanged: (String) -> Unit
) {
    var isUnitExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ✅ Header más compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ Badge más compacto
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ✅ Nombre del tipo de precio más compacto
                    val priceTypeName = when (tariff.typePrice) {
                        1 -> "Costo de Compra Unitario"
                        2 -> "Costo de Compra al por Mayor"
                        3 -> "Precio Unitario de Venta"
                        4 -> "Precio al por Mayor de Venta"
                        else -> "Tipo ${tariff.typePrice}"
                    }

                    Text(
                        text = priceTypeName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ✅ Botón eliminar más compacto
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar precio",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Selector de unidad más compacto
            ExposedDropdownMenuBox(
                expanded = isUnitExpanded,
                onExpandedChange = { isUnitExpanded = it }
            ) {
                OutlinedTextField(
                    value = tariff.unit?.shortName ?: "Seleccionar unidad",
                    onValueChange = { },
                    label = { Text("Unidad de Medida *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    isError = tariff.unit == null,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = isUnitExpanded,
                    onDismissRequest = { isUnitExpanded = false }
                ) {
                    units.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text("${unit.shortName} - ${unit.description}") },
                            onClick = {
                                onUnitChanged(unit)
                                isUnitExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Precios más compactos - usar OutlinedTextField normal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = if (tariff.priceWithIgv == 0.0) "" else tariff.priceWithIgv.toString(),
                    onValueChange = { newValue ->
                        // ✅ Validar que sea un número válido o esté vacío
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                            // ✅ Convertir a Double solo si no está vacío
                            val price = if (newValue.isEmpty()) 0.0 else newValue.toDoubleOrNull() ?: 0.0
                            onChange("priceWithIgv", price)
                        }
                    },
                    label = { Text("Precio con IGV *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                OutlinedTextField(
                    value = if (tariff.priceWithoutIgv == 0.0) "" else String.format("%.2f", tariff.priceWithoutIgv),
                    onValueChange = { },
                    label = { Text("Precio sin IGV *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = if (tariff.quantityMinimum == 0.0) "" else tariff.quantityMinimum.toString(),
                onValueChange = { newValue ->
                    // ✅ Validar que sea un número válido o esté vacío
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$")) || newValue.isEmpty()) {
                        // ✅ Convertir a Double solo si no está vacío
                        val quantity = if (newValue.isEmpty()) 0.0 else newValue.toDoubleOrNull() ?: 0.0
                        onChange("quantityMinimum", quantity)
                    }
                },
                label = { Text("Cantidad Mínima *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}

@Composable
fun ProductStockSection(
    uiState: NewProductUiState,
    onStockMinChanged: (String) -> Unit,  // ✅ Cambiar de Int a String
    onStockMaxChanged: (String) -> Unit   // ✅ Cambiar de Int a String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Configuración de Stock (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Los factores de unidad se calcularán automáticamente en el backend",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ✅ Stock - usar OutlinedTextField normal
                OutlinedTextField(
                    value = uiState.stockMin.toString(),
                    onValueChange = { 
                        // ✅ Ahora onStockMinChanged acepta String
                        onStockMinChanged(it)
                    },
                    label = { Text("Stock Mínimo (Opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = uiState.stockMax.toString(),
                    onValueChange = { 
                        // ✅ Ahora onStockMaxChanged acepta String
                        onStockMaxChanged(it)
                    },
                    label = { Text("Stock Máximo (Opcional)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProductUnitsSection(
//    uiState: NewProductUiState,
//    units: List<IUnit>,
//    onMinimumUnitChanged: (IUnit?) -> Unit,
//    onMaximumUnitChanged: (IUnit?) -> Unit,
//    onMinimumUnitExpandedChange: (Boolean) -> Unit,
//    onMaximumUnitExpandedChange: (Boolean) -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Text(
//                text = "Unidades de Medida",
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                // Selector de unidad mínima
//                ExposedDropdownMenuBox(
//                    expanded = uiState.isMinimumUnitExpanded,
//                    onExpandedChange = onMinimumUnitExpandedChange,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    OutlinedTextField(
//                        value = uiState.minimumUnit?.shortName ?: "",
//                        onValueChange = { },
//                        label = { Text("Unidad Mínima") },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .menuAnchor(),
//                        readOnly = true,
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isMinimumUnitExpanded) }
//                    )
//
//                    ExposedDropdownMenu(
//                        expanded = uiState.isMinimumUnitExpanded,
//                        onDismissRequest = { onMinimumUnitExpandedChange(false) }
//                    ) {
//                        units.forEach { unit ->
//                            DropdownMenuItem(
//                                text = { Text(unit.shortName) },
//                                onClick = {
//                                    onMinimumUnitChanged(unit)
//                                    onMinimumUnitExpandedChange(false)
//                                }
//                            )
//                        }
//                    }
//                }
//
//                // Selector de unidad máxima
//                ExposedDropdownMenuBox(
//                    expanded = uiState.isMaximumUnitExpanded,
//                    onExpandedChange = onMaximumUnitExpandedChange,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    OutlinedTextField(
//                        value = uiState.maximumUnit?.shortName ?: "",
//                        onValueChange = { },
//                        label = { Text("Unidad Máxima") },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .menuAnchor(),
//                        readOnly = true,
//                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isMaximumUnitExpanded) }
//                    )
//
//                    ExposedDropdownMenu(
//                        expanded = uiState.isMaximumUnitExpanded,
//                        onDismissRequest = { onMaximumUnitExpandedChange(false) }
//                    ) {
//                        units.forEach { unit ->
//                            DropdownMenuItem(
//                                text = { Text(unit.shortName) },
//                                onClick = {
//                                    onMaximumUnitChanged(unit)
//                                    onMaximumUnitExpandedChange(false)
//                                }
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAffectationSection(
    uiState: NewProductUiState,
    typeAffectations: List<ITypeAffectation>,
    onTypeAffectationChanged: (ITypeAffectation?) -> Unit,
    onSubjectPerceptionChanged: (Boolean) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onTypeAffectationExpandedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Configuración de Afectación",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Selector de tipo de afectación (OBLIGATORIO)
            ExposedDropdownMenuBox(
                expanded = uiState.isTypeAffectationExpanded,
                onExpandedChange = onTypeAffectationExpandedChange
            ) {
                OutlinedTextField(
                    value = uiState.typeAffectation?.name ?: "Seleccionar Tipo",
                    onValueChange = { },
                    label = { Text("Tipo de Afectación *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    isError = uiState.typeAffectation == null,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isTypeAffectationExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = uiState.isTypeAffectationExpanded,
                    onDismissRequest = { onTypeAffectationExpandedChange(false) }
                ) {
                    typeAffectations.forEach { affectation ->
                        DropdownMenuItem(
                            text = { Text(affectation.name) },
                            onClick = {
                                onTypeAffectationChanged(affectation)
                                onTypeAffectationExpandedChange(false)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Checkbox para sujeto a percepción (OPCIONAL)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.subjectPerception,
                    onCheckedChange = onSubjectPerceptionChanged
                )
                Text(
                    text = "Sujeto a Percepción (Opcional)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ✅ Checkbox para activo/inactivo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.active,
                    onCheckedChange = onActiveChanged
                )
                Text(
                    text = "Producto Activo",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


@Composable
fun ProductActionButtons(
    isLoading: Boolean,
    isValid: Boolean,
    isEditing: Boolean = false,
    onCreateClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCreateClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = isValid && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Actualizando..." else "Creando...")
            } else {
                Icon(if (isEditing) Icons.Default.Edit else Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditing) "Actualizar Producto" else "Crear Producto")
            }
        }

        OutlinedButton(
            onClick = onCancelClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Cancelar")
        }
    }
}