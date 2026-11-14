package com.misapps.habitos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.min
import kotlin.math.roundToInt

/** Un hábito ("misión"). dias = qué días toca: "0123456" (0 = lunes ... 6 = domingo). */
data class Habito(
    val id: Long = 0,
    val nombre: String,
    val emoji: String = "✅",
    val color: Long,
    val meta: Int = 1,
    val unidad: String = "",
    val dias: String = "0123456",
    val creado: LocalDate = LocalDate.now(),
)

/** Cómo va un hábito hoy. */
data class EstadoHabito(
    val habito: Habito,
    val hoy: Int,
    val tocaHoy: Boolean,
    val racha: Int,
    val mejorRacha: Int,
) {
    val cumplido get() = hoy >= habito.meta
}

data class Jugador(val xp: Int, val nivel: Int, val titulo: String, val xpNivel: Int, val misiones: Int)

data class StatHabito(val habito: Habito, val racha: Int, val mejorRacha: Int, val porcentaje30: Int?)

data class Estadisticas(
    val hoy: LocalDate,
    val general: List<Pair<LocalDate, Double?>>,   // avance de cada día (0..1), null = no había hábitos
    val semanas: List<Pair<LocalDate, Int?>>,      // % por semana
    val porHabito: List<StatHabito>,
)

const val XP_POR_MISION = 10
const val XP_POR_NIVEL = 100
private val TITULOS = listOf(15 to "Leyenda", 10 to "Imparable", 6 to "Disciplinado", 3 to "Constante", 1 to "Novato")
fun tituloDe(nivel: Int) = TITULOS.first { nivel >= it.first }.second

/** Colores para identificar cada hábito (se asignan en orden). */
val COLORES = listOf(0xFF3987E5, 0xFFD95926, 0xFF199E70, 0xFFC98500, 0xFFD55181, 0xFF0CA30C, 0xFF9085E9, 0xFFE66767)

val DIAS_CORTOS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** Hábitos que se sugieren la primera vez. */
data class Sugerencia(val nombre: String, val emoji: String, val meta: Int = 1, val unidad: String = "")

val SUGERENCIAS = listOf(
    Sugerencia("Tomar agua", "💧", 8, "vasos"),
    Sugerencia("Leer", "📚"),
    Sugerencia("Hacer ejercicio", "🏃"),
    Sugerencia("Dormir temprano", "😴"),
    Sugerencia("Meditar", "🧘"),
    Sugerencia("Estudiar", "📝"),
    Sugerencia("Tender la cama", "🛏️"),
    Sugerencia("Comer fruta", "🍎"),
    Sugerencia("Caminar", "🚶", 30, "min"),
    Sugerencia("Menos celular", "📵"),
    Sugerencia("Lavarme los dientes", "🦷", 3, "veces"),
    Sugerencia("Practicar inglés", "🇺🇸"),
)

val EMOJIS = listOf("✅", "💧", "📚", "🏃", "🧘", "💪", "🥗", "😴", "🦷", "📝", "🇺🇸", "🎸", "💻", "🧹", "🚶", "🙏",
    "💊", "🍎", "📵", "☀️", "🛏️", "🎨", "🐶", "💰")

fun toca(h: Habito, dia: LocalDate): Boolean =
    h.dias.contains(((dia.dayOfWeek.value - 1)).toString()) && !dia.isBefore(h.creado)

