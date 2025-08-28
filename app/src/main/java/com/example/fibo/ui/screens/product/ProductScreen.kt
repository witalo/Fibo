package com.example.fibo.ui.screens.product

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IProduct
import com.example.fibo.model.ISubsidiary
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.ui.components.ProductTopBar
import com.example.fibo.utils.ProductSortOrder


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    onLogout: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Cargar productos cuando se abre la pantalla
    LaunchedEffect(Unit) {
        subsidiaryData?.id?.let { subsidiaryId ->
            viewModel.loadProducts(subsidiaryId)
        }
    }

    // Mostrar mensajes de éxito/error
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            // Aquí podrías mostrar un Snackbar
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Aquí podrías mostrar un Snackbar de error
            viewModel.clearError()
        }
    }

    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            // TopBar vacío para que no tape el contenido
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding()
                )
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Contenido principal en Column
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ProductTopBar como parte del contenido principal
                ProductTopBar(
                    title = "Productos",
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onClearSearch = { viewModel.clearSearch() },
                    onSortClick = { showSortDialog = true },
                    onFilterClick = { showFilterDialog = true },
                    onRefreshClick = { subsidiaryData?.id?.let { viewModel.refreshProducts(it) } },
                    onClearFilters = { 
                        viewModel.clearFilters()
                    },
                    isSearching = uiState.isSearching,
                    isRefreshing = uiState.isRefreshing,
                    currentSortOrder = uiState.sortOrder,
                    currentFilters = null to uiState.filterAvailable
                )
                
                // Contenido principal
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error!!,
                            onRetry = { subsidiaryData?.id?.let { viewModel.refreshProducts(it) } }
                        )
                    }
                    uiState.filteredProducts.isEmpty() -> {
                        EmptyProductsState(
                            onRefresh = { subsidiaryData?.id?.let { viewModel.refreshProducts(it) } }
                        )
                    }
                    else -> {
                        ProductList(
                            products = uiState.filteredProducts,
                            navController = navController,
                            onProductClick = { /* Click desactivado para evitar crash */ },
                            onEditClick = { product ->
                                viewModel.selectProduct(product)
                                navController.navigate("editProduct/${product.id}")
                            },
                            onDeleteClick = { /* Eliminación desactivada */ }
                        )
                    }
                }
            }

            // Botón flotante para crear producto
            FloatingActionButton(
                onClick = {  
                    try {
                        navController.navigate("newProduct")
                    } catch (e: Exception) {
                        // Log del error para debugging
                        Log.e("NavigationError", "Error al navegar: ${e.message}", e)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Producto")
            }
        }

        // Diálogo de ordenamiento
        if (showSortDialog) {
            SortDialog(
                currentSortOrder = uiState.sortOrder,
                onSortOrderSelected = { sortOrder ->
                    viewModel.setSortOrder(sortOrder)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }

        // Diálogo de filtros
        if (showFilterDialog) {
            FilterDialog(
                currentAvailable = uiState.filterAvailable,
                onAvailableSelected = { available ->
                    println("DEBUG: Disponibilidad seleccionada en diálogo: $available")
                    viewModel.setFilterAvailable(available)
                },
                onClearFilters = {
                    println("DEBUG: Limpiar filtros desde diálogo")
                    viewModel.clearFilters()
                },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
fun ProductList(
    products: List<IProduct>,
    navController: NavController,
    onProductClick: (IProduct) -> Unit,
    onEditClick: (IProduct) -> Unit,
    onDeleteClick: (IProduct) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp, // Padding superior para separar del ProductTopBar
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                onEditClick = { onEditClick(product) },
                onDeleteClick = { onDeleteClick(product) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: IProduct,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Código: ${product.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.barcode.isNotBlank()) {
                        Text(
                            text = "Barcode: ${product.barcode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Solo botón de editar
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stock total
                Column {
                    Text(
                        text = "Stock Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${product.stockVentas}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Estado
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (product.available) "Disponible" else "No Disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.available)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyProductsState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay productos",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Crea tu primer producto para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actualizar")
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun SortDialog(
    currentSortOrder: ProductSortOrder,
    onSortOrderSelected: (ProductSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordenar por") },
        text = {
            Column {
                ProductSortOrder.values().forEach { sortOrder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderSelected(sortOrder) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOrder == sortOrder,
                            onClick = { onSortOrderSelected(sortOrder) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (sortOrder) {
                                ProductSortOrder.NAME_ASC -> "Nombre (A-Z)"
                                ProductSortOrder.NAME_DESC -> "Nombre (Z-A)"
                                ProductSortOrder.CODE_ASC -> "Código (A-Z)"
                                ProductSortOrder.CODE_DESC -> "Código (Z-A)"
                                ProductSortOrder.STOCK_ASC -> "Stock (Menor a Mayor)"
                                ProductSortOrder.STOCK_DESC -> "Stock (Mayor a Menor)"
                                ProductSortOrder.DATE_CREATED_ASC -> "Fecha (Más Antiguo)"
                                ProductSortOrder.DATE_CREATED_DESC -> "Fecha (Más Reciente)"
                            }
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

@Composable
fun FilterDialog(
    currentAvailable: Boolean?,
    onAvailableSelected: (Boolean?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros") },
        text = {
            Column {
                // Filtro por disponibilidad
                Text(
                    text = "Disponibilidad",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row {
                    FilterChip(
                        selected = currentAvailable == null,
                        onClick = { onAvailableSelected(null) },
                        label = { Text("Todos") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = currentAvailable == true,
                        onClick = { onAvailableSelected(true) },
                        label = { Text("Disponible") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = currentAvailable == false,
                        onClick = { onAvailableSelected(false) },
                        label = { Text("No Disponible") }
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onClearFilters) {
                    Text("Limpiar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    product: IProduct?,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar eliminación") },
        text = {
            Text(
                text = "¿Estás seguro de que quieres eliminar el producto '${product?.name ?: ""}'? Esta acción no se puede deshacer."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Eliminando...")
                } else {
                    Text("Eliminar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}