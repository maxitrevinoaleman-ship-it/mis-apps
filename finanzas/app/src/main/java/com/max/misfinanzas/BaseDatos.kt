package com.max.misfinanzas

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDateTime

/** Guarda los movimientos en el celular, así la app funciona aunque tu compu esté apagada. */
class BaseDatos(context: Context) : SQLiteOpenHelper(context, "finanzas.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE movimientos (
                uuid TEXT PRIMARY KEY,
                tipo TEXT NOT NULL,
                monto REAL NOT NULL,
                categoria TEXT NOT NULL,
                descripcion TEXT NOT NULL DEFAULT '',
                fecha TEXT NOT NULL,
                borrado INTEGER NOT NULL DEFAULT 0)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) {}

    fun todos(): List<Movimiento> =
        readableDatabase.rawQuery(
            "SELECT uuid, tipo, monto, categoria, descripcion, fecha, borrado FROM movimientos", null
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        Movimiento(
                            uuid = c.getString(0),
                            tipo = c.getString(1),
                            monto = c.getDouble(2),
                            categoria = c.getString(3),
                            descripcion = c.getString(4) ?: "",
                            fecha = LocalDateTime.parse(c.getString(5)),
                            borrado = c.getInt(6) == 1,
                        )
                    )
                }
            }
        }

    private fun valores(m: Movimiento) = ContentValues().apply {
        put("uuid", m.uuid)
        put("tipo", m.tipo)
        put("monto", m.monto)
        put("categoria", m.categoria)
        put("descripcion", m.descripcion)
        put("fecha", m.fecha.format(FORMATO_GUARDAR))
        put("borrado", if (m.borrado) 1 else 0)
    }

    fun guardar(m: Movimiento) {
        writableDatabase.insertWithOnConflict("movimientos", null, valores(m), SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Se marca como borrado (no se elimina) para que la compu también se entere. */
    fun borrar(uuid: String) {
        writableDatabase.execSQL("UPDATE movimientos SET borrado = 1 WHERE uuid = ?", arrayOf(uuid))
    }

    fun borrarTodo() {
        writableDatabase.execSQL("DELETE FROM movimientos")
    }

    /** Junta lo que mandó la compu con lo que hay en el celular. */
    fun mezclar(remotos: List<Movimiento>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (r in remotos) {
                db.insertWithOnConflict("movimientos", null, valores(r), SQLiteDatabase.CONFLICT_IGNORE)
                if (r.borrado) {
                    db.execSQL("UPDATE movimientos SET borrado = 1 WHERE uuid = ?", arrayOf(r.uuid))
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
