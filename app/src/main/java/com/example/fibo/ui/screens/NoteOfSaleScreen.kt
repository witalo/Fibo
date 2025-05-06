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
import com.example.fibo.ui.screens.noteofsale.NoteOfSalePdfDialog
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.NoteOfSaleState
import com.example.fibo.viewmodels.NoteOfSaleViewModel

@Composable
fun NoteOfSaleScreen(
    navController: NavController,
    viewModel: NoteOfSaleViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val noteOfSaleState by viewModel.noteOfSaleState.collectAsState()
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
                "Nueva Nota de salida" -> navController.navigate(Screen.NewNoteOfSale.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "Nota de salida",
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

                        when (noteOfSaleState) {
                            is NoteOfSaleState.Loading -> CenterLoadingIndicator()
                            is NoteOfSaleState.WaitingForUser -> {
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
                            is NoteOfSaleState.Success -> {
                                val noteOfSale =
                                    (noteOfSaleState as NoteOfSaleState.Success).data
                                NoteOfSaleContent(
                                    noteOfSale = noteOfSale,
                                    onNoteOfSaleClick = { n ->
                                        navController.navigate("noteOfSale_detail/${n.id}")
                                    },
                                    onNewNoteOfSale = { navController.navigate(Screen.NewNoteOfSale.route) }
                                )
                            }
                            is NoteOfSaleState.Error -> {
                                ErrorMessage(
                                    message = (noteOfSaleState as NoteOfSaleState.Error).message,
                                    onRetry = { userData?.id?.let { userId ->
                                        viewModel.loadNoteOfSale(selectedDate, userId)
                                    } ?: run {
                                        NoteOfSaleState.WaitingForUser
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
fun NoteOfSaleContent(
    noteOfSale: List<IOperation>,
    onNoteOfSaleClick: (IOperation) -> Unit,
    onNewNoteOfSale: () -> Unit
) {
    val noteOfSaleCount = noteOfSale.count { it.documentTypeReadable == "NOTA DE AJUSTE DE OPERACIONES" }

    // Calculate monetary totals
    val noteOfSaleAmountTotal = noteOfSale
        .filter { it.documentTypeReadable == "NOTA DE AJUSTE DE OPERACIONES" }
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
            if (noteOfSale.isEmpty()) {
                EmptyState(message = "No hay notas de salida para esta fecha")
            } else {
                // Wrap the InvoiceList in a Box to ensure scrolling works
                NoteOfSaleList(
                    noteOfSale = noteOfSale,
                    onNoteOfSaleClick = onNoteOfSaleClick
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
            ActionButtonsNoteOfSale(
                onNewNoteOfSale = onNewNoteOfSale,
                noteOfSaleCount = noteOfSaleCount,
                noteOfSaleAmountTotal = noteOfSaleAmountTotal,
            )
        }
    }
}

@Composable
fun NoteOfSaleList(
    noteOfSale: List<IOperation>,
    onNoteOfSaleClick: (IOperation) -> Unit
) {
    // Removed fillMaxSize to allow proper scrolling
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(noteOfSale) { note ->
            NoteOfSaleItem(
                noteOfSale = note,
                onClick = { onNoteOfSaleClick(note) }
            )
        }
    }
}

@Composable
fun NoteOfSaleItem(
    noteOfSale: IOperation,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val isAnulado = noteOfSale.operationStatus.replace("A_", "") == "06" || noteOfSale.operationStatus.replace("A_", "") == "04"
    var showPdfDialog by remember { mutableStateOf(false) }
    // Show PDF Dialog
    if (showPdfDialog) {
        NoteOfSalePdfDialog(
            isVisible = true,
            noteOfSale = noteOfSale,
            onDismiss = { showPdfDialog = false }
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
                    text = "${noteOfSale.serial}-${noteOfSale.correlative}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        brush = ColorGradients.orangeFire
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = noteOfSale.emitDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            noteOfSale.client.names?.let {
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
                    label = "NOTA DE SALIDA",//quotation.documentTypeReadable,
                    gradient = when (noteOfSale.documentTypeReadable) {
                        "NOTA DE SALIDA" -> ColorGradients.blueOcean
                        else -> ColorGradients.greenNature
                    }
                )

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
                    text = "S/. ${String.format("%.2f", noteOfSale.totalToPay)}",
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
fun ActionButtonsNoteOfSale(
    onNewNoteOfSale: () -> Unit,
    noteOfSaleCount: Int,
    noteOfSaleAmountTotal: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Botón Nueva Nota de Salida - Versión mejorada
        ElevatedButton(
            onClick = onNewNoteOfSale,
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
                        text = "Total: $noteOfSaleCount",
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
                        text = "S/. ${String.format("%.2f", noteOfSaleAmountTotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Nueva Nota",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Text(
                    text = "Nueva Nota",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}