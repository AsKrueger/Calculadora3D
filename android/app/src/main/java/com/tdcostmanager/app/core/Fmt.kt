package com.tdcostmanager.app.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/** Formato de importes, cantidades y fechas según el idioma y la moneda elegidos. */
class Fmt(val locale: Locale, currencyCode: String) {

    val currency: Currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance("EUR"))

    private val moneyFmt = (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
        currency = this@Fmt.currency
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    val currencySymbol: String get() = currency.getSymbol(locale)

    fun money(value: BigDecimal?): String =
        value?.let { moneyFmt.format(it.setScale(2, RoundingMode.HALF_UP)) } ?: "—"

    /** Importes pequeños (coste por gramo) con más decimales. */
    fun moneyPrecise(value: BigDecimal): String {
        val f = (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
            currency = this@Fmt.currency
            minimumFractionDigits = 2
            maximumFractionDigits = 4
        }
        return f.format(value)
    }

    fun number(value: BigDecimal?, maxDecimals: Int = 2): String {
        if (value == null) return "—"
        val f = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxDecimals
        }
        return f.format(value.setScale(maxDecimals, RoundingMode.HALF_UP))
    }

    fun percent(value: BigDecimal?): String = value?.let { number(it, 1) + " %" } ?: "—"

    /** Texto para un campo editable: sin separador de miles, con la coma o el punto del idioma. */
    fun input(value: BigDecimal?): String {
        if (value == null) return ""
        val plain = value.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString()
        return plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
    }

    fun hours(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        return if (h > 0) "${h} h ${m} min" else "$m min"
    }

    fun date(epochMillis: Long): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    companion object {
        /** Acepta coma o punto decimal y espacios. Null si no es un número. */
        fun parse(text: String): BigDecimal? {
            val t = text.trim().replace(" ", "").replace(' '.toString(), "")
            if (t.isEmpty()) return null
            val normalized = if (t.contains(',') && t.contains('.')) {
                // «1.234,5» o «1,234.5»: el último separador es el decimal
                if (t.lastIndexOf(',') > t.lastIndexOf('.')) t.replace(".", "").replace(',', '.')
                else t.replace(",", "")
            } else t.replace(',', '.')
            return normalized.toBigDecimalOrNull()
        }
    }
}
