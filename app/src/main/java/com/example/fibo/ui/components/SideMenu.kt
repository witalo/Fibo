package com.example.fibo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fibo.R
import com.example.fibo.navigation.Screen
import com.example.fibo.utils.ColorGradients
import com.example.fibo.utils.applyTextGradient
import kotlinx.coroutines.launch
@Composable
fun SideMenu(
    isOpen: Boolean,
    onClose: () -> Unit,
    onMenuItemSelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = if (isOpen) DrawerValue.Open else DrawerValue.Closed)

    LaunchedEffect(isOpen) {
        if (isOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    // Logo y cabecera
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                brush = ColorGradients.blueVibrant, // Usando tu gradiente azul vibrante
                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                            )
//                            .background(
//                                brush = Brush.verticalGradient(
//                                    colors = listOf(
//                                        MaterialTheme.colorScheme.primary,
//                                        MaterialTheme.colorScheme.primaryContainer
//                                    )
//                                ),
//                                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
//                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Logo placeholder (sustituir por tu logo real)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White, CircleShape)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Reemplaza esto con tu logo real
//                                Text(
//                                    text = "FIBO",
//                                    style = MaterialTheme.typography.headlineMedium,
//                                    color = MaterialTheme.colorScheme.primary,
//                                    fontWeight = FontWeight.Bold
//                                )

//                                 Alternativa: usar un logo con Image
                                 Image(
                                     painter = painterResource(id = R.drawable.fibo),
                                     contentDescription = "Logo",
                                     modifier = Modifier.fillMaxSize()
                                 )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Fibo",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Facturación Electrónica",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sección principal
                    Text(
                        text = "PRINCIPAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            brush = ColorGradients.blueVibrant // Usa el gradiente que prefieras
                        ),
//                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )

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

                    // Sección de documentos
                    Text(
                        text = "DOCUMENTOS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            brush = ColorGradients.blueVibrant // Usa el gradiente que prefieras
                        ),
//                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                    )

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
                        icon = Icons.Default.History,
                        text = "Historial",
                        onClick = { onMenuItemSelected("Historial") }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Logout
                    MenuItem(
                        icon = Icons.Default.ExitToApp,
                        text = "Cerrar sesión",
                        onClick = onLogout,
                        iconGradient = ColorGradients.redPassion, // Gradiente para el icono
                        textGradient = ColorGradients.redPassion  // Gradiente para el texto
//                        contentColor = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        },
        content = {}
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
            // Icono con soporte para gradiente
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
                        tint = Color.White // Importante para que el gradiente se vea bien
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

            // Texto con soporte para gradiente
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
                        color = Color.White // Base blanca para mejor contraste
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
//fun MenuItem(
//    icon: ImageVector,
//    text: String,
//    onClick: () -> Unit,
//    contentColor: Color = MaterialTheme.colorScheme.onSurface
//) {
//    Surface(
//        onClick = onClick,
//        modifier = Modifier
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
//            Icon(
//                imageVector = icon,
//                contentDescription = text,
//                modifier = Modifier.size(24.dp),
//                tint = contentColor
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Text(
//                text = text,
//                style = MaterialTheme.typography.bodyLarge,
//                color = contentColor
//            )
//        }
//    }
//}
//@Composable
//fun SideMenu(
//    isOpen: Boolean,
//    onClose: () -> Unit,
//    onMenuItemSelected: (String) -> Unit,
//    onLogout: () -> Unit
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
//            ModalDrawerSheet {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(16.dp)
//                ) {
//                    // Header del menú
//                    Text(
//                        text = "Menú",
//                        style = MaterialTheme.typography.headlineMedium,
//                        modifier = Modifier.padding(vertical = 16.dp)
//                    )
//
//                    Divider()
//
//                    // Items del menú
//                    MenuItem(
//                        icon = Icons.Default.Person,
//                        text = "Perfil",
//                        onClick = { onMenuItemSelected("Perfil") }
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Person,
//                        text = "Nueva Factura",
//                        onClick = { onMenuItemSelected("Nueva Factura") }
//                    )
//
//                    MenuItem(
//                        icon = Icons.Default.Person,
//                        text = "Nueva Boleta",
//                        onClick = { onMenuItemSelected("Nueva Boleta") }
//                    )
//
//                    Spacer(modifier = Modifier.weight(1f))
//
//                    Divider()
//
//                    // Logout
//                    MenuItem(
//                        icon = Icons.Default.ExitToApp,
//                        text = "Cerrar sesión",
//                        onClick = onLogout
//                    )
//                }
//            }
//        },
//        content = {}
//    )
//}
//
//
//@Composable
//fun MenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
//    TextButton(
//        onClick = onClick,
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp),
//        colors = ButtonDefaults.textButtonColors(
//            contentColor = MaterialTheme.colorScheme.onSurface
//        )
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = text,
//                modifier = Modifier.size(24.dp)
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Text(text = text, style = MaterialTheme.typography.bodyLarge)
//        }
//    }
//}