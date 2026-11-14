@file:OptIn(ExperimentalMaterial3Api::class)

package com.max.misfinanzas

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt

private val FORMA = RoundedCornerShape(16.dp)

@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(Superficie, FORMA)
            .border(1.dp, Borde, FORMA)
            .padding(16.dp),
        content = contenido,
    )
}

@Composable
fun TituloTarjeta(texto: String, extra: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, color = Texto2, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (extra != null) Text(extra, color = Suave, fontSize = 12.sp)
    }
}

@Composable
fun Marca(color: Color, tam: Int = 10) {
    Box(Modifier.size(tam.dp).clip(RoundedCornerShape(3.dp)).background(color))
}

// ------------------------------------------------------------------ filtro de periodo

@Composable
fun FiltroPeriodo(actual: Periodo, alCambiar: (Periodo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Periodo.entries.forEach { p ->
            FilterChip(
                selected = p == actual,
                onClick = { alCambiar(p) },
                label = { Text(p.etiqueta, fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = Texto2,
                    selectedContainerColor = Texto,
                    selectedLabelColor = Fondo,
                ),
            )
        }
    }
}

// ------------------------------------------------------------------ RESUMEN

@Composable
fun PantallaResumen(r: Resumen, periodo: Periodo) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                Text("Balance", color = Suave, fontSize = 12.sp)
                Text(dinero(r.balance, conSigno = true), color = Texto, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Text(
                    (if (r.balance >= 0) "▲ Te queda dinero " else "▼ Gastaste más de lo que ganaste ") + periodo.frase,
                    color = Suave, fontSize = 12.sp,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Kpi(Modifier.weight(1f), "Ganaste", r.ingresos, ColorIngreso,
                    "${r.cantIngresos} ${if (r.cantIngresos == 1) "ganancia" else "ganancias"}")
                Kpi(Modifier.weight(1f), "Gastaste", r.gastos, ColorGasto,
                    "${r.cantGastos} ${if (r.cantGastos == 1) "gasto" else "gastos"}")
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                TituloTarjeta("Tu dinero, movimiento por movimiento", periodo.frase)
                GraficaSaldo(r.serie, periodo)
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                TituloTarjeta("¿En qué gastas?")
                Barras(r.gastosPorCategoria, ColorGasto, periodo)
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                TituloTarjeta("¿De dónde ganas?")
                Barras(r.ingresosPorCategoria, ColorIngreso, periodo)
            }
        }
    }
}

@Composable
private fun Kpi(modifier: Modifier, titulo: String, valor: Double, color: Color, sub: String) {
    Tarjeta(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Marca(color)
            Spacer(Modifier.width(6.dp))
            Text(titulo, color = Suave, fontSize = 12.sp)
        }
        Text(dinero(valor), color = Texto, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Text(sub, color = Suave, fontSize = 12.sp)
    }
}

@Composable
private fun Barras(datos: List<Pair<String, Double>>, color: Color, periodo: Periodo) {
    if (datos.isEmpty()) {
        Text("Nada ${periodo.frase}", color = Suave, fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)
        return
    }
    val maximo = datos.maxOf { it.second }
    val total = datos.sumOf { it.second }
    datos.forEach { (categoria, monto) ->
        Column(Modifier.padding(bottom = 11.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                Text(categoria, color = Texto, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${dinero(monto)} · ${(monto / total * 100).roundToInt()}%", color = Texto2, fontSize = 13.sp)
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Superficie2)) {
                Box(
                    Modifier.fillMaxWidth((monto / maximo).toFloat().coerceIn(0.02f, 1f))
                        .height(8.dp).clip(CircleShape).background(color)
                )
            }
        }
    }
}

// ------------------------------------------------------------------ TABLAS (gastos / ganancias)

private enum class Columna { FECHA, DESCRIPCION, MONTO }

