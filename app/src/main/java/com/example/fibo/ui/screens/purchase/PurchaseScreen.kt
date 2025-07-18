package com.example.fibo.ui.screens.purchase

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISupplier
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.ui.components.AppTopBarWithSearch
import com.example.fibo.ui.components.ClientFilterChip
import com.example.fibo.ui.screens.Chip
import com.example.fibo.ui.screens.cleanDocumentType
import com.example.fibo.ui.screens.isWithinDays
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.PurchaseState
import com.example.fibo.viewmodels.PurchaseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseScreen(
    navController: NavController,
    viewModel: PurchaseViewModel = hiltViewModel(),
    subsidiaryData: com.example.fibo.model.ISubsidiary? = null,
    onLogout: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()

    // Estados para el diálogo de búsqueda de proveedores
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()

    // Maneja el refresco cuando la pantalla obtiene foco
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadPurchases(viewModel.selectedDate.value)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            AppTopBarWithSearch(
                title = if (selectedSupplier != null) {
                    "${selectedSupplier?.names?.take(15)}..."
                } else {
                    "Compras"
                },
                onDateSelected = { date ->
                    viewModel.updateSelectedDate(date)
                },
                currentDate = selectedDate,
                onTitleClick = { isSearchDialogOpen = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (purchaseState) {
                is PurchaseState.Loading -> CenterLoadingIndicator()
                is PurchaseState.WaitingForUser -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Esperando autenticación...")
                        }
                    }
                }
                is PurchaseState.Success -> {
                    val purchases = (purchaseState as PurchaseState.Success).data
                    PurchaseContent(
                        purchases = purchases,
                        onPurchaseClick = { purchase ->
                            navController.navigate("purchase_detail/${purchase.id}")
                        },
                        onNewPurchase = { navController.navigate(Screen.NewPurchase.route) },
                        selectedSupplier = selectedSupplier,
                        onClearSupplierFilter = { viewModel.clearSupplierSearch() }
                    )
                }
                is PurchaseState.Error -> {
                    ErrorMessage(
                        message = (purchaseState as PurchaseState.Error).message,
                        onRetry = { viewModel.loadPurchases(selectedDate) }
                    )
                }
            }
        }
        
        // Diálogo de búsqueda de proveedores
        SupplierSearchDialog(
            isVisible = isSearchDialogOpen,
            onDismiss = { isSearchDialogOpen = false },
            searchQuery = searchQuery,
            onSearchQueryChange = { query -> viewModel.searchSuppliers(query) },
            searchResults = searchResults,
            isLoading = isSearching,
            onSupplierSelected = { supplier ->
                viewModel.selectSupplier(supplier)
                isSearchDialogOpen = false
            }
        )
    }
}

@Composable
fun PurchaseContent(
    purchases: List<IOperation>,
    onPurchaseClick: (IOperation) -> Unit,
    onNewPurchase: () -> Unit,
    selectedSupplier: ISupplier?,
    onClearSupplierFilter: () -> Unit
) {
    // Calculate cantidad de compras
    val invoiceCount = purchases.count { it.documentTypeReadable == "FACTURA" }
    val receiptCount = purchases.count { it.documentTypeReadable == "BOLETA" }

    // Calculate monetary totals
    val invoiceAmountTotal = purchases
        .filter { it.documentTypeReadable == "FACTURA" }
        .sumOf { it.totalToPay }

    val receiptAmountTotal = purchases
        .filter { it.documentTypeReadable == "BOLETA" }
        .sumOf { it.totalToPay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Mostrar información del proveedor seleccionado (si hay uno)
        if (selectedSupplier != null) {
            SupplierFilterChip(
                supplier = selectedSupplier,
                onClear = onClearSupplierFilter,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Sección de Listado (toma todo el espacio disponible)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (purchases.isEmpty()) {
                EmptyState(message = "No hay compras")
            } else {
                PurchaseList(
                    purchases = purchases,
                    onPurchaseClick = onPurchaseClick
                )
            }
        }

        // Sección de Botones (fijo en la parte inferior)
        ActionButtonsPurchase(
            onNewPurchase = onNewPurchase,
            invoiceCount = invoiceCount,
            receiptCount = receiptCount,
            invoiceAmountTotal = invoiceAmountTotal,
            receiptAmountTotal = receiptAmountTotal
        )
    }
}

@Composable
fun PurchaseList(
    purchases: List<IOperation>,
    onPurchaseClick: (IOperation) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(purchases) { purchase ->
            PurchaseItem(
                purchase = purchase,
                onClick = { onPurchaseClick(purchase) }
            )
        }
    }
}

