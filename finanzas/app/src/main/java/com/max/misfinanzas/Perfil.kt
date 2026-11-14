package com.max.misfinanzas

import android.content.SharedPreferences

/** Lo que la app sabe de la persona que la usa (se guarda en el celular). */
class Perfil(private val prefs: SharedPreferences) {

    var listo: Boolean
        get() = prefs.getBoolean("listo", false)
        set(v) = prefs.edit().putBoolean("listo", v).apply()

    var nombre: String
        get() = prefs.getString("nombre", "") ?: ""
        set(v) = prefs.edit().putString("nombre", v.trim()).apply()

    var moneda: String
        get() = prefs.getString("moneda", "MXN") ?: "MXN"
        set(v) = prefs.edit().putString("moneda", v).apply()

    /** Dirección de la computadora para sincronizar (opcional, ej. 192.168.1.10:8765). */
    var direccion: String
        get() = prefs.getString("direccion", "") ?: ""
        set(v) = prefs.edit().putString("direccion", v.trim()).apply()

    fun categorias(tipo: String): List<String> {
        val guardadas = prefs.getString("categorias_$tipo", null)
            ?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() }
        return if (guardadas.isNullOrEmpty()) {
            if (tipo == TIPO_GASTO) Categorias.inicialesGasto else Categorias.inicialesIngreso
        } else guardadas
    }

    fun guardarCategorias(tipo: String, lista: List<String>) {
        val limpia = lista.map { it.replace("|", "/").trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit().putString("categorias_$tipo", limpia.joinToString("|")).apply()
    }

    fun borrarTodo() {
        prefs.edit().clear().apply()
    }
}
