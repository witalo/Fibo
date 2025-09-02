package com.example.fibo.ui.screens

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.navigation.Screen
import com.example.fibo.reports.PdfDialogViewModel
import com.example.fibo.reports.PdfViewerDialog
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.ui.components.AppTopBarWithSearch
import com.example.fibo.ui.components.ClientFilterChip
import com.example.fibo.ui.components.ClientSearchDialog
import com.example.fibo.utils.ColorGradients
import com.example.fibo.viewmodels.NoteOfSaleViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun NoteOfSaleScreen(
    navController: NavController,
    viewModel: NoteOfSaleViewModel = hiltViewModel(),
    subsidiaryData: com.example.fibo.model.ISubsidiary? = null,
    onLogout: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val invoiceState by viewModel.invoiceState.collectAsState()

    // Estados para el diálogo de búsqueda
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()

    // Note Maneja el refresco cuando la pantalla obtiene foco
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadNoteOfSale(viewModel.selectedDate.value)
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
                title = if (selectedClient != null) {
                    "${selectedClient?.names?.take(15)}..."
                } else {
                    "Comprobantes"
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
            when (invoiceState) {
                is NoteOfSaleViewModel.InvoiceState.Loading -> NoteOfSaleCenterLoadingIndicator()
                is NoteOfSaleViewModel.InvoiceState.WaitingForUser -> {
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
                is NoteOfSaleViewModel.InvoiceState.Success -> {
                    val invoices = (invoiceState as NoteOfSaleViewModel.InvoiceState.Success).data
                    NoteOfSaleContent(
                        invoices = invoices,
                        onInvoiceClick = { invoice ->
                            navController.navigate("invoice_detail/${invoice.id}")
                        },
                        onNewInvoice = { navController.navigate(Screen.NewNoteOfSale.route) },
                        onNewReceipt = { navController.navigate(Screen.NewNoteOfSale.route) },
                        navController = navController,
                        selectedClient = selectedClient,
                        onClearClientFilter = { viewModel.clearClientSearch() }
                    )
                }
                is NoteOfSaleViewModel.InvoiceState.Error -> {
                    NoteOfSaleErrorMessage(
                        message = (invoiceState as NoteOfSaleViewModel.InvoiceState.Error).message,
                        onRetry = {viewModel.loadNoteOfSale(selectedDate) }
                    )
                }
            }
        }
    }
    
    // Diálogo de búsqueda de clientes
    ClientSearchDialog(
        isVisible = isSearchDialogOpen,
        onDismiss = { isSearchDialogOpen = false },
        searchQuery = searchQuery,
        onSearchQueryChange = { query -> viewModel.searchClients(query) },
        searchResults = searchResults,
        isLoading = isSearching,
        onClientSelected = { client ->
            viewModel.selectClient(client)
            isSearchDialogOpen = false
        }
    )
}

@Composable
fun NoteOfSaleContent(
    invoices: List<IOperation>,
    onInvoiceClick: (IOperation) -> Unit,
    onNewInvoice: () -> Unit,
    onNewReceipt: () -> Unit,
    navController: NavController,
    selectedClient: IPerson?,
    onClearClientFilter: () -> Unit
) {
    // Calculate cantidad de facturas y boletas
    val invoiceCount = invoices.count { it.documentTypeReadable == "NOTA DE SALIDA" }

    // Calculate monetary totals
    val invoiceAmountTotal = invoices
        .filter { it.documentTypeReadable == "NOTA DE SALIDA" }
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
                .weight(0.90f)
                .fillMaxWidth()
        ) {
            if (invoices.isEmpty()) {
                NoteOfSaleEmptyState(message = "No hay ventas")
            } else {
                // Wrap the InvoiceList in a Box to ensure scrolling works
                NoteOfSaleList(
                    invoices = invoices,
                    onInvoiceClick = onInvoiceClick,
                    navController = navController
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sección de Botones (18% del espacio)
        Column(
            modifier = Modifier
                .weight(0.10f)
                .fillMaxWidth(),
//            verticalArrangement = Arrangement.Bottom
        ) {
            NoteOfSaleActionButtons(
                onNewInvoice = onNewInvoice,
                invoiceCount = invoiceCount,
                invoiceAmountTotal = invoiceAmountTotal
            )
        }
    }
}

@Composable
fun NoteOfSaleList(
    invoices: List<IOperation>,
    onInvoiceClick: (IOperation) -> Unit,
    navController: NavController
) {
    // Removed fillMaxSize to allow proper scrolling
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(invoices) { invoice ->
            NoteOfSaleItem(
                invoice = invoice,
                onClick = { onInvoiceClick(invoice) },
                navController = navController
            )
        }
    }
}

