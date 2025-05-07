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
    onClose: () -> Unit, // Nuevo parámetro
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
        gesturesEnabled = true, // Permite cerrar arrastrando
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(
                                brush = ColorGradients.blueVibrant,
//                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            ),
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
                                    .size(100.dp)
                                    .background(Color.White, CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Fibo",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subsidiaryData?.name ?: "Facturación Electrónica",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                                textAlign = TextAlign.Center, // Centra el texto horizontalmente
                                maxLines = 2, // Limita a 2 líneas si es necesario
                                overflow = TextOverflow.Ellipsis // Añade puntos suspensivos si el texto es muy largo
                            )
                        }
                    }

                    // Menu Items
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // PRINCIPAL Section
                        Text(
                            text = "PRINCIPAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                brush = ColorGradients.blueVibrant
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                        MenuItem(icon = Icons.Default.Home, text = "Inicio", onClick = { onMenuItemSelected("Inicio") })
                        MenuItem(icon = Icons.Default.Person, text = "Perfil", onClick = { onMenuItemSelected("Perfil") })

                        Spacer(modifier = Modifier.height(16.dp))

                        // DOCUMENTOS Section
                        Text(
                            text = "DOCUMENTOS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                brush = ColorGradients.blueVibrant
                            ),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                        MenuItem(icon = Icons.Default.Description, text = "Nueva Factura", onClick = { onMenuItemSelected("Nueva Factura") })
                        MenuItem(icon = Icons.Default.Receipt, text = "Nueva Boleta", onClick = { onMenuItemSelected("Nueva Boleta") })
                        MenuItem(icon = Icons.Default.AddCircleOutline, text = "Cotizaciones", onClick = { onMenuItemSelected("Cotizaciones") })
//                        MenuItem(icon = Icons.Default.AddCircle, text = "Nota de salida", onClick = { onMenuItemSelected("Nota de salida") })

                        // Flexible spacer before footer
                        Spacer(modifier = Modifier.weight(0.5f))

                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Footer
                        MenuItem(
                            icon = Icons.Default.ExitToApp,
                            text = "Cerrar sesión",
                            onClick = onLogout,
                            iconGradient = ColorGradients.redPassion,
                            textGradient = ColorGradients.redPassion
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        },
        content = { content() }
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
            .padding(vertical = 4.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
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
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
            }
        }
    }
}
//@Composable
//fun SideMenu(
//    isOpen: Boolean,
//    subsidiaryData: ISubsidiary?,
//    onMenuItemSelected: (String) -> Unit,
//    onLogout: () -> Unit,
//    content: @Composable () -> Unit
//) {
//    val drawerState = rememberDrawerState(initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed)
//
//    LaunchedEffect(isOpen) {
//        if (isOpen) {
//            drawerState.open()
//        } else {
//            drawerState.close()
//        }
//    }
//
//    ModalNavigationDrawer(
//        drawerState = drawerState,
//        gesturesEnabled = isOpen,
//        drawerContent = {
//            ModalDrawerSheet(
//                modifier = Modifier.width(300.dp),
//                drawerContainerColor = MaterialTheme.colorScheme.surface,
//                drawerContentColor = MaterialTheme.colorScheme.onSurface
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxHeight()
//                        .verticalScroll(rememberScrollState())
//                        .padding(horizontal = 16.dp)
//                ) {
//                    // Logo y cabecera
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(200.dp)
//                            .background(
//                                brush = ColorGradients.blueVibrant, // Usando tu gradiente azul vibrante
//                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
//                            )
////                            .background(
////                                brush = Brush.verticalGradient(
////                                    colors = listOf(
////                                        MaterialTheme.colorScheme.primary,
////                                        MaterialTheme.colorScheme.primaryContainer
////                                    )
////                                ),
////                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
////                            )
//                            .padding(16.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                            modifier = Modifier.wrapContentHeight()
//                        ) {
//                            // Logo placeholder (sustituir por tu logo real)
//                            Box(
//                                modifier = Modifier
//                                    .size(80.dp)
//                                    .background(Color.White, CircleShape)
//                                    .padding(12.dp),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                // Reemplaza esto con tu logo real
////                                Text(
////                                    text = "FIBO",
////                                    style = MaterialTheme.typography.headlineMedium,
////                                    color = MaterialTheme.colorScheme.primary,
////                                    fontWeight = FontWeight.Bold
////                                )
//
////                                 Alternativa: usar un logo con Image
//                                 Image(
//                                     painter = painterResource(id = R.drawable.fibo),
//                                     contentDescription = "Logo",
//                                     modifier = Modifier
//                                         .size(80.dp) // Tamaño fijo para el logo
//                                         .background(Color.White, CircleShape)
//                                         .padding(12.dp),
//                                     contentScale = ContentScale.Fit // Ajusta la imagen al contenedor
//                                 )
//                            }
//
//                            Spacer(modifier = Modifier.height(16.dp))
//
//                            Text(
//                                text = "Fibo",
//                                style = MaterialTheme.typography.titleLarge,
//                                color = Color.White,
//                                fontWeight = FontWeight.Bold
//                            )
//
//                            Text(
//                                text = subsidiaryData?.name ?: "Facturación Electrónica",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = Color.White.copy(alpha = 0.8f)
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Sección principal
//                    Text(
//                        text = "PRINCIPAL",
//                        style = MaterialTheme.typography.labelSmall.copy(
//                            brush = ColorGradients.blueVibrant // Usa el gradiente que prefieras
//                        ),
////                        color = MaterialTheme.colorScheme.primary,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Home,
//                        text = "Inicio",
//                        onClick = { onMenuItemSelected("Inicio") }
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Person,
//                        text = "Perfil",
//                        onClick = { onMenuItemSelected("Perfil") }
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    // Sección de documentos
//                    Text(
//                        text = "DOCUMENTOS",
//                        style = MaterialTheme.typography.labelSmall.copy(
//                            brush = ColorGradients.blueVibrant // Usa el gradiente que prefieras
//                        ),
////                        color = MaterialTheme.colorScheme.primary,
//                        fontWeight = FontWeight.Bold,
//                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Description,
//                        text = "Nueva Factura",
//                        onClick = { onMenuItemSelected("Nueva Factura") }
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Receipt,
//                        text = "Nueva Boleta",
//                        onClick = { onMenuItemSelected("Nueva Boleta") }
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.History,
//                        text = "Historial",
//                        onClick = { onMenuItemSelected("Historial") }
//                    )
//
//                    Spacer(modifier = Modifier.weight(1f))
//
//                    Divider(
//                        modifier = Modifier.padding(vertical = 4.dp),
//                        color = MaterialTheme.colorScheme.outlineVariant
//                    )
//
//                    // Logout
//                    MenuItem(
//                        icon = Icons.Default.ExitToApp,
//                        text = "Cerrar sesión",
//                        onClick = onLogout,
//                        iconGradient = ColorGradients.redPassion, // Gradiente para el icono
//                        textGradient = ColorGradients.redPassion  // Gradiente para el texto
////                        contentColor = MaterialTheme.colorScheme.error
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//            }
//        },
//        content = { content() }
//    )
//}
//@Composable
//fun MenuItem(
//    icon: ImageVector,
//    text: String,
//    onClick: () -> Unit,
//    modifier: Modifier = Modifier,
//    iconTint: Color = MaterialTheme.colorScheme.onSurface,
//    textColor: Color = MaterialTheme.colorScheme.onSurface,
//    iconGradient: Brush? = null,
//    textGradient: Brush? = null
//) {
//    Surface(
//        onClick = onClick,
//        modifier = modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
//        color = Color.Transparent
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 12.dp, horizontal = 16.dp)
//        ) {
//            // Icono con soporte para gradiente
//            if (iconGradient != null) {
//                Box(
//                    modifier = Modifier
//                        .size(24.dp)
//                        .graphicsLayer(alpha = 0.99f)
//                        .drawWithContent {
//                            drawContent()
//                            drawRect(brush = iconGradient, blendMode = BlendMode.SrcAtop)
//                        }
//                ) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = text,
//                        modifier = Modifier.size(24.dp),
//                        tint = Color.White // Importante para que el gradiente se vea bien
//                    )
//                }
//            } else {
//                Icon(
//                    imageVector = icon,
//                    contentDescription = text,
//                    modifier = Modifier.size(24.dp),
//                    tint = iconTint
//                )
//            }
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            // Texto con soporte para gradiente
//            if (textGradient != null) {
//                Box(
//                    modifier = Modifier
//                        .graphicsLayer(alpha = 0.99f)
//                        .drawWithContent {
//                            drawContent()
//                            drawRect(brush = textGradient, blendMode = BlendMode.SrcAtop)
//                        }
//                ) {
//                    Text(
//                        text = text,
//                        style = MaterialTheme.typography.bodyLarge,
//                        color = Color.White // Base blanca para mejor contraste
//                    )
//                }
//            } else {
//                Text(
//                    text = text,
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = textColor
//                )
//            }
//        }
//    }
//}
