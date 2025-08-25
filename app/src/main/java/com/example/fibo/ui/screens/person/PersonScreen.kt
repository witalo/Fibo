package com.example.fibo.ui.screens.person

import androidx.compose.material.icons.filled.Menu
import com.example.fibo.ui.screens.CenterLoadingIndicator
import com.example.fibo.ui.screens.EmptyState
import com.example.fibo.ui.screens.ErrorMessage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IPerson
import com.example.fibo.model.ISubsidiary
import com.example.fibo.navigation.Screen
import com.example.fibo.ui.components.AppScaffold
import com.example.fibo.ui.components.AppTopBarWithFilter
import com.example.fibo.ui.components.SideMenu
import com.example.fibo.utils.PersonState
import com.example.fibo.ui.components.SideMenu
@Composable
fun PersonScreen(
    navController: NavController,
    viewModel: PersonViewModel = hiltViewModel(),
    subsidiaryData: ISubsidiary? = null,
    onLogout: () -> Unit
) {
    val companyData by viewModel.companyData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val personState by viewModel.personState.collectAsState()
    val selectedTypes by viewModel.selectedTypes.collectAsState()

    // Estados para el diálogo de búsqueda
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val selectedPerson by viewModel.selectedPerson.collectAsState()

    // Estado para el diálogo de filtros
    var isFilterDialogOpen by remember { mutableStateOf(false) }
    // Escuchar cuando se crea una nueva persona
//    LaunchedEffect(Unit) {
//        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("person_created")?.let { wasCreated ->
//            if (wasCreated) {
//                // Limpiar el flag
//                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("person_created")
//                // Recargar la lista de personas con los parámetros correctos
//                subsidiaryData?.id?.let { subsidiaryId ->
//                    val currentTypes = viewModel.getCurrentTypes()
//                    viewModel.loadPersons(subsidiaryId, currentTypes)
//                }
//            }
//        }
//    }
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("person_created")?.let { wasCreated ->
            if (wasCreated) {
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("person_created")
                subsidiaryData?.id?.let { subsidiaryId ->
                    val currentTypes = viewModel.getCurrentTypes()
                    viewModel.loadPersons(subsidiaryId, currentTypes)
                }
            }
        }

        // ✅ Escuchar cuando se actualiza una persona
        navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("person_updated")?.let { wasUpdated ->
            if (wasUpdated) {
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("person_updated")
                subsidiaryData?.id?.let { subsidiaryId ->
                    val currentTypes = viewModel.getCurrentTypes()
                    viewModel.loadPersons(subsidiaryId, currentTypes)
                }
            }
        }
    }

    AppScaffold(
        navController = navController,
        subsidiaryData = subsidiaryData,
        onLogout = onLogout,
        topBar = {
            AppTopBarWithFilter(
                title = if (selectedPerson != null) {
                    "${selectedPerson?.names?.take(15)}..."
                } else {
                    "Personas"
                },
                onFilterClick = { isFilterDialogOpen = true },
                onTitleClick = { isSearchDialogOpen = true },
                filterBadgeCount = selectedTypes.size
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (personState) {
                is PersonState.Loading -> CenterLoadingIndicator()
                is PersonState.WaitingForUser -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando datos...")
                        }
                    }
                }

                is PersonState.Success -> {
                    val persons = (personState as PersonState.Success).data
                    PersonContent(
                        persons = persons,
                        onPersonClick = { person ->
                            navController.navigate(Screen.PersonDetail.createRoute(person.id))
                        },
                        onPersonEdit = { person ->
                            navController.navigate(Screen.EditPerson.createRoute(person.id))
                        },
                        selectedPerson = selectedPerson,
                        onClearPersonFilter = { viewModel.clearPersonSelection() }
                    )
                }

                is PersonState.Error -> {
                    ErrorMessage(
                        message = (personState as PersonState.Error).message,
                        onRetry = {
                            subsidiaryData?.id?.let { subsidiaryId ->
                                viewModel.loadPersons(subsidiaryId, viewModel.getCurrentTypes())
                            }
                        }
                    )
                }
            }

            // Botón flotante para crear nueva persona
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewPerson.route) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear persona")
            }
        }
    }

    // Diálogo de búsqueda de personas
    PersonSearchDialog(
        isVisible = isSearchDialogOpen,
        onDismiss = { isSearchDialogOpen = false },
        searchQuery = searchQuery,
        onSearchQueryChange = { query -> viewModel.searchPersons(query) },
        searchResults = searchResults,
        isLoading = isSearching,
        onPersonSelected = { person ->
            viewModel.selectPerson(person)
            isSearchDialogOpen = false
        }
    )

    // Diálogo de filtros de tipos
    PersonFilterDialog(
        isVisible = isFilterDialogOpen,
        onDismiss = { isFilterDialogOpen = false },
        selectedTypes = selectedTypes,
        onTypeToggled = { type -> viewModel.togglePersonType(type) }
    )
}

