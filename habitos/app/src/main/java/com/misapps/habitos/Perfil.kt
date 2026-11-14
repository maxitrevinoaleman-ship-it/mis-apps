package com.misapps.habitos

import android.content.SharedPreferences

/** Lo que la app sabe de la persona (se guarda en el celular). */
class Perfil(private val prefs: SharedPreferences) {

    var listo: Boolean
        get() = prefs.getBoolean("listo", false)
        set(v) = prefs.edit().putBoolean("listo", v).apply()

    var nombre: String
        get() = prefs.getString("nombre", "") ?: ""
        set(v) = prefs.edit().putString("nombre", v.trim()).apply()

    var recordatorio: Boolean
        get() = prefs.getBoolean("recordatorio", false)
        set(v) = prefs.edit().putBoolean("recordatorio", v).apply()

    /** Hora del recordatorio en minutos desde la medianoche (20:00 = 1200). */
    var horaRecordatorio: Int
        get() = prefs.getInt("hora_recordatorio", 20 * 60)
        set(v) = prefs.edit().putInt("hora_recordatorio", v).apply()

    fun borrarTodo() {
        prefs.edit().clear().apply()
    }
}

fun horaTexto(minutos: Int): String {
    val h = minutos / 60
    val m = minutos % 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "a.m." else "p.m.")
}
