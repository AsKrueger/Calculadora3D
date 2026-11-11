package com.tdcostmanager.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdcostmanager.app.core.calc.PriceRounding
import com.tdcostmanager.app.core.calc.PricingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class BusinessInfo(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val taxId: String = "",
    /** Texto al pie del presupuesto: validez, forma de pago… */
    val quoteFooter: String = "",
    val logo: String? = null
)

data class AppSettings(
    val currency: String,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val energyPrice: BigDecimal = BigDecimal("0.20"),
    val vatPercent: BigDecimal = BigDecimal.ZERO,
    val failurePercent: BigDecimal = BigDecimal("5"),
    val laborRate: BigDecimal = BigDecimal("15"),
    val pricingMode: PricingMode = PricingMode.MARKUP,
    val pricingValue: BigDecimal = BigDecimal("40"),
    val rounding: PriceRounding = PriceRounding.CENTS_10,
    val defaultPrinterId: Long? = null,
    val defaultChannelId: Long? = null,
    val business: BusinessInfo = BusinessInfo(),
    val saveImagesOffline: Boolean = true,
    val onboardingDone: Boolean = false,
    val seeded: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object K {
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme")
        val energy = stringPreferencesKey("energy_price")
        val vat = stringPreferencesKey("vat")
        val failure = stringPreferencesKey("failure")
        val labor = stringPreferencesKey("labor_rate")
        val pricingMode = stringPreferencesKey("pricing_mode")
        val pricingValue = stringPreferencesKey("pricing_value")
        val rounding = stringPreferencesKey("rounding")
        val printer = longPreferencesKey("default_printer")
        val channel = longPreferencesKey("default_channel")
        val bizName = stringPreferencesKey("biz_name")
        val bizEmail = stringPreferencesKey("biz_email")
        val bizPhone = stringPreferencesKey("biz_phone")
        val bizAddress = stringPreferencesKey("biz_address")
        val bizTax = stringPreferencesKey("biz_tax")
        val bizFooter = stringPreferencesKey("biz_footer")
        val bizLogo = stringPreferencesKey("biz_logo")
        val saveImages = booleanPreferencesKey("save_images_offline")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val seeded = booleanPreferencesKey("seeded")
        val draft = stringPreferencesKey("calc_draft")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map(::readFrom)

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val old = readFrom(p)
            val s = transform(old)
            p[K.currency] = s.currency
            p[K.theme] = s.theme.name
            p[K.energy] = s.energyPrice.toPlainString()
            p[K.vat] = s.vatPercent.toPlainString()
            p[K.failure] = s.failurePercent.toPlainString()
            p[K.labor] = s.laborRate.toPlainString()
            p[K.pricingMode] = s.pricingMode.name
            p[K.pricingValue] = s.pricingValue.toPlainString()
            p[K.rounding] = s.rounding.name
            s.defaultPrinterId?.let { p[K.printer] = it } ?: p.remove(K.printer)
            s.defaultChannelId?.let { p[K.channel] = it } ?: p.remove(K.channel)
            p[K.bizName] = s.business.name
            p[K.bizEmail] = s.business.email
            p[K.bizPhone] = s.business.phone
            p[K.bizAddress] = s.business.address
            p[K.bizTax] = s.business.taxId
            p[K.bizFooter] = s.business.quoteFooter
            s.business.logo?.let { p[K.bizLogo] = it } ?: p.remove(K.bizLogo)
            p[K.saveImages] = s.saveImagesOffline
            p[K.onboarding] = s.onboardingDone
            p[K.seeded] = s.seeded
        }
    }

    private fun readFrom(p: Preferences): AppSettings = AppSettings(
        currency = p[K.currency] ?: defaultCurrency(),
        theme = p[K.theme].enumOr(ThemeMode.SYSTEM),
        energyPrice = p[K.energy].decOr("0.20"),
        vatPercent = p[K.vat].decOr("0"),
        failurePercent = p[K.failure].decOr("5"),
        laborRate = p[K.labor].decOr("15"),
        pricingMode = p[K.pricingMode].enumOr(PricingMode.MARKUP),
        pricingValue = p[K.pricingValue].decOr("40"),
        rounding = p[K.rounding].enumOr(PriceRounding.CENTS_10),
        defaultPrinterId = p[K.printer],
        defaultChannelId = p[K.channel],
        business = BusinessInfo(
            p[K.bizName] ?: "", p[K.bizEmail] ?: "", p[K.bizPhone] ?: "",
            p[K.bizAddress] ?: "", p[K.bizTax] ?: "", p[K.bizFooter] ?: "", p[K.bizLogo]
        ),
        saveImagesOffline = p[K.saveImages] ?: true,
        onboardingDone = p[K.onboarding] ?: false,
        seeded = p[K.seeded] ?: false
    )

    /** Borrador de la calculadora, para no perder lo tecleado si Android cierra la app. */
    val draft: Flow<String?> = context.dataStore.data.map { it[K.draft] }

    suspend fun saveDraft(json: String?) {
        context.dataStore.edit { p -> if (json == null) p.remove(K.draft) else p[K.draft] = json }
    }

    /** Todos los ajustes como texto, para la copia de seguridad. */
    suspend fun exportAll(): Map<String, String> =
        context.dataStore.data.first().asMap()
            .filterKeys { it.name != K.draft.name }
            .mapKeys { it.key.name }
            .mapValues { it.value.toString() }

    suspend fun importAll(values: Map<String, String>) {
        context.dataStore.edit { p ->
            p.clear()
            val bools = setOf(K.saveImages.name, K.onboarding.name, K.seeded.name)
            val longs = setOf(K.printer.name, K.channel.name)
            values.forEach { (k, v) ->
                when (k) {
                    in bools -> p[booleanPreferencesKey(k)] = v.toBoolean()
                    in longs -> v.toLongOrNull()?.let { p[longPreferencesKey(k)] = it }
                    else -> p[stringPreferencesKey(k)] = v
                }
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        fun defaultCurrency(): String = runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("EUR")
    }
}

private fun String?.decOr(default: String): BigDecimal =
    this?.toBigDecimalOrNull() ?: BigDecimal(default)

private inline fun <reified T : Enum<T>> String?.enumOr(default: T): T =
    this?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default
