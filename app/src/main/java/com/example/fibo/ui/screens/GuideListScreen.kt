package com.example.fibo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.viewmodels.GuideListViewModel
import com.example.fibo.model.IGuide
import com.example.fibo.ui.components.ErrorDialog
import com.example.fibo.ui.components.LoadingDialog
import java.text.SimpleDateFormat
import java.util.*
import com.example.fibo.ui.screens.guide.GuidePdfPreviewDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideListScreen(
    navController: NavController,
    viewModel: GuideListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val guides by viewModel.guides.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val documentType by viewModel.documentType.collectAsState()

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Estados para el diálogo PDF
    var showPdfDialog by remember { mutableStateOf(false) }
    var selectedGuideId by remember { mutableStateOf(0) }

    // Estado para el modal de filtros
    var showFiltersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            showError = true
            errorMessage = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Guías de Remisión") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { showFiltersDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                    }
                    IconButton(onClick = { navController.navigate("new_guide") }) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Guía")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Información de filtros activos
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Filtros activos:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Desde: $startDate hasta: $endDate",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Tipo: ${when(documentType) {
                            "09" -> "GUÍA DE REMISIÓN REMITENTE"
                            "31" -> "GUÍA DE REMISIÓN TRANSPORTISTA"
                            "NA" -> "NO APLICA"
                            else -> "NO APLICA"
                        }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Información de resultados
            Text(
                text = "Total de guías: $totalSales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista de guías
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (guides.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron guías",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(guides) { guide ->
                        GuideItem(
                            guide = guide,
                            onPdfClick = { guideId ->
                                selectedGuideId = guideId
                                showPdfDialog = true
                            }
                        )
                    }
                }

                // Paginación
                if (totalPages > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PaginationSection(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { viewModel.goToPage(it) },
                        onPreviousPage = { viewModel.previousPage() },
                        onNextPage = { viewModel.nextPage() }
                    )
                }
            }
        }

        // Diálogo de error
        if (showError) {
            ErrorDialog(
                message = errorMessage,
                onDismiss = { 
                    showError = false
                    viewModel.clearError()
                }
            )
        }

        // Diálogo de PDF
        if (showPdfDialog) {
            GuidePdfPreviewDialog(
                isVisible = showPdfDialog,
                guideId = selectedGuideId,
                onDismiss = { 
                    showPdfDialog = false
                    selectedGuideId = 0
                }
            )
        }

        // Modal de filtros
        if (showFiltersDialog) {
            FilterDialog(
                startDate = startDate,
                endDate = endDate,
                documentType = documentType,
                onStartDateChange = { viewModel.updateStartDate(it) },
                onEndDateChange = { viewModel.updateEndDate(it) },
                onDocumentTypeChange = { viewModel.updateDocumentType(it) },
                onDismiss = { showFiltersDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    startDate: String,
    endDate: String,
    documentType: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onDocumentTypeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var expandedDocumentType by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Título
                Text(
                    text = "Filtros de Búsqueda",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Fecha inicio
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { },
                    label = { Text("Fecha inicio") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Fecha fin
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { },
                    label = { Text("Fecha fin") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tipo de documento
                ExposedDropdownMenuBox(
                    expanded = expandedDocumentType,
                    onExpandedChange = { expandedDocumentType = !expandedDocumentType }
                ) {
                    OutlinedTextField(
                        value = when(documentType) {
                            "09" -> "GUÍA DE REMISIÓN REMITENTE"
                            "31" -> "GUÍA DE REMISIÓN TRANSPORTISTA"
                            "NA" -> "NO APLICA"
                            else -> "NO APLICA"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Tipo de documento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDocumentType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDocumentType,
                        onDismissRequest = { expandedDocumentType = false }
                    ) {
                        listOf(
                            "NA" to "NO APLICA",
                            "09" to "GUÍA DE REMISIÓN REMITENTE",
                            "31" to "GUÍA DE REMISIÓN TRANSPORTISTA"
                        ).forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onDocumentTypeChange(code)
                                    expandedDocumentType = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = onDismiss) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDateSelected = { date -> 
                onStartDateChange(date)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDateSelected = { date -> 
                onEndDateChange(date)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

@Composable
private fun GuideItem(
    guide: IGuide,
    onPdfClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con fecha y tipo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = guide.emitDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when(guide.documentType) {
                        "09" -> "REMITENTE"
                        "31" -> "TRANSPORTISTA"
                        else -> guide.documentType
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Serie y número
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Serie: ${guide.serial}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Núm: ${guide.correlative}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Entidad
            Text(
                text = "Entidad: ${guide.client?.names ?: "Sin cliente"}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Estado y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Enviado al cliente
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (guide.sendWhatsapp) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (guide.sendWhatsapp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "WhatsApp: ${if (guide.sendWhatsapp) "SI" else "NO"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Estado SUNAT
                    Text(
                        text = "SUNAT: ${guide.operationStatusReadable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botón PDF
                IconButton(
                    onClick = { onPdfClick(guide.id) }
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = "Ver PDF",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginationSection(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPreviousPage,
            enabled = currentPage > 1
        ) {
            Text("Anterior")
        }

        Text(
            text = "Página $currentPage de $totalPages",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onNextPage,
            enabled = currentPage < totalPages
        ) {
            Text("Siguiente")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                    onDateSelected(date)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
} 