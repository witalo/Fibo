package com.example.fibo.utils

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

object ColorGradients {
    // Gradientes Azules
    val blueVibrant = Brush.linearGradient(
        colors = listOf(
            Color(0xFF00B4DB),
            Color(0xFF0083B0),
            Color(0xFF0062A3)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val blueOcean = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4FACFE),
            Color(0xFF00F2FE)
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val blueDeep = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1CB5E0),
            Color(0xFF000046))
    )
    val blueButtonGradient = Brush.linearGradient(
        colors = listOf(
        Color(0xFF1085F8),  // Azul claro
        Color(0xFF1174D5),  // Azul medio
        Color(0xFF1069BE)   // Azul oscuro
        ),
        start = Offset(100f, 0f),  // Ajuste para que el degradado no sea muy brusco
        end = Offset(0f, 100f)
    )

    val blueButton = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2078C5),
            Color(0xFF1E71B9),
            Color(0xFF135893),
            Color(0xFF093B67),
            Color(0xFF000046))
    )

    // Gradientes Rojos
    val redPassion = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF416C),
            Color(0xFFFF4B2B))
    )

    val redWine = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF8E0E00),
            Color(0xFF1F1C18))
    )

    // Gradientes Verdes
    val greenNature = Brush.linearGradient(
        colors = listOf(
            Color(0xFF11998E),
            Color(0xFF38EF7D))
    )

    val greenEmerald = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF348F50),
            Color(0xFF56B4D3))
    )

    // Gradientes Morados
    val purpleDream = Brush.linearGradient(
        colors = listOf(
            Color(0xFFA18CD1),
            Color(0xFFFBC2EB))
    )

    val purpleDeep = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF654EA3),
            Color(0xFFEAAFC8))
    )

    // Gradientes Naranjas
    val orangeSunset = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF8008),
            Color(0xFFFFC837))
    )

    val orangeFire = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF46B45),
            Color(0xFFEEA849))
    )

    // Gradientes Grises
    val grayModern = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEFEFBB),
            Color(0xFFD4D3DD))
    )

    val grayDark = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF232526),
            Color(0xFF414345))
    )

    // Gradientes Especiales
    val rainbow = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF0000), // Rojo
            Color(0xFFFFA500), // Naranja
            Color(0xFFFFFF00), // Amarillo
            Color(0xFF00FF00), // Verde
            Color(0xFF0000FF), // Azul
            Color(0xFF4B0082), // Índigo
            Color(0xFFEE82EE)  // Violeta
        ),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    val goldLuxury = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD4AF37),
            Color(0xFFF5D020),
            Color(0xFFBF953F))
    )
}
// En tu archivo de utils
@SuppressLint("SuspiciousModifierThen")
fun Modifier.applyTextGradient(gradient: Brush): Modifier = this.then(
    graphicsLayer(alpha = 0.99f).drawWithContent {
        drawContent()
        drawRect(brush = gradient, blendMode = BlendMode.SrcAtop)
    }
)