package com.max.misfinanzas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

/**
 * Habla con Mis Finanzas de tu computadora (opcional).
 * Manda todo lo del celular y recibe todo lo de la compu; así los dos quedan iguales.
 */
object Sincronizador {

    /** "192.168.1.5" -> "http://192.168.1.5:8765" */
    fun normalizar(direccion: String): String {
        var d = direccion.trim().removeSuffix("/")
        if (!d.startsWith("http://") && !d.startsWith("https://")) d = "http://$d"
        val sinProtocolo = d.substringAfter("://")
        if (!sinProtocolo.contains(":")) d = "$d:8765"
        return d
    }

    suspend fun sincronizar(direccion: String, locales: List<Movimiento>): List<Movimiento> =
        withContext(Dispatchers.IO) {
            val conexion = URL(normalizar(direccion) + "/api/sync").openConnection() as HttpURLConnection
            try {
                conexion.requestMethod = "POST"
                conexion.connectTimeout = 4000
                conexion.readTimeout = 10000
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")

                val lista = JSONArray()
                for (m in locales) {
                    lista.put(
                        JSONObject()
                            .put("uuid", m.uuid)
                            .put("tipo", m.tipo)
                            .put("monto", m.monto)
                            .put("categoria", m.categoria)
                            .put("descripcion", m.descripcion)
                            .put("fecha", m.fecha.format(FORMATO_GUARDAR))
                            .put("borrado", m.borrado)
                    )
                }
                val cuerpo = JSONObject().put("movimientos", lista).toString().toByteArray(Charsets.UTF_8)
                conexion.outputStream.use { it.write(cuerpo) }

                if (conexion.responseCode !in 200..299) {
                    throw IOException("La compu respondió con error ${conexion.responseCode}")
                }
                val texto = conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val recibidos = JSONObject(texto).getJSONArray("movimientos")
                (0 until recibidos.length()).mapNotNull { i ->
                    // Si algún dato viene raro, se salta en vez de tronar
                    runCatching {
                    val o = recibidos.getJSONObject(i)
                    Movimiento(
                        uuid = o.getString("uuid"),
                        tipo = o.getString("tipo"),
                        monto = o.getDouble("monto"),
                        categoria = o.getString("categoria"),
                        descripcion = o.optString("descripcion", ""),
                        fecha = LocalDateTime.parse(o.getString("fecha")),
                        borrado = o.optBoolean("borrado", false),
                    )
                    }.getOrNull()
                }
            } finally {
                conexion.disconnect()
            }
        }
}
