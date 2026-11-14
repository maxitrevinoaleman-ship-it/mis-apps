@file:OptIn(ExperimentalLayoutApi::class)

package com.misapps.habitos

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pantalla para crear o editar una misión. */
@Composable
fun EditorHabito(original: Habito?, colorNuevo: Long, alGuardar: (Habito) -> Unit, alCerrar: () -> Unit) {
    BackHandler { alCerrar() }
    var nombre by remember { mutableStateOf(original?.nombre ?: "") }
    var emoji by remember { mutableStateOf(original?.emoji ?: "✅") }
    var metaTexto by remember { mutableStateOf((original?.meta ?: 1).toString()) }
    var unidad by remember { mutableStateOf(original?.unidad ?: "") }
    var dias by remember { mutableStateOf(original?.dias ?: "0123456") }
    var color by remember { mutableStateOf(original?.color ?: colorNuevo) }
    val meta = metaTexto.toIntOrNull()?.coerceIn(1, 1000)
    val valido = nombre.isNotBlank() && meta != null && dias.isNotEmpty()

    Column(Modifier.fillMaxSize().background(Fondo).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = alCerrar) { Text("← Volver", color = Tinta, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(if (original == null) "Nueva misión" else "Editar misión", style = TituloGrande)
            Spacer(Modifier.height(18.dp))
            Etiqueta("Nombre")
            CampoTexto(nombre, { nombre = it.take(40) }, "Ej. Estudiar inglés",
                opciones = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
            Etiqueta("Ícono")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { e ->
                    val on = e == emoji
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(if (on) ColorXp else Carta)
                            .border(if (on) 2.5.dp else 1.5.dp, Tinta, RoundedCornerShape(11.dp)).clickable { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 20.sp) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Etiqueta("Meta al día")
                    CampoTexto(metaTexto, { metaTexto = it.filter(Char::isDigit).take(4) }, "1",
                        opciones = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Column(Modifier.weight(1f)) {
                    Etiqueta("Unidad (opcional)")
                    CampoTexto(unidad, { unidad = it.take(15) }, "vasos, min…")
                }
            }
            Etiqueta("¿Qué días?")
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DIAS_CORTOS.forEachIndexed { i, d ->
                    val on = dias.contains(i.toString())
                    Box(
                        Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp)).background(if (on) Tinta else Carta)
                            .border(2.dp, Tinta, RoundedCornerShape(10.dp))
                            .clickable {
                                val c = i.toString()
                                dias = (if (on) dias.replace(c, "") else dias + c).toList().sorted().joinToString("")
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(d, color = if (on) Carta else Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                }
            }
            Etiqueta("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                COLORES.forEach { c ->
                    val on = c == color
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(c))
                            .border(if (on) 4.dp else 2.dp, Tinta, RoundedCornerShape(10.dp)).clickable { color = c }
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }
        Boton(
            if (original == null) "Agregar misión" else "Guardar cambios",
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            activo = valido,
        ) {
            val base = original ?: Habito(nombre = "", color = color)
            alGuardar(base.copy(nombre = nombre.trim(), emoji = emoji, meta = meta ?: 1, unidad = unidad.trim(), dias = dias, color = color))
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, fontWeight = FontWeight.ExtraBold, color = Tinta, fontSize = 15.sp, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

/** Ajustes: nombre, recordatorio y borrar todo. */
@Composable
fun PantallaAjustes(perfil: Perfil, alCambiar: () -> Unit, alBorrarTodo: () -> Unit, alCerrar: () -> Unit) {
    BackHandler { alCerrar() }
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(perfil.nombre) }
    var recordatorio by remember { mutableStateOf(perfil.recordatorio) }
    var hora by remember { mutableIntStateOf(perfil.horaRecordatorio) }
    var confirmar by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) {
            recordatorio = false
            perfil.recordatorio = false
            Recordatorio.programar(context)
        }
    }

    Column(Modifier.fillMaxSize().background(Fondo).systemBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            TextButton(onClick = alCerrar) { Text("← Volver", color = Tinta, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text("Ajustes", style = TituloGrande)
            Etiqueta("Tu nombre")
            CampoTexto(nombre, {
                nombre = it.take(30)
                if (nombre.isNotBlank()) {
                    perfil.nombre = nombre
                    alCambiar()
                }
            }, "Tu nombre", opciones = KeyboardOptions(capitalization = KeyboardCapitalization.Words))

            Etiqueta("Recordatorio diario")
            Caja(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (recordatorio) "A las ${horaTexto(hora)}" else "Apagado", fontWeight = FontWeight.ExtraBold, color = Tinta, fontSize = 16.sp)
                        Text("Solo te avisa si te falta alguna misión.", color = Suave, fontSize = 13.sp)
                    }
                    Switch(
                        checked = recordatorio,
                        onCheckedChange = {
                            recordatorio = it
                            perfil.recordatorio = it
                            if (it && Build.VERSION.SDK_INT >= 33) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                            Recordatorio.programar(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = ColorHecho, checkedThumbColor = Carta,
                            uncheckedTrackColor = Lila, uncheckedBorderColor = Tinta, uncheckedThumbColor = Tinta,
                        ),
                    )
                }
            }
            if (recordatorio) {
                Spacer(Modifier.height(10.dp))
                Boton("Cambiar hora: ${horaTexto(hora)}", Modifier.fillMaxWidth(), fondo = Carta) {
                    TimePickerDialog(context, { _, h, m ->
                        hora = h * 60 + m
                        perfil.horaRecordatorio = hora
                        Recordatorio.programar(context)
                    }, hora / 60, hora % 60, false).show()
                }
            }

            Spacer(Modifier.height(36.dp))
            Text(
                "Borrar todos mis datos", color = Peligro, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                modifier = Modifier.clip(CircleShape).clickable { confirmar = true }.padding(vertical = 8.dp),
            )
            Text("Borra tus misiones, tu XP y tu nombre de este celular.", color = Suave, fontSize = 13.sp)
            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            containerColor = Carta,
            title = { Text("¿Borrar todo?", fontWeight = FontWeight.Black) },
            text = { Text("Pierdes tus misiones, rachas y nivel. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    alBorrarTodo()
                }) { Text("Borrar todo", color = Peligro, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar", color = Tinta) } },
        )
    }
}

/** Confirmación para quitar una misión. */
@Composable
fun DialogoQuitar(h: Habito, alCerrar: () -> Unit, alConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Carta,
        title = { Text("¿Quitar ${h.emoji} ${h.nombre}?", fontWeight = FontWeight.Black) },
        text = { Text("Tu historial y tu XP se quedan guardados.") },
        confirmButton = { TextButton(onClick = alConfirmar) { Text("Quitar", color = Peligro, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar", color = Tinta) } },
    )
}
