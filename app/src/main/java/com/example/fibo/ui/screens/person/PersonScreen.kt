//package com.example.fibo.ui.screens.person
//
//import com.example.fibo.ui.screens.CenterLoadingIndicator
//import com.example.fibo.ui.screens.EmptyState
//import com.example.fibo.ui.screens.ErrorMessage
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.ui.draw.clip
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.ReceiptLong
//import androidx.compose.material3.AlertDialog
//import androidx.compose.material3.Button
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.ElevatedButton
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavController
//import com.example.fibo.R
//import com.example.fibo.model.IOperation
//import com.example.fibo.model.IPerson
//import com.example.fibo.navigation.Screen
//import com.example.fibo.ui.components.AppScaffold
//import com.example.fibo.ui.components.AppTopBarWithSearch
//import com.example.fibo.utils.ColorGradients
//
//@Composable
//fun PersonScreen(
//    navController: NavController,
//    viewModel: PersonViewModel = hiltViewModel(),
//    subsidiaryData: com.example.fibo.model.ISubsidiary? = null,
//    onLogout: () -> Unit
//) {
//    val companyData by viewModel.companyData.collectAsState()
//    val userData by viewModel.userData.collectAsState()
//    val selectedDate by viewModel.selectedDate.collectAsState()
//    val quotationState by viewModel.quotationState.collectAsState()
//
//    // Estados para el diálogo de búsqueda
//    var isSearchDialogOpen by remember { mutableStateOf(false) }
//    val searchQuery by viewModel.searchQuery.collectAsState()
//    val searchResults by viewModel.searchResults.collectAsState()
//    val isSearching by viewModel.isSearching.collectAsState()
//    val selectedClient by viewModel.selectedClient.collectAsState()
//
//    AppScaffold(
//        navController = navController,
//        subsidiaryData = subsidiaryData,
//        onLogout = onLogout,
//        topBar = {
//            AppTopBarWithSearch(
//                title = if (selectedClient != null) {
//                    "${selectedClient?.names?.take(15)}..."
//                } else {
//                    "Clientes"
//                },
//                onDateSelected = { date ->
//                    viewModel.updateDate(date)
//                },
//                currentDate = selectedDate,
//                onTitleClick = { isSearchDialogOpen = true }
//            )
//        }
//    ) { paddingValues ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .background(MaterialTheme.colorScheme.background)
//        ) {
//            when (quotationState) {
//                is QuotationState.Loading -> CenterLoadingIndicator()
//                is QuotationState.WaitingForUser -> {
//                    // Mensaje de espera para autenticación
//                    Box(
//                        modifier = Modifier.fillMaxSize(),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            CircularProgressIndicator()
//                            Spacer(modifier = Modifier.height(16.dp))
//                            Text("Esperando autenticación...")
//                        }
//                    }
//                }
//
//                is QuotationState.Success -> {
//                    val quotation = (quotationState as QuotationState.Success).data
//                    QuotationContent(
//                        quotation = quotation,
//                        onQuotationClick = { q ->
//                            navController.navigate("quotation_detail/${q.id}")
//                        },
//                        onNewQuotation = { navController.navigate(Screen.NewQuotation.route) },
//                        navController = navController,
//                        selectedClient = selectedClient,
//                        onClearClientFilter = { viewModel.clearClientSearch() }
//                    )
//                }
//
//                is QuotationState.Error -> {
//                    ErrorMessage(
//                        message = (quotationState as QuotationState.Error).message,
//                        onRetry = {
//                            userData?.id?.let { userId ->
//                                viewModel.loadQuotation(selectedDate, userId)
//                            } ?: run {
//                                QuotationState.WaitingForUser
//                            }
//                        }
//                    )
//                }
//            }
//        }
//    }
//
//    // Diálogo de búsqueda de clientes
//    ClientSearchDialog(
//        isVisible = isSearchDialogOpen,
//        onDismiss = { isSearchDialogOpen = false },
//        searchQuery = searchQuery,
//        onSearchQueryChange = { query -> viewModel.searchClients(query) },
//        searchResults = searchResults,
//        isLoading = isSearching,
//        onClientSelected = { client ->
//            viewModel.selectClient(client)
//            isSearchDialogOpen = false
//        }
//    )
//}
//
//@Composable
//fun QuotationContent(
//    quotation: List<IOperation>,
//    onQuotationClick: (IOperation) -> Unit,
//    onNewQuotation: () -> Unit,
//    navController: NavController,
//    selectedClient: IPerson?,
//    onClearClientFilter: () -> Unit
//) {
//    val quotationCount = quotation.count { it.documentTypeReadable == "COMPROBANTE DE OPERACIONES" }
//
//    // Calculate monetary totals
//    val quotationAmountTotal = quotation
//        .filter { it.documentTypeReadable == "COMPROBANTE DE OPERACIONES" }
//        .sumOf { it.totalToPay }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(horizontal = 8.dp)
//    ) {
//        // Mostrar información del cliente seleccionado (si hay uno)
//        if (selectedClient != null) {
//            ClientFilterChip(
//                client = selectedClient,
//                onClear = onClearClientFilter,
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
//        }
//
//        // Sección de Listado (toma todo el espacio disponible)
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//        ) {
//            if (quotation.isEmpty()) {
//                EmptyState(message = "No hay cotizaciones")
//            } else {
//                QuotationList(
//                    quotation = quotation,
//                    onQuotationClick = onQuotationClick,
//                    navController = navController
//                )
//            }
//        }
//
//        // Sección de Botones (fijo en la parte inferior)
//        ActionButtonsQuotation(
//            onNewQuotation = onNewQuotation,
//            quotationCount = quotationCount,
//            quotationAmountTotal = quotationAmountTotal
//        )
//    }
//}
//
//@Composable
//fun QuotationList(
//    quotation: List<IOperation>,
//    onQuotationClick: (IOperation) -> Unit,
//    navController: NavController
//) {
//    LazyColumn(
//        contentPadding = PaddingValues(vertical = 8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        items(quotation) { quotation ->
//            QuotationItem(
//                quotation = quotation,
//                onClick = { onQuotationClick(quotation) },
//                navController = navController
//            )
//        }
//    }
//}
//
//@Composable
//fun QuotationItem(
//    quotation: IOperation,
//    onClick: () -> Unit,
//    navController: NavController
//) {
//    val isDarkTheme = isSystemInDarkTheme()
//    var showPdfDialog by remember { mutableStateOf(false) }
//
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .padding(horizontal = 4.dp),
//        shape = RoundedCornerShape(12.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
//        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//        border = BorderStroke(
//            width = 1.dp,
//            color = if (isDarkTheme) Color(0xFF444444) else Color(0xFFE0E0E0)
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Icono de la izquierda
//            Surface(
//                modifier = Modifier.size(48.dp),
//                shape = RoundedCornerShape(8.dp),
//                color = Color(0xFF4CAF50).copy(alpha = 0.1f)
//            ) {
//                Box(
//                    contentAlignment = Alignment.Center,
//                    modifier = Modifier.fillMaxSize()
//                ) {
//                    Icon(
//                        Icons.Default.ReceiptLong,
//                        contentDescription = null,
//                        tint = Color(0xFF4CAF50),
//                        modifier = Modifier.size(24.dp)
//                    )
//                }
//            }
//
//            // Contenido principal
//            Column(
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(horizontal = 12.dp)
//            ) {
//                Text(
//                    text = "${quotation.serial}-${quotation.correlative}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = if (isDarkTheme) Color.White else Color(0xFF1976D2)
//                )
//
//                Text(
//                    text = quotation.client.names?:"-",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF666666),
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.padding(top = 2.dp)
//                )
//
//                Text(
//                    text = quotation.emitDate,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = if (isDarkTheme) Color(0xFF999999) else Color(0xFF888888),
//                    modifier = Modifier.padding(top = 4.dp)
//                )
//            }
//
//            // Información de precio y botones
//            Column(
//                horizontalAlignment = Alignment.End
//            ) {
//                Text(
//                    text = "S/. ${String.format("%.2f", quotation.totalToPay)}",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = when (isDarkTheme) {
//                        true -> Color(0xFFFF9800)
//                        false -> Color(0xF5097BD9)
//                    }
//                )
//            }
//        }
//    }
//
//    if (showPdfDialog) {
//        QuotationPdfDialog(
//            isVisible = showPdfDialog,
//            quotation = quotation,
//            onDismiss = { showPdfDialog = false }
//        )
//    }
//}
//
//@Composable
//fun ActionButtonsQuotation(
//    onNewQuotation: () -> Unit,
//    quotationCount: Int,
//    quotationAmountTotal: Double
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 3.dp, vertical = 3.dp),
//        verticalArrangement = Arrangement.spacedBy(6.dp)
//    ) {
//        // Botón Nueva Cotización - Estilo mejorado como HomeScreen
//        ElevatedButton(
//            onClick = onNewQuotation,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(50.dp),
//            colors = ButtonDefaults.elevatedButtonColors(
//                containerColor = Color(0xFF8E44AD),  // Morado profesional para cotizaciones
//                contentColor = Color.White
//            ),
//            elevation = ButtonDefaults.elevatedButtonElevation(
//                defaultElevation = 6.dp,
//                pressedElevation = 2.dp
//            ),
//            shape = RoundedCornerShape(12.dp),
//            border = BorderStroke(
//                width = 1.dp,
//                color = Color(0xFFFFFFFF).copy(alpha = 0.3f)
//            )
//        ) {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(4.dp),
//                modifier = Modifier.fillMaxWidth(0.95f)
//            ) {
//                // Counter Chip
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(16.dp))
//                        .background(Color.White.copy(alpha = 0.15f))
//                        .padding(horizontal = 6.dp, vertical = 2.dp)
//                ) {
//                    Text(
//                        text = "Total: $quotationCount",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = Color.White
//                    )
//                }
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(16.dp))
//                        .background(Color.White.copy(alpha = 0.15f))
//                        .padding(horizontal = 6.dp, vertical = 2.dp)
//                ) {
//                    Text(
//                        text = "S/. ${String.format("%.2f", quotationAmountTotal)}",
//                        style = MaterialTheme.typography.labelSmall,
//                        color = Color.White
//                    )
//                }
//
//                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible
//
//                Icon(
//                    imageVector = Icons.Default.ReceiptLong,
//                    contentDescription = "Nueva Cotización",
//                    modifier = Modifier.size(18.dp),
//                    tint = Color.White
//                )
//                Text(
//                    text = "Nueva Cotización",
//                    style = MaterialTheme.typography.titleSmall,
//                    fontWeight = FontWeight.SemiBold,
//                    maxLines = 1 // Forzar una sola línea
//                )
//                Spacer(modifier = Modifier.weight(1f)) // Espacio flexible
//            }
//        }
//    }
//}
//
//@Composable
//fun DocumentTypeChip(
//    label: String,
//    isSelected: Boolean,
//    onClick: () -> Unit
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier
//            .height(32.dp)
//            .border(
//                width = 1.dp,
//                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
//                shape = RoundedCornerShape(16.dp)
//            ),
//        shape = RoundedCornerShape(16.dp),
//        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
//    ) {
//        Box(
//            modifier = Modifier
//                .padding(horizontal = 16.dp)
//                .fillMaxSize(),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                text = label,
//                fontWeight = FontWeight.Bold,
//                fontSize = 14.sp,
//                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}