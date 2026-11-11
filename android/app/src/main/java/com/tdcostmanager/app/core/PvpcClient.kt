package com.tdcostmanager.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** Precio de la luz de un día en España: media y franja más barata (€/kWh). */
data class PvpcDay(
    val date: LocalDate,
    val average: BigDecimal,
    val hourly: List<Pair<Int, BigDecimal>>
) {
    /** Hora de inicio de las 3 horas seguidas más baratas. */
    val cheapestWindowStart: Int?
        get() {
            if (hourly.size < 3) return null
            val byHour = hourly.toMap()
            return (0..21).filter { h -> (h..h + 2).all { it in byHour } }
                .minByOrNull { h -> (h..h + 2).sumOf { byHour.getValue(it).toDouble() } }
        }
}

/**
 * PVPC de Red Eléctrica (API pública REData, sin token). Solo se llama cuando el usuario
 * pulsa el botón; la app nunca depende de ella para calcular.
 */
object PvpcClient {

    private val MADRID = ZoneId.of("Europe/Madrid")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun today(): PvpcDay = withContext(Dispatchers.IO) {
        val day = LocalDate.now(MADRID)
        val url = URL(
            "https://apidatos.ree.es/es/datos/mercados/precios-mercados-tiempo-real" +
                "?start_date=${day}T00:00&end_date=${day}T23:59&time_trunc=hour"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            parse(conn.inputStream.bufferedReader().readText(), day)
        } finally {
            conn.disconnect()
        }
    }

    fun parse(body: String, day: LocalDate): PvpcDay {
        val root = json.parseToJsonElement(body).jsonObject
        val pvpc = root["included"]!!.jsonArray.map { it.jsonObject }
            .firstOrNull { (it["type"]?.jsonPrimitive?.content ?: "").equals("PVPC", true) }
            ?: error("PVPC not found")
        val values = (pvpc["attributes"] as JsonObject)["values"]!!.jsonArray.map { it.jsonObject }
        val hourly = values.map { v ->
            val hour = OffsetDateTime.parse(v["datetime"]!!.jsonPrimitive.content).atZoneSameInstant(MADRID).hour
            // La API da €/MWh
            val price = BigDecimal(v["value"]!!.jsonPrimitive.content).divide(BigDecimal(1000), 5, RoundingMode.HALF_UP)
            hour to price
        }.distinctBy { it.first }
        require(hourly.isNotEmpty()) { "no values" }
        val avg = hourly.fold(BigDecimal.ZERO) { a, p -> a + p.second }
            .divide(BigDecimal(hourly.size), 4, RoundingMode.HALF_UP)
        return PvpcDay(day, avg, hourly)
    }
}
