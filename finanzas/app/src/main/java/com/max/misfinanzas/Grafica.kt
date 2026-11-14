package com.max.misfinanzas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Márgenes de la gráfica (espacio para las etiquetas)
private val MARGEN_IZQ = 50.dp
private val MARGEN_DER = 10.dp
private val MARGEN_ARRIBA = 10.dp
private val MARGEN_ABAJO = 24.dp

/**
 * La línea que sube con cada ganancia y baja con cada gasto.
 * Toca o arrastra el dedo sobre ella para ver cada movimiento.
 */
@Composable
fun GraficaSaldo(serie: List<PuntoSaldo>, periodo: Periodo) {
    if (serie.size <= 1) {
        Text(
            "Todavía no hay movimientos ${periodo.frase}.\nAgrega uno con el botón de abajo.",
            color = Suave, fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
            textAlign = TextAlign.Center,
        )
        return
    }

    var seleccion by remember(serie) { mutableStateOf<Int?>(null) }
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(color = Suave, fontSize = 11.sp)
    val (minimo, maximo, marcas) = remember(serie) {
        escalaBonita(serie.minOf { it.saldo }, serie.maxOf { it.saldo })
    }

    Column {
        // Detalle del punto tocado
        val punto = seleccion?.let { serie.getOrNull(it) }
        Row(Modifier.fillMaxWidth().height(40.dp).padding(bottom = 6.dp)) {
            if (punto == null) {
                Text("Toca la gráfica para ver cada movimiento", color = Suave, fontSize = 12.sp)
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        punto.mov?.let { fechaTexto(it.fecha) } ?: "Antes de empezar ${periodo.frase}",
                        color = Suave, fontSize = 12.sp,
                    )
                    val m = punto.mov
                    Text(
                        if (m == null) "Saldo inicial"
                        else "${m.descripcion.ifBlank { m.categoria }}  ${dinero(m.conSigno, conSigno = true)}",
                        color = if (m == null) Texto2 else colorDe(m.tipo),
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Saldo", color = Suave, fontSize = 12.sp)
                    Text(dinero(punto.saldo), color = Texto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(210.dp)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(serie) {
                        fun indice(x: Float): Int {
                            val izq = MARGEN_IZQ.toPx()
                            val ancho = size.width - izq - MARGEN_DER.toPx()
                            val paso = ancho / (serie.size - 1)
                            return ((x - izq) / paso).roundToInt().coerceIn(0, serie.size - 1)
                        }
                        awaitEachGesture {
                            val toque = awaitFirstDown()
                            seleccion = indice(toque.position.x)
                            while (true) {
                                val evento = awaitPointerEvent()
                                val dedo = evento.changes.firstOrNull() ?: break
                                if (!dedo.pressed) break
                                seleccion = indice(dedo.position.x)
                                dedo.consume()
                            }
                        }
                    }
            ) {
                val izq = MARGEN_IZQ.toPx()
                val der = MARGEN_DER.toPx()
                val arriba = MARGEN_ARRIBA.toPx()
                val abajo = size.height - MARGEN_ABAJO.toPx()
                val pasoX = (size.width - izq - der) / (serie.size - 1)
                fun x(i: Int) = izq + i * pasoX
                fun y(v: Double) = (arriba + (maximo - v) / (maximo - minimo) * (abajo - arriba)).toFloat()

                // Rejilla y eje Y
                for (m in marcas) {
                    val yy = y(m)
                    if (m == 0.0) {
                        drawLine(
                            Color(0xFF6B6A64), Offset(izq, yy), Offset(size.width - der, yy),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        )
                    } else {
                        drawLine(Borde, Offset(izq, yy), Offset(size.width - der, yy), strokeWidth = 1.dp.toPx())
                    }
                    val etiqueta = medidor.measure(dineroCorto(m), estiloEje)
                    drawText(
                        etiqueta,
                        topLeft = Offset(izq - 8.dp.toPx() - etiqueta.size.width, yy - etiqueta.size.height / 2f),
                    )
                }

                // Área suave y línea
                val linea = Path()
                serie.forEachIndexed { i, p ->
                    if (i == 0) linea.moveTo(x(i), y(p.saldo)) else linea.lineTo(x(i), y(p.saldo))
                }
                val area = Path()
                area.moveTo(x(0), abajo)
                serie.forEachIndexed { i, p -> area.lineTo(x(i), y(p.saldo)) }
                area.lineTo(x(serie.size - 1), abajo)
                area.close()
                drawPath(area, Brush.verticalGradient(listOf(Texto.copy(alpha = 0.10f), Color.Transparent), arriba, abajo))
                drawPath(
                    linea, Texto2,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Línea vertical del punto tocado
                seleccion?.let { i ->
                    drawLine(Suave, Offset(x(i), arriba), Offset(x(i), abajo), strokeWidth = 1.dp.toPx())
                }

                // Puntos: azul = ganancia, naranja = gasto, gris = inicio
                val radio = if (serie.size > 60) 0f else 4.dp.toPx()
                serie.forEachIndexed { i, p ->
                    val activo = seleccion == i
                    val r = if (activo) 6.dp.toPx() else radio
                    if (r > 0f) {
                        val color = p.mov?.let { colorDe(it.tipo) } ?: Suave
                        drawCircle(Superficie, r + 2.dp.toPx(), Offset(x(i), y(p.saldo)))
                        drawCircle(color, r, Offset(x(i), y(p.saldo)))
                    }
                }

                // Fechas abajo
                val primera = serie[1].mov?.fecha
                val ultima = serie.last().mov?.fecha
                if (primera != null && ultima != null) {
                    val t1 = medidor.measure(diaTexto(primera), estiloEje)
                    val t2 = medidor.measure(diaTexto(ultima), estiloEje)
                    val yTexto = size.height - t1.size.height
                    drawText(t1, topLeft = Offset(izq, yTexto))
                    drawText(t2, topLeft = Offset(size.width - der - t2.size.width, yTexto))
                }
            }
        }

        // Leyenda
        Row(Modifier.padding(top = 10.dp)) {
            Leyenda(ColorIngreso, "Ganancia (sube)")
            Spacer(Modifier.width(14.dp))
            Leyenda(ColorGasto, "Gasto (baja)")
            Spacer(Modifier.width(14.dp))
            Leyenda(Suave, "Inicio")
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = Texto2, fontSize = 12.sp)
    }
}
