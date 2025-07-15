package com.example.fibo.ui.screens.product
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.IProduct
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.applyTextGradient
import com.example.fibo.viewmodels.ProductUiState
import com.example.fibo.viewmodels.ProductViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    viewModel: ProductViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onAddProduct: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // Debug: Log del estado UI en ProductScreen
    Log.d("ProductScreen", "UI State - isLoading: ${uiState.isLoading}, products.size: ${uiState.products.size}, error: ${uiState.error}")
    Log.d("ProductScreen", "Estado hash: ${uiState.hashCode()}")

    Scaffold(
        topBar = {
            // TopBar Negro con texto blanco
            TopAppBar(
                title = { Text("Productos", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        modifier = Modifier.background(ColorGradients.grayModern)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Spacer(modifier = Modifier.height(3.dp))

            // Header compacto
            CompactHeaderCard(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onAddProductClick = {
                    viewModel.onAddProductClick()
                    onAddProduct()
                }
            )

            // Body con scroll horizontal
            CompactBodyCard(
                uiState = uiState,
                onRetry = viewModel::retryLoad,
                onNextPage = viewModel::loadNextPage,
                onPreviousPage = viewModel::loadPreviousPage,
                modifier = Modifier.weight(1f)
            )

            // Footer compacto
            CompactFooterCard(
                totalProducts = uiState.totalProducts,
                filteredTotal = uiState.filteredTotal,
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                isFiltering = searchQuery.isNotBlank()
            )
        }
    }
}

// Header Card Compacto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactHeaderCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddProductClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp), // Reducido ligeramente
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Tu padding original
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tu buscador original con un solo cambio
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Buscar productos...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp), // SOLO ESTE CAMBIO: de 48.dp a 54.dp
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            // Botón circular - 10% del ancho
            FloatingActionButton(
                onClick = onAddProductClick,
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar producto",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddProductClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    "Buscar por código o nombre...",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = Color.White.copy(alpha = 0.8f)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Limpiar",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(15.dp)),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedBorderColor = Color.White.copy(alpha = 0.8f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                cursorColor = Color.White
            )
        )

        // Add Product Button
        Button(
            onClick = onAddProductClick,
            modifier = Modifier
                .height(56.dp)
                .clip(RoundedCornerShape(30.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(ColorGradients.goldLuxury, RoundedCornerShape(25.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Agregar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Nuevo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Body Card Compacto con scroll horizontal
@Composable
private fun CompactBodyCard(
    uiState: ProductUiState,
    onRetry: () -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug logs
    Log.d("CompactBodyCard", "isLoading: ${uiState.isLoading}")
    Log.d("CompactBodyCard", "error: ${uiState.error}")
    Log.d("CompactBodyCard", "products.size: ${uiState.products.size}")
    Log.d("CompactBodyCard", "products.isEmpty(): ${uiState.products.isEmpty()}")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        // TEMPORAL: Forzar mostrar productos si existen, ignorar isLoading
        if (uiState.products.isNotEmpty()) {
            Log.d("CompactBodyCard", "FORZANDO lista de productos: ${uiState.products.size}")
            CompactProductList(
                products = uiState.products,
                hasMorePages = uiState.hasMorePages,
                hasPreviousPages = uiState.hasPreviousPages,
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                onNextPage = onNextPage,
                onPreviousPage = onPreviousPage
            )
        } else {
            when {
                uiState.isLoading -> {
                    Log.d("CompactBodyCard", "Mostrando loading...")
                    CompactLoadingContent()
                }
                uiState.error != null -> {
                    Log.d("CompactBodyCard", "Mostrando error: ${uiState.error}")
                    CompactErrorContent(error = uiState.error, onRetry = onRetry)
                }
                else -> {
                    Log.d("CompactBodyCard", "Mostrando empty...")
                    CompactEmptyContent()
                }
            }
        }
    }
}

// Loading, Error y Empty compactos
@Composable
private fun CompactLoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = Color(0xFF1976D2)
            )
            Text(
                text = "Cargando...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun CompactErrorContent(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFFFF6B6B)
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color(0xFF666666)
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Reintentar", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun CompactEmptyContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF999999)
            )
            Text(
                text = "No hay productos",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

// Lista de productos compacta con scroll horizontal
@Composable
private fun CompactProductList(
    products: List<IProduct>,
    hasMorePages: Boolean,
    hasPreviousPages: Boolean,
    currentPage: Int,
    totalPages: Int,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit
) {
    // Estado de scroll compartido para sincronizar header y contenido
    val scrollState = rememberScrollState()

    Column {
        // Header con scroll horizontal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1976D2))
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(scrollState) // Scroll sincronizado
                    .padding(end = 16.dp), // Padding extra para evitar corte
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Text("ID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                Text("Código", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                Text("Producto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(140.dp), textAlign = TextAlign.Center)
//                Text("VU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
//                Text("PU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                Text("Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
//                Text("UM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            }
        }

        // Lista con scroll horizontal sincronizado
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 470.dp)
        ) {
            items(products) { product ->
                CompactProductItem(product = product, scrollState = scrollState)
                Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
            }
        }

        // Paginación compacta
        if (totalPages > 1) {
            CompactPaginationControls(
                currentPage = currentPage,
                totalPages = totalPages,
                hasPreviousPages = hasPreviousPages,
                hasMorePages = hasMorePages,
                onPreviousPage = onPreviousPage,
                onNextPage = onNextPage
            )
        }
    }
}
@Composable
private fun CompactProductItem(product: IProduct, scrollState: ScrollState) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState) // Mismo scrollState que el header
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .padding(end = 16.dp), // Padding extra para alineación
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Código
            Text(
                text = product.code,
                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFFFCFAFA),
                modifier = Modifier.width(60.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            // Nombre
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFFFCFAFA),
                modifier = Modifier.width(140.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                textAlign = TextAlign.Start
            )

            // Stock
            Text(
                text = "%.1f".format(product.stock),
                style = MaterialTheme.typography.bodySmall,
                color = if (product.stock > 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
                modifier = Modifier.width(60.dp),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            // Unidad
//            Text(
//                text = product.minimumUnitName,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFFFCFAFA),
//                modifier = Modifier.width(40.dp),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 11.sp,
//                textAlign = TextAlign.Center
//            )

            // Botón de lupa para ver detalles
            IconButton(
                onClick = { showDetailsDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Ver detalles",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Modal de detalles del producto
    if (showDetailsDialog) {
        ProductDetailsDialog(
            product = product,
            onDismiss = { showDetailsDialog = false }
        )
    }
}

@Composable
private fun ProductDetailsDialog(
    product: IProduct,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header compacto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 40.dp), // Espacio para el botón cerrar
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detalles",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Información básica compacta
                    CompactDetailSection(
                        items = listOf(
                            "ID" to product.id.toString(),
                            "Código" to product.code,
                            "Unidad" to product.minimumUnitName
                        )
                    )

                    // Nombre del producto (puede ser largo)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Producto",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Precios compactos
                    CompactDetailSection(
                        title = "Precios",
                        items = listOf(
                            "Sin IGV" to "S/ %.3f".format(product.priceWithoutIgv3),
                            "Con IGV" to "S/ %.3f".format(product.priceWithIgv3),
                            "IGV (18%)" to "S/ %.3f".format(product.priceWithIgv3 - product.priceWithoutIgv3)
                        ),
                        valueColors = listOf(
                            Color(0xFF1976D2),
                            Color(0xFF4CAF50),
                            Color(0xFFFF9800)
                        )
                    )

                    // Stock compacto
                    val stockStatus = when {
                        product.stock <= 0 -> "Sin stock" to Color(0xFFFF5722)
                        product.stock < 10 -> "Stock bajo" to Color(0xFFFF9800)
                        product.stock < 50 -> "Stock normal" to Color(0xFF2196F3)
                        else -> "Stock alto" to Color(0xFF4CAF50)
                    }

                    CompactDetailSection(
                        title = "Inventario",
                        items = listOf(
                            "Cantidad" to "%.1f ${product.minimumUnitName}".format(product.stock),
                            "Estado" to stockStatus.first
                        ),
                        valueColors = listOf(
                            if (product.stock > 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
                            stockStatus.second
                        )
                    )
                }

                // Botón cerrar en la esquina superior derecha
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDetailSection(
    title: String? = null,
    items: List<Pair<String, String>>,
    valueColors: List<Color>? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = valueColors?.getOrNull(index) ?: MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
//@Composable
//private fun CompactProductItem(product: IProduct, scrollState: ScrollState) {
//    Surface(
//        modifier = Modifier.fillMaxWidth(),
//        color = Color.Transparent
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .horizontalScroll(scrollState) // Mismo scrollState que el header
//                .padding(horizontal = 12.dp, vertical = 4.dp)
//                .padding(end = 16.dp), // Padding extra para alineación
//            horizontalArrangement = Arrangement.spacedBy(4.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // ID
////            Text(
////                text = product.id.toString(),
////                style = MaterialTheme.typography.bodySmall,
////                color = Color(0xFF666666),
////                modifier = Modifier.width(40.dp),
////                fontSize = 11.sp,
////                textAlign = TextAlign.Center
////            )
//
//            // Código
//            Text(
//                text = product.code,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFF333333),
//                modifier = Modifier.width(60.dp),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 11.sp,
//                textAlign = TextAlign.Center
//            )
//
//            // Nombre
//            Text(
//                text = product.name,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFF333333),
//                modifier = Modifier.width(140.dp),
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 11.sp,
//                textAlign = TextAlign.Start
//            )
//
//            // Precio Sin IGV (VU)
////            Text(
////                text = "%.3f".format(product.priceWithoutIgv3),
////                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
////                color = Color(0xFF1976D2),
////                modifier = Modifier.width(60.dp),
////                fontSize = 11.sp,
////                textAlign = TextAlign.End
////            )
//
//            // Precio Con IGV (PU)
////            Text(
////                text = "%.3f".format(product.priceWithIgv3),
////                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
////                color = Color(0xFF4CAF50),
////                modifier = Modifier.width(60.dp),
////                fontSize = 11.sp,
////                textAlign = TextAlign.End
////            )
//
//            // Stock
//            Text(
//                text = "%.1f".format(product.stock),
//                style = MaterialTheme.typography.bodySmall,
//                color = if (product.stock > 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
//                modifier = Modifier.width(60.dp),
//                fontSize = 11.sp,
//                textAlign = TextAlign.Center
//            )
//
//            // Unidad
//            Text(
//                text = product.minimumUnitName,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFF666666),
//                modifier = Modifier.width(40.dp),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                fontSize = 11.sp,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}

@Composable
private fun CompactPaginationControls(
    currentPage: Int,
    totalPages: Int,
    hasPreviousPages: Boolean,
    hasMorePages: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF8F9FA),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp), // Reducido el padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Anterior - Tamaño fijo pequeño
            Button(
                onClick = onPreviousPage,
                enabled = hasPreviousPages,
                modifier = Modifier
                    .height(32.dp)
                    .width(36.dp), // Ancho más pequeño
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasPreviousPages) Color(0xFF1976D2) else Color(0xFFCCCCCC),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF999999)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp) // Sin padding interno
            ) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Información de página central - Flexible
            Card(
                modifier = Modifier
                    .height(32.dp)
                    .weight(1f) // Ocupa el espacio disponible
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentPage / $totalPages", // Texto más corto
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF333333),
                        fontSize = 11.sp
                    )
                }
            }

            // Botón Siguiente - Tamaño fijo pequeño
            Button(
                onClick = onNextPage,
                enabled = hasMorePages,
                modifier = Modifier
                    .height(32.dp)
                    .width(36.dp), // Ancho más pequeño
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasMorePages) Color(0xFF1976D2) else Color(0xFFCCCCCC),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF999999)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp) // Sin padding interno
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Footer compacto
@Composable
private fun CompactFooterCard(
    totalProducts: Int,
    filteredTotal: Int,
    currentPage: Int,
    totalPages: Int,
    isFiltering: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información de productos
            Column {
                if (isFiltering) {
                    Text(
                        text = "Filtrados: $filteredTotal de $totalProducts",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF333333),
                        fontSize = 14.sp
                    )
                } else {
                    Text(
                        text = "Total: $totalProducts productos",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF333333),
                        fontSize = 14.sp
                    )
                }
            }

            // Icono de estado
            Icon(
                if (isFiltering) Icons.Default.Search else Icons.Default.Inventory,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProductScreen(
//    viewModel: ProductViewModel = hiltViewModel(),
//    onBack: () -> Unit = {},
//    onAddProduct: () -> Unit = {}
//) {
//    val uiState by viewModel.uiState.collectAsState()
//    val searchQuery by viewModel.searchQuery.collectAsState()
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(ColorGradients.grayModern)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            // Header con gradiente
//            ModernHeader(
//                onBack = onBack,
//                searchQuery = searchQuery,
//                onSearchQueryChange = viewModel::onSearchQueryChange,
//                onAddProductClick = {
//                    viewModel.onAddProductClick()
//                    onAddProduct()
//                }
//            )
//
//            // Content scrollable
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(horizontal = 16.dp),
//                verticalArrangement = Arrangement.spacedBy(16.dp),
//                contentPadding = PaddingValues(vertical = 16.dp)
//            ) {
//                // Products Content
//                item {
//                    ProductsContent(
//                        uiState = uiState,
//                        onRetry = viewModel::retryLoad,
//                        onNextPage = viewModel::loadNextPage,
//                        onPreviousPage = viewModel::loadPreviousPage
//                    )
//                }
//
//                // Footer Statistics
//                item {
//                    FooterStatistics(
//                        totalProducts = uiState.totalProducts,
//                        filteredTotal = uiState.filteredTotal,
//                        currentPage = uiState.currentPage,
//                        totalPages = uiState.totalPages,
//                        isFiltering = searchQuery.isNotBlank()
//                    )
//                }
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun ModernHeader(
//    onBack: () -> Unit,
//    searchQuery: String,
//    onSearchQueryChange: (String) -> Unit,
//    onAddProductClick: () -> Unit
//) {
//    Surface(
//        modifier = Modifier
//            .fillMaxWidth()
//            .shadow(8.dp),
//        color = Color.Transparent
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(ColorGradients.blueVibrant)
//                .padding(16.dp)
//        ) {
//            Column(
//                verticalArrangement = Arrangement.spacedBy(16.dp)
//            ) {
//                // Top Bar
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    IconButton(
//                        onClick = onBack,
//                        modifier = Modifier
//                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
//                            .size(48.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.ArrowBack,
//                            contentDescription = "Volver",
//                            tint = Color.White
//                        )
//                    }
//
//                    Text(
//                        text = "Gestión de Productos",
//                        style = MaterialTheme.typography.headlineSmall.copy(
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 20.sp
//                        ),
//                        color = Color.White
//                    )
//
//                    Box(modifier = Modifier.size(48.dp)) // Spacer for balance
//                }
//
//                // Modern Search Bar
//                ModernSearchBar(
//                    searchQuery = searchQuery,
//                    onSearchQueryChange = onSearchQueryChange,
//                    onAddProductClick = onAddProductClick
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun ModernSearchBar(
//    searchQuery: String,
//    onSearchQueryChange: (String) -> Unit,
//    onAddProductClick: () -> Unit
//) {
//    Row(
//        horizontalArrangement = Arrangement.spacedBy(12.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // Search Field
//        OutlinedTextField(
//            value = searchQuery,
//            onValueChange = onSearchQueryChange,
//            placeholder = {
//                Text(
//                    "Buscar por código o nombre...",
//                    color = Color.White.copy(alpha = 0.7f)
//                )
//            },
//            leadingIcon = {
//                Icon(
//                    Icons.Default.Search,
//                    contentDescription = "Buscar",
//                    tint = Color.White.copy(alpha = 0.8f)
//                )
//            },
//            trailingIcon = {
//                if (searchQuery.isNotEmpty()) {
//                    IconButton(onClick = { onSearchQueryChange("") }) {
//                        Icon(
//                            Icons.Default.Clear,
//                            contentDescription = "Limpiar",
//                            tint = Color.White.copy(alpha = 0.8f)
//                        )
//                    }
//                }
//            },
//            modifier = Modifier
//                .weight(1f)
//                .clip(RoundedCornerShape(25.dp)),
//            singleLine = true,
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedTextColor = Color.White,
//                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
//                focusedBorderColor = Color.White.copy(alpha = 0.8f),
//                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
//                focusedContainerColor = Color.White.copy(alpha = 0.1f),
//                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
//                cursorColor = Color.White
//            )
//        )
//
//        // Add Product Button
//        Button(
//            onClick = onAddProductClick,
//            modifier = Modifier
//                .height(56.dp)
//                .clip(RoundedCornerShape(25.dp)),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Color.Transparent
//            ),
//            contentPadding = PaddingValues(horizontal = 20.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .background(ColorGradients.goldLuxury, RoundedCornerShape(25.dp))
//                    .padding(horizontal = 16.dp, vertical = 8.dp)
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    Icon(
//                        Icons.Default.Add,
//                        contentDescription = "Agregar",
//                        tint = Color.White,
//                        modifier = Modifier.size(20.dp)
//                    )
//                    Text(
//                        "Nuevo",
//                        color = Color.White,
//                        fontWeight = FontWeight.Bold
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun ProductsContent(
//    uiState: ProductUiState,
//    onRetry: () -> Unit,
//    onNextPage: () -> Unit,
//    onPreviousPage: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .shadow(8.dp, RoundedCornerShape(20.dp)),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color.White
//        )
//    ) {
//        when {
//            uiState.isLoading -> {
//                ModernLoadingContent()
//            }
//            uiState.error != null -> {
//                ModernErrorContent(
//                    error = uiState.error,
//                    onRetry = onRetry
//                )
//            }
//            uiState.products.isEmpty() -> {
//                ModernEmptyContent()
//            }
//            else -> {
//                ModernProductList(
//                    products = uiState.products,
//                    hasMorePages = uiState.hasMorePages,
//                    hasPreviousPages = uiState.hasPreviousPages,
//                    currentPage = uiState.currentPage,
//                    totalPages = uiState.totalPages,
//                    onNextPage = onNextPage,
//                    onPreviousPage = onPreviousPage
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun ModernLoadingContent() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(400.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(24.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .background(ColorGradients.blueOcean, CircleShape),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    modifier = Modifier.size(40.dp),
//                    strokeWidth = 4.dp,
//                    color = Color.White
//                )
//            }
//            Text(
//                text = "Cargando productos...",
//                style = MaterialTheme.typography.bodyLarge.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                modifier = Modifier.applyTextGradient(ColorGradients.blueVibrant)
//            )
//        }
//    }
//}
//
//@Composable
//private fun ModernErrorContent(
//    error: String,
//    onRetry: () -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(400.dp)
//            .padding(32.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .background(
//                        Brush.linearGradient(
//                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
//                        ),
//                        CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    Icons.Default.Warning,
//                    contentDescription = null,
//                    modifier = Modifier.size(40.dp),
//                    tint = Color.White
//                )
//            }
//            Text(
//                text = error,
//                style = MaterialTheme.typography.bodyLarge,
//                textAlign = TextAlign.Center,
//                color = Color(0xFF666666)
//            )
//            Button(
//                onClick = onRetry,
//                modifier = Modifier
//                    .clip(RoundedCornerShape(25.dp))
//                    .background(ColorGradients.blueButtonGradient),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = Color.Transparent
//                )
//            ) {
//                Text("Reintentar", color = Color.White, fontWeight = FontWeight.Bold)
//            }
//        }
//    }
//}
//
//@Composable
//private fun ModernEmptyContent() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(400.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .background(ColorGradients.grayModern, CircleShape),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    Icons.Default.ShoppingCart,
//                    contentDescription = null,
//                    modifier = Modifier.size(40.dp),
//                    tint = Color.White
//                )
//            }
//            Text(
//                text = "No se encontraron productos",
//                style = MaterialTheme.typography.bodyLarge.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = Color(0xFF666666)
//            )
//        }
//    }
//}
//
//@Composable
//private fun ModernProductList(
//    products: List<IProduct>,
//    hasMorePages: Boolean,
//    hasPreviousPages: Boolean,
//    currentPage: Int,
//    totalPages: Int,
//    onNextPage: () -> Unit,
//    onPreviousPage: () -> Unit
//) {
//    Column {
//        // Modern Header
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(ColorGradients.blueVibrant)
//                .padding(16.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("ID", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Código", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Producto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Sin IGV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Con IGV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Stock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//                Text("Unidad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
//            }
//        }
//
//        // Products List
//        LazyColumn(
//            modifier = Modifier.heightIn(max = 500.dp)
//        ) {
//            items(products) { product ->
//                ModernProductItem(product = product)
//                Divider(
//                    color = Color(0xFFE0E0E0),
//                    thickness = 0.5.dp
//                )
//            }
//        }
//
//        // Pagination
//        if (totalPages > 1) {
//            ModernPaginationControls(
//                currentPage = currentPage,
//                totalPages = totalPages,
//                hasPreviousPages = hasPreviousPages,
//                hasMorePages = hasMorePages,
//                onPreviousPage = onPreviousPage,
//                onNextPage = onNextPage
//            )
//        }
//    }
//}
//
//@Composable
//private fun ModernProductItem(product: IProduct) {
//    Surface(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 8.dp, vertical = 4.dp),
//        color = Color.Transparent
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // ID
//            Text(
//                text = product.id.toString(),
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = Color(0xFF666666),
//                modifier = Modifier.weight(0.8f)
//            )
//
//            // Código
//            Text(
//                text = product.code,
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = Color(0xFF333333),
//                modifier = Modifier.weight(1.2f),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//
//            // Nombre
//            Text(
//                text = product.name,
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = Color(0xFF333333),
//                modifier = Modifier.weight(2f),
//                maxLines = 2,
//                overflow = TextOverflow.Ellipsis
//            )
//
//            // Precio Sin IGV
//            Text(
//                text = "S/ %.3f".format(product.priceWithoutIgv3),
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Bold
//                ),
//                color = Color(0xFF1976D2),
//                modifier = Modifier.weight(1f),
//                textAlign = TextAlign.End
//            )
//
//            // Precio Con IGV
//            Text(
//                text = "S/ %.3f".format(product.priceWithIgv3),
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Bold
//                ),
//                color = Color(0xFF4CAF50),
//                modifier = Modifier.weight(1f),
//                textAlign = TextAlign.End
//            )
//
//            // Stock
//            Text(
//                text = "%.1f".format(product.stock),
//                style = MaterialTheme.typography.bodySmall.copy(
//                    fontWeight = FontWeight.Medium
//                ),
//                color = if (product.stock > 0) Color(0xFF4CAF50) else Color(0xFFFF5722),
//                modifier = Modifier.weight(1f),
//                textAlign = TextAlign.Center
//            )
//
//            // Unidad
//            Text(
//                text = product.minimumUnitName,
//                style = MaterialTheme.typography.bodySmall,
//                color = Color(0xFF666666),
//                modifier = Modifier.weight(1f),
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//@Composable
//private fun ModernPaginationControls(
//    currentPage: Int,
//    totalPages: Int,
//    hasPreviousPages: Boolean,
//    hasMorePages: Boolean,
//    onPreviousPage: () -> Unit,
//    onNextPage: () -> Unit
//) {
//    Surface(
//        modifier = Modifier.fillMaxWidth(),
//        color = Color(0xFFF8F9FA)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Previous Button
//            Button(
//                onClick = onPreviousPage,
//                enabled = hasPreviousPages,
//                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = if (hasPreviousPages) Color.Transparent else Color.Gray.copy(alpha = 0.3f)
//                ),
//                contentPadding = PaddingValues(0.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .background(
//                            if (hasPreviousPages) ColorGradients.blueButtonGradient
//                            else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
//                            RoundedCornerShape(20.dp)
//                        )
//                        .padding(horizontal = 16.dp, vertical = 8.dp)
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.KeyboardArrowLeft,
//                            contentDescription = "Anterior",
//                            tint = Color.White,
//                            modifier = Modifier.size(20.dp)
//                        )
//                        Text("Anterior", color = Color.White, fontWeight = FontWeight.Medium)
//                    }
//                }
//            }
//
//            // Page Info
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "Página $currentPage",
//                    style = MaterialTheme.typography.titleMedium.copy(
//                        fontWeight = FontWeight.Bold
//                    ),
//                    modifier = Modifier.applyTextGradient(ColorGradients.blueVibrant)
//                )
//                Text(
//                    text = "de $totalPages",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = Color(0xFF666666)
//                )
//            }
//
//            // Next Button
//            Button(
//                onClick = onNextPage,
//                enabled = hasMorePages,
//                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = if (hasMorePages) Color.Transparent else Color.Gray.copy(alpha = 0.3f)
//                ),
//                contentPadding = PaddingValues(0.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .background(
//                            if (hasMorePages) ColorGradients.blueButtonGradient
//                            else Brush.linearGradient(listOf(Color.Gray, Color.Gray)),
//                            RoundedCornerShape(20.dp)
//                        )
//                        .padding(horizontal = 16.dp, vertical = 8.dp)
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(4.dp)
//                    ) {
//                        Text("Siguiente", color = Color.White, fontWeight = FontWeight.Medium)
//                        Icon(
//                            Icons.Default.KeyboardArrowRight,
//                            contentDescription = "Siguiente",
//                            tint = Color.White,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun FooterStatistics(
//    totalProducts: Int,
//    filteredTotal: Int,
//    currentPage: Int,
//    totalPages: Int,
//    isFiltering: Boolean
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .shadow(8.dp, RoundedCornerShape(20.dp)),
//        shape = RoundedCornerShape(20.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = Color.White
//        )
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(ColorGradients.purpleDream)
//                .padding(20.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                // Statistics Info
//                Column(
//                    verticalArrangement = Arrangement.spacedBy(8.dp)
//                ) {
//                    if (isFiltering) {
//                        Text(
//                            text = "Filtrados: $filteredTotal de $totalProducts",
//                            style = MaterialTheme.typography.titleLarge.copy(
//                                fontWeight = FontWeight.Bold,
//                                fontSize = 18.sp
//                            ),
//                            color = Color.White
//                        )
//                        Text(
//                            text = "Mostrando resultados de búsqueda",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = Color.White.copy(alpha = 0.8f)
//                        )
//                    } else {
//                        Text(
//                            text = "Total: $totalProducts productos",
//                            style = MaterialTheme.typography.titleLarge.copy(
//                                fontWeight = FontWeight.Bold,
//                                fontSize = 18.sp
//                            ),
//                            color = Color.White
//                        )
//                        Text(
//                            text = "Inventario completo",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = Color.White.copy(alpha = 0.8f)
//                        )
//                    }
//
//                    if (totalPages > 1) {
//                        Text(
//                            text = "Página $currentPage de $totalPages",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = Color.White.copy(alpha = 0.7f)
//                        )
//                    }
//                }
//
//                // Status Icon
//                Box(
//                    modifier = Modifier
//                        .size(60.dp)
//                        .background(
//                            Color.White.copy(alpha = 0.2f),
//                            CircleShape
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        if (isFiltering) Icons.Default.Search else Icons.Default.Inventory,
//                        contentDescription = null,
//                        tint = Color.White,
//                        modifier = Modifier.size(30.dp)
//                    )
//                }
//            }
//        }
//    }
//}
//
//// Extension function for safe double conversion
//fun String?.toSafeDouble(): Double {
//    return this?.toDoubleOrNull() ?: 0.0
//}