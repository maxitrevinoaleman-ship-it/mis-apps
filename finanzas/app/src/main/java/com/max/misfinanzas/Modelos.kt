package com.max.misfinanzas

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

/** Un gasto o una ganancia. El uuid sirve para sincronizar con la compu sin duplicar. */
data class Movimiento(
    val uuid: String,
    val tipo: String,          // "gasto" o "ingreso"
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val fecha: LocalDateTime,
    val borrado: Boolean = false,
) {
    val esIngreso get() = tipo == TIPO_INGRESO
    val conSigno get() = if (esIngreso) monto else -monto
}

const val TIPO_GASTO = "gasto"
const val TIPO_INGRESO = "ingreso"

/** Categorías que se proponen la primera vez (cada quien las cambia en Ajustes). */
object Categorias {
    val sugeridasGasto = listOf("Comida", "Transporte", "Escuela", "Diversión", "Ropa", "Tecnología", "Salud", "Hogar",
        "Servicios", "Mascotas", "Regalos", "Otros")
    val sugeridasIngreso = listOf("Sueldo", "Mesada", "Ventas", "Trabajos extra", "Regalo", "Otros")
    val inicialesGasto = listOf("Comida", "Transporte", "Escuela", "Diversión", "Ropa", "Otros")
    val inicialesIngreso = listOf("Mesada", "Ventas", "Regalo", "Otros")
}

/** Monedas que se pueden elegir. */
data class Moneda(val codigo: String, val simbolo: String, val nombre: String)

val MONEDAS = listOf(
    Moneda("MXN", "\$", "Peso mexicano"),
    Moneda("USD", "US\$", "Dólar"),
    Moneda("EUR", "€", "Euro"),
    Moneda("COP", "COL\$", "Peso colombiano"),
    Moneda("ARS", "AR\$", "Peso argentino"),
    Moneda("CLP", "CLP\$", "Peso chileno"),
    Moneda("PEN", "S/", "Sol peruano"),
    Moneda("GTQ", "Q", "Quetzal"),
)

fun monedaDe(codigo: String) = MONEDAS.firstOrNull { it.codigo == codigo } ?: MONEDAS.first()

/** Símbolo de la moneda que eligió la persona (se usa al mostrar cantidades). */
var simboloMoneda: String = "\$"

enum class Periodo(val etiqueta: String, val frase: String) {
    HOY("Hoy", "hoy"),
    SEMANA("Esta semana", "esta semana"),
    MES("Este mes", "este mes"),
    TODO("Todo", "en total");

    fun inicio(): LocalDateTime? {
        val hoy = LocalDate.now()
        return when (this) {
            HOY -> hoy.atStartOfDay()
            SEMANA -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
            MES -> hoy.withDayOfMonth(1).atStartOfDay()
            TODO -> null
        }
    }

    fun incluye(m: Movimiento): Boolean {
        val ini = inicio() ?: return true
        return !m.fecha.isBefore(ini)
    }
}

/** Un punto de la gráfica: el saldo después de un movimiento (mov = null es el inicio). */
data class PuntoSaldo(val mov: Movimiento?, val saldo: Double)

data class Resumen(
    val ingresos: Double,
    val gastos: Double,
    val cantIngresos: Int,
    val cantGastos: Int,
    val saldoTotal: Double,
    val gastosPorCategoria: List<Pair<String, Double>>,
    val ingresosPorCategoria: List<Pair<String, Double>>,
    val serie: List<PuntoSaldo>,
) {
    val balance get() = redondear(ingresos - gastos)
}

fun calcularResumen(todos: List<Movimiento>, periodo: Periodo): Resumen {
    val vivos = todos.filter { !it.borrado }.sortedWith(compareBy<Movimiento>({ it.fecha }, { it.uuid }))
    val enPeriodo = vivos.filter { periodo.incluye(it) }
    val antes = vivos.filter { !periodo.incluye(it) }

    var saldo = antes.sumOf { it.conSigno }
    val serie = mutableListOf(PuntoSaldo(null, redondear(saldo)))
    for (m in enPeriodo) {
        saldo += m.conSigno
        serie.add(PuntoSaldo(m, redondear(saldo)))
    }

    val gastos = enPeriodo.filter { !it.esIngreso }
    val ingresos = enPeriodo.filter { it.esIngreso }
    fun porCategoria(lista: List<Movimiento>) = lista.groupBy { it.categoria }
        .map { (cat, movs) -> cat to redondear(movs.sumOf { it.monto }) }
        .sortedByDescending { it.second }

    return Resumen(
        ingresos = redondear(ingresos.sumOf { it.monto }),
        gastos = redondear(gastos.sumOf { it.monto }),
        cantIngresos = ingresos.size,
        cantGastos = gastos.size,
        saldoTotal = redondear(vivos.sumOf { it.conSigno }),
        gastosPorCategoria = porCategoria(gastos),
        ingresosPorCategoria = porCategoria(ingresos),
        serie = serie,
    )
}

// ------------------------------------------------------------------ formatos

private val MEXICO = Locale("es", "MX")
val FORMATO_GUARDAR: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val FORMATO_DIA: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", MEXICO)
private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", MEXICO)
val FORMATO_FECHA_LARGA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", MEXICO)

fun redondear(x: Double): Double = (x * 100).roundToLong() / 100.0

fun dinero(n: Double, conSigno: Boolean = false): String {
    val valor = abs(redondear(n))
    val formato = NumberFormat.getNumberInstance(MEXICO).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (valor % 1.0 == 0.0) 0 else 2
    }
    val signo = when {
        n < 0 && valor > 0 -> "−"
        conSigno && n > 0 -> "+"
        else -> ""
    }
    return signo + simboloMoneda + formato.format(valor)
}

/** Para los ejes de la gráfica: $1.5k */
fun dineroCorto(n: Double): String {
    val a = abs(n)
    val signo = if (n < 0) "−" else ""
    return if (a >= 1000) {
        val k = a / 1000
        signo + simboloMoneda + (if (k % 1.0 == 0.0) k.toLong().toString() else String.format(MEXICO, "%.1f", k)) + "k"
    } else signo + simboloMoneda + a.roundToLong()
}

fun diaTexto(f: LocalDateTime): String = f.format(FORMATO_DIA).replace(".", "")
fun horaTexto(f: LocalDateTime): String = f.format(FORMATO_HORA)
fun fechaTexto(f: LocalDateTime): String = diaTexto(f) + " · " + horaTexto(f)

/** Escala "bonita" para el eje Y: regresa (mínimo, máximo, marcas). */
fun escalaBonita(min0: Double, max0: Double, marcas: Int = 4): Triple<Double, Double, List<Double>> {
    var min = min0
    var max = max0
    if (min == max) {
        min -= 100.0
        max += 100.0
    }
    val pasoCrudo = (max - min) / marcas
    val magnitud = 10.0.pow(floor(log10(pasoCrudo)))
    val r = pasoCrudo / magnitud
    val paso = magnitud * when {
        r <= 1 -> 1.0
        r <= 2 -> 2.0
        r <= 5 -> 5.0
        else -> 10.0
    }
    val inicio = floor(min / paso) * paso
    val fin = ceil(max / paso) * paso
    val lista = mutableListOf<Double>()
    var v = inicio
    while (v <= fin + paso * 0.001) {
        lista.add(redondear(v))
        v += paso
    }
    return Triple(inicio, fin, lista)
}
