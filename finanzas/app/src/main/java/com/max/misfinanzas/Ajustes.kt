@file:OptIn(ExperimentalMaterial3Api::class)

package com.max.misfinanzas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaAjustes(
    perfil: Perfil,
    alCambiar: () -> Unit,
    alSincronizar: () -> Unit,
    alExportar: () -> Unit,
    alBorrarTodo: () -> Unit,
    alCerrar: () -> Unit,
) {
    BackHandler { alCerrar() }
    var nombre by remember { mutableStateOf(perfil.nombre) }
    var moneda by remember { mutableStateOf(perfil.moneda) }
    var direccion by remember { mutableStateOf(perfil.direccion) }
    val gastos = remember { mutableStateListOf<String>().apply { addAll(perfil.categorias(TIPO_GASTO)) } }
    val ingresos = remember { mutableStateListOf<String>().apply { addAll(perfil.categorias(TIPO_INGRESO)) } }
    var confirmarBorrar by remember { mutableStateOf(false) }

    // Guarda las categorías en cuanto cambian
    LaunchedEffect(Unit) {
        snapshotFlow { gastos.toList() to ingresos.toList() }.collect { (g, i) ->
            if (g.isNotEmpty()) perfil.guardarCategorias(TIPO_GASTO, g)
            if (i.isNotEmpty()) perfil.guardarCategorias(TIPO_INGRESO, i)
            alCambiar()
        }
    }

    Scaffold(
        containerColor = Fondo,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontFamily = FontFamily.Serif, fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = alCerrar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Fondo, titleContentColor = Texto, navigationIconContentColor = Texto,
                ),
            )
        },
    ) { espacio ->
        Column(
            Modifier.padding(espacio).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Seccion("Tu nombre")
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it.take(30)
                    if (nombre.isNotBlank()) {
                        perfil.nombre = nombre
                        alCambiar()
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = camposColores(),
                modifier = Modifier.fillMaxWidth(),
            )

            Seccion("Moneda")
            MONEDAS.forEach { m ->
                val elegida = m.codigo == moneda
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (elegida) Superficie2 else Superficie)
                        .border(1.dp, if (elegida) Texto2 else Borde, RoundedCornerShape(10.dp))
                        .clickable {
                            moneda = m.codigo
                            perfil.moneda = m.codigo
                            simboloMoneda = m.simbolo
                            alCambiar()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(m.nombre, color = Texto, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text("${m.codigo}  ${m.simbolo}", color = Suave, fontSize = 13.sp)
                }
            }

            Seccion("Categorías de gastos")
            SelectorCategorias(Categorias.sugeridasGasto, gastos, ColorGasto)
            Seccion("Categorías de ganancias")
            SelectorCategorias(Categorias.sugeridasIngreso, ingresos, ColorIngreso)

            Seccion("Tus datos")
            OutlinedButton(onClick = alExportar, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Text("Exportar todo para Excel (CSV)", color = Texto)
            }

            Seccion("Conectar con tu computadora (opcional)")
            Text(
                "Si usas Mis Finanzas en tu compu, escribe la dirección que muestra (ej. 192.168.1.10:8765). " +
                    "Tu cel y tu compu deben estar en el mismo WiFi.",
                color = Suave, fontSize = 13.sp, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it.take(60) },
                placeholder = { Text("192.168.1.10:8765") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = camposColores(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        perfil.direccion = direccion
                        alCambiar()
                        alSincronizar()
                    },
                    enabled = direccion.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Texto, contentColor = Fondo),
                ) { Text("Guardar y sincronizar") }
                if (perfil.direccion.isNotBlank()) {
                    TextButton(onClick = {
                        direccion = ""
                        perfil.direccion = ""
                        alCambiar()
                    }) { Text("Desconectar", color = Texto2) }
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Borde)
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { confirmarBorrar = true }) {
                Text("Borrar todos mis datos", color = ColorPeligro, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = { confirmarBorrar = false },
            containerColor = Superficie,
            title = { Text("¿Borrar todo?", color = Texto) },
            text = {
                Text(
                    "Se borran todos tus movimientos, tu nombre y tus ajustes de este celular. No se puede deshacer.",
                    color = Texto2,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarBorrar = false
                        alBorrarTodo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPeligro, contentColor = Color.White),
                ) { Text("Borrar todo") }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrar = false }) { Text("Cancelar", color = Texto2) } },
        )
    }
}

@Composable
private fun Seccion(titulo: String) {
    Spacer(Modifier.height(26.dp))
    Text(titulo, color = Texto, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
}
