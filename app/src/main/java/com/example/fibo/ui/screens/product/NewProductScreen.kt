package com.example.fibo.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IProductTariff
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.ITypeAffectation
import com.example.fibo.model.IUnit

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
            viewModel.loadProductForEdit(id)
        }
    }

    // ✅ Manejar resultado de crear/actualizar producto
    LaunchedEffect(uiState.productResult) {
        uiState.productResult?.let { result ->
            result.onSuccess { message ->
                snackbarHostState.showSnackbar(message)
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "product_updated", true
                )
                navController.popBackStack()
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    "Error al ${if (productId != null) "actualizar" else "crear"} producto: ${error.message}"
                )
            }
            viewModel.resetProductResult()
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
            // Información básica del producto
            item {
                ProductBasicInfoSection(
                    uiState = uiState,
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onCodeChanged = { viewModel.onCodeChanged(it) },
                    onBarcodeChanged = { viewModel.onBarcodeChanged(it) },
                    onObservationChanged = { viewModel.onObservationChanged(it) }
                )
            }

            // Configuración de precios
            item {
                ProductPricingSection(
                    uiState = uiState,
                    onAddTariff = { viewModel.addProductTariff() },
                    onRemoveTariff = { index -> viewModel.removeProductTariff(index) },
                    onTariffChanged = { index, field, value ->
                        when (field) {
                            "priceWithIgv" -> {
                                val priceWithIgv = value as Double
                                viewModel.onPriceWithIgvChanged(index, priceWithIgv)
                            }
                            "priceWithoutIgv" -> {
                                val priceWithoutIgv = value as Double
                                viewModel.onPriceWithoutIgvChanged(index, priceWithoutIgv)
                            }
                            else -> viewModel.updateProductTariff(index, field, value)
                        }
                    }
                )
            }

            // Configuración de stock
            item {
                ProductStockSection(
                    uiState = uiState,
                    onStockMinChanged = { viewModel.onStockMinChanged(it) },
                    onStockMaxChanged = { viewModel.onStockMaxChanged(it) }
                )
            }

            // Configuración de unidades
            item {
                ProductUnitsSection(
                    uiState = uiState,
                    units = units,
                    onMinimumUnitChanged = { viewModel.onMinimumUnitChanged(it) },
                    onMaximumUnitChanged = { viewModel.onMaximumUnitChanged(it) },
                    onMinimumUnitExpandedChange = { viewModel.onMinimumUnitExpandedChange(it) },
                    onMaximumUnitExpandedChange = { viewModel.onMaximumUnitExpandedChange(it) }
                )
            }

            // Configuración de afectación
            item {
                ProductAffectationSection(
                    uiState = uiState,
                    typeAffectations = typeAffectations,
                    onTypeAffectationChanged = { viewModel.onTypeAffectationChanged(it) },
                    onSubjectPerceptionChanged = { viewModel.onSubjectPerceptionChanged(it) },
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

@Composable
fun ProductBasicInfoSection(
    uiState: NewProductUiState,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onObservationChanged: (String) -> Unit
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
                text = "Información Básica",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.code,
                    onValueChange = onCodeChanged,
                    label = { Text("Código") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.barcode,
                    onValueChange = onBarcodeChanged,
                    label = { Text("Código de Barras") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.observation,
                onValueChange = onObservationChanged,
                label = { Text("Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun ProductPricingSection(
    uiState: NewProductUiState,
    onAddTariff: () -> Unit,
    onRemoveTariff: (Int) -> Unit,
    onTariffChanged: (Int, String, Any) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configuración de Precios",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onAddTariff,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Agregar tarifa",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.productTariffs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay tarifas configuradas",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                uiState.productTariffs.forEachIndexed { index, tariff ->
                    ProductTariffCard(
                        tariff = tariff,
                        index = index,
                        onRemove = { onRemoveTariff(index) },
                        onChange = { field, value -> onTariffChanged(index, field, value) }
                    )

                    if (index < uiState.productTariffs.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTariffCard(
    tariff: IProductTariff,
    index: Int,
    onRemove: () -> Unit,
    onChange: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarifa ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar tarifa",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tipo de precio (readonly por ahora)
            OutlinedTextField(
                value = when (tariff.typePrice) {
                    1 -> "Precio de Compra"
                    2 -> "Precio de Venta"
                    3 -> "Precio de Venta"
                    else -> "Precio de Venta"
                },
                onValueChange = { },
                label = { Text("Tipo de Precio") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Precios
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tariff.priceWithIgv.toString(),
                    onValueChange = { onChange("priceWithIgv", it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Precio con IGV") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = tariff.priceWithoutIgv.toString(),
                    onValueChange = { onChange("priceWithoutIgv", it.toDoubleOrNull() ?: 0.0) },
                    label = { Text("Precio sin IGV") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cantidad mínima
            OutlinedTextField(
                value = tariff.quantityMinimum.toString(),
                onValueChange = { onChange("quantityMinimum", it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Cantidad Mínima") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun ProductStockSection(
    uiState: NewProductUiState,
    onStockMinChanged: (Int) -> Unit,
    onStockMaxChanged: (Int) -> Unit
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
                text = "Configuración de Stock",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.stockMin.toString(),
                    onValueChange = { onStockMinChanged(it.toIntOrNull() ?: 0) },
                    label = { Text("Stock Mínimo") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.stockMax.toString(),
                    onValueChange = { onStockMaxChanged(it.toIntOrNull() ?: 0) },
                    label = { Text("Stock Máximo") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductUnitsSection(
    uiState: NewProductUiState,
    units: List<IUnit>,
    onMinimumUnitChanged: (IUnit?) -> Unit,
    onMaximumUnitChanged: (IUnit?) -> Unit,
    onMinimumUnitExpandedChange: (Boolean) -> Unit,
    onMaximumUnitExpandedChange: (Boolean) -> Unit
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
                text = "Unidades de Medida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de unidad mínima
                ExposedDropdownMenuBox(
                    expanded = uiState.isMinimumUnitExpanded,
                    onExpandedChange = onMinimumUnitExpandedChange,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.minimumUnit?.shortName ?: "",
                        onValueChange = { },
                        label = { Text("Unidad Mínima") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isMinimumUnitExpanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = uiState.isMinimumUnitExpanded,
                        onDismissRequest = { onMinimumUnitExpandedChange(false) }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.shortName) },
                                onClick = {
                                    onMinimumUnitChanged(unit)
                                    onMinimumUnitExpandedChange(false)
                                }
                            )
                        }
                    }
                }

                // Selector de unidad máxima
                ExposedDropdownMenuBox(
                    expanded = uiState.isMaximumUnitExpanded,
                    onExpandedChange = onMaximumUnitExpandedChange,
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.maximumUnit?.shortName ?: "",
                        onValueChange = { },
                        label = { Text("Unidad Máxima") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isMaximumUnitExpanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = uiState.isMaximumUnitExpanded,
                        onDismissRequest = { onMaximumUnitExpandedChange(false) }
                    ) {
                        units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.shortName) },
                                onClick = {
                                    onMaximumUnitChanged(unit)
                                    onMaximumUnitExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAffectationSection(
    uiState: NewProductUiState,
    typeAffectations: List<ITypeAffectation>,
    onTypeAffectationChanged: (ITypeAffectation?) -> Unit,
    onSubjectPerceptionChanged: (Boolean) -> Unit,
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

            // Selector de tipo de afectación
            ExposedDropdownMenuBox(
                expanded = uiState.isTypeAffectationExpanded,
                onExpandedChange = onTypeAffectationExpandedChange
            ) {
                OutlinedTextField(
                    value = uiState.typeAffectation?.name ?: "",
                    onValueChange = { },
                    label = { Text("Tipo de Afectación") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.subjectPerception,
                    onCheckedChange = onSubjectPerceptionChanged
                )
                Text(
                    text = "Sujeto a Percepción",
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