package com.example.fibo.ui.screens.person

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.ISubsidiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPersonScreen(
    navController: NavController,
    subsidiaryData: ISubsidiary?,
    viewModel: NewPersonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.personResult) {
        uiState.personResult?.let { result ->
            result.onSuccess { message ->
                snackbarHostState.showSnackbar(message)
                // Enviar resultado de vuelta a PersonScreen
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "person_created", true
                )
                navController.popBackStack()
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    "Error al crear persona: ${error.message}"
                )
            }
            viewModel.resetPersonResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Registrar Persona") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TIPO DE DOCUMENTO
            item {
                Text(
                    text = "Tipo de Documento *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DocumentTypeSelector(
                    selectedType = uiState.documentType,
                    onTypeSelected = { viewModel.onDocumentTypeChanged(it) },
                    isError = uiState.documentTypeError.isNotEmpty()
                )
                if (uiState.documentTypeError.isNotEmpty()) {
                    Text(
                        text = uiState.documentTypeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }

            // NÚMERO DE DOCUMENTO CON BOTÓN EXTRAER
            item {
                Text(
                    text = "Número de Documento *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DocumentNumberInput(
                    documentNumber = uiState.documentNumber,
                    documentType = uiState.documentType,
                    onDocumentNumberChanged = { viewModel.onDocumentNumberChanged(it) },
                    onExtractClick = { viewModel.extractPersonData() },
                    isLoading = uiState.isExtracting,
                    isError = uiState.documentNumberError.isNotEmpty()
                )
                if (uiState.documentNumberError.isNotEmpty()) {
                    Text(
                        text = uiState.documentNumberError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // NOMBRES O RAZÓN SOCIAL
            item {
                OutlinedTextField(
                    value = uiState.names,
                    onValueChange = { viewModel.onNamesChanged(it) },
                    label = { Text("Nombres o Razón Social *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.namesError.isNotEmpty(),
                    singleLine = true
                )
                if (uiState.namesError.isNotEmpty()) {
                    Text(
                        text = uiState.namesError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // RAZÓN COMERCIAL (OPCIONAL)
            item {
                OutlinedTextField(
                    value = uiState.shortName,
                    onValueChange = { viewModel.onShortNameChanged(it) },
                    label = { Text("Razón comercial (opcional)") },
                    placeholder = { Text("Marca o nombre comercial") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // DIRECCIÓN FISCAL
            item {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = { viewModel.onAddressChanged(it) },
                    label = { Text("Dirección fiscal *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.addressError.isNotEmpty(),
                    singleLine = true
                )
                if (uiState.addressError.isNotEmpty()) {
                    Text(
                        text = uiState.addressError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // EMAIL Y TELÉFONO EN FILA
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text("Email(opcional)") },
                        placeholder = { Text("ejemplo@email.com") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = { viewModel.onPhoneChanged(it) },
                        label = { Text("Teléfono") },
                        placeholder = { Text("opcional") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // CÓDIGO DEL CLIENTE (OPCIONAL)
            item {
                OutlinedTextField(
                    value = uiState.code,
                    onValueChange = { viewModel.onCodeChanged(it) },
                    label = { Text("Código del cliente (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // LICENCIA DE CONDUCIR (SOLO SI ES CONDUCTOR)
            if (uiState.isDriver) {
                item {
                    OutlinedTextField(
                        value = uiState.driverLicense,
                        onValueChange = { viewModel.onDriverLicenseChanged(it) },
                        label = { Text("Licencia de Conducir *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.driverLicenseError.isNotEmpty(), // Agregar validación de error
                        singleLine = true
                    )
                    if (uiState.driverLicenseError.isNotEmpty()) {
                        Text(
                            text = uiState.driverLicenseError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // TIPO DE PERSONA - CHECKBOXES
            item {
                Text(
                    text = "Tipo de Persona *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                PersonTypeCheckboxes(
                    isClient = uiState.isClient,
                    isSupplier = uiState.isSupplier,
                    isDriver = uiState.isDriver,
                    onClientChanged = { viewModel.onIsClientChanged(it) },
                    onSupplierChanged = { viewModel.onIsSupplierChanged(it) },
                    onDriverChanged = { viewModel.onIsDriverChanged(it) }
                )
            }

            // ACTIVO
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.isEnabled,
                        onCheckedChange = { viewModel.onIsEnabledChanged(it) }
                    )
                    Text(
                        text = "Activo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // BOTONES
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.createPerson(subsidiaryData?.id) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.isFormValid && !uiState.isLoading, // Se deshabilita cuando está cargando
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creando...") // Texto que indica que está procesando
                        } else {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crear Persona")
                        }
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading // También deshabilitar el botón cancelar durante la creación
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    isError: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val documentTypes = listOf(
        "1" to "DNI",
        "6" to "RUC",
        "4" to "CARNET DE EXTRANJERIA",
        "7" to "PASAPORTE",
        "A" to "CED.DIPLOMÁTICA DE IDENTIDAD",
        "B" to "DOCUMENTO IDENTIDAD PAÍS RESIDENCIA",
        "C" to "TAX IDENTIFICACIÓN NUMBER - TIN",
        "D" to "IDENTIFICATION NUMBER - IN",
        "E" to "TAM - TARJETA ANDINA DE MIGRACIÓN",
        "F" to "PERMISO TEMPORAL DE PERMANENCIA PTP",
        "-" to "VARIOS - VENTAS MENORES A S/.700.00"
    )

    val selectedLabel = documentTypes.find { it.first == selectedType }?.second ?: "Seleccionar tipo"

    OutlinedTextField(
        value = selectedLabel,
        onValueChange = {},
        label = { Text("Tipo de documento") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        isError = isError
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        documentTypes.forEach { (value, label) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onTypeSelected(value)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun DocumentNumberInput(
    documentNumber: String,
    documentType: String,
    onDocumentNumberChanged: (String) -> Unit,
    onExtractClick: () -> Unit,
    isLoading: Boolean,
    isError: Boolean
) {
    val isExtractVisible = documentType in listOf("1", "6") // Solo DNI y RUC
    val maxLength = when (documentType) {
        "1" -> 8  // DNI: 8 dígitos
        "6" -> 11 // RUC: 11 dígitos
        else -> 20 // Otros documentos
    }

    val placeholder = when (documentType) {
        "1" -> "8 dígitos"
        "6" -> "11 dígitos"
        else -> "Número de documento"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = documentNumber,
            onValueChange = {
                if (it.length <= maxLength) {
                    onDocumentNumberChanged(it)
                }
            },
            label = { Text(placeholder) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            isError = isError,
            singleLine = true
        )

        if (isExtractVisible) {
            Button(
                onClick = onExtractClick,
                enabled = documentNumber.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("EXTRAER")
                }
            }
        }
    }
}

@Composable
fun PersonTypeCheckboxes(
    isClient: Boolean,
    isSupplier: Boolean,
    isDriver: Boolean,
    onClientChanged: (Boolean) -> Unit,
    onSupplierChanged: (Boolean) -> Unit,
    onDriverChanged: (Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isClient,
                onCheckedChange = onClientChanged
            )
            Text("Cliente")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSupplier,
                onCheckedChange = onSupplierChanged
            )
            Text("Proveedor")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isDriver,
                onCheckedChange = onDriverChanged
            )
            Text("Conductor")
        }
    }
}