@Composable
fun PantallaTabla(tipo: String, movimientos: List<Movimiento>, periodo: Periodo, alBorrar: (Movimiento) -> Unit) {
    var buscar by rememberSaveable(tipo) { mutableStateOf("") }
    var columna by rememberSaveable(tipo) { mutableStateOf(Columna.FECHA) }
    var ascendente by rememberSaveable(tipo) { mutableStateOf(false) }
    val color = colorDe(tipo)
    val nombre = if (tipo == TIPO_GASTO) "gastos" else "ganancias"

    val filas = remember(movimientos, periodo, buscar, columna, ascendente, tipo) {
        val base = movimientos.filter {
            !it.borrado && it.tipo == tipo && periodo.incluye(it) &&
                (buscar.isBlank() || "${it.descripcion} ${it.categoria}".contains(buscar.trim(), ignoreCase = true))
        }
        val ordenadas = when (columna) {
            Columna.FECHA -> base.sortedBy { it.fecha }
            Columna.DESCRIPCION -> base.sortedBy { it.descripcion.ifBlank { it.categoria }.lowercase() }
            Columna.MONTO -> base.sortedBy { it.monto }
        }
        if (ascendente) ordenadas else ordenadas.reversed()
    }

    fun ordenarPor(c: Columna) {
        if (columna == c) ascendente = !ascendente
        else {
            columna = c
            ascendente = c == Columna.DESCRIPCION
        }
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        item {
            OutlinedTextField(
                value = buscar,
                onValueChange = { buscar = it },
                placeholder = { Text("Buscar en $nombre…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (buscar.isNotEmpty()) IconButton(onClick = { buscar = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = camposColores(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Encabezado("Fecha", Columna.FECHA, columna, ascendente, Modifier.weight(0.9f)) { ordenarPor(it) }
                Encabezado("Descripción", Columna.DESCRIPCION, columna, ascendente, Modifier.weight(1.4f)) { ordenarPor(it) }
                Encabezado("Monto", Columna.MONTO, columna, ascendente, Modifier.weight(1f), alinearFin = true) { ordenarPor(it) }
                Spacer(Modifier.width(40.dp))
            }
            HorizontalDivider(color = Borde)
        }
        if (filas.isEmpty()) {
            item {
                Text(
                    if (buscar.isNotBlank()) "Nada coincide con tu búsqueda" else "Sin $nombre ${periodo.frase}",
                    color = Suave, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                )
            }
        }
        items(filas, key = { it.uuid }) { m ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(0.9f)) {
                    Text(diaTexto(m.fecha), color = Texto2, fontSize = 13.sp)
                    Text(horaTexto(m.fecha), color = Suave, fontSize = 11.sp)
                }
                Column(Modifier.weight(1.4f).padding(end = 6.dp)) {
                    Text(m.descripcion.ifBlank { "—" }, color = Texto, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        m.categoria, color = Texto2, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp).background(Superficie2, RoundedCornerShape(50))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(6.dp))
                    Text(dinero(m.monto), color = Texto, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                IconButton(onClick = { alBorrar(m) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Suave, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider(color = Borde)
        }
        if (filas.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Text("Total (${filas.size})", color = Texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                    Text(
                        dinero(filas.sumOf { it.monto }), color = Texto, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(40.dp))
                }
            }
        }
    }
}

@Composable
private fun Encabezado(
    texto: String, c: Columna, actual: Columna, ascendente: Boolean, modifier: Modifier,
    alinearFin: Boolean = false, alTocar: (Columna) -> Unit,
) {
    val flecha = if (c == actual) (if (ascendente) " ↑" else " ↓") else ""
    Text(
        texto + flecha,
        color = if (c == actual) Texto else Suave,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        textAlign = if (alinearFin) TextAlign.End else TextAlign.Start,
        modifier = modifier.clickable { alTocar(c) }.padding(vertical = 4.dp),
    )
}

@Composable
fun camposColores() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Texto2,
    unfocusedBorderColor = Borde,
    focusedContainerColor = Fondo,
    unfocusedContainerColor = Fondo,
    cursorColor = Texto,
    focusedLabelColor = Texto2,
    unfocusedLabelColor = Suave,
    focusedPlaceholderColor = Suave,
    unfocusedPlaceholderColor = Suave,
    focusedLeadingIconColor = Suave,
    unfocusedLeadingIconColor = Suave,
)

// ------------------------------------------------------------------ DIÁLOGOS

@Composable
fun DialogoAgregar(tipo: String, categorias: List<String>, alCerrar: () -> Unit, alGuardar: (Movimiento) -> Unit) {
    val context = LocalContext.current
    val esGasto = tipo == TIPO_GASTO
    var montoTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(categorias.firstOrNull() ?: "Otros") }
    var descripcion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    val monto = montoTexto.replace(",", "").trim().toDoubleOrNull()
    val valido = monto != null && monto > 0

    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Superficie,
        title = { Text(if (esGasto) "Nuevo gasto" else "Nueva ganancia", color = Texto) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = montoTexto,
                    onValueChange = { montoTexto = it },
                    label = { Text("Monto (" + simboloMoneda + ")") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = camposColores(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Categoría", color = Suave, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categorias) { c ->
                        FilterChip(
                            selected = c == categoria,
                            onClick = { categoria = c },
                            label = { Text(c) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Texto2,
                                selectedContainerColor = colorDe(tipo),
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(if (esGasto) "¿En qué? (ej. tacos)" else "¿De qué? (ej. 3 cajas)") },
                    singleLine = true,
                    colors = camposColores(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, anio, mes, dia -> fecha = LocalDate.of(anio, mes + 1, dia) },
                            fecha.year, fecha.monthValue - 1, fecha.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (fecha == LocalDate.now()) "Fecha: hoy" else "Fecha: " + fecha.format(FORMATO_FECHA_LARGA),
                        color = Texto2,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valido,
                onClick = {
                    val cuando = if (fecha == LocalDate.now()) LocalDateTime.now().withNano(0) else fecha.atTime(12, 0)
                    alGuardar(
                        Movimiento(
                            uuid = UUID.randomUUID().toString(),
                            tipo = tipo,
                            monto = redondear(monto ?: 0.0),
                            categoria = categoria,
                            descripcion = descripcion.trim(),
                            fecha = cuando,
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorDe(tipo), contentColor = Color.White),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Texto2) } },
    )
}

@Composable
fun DialogoBorrar(m: Movimiento, alCerrar: () -> Unit, alConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Superficie,
        title = { Text("¿Borrar?", color = Texto) },
        text = {
            Text(
                "${m.descripcion.ifBlank { m.categoria }} · ${dinero(m.monto)} · ${fechaTexto(m.fecha)}",
                color = Texto2,
            )
        },
        confirmButton = {
            Button(
                onClick = alConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPeligro, contentColor = Color.White),
            ) { Text("Borrar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Texto2) } },
    )
}