@Composable
fun NoteOfSaleItem(
    invoice: IOperation,
    onClick: () -> Unit,
    viewModel: NoteOfSaleViewModel = hiltViewModel(),
    pdfViewModel: PdfDialogViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    var showConvertDialog by remember { mutableStateOf(false) }
    var selectedDocumentType by remember { mutableStateOf("FACTURA") } // Default to Factura
    // Observar el estado del diálogo
    val showPdfDialog by viewModel.showPdfDialog.collectAsState()
    val currentInvoiceId by viewModel.currentInvoiceId.collectAsState()
    val showCancelDialog by viewModel.showCancelDialog.collectAsState()
    val currentOperationId by viewModel.currentOperationId.collectAsState()

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

            invoice.client?.names?.let {
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
                Box(
                    modifier = Modifier.clickable { showConvertDialog = true }
                ) {
                    Chip(
                        label = "NOTA DE SALIDA",
                        gradient = when (invoice.documentTypeReadable) {
                            "NOTA DE SALIDA" -> ColorGradients.blueOcean
                            else -> ColorGradients.greenNature
                        }
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.showPdfDialog(invoice.id)
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
                                viewModel.showCancelDialog(invoice.id)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Anular",
                            tint = if (isNoteOfSaleWithinDays(invoice.emitDate, when (invoice.documentType.cleanNoteOfSaleDocumentType()) {
                                    "01" -> 3
                                    "03" -> 5
                                    "NS" -> 90
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
                        text = "Anular Venta",
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
                        text = "Realizado el: ${invoice.emitDate}",
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
                        text = when (invoice.documentType.cleanNoteOfSaleDocumentType()) {
                            "01" -> "Facturas solo pueden anularse dentro de los 3 días de emisión"
                            "03" -> "Boletas solo pueden anularse dentro de los 5 días de emisión"
                            "NS" -> "Esta seguro de anular la Nota de Venta?"
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
                            operationId = invoice.id,
                            operationType = invoice.documentType.cleanNoteOfSaleDocumentType(),
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
                    enabled = when (invoice.documentType.cleanNoteOfSaleDocumentType()) {
                        "01" -> isNoteOfSaleWithinDays(invoice.emitDate, 3)
                        "03" -> isNoteOfSaleWithinDays(invoice.emitDate, 5)
                        "NS" -> isNoteOfSaleWithinDays(invoice.emitDate, 90)
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
            onDismiss = { viewModel.closePdfDialog() }
        )
    }
    // Show Convert Dialog
    if (showConvertDialog) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = {
                Text(
                    text = "${invoice.serial}-${invoice.correlative}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Convertir a comprobante",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Document type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DocumentNoteTypeChip(
                            label = "FACTURA",
                            isSelected = selectedDocumentType == "FACTURA",
                            onClick = { selectedDocumentType = "FACTURA" },
                            modifier = Modifier.weight(1f)
                        )
                        DocumentNoteTypeChip(
                            label = "BOLETA",
                            isSelected = selectedDocumentType == "BOLETA",
                            onClick = { selectedDocumentType = "BOLETA" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConvertDialog = false
                        // Lógica condicional para navegar según el tipo de documento
                        if (selectedDocumentType == "FACTURA") {
                            navController.navigate(Screen.NewInvoice.createRoute(invoice.id)) {
                                popUpTo(Screen.NoteOfSale.route)
                            }
                        } else {
                            navController.navigate(Screen.NewReceipt.createRoute(invoice.id)) {
                                popUpTo(Screen.NoteOfSale.route)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3) // Azul más vibrante
                    )
                ) {
                    Text(
                        "Generar",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConvertDialog = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        "Cancelar",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}
// Función de extensión para verificar días
fun isNoteOfSaleWithinDays(emitDate: String, maxDays: Int): Boolean {
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
fun String.cleanNoteOfSaleDocumentType(): String {
    return this.replace("A_", "")
}
@Composable
fun NoteOfSaleActionButtons(
    onNewInvoice: () -> Unit,
    invoiceCount: Int,
    invoiceAmountTotal: Double
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
                    contentDescription = "Nueva Venta",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Venta",
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
fun NoteOfSaleChip(
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
fun DocumentNoteTypeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoteOfSaleCenterLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun NoteOfSaleEmptyState(message: String) {
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
fun NoteOfSaleErrorMessage(message: String, onRetry: () -> Unit) {
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