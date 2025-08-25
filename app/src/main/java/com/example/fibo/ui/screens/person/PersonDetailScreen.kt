package com.example.fibo.ui.screens.person

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IPerson

// Función para convertir códigos de documento a nombres legibles
private fun getDocumentTypeName(code: String?): String {
    return when (code) {
        "1" -> "DNI"
        "6" -> "RUC"
        "4" -> "CARNET DE EXTRANJERIA"
        "7" -> "PASAPORTE"
        "A" -> "CED.DIPLOMÁTICA DE IDENTIDAD"
        "B" -> "DOCUMENTO IDENTIDAD PAÍS RESIDENCIA"
        "C" -> "TAX IDENTIFICACIÓN NUMBER - TIN"
        "D" -> "IDENTIFICATION NUMBER - IN"
        "E" -> "TAM - TARJETA ANDINA DE MIGRACIÓN"
        "F" -> "PERMISO TEMPORAL DE PERMANENCIA PTP"
        "-" -> "VARIOS - VENTAS MENORES A S/.700.00"
        else -> code ?: "No especificado"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personId: Int?,
    navController: NavController,
    viewModel: PersonDetailViewModel = hiltViewModel()
) {
    val personState by viewModel.personState.collectAsState()

    LaunchedEffect(personId) {
        personId?.let { viewModel.loadPerson(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Persona") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Implementar edición */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (personState) {
            is PersonDetailState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PersonDetailState.Success -> {
                val person = (personState as PersonDetailState.Success).person
                PersonDetailContent(
                    person = person,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is PersonDetailState.Error -> {
                val errorMessage = (personState as PersonDetailState.Error).message
                ErrorContent(
                    message = errorMessage,
                    onRetry = { personId?.let { viewModel.loadPerson(it) } },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun PersonDetailContent(
    person: IPerson,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con información principal
        PersonHeader(person = person)
        
        // Información de contacto
        InfoSection(
            title = "Información de Contacto",
            icon = Icons.Default.ContactPhone,
            content = {
                ContactInfo(person = person)
            }
        )
        
        // Información de documento
        InfoSection(
            title = "Documento de Identidad",
            icon = Icons.Default.Badge,
            content = {
                DocumentInfo(person = person)
            }
        )
        
        // Información de dirección
        if (!person.address.isNullOrBlank()) {
            InfoSection(
                title = "Dirección",
                icon = Icons.Default.LocationOn,
                content = {
                    AddressInfo(person = person)
                }
            )
        }
        
        // Información de roles
        InfoSection(
            title = "Roles y Estado",
            icon = Icons.Default.Person,
            content = {
                RolesInfo(person = person)
            }
        )
        
        // Información adicional para conductores
        if (person.isDriver) {
            InfoSection(
                title = "Información de Conductor",
                icon = Icons.Default.DirectionsCar,
                content = {
                    DriverInfo(person = person)
                }
            )
        }
    }
}

@Composable
private fun PersonHeader(person: IPerson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = person.names ?: "Sin nombre",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (!person.shortName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = person.shortName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (!person.code.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Código: ${person.code}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ContactInfo(person: IPerson) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!person.email.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = person.email
            )
        }
        if (!person.phone.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Phone,
                label = "Teléfono",
                value = person.phone
            )
        }
    }
}

@Composable
private fun DocumentInfo(person: IPerson) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!person.documentType.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Description,
                label = "Tipo de Documento",
                value = getDocumentTypeName(person.documentType)
            )
        }
        if (!person.documentNumber.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Numbers,
                label = "Número de Documento",
                value = person.documentNumber
            )
        }
    }
}

@Composable
private fun AddressInfo(person: IPerson) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!person.address.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Home,
                label = "Dirección",
                value = person.address
            )
        }
        if (!person.country.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Public,
                label = "País",
                value = person.country
            )
        }
    }
}

@Composable
private fun RolesInfo(person: IPerson) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Cliente", style = MaterialTheme.typography.bodyMedium)
            Icon(
                imageVector = if (person.isClient) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (person.isClient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Proveedor", style = MaterialTheme.typography.bodyMedium)
            Icon(
                imageVector = if (person.isSupplier) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (person.isSupplier) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Conductor", style = MaterialTheme.typography.bodyMedium)
            Icon(
                imageVector = if (person.isDriver) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (person.isDriver) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Estado", style = MaterialTheme.typography.bodyMedium)
            Icon(
                imageVector = if (person.isEnabled) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (person.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DriverInfo(person: IPerson) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!person.driverLicense.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.DriveFileRenameOutline,
                label = "Número de Licencia",
                value = person.driverLicense
            )
        }
        if (!person.mtcRegistrationNumber.isNullOrBlank()) {
            InfoRow(
                icon = Icons.Default.Assignment,
                label = "Número de Registro MTC",
                value = person.mtcRegistrationNumber
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}
