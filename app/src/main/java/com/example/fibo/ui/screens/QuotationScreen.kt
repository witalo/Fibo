package com.example.fibo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppTopBar
import com.example.fibo.ui.components.SideMenu
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.QuotationState
import com.example.fibo.viewmodels.QuotationViewModel

@Composable
fun QuotationScreen(
    navController: NavController,
    viewModel: QuotationViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val quotationState by viewModel.quotationState.collectAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryData,
        onMenuItemSelected = { option ->
            when (option) {
                "Inicio" -> navController.navigate(Screen.Home.route)
                "Cotizaciones" -> navController.navigate(Screen.Quotation.route)
                "Notas de salida" -> navController.navigate(Screen.NotesOfSale.route)
                "Perfil" -> navController.navigate(Screen.Profile.route)
                "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
                "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
                "Nueva Cotización" -> navController.navigate(Screen.NewQuotation.route)
                "Nueva Nota de salida" -> navController.navigate(Screen.NewNotesOfSale.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "Cotizaciones",
                        onMenuClick = { isMenuOpen = !isMenuOpen },
                        onDateSelected = { date ->
                            viewModel.updateDate(date)
                        },
                        currentDate = selectedDate
                    )
                },
                content = { paddingValues ->
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
                                val quotation =
                                    (quotationState as QuotationState.Success).data
                                QuotationContent(
                                    quotation = quotation,
                                    onQuotationClick = { q ->
                                        navController.navigate("quotation_detail/${q.id}")
                                    },
                                    onNewQuotation = { navController.navigate(Screen.NewQuotation.route) }
                                )
                            }
                            is QuotationState.Error -> {
                                ErrorMessage(
                                    message = (quotationState as QuotationState.Error).message,
                                    onRetry = { userData?.id?.let { userId ->
                                        viewModel.loadQuotation(selectedDate, userId)
                                    } ?: run {
                                        QuotationState.WaitingForUser
                                    } }
                                )
                            }
                        }
                    }
                }
            )
        }
    )
}
@Composable
fun QuotationContent(
    quotation: List<IOperation>,
    onQuotationClick: (IOperation) -> Unit,
    onNewQuotation: () -> Unit
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
        // Sección de Listado (90% del espacio)
        Box(
            modifier = Modifier
                .weight(0.90f)
                .fillMaxWidth()
        ) {
            if (quotation.isEmpty()) {
                EmptyState(message = "No hay cotizaciones para esta fecha")
            } else {
                // Wrap the InvoiceList in a Box to ensure scrolling works
                QuotationList(
                    quotation = quotation,
                    onQuotationClick = onQuotationClick
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sección de Botones (10% del espacio)
        Column(
            modifier = Modifier
                .weight(0.10f)
                .fillMaxWidth(),
//            verticalArrangement = Arrangement.Bottom
        ) {
            ActionButtonsQuotation(
                onNewQuotation = onNewQuotation,
                quotationCount = quotationCount,
                quotationAmountTotal = quotationAmountTotal,
            )
        }
    }
}

@Composable
fun QuotationList(
    quotation: List<IOperation>,
    onQuotationClick: (IOperation) -> Unit
) {
    // Removed fillMaxSize to allow proper scrolling
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quotation) { quotation ->
            QuotationItem(
                quotation = quotation,
                onClick = { onQuotationClick(quotation) }
            )
        }
    }
}

@Composable
fun QuotationItem(
    quotation: IOperation,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isAnulado = quotation.operationStatus.replace("A_", "") == "06" || quotation.operationStatus.replace("A_", "") == "04"

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

            quotation.client.names?.let {
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
                    label = quotation.documentTypeReadable,
                    gradient = when (quotation.documentTypeReadable) {
                        "COTIZACIÓN" -> ColorGradients.blueOcean
                        else -> ColorGradients.greenNature
                    }
                )

                IconButton(
                    onClick = {
                       // ABRIR EL DIALOG DE PDF
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
        // Botón Nueva Cotización - Versión mejorada
        ElevatedButton(
            onClick = onNewQuotation,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color(0xFFAB6703),  // Azul profesional
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

                Spacer(modifier = Modifier.weight(1f))

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
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}