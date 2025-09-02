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
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.ui.components.AppTopBarWithSearch
import com.example.fibo.ui.components.ClientFilterChip
import com.example.fibo.ui.components.ClientSearchDialog
import com.example.fibo.ui.screens.quotation.QuotationPdfDialog
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.QuotationState
import com.example.fibo.viewmodels.QuotationViewModel

@Composable
fun QuotationScreen(
    navController: NavController,
    viewModel: QuotationViewModel = hiltViewModel(),
    subsidiaryData: com.example.fibo.model.ISubsidiary? = null,
    onLogout: () -> Unit
) {
    val companyData by viewModel.companyData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val quotationState by viewModel.quotationState.collectAsState()

    // Estados para el diálogo de búsqueda
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedClient by viewModel.selectedClient.collectAsState()
    
    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            AppTopBarWithSearch(
                title = if (selectedClient != null) {
                    "${selectedClient?.names?.take(15)}..."
                } else {
                    "Cotizaciones"
                },
                onDateSelected = { date ->
                    viewModel.updateDate(date)
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
            when (quotationState) {
                is QuotationState.Loading -> CenterLoadingIndicator()
                is QuotationState.WaitingForUser -> {
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

                is QuotationState.Success -> {
                    val quotation = (quotationState as QuotationState.Success).data
                    QuotationContent(
                        quotation = quotation,
                        onQuotationClick = { q ->
                            navController.navigate("quotation/${q.id}")
                        },
                        onNewQuotation = { navController.navigate(Screen.NewQuotation.route) },
                        navController = navController,
                        selectedClient = selectedClient,
                        onClearClientFilter = { viewModel.clearClientSearch() }
                    )
                }

                is QuotationState.Error -> {
                    ErrorMessage(
                        message = (quotationState as QuotationState.Error).message,
                        onRetry = {
                            userData?.id?.let { userId ->
                                viewModel.loadQuotation(selectedDate, userId)
                            } ?: run {
                                QuotationState.WaitingForUser
                            }
                        }
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
fun QuotationContent(
    quotation: List<IOperation>,
    onQuotationClick: (IOperation) -> Unit,
    onNewQuotation: () -> Unit,
    navController: NavController,
    selectedClient: IPerson?,
    onClearClientFilter: () -> Unit
) {
    val quotationCount = quotation.count { it.documentTypeReadable == "COMPROBANTE DE OPERACIONES" }

    // Calculate monetary totals
    val quotationAmountTotal = quotation
        .filter { it.documentTypeReadable == "COMPROBANTE DE OPERACIONES" }
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
        
        // Sección de Listado (toma todo el espacio disponible)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (quotation.isEmpty()) {
                EmptyState(message = "No hay cotizaciones")
            } else {
                QuotationList(
                    quotation = quotation,
                    onQuotationClick = onQuotationClick,
                    navController = navController
                )
            }
        }

        // Sección de Botones (fijo en la parte inferior)
        ActionButtonsQuotation(
            onNewQuotation = onNewQuotation,
            quotationCount = quotationCount,
            quotationAmountTotal = quotationAmountTotal
        )
    }
}

@Composable
fun QuotationList(
    quotation: List<IOperation>,
    onQuotationClick: (IOperation) -> Unit,
    navController: NavController
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quotation) { quotation ->
            QuotationItem(
                quotation = quotation,
                onClick = { onQuotationClick(quotation) },
                navController = navController
            )
        }
    }
}

@Composable
fun QuotationItem(
    quotation: IOperation,
    onClick: () -> Unit,
    navController: NavController,
) {

    var showConvertDialog by remember { mutableStateOf(false) }
    var selectedDocumentType by remember { mutableStateOf("FACTURA") } // Default to Factura


    val isAnulado = quotation.operationStatus.replace("A_", "") == "06" || quotation.operationStatus.replace("A_", "") == "04"
    var showPdfDialog by remember { mutableStateOf(false) }
    // Show PDF Dialog if needed
    if (showPdfDialog) {
        QuotationPdfDialog(
            isVisible = true,
            quotation = quotation,
            onDismiss = { showPdfDialog = false }
        )
    }
    // Show Convert Dialog
    if (showConvertDialog) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = {
                Text(
                    text = "${quotation.serial}-${quotation.correlative}",
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
                        DocumentTypeChip(
                            label = "FACTURA",
                            isSelected = selectedDocumentType == "FACTURA",
                            onClick = { selectedDocumentType = "FACTURA" },
                            modifier = Modifier.weight(1f)
                        )
                        DocumentTypeChip(
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
                            navController.navigate(Screen.NewInvoice.createRoute(quotation.id)) {
                                popUpTo(Screen.Quotation.route)
                            }
                        } else {
                            navController.navigate(Screen.NewReceipt.createRoute(quotation.id)) {
                                popUpTo(Screen.Quotation.route)
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
                    text = "${quotation.serial}-${quotation.correlative}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        brush = ColorGradients.orangeFire
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quotation.emitDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            quotation.client?.names?.let {
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
                // Modified Chip to be clickable
                Box(
                    modifier = Modifier.clickable { showConvertDialog = true }
                ) {
                    Chip(
                        label = "COTIZACIÓN",
                        gradient = when (quotation.documentTypeReadable) {
                            "COTIZACIÓN" -> ColorGradients.blueOcean
                            else -> ColorGradients.greenNature
                        }
                    )
                }

                IconButton(
                    onClick = {
                        showPdfDialog = true
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

                Text(
                    text = "S/. ${String.format("%.2f", quotation.totalToPay)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (isSystemInDarkTheme()) {
                        true -> Color(0xFFFF9800)
                        false -> Color(0xF5097BD9)
                    }
                )
            }
        }
    }
}

@Composable
fun ActionButtonsQuotation(
    onNewQuotation: () -> Unit,
    quotationCount: Int,
    quotationAmountTotal: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Botón Nueva Cotización - Estilo mejorado como HomeScreen
        ElevatedButton(
            onClick = onNewQuotation,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFF8E44AD),  // Morado profesional para cotizaciones
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
                        text = "Total: $quotationCount",
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
                        text = "S/. ${String.format("%.2f", quotationAmountTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible

                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Nueva Cotización",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Cotización",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1 // Forzar una sola línea
                )
                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible
            }
        }
    }
}

@Composable
fun DocumentTypeChip(
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