@Composable
fun PersonContent(
    persons: List<IPerson>,
    onPersonClick: (IPerson) -> Unit,
    onPersonEdit: (IPerson) -> Unit,
    selectedPerson: IPerson?,
    onClearPersonFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        // Mostrar información de la persona seleccionada (si hay una)
        if (selectedPerson != null) {
            PersonFilterChip(
                person = selectedPerson,
                onClear = onClearPersonFilter,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Listado de personas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (persons.isEmpty()) {
                EmptyState(message = "No hay personas registradas")
            } else {
                PersonList(
                    persons = persons,
                    onPersonClick = onPersonClick,
                    onPersonEdit = onPersonEdit
                )
            }
        }
    }
}

@Composable
fun PersonList(
    persons: List<IPerson>,
    onPersonClick: (IPerson) -> Unit,
    onPersonEdit: (IPerson) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(persons) { person ->
            PersonItem(
                person = person,
                onClick = { onPersonClick(person) },
                onEdit = { onPersonEdit(person) }
            )
        }
    }
}

@Composable
fun PersonItem(
    person: IPerson,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2C2C2C) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkTheme) Color(0xFF444444) else Color(0xFFE0E0E0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono según el tipo de persona
            val icon = when {
                person.isClient && person.isSupplier -> Icons.Default.Person
                person.isClient -> Icons.Default.Person
                person.isSupplier -> Icons.Default.Business
                else -> Icons.Default.DirectionsCar
            }
            val iconColor = when {
                person.isClient && person.isSupplier -> Color(0xFF9C27B0)
                person.isClient -> Color(0xFF2196F3)
                person.isSupplier -> Color(0xFF4CAF50)
                else -> Color(0xFFFF9800)
            }

            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Contenido principal
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clickable { onClick() }
            ) {
                Text(
                    text = person.names ?: "Sin nombre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF1976D2),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${formatDocumentType(person.documentType)}: ${person.documentNumber ?: "-"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkTheme) Color(0xFFCCCCCC) else Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Badges de tipos
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (person.isClient) {
                        TypeBadge("Cliente", Color(0xFF2196F3))
                    }
                    if (person.isSupplier) {
                        TypeBadge("Proveedor", Color(0xFF4CAF50))
                    }
                    if (!person.isClient && !person.isSupplier) {
                        TypeBadge("Conductor", Color(0xFFFF9800))
                    }
                }
            }

            // ✅ Botón de editar
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar persona",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TypeBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
fun PersonFilterDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedTypes: Set<String>,
    onTypeToggled: (String) -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Filtrar por tipo", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    PersonTypeFilterItem(
                        type = "CLIENT",
                        label = "Clientes",
                        isSelected = selectedTypes.contains("CLIENT"),
                        onToggle = onTypeToggled
                    )
                    PersonTypeFilterItem(
                        type = "SUPPLIER",
                        label = "Proveedores",
                        isSelected = selectedTypes.contains("SUPPLIER"),
                        onToggle = onTypeToggled
                    )
                    PersonTypeFilterItem(
                        type = "DRIVER",
                        label = "Conductores",
                        isSelected = selectedTypes.contains("DRIVER"),
                        onToggle = onTypeToggled
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Aplicar")
                }
            }
        )
    }
}

@Composable
fun PersonTypeFilterItem(
    type: String,
    label: String,
    isSelected: Boolean,
    onToggle: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(type) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle(type) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

// Función auxiliar para formatear tipos de documento
fun formatDocumentType(documentType: String?): String {
    return when (documentType) {
        "01", "1" -> "DNI"
        "06", "6" -> "RUC"
        "07", "7" -> "CE"
        else -> documentType ?: "DOC"
    }
}




@Composable
fun FilterBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$count filtros",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp
        )
    }
}
@Composable
fun PersonFilterChip(
    person: IPerson,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Persona seleccionada",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.names ?: "Sin nombre",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDocumentType(person.documentType)}: ${person.documentNumber ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Limpiar filtro",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
@Composable
fun PersonSearchDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<IPerson>,
    isLoading: Boolean,
    onPersonSelected: (IPerson) -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Buscar persona",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por nombre o documento...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Results or loading/empty states
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        searchResults.isEmpty() && searchQuery.length >= 2 -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No se encontraron resultados",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        searchResults.isEmpty() -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Escribe al menos 2 caracteres para buscar",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            // Results list
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults) { person ->
                                    PersonSearchResultItem(
                                        person = person,
                                        onClick = { onPersonSelected(person) }
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
fun PersonSearchResultItem(
    person: IPerson,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on person type
            val icon = when {
                person.isClient && person.isSupplier -> Icons.Default.Person
                person.isClient -> Icons.Default.Person
                person.isSupplier -> Icons.Default.Business
                else -> Icons.Default.DirectionsCar
            }
            val iconColor = when {
                person.isClient && person.isSupplier -> Color(0xFF9C27B0)
                person.isClient -> Color(0xFF2196F3)
                person.isSupplier -> Color(0xFF4CAF50)
                else -> Color(0xFFFF9800)
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.names ?: "Sin nombre",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatDocumentType(person.documentType)}: ${person.documentNumber ?: "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Type badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (person.isClient) {
                    MiniTypeBadge("C", Color(0xFF2196F3))
                }
                if (person.isSupplier) {
                    MiniTypeBadge("P", Color(0xFF4CAF50))
                }
                if (!person.isClient && !person.isSupplier) {
                    MiniTypeBadge("D", Color(0xFFFF9800))
                }
            }
        }
    }
}

@Composable
fun MiniTypeBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}