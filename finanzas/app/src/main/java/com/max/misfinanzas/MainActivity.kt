@file:OptIn(ExperimentalMaterial3Api::class)

package com.max.misfinanzas

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val baseDatos = BaseDatos(this)
        val perfil = Perfil(getSharedPreferences("perfil", Context.MODE_PRIVATE))
        simboloMoneda = monedaDe(perfil.moneda).simbolo
        setContent {
            TemaFinanzas { Raiz(baseDatos, perfil) }
        }
    }
}

/** Decide si mostrar la bienvenida (primera vez) o la app. */
@Composable
fun Raiz(baseDatos: BaseDatos, perfil: Perfil) {
    var listo by remember { mutableStateOf(perfil.listo) }
    if (!listo) {
        Bienvenida { c ->
            perfil.nombre = c.nombre
            perfil.moneda = c.moneda
            simboloMoneda = monedaDe(c.moneda).simbolo
            perfil.guardarCategorias(TIPO_GASTO, c.categoriasGasto)
            perfil.guardarCategorias(TIPO_INGRESO, c.categoriasIngreso)
            if (c.saldoInicial > 0) {
                baseDatos.guardar(
                    Movimiento(
                        uuid = UUID.randomUUID().toString(),
                        tipo = TIPO_INGRESO,
                        monto = redondear(c.saldoInicial),
                        categoria = "Otros",
                        descripcion = "Saldo inicial",
                        fecha = LocalDateTime.now().withNano(0),
                    )
                )
            }
            perfil.listo = true
            listo = true
        }
    } else {
        App(baseDatos, perfil, alBorrarTodo = {
            baseDatos.borrarTodo()
            perfil.borrarTodo()
            simboloMoneda = monedaDe("MXN").simbolo
            listo = false
        })
    }
}

