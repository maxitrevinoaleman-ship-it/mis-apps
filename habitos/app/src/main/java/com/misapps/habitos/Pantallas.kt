@file:OptIn(ExperimentalLayoutApi::class)

package com.misapps.habitos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale("es", "MX")
private val FECHA_LARGA: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)
private val FECHA_CORTA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", ES)

fun fechaLarga(d: LocalDate) = d.format(FECHA_LARGA).replaceFirstChar { it.uppercase() }
fun fechaCorta(d: LocalDate) = d.format(FECHA_CORTA).replace(".", "")

/** Campo de texto con borde grueso. */
@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    pista: String,
    modifier: Modifier = Modifier,
    opciones: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
) {
    TextField(
        value = valor,
        onValueChange = alCambiar,
        placeholder = { Text(pista, color = Suave) },
        singleLine = true,
        keyboardOptions = opciones,
        keyboardActions = acciones,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Tinta),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Carta, unfocusedContainerColor = Carta,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Tinta,
        ),
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(BORDE, Tinta, RoundedCornerShape(12.dp)),
    )
}

// ================================================================== TARJETA DEL JUGADOR

@Composable
fun TarjetaJugador(nombre: String, j: Jugador, alAjustes: () -> Unit) {
    Caja(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Caja(Modifier.size(78.dp), fondo = ColorXp, radio = 20.dp, sombra = 2.dp) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("Nivel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Tinta)
                    Text("${j.nivel}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Tinta, lineHeight = 32.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (nombre.isBlank()) "Jugador" else nombre, fontSize = 13.sp, color = Suave, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "⚙", fontSize = 20.sp, color = Tinta2,
                        modifier = Modifier.clip(CircleShape).clickable { alAjustes() }.padding(horizontal = 6.dp),
                    )
                }
                Text(j.titulo, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Tinta)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(18.dp).clip(CircleShape).background(Fondo)
                        .border(2.dp, Tinta, CircleShape)
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(j.xpNivel / XP_POR_NIVEL.toFloat())
                            .background(ColorXp)
                    )
                }
                Text(
                    "${j.xpNivel} / $XP_POR_NIVEL XP para nivel ${j.nivel + 1}",
                    fontSize = 12.sp, color = Tinta2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ================================================================== HOY

@Composable
fun PantallaHoy(
    estado: List<EstadoHabito>,
    alMarcar: (EstadoHabito, Int) -> Unit,   // nueva cantidad
    alIrAHabitos: () -> Unit,
) {
    val toca = estado.filter { it.tocaHoy }
    val descansan = estado.filter { !it.tocaHoy }
    val hechos = toca.count { it.cumplido }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(fechaLarga(LocalDate.now()), color = Suave, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Misiones de hoy", style = TituloGrande)
                }
                if (toca.isNotEmpty()) Text("$hechos de ${toca.size}", fontWeight = FontWeight.ExtraBold, color = Tinta2, fontSize = 16.sp)
            }
        }
        if (estado.isEmpty()) {
            item {
                Caja(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Todavía no tienes misiones.", color = Tinta2, fontSize = 16.sp)
                        Spacer(Modifier.height(14.dp))
                        Boton("Crear mi primera misión", Modifier.fillMaxWidth()) { alIrAHabitos() }
                    }
                }
            }
        } else if (toca.isEmpty()) {
            item {
                Caja(Modifier.fillMaxWidth()) {
                    Text("Hoy no te toca ninguna misión. Día de descanso 😎", color = Tinta2, fontSize = 16.sp, modifier = Modifier.padding(20.dp))
                }
            }
        }
        if (toca.isNotEmpty() && hechos == toca.size) {
            item {
                Caja(Modifier.fillMaxWidth().padding(bottom = 12.dp), fondo = ColorHecho) {
                    Text(
                        "🏆 ¡Completaste todas las misiones de hoy!", color = Carta, fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp, modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }
        items(toca, key = { it.habito.id }) { e -> TarjetaMision(e, alMarcar) }
        if (descansan.isNotEmpty()) {
            item {
                Text("Descansan hoy", fontWeight = FontWeight.ExtraBold, color = Tinta2, fontSize = 16.sp,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    descansan.forEach { e ->
                        Text(
                            "${e.habito.emoji} ${e.habito.nombre}", fontWeight = FontWeight.SemiBold, color = Tinta2, fontSize = 14.sp,
                            modifier = Modifier.clip(CircleShape).background(Carta).border(2.dp, Tinta, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Cada misión cumplida te da +$XP_POR_MISION XP. Cada $XP_POR_NIVEL XP subes de nivel.",
                color = Tinta2, fontSize = 13.sp, modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

@Composable
private fun TarjetaMision(e: EstadoHabito, alMarcar: (EstadoHabito, Int) -> Unit) {
    val h = e.habito
    val color = colorDe(h)
    val unidad = if (h.unidad.isNotBlank()) " ${h.unidad}" else ""
    Caja(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        // Franja de color con el emoji, la racha y el sello
        Row(
            Modifier.fillMaxWidth().background(color).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Carta)
                    .border(BORDE, Tinta, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(h.emoji, fontSize = 22.sp) }
            Spacer(Modifier.weight(1f))
            if (e.cumplido) {
                Text(
                    "¡Hecho!", color = ColorHecho, fontWeight = FontWeight.Black, fontSize = 16.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Carta)
                        .border(2.5.dp, ColorHecho, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            if (e.racha > 0) {
                Text(
                    "🔥 ${e.racha}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Tinta,
                    modifier = Modifier.clip(CircleShape).background(Carta).border(2.dp, Tinta, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(BORDE).background(Tinta))
        Column(Modifier.padding(14.dp)) {
            Text(h.nombre, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Tinta)
            Text(
                when {
                    h.meta > 1 -> "${e.hoy} de ${h.meta}$unidad"
                    e.cumplido -> "Completada"
                    else -> "Pendiente"
                },
                color = Suave, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            if (h.meta in 2..12) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..h.meta).forEach { n ->
                        Box(
                            Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp))
                                .background(if (n <= e.hoy) color else Fondo)
                                .border(2.dp, Tinta, RoundedCornerShape(6.dp))
                                .clickable { alMarcar(e, if (n == e.hoy) n - 1 else n) }
                        )
                    }
                }
            } else if (h.meta > 12) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(16.dp).clip(CircleShape).background(Fondo).border(2.dp, Tinta, CircleShape)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((e.hoy.toFloat() / h.meta).coerceIn(0f, 1f)).background(color))
                }
            }
            Spacer(Modifier.height(12.dp))
            when {
                h.meta == 1 -> Boton(
                    if (e.cumplido) "Deshacer" else "Completar · +$XP_POR_MISION XP",
                    Modifier.fillMaxWidth(), fondo = if (e.cumplido) Carta else ColorXp,
                ) { alMarcar(e, if (e.cumplido) 0 else 1) }

                e.cumplido -> Boton("Reiniciar", Modifier.fillMaxWidth(), fondo = Carta) { alMarcar(e, 0) }

                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Boton("−", Modifier.width(64.dp), fondo = Carta) { alMarcar(e, (e.hoy - 1).coerceAtLeast(0)) }
                    Boton("+1${unidad.removeSuffix("s")}", Modifier.weight(1f)) { alMarcar(e, e.hoy + 1) }
                }
            }
        }
    }
}

// ================================================================== MIS HÁBITOS (lista)

@Composable
fun PantallaHabitos(habitos: List<Habito>, alNuevo: () -> Unit, alEditar: (Habito) -> Unit, alQuitar: (Habito) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp)) {
        item {
            Text(
                if (habitos.isEmpty()) "Tus misiones" else "Tus ${habitos.size} ${if (habitos.size == 1) "misión" else "misiones"}",
                style = TituloGrande, modifier = Modifier.padding(bottom = 12.dp),
            )
            Boton("+ Nueva misión", Modifier.fillMaxWidth().padding(bottom = 16.dp)) { alNuevo() }
        }
        items(habitos, key = { it.id }) { h ->
            Caja(Modifier.fillMaxWidth().padding(bottom = 12.dp), alTocar = { alEditar(h) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colorDe(h))
                            .border(2.dp, Tinta, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(h.emoji, fontSize = 22.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Tinta)
                        val dias = if (h.dias.length == 7) "Todos los días" else h.dias.map { DIAS_CORTOS[it.digitToInt()] }.joinToString(", ")
                        Text(
                            (if (h.meta > 1) "${h.meta} ${h.unidad} al día · " else "") + dias,
                            color = Suave, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        "Quitar", color = Peligro, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { alQuitar(h) }.padding(8.dp),
                    )
                }
            }
        }
        item {
            Text("Toca una misión para editarla.", color = Suave, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ================================================================== PROGRESO

private fun nivelColor(v: Double): Color = when {
    v <= 0.0 -> RAMPA[0]
    v < 0.25 -> RAMPA[1]
    v < 0.5 -> RAMPA[2]
    v < 0.75 -> RAMPA[3]
    v < 1.0 -> RAMPA[4]
    else -> RAMPA[5]
}

@Composable
fun PantallaProgreso(j: Jugador, estado: List<EstadoHabito>, s: Estadisticas) {
    val top = estado.maxByOrNull { it.racha }
    val pcts = s.porHabito.mapNotNull { it.porcentaje30 }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 40.dp)) {
        item {
            Text("Tu progreso", style = TituloGrande, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                Ficha(Modifier.weight(1f), "%,d".format(j.xp), "XP en total", ColorXp)
                Ficha(Modifier.weight(1f), "${top?.racha ?: 0}", if (top != null && top.racha > 0) "días · ${top.habito.nombre}" else "días de racha", Carta)
                Ficha(Modifier.weight(1f), if (pcts.isEmpty()) "—" else "${pcts.average().toInt()}%", "en 30 días", Carta)
            }
        }
        item {
            Caja(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cada semana", style = TituloCaja)
                    Text("% de misiones cumplidas", color = Suave, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    BarrasSemanas(s.semanas)
                }
            }
        }
        item {
            Caja(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Últimas 12 semanas", style = TituloCaja)
                    Spacer(Modifier.height(12.dp))
                    Calendario(s)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                        Text("Nada ", color = Suave, fontSize = 12.sp)
                        RAMPA.forEach { c ->
                            Box(Modifier.padding(horizontal = 2.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(c))
                        }
                        Text(" Todo", color = Suave, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Caja(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Misión por misión", style = TituloCaja)
                    Spacer(Modifier.height(8.dp))
                    if (s.porHabito.isEmpty()) Text("Cuando tengas misiones, aquí verás cómo te va.", color = Suave)
                    s.porHabito.forEach { p ->
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(colorDe(p.habito))
                                    .border(2.dp, Tinta, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) { Text(p.habito.emoji, fontSize = 18.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.habito.nombre, fontWeight = FontWeight.ExtraBold, color = Tinta, fontSize = 15.sp)
                                Text("🔥 ${p.racha} · récord ${p.mejorRacha}", color = Suave, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(p.porcentaje30?.let { "$it%" } ?: "—", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Tinta)
                                Text("30 días", color = Suave, fontSize = 11.sp)
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth().padding(top = 6.dp).height(10.dp).clip(CircleShape).background(Fondo)
                                .border(1.5.dp, Tinta, CircleShape)
                        ) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(((p.porcentaje30 ?: 0) / 100f).coerceIn(0f, 1f)).background(colorDe(p.habito)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Ficha(modifier: Modifier, valor: String, texto: String, fondo: Color) {
    Caja(modifier, fondo = fondo, radio = 14.dp, sombra = 2.dp) {
        Column(Modifier.padding(10.dp)) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Tinta, maxLines = 1)
            Text(texto, fontSize = 12.sp, color = Tinta2, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BarrasSemanas(semanas: List<Pair<LocalDate, Int?>>) {
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val izq = 30.dp.toPx()
        val ancho = size.width - izq
        val alto = size.height - 2.dp.toPx()
        val n = semanas.size
        val hueco = 5.dp.toPx()
        val anchoBarra = ((ancho - hueco * (n - 1)) / n).coerceAtMost(30.dp.toPx())
        val paso = ancho / n
        // guías de 50% y 100%
        listOf(0.5f, 1f).forEach { f ->
            val y = alto * (1 - f)
            drawLine(Lila, Offset(izq, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        semanas.forEachIndexed { i, (_, pct) ->
            if (pct == null) return@forEachIndexed
            val h = (alto * pct / 100f).coerceAtLeast(2.dp.toPx())
            val x = izq + paso * i + (paso - anchoBarra) / 2
            val color = if (i == n - 1) ColorXp else RAMPA[3]
            drawRoundRect(color, Offset(x, alto - h), Size(anchoBarra, h), CornerRadius(5.dp.toPx()))
            drawRoundRect(Tinta, Offset(x, alto - h), Size(anchoBarra, h), CornerRadius(5.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
        drawLine(Tinta, Offset(izq, alto), Offset(size.width, alto), strokeWidth = 2.5.dp.toPx())
    }
    Row(Modifier.fillMaxWidth().padding(start = 30.dp, top = 4.dp)) {
        semanas.forEachIndexed { i, (inicio, _) ->
            Text(
                when {
                    i == semanas.size - 1 -> "Esta"
                    i % 3 == 0 -> fechaCorta(inicio)
                    else -> ""
                },
                fontSize = 10.sp, color = Suave, maxLines = 1, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Calendario(s: Estadisticas) {
    // Columnas = semanas, filas = lunes..domingo
    val primero = s.general.first().first
    val relleno = primero.dayOfWeek.value - DayOfWeek.MONDAY.value
    val celdas: List<Pair<LocalDate, Double?>?> = List(relleno) { null } + s.general
    val columnas = celdas.chunked(7)
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 6.dp)) {
            listOf("L", "", "M", "", "V", "", "D").forEach { Text(it, fontSize = 10.sp, color = Suave, modifier = Modifier.height(18.dp)) }
        }
        columnas.forEach { col ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 4.dp)) {
                col.forEach { celda ->
                    val forma = RoundedCornerShape(5.dp)
                    when {
                        celda == null -> Spacer(Modifier.size(18.dp))
                        celda.second == null -> Box(Modifier.size(18.dp).clip(forma).border(1.dp, Lila, forma))
                        else -> Box(
                            Modifier.size(18.dp).clip(forma).background(nivelColor(celda.second!!))
                                .border(if (celda.first == s.hoy) 2.5.dp else 1.dp, if (celda.first == s.hoy) Tinta else Tinta.copy(alpha = 0.2f), forma)
                        )
                    }
                }
            }
        }
    }
}
