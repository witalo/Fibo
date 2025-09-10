package com.example.fibo.ui.screens.reports
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.ISubsidiary
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.SideMenu
import com.example.fibo.ui.screens.CenterLoadingIndicator
import com.example.fibo.ui.screens.EmptyState
import com.example.fibo.ui.screens.ErrorMessage
import com.example.fibo.utils.ReportState
import java.text.NumberFormat
import java.util.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary? = null,
    onLogout: () -> Unit,
    reportViewModel: ReportViewModel = hiltViewModel()
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    // Obtener datos de subsidiary desde PreferencesManager si no se proporcionan
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val currentSubsidiaryData by preferencesManager.subsidiaryData.collectAsStateWithLifecycle()
    val subsidiaryToUse = subsidiaryData ?: currentSubsidiaryData

    val reportState by reportViewModel.reportState.collectAsStateWithLifecycle()
    val operationDetails by reportViewModel.operationDetails.collectAsStateWithLifecycle()
    val isLoadingDetails by reportViewModel.isLoadingDetails.collectAsStateWithLifecycle()
    val isFilterDialogOpen by reportViewModel.isFilterDialogOpen.collectAsStateWithLifecycle()


    // Observar el estado de exportación
    val exportState by reportViewModel.exportState.collectAsStateWithLifecycle()
    val showReportTypeDialog by reportViewModel.showReportTypeDialog.collectAsStateWithLifecycle()


    if (isFilterDialogOpen) {
        FilterDialog(
            viewModel = reportViewModel,
            onDismiss = { reportViewModel.closeFilterDialog() },
            onApply = { reportViewModel.applyFilters() }
        )
    }
    // Mostrar diálogo de tipo de reporte
    if (showReportTypeDialog) {
        ExportTypeDialog(
            onDismiss = { reportViewModel.hideExportDialog() },
            onExportSummary = {
                reportViewModel.exportToExcel(ReportType.SUMMARY)
            },
            onExportDetailed = {
                reportViewModel.exportToExcel(ReportType.DETAILED)
            }
        )
    }
    // 🆕 NUEVO: Manejar estados de exportación (mostrar mensajes)
    when (val state = exportState) {
        is ExportState.Success -> {
            LaunchedEffect(state) {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_LONG
                ).show()
                // Resetear estado después de mostrar
                kotlinx.coroutines.delay(100)
                reportViewModel.resetExportState()
            }
        }

        is ExportState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(
                    context,
                    state.message,
                    Toast.LENGTH_LONG
                ).show()
                // Resetear estado después de mostrar
                kotlinx.coroutines.delay(100)
                reportViewModel.resetExportState()
            }
        }

        ExportState.Idle -> { /* No hacer nada */ }
    }

