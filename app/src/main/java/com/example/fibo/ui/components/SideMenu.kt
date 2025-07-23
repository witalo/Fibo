package com.example.fibo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.model.ISubsidiary
import com.example.fibo.navigation.Screen
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.applyTextGradient
import kotlinx.coroutines.launch


@Composable
fun SideMenu(
    isOpen: Boolean,
    onClose: () -> Unit,
    subsidiaryData: ISubsidiary?,
    onMenuItemSelected: (String) -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed)

    LaunchedEffect(isOpen) {
        if (isOpen) drawerState.open() else drawerState.close()
    }

    // Manejar el cierre por gestos o clic fuera
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            onClose()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Header Section - Fijo en la parte superior
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // Reducido un poco para dar más espacio al contenido
                            .background(brush = ColorGradients.blueVibrant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.fibo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(80.dp) // Reducido para ahorrar espacio
                                    .background(Color.White, CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Fibo",
                                style = MaterialTheme.typography.titleMedium, // Reducido
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subsidiaryData?.name ?: "Facturación Electrónica",
                                style = MaterialTheme.typography.bodyMedium, // Reducido
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Contenido scrollable - Ocupa el espacio restante
                    Column(
                        modifier = Modifier
                            .weight(1f) // Toma todo el espacio disponible
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // PRINCIPAL Section
                        SectionHeader(text = "PRINCIPAL")
                        MenuItem(
                            icon = Icons.Default.Home,
                            text = "Inicio",
                            onClick = { onMenuItemSelected("Inicio") }
                        )
                        MenuItem(
                            icon = Icons.Default.Person,
                            text = "Perfil",
                            onClick = { onMenuItemSelected("Perfil") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // DOCUMENTOS Section
                        SectionHeader(text = "DOCUMENTOS")
                        MenuItem(
                            icon = Icons.Default.Description,
                            text = "Nueva Factura",
                            onClick = { onMenuItemSelected("Nueva Factura") }
                        )
                        MenuItem(
                            icon = Icons.Default.Receipt,
                            text = "Nueva Boleta",
                            onClick = { onMenuItemSelected("Nueva Boleta") }
                        )
                        MenuItem(
                            icon = Icons.Default.AddCircleOutline,
                            text = "Cotizaciones",
                            onClick = { onMenuItemSelected("Cotizaciones") }
                        )
                        MenuItem(
                            icon = Icons.Default.AddCircleOutline,
                            text = "Productos",
                            onClick = { onMenuItemSelected("Productos") }
                        )
                        MenuItem(
                            icon = Icons.Default.ShoppingCart,
                            text = "Compras",
                            onClick = { onMenuItemSelected("Compras") }
                        )
                        MenuItem(
                            icon = Icons.Default.AddCircle,
                            text = "Nota de salida",
                            onClick = { onMenuItemSelected("Nota de salida") }
                        )
                         MenuItem(
                            icon = Icons.Default.AddCircle,
                            text = "Nueva Guía",
                            onClick = { onMenuItemSelected("Nueva Guía") }
                        )
                        MenuItem(
                            icon = Icons.Default.List,
                            text = "Guías",
                            onClick = { onMenuItemSelected("Guías") }
                        )
                        MenuItem(
                            icon = Icons.Default.Analytics,
                            text = "Reporte",
                            onClick = { onMenuItemSelected("Reporte") }
                        )
                        MenuItem(
                            icon = Icons.Default.Analytics,
                            text = "Reporte Pagos",
                            onClick = { onMenuItemSelected("Reporte Pagos") }
                        )
                        // Espacio adicional para asegurar que el último item sea visible
                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    // Footer - Fijo en la parte inferior
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        MenuItem(
                            icon = Icons.Default.ExitToApp,
                            text = "Cerrar sesión",
                            onClick = onLogout,
                            iconGradient = ColorGradients.redPassion,
                            textGradient = ColorGradients.redPassion
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        content = { content() }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            brush = ColorGradients.blueVibrant
        ),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
    )
}

@Composable
fun MenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconGradient: Brush? = null,
    textGradient: Brush? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp), // Reducido para optimizar espacio
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp) // Añadido para mejor UX
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp) // Reducido ligeramente
        ) {
            if (iconGradient != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = iconGradient, blendMode = BlendMode.SrcAtop)
                        }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = text,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (textGradient != null) {
                Box(
                    modifier = Modifier
                        .graphicsLayer(alpha = 0.99f)
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = textGradient, blendMode = BlendMode.SrcAtop)
                        }
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium, // Ajustado para consistencia
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}