package com.tdcostmanager.app.ui.common

import com.tdcostmanager.app.domain.model.UnitType
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formato de cifras y fechas para la interfaz, siempre en español de España
 * (coma decimal, punto de miles), independientemente del idioma del dispositivo.
 */
object Format {

    private val spanish = Locale("es", "ES")
    private val symbols = DecimalFormatSymbols(spanish)
    private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", spanish)

    private fun pattern(pattern: String) = DecimalFormat(pattern, symbols)

    /** 41,84 € */
    fun euros(value: Double): String = pattern("#,##0.00").format(value) + " €"

    /** 0,12345 €/kWh */
    fun pricePerKWh(value: Double): String = pattern("#,##0.00000").format(value) + " €/kWh"

    /** 1.000 · 1,5 · 0,25 (hasta [maxDecimals] decimales, sin ceros sobrantes) */
    fun number(value: Double, maxDecimals: Int = 2): String =
        pattern("#,##0" + if (maxDecimals > 0) "." + "#".repeat(maxDecimals) else "").format(value)

    /** 145 g · 1,5 kg · 400 ml */
    fun quantity(value: Double, unit: UnitType): String = "${number(value)} ${unit.symbol}"

    /** 1,5 h */
    fun hours(value: Double): String = "${number(value)} h"

    /** 30 % */
    fun percent(value: Double): String = "${number(value)} %"

    /** 3,5 kWh */
    fun energy(value: Double): String = "${number(value, 3)} kWh"

    /**
     * 24/10/2026 11:00. Acepta un instante (`2026-10-24T09:00:00Z`), que se muestra en la zona
     * del dispositivo, o una fecha y hora local (`2026-10-24T11:00:00`). Si no puede leerlo, lo devuelve tal cual.
     */
    fun dateTime(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
        runCatching { Instant.parse(iso).atZone(zone).format(dateTime) }
            .recoverCatching { LocalDateTime.parse(iso).format(dateTime) }
            .getOrDefault(iso)

    /** Valor para precargar un campo editable: sin separador de miles y con coma decimal (1000 · 1,5). */
    fun forInput(value: Double): String = pattern("0.########").format(value)

    /**
     * Lee un número escrito por el usuario. Acepta coma o punto como separador decimal («1,5», «1.5»)
     * y, si aparecen ambos, interpreta el punto como separador de miles («1.234,5»).
     * Devuelve null si el texto está vacío o no es un número.
     */
    fun parseDecimal(text: String): Double? {
        val clean = text.trim().replace(" ", "")
        if (clean.isEmpty()) return null
        val normalized = if (clean.contains(',') && clean.contains('.')) {
            clean.replace(".", "").replace(',', '.')
        } else {
            clean.replace(',', '.')
        }
        if (normalized.count { it == '.' } > 1) return null
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
}