@Composable
fun PurchaseItem(
    purchase: IOperation,
    onClick: () -> Unit,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Observar el estado del diálogo
    val showCancelDialog by viewModel.showCancelDialog.collectAsState()
    val currentOperationId by viewModel.currentOperationId.collectAsState()
    
    val isAnulado = purchase.operationStatus.replace("A_", "") == "06" || purchase.operationStatus.replace("A_", "") == "04"
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAnulado) 
                when (isSystemInDarkTheme()) {
                    true -> Color(0xFF7C1D1D)
                    false -> Color(0xFFFDCFCF)
                }
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${purchase.serial}-${purchase.correlative}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        brush = ColorGradients.purpleDream
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = purchase.emitDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mostrar información del proveedor en lugar del cliente
            purchase.supplier?.names?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    label = purchase.documentTypeReadable,
                    gradient = when (purchase.documentTypeReadable) {
                        "FACTURA" -> ColorGradients.purpleDeep
                        "BOLETA" -> ColorGradients.purpleDream
                        else -> ColorGradients.purpleDream
                    }
                )
                
                IconButton(
                    onClick = {
                        // PDF para compras - similar funcionalidad
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_pdf),
                        contentDescription = "PDF",
                        modifier = Modifier.size(25.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                
                if (isAnulado) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFFFFF).copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Anulado",
                            color = Color(0xFFC52B2B),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            if (!isAnulado) {
                                viewModel.showCancelDialog(purchase.id)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Anular",
                            tint = if (isWithinDays(purchase.emitDate, when (purchase.documentType.cleanDocumentType()) {
                                    "01" -> 3
                                    "03" -> 5
                                    else -> 0
                                })) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                
                Text(
                    text = "S/. ${String.format("%.2f", purchase.totalToPay)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (isSystemInDarkTheme()) {
                        true -> Color(0xFFAB47BC)
                        false -> Color(0xFF8E24AA)
                    }
                )
            }
        }
    }

    // Diálogo de anulación para compras
    if (showCancelDialog && currentOperationId == purchase.id) {
        AlertDialog(
            onDismissRequest = { viewModel.closeCancelDialog() },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Anular Compra",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${purchase.serial}-${purchase.correlative}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emitido el: ${purchase.emitDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (purchase.documentType.cleanDocumentType()) {
                            "01" -> "Facturas solo pueden anularse dentro de los 3 días de emisión"
                            "03" -> "Boletas solo pueden anularse dentro de los 5 días de emisión"
                            else -> "Este comprobante no puede ser anulado"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelOperation(
                            operationId = purchase.id,
                            operationType = purchase.documentType.cleanDocumentType(),
                            emitDate = purchase.emitDate
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    enabled = when (purchase.documentType.cleanDocumentType()) {
                        "01" -> isWithinDays(purchase.emitDate, 3)
                        "03" -> isWithinDays(purchase.emitDate, 5)
                        else -> false
                    }
                ) {
                    Text(
                        text = "Anular",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { viewModel.closeCancelDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D2D2D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ActionButtonsPurchase(
    onNewPurchase: () -> Unit,
    invoiceCount: Int,
    receiptCount: Int,
    invoiceAmountTotal: Double,
    receiptAmountTotal: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Botón Nueva Compra - Estilo similar a HomeScreen
        ElevatedButton(
            onClick = onNewPurchase,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF8E24AA),  // Morado para compras
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevatedButtonElevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFFFFFFF).copy(alpha = 0.3f)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                // Counter Chips
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "F: $invoiceCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "B: $receiptCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "S/. ${String.format("%.2f", invoiceAmountTotal + receiptAmountTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Nueva Compra",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Compra",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SupplierFilterChip(
    supplier: ISupplier,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proveedor seleccionado:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = supplier.names,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = supplier.documentNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Limpiar filtro",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun SupplierSearchDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<ISupplier>,
    isLoading: Boolean,
    onSupplierSelected: (ISupplier) -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Buscar Proveedor")
            },
            text = {
                Column {
                    androidx.compose.material3.OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = { Text("Nombre o RUC del proveedor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp)
                        ) {
                            items(searchResults) { supplier ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { onSupplierSelected(supplier) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = supplier.names,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = supplier.documentNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun CenterLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
} 