@Composable
fun App(baseDatos: BaseDatos, perfil: Perfil, alBorrarTodo: () -> Unit) {
    val context = LocalContext.current
    var movimientos by remember { mutableStateOf(baseDatos.todos()) }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var periodo by rememberSaveable { mutableStateOf(Periodo.MES) }
    var version by remember { mutableIntStateOf(0) }  // sube cuando cambian los ajustes
    var sincronizando by remember { mutableStateOf(false) }
    var conectado by remember { mutableStateOf(false) }
    var verAjustes by rememberSaveable { mutableStateOf(false) }
    var agregando by remember { mutableStateOf<String?>(null) }
    var porBorrar by remember { mutableStateOf<Movimiento?>(null) }
    val avisos = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resumen = remember(movimientos, periodo, version) { calcularResumen(movimientos, periodo) }
    val nombre = remember(version) { perfil.nombre.substringBefore(" ") }
    val hayCompu = remember(version) { perfil.direccion.isNotBlank() }

    fun recargar() {
        movimientos = baseDatos.todos()
    }

    fun sincronizar(silencioso: Boolean) {
        val direccion = perfil.direccion
        if (direccion.isBlank() || sincronizando) return
        scope.launch {
            sincronizando = true
            try {
                val deLaCompu = Sincronizador.sincronizar(direccion, baseDatos.todos())
                baseDatos.mezclar(deLaCompu)
                recargar()
                conectado = true
                if (!silencioso) avisos.showSnackbar("Sincronizado con tu computadora")
            } catch (e: Exception) {
                conectado = false
                if (!silencioso) avisos.showSnackbar("No pude conectar. Revisa que tu compu esté prendida y en el mismo WiFi.")
            } finally {
                sincronizando = false
            }
        }
    }

    fun exportar() {
        try {
            val carpeta = File(context.cacheDir, "exportes").apply { mkdirs() }
            val archivo = File(carpeta, "mis-finanzas-${LocalDate.now()}.csv")
            val filas = movimientos.filter { !it.borrado }.sortedBy { it.fecha }
            val texto = buildString {
                append('﻿')
                append("Fecha,Tipo,Categoría,Concepto,Monto\n")
                for (m in filas) {
                    val concepto = m.descripcion.replace("\"", "\"\"")
                    append("${m.fecha.format(FORMATO_GUARDAR).replace('T', ' ')},")
                    append(if (m.esIngreso) "Ganancia," else "Gasto,")
                    append("\"${m.categoria}\",\"$concepto\",${m.conSigno}\n")
                }
            }
            archivo.writeText(texto, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".archivos", archivo)
            val enviar = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(enviar, "Guardar o compartir"))
        } catch (e: Exception) {
            scope.launch { avisos.showSnackbar("No se pudo exportar: ${e.message}") }
        }
    }

    LaunchedEffect(Unit) { sincronizar(silencioso = true) }

    if (verAjustes) {
        PantallaAjustes(
            perfil = perfil,
            alCambiar = { version++ },
            alSincronizar = { sincronizar(silencioso = false) },
            alExportar = { exportar() },
            alBorrarTodo = alBorrarTodo,
            alCerrar = { verAjustes = false },
        )
        return
    }

    Scaffold(
        containerColor = Fondo,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (nombre.isBlank()) "Mis Finanzas" else "Hola, $nombre",
                            fontFamily = FontFamily.Serif, fontSize = 22.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hayCompu) {
                                Box(
                                    Modifier.size(7.dp).clip(CircleShape)
                                        .background(if (conectado) ColorConectado else Suave)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("Saldo: ${dinero(resumen.saldoTotal)}", color = Suave, fontSize = 13.sp)
                        }
                    }
                },
                actions = {
                    if (hayCompu) {
                        if (sincronizando) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp).size(22.dp),
                                strokeWidth = 2.dp, color = Texto2,
                            )
                        } else {
                            IconButton(onClick = { sincronizar(silencioso = false) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sincronizar con tu computadora")
                            }
                        }
                    }
                    IconButton(onClick = { verAjustes = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Fondo,
                    titleContentColor = Texto,
                    actionIconContentColor = Texto2,
                ),
            )
        },
        floatingActionButton = {
            val tipo = if (pestana == 2) TIPO_INGRESO else TIPO_GASTO
            ExtendedFloatingActionButton(
                onClick = { agregando = tipo },
                containerColor = colorDe(tipo),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (tipo == TIPO_GASTO) "Agregar gasto" else "Agregar ganancia", fontWeight = FontWeight.Bold) },
            )
        },
    ) { espacio ->
        Column(Modifier.padding(espacio).fillMaxSize()) {
            TabRow(
                selectedTabIndex = pestana,
                containerColor = Fondo,
                contentColor = Texto,
                indicator = { posiciones ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(posiciones[pestana]),
                        color = when (pestana) {
                            1 -> ColorGasto
                            2 -> ColorIngreso
                            else -> Texto
                        },
                    )
                },
                divider = {},
            ) {
                listOf("Resumen", "Gastos", "Ganancias").forEachIndexed { i, titulo ->
                    Tab(
                        selected = pestana == i,
                        onClick = { pestana = i },
                        selectedContentColor = Texto,
                        unselectedContentColor = Suave,
                        text = { Text(titulo, fontWeight = FontWeight.SemiBold) },
                    )
                }
            }
            FiltroPeriodo(periodo) { periodo = it }
            when (pestana) {
                0 -> PantallaResumen(resumen, periodo)
                1 -> PantallaTabla(TIPO_GASTO, movimientos, periodo) { porBorrar = it }
                else -> PantallaTabla(TIPO_INGRESO, movimientos, periodo) { porBorrar = it }
            }
        }
    }

    agregando?.let { tipo ->
        DialogoAgregar(
            tipo = tipo,
            categorias = remember(version, tipo) { perfil.categorias(tipo) },
            alCerrar = { agregando = null },
            alGuardar = { nuevo ->
                baseDatos.guardar(nuevo)
                agregando = null
                recargar()
                sincronizar(silencioso = true)
                scope.launch {
                    avisos.showSnackbar(if (tipo == TIPO_GASTO) "Gasto agregado" else "Ganancia agregada")
                }
            },
        )
    }

    porBorrar?.let { m ->
        DialogoBorrar(
            m = m,
            alCerrar = { porBorrar = null },
            alConfirmar = {
                baseDatos.borrar(m.uuid)
                porBorrar = null
                recargar()
                sincronizar(silencioso = true)
            },
        )
    }
}
