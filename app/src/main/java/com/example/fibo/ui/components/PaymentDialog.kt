package com.example.fibo.ui.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fibo.model.IPayment
import com.example.fibo.model.PaymentMethods
import com.example.fibo.model.PaymentSummary
import com.example.fibo.utils.getCurrentFormattedDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    isVisible: Boolean,
    totalAmount: Double,
    payments: List<IPayment>,
    paymentSummary: PaymentSummary,
    currentOperationDate: String,
    onDismiss: () -> Unit,
    onAddPayment: (IPayment) -> Unit,
    onRemovePayment: (Int) -> Unit,
    onFinalizeSale: () -> Unit
) {
    if (!isVisible) return

    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethods.AVAILABLE_METHODS[0]) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentNote by remember { mutableStateOf("") }
    var paymentDate by remember { mutableStateOf(currentOperationDate) }
    var expanded by remember { mutableStateOf(false) }

    // Actualizar fecha cuando cambia el método de pago
    LaunchedEffect(selectedPaymentMethod) {
        paymentDate = if (selectedPaymentMethod.isCredit) {
            getCurrentFormattedDate()
        } else {
            currentOperationDate
        }
        paymentNote = ""
    }

    // Calcular el monto sugerido y actualizar cuando cambie el resumen de pagos
    val suggestedAmount = maxOf(0.0, paymentSummary.remaining)

    LaunchedEffect(paymentSummary.remaining) {
        // Solo actualizar si el campo está vacío o si hay cambios en los pagos
        if (paymentAmount.isEmpty() && suggestedAmount > 0) {
            paymentAmount = String.format("%.2f", suggestedAmount)
        } else if (suggestedAmount > 0 && paymentAmount.toDoubleOrNull() != suggestedAmount) {
            // Si el monto actual excede lo que queda por pagar, ajustarlo
            val currentAmount = paymentAmount.toDoubleOrNull() ?: 0.0
            if (currentAmount > suggestedAmount) {
                paymentAmount = String.format("%.2f", suggestedAmount)
            } else {
                // Actualizar automáticamente cuando se eliminen pagos
                paymentAmount = String.format("%.2f", suggestedAmount)
            }
        }
    }

    // Función para limpiar campos después de agregar pago
    fun clearPaymentFields() {
        paymentAmount = ""
        if (!selectedPaymentMethod.isCredit) {
            paymentNote = ""
        }
        // Recalcular el monto sugerido
        val newRemaining = maxOf(0.0, paymentSummary.remaining)
        if (newRemaining > 0) {
            paymentAmount = String.format("%.2f", newRemaining)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header con gradiente azul mejorado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1976D2),
                                    Color(0xFF2196F3),
                                    Color(0xFF42A5F5)
                                )
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Forma de Pago",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Total: ${String.format("%.2f", totalAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección: Agregar Medio de Pago
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            "Agregar Pago",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Dropdown de métodos de pago
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.weight(1.2f)
                            ) {
                                OutlinedTextField(
                                    value = selectedPaymentMethod.name,
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Método", style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    PaymentMethods.AVAILABLE_METHODS.forEach { method ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    method.name,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            },
                                            onClick = {
                                                selectedPaymentMethod = method
                                                expanded = false
                                                // Recalcular monto cuando cambie el método
                                                if (suggestedAmount > 0) {
                                                    // Ajustar el monto si excede lo pendiente
                                                    val currentAmount = paymentAmount.toDoubleOrNull() ?: 0.0
                                                    if (currentAmount > suggestedAmount) {
                                                        paymentAmount = String.format("%.2f", suggestedAmount)
                                                    } else if (paymentAmount.isEmpty()) {
                                                        paymentAmount = String.format("%.2f", suggestedAmount)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Campo de importe con validación visual
                            OutlinedTextField(
                                value = paymentAmount,
                                onValueChange = {
                                    paymentAmount = it
                                },
                                label = { Text("Monto", style = MaterialTheme.typography.labelSmall) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                isError = paymentAmount.toDoubleOrNull()?.let { it > paymentSummary.remaining } ?: false,
                                supportingText = if (paymentSummary.remaining > 0) {
                                    {
                                        Text(
                                            "Debe: ${String.format("%.2f", paymentSummary.remaining)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else null
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nota O Fecha según el tipo de pago
                        if (selectedPaymentMethod.isCredit) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Fecha de Cuota",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                DateSelectorLimit(
                                    currentDate = paymentDate,
                                    onDateSelected = { paymentDate = it },
                                    daysBefore = 0,
                                    daysAfter = 365
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = paymentNote,
                                    onValueChange = { paymentNote = it },
                                    label = { Text("Nota (opcional)", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    maxLines = 1,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    "Fecha: $currentOperationDate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Validación del monto ingresado
                        val currentAmount = paymentAmount.toDoubleOrNull() ?: 0.0
                        val isValidAmount = currentAmount > 0 && currentAmount <= paymentSummary.remaining
                        val exceedsRemaining = currentAmount > paymentSummary.remaining

                        // Mostrar mensaje de error si excede el monto
                        if (exceedsRemaining && paymentSummary.remaining > 0) {
                            Text(
                                "⚠️ El monto no puede ser mayor a ${String.format("%.2f", paymentSummary.remaining)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF44336),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Botón agregar con gradiente azul mejorado
                        Button(
                            onClick = {
                                val amount = paymentAmount.toDoubleOrNull() ?: 0.0
                                if (amount > 0 && amount <= paymentSummary.remaining) {
                                    val payment = IPayment(
                                        wayPay = selectedPaymentMethod.id,
                                        amount = amount,
                                        note = if (selectedPaymentMethod.isCredit) "" else paymentNote,
                                        paymentDate = paymentDate
                                    )
                                    onAddPayment(payment)
                                    clearPaymentFields()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(
                                    brush = if (isValidAmount) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF1976D2),
                                                Color(0xFF2196F3)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF9E9E9E),
                                                Color(0xFFBDBDBD)
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent
                            ),
                            enabled = isValidAmount
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Agregar Pago",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lista de pagos agregados
                if (payments.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Encabezado de tabla
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MÉTODO", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(1.1f))
                                Text("MONTO", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(0.6f))
                                Text("FECHA", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(0.7f))
                                Text("INFO", fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.weight(0.6f))
                                Spacer(modifier = Modifier.width(32.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Items de pago
                            payments.forEach { payment ->
                                val method = PaymentMethods.getMethodById(payment.wayPay)
                                PaymentItem(
                                    payment = payment,
                                    methodName = method?.name?.take(12) ?: "Desc.",
                                    onRemove = {
                                        onRemovePayment(payment.id)
                                        // Recalcular monto después de eliminar
                                        if (suggestedAmount > 0) {
                                            paymentAmount = String.format("%.2f", suggestedAmount)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Resumen de pago
                PaymentSummaryCard(paymentSummary = paymentSummary)

                Spacer(modifier = Modifier.height(16.dp))

                // Botones finales con gradientes mejorados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botón Cancelar
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            "Cancelar",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Botón Finalizar con gradiente verde/azul
                    Button(
                        onClick = onFinalizeSale,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .background(
                                brush = if (paymentSummary.isComplete) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50),
                                            Color(0xFF66BB6A)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF9E9E9E),
                                            Color(0xFFBDBDBD)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = paymentSummary.isComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Finalizar Venta",
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

@Composable
private fun PaymentItem(
    payment: IPayment,
    methodName: String,
    onRemove: () -> Unit
) {
    val method = PaymentMethods.getMethodById(payment.wayPay)
    val isCredit = method?.isCredit ?: false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = methodName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "%.2f".format(payment.amount),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = payment.paymentDate.takeLast(5),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.7f)
        )
        Text(
            text = if (isCredit) "CUOTA" else (payment.note.take(8).ifEmpty { "-" }),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PaymentSummaryCard(paymentSummary: PaymentSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "Resumen de Pago",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            SummaryRow("Total", paymentSummary.totalAmount)
            SummaryRow("Pagado", paymentSummary.totalPaid)

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            SummaryRow(
                "Diferencia",
                paymentSummary.remaining,
                isHighlight = true,
                color = when {
                    paymentSummary.remaining < 0 -> Color(0xFF4CAF50) // Verde
                    paymentSummary.remaining > 0 -> Color(0xFFF44336) // Rojo
                    else -> Color(0xFF757575) // Gris
                }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    amount: Double,
    isHighlight: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isHighlight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = String.format("%.2f", amount),
            style = if (isHighlight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun PaymentDialog(
//    isVisible: Boolean,
//    totalAmount: Double,
//    payments: List<IPayment>,
//    paymentSummary: PaymentSummary,
//    currentOperationDate: String, // Fecha de la operación actual
//    onDismiss: () -> Unit,
//    onAddPayment: (IPayment) -> Unit,
//    onRemovePayment: (Int) -> Unit,
//    onFinalizeSale: () -> Unit
//) {
//    if (!isVisible) return
//
//    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethods.AVAILABLE_METHODS[0]) }
//    var paymentAmount by remember { mutableStateOf("") }
//    var paymentNote by remember { mutableStateOf("") }
//    var paymentDate by remember { mutableStateOf(currentOperationDate) }
//    var expanded by remember { mutableStateOf(false) }
//
//    // Actualizar fecha cuando cambia el método de pago
//    LaunchedEffect(selectedPaymentMethod) {
//        paymentDate = if (selectedPaymentMethod.isCredit) {
//            // Para crédito, usar fecha actual como sugerencia
//            getCurrentFormattedDate()
//        } else {
//            // Para contado, usar la fecha de la operación
//            currentOperationDate
//        }
//        // Limpiar nota al cambiar método
//        paymentNote = ""
//    }
//
//    // Calcular el monto sugerido (lo que falta por pagar)
//    val suggestedAmount = maxOf(0.0, paymentSummary.remaining)
//
//    LaunchedEffect(paymentSummary.remaining) {
//        if (paymentAmount.isEmpty() && suggestedAmount > 0) {
//            paymentAmount = String.format("%.2f", suggestedAmount)
//        }
//    }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(
//            dismissOnClickOutside = false,
//            usePlatformDefaultWidth = false
//        )
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(0.96f)
//                .heightIn(max = 650.dp),
//            shape = RoundedCornerShape(12.dp),
//            tonalElevation = 6.dp,
//            color = MaterialTheme.colorScheme.surface
//        ) {
//            Column(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .fillMaxWidth()
//                    .verticalScroll(rememberScrollState())
//            ) {
//                // Header más compacto
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            brush = ColorGradients.blueButtonGradient,
//                            shape = RoundedCornerShape(8.dp)
//                        )
//                        .padding(12.dp)
//                ) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            "Forma de Pago",
//                            style = MaterialTheme.typography.titleMedium,
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold
//                        )
//                        IconButton(
//                            onClick = onDismiss,
//                            modifier = Modifier
//                                .size(28.dp)
//                                .background(
//                                    Color.White.copy(alpha = 0.2f),
//                                    CircleShape
//                                )
//                        ) {
//                            Icon(
//                                Icons.Default.Close,
//                                contentDescription = "Cerrar",
//                                tint = Color.White,
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(14.dp))
//
//                // Sección: Agregar Medio de Pago - más compacta
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = CardDefaults.cardColors(
//                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
//                    )
//                ) {
//                    Column(
//                        modifier = Modifier.padding(12.dp)
//                    ) {
//                        Text(
//                            "Agregar Pago",
//                            style = MaterialTheme.typography.titleSmall,
//                            color = MaterialTheme.colorScheme.primary,
//                            fontWeight = FontWeight.SemiBold
//                        )
//
//                        Spacer(modifier = Modifier.height(10.dp))
//
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            // Dropdown de métodos de pago
//                            ExposedDropdownMenuBox(
//                                expanded = expanded,
//                                onExpandedChange = { expanded = it },
//                                modifier = Modifier.weight(1.2f)
//                            ) {
//                                OutlinedTextField(
//                                    value = selectedPaymentMethod.name,
//                                    onValueChange = { },
//                                    readOnly = true,
//                                    label = { Text("Método", style = MaterialTheme.typography.labelSmall) },
//                                    trailingIcon = {
//                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
//                                    },
//                                    modifier = Modifier
//                                        .fillMaxWidth()
//                                        .menuAnchor(),
//                                    shape = RoundedCornerShape(6.dp),
//                                    textStyle = MaterialTheme.typography.bodySmall
//                                )
//
//                                ExposedDropdownMenu(
//                                    expanded = expanded,
//                                    onDismissRequest = { expanded = false }
//                                ) {
//                                    PaymentMethods.AVAILABLE_METHODS.forEach { method ->
//                                        DropdownMenuItem(
//                                            text = {
//                                                Text(
//                                                    method.name,
//                                                    style = MaterialTheme.typography.bodySmall
//                                                )
//                                            },
//                                            onClick = {
//                                                selectedPaymentMethod = method
//                                                expanded = false
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//
//                            // Campo de importe
//                            OutlinedTextField(
//                                value = paymentAmount,
//                                onValueChange = { paymentAmount = it },
//                                label = { Text("Monto", style = MaterialTheme.typography.labelSmall) },
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                modifier = Modifier.weight(0.8f),
//                                shape = RoundedCornerShape(6.dp),
//                                textStyle = MaterialTheme.typography.bodySmall
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.height(10.dp))
//
//                        // Nota O Fecha según el tipo de pago
//                        if (selectedPaymentMethod.isCredit) {
//                            // SOLO PARA CRÉDITO: Selector de fecha
//                            Column(modifier = Modifier.fillMaxWidth()) {
//                                Text(
//                                    "Fecha de Cuota",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                                    fontWeight = FontWeight.Medium
//                                )
//
//                                Spacer(modifier = Modifier.height(6.dp))
//
//                                DateSelectorLimit(
//                                    currentDate = paymentDate,
//                                    onDateSelected = { paymentDate = it },
//                                    daysBefore = 0,
//                                    daysAfter = 365
//                                )
//                            }
//                        } else {
//                            // PARA CONTADO: Campo de nota
//                            Column(modifier = Modifier.fillMaxWidth()) {
//                                OutlinedTextField(
//                                    value = paymentNote,
//                                    onValueChange = { paymentNote = it },
//                                    label = { Text("Nota", style = MaterialTheme.typography.labelSmall) },
//                                    modifier = Modifier.fillMaxWidth(),
//                                    shape = RoundedCornerShape(6.dp),
//                                    maxLines = 1,
//                                    textStyle = MaterialTheme.typography.bodySmall
//                                )
//
//                                Spacer(modifier = Modifier.height(4.dp))
//
//                                Text(
//                                    "Fecha: $currentOperationDate",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        // Botón agregar más compacto
//                        Button(
//                            onClick = {
//                                val amount = paymentAmount.toDoubleOrNull() ?: 0.0
//                                if (amount > 0) {
//                                    val payment = IPayment(
//                                        wayPay = selectedPaymentMethod.id,
//                                        amount = amount,
//                                        note = if (selectedPaymentMethod.isCredit) "" else paymentNote,
//                                        paymentDate = paymentDate
//                                    )
//                                    onAddPayment(payment)
//                                    paymentAmount = ""
//                                    if (!selectedPaymentMethod.isCredit) {
//                                        paymentNote = ""
//                                    }
//                                    val remaining = maxOf(0.0, paymentSummary.remaining - amount)
//                                    if (remaining > 0) {
//                                        paymentAmount = String.format("%.2f", remaining)
//                                    }
//                                }
//                            },
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .height(40.dp),
//                            shape = RoundedCornerShape(6.dp),
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.primary
//                            )
//                        ) {
//                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
//                            Spacer(modifier = Modifier.width(6.dp))
//                            Text("Agregar",
////                                style = MaterialTheme.typography.labelMedium
//                                style = MaterialTheme.typography.labelMedium.copy(
//                                    color = Color.White,
//                                    fontWeight = FontWeight.SemiBold  // Texto en negrita
//                                )
//                            )
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Lista de pagos agregados - más compacta
//                if (payments.isNotEmpty()) {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(8.dp),
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
//                        )
//                    ) {
//                        Column(modifier = Modifier.padding(10.dp)) {
//                            // Encabezado de tabla más compacto
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .background(
//                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
//                                        RoundedCornerShape(6.dp)
//                                    )
//                                    .padding(6.dp),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Text("MÉTODO", fontWeight = FontWeight.Bold,
//                                    style = MaterialTheme.typography.labelSmall,
//                                    modifier = Modifier.weight(1.1f))
//                                Text("MONTO", fontWeight = FontWeight.Bold,
//                                    style = MaterialTheme.typography.labelSmall,
//                                    modifier = Modifier.weight(0.6f))
//                                Text("FECHA", fontWeight = FontWeight.Bold,
//                                    style = MaterialTheme.typography.labelSmall,
//                                    modifier = Modifier.weight(0.7f))
//                                Text("INFO", fontWeight = FontWeight.Bold,
//                                    style = MaterialTheme.typography.labelSmall,
//                                    modifier = Modifier.weight(0.6f))
//                                Spacer(modifier = Modifier.width(32.dp))
//                            }
//
//                            Spacer(modifier = Modifier.height(6.dp))
//
//                            // Items de pago
//                            payments.forEach { payment ->
//                                val method = PaymentMethods.getMethodById(payment.wayPay)
//                                PaymentItem(
//                                    payment = payment,
//                                    methodName = method?.name?.take(12) ?: "Desc.", // Limitar texto
//                                    onRemove = { onRemovePayment(payment.id) }
//                                )
//                                Spacer(modifier = Modifier.height(3.dp))
//                            }
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                // Resumen de pago más compacto
//                PaymentSummaryCard(paymentSummary = paymentSummary)
//
//                Spacer(modifier = Modifier.height(14.dp))
//
//                // Botones finales más compactos
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(10.dp)
//                ) {
//                    OutlinedButton(
//                        onClick = onDismiss,
//                        modifier = Modifier
//                            .weight(1f)
//                            .height(42.dp),
//                        shape = RoundedCornerShape(6.dp)
//                    ) {
//                        Text("Cancelar", style = MaterialTheme.typography.labelMedium)
//                    }
//
//                    Button(
//                        onClick = onFinalizeSale,
//                        modifier = Modifier
//                            .weight(1f)
//                            .height(42.dp),
//                        shape = RoundedCornerShape(6.dp),
//                        enabled = paymentSummary.isComplete,
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.secondary
//                        )
//                    ) {
//                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
//                        Spacer(modifier = Modifier.width(6.dp))
//                        Text("Finalizar",
////                            style = MaterialTheme.typography.labelMedium
//                            style = MaterialTheme.typography.labelMedium.copy(
//                                color = Color.White,
//                                fontWeight = FontWeight.SemiBold  // Texto en negrita
//                            )
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun PaymentItem(
//    payment: IPayment,
//    methodName: String,
//    onRemove: () -> Unit
//) {
//    val method = PaymentMethods.getMethodById(payment.wayPay)
//    val isCredit = method?.isCredit ?: false
//
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(
//                MaterialTheme.colorScheme.surface,
//                RoundedCornerShape(6.dp)
//            )
//            .padding(6.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Text(
//            text = methodName,
//            style = MaterialTheme.typography.bodySmall,
//            modifier = Modifier.weight(1.1f),
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis
//        )
//        Text(
//            text = "%.2f".format(payment.amount),
//            style = MaterialTheme.typography.bodySmall,
//            modifier = Modifier.weight(0.6f),
//            fontWeight = FontWeight.Medium
//        )
//        Text(
//            text = payment.paymentDate.takeLast(5), // Solo mostrar MM-dd
//            style = MaterialTheme.typography.labelSmall,
//            modifier = Modifier.weight(0.7f)
//        )
//        Text(
//            text = if (isCredit) "CUOTA" else (payment.note.take(8).ifEmpty { "-" }),
//            style = MaterialTheme.typography.labelSmall,
//            modifier = Modifier.weight(0.6f),
//            maxLines = 1,
//            overflow = TextOverflow.Ellipsis,
//            color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
//        )
//        IconButton(
//            onClick = onRemove,
//            modifier = Modifier.size(28.dp)
//        ) {
//            Icon(
//                Icons.Default.Delete,
//                contentDescription = "Eliminar",
//                tint = MaterialTheme.colorScheme.error,
//                modifier = Modifier.size(16.dp)
//            )
//        }
//    }
//}
//
//@Composable
//private fun PaymentSummaryCard(paymentSummary: PaymentSummary) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape = RoundedCornerShape(8.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
//        )
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//            Text(
//                "Resumen",
//                style = MaterialTheme.typography.titleSmall,
//                color = MaterialTheme.colorScheme.secondary,
//                fontWeight = FontWeight.Bold
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            SummaryRow("Total", paymentSummary.totalAmount)
//            SummaryRow("Pagado", paymentSummary.totalPaid)
//
//            Divider(modifier = Modifier.padding(vertical = 6.dp), thickness = 0.5.dp)
//
//            SummaryRow(
//                "Diferencia",
//                paymentSummary.remaining,
//                isHighlight = true,
//                color = if (paymentSummary.remaining < 0) Color.Green
//                else if (paymentSummary.remaining > 0) Color.Red
//                else Color.Gray
//            )
//        }
//    }
//}
//
//@Composable
//private fun SummaryRow(
//    label: String,
//    amount: Double,
//    isHighlight: Boolean = false,
//    color: Color = MaterialTheme.colorScheme.onSurface
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = label,
//            style = if (isHighlight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
//            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
//        )
//        Text(
//            text = String.format("%.2f", amount),
//            style = if (isHighlight) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
//            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
//            color = color
//        )
//    }
//}
