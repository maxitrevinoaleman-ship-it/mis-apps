@file:OptIn(ExperimentalLayoutApi::class)

package com.misapps.habitos

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Inicio(val nombre: String, val habitos: List<Sugerencia>, val recordatorio: Boolean, val hora: Int)

@Composable
fun Bienvenida(alTerminar: (Inicio) -> Unit) {
    val context = LocalContext.current
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var nombre by rememberSaveable { mutableStateOf("") }
    val elegidas = remember { mutableStateListOf(SUGERENCIAS[0], SUGERENCIAS[1], SUGERENCIAS[2]) }
    var recordatorio by rememberSaveable { mutableStateOf(true) }
    var hora by rememberSaveable { mutableIntStateOf(20 * 60) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) recordatorio = false
    }

    BackHandler(enabled = paso > 0) { paso-- }

    Column(Modifier.fillMaxSize().background(Fondo).systemBarsPadding().imePadding().padding(22.dp)) {
        if (paso > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                (1..3).forEach { i ->
                    Box(
                        Modifier.height(10.dp).width(if (i == paso) 34.dp else 18.dp).clip(CircleShape)
                            .background(if (i <= paso) Tinta else Lila)
                    )
                }
            }
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (paso) {
                0 -> {
                    Spacer(Modifier.height(40.dp))
                    Caja(Modifier.size(96.dp), fondo = ColorXp, radio = 26.dp) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("✓", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Tinta) }
                    }
                    Spacer(Modifier.height(26.dp))
                    Text("Mis Hábitos", style = TituloGrande.copy(fontSize = 40.sp, lineHeight = 44.sp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Convierte tus hábitos en misiones. Cada misión cumplida te da XP, subes de nivel y armas rachas de días seguidos.",
                        color = Tinta2, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Chip("🔥 Rachas")
                        Chip("⭐ Niveles")
                        Chip("📈 Progreso")
                    }
                }

                1 -> {
                    Titulo("¿Cómo te llamas, jugador?", "Así te saluda la app.")
                    CampoTexto(
                        valor = nombre,
                        alCambiar = { nombre = it.take(30) },
                        pista = "Tu nombre",
                        opciones = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        acciones = KeyboardActions(onNext = { if (nombre.isNotBlank()) paso = 2 }),
                    )
                }

                2 -> {
                    Titulo("Elige tus primeras misiones", "Puedes agregar, cambiar o quitar después.")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SUGERENCIAS.forEach { s ->
                            val on = s in elegidas
                            Caja(
                                fondo = if (on) ColorXp else Carta, radio = 12.dp, sombra = if (on) 3.dp else 2.dp,
                                alTocar = { if (on) elegidas.remove(s) else elegidas.add(s) },
                            ) {
                                Text(
                                    "${s.emoji} ${s.nombre}" + if (s.meta > 1) " · ${s.meta} ${s.unidad}" else "",
                                    fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = Tinta, fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                )
                            }
                        }
                    }
                }

                else -> {
                    Titulo("¿Te recuerdo tus misiones?", "Un aviso al día, solo si te falta alguna.")
                    Caja(fondo = Carta) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Recordatorio diario", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Tinta)
                                Text(if (recordatorio) "A las ${horaTexto(hora)}" else "Apagado", color = Suave, fontSize = 14.sp)
                            }
                            Switch(
                                checked = recordatorio,
                                onCheckedChange = { recordatorio = it },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = ColorHecho, checkedThumbColor = Carta,
                                    uncheckedTrackColor = Lila, uncheckedBorderColor = Tinta, uncheckedThumbColor = Tinta,
                                ),
                            )
                        }
                    }
                    if (recordatorio) {
                        Spacer(Modifier.height(12.dp))
                        Boton("Cambiar hora: ${horaTexto(hora)}", fondo = Carta) {
                            TimePickerDialog(context, { _, h, m -> hora = h * 60 + m }, hora / 60, hora % 60, false).show()
                        }
                    }
                }
            }
        }

        val puede = when (paso) {
            1 -> nombre.isNotBlank()
            2 -> elegidas.isNotEmpty()
            else -> true
        }
        Boton(
            texto = when (paso) {
                0 -> "Empezar"
                3 -> "¡A jugar, ${nombre.trim().substringBefore(" ")}!"
                else -> "Siguiente"
            },
            activo = puede,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (paso < 3) {
                paso++
            } else {
                if (recordatorio && Build.VERSION.SDK_INT >= 33) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                alTerminar(Inicio(nombre.trim(), elegidas.toList(), recordatorio, hora))
            }
        }
        if (paso == 2 && elegidas.isEmpty()) {
            Text("Elige al menos una.", color = Suave, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        }
    }
}

@Composable
private fun Titulo(texto: String, sub: String) {
    Text(texto, style = TituloGrande)
    Spacer(Modifier.height(8.dp))
    Text(sub, color = Suave, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun Chip(texto: String) {
    Caja(fondo = Carta, radio = 99.dp, sombra = 2.dp) {
        Text(texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Tinta, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}
