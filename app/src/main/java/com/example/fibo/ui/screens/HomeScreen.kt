package com.example.fibo.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.viewmodels.HomeViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.fibo.R
import com.example.fibo.model.IOperation
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppTopBar
import com.example.fibo.ui.components.SideMenu


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

    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryData,
        onMenuItemSelected = { option ->
            when (option) {
                "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
                "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
//                "Historial" -> navController.navigate(Screen.History.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "Inicio",
                        onMenuClick = { isMenuOpen = !isMenuOpen },
                        onDateSelected = { date ->
                            homeViewModel.updateSelectedDate(date)
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
                        when (invoiceState) {
                            is HomeViewModel.InvoiceState.Loading -> CenterLoadingIndicator()
                            is HomeViewModel.InvoiceState.Success -> {
                                val invoices = (invoiceState as HomeViewModel.InvoiceState.Success).data
                                InvoiceContent(
                                    invoices = invoices,
                                    onInvoiceClick = { invoice ->
                                        navController.navigate("invoice_detail/${invoice.id}")
                                    },
                                    onNewInvoice = { navController.navigate(Screen.NewInvoice.route) },
                                    onNewReceipt = { navController.navigate(Screen.NewReceipt.route) }
                                )
                            }
                            is HomeViewModel.InvoiceState.Error -> {
                                ErrorMessage(
                                    message = (invoiceState as HomeViewModel.InvoiceState.Error).message,
                                    onRetry = { homeViewModel.loadInvoices(selectedDate) }
                                )
                            }
                        }
                    }
                }
            )
        }
    )
}
//@Composable
//fun HomeScreen(
//    navController: NavController,
//    homeViewModel: HomeViewModel = hiltViewModel(),
//    onLogout: () -> Unit
//) {
//    val subsidiaryData by homeViewModel.subsidiaryData.collectAsState()
//    val selectedDate by homeViewModel.selectedDate.collectAsState()
//    val invoiceState by homeViewModel.invoiceState.collectAsState()
//    var isMenuOpen by remember { mutableStateOf(false) }
//
//    //    // Función para alternar el menú
//    fun toggleMenu() {
//        isMenuOpen = !isMenuOpen
//    }
//
//    Scaffold(
//        topBar = {
//            AppTopBar(
//                title = "Inicio",
//                onMenuClick = { toggleMenu() },
//                onDateSelected = { date ->
//                    homeViewModel.updateSelectedDate(date)
//                },
//                currentDate = selectedDate
//            )
//        },
//        content = { paddingValues ->
////            Box(
////                modifier = Modifier
////                    .fillMaxSize()
////                    .padding(paddingValues)
////            ) {
////                when (invoiceState) {
////                    is HomeViewModel.InvoiceState.Loading -> {
////                        CenterLoadingIndicator()
////                    }
////                    is HomeViewModel.InvoiceState.Success -> {
////                        val invoices = (invoiceState as HomeViewModel.InvoiceState.Success).data
////                        InvoiceContent(
////                            invoices = invoices,
////                            onInvoiceClick = { invoice ->
////                                // Navegar al detalle
////                                navController.navigate("invoice_detail/${invoice.id}")
////                            },
////                            onNewInvoice = { navController.navigate(Screen.NewInvoice.route) },
////                            onNewReceipt = { navController.navigate(Screen.NewReceipt.route) }
////                        )
////                    }
////                    is HomeViewModel.InvoiceState.Error -> {
////                        ErrorMessage(
////                            message = (invoiceState as HomeViewModel.InvoiceState.Error).message,
////                            onRetry = { homeViewModel.loadInvoices(selectedDate) }
////                        )
////                    }
////                }
//
//                SideMenu(
//                    isOpen = isMenuOpen,
//                    subsidiaryData = subsidiaryData,
//                    onMenuItemSelected = { option ->
//                        when (option) {
//                            "Nueva Factura" -> navController.navigate(Screen.NewInvoice.route)
//                            "Nueva Boleta" -> navController.navigate(Screen.NewReceipt.route)
//                        }
//                        isMenuOpen = false
//                    },
//                    onLogout = onLogout,
//                    content = {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .padding(paddingValues)
//                                .background(MaterialTheme.colorScheme.background) // Fondo por defecto
//                        ) {
//                            when (invoiceState) {
//                                is HomeViewModel.InvoiceState.Loading -> CenterLoadingIndicator()
//                                is HomeViewModel.InvoiceState.Success -> {
//                                    val invoices = (invoiceState as HomeViewModel.InvoiceState.Success).data
//                                    InvoiceContent(
//                                        invoices = invoices,
//                                        onInvoiceClick = { invoice ->
//                                            navController.navigate("invoice_detail/${invoice.id}")
//                                        },
//                                        onNewInvoice = { navController.navigate(Screen.NewInvoice.route) },
//                                        onNewReceipt = { navController.navigate(Screen.NewReceipt.route) }
//                                    )
//                                }
//                                is HomeViewModel.InvoiceState.Error -> {
//                                    ErrorMessage(
//                                        message = (invoiceState as HomeViewModel.InvoiceState.Error).message,
//                                        onRetry = { homeViewModel.loadInvoices(selectedDate) }
//                                    )
//                                }
//                            }
//                        }
//                    }
//                )
////            }
//        }
//    )
//}
@Composable
fun InvoiceContent(
    invoices: List<IOperation>,
    onInvoiceClick: (IOperation) -> Unit,
    onNewInvoice: () -> Unit,
    onNewReceipt: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Sección de Listado (70% del espacio)
        Box(
            modifier = Modifier
                .weight(0.85f)
                .fillMaxWidth()
        ) {
            if (invoices.isEmpty()) {
                EmptyState(message = "No hay comprobantes para esta fecha")
            } else {
                // Wrap the InvoiceList in a Box to ensure scrolling works
                InvoiceList(
                    invoices = invoices,
                    onInvoiceClick = onInvoiceClick
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sección de Botones (30% del espacio)
        Column(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom
        ) {
            ActionButtons(
                onNewInvoice = onNewInvoice,
                onNewReceipt = onNewReceipt
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Added a border for better styling
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.emitDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            invoice.client.names?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
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
                    color = when (invoice.documentTypeReadable) {
                        "FACTURA" -> MaterialTheme.colorScheme.tertiary
                        "BOLETA" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_whasap),
                    contentDescription = "WHASAP",
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.is_pdf),
                    contentDescription = "PDF",
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "S/. ${invoice.totalToPay}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
@Composable
fun ActionButtons(
    onNewInvoice: () -> Unit,
    onNewReceipt: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FilledTonalButton(
            onClick = onNewInvoice,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nueva Factura")
        }

        FilledTonalButton(
            onClick = onNewReceipt,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nueva Boleta")
        }
    }
}

@Composable
fun Chip(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium
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
