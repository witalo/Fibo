package com.example.fibo.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.viewmodels.HomeViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.navigation.Screen
import com.example.fibo.reports.PdfDialogViewModel
import com.example.fibo.reports.PdfGenerator
import com.example.fibo.reports.PdfViewerDialog
import com.example.fibo.ui.components.AppTopBar
import com.example.fibo.ui.components.SideMenu
import com.example.fibo.utils.ColorGradients
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Close
import com.example.fibo.model.IPerson
import com.example.fibo.ui.components.ClientFilterChip
import com.example.fibo.ui.components.ClientSearchDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val subsidiaryData by homeViewModel.subsidiaryData.collectAsState()
    val selectedDate by homeViewModel.selectedDate.collectAsState()
    val invoiceState by homeViewModel.invoiceState.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    // Estados para el diálogo de búsqueda
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val searchResults by homeViewModel.searchResults.collectAsState()
    val isSearching by homeViewModel.isSearching.collectAsState()
    val selectedClient by homeViewModel.selectedClient.collectAsState()

    // Maneja el refresco cuando la pantalla obtiene foco
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.loadInvoices(homeViewModel.selectedDate.value)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


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
                "Nueva Nota de salida" -> navController.navigate(Screen.NewNoteOfSale.route)
                "Reporte" -> navController.navigate(Screen.Reports.route)
                "Reporte Pagos" -> navController.navigate(Screen.ReportPayment.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBar(
//                        title = "Inicio",
                        title = if (selectedClient != null) {
                            "${selectedClient?.names?.take(15)}..."
                        } else {
                            "Comprobantes"
                        },
                        onMenuClick = { isMenuOpen = !isMenuOpen },
                        onDateSelected = { date ->
                            homeViewModel.updateSelectedDate(date)
                        },
                        currentDate = selectedDate,
                        onTitleClick = { isSearchDialogOpen = true }
                    )
                },
                content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .background(MaterialTheme.colorScheme.background)
                    ) {

                        when (invoiceState) {
                            is HomeViewModel.InvoiceState.Loading -> CenterLoadingIndicator()
                            is HomeViewModel.InvoiceState.WaitingForUser -> {
                                // Mensaje de espera para autenticación
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
                            is HomeViewModel.InvoiceState.Success -> {
                                val invoices =
                                    (invoiceState as HomeViewModel.InvoiceState.Success).data
                                InvoiceContent(
                                    invoices = invoices,
                                    onInvoiceClick = { invoice ->
                                        navController.navigate("invoice_detail/${invoice.id}")
                                    },
                                    onNewInvoice = { navController.navigate(Screen.NewInvoice.route) },
                                    onNewReceipt = { navController.navigate(Screen.NewReceipt.route) },
                                    selectedClient = selectedClient,
                                    onClearClientFilter = { homeViewModel.clearClientSearch() }
                                )
                            }
                            is HomeViewModel.InvoiceState.Error -> {
                                ErrorMessage(
                                    message = (invoiceState as HomeViewModel.InvoiceState.Error).message,
                                    onRetry = { homeViewModel.loadInvoices(selectedDate) }
                                )
                            }
                        }
//                        when (invoiceState) {
//                            is HomeViewModel.InvoiceState.Loading -> CenterLoadingIndicator()
//                            is HomeViewModel.InvoiceState.Success -> {
//                                val invoices =
//                                    (invoiceState as HomeViewModel.InvoiceState.Success).data
//                                InvoiceContent(
//                                    invoices = invoices,
//                                    onInvoiceClick = { invoice ->
//                                        navController.navigate("invoice_detail/${invoice.id}")
//                                    },
//                                    onNewInvoice = { navController.navigate(Screen.NewInvoice.route) },
//                                    onNewReceipt = { navController.navigate(Screen.NewReceipt.route) }
//                                )
//                            }
//
//                            is HomeViewModel.InvoiceState.Error -> {
//                                ErrorMessage(
//                                    message = (invoiceState as HomeViewModel.InvoiceState.Error).message,
//                                    onRetry = { homeViewModel.loadInvoices(selectedDate) }
//                                )
//                            }
//                        }
                    }
                }
            )
            // Diálogo de búsqueda de clientes
            ClientSearchDialog(
                isVisible = isSearchDialogOpen,
                onDismiss = { isSearchDialogOpen = false },
                searchQuery = searchQuery,
                onSearchQueryChange = { query -> homeViewModel.searchClients(query) },
                searchResults = searchResults,
                isLoading = isSearching,
                onClientSelected = { client ->
                    homeViewModel.selectClient(client)
                    isSearchDialogOpen = false
                }
            )
        }
    )
}

