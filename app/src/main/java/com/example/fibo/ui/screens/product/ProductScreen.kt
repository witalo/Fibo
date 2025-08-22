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
            ProductTopBar(
                title = "Productos",
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onClearSearch = { viewModel.clearSearch() },
                onSortClick = { showSortDialog = true },
                onFilterClick = { showFilterDialog = true },
                onRefreshClick = { subsidiaryData?.id?.let { viewModel.refreshProducts(it) } },
                onClearFilters = { // AGREGAR ESTA FUNCIÓN
                    viewModel.setFilterCategory(null)
                    viewModel.setFilterAvailable(null)
                },
                isSearching = uiState.isSearching,
                isRefreshing = uiState.isRefreshing,
                currentSortOrder = uiState.sortOrder,
                currentFilters = uiState.filterCategory to uiState.filterAvailable
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Barra de herramientas superior
                ToolbarSection(
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onClearSearch = { viewModel.clearSearch() },
                    onSortClick = { showSortDialog = true },
                    onFilterClick = { showFilterDialog = true },
                    onRefreshClick = { subsidiaryData?.id?.let { viewModel.refreshProducts(it) } },
                    searchQuery = uiState.searchQuery,
                    isSearching = uiState.isSearching,
                    isRefreshing = uiState.isRefreshing
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            navController = navController, // Pasar navController aquí
                            onProductClick = { product ->
                                viewModel.selectProduct(product)
                                navController.navigate("productDetail/${product.id}")
                            },
                            onEditClick = { product ->
                                viewModel.selectProduct(product)
                                navController.navigate("editProduct/${product.id}")
                            },
                            onDeleteClick = { product ->
                                viewModel.showDeleteDialog(product)
                            }
                        )
                    }
                }
            }

            // Botón flotante para crear producto
            FloatingActionButton(
                onClick = {  try {
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
                currentCategory = uiState.filterCategory,
                currentAvailable = uiState.filterAvailable,
                onCategorySelected = { category ->
                    viewModel.setFilterCategory(category)
                },
                onAvailableSelected = { available ->
                    viewModel.setFilterAvailable(available)
                },
                onClearFilters = {
                    viewModel.clearFilters()
                },
                onDismiss = { showFilterDialog = false }
            )
        }

        // Diálogo de confirmación de eliminación
        if (uiState.showDeleteDialog) {
            DeleteConfirmationDialog(
                product = uiState.productToDelete,
                isDeleting = uiState.isDeleting,
                onConfirm = {
                    viewModel.deleteProduct(
                        onSuccess = {
                            // Producto eliminado exitosamente
                        },
                        onError = { error ->
                            // Error al eliminar
                        }
                    )
                },
                onDismiss = { viewModel.hideDeleteDialog() }
            )
        }
    }
}

@Composable
fun ToolbarSection(
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    searchQuery: String,
    isSearching: Boolean,
    isRefreshing: Boolean
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Buscar productos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSortClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Sort, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ordenar")
            }

            OutlinedButton(
                onClick = onFilterClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filtrar")
            }

            IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                }
            }
        }
    }
}

@Composable
fun ProductList(
    products: List<IProduct>,
    navController: NavController, // Agregar este parámetro
    onProductClick: (IProduct) -> Unit,
    onEditClick: (IProduct) -> Unit,
    onDeleteClick: (IProduct) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = { onProductClick(product) },
                onEditClick = { onEditClick(product) }, // Usar el callback en lugar de navegar directamente
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
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
                        text = "${product.totalStock}",
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
    currentCategory: String?,
    currentAvailable: Boolean?,
    onCategorySelected: (String?) -> Unit,
    onAvailableSelected: (Boolean?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtros") },
        text = {
            Column {
                // Filtro por categoría
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Row {
                    FilterChip(
                        selected = currentCategory == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("Todas") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = currentCategory == "01",
                        onClick = { onCategorySelected("01") },
                        label = { Text("Venta") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = currentCategory == "02",
                        onClick = { onCategorySelected("02") },
                        label = { Text("Vehículo") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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