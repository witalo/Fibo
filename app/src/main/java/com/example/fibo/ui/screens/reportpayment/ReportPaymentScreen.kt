package com.example.fibo.ui.screens.reportpayment

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.*
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ReportPaymentScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: ReportPaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val subsidiaryData by viewModel.preferencesManager.subsidiaryData.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isMenuOpen by remember { mutableStateOf(false) }

    // Manejar la descarga del Excel
    LaunchedEffect(exportState) {
        val currentState = exportState
        when (currentState) {
            is ExportState.Success -> {
                downloadExcelFile(
                    context = context,
                    url = currentState.downloadUrl,
                    fileName = currentState.fileName
                )
                viewModel.resetExportState()
            }
            is ExportState.Error -> {
                // Opcional: mostrar un toast o snackbar con el error
            }
            else -> {}
        }
    }

    SideMenu(
        isOpen = isMenuOpen,
        onClose = { isMenuOpen = false },
        subsidiaryData = subsidiaryData,
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
                "Reporte pagos" -> navController.navigate(Screen.ReportPayment.route)
            }
            isMenuOpen = false
        },
        onLogout = onLogout,
        content = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = "Reporte de Pagos",
                        onMenuClick = { isMenuOpen = !isMenuOpen },
                        actions = {
                            // Botón de recargar
                            IconButton(
                                onClick = { viewModel.loadPaymentReport() }
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Recargar",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            )
                        )
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sección de filtros
                            item {
                                FilterSection(
                                    startDate = startDate,
                                    endDate = endDate,
                                    onStartDateChange = viewModel::updateStartDate,
                                    onEndDateChange = viewModel::updateEndDate,
                                    onExport = viewModel::exportToExcel,
                                    isExporting = exportState is ExportState.Loading
                                )
                            }

                            // Total general y resumen
                            item {
                                TotalSummaryCard(
                                    totalAmount = uiState.totalAmount,
                                    transactionCount = uiState.salesWithPayments.size
                                )
                            }

                            // Gráfico circular y resumen por método de pago
                            if (uiState.chartData.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Gráfico circular
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        ) {
                                            PaymentPieChart(
                                                data = uiState.chartData,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Lista de métodos de pago
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    "Métodos de Pago",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                uiState.paymentSummary.take(5).forEach { item ->
                                                    PaymentMethodItem(item = item)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Tabla de ventas con pagos
                            item {
                                PaymentTable(
                                    sales = uiState.salesWithPayments,
                                    availablePaymentMethods = uiState.availablePaymentMethods
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun FilterSection(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onExport: () -> Unit,
    isExporting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Selector de fecha inicio
                DatePickerField(
                    label = "Fecha Inicio",
                    date = startDate,
                    onDateChange = onStartDateChange,
                    modifier = Modifier.weight(1f)
                )

                // Selector de fecha fin
                DatePickerField(
                    label = "Fecha Fin",
                    date = endDate,
                    onDateChange = onEndDateChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // Botón de exportar
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar a Excel")
                }
            }
        }
    }
}

@Composable
private fun TotalSummaryCard(
    totalAmount: Double,
    transactionCount: Int
) {
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total de Ventas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    numberFormat.format(totalAmount),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Mostrando $transactionCount transacciones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PaymentPieChart(
    data: List<ChartDataItem>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f * 0.8f

        var startAngle = -90f
        val total = data.sumOf { it.value.toDouble() }.toFloat()

        data.forEach { item ->
            val sweepAngle = (item.value / total) * 360f * animationProgress.value

            drawArc(
                color = Color(item.color),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun PaymentMethodItem(item: PaymentSummaryItem) {
    val numberFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "PE")).apply {
            maximumFractionDigits = 2
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Primera fila: Icono y método de pago
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.paymentMethod.icon,
                fontSize = 24.sp,
                modifier = Modifier.width(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.paymentMethod.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Cant: ${item.transactionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Segunda fila: Monto y porcentaje
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = numberFormat.format(item.totalAmount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${String.format("%.1f", item.percentage)}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PaymentTable(
    sales: List<SaleWithPayments>,
    availablePaymentMethods: List<PaymentMethod>
) {
    val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Encabezado
            Text(
                "Detalle de Pagos por Venta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            // Tabla con scroll horizontal
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    Column {
                        // Encabezados de tabla
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(vertical = 12.dp)
                        ) {
                            TableHeaderCell("TIPO", 80.dp)
                            TableHeaderCell("SERIE", 120.dp)
                            TableHeaderCell("FECHA", 100.dp)
                            TableHeaderCell("CLIENTE", 150.dp)
                            TableHeaderCell("TOTAL", 100.dp)

                            // Columnas dinámicas para cada método de pago
                            availablePaymentMethods.forEach { method ->
                                TableHeaderCell(method.name, 120.dp)
                            }
                        }

                        // Filas de datos
                        sales.forEach { sale ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        BorderStroke(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                    .padding(vertical = 8.dp)
                            ) {
                                TableCell(
                                    text = getDocumentTypeLabel(sale.documentType),
                                    width = 80.dp,
                                    color = getDocumentTypeColor(sale.documentType)
                                )
                                TableCell("${sale.serial}-${sale.correlative}", 120.dp)
                                TableCell(formatDate(sale.emitDate), 100.dp)
                                TableCell(sale.clientName, 150.dp)
                                TableCell(
                                    numberFormat.format(sale.totalAmount),
                                    100.dp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Valores para cada método de pago
                                availablePaymentMethods.forEach { method ->
                                    val amount = when(method.id) {
                                        1 -> sale.totalCash
                                        2 -> sale.totalDebitCard
                                        3 -> sale.totalCreditCard
                                        4 -> sale.totalTransfer
                                        5 -> sale.totalMonue
                                        6 -> sale.totalCheck
                                        7 -> sale.totalCoupon
                                        8 -> sale.totalYape
                                        9 -> sale.totalDue
                                        10 -> sale.totalOther
                                        else -> 0.0
                                    }

                                    TableCell(
                                        if (amount > 0) numberFormat.format(amount) else "-",
                                        120.dp,
                                        color = if (amount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = fontWeight,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    OutlinedTextField(
        value = date.format(dateFormatter),
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
            }
        },
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { selectedDate ->
                onDateChange(selectedDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            initialDate = date
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerDialog(
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    initialDate: LocalDate
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(initialDate.withDayOfMonth(1)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar fecha",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Navegación de mes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Mes anterior"
                        )
                    }

                    Text(
                        text = displayedMonth.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
                        ).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) }
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Mes siguiente"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Días de la semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("D", "L", "M", "X", "J", "V", "S").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendario
                val firstDayOfMonth = displayedMonth.dayOfWeek.value % 7
                val daysInMonth = displayedMonth.lengthOfMonth()
                val totalCells = firstDayOfMonth + daysInMonth
                val weeks = (totalCells + 6) / 7

                Column {
                    repeat(weeks) { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(7) { dayOfWeek ->
                                val cellIndex = week * 7 + dayOfWeek
                                val dayNumber = cellIndex - firstDayOfMonth + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = displayedMonth.withDayOfMonth(dayNumber)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == LocalDate.now()

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable { selectedDate = cellDate },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fecha seleccionada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Fecha seleccionada: ${
                            selectedDate.format(
                                DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(selectedDate)
                    onDismiss()
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Funciones auxiliares
private fun getDocumentTypeLabel(type: String): String {
    return when (type) {
        "01" -> "FACTURA"
        "03" -> "BOLETA"
        "NS" -> "NOTA"
        else -> type
    }
}

private fun getDocumentTypeColor(type: String): Color {
    return when (type) {
        "01" -> Color(0xFF2196F3) // Azul para factura
        "03" -> Color(0xFF4CAF50) // Verde para boleta
        "NS" -> Color(0xFFFF9800) // Naranja para nota
        else -> Color.Gray
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

private fun downloadExcelFile(context: Context, url: String, fileName: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle("Descargando reporte")
        .setDescription(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}

// Componente personalizado para el TopBar con gradiente
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onMenuClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    )
                )
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}