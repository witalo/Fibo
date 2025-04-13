package com.example.fibo.ui.screens.receipt

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ITariff
import com.example.fibo.ui.screens.invoice.AddProductDialog
import com.example.fibo.ui.screens.invoice.ResumenRow
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.ProductSearchState
import com.example.fibo.utils.getAffectationColor
import com.example.fibo.utils.getAffectationTypeShort
import com.example.fibo.utils.getCurrentFormattedDate
import com.example.fibo.utils.getCurrentFormattedTime
import com.example.fibo.viewmodels.NewInvoiceViewModel
import com.example.fibo.viewmodels.NewReceiptViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReceiptScreen(
    onBack: () -> Unit,
    onReceiptCreated: (String) -> Unit,
    viewModel: NewReceiptViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val companyData by viewModel.companyData.collectAsState()
    val subsidiaryData by viewModel.subsidiaryData.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var clientData by remember { mutableStateOf<IPerson?>(null) }
    var documentNumber by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var operationDetails by remember { mutableStateOf<List<IOperationDetail>>(emptyList()) }

    var discountGlobalValue by remember { mutableStateOf(0.0) }
    var discountGlobalPercentage by remember { mutableStateOf(0.0) }
    var discountGlobalString by remember { mutableStateOf("0.00") }
    var applyGlobalDiscount by remember { mutableStateOf(false) }

    // Calcular totales basados en typeAffectationId
    val totalTaxed = operationDetails.filter { it.typeAffectationId == 1 }
        .sumOf { it.totalValue }
    val totalExonerated = operationDetails.filter { it.typeAffectationId == 2 }
        .sumOf { it.totalValue }
    val totalUnaffected = operationDetails.filter { it.typeAffectationId == 3 }
        .sumOf { it.totalValue }
    val totalFree = operationDetails.filter { it.typeAffectationId == 4 }
        .sumOf { it.totalValue }

    // Suma de descuentos por ítem
    val discountForItem = operationDetails.sumOf { it.totalDiscount }

    // Total general sin descuento global
    val totalIgv = operationDetails.filter { it.typeAffectationId == 1 }
        .sumOf { it.totalIgv }

    val baseImponible = totalTaxed + totalExonerated + totalUnaffected
    val totalAmount = baseImponible + totalIgv
    var discountByPercentage by remember { mutableStateOf(false) } //  Controla si el descuento es por porcentaje o monto
    LaunchedEffect(discountGlobalString, applyGlobalDiscount, discountByPercentage, baseImponible) {
        if (applyGlobalDiscount) {
            val inputValue = discountGlobalString.toDoubleOrNull() ?: 0.0
            if (discountByPercentage) {
                // Solo aplicar como porcentaje (validar que no sea mayor a 100)
                discountGlobalPercentage = min(inputValue, 100.0)
                discountGlobalValue = (baseImponible * discountGlobalPercentage) / 100
            } else {
                // Aplicar como monto fijo (no puede ser mayor que el total)
                discountGlobalValue = min(inputValue, baseImponible)
                discountGlobalPercentage = if (baseImponible > 0) {
                    (discountGlobalValue / baseImponible) * 100
                } else {
                    0.0
                }
            }
        } else {
            discountGlobalValue = 0.0
            discountGlobalPercentage = 0.0
        }
    }
    // Total de descuentos (global + por ítem)
    val totalDiscount = discountGlobalValue + discountForItem

    // Total a pagar considerando todos los descuentos
//    val totalToPay = totalAmount - totalDiscount
    val totalToPay = baseImponible + totalIgv - discountGlobalValue
    // Agrega este estado al inicio de tu composable
    var showBoletaConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Boleta", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp) // Padding más ajustado
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // CARD CABECERA - Información del Cliente
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp), // Espacio reducido
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp) // Bordes redondeados
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp) // Padding interno reducido
                        .fillMaxWidth()
                ) {
                    Text(
                        "Información del Cliente",
                        style = MaterialTheme.typography.titleSmall, // Texto más pequeño
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp) // Espacio reducido
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it },
                            label = { Text("DNI", style = MaterialTheme.typography.labelSmall) },
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                                .background(
                                    brush = ColorGradients.blueButtonGradient,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (documentNumber.length == 8 && documentNumber.all { it.isDigit() }) {
                                        viewModel.fetchClientData(documentNumber) { person ->
                                            val modifiedPerson = person.copy(
                                                names = person.names?.uppercase(),
                                                documentType = "01",
                                                documentNumber = person.documentNumber,
                                                address = person.address?.trim(),
                                            )
                                            clientData = modifiedPerson
                                        }
                                    } else {
                                        // Puedes mostrar un mensaje de error si no es válido
                                        Toast
                                            .makeText(
                                                context,
                                                "El DNI debe tener 8 dígitos",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Extraer",
                                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientData?.names ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(names = it) ?: IPerson(names = it)
                        },
                        label = { Text("Nombres...", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = clientData?.address ?: "",
                        onValueChange = {
                            clientData = clientData?.copy(address = it) ?: IPerson(address = it)
                        },
                        label = { Text("Dirección", style = MaterialTheme.typography.labelSmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // CARD CUERPO - Lista de productos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Detalle de Productos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    spotColor = MaterialTheme.colorScheme.primary
                                )
                                .background(
                                    brush = ColorGradients.blueButtonGradient,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { showAddItemDialog = true }
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar Item",
                                    modifier = Modifier.size(18.dp),  // Tamaño ligeramente mayor
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))  // Espacio un poco mayor
                                Text(
                                    "Agregar",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold  // Texto en negrita
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (operationDetails.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp), // Padding reducido
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay productos agregados",
                                style = MaterialTheme.typography.bodySmall, // Texto más pequeño
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Encabezado más compacto
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    "Producto",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.5f)
                                )
                                Text(
                                    "Cant.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(30.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Precio",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End
                                )
                                Spacer(modifier = Modifier.width(24.dp))
                            }

                            // Items más compactos
                            operationDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            detail.tariff.productName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Código: ${detail.tariff.productCode} (${getAffectationTypeShort(detail.typeAffectationId)
                                            })",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (detail.totalDiscount > 0) {
                                            Text(
                                                "Dscto: S/ ${String.format("%.2f", detail.totalDiscount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFFF5722)
                                            )
                                        }
                                    }
                                    // Tipo de afectación