@Composable
fun InvoiceContent(
    invoices: List<IOperation>,
    onInvoiceClick: (IOperation) -> Unit,
    onNewInvoice: () -> Unit,
    onNewReceipt: () -> Unit,
    selectedClient: IPerson?,
    onClearClientFilter: () -> Unit
) {
    // Calculate cantidad de facturas y boletas
    val invoiceCount = invoices.count { it.documentTypeReadable == "FACTURA" }
    val receiptCount = invoices.count { it.documentTypeReadable == "BOLETA" }

    // Calculate monetary totals
    val invoiceAmountTotal = invoices
        .filter { it.documentTypeReadable == "FACTURA" }
        .sumOf { it.totalToPay }

    val receiptAmountTotal = invoices
        .filter { it.documentTypeReadable == "BOLETA" }
        .sumOf { it.totalToPay }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Mostrar información del cliente seleccionado (si hay uno)
        if (selectedClient != null) {
            ClientFilterChip(
                client = selectedClient,
                onClear = onClearClientFilter,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        // Sección de Listado (82% del espacio)
        Box(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxWidth()
        ) {
            if (invoices.isEmpty()) {
                EmptyState(message = "No hay comprobantes")
            } else {
                // Wrap the InvoiceList in a Box to ensure scrolling works
                InvoiceList(
                    invoices = invoices,
                    onInvoiceClick = onInvoiceClick
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sección de Botones (18% del espacio)
        Column(
            modifier = Modifier
                .weight(0.18f)
                .fillMaxWidth(),
//            verticalArrangement = Arrangement.Bottom
        ) {
            ActionButtons(
                onNewInvoice = onNewInvoice,
                onNewReceipt = onNewReceipt,
                invoiceCount = invoiceCount,
                receiptCount = receiptCount,
                invoiceAmountTotal = invoiceAmountTotal,
                receiptAmountTotal = receiptAmountTotal
            )
        }
    }
}

@Composable
fun InvoiceList(
    invoices: List<IOperation>,
    onInvoiceClick: (IOperation) -> Unit
) {
    // Removed fillMaxSize to allow proper scrolling
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(invoices) { invoice ->
            InvoiceItem(
                invoice = invoice,
                onClick = { onInvoiceClick(invoice) }
            )
        }
    }
}

@Composable
fun InvoiceItem(
    invoice: IOperation,
    onClick: () -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    pdfViewModel: PdfDialogViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Observar el estado del diálogo
    val showPdfDialog by homeViewModel.showPdfDialog.collectAsState()
    val currentInvoiceId by homeViewModel.currentInvoiceId.collectAsState()
    val showCancelDialog by homeViewModel.showCancelDialog.collectAsState()
    val currentOperationId by homeViewModel.currentOperationId.collectAsState()
    
    val isAnulado = invoice.operationStatus.replace("A_", "") == "06" || invoice.operationStatus.replace("A_", "") == "04"
    
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
//                Color.Red.copy(alpha = 0.1f)
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
                    text = "${invoice.serial}-${invoice.correlative}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        brush = ColorGradients.orangeFire
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.emitDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            invoice.client.names?.let {
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
                    label = invoice.documentTypeReadable,
                    gradient = when (invoice.documentTypeReadable) {
                        "FACTURA" -> ColorGradients.blueOcean
                        "BOLETA" -> ColorGradients.greenNature
                        else -> ColorGradients.greenNature
                    }
                )
                
                IconButton(
                    onClick = {
                        homeViewModel.showPdfDialog(invoice.id)
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
                                homeViewModel.showCancelDialog(invoice.id)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Anular",
                            tint = if (isWithinDays(invoice.emitDate, when (invoice.documentType.cleanDocumentType()) {
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
                    text = "S/. ${String.format("%.2f", invoice.totalToPay)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (isSystemInDarkTheme()) {
                        true -> Color(0xFFFF9800)
                        false -> Color(0xFF097BD9)
                    }
                )
            }
        }
    }

    // Diálogo de anulación
    if (showCancelDialog && currentOperationId == invoice.id) {
        AlertDialog(
            onDismissRequest = { homeViewModel.closeCancelDialog() },
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
                        text = "Anular Comprobante",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${invoice.serial}-${invoice.correlative}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Emitido el: ${invoice.emitDate}",
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
                        text = when (invoice.documentType.cleanDocumentType()) {
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
                        homeViewModel.cancelOperation(
                            operationId = invoice.id,
                            operationType = invoice.documentType.cleanDocumentType(),
                            emitDate = invoice.emitDate
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
                    enabled = when (invoice.documentType.cleanDocumentType()) {
                        "01" -> isWithinDays(invoice.emitDate, 3)
                        "03" -> isWithinDays(invoice.emitDate, 5)
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
                    onClick = { homeViewModel.closeCancelDialog() },
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

    // Mostrar el diálogo PDF cuando sea necesario
    if (showPdfDialog) {
        // Cargar los datos de la operación cuando el diálogo se muestra
        LaunchedEffect(currentInvoiceId) {
            if (currentInvoiceId > 0) {
                pdfViewModel.fetchOperationById(currentInvoiceId)
            }
        }

        // Implementar el diálogo de PDF
        // Mostrar el diálogo PdfViewerDialog cuando showPdfDialog es true
        PdfViewerDialog(
            isVisible = showPdfDialog,
            operationId = currentInvoiceId,
            onDismiss = { homeViewModel.closePdfDialog() }
        )
    }
}
// Función de extensión para verificar días
fun isWithinDays(emitDate: String, maxDays: Int): Boolean {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val emitDateObj = dateFormat.parse(emitDate) ?: return false
        val currentDate = Date()

        val diffInMillis = currentDate.time - emitDateObj.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        diffInDays <= maxDays
    } catch (e: Exception) {
        false
    }
}
fun String.cleanDocumentType(): String {
    return this.replace("A_", "")
}
@Composable
fun ActionButtons(
    onNewInvoice: () -> Unit,
    onNewReceipt: () -> Unit,
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
        // Botón Nueva Factura - Versión mejorada
        ElevatedButton(
            onClick = onNewInvoice,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF4A6FA5),  // Azul profesional
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
                // Counter Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Total: $invoiceCount",
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
                        text = "S/. ${String.format("%.2f", invoiceAmountTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }


                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible

                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Nueva Factura",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Factura",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1 // Forzar una sola línea
                )
                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible
//                Spacer(modifier = Modifier.weight(1f))
//                Icon(
//                    imageVector = Icons.Filled.ArrowForward,
//                    contentDescription = null,
//                    modifier = Modifier.size(18.dp),
//                    tint = Color.White.copy(alpha = 0.6f)
//                )
            }
        }

        // Botón Nueva Boleta - Versión mejorada
        ElevatedButton(
            onClick = onNewReceipt,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF6A8C72),  // Verde profesional
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
                // Counter Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Total: $receiptCount",
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
                        text = "S/. ${String.format("%.2f", receiptAmountTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible

                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = "Nueva Boleta",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Boleta",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1 // Forzar una sola línea
                )
                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible
//                Spacer(modifier = Modifier.weight(1f))
//                Icon(
//                    imageVector = Icons.Filled.ArrowForward,
//                    contentDescription = null,
//                    modifier = Modifier.size(18.dp),
//                    tint = Color.White.copy(alpha = 0.6f)
//                )
            }
        }
    }
}


@Composable
fun Chip(
    label: String,
    gradient: Brush // Mantenemos Brush pero con otro nombre
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f)) // Fondo transparente
//            .border(
//                width = 1.dp,
//                brush = gradient, // Borde con el gradiente
//                shape = RoundedCornerShape(16.dp)
//            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                brush = gradient
            )
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