package com.example.fibo.ui.screens.person

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
                title = { Text("Nueva Persona") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
            item {
                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.names,
                    onValueChange = { viewModel.onNamesChanged(it) },
                    label = { Text("Nombres Completos *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.namesError.isNotEmpty()
                )
                if (uiState.namesError.isNotEmpty()) {
                    Text(
                        text = uiState.namesError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.shortName,
                    onValueChange = { viewModel.onShortNameChanged(it) },
                    label = { Text("Nombre Corto *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.shortNameError.isNotEmpty()
                )
                if (uiState.shortNameError.isNotEmpty()) {
                    Text(
                        text = uiState.shortNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.code,
                    onValueChange = { viewModel.onCodeChanged(it) },
                    label = { Text("Código") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Documento de Identidad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.documentType,
                        onValueChange = { viewModel.onDocumentTypeChanged(it) },
                        label = { Text("Tipo *") },
                        modifier = Modifier.weight(1f),
                        isError = uiState.documentTypeError.isNotEmpty()
                    )
                    OutlinedTextField(
                        value = uiState.documentNumber,
                        onValueChange = { viewModel.onDocumentNumberChanged(it) },
                        label = { Text("Número *") },
                        modifier = Modifier.weight(2f),
                        isError = uiState.documentNumberError.isNotEmpty()
                    )
                }
            }

            item {
                Text(
                    text = "Información de Contacto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { viewModel.onPhoneChanged(it) },
                    label = { Text("Teléfono *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.phoneError.isNotEmpty()
                )
                if (uiState.phoneError.isNotEmpty()) {
                    Text(
                        text = uiState.phoneError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.emailError.isNotEmpty()
                )
                if (uiState.emailError.isNotEmpty()) {
                    Text(
                        text = uiState.emailError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = { viewModel.onAddressChanged(it) },
                    label = { Text("Dirección *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.addressError.isNotEmpty()
                )
                if (uiState.addressError.isNotEmpty()) {
                    Text(
                        text = uiState.addressError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                Text(
                    text = "Tipo de Persona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isClient,
                            onCheckedChange = { viewModel.onIsClientChanged(it) }
                        )
                        Text("Cliente")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = uiState.isSupplier,
                            onCheckedChange = { viewModel.onIsSupplierChanged(it) }
                        )
                        Text("Proveedor")
                    }
                }
            }

            item {
                if (uiState.isClient || uiState.isSupplier) {
                    OutlinedTextField(
                        value = uiState.economicActivityMain.toString(),
                        onValueChange = { viewModel.onEconomicActivityMainChanged(it.toIntOrNull() ?: 0) },
                        label = { Text("Actividad Económica Principal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                if (uiState.isClient) {
                    OutlinedTextField(
                        value = uiState.driverLicense,
                        onValueChange = { viewModel.onDriverLicenseChanged(it) },
                        label = { Text("Licencia de Conducir") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.createPerson(subsidiaryData?.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isFormValid && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear Persona")
                    }
                }
            }
        }
    }
}