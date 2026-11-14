package com.misapps.habitos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val baseDatos = BaseDatos(this)
        val perfil = Perfil(getSharedPreferences("perfil", Context.MODE_PRIVATE))
        Recordatorio.crearCanal(this)
        setContent {
            TemaHabitos { Raiz(baseDatos, perfil) }
        }
    }
}

@Composable
fun Raiz(baseDatos: BaseDatos, perfil: Perfil) {
    val context = LocalContext.current
    var listo by remember { mutableStateOf(perfil.listo) }
    if (!listo) {
        Bienvenida { inicio ->
            perfil.nombre = inicio.nombre
            perfil.recordatorio = inicio.recordatorio
            perfil.horaRecordatorio = inicio.hora
            inicio.habitos.forEach { s ->
                baseDatos.crear(Habito(nombre = s.nombre, emoji = s.emoji, meta = s.meta, unidad = s.unidad, color = baseDatos.siguienteColor()))
            }
            perfil.listo = true
            Recordatorio.programar(context)
            listo = true
        }
    } else {
        App(baseDatos, perfil, alBorrarTodo = {
            baseDatos.borrarTodo()
            perfil.borrarTodo()
            Recordatorio.programar(context)
            listo = false
        })
    }
}

private enum class Pestana(val titulo: String) { HOY("Hoy"), HABITOS("Mis hábitos"), PROGRESO("Progreso") }

@Composable
fun App(baseDatos: BaseDatos, perfil: Perfil, alBorrarTodo: () -> Unit) {
    var version by remember { mutableIntStateOf(0) }       // sube cada vez que cambia algo
    var dia by remember { mutableStateOf(LocalDate.now()) }  // para cambiar de día a medianoche
    var pestana by rememberSaveable { mutableStateOf(Pestana.HOY) }
    var verAjustes by rememberSaveable { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Habito?>(null) }
    var creando by remember { mutableStateOf(false) }
    var porQuitar by remember { mutableStateOf<Habito?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val estado = remember(version, dia) { baseDatos.estadoHoy(dia) }
    val jugador = remember(version, dia) { baseDatos.jugador() }
    val stats = remember(version, dia, pestana) { if (pestana == Pestana.PROGRESO) baseDatos.estadisticas(hoy = dia) else null }
    val nombre = remember(version) { perfil.nombre.substringBefore(" ") }

    // Revisa cada minuto si ya cambió el día
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            if (LocalDate.now() != dia) dia = LocalDate.now()
        }
    }
    LaunchedEffect(aviso) {
        if (aviso != null) {
            delay(2200)
            aviso = null
        }
    }

    when {
        verAjustes -> {
            PantallaAjustes(perfil, alCambiar = { version++ }, alBorrarTodo = alBorrarTodo, alCerrar = { verAjustes = false })
            return
        }
        creando || editando != null -> {
            EditorHabito(
                original = editando,
                colorNuevo = remember(creando) { baseDatos.siguienteColor() },
                alGuardar = { h ->
                    if (editando == null) {
                        baseDatos.crear(h.copy(creado = LocalDate.now()))
                        aviso = "Nueva misión: ${h.emoji} ${h.nombre}"
                    } else {
                        baseDatos.editar(h)
                        aviso = "Cambios guardados"
                    }
                    creando = false
                    editando = null
                    version++
                },
                alCerrar = {
                    creando = false
                    editando = null
                },
            )
            return
        }
    }

    Box(Modifier.fillMaxSize().background(Fondo)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                TarjetaJugador(nombre, jugador) { verAjustes = true }
            }
            // Pestañas
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pestana.entries.forEach { p ->
                    val on = p == pestana
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) Tinta else Carta)
                            .border(BORDE, Tinta, RoundedCornerShape(12.dp)).clickable { pestana = p }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(p.titulo, color = if (on) Carta else Tinta, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when (pestana) {
                    Pestana.HOY -> PantallaHoy(
                        estado = estado,
                        alMarcar = { e, cantidad ->
                            val antes = e.cumplido
                            baseDatos.poner(e.habito, cantidad, dia)
                            val ahora = cantidad >= e.habito.meta
                            val nivelAntes = jugador.nivel
                            version++
                            if (!antes && ahora) {
                                val nuevo = baseDatos.jugador()
                                aviso = if (nuevo.nivel > nivelAntes) "¡Subiste a nivel ${nuevo.nivel}! Ahora eres ${nuevo.titulo}"
                                else "${e.habito.emoji} +$XP_POR_MISION XP"
                            }
                        },
                        alIrAHabitos = { pestana = Pestana.HABITOS },
                    )

                    Pestana.HABITOS -> PantallaHabitos(
                        habitos = estado.map { it.habito },
                        alNuevo = { creando = true },
                        alEditar = { editando = it },
                        alQuitar = { porQuitar = it },
                    )

                    Pestana.PROGRESO -> stats?.let { PantallaProgreso(jugador, estado, it) }
                }
            }
        }

        // Aviso flotante (+10 XP, nivel nuevo...)
        aviso?.let { texto ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp, start = 24.dp, end = 24.dp)) {
                Caja(fondo = ColorXp, radio = 14.dp) {
                    Text(texto, fontWeight = FontWeight.ExtraBold, color = Tinta, fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp))
                }
            }
        }
    }

    porQuitar?.let { h ->
        DialogoQuitar(h, alCerrar = { porQuitar = null }, alConfirmar = {
            baseDatos.archivar(h.id)
            porQuitar = null
            aviso = "Quitaste ${h.nombre}"
            version++
        })
    }
}