class BaseDatos(context: Context) : SQLiteOpenHelper(context, "habitos.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE habitos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                emoji TEXT NOT NULL,
                color INTEGER NOT NULL,
                meta INTEGER NOT NULL DEFAULT 1,
                unidad TEXT NOT NULL DEFAULT '',
                dias TEXT NOT NULL DEFAULT '0123456',
                creado TEXT NOT NULL,
                archivado INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE registros (
                habito_id INTEGER NOT NULL,
                fecha TEXT NOT NULL,
                cantidad INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (habito_id, fecha))"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) {}

    // ------------------------------------------------------------ hábitos

    fun habitos(): List<Habito> =
        readableDatabase.rawQuery(
            "SELECT id, nombre, emoji, color, meta, unidad, dias, creado FROM habitos WHERE archivado = 0 ORDER BY id", null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Habito(
                            id = c.getLong(0), nombre = c.getString(1), emoji = c.getString(2), color = c.getLong(3),
                            meta = c.getInt(4), unidad = c.getString(5) ?: "", dias = c.getString(6),
                            creado = LocalDate.parse(c.getString(7)),
                        )
                    )
                }
            }
        }

    private fun valores(h: Habito) = ContentValues().apply {
        put("nombre", h.nombre.trim().take(40))
        put("emoji", h.emoji)
        put("color", h.color)
        put("meta", h.meta.coerceIn(1, 1000))
        put("unidad", h.unidad.trim().take(15))
        put("dias", h.dias)
    }

    fun crear(h: Habito): Long {
        val v = valores(h)
        v.put("creado", h.creado.toString())
        return writableDatabase.insert("habitos", null, v)
    }

    fun editar(h: Habito) {
        writableDatabase.update("habitos", valores(h), "id = ?", arrayOf(h.id.toString()))
    }

    /** No se borra para no perder el historial ni la XP; solo deja de aparecer. */
    fun archivar(id: Long) {
        writableDatabase.execSQL("UPDATE habitos SET archivado = 1 WHERE id = ?", arrayOf(id))
    }

    fun siguienteColor(): Long {
        val usados = readableDatabase.rawQuery("SELECT COUNT(*) FROM habitos", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
        return COLORES[usados % COLORES.size]
    }

    // ------------------------------------------------------------ registros

    private fun registros(id: Long): Map<LocalDate, Int> =
        readableDatabase.rawQuery("SELECT fecha, cantidad FROM registros WHERE habito_id = ?", arrayOf(id.toString()))
            .use { c ->
                buildMap {
                    while (c.moveToNext()) put(LocalDate.parse(c.getString(0)), c.getInt(1))
                }
            }

    fun poner(h: Habito, cantidad: Int, fecha: LocalDate = LocalDate.now()) {
        val v = ContentValues().apply {
            put("habito_id", h.id)
            put("fecha", fecha.toString())
            put("cantidad", cantidad.coerceIn(0, h.meta * 5))
        }
        writableDatabase.insertWithOnConflict("registros", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun borrarTodo() {
        writableDatabase.execSQL("DELETE FROM registros")
        writableDatabase.execSQL("DELETE FROM habitos")
        try {
            writableDatabase.execSQL("DELETE FROM sqlite_sequence WHERE name = 'habitos'")
        } catch (e: Exception) {
            // la tabla interna todavía no existe: no pasa nada
        }
    }

    // ------------------------------------------------------------ rachas y estadísticas

    private fun rachas(h: Habito, regs: Map<LocalDate, Int>, hoy: LocalDate): Pair<Int, Int> {
        fun cumplido(d: LocalDate) = (regs[d] ?: 0) >= h.meta
        // Racha actual: si hoy todavía no lo haces, no se rompe (cuenta desde ayer)
        var actual = 0
        var d = hoy
        if (toca(h, d) && !cumplido(d)) d = d.minusDays(1)
        while (!d.isBefore(h.creado)) {
            if (toca(h, d)) {
                if (!cumplido(d)) break
                actual++
            }
            d = d.minusDays(1)
        }
        // Mejor racha
        var mejor = 0
        var corrida = 0
        d = h.creado
        while (!d.isAfter(hoy)) {
            if (toca(h, d)) {
                if (cumplido(d)) {
                    corrida++
                    mejor = maxOf(mejor, corrida)
                } else if (d != hoy) {
                    corrida = 0
                }
            }
            d = d.plusDays(1)
        }
        return actual to maxOf(mejor, actual)
    }

    fun estadoHoy(hoy: LocalDate = LocalDate.now()): List<EstadoHabito> = habitos().map { h ->
        val regs = registros(h.id)
        val (racha, mejor) = rachas(h, regs, hoy)
        EstadoHabito(h, regs[hoy] ?: 0, toca(h, hoy), racha, mejor)
    }

    fun jugador(): Jugador {
        val total = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM registros r JOIN habitos h ON h.id = r.habito_id WHERE r.cantidad >= h.meta", null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        val xp = total * XP_POR_MISION
        val nivel = xp / XP_POR_NIVEL + 1
        return Jugador(xp, nivel, tituloDe(nivel), xp % XP_POR_NIVEL, total)
    }

    fun estadisticas(dias: Int = 84, hoy: LocalDate = LocalDate.now()): Estadisticas {
        val desde = hoy.minusDays((dias - 1).toLong())
        val fechas = (0 until dias).map { desde.plusDays(it.toLong()) }
        val avances = fechas.associateWith { mutableListOf<Double>() }
        val porHabito = habitos().map { h ->
            val regs = registros(h.id)
            val (racha, mejor) = rachas(h, regs, hoy)
            var tocaron = 0
            var cumplidos = 0
            for (f in fechas) {
                if (!toca(h, f)) continue
                val avance = min((regs[f] ?: 0).toDouble() / h.meta, 1.0)
                avances.getValue(f).add(avance)
                val haceDias = ChronoUnit.DAYS.between(f, hoy)
                if (haceDias < 30 && !(f == hoy && avance < 1.0)) {
                    tocaron++
                    if (avance >= 1.0) cumplidos++
                }
            }
            StatHabito(h, racha, mejor, if (tocaron > 0) (cumplidos * 100.0 / tocaron).roundToInt() else null)
        }
        val general = fechas.map { f ->
            val v = avances.getValue(f)
            f to (if (v.isEmpty()) null else v.average())
        }
        val semanas = fechas.chunked(7).map { semana ->
            val vals = semana.mapNotNull { f -> avances.getValue(f).takeIf { it.isNotEmpty() }?.average() }
            semana.first() to (if (vals.isEmpty()) null else (vals.average() * 100).roundToInt())
        }
        return Estadisticas(hoy, general, semanas, porHabito)
    }
}
