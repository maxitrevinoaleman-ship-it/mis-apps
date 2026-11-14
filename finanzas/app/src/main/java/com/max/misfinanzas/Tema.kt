package com.max.misfinanzas

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Negro formal (igual que Mis Finanzas en la computadora)
val Fondo = Color(0xFF000000)
val Superficie = Color(0xFF0C0C0C)
val Superficie2 = Color(0xFF161616)
val Borde = Color(0xFF262626)
val Texto = Color(0xFFF0EFEC)
val Texto2 = Color(0xFFC3C2B7)
val Suave = Color(0xFF8B8A82)
val ColorIngreso = Color(0xFF3987E5)
val ColorGasto = Color(0xFFD95926)
val ColorPeligro = Color(0xFFFF6B6B)
val ColorConectado = Color(0xFF3DDC97)

fun colorDe(tipo: String) = if (tipo == TIPO_INGRESO) ColorIngreso else ColorGasto

@Composable
fun TemaFinanzas(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Texto,
            onPrimary = Fondo,
            secondary = Texto2,
            background = Fondo,
            onBackground = Texto,
            surface = Superficie,
            onSurface = Texto,
            surfaceVariant = Superficie2,
            onSurfaceVariant = Texto2,
            surfaceContainerHigh = Superficie,
            secondaryContainer = Superficie2,
            onSecondaryContainer = Texto,
            outline = Borde,
            outlineVariant = Borde,
            error = ColorPeligro,
        ),
        content = content,
    )
}