// Diálogo de filtros (YA EXISTENTE)
    if (isFilterDialogOpen) {
        FilterDialog(
            viewModel = reportViewModel,
            onDismiss = { reportViewModel.closeFilterDialog() },
            onApply = { reportViewModel.applyFilters() }
        )
    }

    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryToUse,
        onMenuItemSelected = { option ->
            when (option) {
                "Inicio" -> navController.navigate(Screen.Home.route)
                "Perfil" -> navController.navigate(Screen.Profile.route)
                "Cotizaciones" -> navController.navigate(Screen.Quotation.route)
                "Nota de salida" -> navController.navigate(Screen.NoteOfSale.route)
                "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
                "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
                "Productos" -> navController.navigate(Screen.Product.route)
                "Compras" -> navController.navigate(Screen.Purchase.route)
                "Guías" -> navController.navigate(Screen.Guides.route)
                "Nueva Guía" -> navController.navigate(Screen.NewGuide.route)
                "Reporte" -> navController.navigate(Screen.Reports.route)
                "Reporte Pagos" -> navController.navigate(Screen.ReportPayment.route)
                "Reporte Mensual" -> navController.navigate(Screen.MonthlyReport.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    ReportTopBar(
                        onMenuClick = { isMenuOpen = !isMenuOpen },
                        onFilterClick = { reportViewModel.openFilterDialog() },
                        onExportClick = {
                            reportViewModel.showExportDialog()
//                            val exportData = reportViewModel.getExportData()
                            // Implementar exportación aquí
                        },
                        dateRange = reportViewModel.getFormattedDateRange(),
                        documentTypes = reportViewModel.getSelectedDocumentTypesText()
                    )
                },
                content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        when (val currentState = reportState) {
                            is ReportState.Initial,
                            is ReportState.Loading -> {
                                CenterLoadingIndicator()
                            }

                            is ReportState.Success -> {
                                ReportContent(
                                    operations = currentState.operations,
                                    operationDetails = operationDetails,
                                    isLoadingDetails = isLoadingDetails,
                                    onOperationClick = { operation ->
                                        reportViewModel.loadOperationDetails(operation.id)
                                    }
                                )
                            }

                            is ReportState.Error -> {
                                ErrorMessage(
                                    message = currentState.message,
                                    onRetry = { reportViewModel.refreshReports() }
                                )
                            }

                            is ReportState.Empty -> {
                                EmptyState()
                            }
                        }
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportTopBar(
    onMenuClick: () -> Unit,
    onFilterClick: () -> Unit,
    onExportClick: () -> Unit,
    dateRange: String,
    documentTypes: String
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Reportes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = dateRange,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                    
                    if (documentTypes != "Todos los tipos") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = documentTypes,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = onFilterClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (documentTypes != "Todos los tipos") {
                        Box(
                            modifier = Modifier
                                .offset(x = (-8).dp, y = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                                .align(Alignment.TopEnd)
                        )
                    }
                }

                IconButton(
                    onClick = onExportClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GetApp,
                        contentDescription = "Exportar Excel",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.height(56.dp)
    )
}

@Composable
fun ReportContent(
    operations: List<IOperation>,
    operationDetails: Map<Int, List<IOperationDetail>>,
    isLoadingDetails: Set<Int>,
    onOperationClick: (IOperation) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(operations) { operation ->
            OperationCard(
                operation = operation,
                details = operationDetails[operation.id] ?: emptyList(),
                isLoadingDetails = isLoadingDetails.contains(operation.id),
                onClick = { onOperationClick(operation) }
            )
        }
    }
}

@Composable
fun OperationCard(
    operation: IOperation,
    details: List<IOperationDetail>,
    isLoadingDetails: Boolean,
    onClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (details.isEmpty() && !isLoadingDetails) {
                    onClick()
                }
                isExpanded = !isExpanded
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocumentTypeChip(operation.documentTypeReadable)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${operation.serial}-${operation.correlative}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = operation.client?.names!!,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = operation.client.documentNumber!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(operation.totalToPay),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatDate(operation.emitDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingDetails) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cargando detalles...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (details.isEmpty()) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cargar detalles del comprobante")
                    }
                } else {
                    OperationDetails(details = details)
                }
            }
        }
    }
}

@Composable
fun OperationDetails(details: List<IOperationDetail>) {
    Column {
        Text(
            text = "Detalles del comprobante:",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        details.forEach { detail ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = detail.tariff.productName,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cant: ${formatQuantity(detail.quantity)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "P.U: ${formatCurrency(detail.unitPrice)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatCurrency(detail.totalAmount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentTypeChip(documentType: String) {
    val color = when (documentType) {
        "FACTURA" -> Color(0xFF4CAF50)
        "BOLETA" -> Color(0xFF2196F3)
        "NOTA DE CRÉDITO" -> Color(0xFFFF9800)
        "NOTA DE DÉBITO" -> Color(0xFFF44336)
        "NOTA DE SALIDA" -> Color(0xFF9C27B0)
        else -> Color(0xFF757575)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = documentType,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No se encontraron registros",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ajusta los filtros para ver más resultados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
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
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return format.format(amount)
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity == quantity.toInt().toDouble()) {
        quantity.toInt().toString()
    } else {
        String.format("%.2f", quantity)
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parts = dateString.split("-")
        if (parts.size >= 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}