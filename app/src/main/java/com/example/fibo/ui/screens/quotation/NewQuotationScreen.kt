package com.example.fibo.ui.screens.quotation

import android.util.Log
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fibo.model.IOperation
import com.example.fibo.model.IOperationDetail
import com.example.fibo.model.IPerson
import com.example.fibo.model.IProduct
import com.example.fibo.model.ITariff
import com.example.fibo.ui.components.DateSelector
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.ProductSearchState
import com.example.fibo.utils.getAffectationColor
import com.example.fibo.utils.getAffectationTypeShort
import com.example.fibo.utils.getCurrentFormattedDate
import com.example.fibo.utils.getCurrentFormattedTime
import com.example.fibo.viewmodels.HomeViewModel
import com.example.fibo.viewmodels.NewQuotationViewModel
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewQuotationScreen(
    onBack: () -> Unit,
    onQuotationCreated: (String) -> Unit,
    viewModel: NewQuotationViewModel = hiltViewModel()
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

    // 2. CALCULAR TOTALES POR TIPO DE OPERACIÓN (ANTES DE DESCUENTOS)
    val totalTaxedBeforeDiscount = operationDetails.filter { it.typeAffectationId == 1 }
        .sumOf { it.totalValue } // Suma de valores gravados
    val totalExonerated = operationDetails.filter { it.typeAffectationId == 2 }
        .sumOf { it.totalValue }
    val totalUnaffected = operationDetails.filter { it.typeAffectationId == 3 }
        .sumOf { it.totalValue }
    val totalFree = operationDetails.filter { it.typeAffectationId == 4 }
        .sumOf { it.totalValue }

    val igvPercentage =
        companyData?.percentageIgv ?: 18.0 // Valor por defecto 18% si no está definido
    val igvFactor = igvPercentage / 100.0

    // 3. CALCULAR DESCUENTOS
    val discountForItem = operationDetails.sumOf { it.totalDiscount } // Descuentos por ítem
    // *********MODIFICACIÓN: Convertir el descuento global a valor sin IGV para aplicarlo a la base gravada
    val discountGlobalWithoutIgv = if (applyGlobalDiscount) {
        discountGlobalValue / (1 + igvFactor) // Quitar IGV al descuento
    } else {
        0.0
    }
    // Aplicar descuento global SIN IGV solo a operaciones gravadas (tipo 1)
    val effectiveGlobalDiscount = if (applyGlobalDiscount) {
        min(
            discountGlobalWithoutIgv, // <-- AQUÍ está el cambio principal
            totalTaxedBeforeDiscount
        ) // No puede ser mayor que el total gravado
    } else {
        0.0
    }
    // *********
    // Aplicar descuento global solo a operaciones gravadas (tipo 1)
//    val effectiveGlobalDiscount = if (applyGlobalDiscount) {
//        min(
//            discountGlobalValue,
//            totalTaxedBeforeDiscount
//        ) // No puede ser mayor que el total gravado
//    } else {
//        0.0
//    }
    // 4. CALCULAR VALORES DESPUÉS DE DESCUENTOS
    val totalTaxedAfterDiscount = max(0.0, totalTaxedBeforeDiscount - effectiveGlobalDiscount)
    val totalIgv =
        totalTaxedAfterDiscount * igvFactor // IGV solo sobre lo gravado después de descuento

    // 5. CALCULAR TOTALES FINALES
    val baseImponible = totalTaxedAfterDiscount + totalExonerated + totalUnaffected
    val totalAmount = baseImponible + totalIgv
    val totalToPay = totalAmount // En una cotizacion normal, el total a pagar es igual al totalAmount
    var discountByPercentage by remember { mutableStateOf(false) } //  Controla si el descuento es por porcentaje o monto
    // 6. CÁLCULO DEL DESCUENTO GLOBAL (para mostrar en UI)
    LaunchedEffect(discountGlobalString, applyGlobalDiscount, discountByPercentage, baseImponible) {
        if (applyGlobalDiscount) {
            val inputValue = discountGlobalString.toDoubleOrNull() ?: 0.0
            if (discountByPercentage) {
                discountGlobalPercentage = min(inputValue, 100.0)
                discountGlobalValue = (totalTaxedBeforeDiscount * discountGlobalPercentage) / 100
            } else {
                discountGlobalValue = min(inputValue, totalTaxedBeforeDiscount)
                discountGlobalPercentage = if (totalTaxedBeforeDiscount > 0) {
                    (discountGlobalValue / totalTaxedBeforeDiscount) * 100
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
    // Estados para serie y fecha
    var quotationDate by remember { mutableStateOf(getCurrentFormattedDate()) }
    var showSerialsDialog by remember { mutableStateOf(false) }

    // Obtener series del ViewModel
    val serials by viewModel.serials.collectAsState()
    val selectedSerial by viewModel.selectedSerial.collectAsState()

    // Cargar series al iniciar o cuando cambie la sucursal
    LaunchedEffect(subsidiaryData) {
        subsidiaryData?.id?.let { subsidiaryId ->
            viewModel.loadSerials(subsidiaryId)
        }
    }

    //Editar cantidad********
    // 1. Estado para controlar la edición (debe estar en el ámbito superior del composable)
    var editingDetailId by remember { mutableStateOf<Int?>(null) }
    val focusRequester = remember { FocusRequester() }

    // 2. Función para actualizar cantidades (también en el ámbito superior)
    fun updateQuantity(detail: IOperationDetail, newQuantity: Double) {
        operationDetails = operationDetails.map {
            if (it.id == detail.id) {
                it.copy(
                    quantity = newQuantity,
                    totalValue = newQuantity * it.unitValue,
                    totalIgv = if (it.typeAffectationId == 1) {
                        (newQuantity * it.unitValue) * (it.igvPercentage / 100)
                    } else 0.0,
                    totalAmount = if (it.typeAffectationId == 1) {
                        (newQuantity * it.unitValue) * (1 + it.igvPercentage / 100)
                    } else {
                        newQuantity * it.unitValue
                    }
                )
            } else it
        }
    }
    //Editar cantidad********

    // Agrega este estado al inicio de tu composable
    var showConfirmationDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Cotización", style = MaterialTheme.typography.titleSmall) },
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
            // NUEVO CARD PARA SERIE Y FECHA
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
                    Text(
                        "Configuración de Cotización",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Selector de Series
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Serie",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSerialsDialog = true }
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    selectedSerial?.serial ?: "Seleccionar serie",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Selector de Fecha (usando tu componente reutilizable)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "  Fecha",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            DateSelector(
                                currentDate = quotationDate,
                                onDateSelected = { quotationDate = it }
                            )
                        }
                    }
                }
            }

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
                            label = { Text("DNI/RUC", style = MaterialTheme.typography.labelSmall) },
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
                                    if (documentNumber.all { it.isDigit() }) {
                                        when (documentNumber.length) {
                                            8 -> { // Validación para DNI
                                                viewModel.fetchClientData(documentNumber) { person ->
                                                    val modifiedPerson = person.copy(
                                                        names = person.names?.uppercase(),
                                                        documentType = "1",
                                                        documentNumber = person.documentNumber,
                                                        address = person.address?.trim(),
                                                    )
                                                    clientData = modifiedPerson
                                                }
                                            }
                                            11 -> { // Validación para RUC
                                                viewModel.fetchClientData(documentNumber) { person ->
                                                    val modifiedPerson = person.copy(
                                                        names = person.names?.uppercase(),
                                                        documentType = "6",
                                                        documentNumber = person.documentNumber,
                                                        address = person.address?.trim(),
                                                    )
                                                    clientData = modifiedPerson
                                                }
                                            }
                                            else -> {
                                                Toast.makeText(
                                                    context,
                                                    "Ingrese 8 dígitos para DNI o 11 dígitos para RUC",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Solo se permiten números",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                        label = {
                            Text(
                                "Denominación...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
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
                                            "Código: ${detail.tariff.productCode} (${
                                                getAffectationTypeShort(
                                                    detail.typeAffectationId
                                                )
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
//                                    Text(
//                                        "${detail.quantity}",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        modifier = Modifier.width(30.dp),
//                                        textAlign = TextAlign.Center
//                                    )
                                    // Luego, en tu lista de productos, reemplaza la parte de cantidad con esto:
                                    Box(
                                        modifier = Modifier.width(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (editingDetailId == detail.id) {
                                            var textFieldValue by remember(detail.id) {
                                                mutableStateOf(TextFieldValue(
                                                    text = "%.2f".format(detail.quantity),
                                                    selection = TextRange(0, "%.2f".format(detail.quantity).length)
                                                ))
                                            }

                                            val focusManager = LocalFocusManager.current
                                            val keyboardController = LocalSoftwareKeyboardController.current

                                            DisposableEffect(Unit) {
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                                onDispose { }
                                            }

                                            BasicTextField(
                                                value = textFieldValue,
                                                onValueChange = { newValue ->
                                                    if (newValue.text.matches(Regex("^\\d*\\.?\\d{0,2}\$")) || newValue.text.isEmpty()) {
                                                        textFieldValue = newValue
                                                        val quantity = newValue.text.toDoubleOrNull() ?: 0.0
                                                        updateQuantity(detail, quantity)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .width(80.dp)
                                                    .height(32.dp)
                                                    .focusRequester(focusRequester),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = {
                                                        editingDetailId = null
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                    }
                                                ),
                                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                singleLine = true,
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                                                shape = MaterialTheme.shapes.small
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = MaterialTheme.colorScheme.outline,
                                                                shape = MaterialTheme.shapes.small
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        innerTextField()
                                                    }
                                                }
                                            )
                                        } else {
                                            Text(
                                                "%.2f".format(detail.quantity),
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier
                                                    .width(80.dp)
                                                    .clickable {
                                                        editingDetailId = detail.id
                                                    },
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }


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
                                            operationDetails =
                                                operationDetails.filter { it != detail }
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

            // Diálogo para seleccionar serie
            if (showSerialsDialog) {
                AlertDialog(
                    onDismissRequest = { showSerialsDialog = false },
                    title = {
                        Text(
                            "Seleccionar serie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            serials.forEach { serial ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.selectSerial(serial)
                                            showSerialsDialog = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (serial.id == selectedSerial?.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = serial.id == selectedSerial?.id,
                                            onClick = {
                                                viewModel.selectSerial(serial)
                                                showSerialsDialog = false
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = serial.serial,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (serial.id == selectedSerial?.id)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSerialsDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 4.dp
                            )
                        ) {
                            Text(
                                "Cancelar",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp)
                )
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
                                        label = {
                                            Text(
                                                if (discountByPercentage) "Porcentaje descuento" else "Monto descuento (S/)"
                                            )
                                        },
                                        placeholder = {
                                            Text(
                                                if (discountByPercentage) "Ej: 10.50" else "Ej: 50.00"
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
                                            "Descuento: S/ ${String.format("%.2f", discountGlobalValue)
                                            }",
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
                    // Título del resumen
                    Text(
                        "Resumen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Descuentos globales (si existen)
                    if (totalDiscount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        ResumenRowQuotation(
                            label = "Descuentos:",
                            value = -totalDiscount,
                            color = Color(0xFFFF5722) // Color naranja/rojo para destacar
                        )
                    }
                    // Mostrar los diferentes tipos según SUNAT (con valores después de descuentos)
                    if (totalExonerated > 0) {
                        ResumenRowQuotation(
                            label = "Op. Exoneradas:",
                            value = totalExonerated,
                            color = getAffectationColor(2)
                        )
                    }

                    if (totalUnaffected > 0) {
                        ResumenRowQuotation(
                            label = "Op. Inafectas:",
                            value = totalUnaffected,
                            color = getAffectationColor(3)
                        )
                    }

                    if (totalFree > 0) {
                        ResumenRowQuotation(
                            label = "Op. Gratuitas:",
                            value = totalFree,
                            color = getAffectationColor(4)
                        )
                    }
                    if (totalTaxedAfterDiscount > 0) {
                        ResumenRowQuotation(
                            label = "Op. Gravadas:",
                            value = totalTaxedAfterDiscount,
                            color = getAffectationColor(1)
                        )
                    }
                    // IGV (solo aplicable a operaciones gravadas)
                    Spacer(modifier = Modifier.height(4.dp))
                    ResumenRowQuotation(
                        label = "IGV (${igvPercentage}%):",
                        value = totalIgv,
                        color = getAffectationColor(1) // Mismo color que operaciones gravadas
                    )

                    // Línea divisoria
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Total a pagar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL A PAGAR:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            "S/ ${String.format("%.2f", totalToPay)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                brush = ColorGradients.goldLuxury,
                                fontWeight = FontWeight.Bold
                            )
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
                            onClick = { showConfirmationDialog = true },
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Confirmar",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Cotizacion",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        // Diálogo de confirmación para cotizacion
                        if (showConfirmationDialog) {
                            AlertDialog(
                                onDismissRequest = { showConfirmationDialog = false },
                                title = { Text("Confirmar registro", style = MaterialTheme.typography.titleMedium) },
                                text = { Text("¿Está seguro que desea registrar esta cotizacion?") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showConfirmationDialog = false
                                            // Validaciones adicionales antes de crear la operación
                                            if (clientData?.documentNumber.isNullOrBlank()) {
                                                Toast.makeText(context, "Ingrese el RUC del cliente", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            if (operationDetails.isEmpty()) {
                                                Toast.makeText(context, "Agregue al menos un producto", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val operation = IOperation(
                                                id = 0, // ID se generará en el backend
                                                serial = selectedSerial?.serial ?: "", // Serie seleccionada
                                                correlative = 0, // Se asignará automáticamente
                                                documentType = "48", // Cotizacion
                                                operationType = "0101", // Cotizacion a cliente
                                                operationStatus = "01", // Pendiente de envío a SUNAT
                                                operationAction = "NA", // Emitir
                                                currencyType = "PEN", // Soles peruanos
                                                operationDate = getCurrentFormattedDate(), // Fecha actual
                                                emitDate = quotationDate, // Fecha de emisión
                                                emitTime = getCurrentFormattedTime(), // Hora actual
                                                userId = userData?.id ?: 0, // ID del usuario logueado
                                                subsidiaryId = subsidiaryData?.id ?: 0, // Sucursal
                                                client = clientData?.copy(
                                                    documentType = clientData!!.documentType,
                                                    documentNumber = clientData!!.documentNumber?.trim(),
                                                    names = clientData!!.names?.trim()?.uppercase(),
                                                    address = clientData!!.address?.trim(),
                                                    email = clientData!!.email,
                                                    phone = clientData!!.phone
                                                ) ?: run {
                                                    Toast.makeText(context, "Complete datos del cliente", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                },
                                                operationDetailSet = operationDetails.map { detail ->
                                                    detail.copy(
                                                        // Asegurar valores positivos
                                                        id = 0,
                                                        typeAffectationId = max(1, detail.typeAffectationId),
                                                        description = detail.description.trim().uppercase(),
                                                        tariff = detail.tariff,
                                                        quantity = max(0.0, detail.quantity),
                                                        unitValue = max(0.0, detail.unitValue),
                                                        unitPrice = max(0.0, detail.unitPrice),
                                                        discountPercentage = max(0.0, detail.discountPercentage),
                                                        totalDiscount = max(0.0, detail.totalDiscount),
                                                        perceptionPercentage = max(0.0, detail.perceptionPercentage),
                                                        totalPerception = max(0.0, detail.totalPerception),
                                                        igvPercentage = max(0.0, detail.igvPercentage),
                                                        totalValue = max(0.0, detail.totalValue),
                                                        totalIgv = max(0.0, detail.totalIgv),
                                                        totalAmount = max(0.0, detail.totalAmount),
                                                        totalToPay = max(0.0, detail.totalToPay)
                                                    )
                                                },
                                                discountGlobal = max(0.0, discountGlobalValue),
                                                discountPercentageGlobal = max(0.0, min(discountGlobalPercentage, 100.0)),
                                                discountForItem = max(0.0, discountForItem),
                                                totalDiscount = max(0.0, totalDiscount),
                                                totalTaxed = max(0.0, totalTaxedAfterDiscount), // Usar valor después de descuento
                                                totalUnaffected = max(0.0, totalUnaffected),
                                                totalExonerated = max(0.0, totalExonerated),
                                                totalIgv = max(0.0, totalIgv),
                                                totalFree = max(0.0, totalFree),
                                                totalAmount = max(0.0, totalAmount),
                                                totalToPay = max(0.0, totalToPay),
                                                totalPayed = max(0.0, totalToPay) // Asumimos que se paga completo
                                            )
                                            viewModel.createQuotation(operation) { operationId, message ->
                                                Toast.makeText(context, "Cotización $message creada", Toast.LENGTH_SHORT).show()
                                                onBack() // <-- Navega hacia atrás
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0D47A1), // Azul oscuro
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Confirmar",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Confirmar",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                },
                                dismissButton = {
                                    Button(
                                        onClick = { showConfirmationDialog = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFB71C1C), // Rojo oscuro
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Cancelar",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        // Diálogo para agregar producto
        if (showAddItemDialog) {
            AddProductQuotationDialog(
                onDismiss = { showAddItemDialog = false },
                onProductAdded = { newItem ->
                    operationDetails = operationDetails + newItem
                    showAddItemDialog = false
                },
                viewModel = viewModel,
                subsidiaryId = subsidiaryData?.id ?: 0,
                igvPercentage = igvPercentage
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
fun ResumenRowQuotation(
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
fun AddProductQuotationDialog(
    onDismiss: () -> Unit,
    onProductAdded: (IOperationDetail) -> Unit,
    viewModel: NewQuotationViewModel,
    subsidiaryId: Int = 0,
    igvPercentage: Double = 18.0
) {
    val valueIgv = igvPercentage / 100
    val decimalRegex = Regex("^\\d*(\\.\\d{0,4})?$")

    // Estados de búsqueda
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    // Estados del producto seleccionado
    var observaciones by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var discount by remember { mutableStateOf("0.00") }
    var selectedAffectationType by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.typeAffectationId ?: 1)
    }

    // Precios
    var priceWithIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithIgv?.toString() ?: "0.00")
    }
    var priceWithoutIgv by remember(selectedProduct) {
        mutableStateOf(selectedProduct?.priceWithoutIgv?.toString() ?: "0.00")
    }

    // Convertir valores a números
    val qtyValue = quantity.toDoubleOrNull() ?: 1.0
    val priceWithIgvValue = priceWithIgv.toDoubleOrNull() ?: 0.0
    val priceWithoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
    val discountValue = discount.toDoubleOrNull() ?: 0.0

    // CÁLCULOS CORRECTOS SEGÚN SUNAT:
    // 1. Calcular valores base
    val subtotalWithoutDiscount = priceWithoutIgvValue * qtyValue
    val maxPossibleDiscount = subtotalWithoutDiscount

    // 2. Aplicar descuento (no puede superar el valor del ítem)
    val effectiveDiscount = min(discountValue, maxPossibleDiscount)
    val subtotalAfterDiscount = max(0.0, subtotalWithoutDiscount - effectiveDiscount)

    // 3. Calcular IGV solo para operaciones gravadas (tipo 1) y después del descuento
    val igvAmount = if (selectedAffectationType == 1) {
        subtotalAfterDiscount * valueIgv
    } else {
        0.0
    }

    // 4. Calcular total según tipo de operación
    val totalAmount = when (selectedAffectationType) {
        1 -> subtotalAfterDiscount + igvAmount  // Gravada: (Base - Descuento) + IGV
        2, 3, 4 -> subtotalAfterDiscount           // Exonerada/Inafecta: Base - Descuento
//        4 -> 0.0                               // Gratuita (valor comercial = 0)
        else -> subtotalAfterDiscount + igvAmount
    }

    // 5. Porcentaje de descuento real aplicado
    val actualDiscountPercentage = if (subtotalWithoutDiscount > 0) {
        (effectiveDiscount / subtotalWithoutDiscount) * 100
    } else {
        0.0
    }
    //--------------------------------------------------
    // Lista de tipos de afectación
    val affectationTypes = listOf(
        AffectationType(1, "Gravada"),
        AffectationType(2, "Exonerada"),
        AffectationType(3, "Inafecta"),
        AffectationType(4, "Gratuita")
    )

    // Estado para controlar si el dropdown está expandido
    var expandedAffectationType by remember { mutableStateOf(false) }
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { product ->
            selectedAffectationType = product.typeAffectationId

            // Ajustar precios según tipo de afectación
            when (selectedAffectationType) {
                1 -> { // Gravado
                    priceWithoutIgv = String.format("%.4f", product.priceWithoutIgv)
                    priceWithIgv = String.format("%.4f", product.priceWithIgv)
                }
//                4 -> { // Gratuito
//                    priceWithoutIgv = "0.00"
//                    priceWithIgv = "0.00"
//                }
                else -> { // Exonerado o Inafecto
                    priceWithoutIgv = String.format("%.4f", product.priceWithoutIgv)
                    priceWithIgv = String.format("%.4f", product.priceWithoutIgv) // Mismo valor
                }
            }
        }
    }

    //--------------------------------------------------
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
            dismissOnClickOutside = false, // <- esto evita el cierre al hacer click fuera
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
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                                            2.dp
                                        )
                                    )
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 350.dp, max = 350.dp)
                                    ) {
                                        itemsIndexed(products) { index, product ->
                                            ProductQuotationListItem(
                                                product = product,
                                                onClick = {
                                                    viewModel.getTariff(product.id)
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
                                EmptySearchQuotationResult()
                            }

                            is ProductSearchState.Error -> {
                                SearchQuotationError((searchState as ProductSearchState.Error).message)
                            }

                            else -> {
                                // Estado Idle
                                if (searchQuery.isNotEmpty()) {
                                    MinimumSearchQuotationInfo()
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
                            SelectedProductQuotationCard(
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = observaciones,
                                    onValueChange = { observaciones = it.uppercase() },
                                    label = { Text("Descripción") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 2,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters // Opcional: teclado en mayúsculas
                                    )
                                )
                            }
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
//                                        if (it.isEmpty() || it.matches(decimalRegex)) {
//                                            priceWithoutIgv = it
//                                            val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
//                                            val withIgvValue = (withoutIgvValue * (1 + valueIgv))
//                                            priceWithIgv = String.format("%.4f", withIgvValue)
//                                        }
                                        //------------------------------------------------
                                        if (it.isEmpty() || it.matches(decimalRegex)) {
                                            priceWithoutIgv = it
                                            val withoutIgvValue = it.toDoubleOrNull() ?: 0.0
                                            // Actualizar precio con IGV según el tipo de afectación
                                            priceWithIgv = when (selectedAffectationType) {
                                                1 -> String.format("%.4f", withoutIgvValue * (1 + valueIgv)) // Gravado
//                                                4 -> "0.00" // Gratuito
                                                else -> String.format("%.4f", withoutIgvValue) // Exonerado o Inafecto
                                            }
                                        }
                                        //--------------------------------------------
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio sin IGV") },
                                    leadingIcon = {
                                        Text(
                                            "S/",
                                            modifier = Modifier.padding(start = 3.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = priceWithIgv,
                                    onValueChange = {
//                                        if (it.isEmpty() || it.matches(decimalRegex)) {
//                                            priceWithIgv = it
//                                            val withIgvValue = it.toDoubleOrNull() ?: 0.0
//                                            val withoutIgvValue = (withIgvValue / (1 + valueIgv))
//                                            priceWithoutIgv = String.format("%.4f", withoutIgvValue)
//                                        }
                                        //---------------------------------------------
                                        if (it.isEmpty() || it.matches(decimalRegex)) {
                                            priceWithIgv = it
                                            val withIgvValue = it.toDoubleOrNull() ?: 0.0

                                            // Actualizar precio sin IGV según el tipo de afectación
                                            priceWithoutIgv = when (selectedAffectationType) {
                                                1 -> String.format("%.4f", withIgvValue / (1 + valueIgv)) // Gravado
//                                                4 -> "0.00" // Gratuito
                                                else -> priceWithIgv // Exonerado o Inafecto (mismo valor)
                                            }
                                        }
                                        //---------------------------------------------
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    label = { Text("Precio con IGV") },
                                    leadingIcon = {
                                        Text(
                                            "S/",
                                            modifier = Modifier.padding(start = 3.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // Selector de tipo de afectación
                            ExposedDropdownMenuBox(
                                expanded = expandedAffectationType,
                                onExpandedChange = { expandedAffectationType = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = affectationTypes.find { it.id == selectedAffectationType }?.name ?: "Gravada",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Tipo de Afectación") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAffectationType)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expandedAffectationType,
                                    onDismissRequest = { expandedAffectationType = false }
                                ) {
                                    affectationTypes.forEach { affectationType ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = affectationType.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                selectedAffectationType = affectationType.id
                                                expandedAffectationType = false

                                                // Actualizar cálculos según el tipo de afectación
//                                                if (affectationType.id == 1) { // Gratuita
//                                                    priceWithoutIgv = "0.00"
//                                                    priceWithIgv = "0.00"
//                                                } else
                                                if (affectationType.id != 1) { // Exonerada o Inafecta
                                                    // Para exonerada o inafecta, mantener el precio pero no aplicar IGV
                                                    val withoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
                                                    priceWithIgv = String.format("%.4f", withoutIgvValue)
                                                } else { // Gravada
                                                    // Restaurar el cálculo normal con IGV
                                                    val withoutIgvValue = priceWithoutIgv.toDoubleOrNull() ?: 0.0
                                                    val withIgvValue = (withoutIgvValue * (1 + valueIgv))
                                                    priceWithIgv = String.format("%.4f", withIgvValue)
                                                }
                                            },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(
                                                            color = getAffectationColor(affectationType.id),
                                                            shape = CircleShape
                                                        )
                                                )
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Resumen de la venta
                            PurchaseQuotationSummary(
                                subtotal = subtotalAfterDiscount, // Base después de descuento
                                igv = igvAmount,                 // IGV (0 si no es gravado)
                                discount = effectiveDiscount,    // Descuento aplicado (efectivo)
                                total = totalAmount,             // Depende del tipo de operación
                                igvPercentage = igvPercentage   // Ej: 18.0
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
                                            stock = product.stock,
                                            priceWithIgv = priceWithIgv.toDoubleOrNull() ?: 0.0,
                                            priceWithoutIgv = priceWithoutIgv.toDoubleOrNull()
                                                ?: 0.0,
                                            productTariffId = product.productTariffId,
                                            typeAffectationId = product.typeAffectationId
                                        )

                                        val operationDetail = IOperationDetail(
                                            id = Random.nextInt(1, Int.MAX_VALUE), // Genera un ID único,
                                            tariff = tariff,
                                            description = observaciones,
                                            typeAffectationId = selectedAffectationType, // Usar el tipo seleccionado, no el del producto
                                            quantity = qtyValue,
                                            unitValue = priceWithoutIgvValue, // Precio unitario sin IGV
                                            unitPrice = priceWithIgvValue,    // Precio unitario con IGV
                                            totalDiscount = effectiveDiscount,
                                            discountPercentage = actualDiscountPercentage,
                                            igvPercentage = if (selectedAffectationType == 1) igvPercentage else 0.0,
                                            totalValue = subtotalAfterDiscount, // Base imponible después de descuento
                                            totalIgv = igvAmount,
                                            totalAmount = totalAmount,
                                            totalToPay = totalAmount,
                                            perceptionPercentage = 0.0,
                                            totalPerception = 0.0
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
private fun ProductQuotationListItem(
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
private fun SelectedProductQuotationCard(
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
                        InfoQuotationRow(
                            label = "Código:",
                            value = product.productCode,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        InfoQuotationRow(
                            label = "Stock:",
                            value = "${product.stock} ${product.unitName}",
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
private fun InfoQuotationRow(
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
private fun EmptySearchQuotationResult() {
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
private fun SearchQuotationError(errorMessage: String) {
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
private fun MinimumSearchQuotationInfo() {
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
private fun PurchaseQuotationSummary(
    subtotal: Double,         // Base imponible después de descuento (subtotalAfterDiscount)
    igv: Double,              // IGV calculado (solo si es gravado)
    discount: Double,         // Descuento aplicado (efectivo)
    total: Double,            // Total según tipo de operación
    igvPercentage: Double    // % de IGV (ej: 18%)
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
            // 1. Subtotal (base después de descuento)
            SummaryQuotationRow(
                label = "Base Imponible:",
                value = subtotal,
                showCurrency = true
            )
            // 2. Descuento (si existe)
            SummaryQuotationRow(
                label = "Descuento:",
                value = -discount, // Mostrar como negativo
                showCurrency = true,
                valueColor = if (discount > 0) Color(0xFFFF5722) else null
            )
            // 3. IGV (solo si es operación gravada)
            if (igv > 0) {
                SummaryQuotationRow(
                    label = "IGV (${igvPercentage}%):",
                    value = igv,
                    showCurrency = true
                )
            }
            // 4. Línea divisoria
            Divider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            // 5. Total
            SummaryQuotationRow(
                label = "TOTAL:",
                value = total,
                showCurrency = true,
                isTotal = true
            )
        }
    }
}

@Composable
private fun SummaryQuotationRow(
    label: String,
    value: Double,
    showCurrency: Boolean = true,
    valueColor: Color? = null,
    isTotal: Boolean = false
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
            style = if (isTotal) MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ) else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (showCurrency) "S/ ${"%.2f".format(value)}"
            else "%.2f".format(value),
            style = if (isTotal) MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ) else MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

data class AffectationType(val id: Int, val name: String)
