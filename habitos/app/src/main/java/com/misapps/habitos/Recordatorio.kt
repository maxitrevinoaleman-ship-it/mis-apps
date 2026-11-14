package com.misapps.habitos

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.ZoneId

/** Recordatorio diario: "te faltan X misiones hoy". */
object Recordatorio {
    private const val CANAL = "recordatorio_diario"
    private const val CODIGO = 4512

    private fun intento(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, CODIGO, Intent(context, AvisoReceptor::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Programa (o quita) el aviso diario según el perfil. */
    fun programar(context: Context) {
        val perfil = Perfil(context.getSharedPreferences("perfil", Context.MODE_PRIVATE))
        val alarmas = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmas.cancel(intento(context))
        if (!perfil.listo || !perfil.recordatorio) return
        val minutos = perfil.horaRecordatorio
        var cuando = LocalDateTime.now().withHour(minutos / 60).withMinute(minutos % 60).withSecond(0).withNano(0)
        if (!cuando.isAfter(LocalDateTime.now())) cuando = cuando.plusDays(1)
        val millis = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmas.setInexactRepeating(AlarmManager.RTC_WAKEUP, millis, AlarmManager.INTERVAL_DAY, intento(context))
    }

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Recordatorio diario", NotificationManager.IMPORTANCE_DEFAULT)
            canal.description = "Te avisa si te faltan misiones del día"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }

    fun avisar(context: Context) {
        val perfil = Perfil(context.getSharedPreferences("perfil", Context.MODE_PRIVATE))
        if (!perfil.listo || !perfil.recordatorio) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val estado = BaseDatos(context).estadoHoy().filter { it.tocaHoy }
        val faltan = estado.filter { !it.cumplido }
        if (estado.isEmpty() || faltan.isEmpty()) return  // nada que recordar
        val nombre = perfil.nombre.substringBefore(" ").ifBlank { "Hola" }
        val titulo = if (faltan.size == 1) "$nombre, te falta 1 misión hoy" else "$nombre, te faltan ${faltan.size} misiones hoy"
        val texto = faltan.take(4).joinToString(" · ") { it.habito.emoji + " " + it.habito.nombre }
        crearCanal(context)
        val abrir = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(CODIGO, aviso)
        } catch (e: SecurityException) {
            // sin permiso de notificaciones
        }
    }
}

class AvisoReceptor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Recordatorio.avisar(context)
    }
}

/** Al prender el celular, vuelve a programar el recordatorio. */
class ArranqueReceptor : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Recordatorio.programar(context)
    }
}
