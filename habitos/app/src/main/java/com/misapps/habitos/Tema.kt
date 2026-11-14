package com.misapps.habitos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Estilo "videojuego": colores vivos, bordes gruesos y sombras sólidas
val Fondo = Color(0xFFEFEAFF)
val Carta = Color(0xFFFFFFFF)
val Tinta = Color(0xFF16131F)
val Tinta2 = Color(0xFF4B4560)
val Suave = Color(0xFF7A7390)
val ColorXp = Color(0xFFFFD23F)
val ColorHecho = Color(0xFF22C47A)
val Lila = Color(0xFFD9D0FF)
val Peligro = Color(0xFFE5484D)
val RAMPA = listOf(Color(0xFFE6E0FA), Color(0xFFC9BEF5), Color(0xFFA08DEB), Color(0xFF7A62DD), Color(0xFF5A3FC4), Color(0xFF3B2394))

val BORDE = 2.5.dp

fun colorDe(h: Habito) = Color(h.color)

@Composable
fun TemaHabitos(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Tinta,
            onPrimary = Carta,
            background = Fondo,
            onBackground = Tinta,
            surface = Carta,
            onSurface = Tinta,
            surfaceVariant = Lila,
            onSurfaceVariant = Tinta2,
            surfaceContainerHigh = Carta,
            secondaryContainer = ColorXp,
            onSecondaryContainer = Tinta,
            outline = Tinta,
            error = Peligro,
        ),
        content = content,
    )
}

/** Caja con borde grueso y sombra sólida abajo a la derecha. */
@Composable
fun Caja(
    modifier: Modifier = Modifier,
    fondo: Color = Carta,
    radio: Dp = 18.dp,
    sombra: Dp = 4.dp,
    alTocar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(radio)
    Box(modifier.padding(end = sombra, bottom = sombra)) {
        Box(Modifier.matchParentSize().offset(sombra, sombra).background(Tinta, forma))
        Column(
            Modifier.fillMaxWidth().clip(forma).background(fondo).border(BORDE, Tinta, forma)
                .then(if (alTocar != null) Modifier.clickable { alTocar() } else Modifier),
            content = contenido,
        )
    }
}

/** Botón grueso estilo juego. */
@Composable
fun Boton(
    texto: String,
    modifier: Modifier = Modifier,
    fondo: Color = ColorXp,
    colorTexto: Color = Tinta,
    activo: Boolean = true,
    alTocar: () -> Unit,
) {
    Caja(
        modifier = modifier,
        fondo = if (activo) fondo else Lila,
        radio = 14.dp,
        sombra = if (activo) 3.dp else 0.dp,
        alTocar = if (activo) alTocar else null,
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(texto, color = if (activo) colorTexto else Suave, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

val TituloGrande = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, color = Tinta, letterSpacing = (-0.3).sp, lineHeight = 32.sp)
val TituloCaja = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Tinta)
