package com.example.fibo.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fibo.model.ISubsidiary
import com.example.fibo.navigation.Screen
import com.example.fibo.utils.ProductSortOrder

@Composable
fun AppScaffold(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    onLogout: () -> Unit,
    topBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryData,
        onMenuItemSelected = { option ->
            when (option) {
                "Inicio" -> navController.navigate(Screen.Home.route)
                "Cotizaciones" -> navController.navigate(Screen.Quotation.route)
                "Nota de salida" -> navController.navigate(Screen.NoteOfSale.route)
                "Perfil" -> navController.navigate(Screen.Profile.route)
                "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
                "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
                "Nueva Cotización" -> navController.navigate(Screen.NewQuotation.route)
                "Productos" -> navController.navigate(Screen.Product.route)
                "Cliente/Proveedor" -> navController.navigate(Screen.Person.route)
                "Compras" -> navController.navigate(Screen.Purchase.route)
                "Nueva Nota de salida" -> navController.navigate(Screen.NewNoteOfSale.route)
                "Nueva Guía" -> {
                    Log.d("Navigation", "Intentando navegar a Nueva Guía")
                    navController.navigate(Screen.NewGuide.route)
                }
                "Guías" -> navController.navigate(Screen.Guides.route)
                "Reporte" -> navController.navigate(Screen.Reports.route)
                "Reporte Pagos" -> navController.navigate(Screen.ReportPayment.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBarWrapper(
                        topBar = topBar,
                        onMenuClick = { isMenuOpen = !isMenuOpen }
                    )
                },
                content = content
            )
        }
    )
}

@Composable
private fun AppTopBarWrapper(
    topBar: @Composable () -> Unit,
    onMenuClick: () -> Unit
) {
    // Esta función wrapper permite inyectar la función onMenuClick a cualquier TopBar
    CompositionLocalProvider(
        LocalMenuClickHandler provides onMenuClick
    ) {
        topBar()
    }
}
// ... existing code ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSortClick: () -> Unit,
    onFilterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onClearFilters: () -> Unit, // AGREGAR ESTE PARÁMETRO
    isSearching: Boolean,
    isRefreshing: Boolean,
    currentSortOrder: ProductSortOrder,
    currentFilters: Pair<String?, Boolean?>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Título y botón de refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

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
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            label = { Text("Buscar productos...") },
            placeholder = { Text("Nombre, código o barcode") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Limpiar búsqueda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Barra de herramientas con filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botón de ordenamiento
            FilterChip(
                selected = false, // Siempre false para este botón
                onClick = onSortClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (currentSortOrder) {
                                ProductSortOrder.NAME_ASC -> "Nombre ↑"
                                ProductSortOrder.NAME_DESC -> "Nombre ↓"
                                ProductSortOrder.CODE_ASC -> "Código ↑"
                                ProductSortOrder.CODE_DESC -> "Código ↓"
                                ProductSortOrder.STOCK_ASC -> "Stock ↑"
                                ProductSortOrder.STOCK_DESC -> "Stock ↓"
                                ProductSortOrder.DATE_CREATED_ASC -> "Fecha ↑"
                                ProductSortOrder.DATE_CREATED_DESC -> "Fecha ↓"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            // Botón de filtros
            FilterChip(
                selected = currentFilters.first != null || currentFilters.second != null,
                onClick = onFilterClick,
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Filtros",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (currentFilters.first != null || currentFilters.second != null)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    selectedLabelColor = if (currentFilters.first != null || currentFilters.second != null)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            // Indicadores de filtros activos
            if (currentFilters.first != null) {
                FilterChip(
                    selected = true,
                    onClick = onFilterClick,
                    label = {
                        Text(
                            text = when (currentFilters.first) {
                                "01" -> "Venta"
                                "02" -> "Vehículo"
                                "03" -> "Reserva"
                                else -> "Categoría"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }

            if (currentFilters.second != null) {
                FilterChip(
                    selected = true,
                    onClick = onFilterClick,
                    label = {
                        Text(
                            text = if (currentFilters.second == true) "Disponible" else "No Disponible",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (currentFilters.second == true)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = if (currentFilters.second == true)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            // En la parte donde muestras el FilterChip para limpiar todo:
            if (currentFilters.first != null || currentFilters.second != null || searchQuery.isNotBlank()) {
                FilterChip(
                    selected = false,
                    onClick = {
                        onClearSearch()
                        onClearFilters() // USAR LA FUNCIÓN PASADA COMO PARÁMETRO
                    },
                    label = { Text("Limpiar todo") },
                    leadingIcon = {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                )
            }
        }

        // Indicador de búsqueda activa
        if (isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buscando...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
// CompositionLocal para pasar la función de menú
val LocalMenuClickHandler = compositionLocalOf<() -> Unit> { 
    error("MenuClickHandler not provided") 
} 