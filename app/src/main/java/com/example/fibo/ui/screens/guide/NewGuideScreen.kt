package com.example.fibo.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.viewmodels.GuideViewModel
import com.example.fibo.ui.components.LoadingDialog
import com.example.fibo.ui.components.ErrorDialog
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.ITariff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGuideScreen(
    navController: NavController,
    viewModel: GuideViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val guideState by viewModel.guideState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()

    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            showError = true
            errorMessage = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Guía de Remisión") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Sección de Cliente
            ClientSection(
                clientId = guideState.clientId,
                onClientSelected = { viewModel.updateField("clientId", it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Documento
            DocumentSection(
                serial = guideState.serial,
                correlative = guideState.correlative,
                onSerialChanged = { viewModel.updateField("serial", it) },
                onCorrelativeChanged = { viewModel.updateField("correlative", it.toIntOrNull() ?: 0) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Productos
            ProductsSection(
                details = guideState.operationDetailSet,
                onAddProduct = { viewModel.addProduct(it) },
                onRemoveProduct = { viewModel.removeProduct(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Transporte
            TransportSection(
                transportData = guideState,
                onTransportDataChanged = { field, value -> viewModel.updateField(field, value) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Conductor
            DriverSection(
                driverData = guideState,
                onDriverDataChanged = { field, value -> viewModel.updateField(field, value) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Destinatario
            ReceiverSection(
                receiverData = guideState,
                onReceiverDataChanged = { field, value -> viewModel.updateField(field, value) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sección de Direcciones
            AddressSection(
                addressData = guideState,
                onAddressDataChanged = { field, value -> viewModel.updateField(field, value) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Guardar
            Button(
                onClick = { viewModel.saveGuide() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar Guía")
            }
        }

        // Diálogo de carga
        if (isSaving) {
            LoadingDialog()
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
    }
}

@Composable
private fun ClientSection(
    clientId: Int,
    onClientSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Datos del Cliente",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // TODO: Implementar búsqueda de cliente con SearchBar
            OutlinedTextField(
                value = clientId.toString(),
                onValueChange = { /* TODO */ },
                label = { Text("Buscar Cliente") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false // Temporalmente deshabilitado
            )
        }
    }
}

@Composable
private fun DocumentSection(
    serial: String,
    correlative: Int,
    onSerialChanged: (String) -> Unit,
    onCorrelativeChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Datos del Documento",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = serial,
                onValueChange = onSerialChanged,
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = correlative.toString(),
                onValueChange = onCorrelativeChanged,
                label = { Text("Correlativo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
private fun ProductsSection(
    details: List<IOperationDetail>,
    onAddProduct: (IOperationDetail) -> Unit,
    onRemoveProduct: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productos",
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(onClick = {
                    // Crear un nuevo IOperationDetail con valores por defecto
                    val newDetail = IOperationDetail(
                        id = 0,
                        tariff = ITariff(
                            productId = 0,
                            productCode = "",
                            productName = "",
                            unitId = 0,
                            unitName = "",
                            stock = 0.0,
                            priceWithIgv = 0.0,
                            priceWithoutIgv = 0.0,
                            productTariffId = 0,
                            typeAffectationId = 0
                        ),
                        quantity = 0.0,
                        description = "",
                        typeAffectationId = 0,
                        unitValue = 0.0,
                        unitPrice = 0.0,
                        discountPercentage = 0.0,
                        totalDiscount = 0.0,
                        perceptionPercentage = 0.0,
                        totalPerception = 0.0,
                        igvPercentage = 0.0,
                        totalValue = 0.0,
                        totalIgv = 0.0,
                        totalAmount = 0.0,
                        totalToPay = 0.0
                    )
                    onAddProduct(newDetail)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (details.isEmpty()) {
                Text(
                    text = "No hay productos agregados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                details.forEachIndexed { index, detail ->
                    ProductItem(
                        detail = detail,
                        onRemove = { onRemoveProduct(index) }
                    )
                    if (index < details.size - 1) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductItem(
    detail: IOperationDetail,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.tariff.productName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Cantidad: ${detail.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (detail.description.isNotEmpty()) {
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar Producto")
        }
    }
}

@Composable
private fun TransportSection(
    transportData: GuideViewModel.GuideState,
    onTransportDataChanged: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Datos del Transporte",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tipo de documento de la empresa de transporte
            OutlinedTextField(
                value = transportData.transportationCompanyDocumentType,
                onValueChange = { onTransportDataChanged("transportationCompanyDocumentType", it) },
                label = { Text("Tipo de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Número de documento de la empresa de transporte
            OutlinedTextField(
                value = transportData.transportationCompanyDocumentNumber,
                onValueChange = { onTransportDataChanged("transportationCompanyDocumentNumber", it) },
                label = { Text("Número de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Nombre de la empresa de transporte
            OutlinedTextField(
                value = transportData.transportationCompanyNames,
                onValueChange = { onTransportDataChanged("transportationCompanyNames", it) },
                label = { Text("Nombre de la Empresa") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Número de registro MTC
            OutlinedTextField(
                value = transportData.transportationCompanyMtcRegistrationNumber,
                onValueChange = { onTransportDataChanged("transportationCompanyMtcRegistrationNumber", it) },
                label = { Text("Número de Registro MTC") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Placa del vehículo principal
            OutlinedTextField(
                value = transportData.mainVehicleLicensePlate,
                onValueChange = { onTransportDataChanged("mainVehicleLicensePlate", it) },
                label = { Text("Placa del Vehículo") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // TODO: Implementar lista de vehículos adicionales
        }
    }
}

@Composable
private fun DriverSection(
    driverData: GuideViewModel.GuideState,
    onDriverDataChanged: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Datos del Conductor",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tipo de documento del conductor
            OutlinedTextField(
                value = driverData.mainDriverDocumentType,
                onValueChange = { onDriverDataChanged("mainDriverDocumentType", it) },
                label = { Text("Tipo de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Número de documento del conductor
            OutlinedTextField(
                value = driverData.mainDriverDocumentNumber,
                onValueChange = { onDriverDataChanged("mainDriverDocumentNumber", it) },
                label = { Text("Número de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Nombre del conductor
            OutlinedTextField(
                value = driverData.mainDriverNames,
                onValueChange = { onDriverDataChanged("mainDriverNames", it) },
                label = { Text("Nombre del Conductor") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Licencia de conducir
            OutlinedTextField(
                value = driverData.mainDriverDriverLicense,
                onValueChange = { onDriverDataChanged("mainDriverDriverLicense", it) },
                label = { Text("Licencia de Conducir") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // TODO: Implementar lista de conductores adicionales
        }
    }
}

@Composable
private fun ReceiverSection(
    receiverData: GuideViewModel.GuideState,
    onReceiverDataChanged: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Datos del Destinatario",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tipo de documento del destinatario
            OutlinedTextField(
                value = receiverData.receiverDocumentType,
                onValueChange = { onReceiverDataChanged("receiverDocumentType", it) },
                label = { Text("Tipo de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Número de documento del destinatario
            OutlinedTextField(
                value = receiverData.receiverDocumentNumber,
                onValueChange = { onReceiverDataChanged("receiverDocumentNumber", it) },
                label = { Text("Número de Documento") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Nombre del destinatario
            OutlinedTextField(
                value = receiverData.receiverNames,
                onValueChange = { onReceiverDataChanged("receiverNames", it) },
                label = { Text("Nombre del Destinatario") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AddressSection(
    addressData: GuideViewModel.GuideState,
    onAddressDataChanged: (String, Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Direcciones",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dirección de Origen
            Text(
                text = "Punto de Partida",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideOriginDistrictId,
                onValueChange = { onAddressDataChanged("guideOriginDistrictId", it) },
                label = { Text("Distrito") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideOriginAddress,
                onValueChange = { onAddressDataChanged("guideOriginAddress", it) },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideOriginSerial,
                onValueChange = { onAddressDataChanged("guideOriginSerial", it) },
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dirección de Destino
            Text(
                text = "Punto de Llegada",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideArrivalDistrictId,
                onValueChange = { onAddressDataChanged("guideArrivalDistrictId", it) },
                label = { Text("Distrito") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideArrivalAddress,
                onValueChange = { onAddressDataChanged("guideArrivalAddress", it) },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = addressData.guideArrivalSerial,
                onValueChange = { onAddressDataChanged("guideArrivalSerial", it) },
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de observaciones
            OutlinedTextField(
                value = addressData.observation,
                onValueChange = { onAddressDataChanged("observation", it) },
                label = { Text("Observaciones") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}