//                                    Text(
//                                        getAffectationTypeShort(detail.typeAffectationId),
//                                        style = MaterialTheme.typography.labelSmall,
//                                        modifier = Modifier.width(30.dp),
//                                        textAlign = TextAlign.Center,
//                                        color = getAffectationColor(detail.typeAffectationId)
//                                    )
                                    Text(
                                        "${detail.quantity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(30.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "S/ ${detail.unitPrice}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        "S/ ${String.format("%.2f", detail.totalAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    IconButton(
                                        onClick = {
                                            operationDetails = operationDetails.filter { it != detail }
                                        },
                                        modifier = Modifier.size(28.dp) // Tamaño reducido
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color(0xFFFF5722),
                                            modifier = Modifier.size(18.dp) // Icono más pequeño
                                        )
                                    }
                                }
                                Divider(thickness = 0.5.dp) // Línea más fina
                            }
                        }
                    }
                }
            }

            // CARD DESCUENTO GLOBAL
            if (operationDetails.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = applyGlobalDiscount,
                                onCheckedChange = { applyGlobalDiscount = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                "Aplicar descuento global",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        AnimatedVisibility(visible = applyGlobalDiscount) {
                            Column {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = discountByPercentage,
                                        onCheckedChange = {
                                            discountByPercentage = it
                                            // Resetear el valor cuando se cambia el tipo
                                            discountGlobalString = "0.00"
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        "Descuento por porcentaje",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = discountGlobalString,
                                        onValueChange = { discountGlobalString = it },
                                        label = { Text(
                                            if (discountByPercentage) "Porcentaje descuento" else "Monto descuento (S/)"
                                        )  },
                                        placeholder = {
                                            Text(
                                                if (discountByPercentage) "Ej: 10.5" else "Ej: 50.00"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        suffix = {
                                            Text(
                                                if (discountByPercentage) "%" else "S/",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "Descuento: S/ ${String.format("%.2f", discountGlobalValue)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF5722)
                                        )
                                        Text(
                                            "(${String.format("%.2f", discountGlobalPercentage)}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (discountByPercentage) {
                                        "Ingrese un porcentaje de descuento (ej: 10.5 para 10.5%)"
                                    } else {
                                        "Ingrese un monto fijo de descuento en soles (ej: 50.00)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            // CARD FOOTER - Totales y botones
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Resumen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Mostrar los diferentes tipos según SUNAT
                    if (totalTaxed > 0) {
                        ResumenReceiptRow(
                            label = "Op. Gravadas:",
                            value = totalTaxed,
                            color = getAffectationColor(1)
                        )
                    }

                    if (totalExonerated > 0) {
                        ResumenReceiptRow(
                            label = "Op. Exoneradas:",
                            value = totalExonerated,
                            color = getAffectationColor(2)
                        )
                    }

                    if (totalUnaffected > 0) {
                        ResumenReceiptRow(
                            label = "Op. Inafectas:",
                            value = totalUnaffected,
                            color = getAffectationColor(3)
                        )
                    }

                    if (totalFree > 0) {
                        ResumenReceiptRow(
                            label = "Op. Gratuitas:",
                            value = totalFree,
                            color = getAffectationColor(4)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    if (totalDiscount > 0) {
                        ResumenReceiptRow(
                            label = "Descuentos:",
                            value = -totalDiscount,
                            color = Color(0xFFFF5722)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    ResumenReceiptRow(
                        label = "IGV (${companyData?.percentageIgv}%):",
                        value = totalIgv
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TOTAL:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalToPay)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = ColorGradients.goldLuxury
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones modernos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient)
                        ) {
                            Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = { showBoletaConfirmationDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            ),
                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
                        ) {
                            Text("Emitir Boleta", style = MaterialTheme.typography.labelLarge)
                        }

// Diálogo de confirmación para boleta
                        if (showBoletaConfirmationDialog) {
                            AlertDialog(
                                onDismissRequest = { showBoletaConfirmationDialog = false },
                                title = { Text(text = "Confirmar emisión", style = MaterialTheme.typography.titleMedium) },
                                text = { Text("¿Está seguro que desea emitir esta boleta de venta?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showBoletaConfirmationDialog = false
                                            val operation = IOperation(
                                                id = 0,
                                                serial = "",
                                                correlative = 0,
                                                documentType = "03",
                                                operationType = "0101",
                                                operationStatus = "01",
                                                operationAction = "E",
                                                currencyType = "PEN",
                                                operationDate = getCurrentFormattedDate(),
                                                emitDate = getCurrentFormattedDate(),
                                                emitTime = getCurrentFormattedTime(),
                                                userId = userData?.id!!,
                                                subsidiaryId = subsidiaryData?.id!!,
                                                client = clientData ?: IPerson(),
                                                operationDetailSet = operationDetails,
                                                discountGlobal = discountGlobalValue,
                                                discountPercentageGlobal = discountGlobalPercentage,
                                                discountForItem = discountForItem,
                                                totalDiscount = totalDiscount,
                                                totalTaxed = totalTaxed,
                                                totalUnaffected = totalUnaffected,
                                                totalExonerated = totalExonerated,
                                                totalIgv = totalIgv,
                                                totalFree = totalFree,
                                                totalAmount = totalAmount,
                                                totalToPay = totalToPay,
                                                totalPayed = totalToPay
                                            )
                                            viewModel.createInvoice(operation) { operationId, message ->
                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                onBack()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text("Confirmar", style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ))
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showBoletaConfirmationDialog = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Cancelar", style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ))
                                    }
                                }
                            )
                        }
//                        Button(
//                            onClick = {
//                                val operation = IOperation(
//                                    id = 0,
//                                    serial = "",
//                                    correlative = 0,
//                                    documentType = "03",
//                                    operationType = "0101",
//                                    operationStatus = "01",
//                                    operationAction = "E",
//                                    currencyType = "PEN",
//                                    operationDate = getCurrentFormattedDate(),
//                                    emitDate = getCurrentFormattedDate(),
//                                    emitTime = getCurrentFormattedTime(),
//                                    userId = userData?.id!!,
//                                    subsidiaryId = subsidiaryData?.id!!,
//                                    client = clientData ?: IPerson(),
//                                    operationDetailSet = operationDetails,
//                                    discountGlobal = discountGlobalValue,
//                                    discountPercentageGlobal = discountGlobalPercentage,
//                                    discountForItem = discountForItem,
//                                    totalDiscount = totalDiscount,
//                                    totalTaxed = totalTaxed,
//                                    totalUnaffected = totalUnaffected,
//                                    totalExonerated = totalExonerated,
//                                    totalIgv = totalIgv,
//                                    totalFree = totalFree,
//                                    totalAmount = totalAmount,
//                                    totalToPay = totalToPay,
//                                    totalPayed = totalToPay
//                                )
////                                viewModel.createInvoice(operation) { operationId ->
////                                    onInvoiceCreated(operationId.toString())
////                                }
//                                viewModel.createInvoice(operation) { operationId, message ->
//                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
//                                    onBack()
//                                    // Alternatively, if you still want to navigate to invoice detail:
//                                    // onInvoiceCreated(operationId.toString())
//                                }
//                            },
//                            modifier = Modifier
//                                .weight(1f)
//                                .height(48.dp),
//                            shape = RoundedCornerShape(8.dp),
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.inverseSurface,
//                                contentColor = Color.White
//                            ),
//                            border = BorderStroke(1.dp, ColorGradients.blueButtonGradient),
//                            elevation = ButtonDefaults.buttonElevation(
//                                defaultElevation = 2.dp,
//                                pressedElevation = 4.dp
//                            ),
//                            enabled = operationDetails.isNotEmpty() && clientData?.names?.isNotBlank() == true
//                        ) {
//                            Text("Emitir Boleta", style = MaterialTheme.typography.labelLarge)
//                        }
                    }
                }
            }
        }
        // Diálogo para agregar producto
        if (showAddItemDialog) {
            AddReceiptProductDialog(
                onDismiss = { showAddItemDialog = false },
                onProductAdded = { newItem ->
                    operationDetails = operationDetails + newItem
                    showAddItemDialog = false
                },
                viewModel = viewModel,
                subsidiaryId = subsidiaryData?.id ?: 0,
                igvPercentage = companyData?.percentageIgv ?: 0.18
            )
        }
        // Indicador de carga
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        // Diálogo de error
        error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error", style = MaterialTheme.typography.titleSmall) },
                text = { Text(errorMessage, style = MaterialTheme.typography.bodySmall) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearError() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Aceptar", style = MaterialTheme.typography.labelLarge)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun ResumenReceiptRow(
    label: String,
    value: Double,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "S/ ${String.format("%.2f", value)}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptProductDialog(
    onDismiss: () -> Unit,
    onProductAdded: (IOperationDetail) -> Unit,
    viewModel: NewReceiptViewModel,
    subsidiaryId: Int = 0,
    igvPercentage: Double = 0.18
) {
    val decimalRegex = Regex("^\\d*(\\.\\d{0,4})?$")
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    // Estados para el producto seleccionado
    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0.00") }
    var selectedAffectationType by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.typeAffectationId ?: 1)
    }

    // Precios con IGV y sin IGV (se actualizan cuando se selecciona un producto)
    var priceWithIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithIgv?.toString() ?: "0.00")
    }
    var priceWithoutIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithoutIgv?.toString() ?: "0.00")
    }

    // Cálculos para el resumen
    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
    val priceValue = priceWithIgv.toDoubleOrNull() ?: 0.0
    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
    val discountValue = discount.toDoubleOrNull() ?: 0.0
    val discountPercentage = if (priceWithoutIgvValue * qtyValue > 0)
        (discountValue / (priceWithoutIgvValue * qtyValue)) * 100 else 0.0

    // Subtotal sin IGV (antes de descuentos)
    val subtotalWithoutDiscount = priceWithoutIgvValue * qtyValue
// Subtotal con descuento aplicado
    val subtotalAfterDiscount = max(0.0, subtotalWithoutDiscount - discountValue)

    val subtotal = priceWithoutIgvValue * qtyValue

    // El IGV solo aplica para operaciones gravadas (tipo 1)
//    val igvAmount = if (selectedAffectationType == 1) subtotal * igvPercentage else 0.0
    val igvAmount = if (selectedAffectationType == 1) subtotalAfterDiscount * igvPercentage else 0.0


    // Total varía según el tipo de afectación
    val total = when (selectedAffectationType) {
        1 -> subtotalAfterDiscount + igvAmount // Gravada: (Base - Descuento) + IGV
        2, 3 -> subtotalAfterDiscount          // Exonerada o Inafecta: Base - Descuento
        4 -> 0.0                               // Gratuita
        else -> subtotalAfterDiscount + igvAmount
    }
//    val total = when (selectedAffectationType) {
//        1 -> subtotal + igvAmount - discountValue // Gravada
//        2, 3 -> subtotal - discountValue // Exonerada o Inafecta
//        4 -> 0.0 // Gratuita
//        else -> subtotal + igvAmount - discountValue
//    }

    // Debounce para la búsqueda
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 3) {
            delay(350) // Tiempo de debounce
            viewModel.searchProductsByQuery(searchQuery, subsidiaryId)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false, // <- esto evita el cierre al hacer clic fuera
            usePlatformDefaultWidth = false
        )
//        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp),
            shape = RoundedCornerShape(15.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, start = 18.dp, end = 18.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Encabezado del diálogo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Agregar Producto",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Campo de búsqueda con autocompletado
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Buscar producto") },
                    placeholder = { Text("Ingrese 3 caracteres") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Resultados de la búsqueda
                AnimatedVisibility(
                    visible = searchQuery.length >= 3 && selectedProduct == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
                        when (searchState) {
                            is ProductSearchState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(35.dp),
                                        strokeWidth = 3.dp,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                            is ProductSearchState.Success -> {
                                val products = (searchState as ProductSearchState.Success).products
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    )
                                ) {
                                    LazyColumn(modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 350.dp, max = 350.dp)) {
                                        itemsIndexed(products) { index, product ->
                                            ReceiptProductListItem(
                                                product = product,
                                                onClick = {
                                                    viewModel.getTariff(product.id, subsidiaryId)
                                                }
                                            )

                                            if (index < products.size - 1) {
                                                Divider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    thickness = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is ProductSearchState.Empty -> {
                                EmptyReceiptSearchResult()
                            }
                            is ProductSearchState.Error -> {
                                SearchReceiptError((searchState as ProductSearchState.Error).message)
                            }
                            else -> {
                                // Estado Idle
                                if (searchQuery.isNotEmpty()) {
                                    MinimumSearchReceiptInfo()
                                }
                            }
                        }
                    }
                }

                // Detalles del producto seleccionado
                AnimatedVisibility(
                    visible = selectedProduct != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    selectedProduct?.let { product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            // Card con datos del producto seleccionado
                            SelectedReceiptProductCard(
                                product = product,
                                onClear = { viewModel.clearProductSelection() }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Sección de cantidad, precio y descuento
                            Text(
                                "Detalle de venta",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Cantidad y Descuento
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { quantity = it },
                                    label = { Text("Cantidad") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = discount,
                                    onValueChange = { discount = it },
                                    label = { Text("Descuento S/") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Precios
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = priceWithoutIgv,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(decimalRegex)) {
                                            priceWithoutIgv = it
                                            val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
                                            val withIgvValue  = (withoutIgvValue * (1 + igvPercentage))
                                            priceWithIgv = String.format("%.4f", withIgvValue)
                                        }
//                                        priceWithoutIgv = it
//                                        // Calcular precio con IGV
//                                        val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithIgv = (withoutIgvValue * (1 + igvPercentage)).toString()
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio sin IGV") },
                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = priceWithIgv,
                                    onValueChange = {
                                        if (it.isEmpty() || it.matches(decimalRegex)) {
                                            priceWithIgv = it
                                            val priceWithIgvValue = it.toDoubleOrNull() ?: 0.0
                                            val withoutIgvValue  = (priceWithIgvValue / (1 + igvPercentage))
                                            priceWithoutIgv  = String.format("%.4f", withoutIgvValue )
                                        }
//                                        priceWithIgv = it
//                                        // Calcular precio sin IGV
//                                        val withIgvValue = it.toDoubleOrNull() ?: 0.0
//                                        priceWithoutIgv = (withIgvValue / (1 + igvPercentage)).toString()
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio con IGV") },
                                    leadingIcon = { Text("S/", modifier = Modifier.padding(start = 3.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(15.dp))

                            // Resumen de la venta
                            PurchaseReceiptSummary(
                                subtotal = subtotal,
                                igv = igvAmount,
                                discount = discountValue,
                                total = total,
                                igvPercentage = igvPercentage
                            )

                            Spacer(modifier = Modifier.height(15.dp))

                            // Botón agregar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = MaterialTheme.colorScheme.primary
                                    )
                                    .background(
                                        brush = ColorGradients.blueButtonGradient,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        val tariff = ITariff(
                                            productId = product.productId,
                                            productCode = product.productCode,
                                            productName = product.productName,
                                            unitId = product.unitId,
                                            unitName = product.unitName,
                                            remainingQuantity = product.remainingQuantity,
                                            priceWithIgv = priceWithIgv.toDoubleOrNull() ?: 0.0,
                                            priceWithoutIgv = priceWithoutIgv.toDoubleOrNull()
                                                ?: 0.0,
                                            productTariffId = product.productTariffId,
                                            typeAffectationId = product.typeAffectationId
                                        )

                                        val operationDetail = IOperationDetail(
                                            id = 0,
                                            tariff = tariff,
                                            typeAffectationId = product.typeAffectationId,
                                            quantity = qtyValue,
                                            unitValue = priceWithoutIgvValue,
                                            unitPrice = priceValue,
                                            totalDiscount = discountValue,
//                                            discountPercentage = if (subtotal > 0) (discountValue / subtotal) * 100 else 0.0,
                                            discountPercentage = if (subtotalWithoutDiscount > 0) (discountValue / subtotalWithoutDiscount) * 100 else 0.0,
                                            igvPercentage = igvPercentage * 100,
                                            perceptionPercentage = 0.0,
                                            totalPerception = 0.0,
//                                            totalValue = when (product.typeAffectationId) {
//                                                1 -> subtotal  // Operación gravada
//                                                2 -> subtotal  // Operación exonerada
//                                                3 -> subtotal  // Operación inafecta
//                                                4 -> 0.0       // Operación gratuita (valor comercial en totalValue)
//                                                else -> subtotal
//                                            },
                                            totalValue = when (product.typeAffectationId) {
                                                1 -> subtotalAfterDiscount  // Operación gravada (base neta)
                                                2 -> subtotalAfterDiscount  // Operación exonerada
                                                3 -> subtotalAfterDiscount  // Operación inafecta
                                                4 -> 0.0                   // Operación gratuita (valor comercial en totalValue)
                                                else -> subtotalAfterDiscount
                                            },
                                            totalIgv = if (product.typeAffectationId == 1) igvAmount else 0.0,
//                                            totalAmount = when (product.typeAffectationId) {
//                                                1 -> subtotal + igvAmount - discountValue  // Gravada: Base + IGV - Descuento
//                                                2, 3 -> subtotal - discountValue           // Exonerada/Inafecta: Base - Descuento
//                                                4 -> 0.0                                   // Gratuita: Se registra 0
//                                                else -> subtotal + igvAmount - discountValue
//                                            },
                                            totalAmount = when (product.typeAffectationId) {
                                                1 -> subtotalAfterDiscount + igvAmount  // Gravada: (Base - Descuento) + IGV
                                                2, 3 -> subtotalAfterDiscount           // Exonerada/Inafecta: Base - Descuento
                                                4 -> 0.0                               // Gratuita: Se registra 0
                                                else -> subtotalAfterDiscount + igvAmount
                                            },
                                            totalToPay = total
                                        )

                                        onProductAdded(operationDetail)
                                    }
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Agregar Producto",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
private fun ReceiptProductListItem(
    product: IProduct,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono o imagen del producto
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Información del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Código: ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ícono de selección
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Seleccionar",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@Composable
private fun SelectedReceiptProductCard(
    product: ITariff,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PRODUCTO SELECCIONADO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            brush = ColorGradients.blueVibrant
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cambiar selección",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Nombre del producto
                Text(
                    text = product.productName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Detalles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        InfoReceiptRow(
                            label = "Código:",
                            value = product.productCode,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoReceiptRow(
                            label = "Stock:",
                            value = "${product.remainingQuantity} ${product.unitName}",
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Precio con mejor estilo
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "S/ ${"%.2f".format(product.priceWithIgv)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = ColorGradients.orangeSunset,
                                fontWeight = FontWeight.Bold,
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoReceiptRow(
    label: String,
    value: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyReceiptSearchResult() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No se encontraron productos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchReceiptError(errorMessage: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Error en la búsqueda",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun MinimumSearchReceiptInfo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Ingrese al menos 3 caracteres para buscar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PurchaseReceiptSummary(
    subtotal: Double,
    igv: Double,
    discount: Double,
    total: Double,
    igvPercentage: Double = 0.18
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Resumen",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SummaryReceiptRow(
                label = "Subtotal:",
                value = subtotal
            )

            SummaryReceiptRow(
                label = "IGV (${igvPercentage}%):",
                value = igv
            )

            SummaryReceiptRow(
                label = "Descuento:",
                value = -discount,
                valueColor = if (discount > 0) MaterialTheme.colorScheme.tertiary else null
            )

            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            SummaryReceiptRow(
                label = "TOTAL:",
                value = total,
                isTotal = true
            )
        }
    }
}

@Composable
private fun SummaryReceiptRow(
    label: String,
    value: Double,
    isTotal: Boolean = false,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = "S/ ${String.format("%.2f", value)}",
//            style = if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            style = MaterialTheme.typography.titleMedium.copy(
                brush = if (isTotal) ColorGradients.orangeFire else ColorGradients.orangeSunset
            ),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = when {
                valueColor != null -> valueColor
                isTotal -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
