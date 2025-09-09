package com.example.fibo.ui.screens.report

import android.annotation.SuppressLint
import android.os.Build
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.*
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.viewmodels.MonthlyReportViewModel
import java.text.NumberFormat
import java.util.*
import android.content.Context
import android.os.Environment
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast

@SuppressLint("CoroutineCreationDuringComposition")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    onLogout: () -> Unit,
    viewModel: MonthlyReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Función para exportar a Excel
    val exportToExcel = {
        if (subsidiaryData?.id != null) {
            coroutineScope.launch {
                try {
                    val excelData = viewModel.exportToExcel(subsidiaryData.id)
                    
                    if (excelData != null) {
                        // Guardar archivo en Downloads
                        val fileName = "REPORTE_MENSUAL_${System.currentTimeMillis()}.xlsx"
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)
                        
                        FileOutputStream(file).use { fos ->
                            fos.write(excelData)
                        }
                        
                        Toast.makeText(
                            context,
                            "Excel descargado en: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error al descargar el Excel",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    // Log para debug
    LaunchedEffect(Unit) {
        android.util.Log.d("MonthlyReportScreen", "Pantalla cargada correctamente")
    }

    // Cargar datos iniciales
    LaunchedEffect(uiState.selectedYear, uiState.selectedMonth) {
        if (subsidiaryData?.id != null) {
            viewModel.loadMonthlyReport(subsidiaryData.id, uiState.selectedYear, uiState.selectedMonth)
        }
    }

    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            TopAppBar(
                title = { Text("Reporte Mensual", style = MaterialTheme.typography.titleSmall) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000))
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header con fecha y exportar
                item {
                    MonthlyReportHeader(
                        selectedYear = uiState.selectedYear,
                        selectedMonth = uiState.selectedMonth,
                        onYearChange = { viewModel.updateSelectedDate(it, uiState.selectedMonth) },
                        onMonthChange = { viewModel.updateSelectedDate(uiState.selectedYear, it) },
                        onExportClick = exportToExcel
                    )
                }

                // Resumen de ventas lado a lado
                item {
                    SalesSummarySection(
                        salesData = uiState.reportData?.sales,
                        isLoading = uiState.isLoading
                    )
                }

                // Resumen de compras
                item {
                    PurchasesSummarySection(
                        purchasesData = uiState.reportData?.purchases,
                        isLoading = uiState.isLoading
                    )
                }

                // Top productos con gráfico de torta
                item {
                    TopProductsSection(
                        topProducts = uiState.reportData?.topProducts ?: emptyList(),
                        isLoading = uiState.isLoading
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyReportHeader(
    selectedYear: Int,
    selectedMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onExportClick: () -> Unit
) {
    var showMonthDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Título principal
            Text(
                text = "Resumen Mensual de Ventas y Compras",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Comparación del mes actual vs mes anterior",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Selectores de fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector de año
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Año",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = { 
                            val year = it.toIntOrNull()
                            if (year != null && year > 2000 && year < 2100) {
                                onYearChange(year)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }

                // Selector de mes
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = getMonthName(selectedMonth),
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMonthDialog = true },
                        readOnly = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White.copy(alpha = 0.8f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showMonthDialog = true }) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown, 
                                    contentDescription = "Seleccionar mes",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        },
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón exportar
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34C759)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.FileDownload, 
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Exportar a Excel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Diálogo de selección de mes
    if (showMonthDialog) {
        MonthSelectionDialog(
            selectedMonth = selectedMonth,
            onMonthSelected = { month ->
                onMonthChange(month)
                showMonthDialog = false
            },
            onDismiss = { showMonthDialog = false }
        )
    }
}

@Composable
fun SalesSummarySection(
    salesData: MonthlySalesData?,
    isLoading: Boolean
) {
    if (isLoading || salesData == null) {
        LoadingCard(title = "VENTAS (MES ACTUAL)")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ventas mes actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            SalesMonthCard(
                title = "VENTAS (MES ACTUAL)",
                documents = salesData.currentMonth.filter { it.count > 0 }, // Solo mostrar con movimiento
                totalAmount = salesData.totalCurrentMonth,
                isCurrentMonth = true
            )
        }

        // Ventas mes anterior
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF9C27B0)
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            SalesMonthCard(
                title = "VENTAS (MES ANTERIOR)",
                documents = salesData.previousMonth.filter { it.count > 0 }, // Solo mostrar con movimiento
                totalAmount = salesData.totalPreviousMonth,
                isCurrentMonth = false
            )
        }
    }
}

@Composable
fun SalesMonthCard(
    title: String,
    documents: List<DocumentTypeSummary>,
    totalAmount: Double,
    isCurrentMonth: Boolean
) {
    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de documentos
        documents.forEach { doc ->
            DocumentTypeRow(
                documentType = doc.documentTypeName,
                count = doc.count,
                amount = doc.totalAmount,
                color = when (doc.documentType) {
                    "01" -> Color(0xFF81D4FA) // Azul claro para Facturas
                    "03" -> Color(0xFF8BC34A) // Verde claro para Boletas
                    "07" -> Color(0xFFFF9800) // Naranja para Notas de Crédito
                    "08" -> Color(0xFFF44336) // Rojo para Notas de Débito
                    "NS" -> Color(0xFF9C27B0) // Púrpura para Notas de Salida
                    "NE" -> Color(0xFFFF5722) // Naranja para Notas de Entrada
                    else -> Color(0xFF607D8B) // Gris para otros
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Total
        if (documents.isNotEmpty()) {
            Divider(
                color = Color.White.copy(alpha = 0.5f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL: S/.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = String.format("%.2f", totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DocumentTypeRow(
    documentType: String,
    count: Int,
    amount: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$documentType $count",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
        
        Text(
            text = "S/. ${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PurchasesSummarySection(
    purchasesData: MonthlyPurchasesData?,
    isLoading: Boolean
) {
    if (isLoading || purchasesData == null) {
        LoadingCard(title = "COMPRAS (MES ACTUAL)")
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF34C759)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "COMPRAS (MES ACTUAL)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (purchasesData.currentMonth.isEmpty()) {
                Text(
                    text = "No hay compras registradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            } else {
                purchasesData.currentMonth.filter { it.count > 0 }.forEach { doc ->
                    DocumentTypeRow(
                        documentType = doc.documentTypeName,
                        count = doc.count,
                        amount = doc.totalAmount,
                        color = when (doc.documentType) {
                            "01" -> Color(0xFF81D4FA) // Azul claro para Facturas
                            "03" -> Color(0xFF8BC34A) // Verde claro para Boletas
                            "NE" -> Color(0xFFFF5722) // Naranja para Notas de Entrada
                            else -> Color(0xFF607D8B) // Gris para otros
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Total
            if (purchasesData.currentMonth.isNotEmpty()) {
                Divider(
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                Text(
                    text = "TOTAL: S/. ${String.format("%.2f", purchasesData.totalCurrentMonth)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TopProductsSection(
    topProducts: List<TopProduct>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Gráfico de Productos Vendidos (MES ACTUAL)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Distribución por total vendido en soles",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF007AFF)
                    )
                }
            } else if (topProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay productos vendidos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Gráfico de torta real
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Gráfico de torta
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PieChart(
                            products = topProducts,
                            modifier = Modifier.size(180.dp)
                        )
                    }
                    
                    // Leyenda
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        TopProductsLegend(products = topProducts)
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    products: List<TopProduct>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF007AFF), // Azul iOS
        Color(0xFF34C759), // Verde iOS
        Color(0xFFFF9500), // Naranja iOS
        Color(0xFFFF3B30), // Rojo iOS
        Color(0xFFAF52DE), // Púrpura iOS
        Color(0xFF5AC8FA), // Cian iOS
        Color(0xFFFF2D92), // Rosa iOS
        Color(0xFF8E8E93), // Gris iOS
        Color(0xFFFFCC00), // Amarillo iOS
        Color(0xFF32D74B)  // Verde claro iOS
    )

    val totalAmount = products.sumOf { it.totalAmount }
    if (totalAmount <= 0) return

    var startAngle = -90f

    androidx.compose.foundation.Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2f
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        products.take(10).forEachIndexed { index, product ->
            val color = colors[index % colors.size]
            val sweepAngle = ((product.totalAmount / totalAmount) * 360f).toFloat()

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - radius,
                    centerY - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 40.dp.toPx())
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
fun TopProductsLegend(products: List<TopProduct>) {
    val colors = listOf(
        Color(0xFF007AFF), // Azul iOS
        Color(0xFF34C759), // Verde iOS
        Color(0xFFFF9500), // Naranja iOS
        Color(0xFFFF3B30), // Rojo iOS
        Color(0xFFAF52DE), // Púrpura iOS
        Color(0xFF5AC8FA), // Cian iOS
        Color(0xFFFF2D92), // Rosa iOS
        Color(0xFF8E8E93), // Gris iOS
        Color(0xFFFFCC00), // Amarillo iOS
        Color(0xFF32D74B)  // Verde claro iOS
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        products.take(10).forEachIndexed { index, product ->
            val color = colors[index % colors.size]
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${String.format("%.1f", product.percentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007AFF)
                )
            }
        }
    }
}

@Composable
fun LoadingCard(title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cargando $title...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MonthSelectionDialog(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val months = listOf(
        1 to "Enero", 2 to "Febrero", 3 to "Marzo", 4 to "Abril",
        5 to "Mayo", 6 to "Junio", 7 to "Julio", 8 to "Agosto",
        9 to "Septiembre", 10 to "Octubre", 11 to "Noviembre", 12 to "Diciembre"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Mes") },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp)
            ) {
                items(months) { (monthNumber, monthName) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onMonthSelected(monthNumber) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMonth == monthNumber) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = monthName,
                            modifier = Modifier.padding(16.dp),
                            color = if (selectedMonth == monthNumber) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "Enero"
        2 -> "Febrero"
        3 -> "Marzo"
        4 -> "Abril"
        5 -> "Mayo"
        6 -> "Junio"
        7 -> "Julio"
        8 -> "Agosto"
        9 -> "Septiembre"
        10 -> "Octubre"
        11 -> "Noviembre"
        12 -> "Diciembre"
        else -> "Mes inválido"
    }
}
