@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.max.misfinanzas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Lo que se eligió en la bienvenida. */
data class Configuracion(
    val nombre: String,
    val moneda: String,
    val saldoInicial: Double,
    val categoriasGasto: List<String>,
    val categoriasIngreso: List<String>,
)

/**
 * Primera vez que se abre la app: nombre, moneda, cuánto dinero tiene y sus categorías.
 */
@Composable
fun Bienvenida(alTerminar: (Configuracion) -> Unit) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var moneda by rememberSaveable { mutableStateOf("MXN") }
    var saldoTexto by rememberSaveable { mutableStateOf("") }
    val gastos = remember { mutableStateListOf<String>().apply { addAll(Categorias.inicialesGasto) } }
    val ingresos = remember { mutableStateListOf<String>().apply { addAll(Categorias.inicialesIngreso) } }

    BackHandler(enabled = paso > 0) { paso-- }

    Column(
        Modifier.fillMaxSize().background(Fondo).systemBarsPadding().imePadding().padding(24.dp)
    ) {
        // Puntos de avance
        if (paso > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 28.dp)) {
                (1..3).forEach { i ->
                    Box(
                        Modifier.height(4.dp).width(if (i == paso) 28.dp else 14.dp).clip(CircleShape)
                            .background(if (i <= paso) Texto else Borde)
                    )
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (paso) {
                0 -> {
                    Spacer(Modifier.height(60.dp))
                    Box(
                        Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(Superficie)
                            .border(1.dp, Borde, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("\$", color = Texto, fontSize = 46.sp, fontFamily = FontFamily.Serif)
                    }
                    Spacer(Modifier.height(28.dp))
                    Text("Mis Finanzas", color = Texto, fontSize = 36.sp, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Anota lo que gastas y lo que ganas, y mira a dónde se va tu dinero. " +
                            "Todo se queda guardado en tu celular.",
                        color = Texto2, fontSize = 17.sp, lineHeight = 25.sp,
                    )
                }

                1 -> {
                    Titulo("¿Cómo te llamas?", "Así te saluda la app.")
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it.take(30) },
                        placeholder = { Text("Tu nombre") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = { if (nombre.isNotBlank()) paso = 2 }),
                        colors = camposColores(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                2 -> {
                    Titulo("¿Qué moneda usas?", "Puedes cambiarla después en Ajustes.")
                    MONEDAS.forEach { m ->
                        val elegida = m.codigo == moneda
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (elegida) Superficie2 else Superficie)
                                .border(1.dp, if (elegida) Texto2 else Borde, RoundedCornerShape(12.dp))
                                .clickable { moneda = m.codigo }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(m.nombre, color = Texto, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Text("${m.codigo}  ${m.simbolo}", color = Suave, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("¿Cuánto dinero tienes ahorita? (opcional)", color = Texto2, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = saldoTexto,
                        onValueChange = { saldoTexto = it.take(12) },
                        placeholder = { Text("0.00") },
                        prefix = { Text(monedaDe(moneda).simbolo + " ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = camposColores(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    Titulo("Elige tus categorías", "Toca para quitar o poner. Puedes agregar las tuyas.")
                    Text("En qué gastas", color = Texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    SelectorCategorias(Categorias.sugeridasGasto, gastos, ColorGasto)
                    Spacer(Modifier.height(24.dp))
                    Text("De dónde te llega dinero", color = Texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    SelectorCategorias(Categorias.sugeridasIngreso, ingresos, ColorIngreso)
                }
            }
        }

        val puedeSeguir = when (paso) {
            1 -> nombre.isNotBlank()
            3 -> gastos.isNotEmpty() && ingresos.isNotEmpty()
            else -> true
        }
        Button(
            onClick = {
                if (paso < 3) paso++
                else alTerminar(
                    Configuracion(
                        nombre = nombre.trim(),
                        moneda = moneda,
                        saldoInicial = saldoTexto.replace(",", "").trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                        categoriasGasto = gastos.toList(),
                        categoriasIngreso = ingresos.toList(),
                    )
                )
            },
            enabled = puedeSeguir,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Texto, contentColor = Fondo),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
            Text(
                when (paso) {
                    0 -> "Empezar"
                    3 -> "Listo, " + nombre.trim().substringBefore(" ")
                    else -> "Siguiente"
                },
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
        }
        if (paso == 3 && !puedeSeguir) {
            Text(
                "Elige al menos una categoría de cada tipo.",
                color = Suave, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun Titulo(texto: String, sub: String) {
    Text(texto, color = Texto, fontSize = 30.sp, fontFamily = FontFamily.Serif, lineHeight = 36.sp)
    Spacer(Modifier.height(8.dp))
    Text(sub, color = Suave, fontSize = 15.sp)
    Spacer(Modifier.height(24.dp))
}

/** Chips para elegir categorías + campo para agregar una propia. */
@Composable
fun SelectorCategorias(sugeridas: List<String>, elegidas: MutableList<String>, color: Color) {
    var nueva by remember { mutableStateOf("") }
    val todas = (sugeridas + elegidas).distinct()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        todas.forEach { c ->
            val on = c in elegidas
            FilterChip(
                selected = on,
                onClick = { if (on) elegidas.remove(c) else elegidas.add(c) },
                label = { Text(c) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Superficie,
                    labelColor = Texto2,
                    selectedContainerColor = color,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = nueva,
            onValueChange = { nueva = it.take(20) },
            placeholder = { Text("Otra categoría…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = camposColores(),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = {
                val limpia = nueva.trim().replace("|", "/")
                if (limpia.isNotEmpty() && elegidas.none { it.equals(limpia, ignoreCase = true) }) elegidas.add(limpia)
                nueva = ""
            },
            enabled = nueva.isNotBlank(),
        ) { Text("Agregar", color = Texto) }
    }
}
