package com.tdcostmanager.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.CsvExport
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.BuildConfig
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.Ads
import com.tdcostmanager.app.core.Billing
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Favorite
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.core.calc.PriceRounding
import com.tdcostmanager.app.data.settings.AppSettings
import com.tdcostmanager.app.data.settings.ThemeMode
import com.tdcostmanager.app.ui.common.ConfirmDialog
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.NumberField
import com.tdcostmanager.app.ui.common.Picker
import com.tdcostmanager.app.ui.common.Section
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Currency

/** Idiomas de la app, cada uno con su propio nombre. */
val LANGUAGES = listOf(
    "es" to "Español", "en" to "English", "pt" to "Português", "fr" to "Français", "de" to "Deutsch", "it" to "Italiano"
)

val CURRENCIES = listOf(
    "EUR", "USD", "GBP", "MXN", "ARS", "COP", "CLP", "PEN", "UYU", "BRL", "CAD", "AUD", "CHF", "SEK", "NOK", "DKK", "PLN", "JPY", "INR"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBusiness: () -> Unit,
    onFeedback: () -> Unit,
    onPrivacy: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val pvpc by vm.pvpc.collectAsStateWithLifecycle()
    val printers by vm.printers.collectAsStateWithLifecycle()
    val channels by vm.channels.collectAsStateWithLifecycle()
    val privacyRequired by Ads.privacyOptionsRequired.collectAsState()
    val adsRemoved by Billing.adsRemoved.collectAsState()
    val removeAdsProduct by Billing.product.collectAsState()
    val fmt = LocalFmt.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) { vm.messageFlow.collect { snackbar.showSnackbar(it) } }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        confirmRestore = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val settings = s ?: return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            Section(stringResource(R.string.settings_general)) {
                val languages = listOf("" to stringResource(R.string.lang_system)) + LANGUAGES
                val current = vm.currentLanguage()
                Picker(
                    stringResource(R.string.settings_language), languages,
                    languages.firstOrNull { it.first == current } ?: languages.first(),
                    labelOf = { it.second },
                    onSelect = { vm.setLanguage(it.first) },
                    modifier = Modifier.fillMaxWidth()
                )
                val currencies = (CURRENCIES + settings.currency).distinct()
                Picker(
                    stringResource(R.string.settings_currency), currencies, settings.currency,
                    labelOf = { code -> currencyLabel(code, fmt) },
                    onSelect = { code -> vm.update { it.copy(currency = code) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Picker(
                    stringResource(R.string.settings_theme), ThemeMode.entries, settings.theme,
                    labelOf = {
                        stringResource(
                            when (it) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            }
                        )
                    },
                    onSelect = { t -> vm.update { it.copy(theme = t) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DefaultsSection(settings, vm, printers, channels, pvpc, fmt)

            Section(stringResource(R.string.settings_business)) {
                SettingRow(Icons.Outlined.Business, stringResource(R.string.settings_business_row),
                    settings.business.name.ifBlank { stringResource(R.string.settings_business_hint) }, onClick = onBusiness)
            }

            Section(stringResource(R.string.settings_data)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WifiOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_offline_images), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (settings.saveImagesOffline) R.string.settings_offline_images_on else R.string.settings_offline_images_off),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.saveImagesOffline, onCheckedChange = vm::setSaveImagesOffline)
                }
                Text(
                    stringResource(
                        R.string.settings_storage,
                        Formatter.formatShortFileSize(context, storage.imagesBytes),
                        Formatter.formatShortFileSize(context, storage.cacheBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                SettingRow(Icons.Outlined.CleaningServices, stringResource(R.string.settings_clear_cache),
                    stringResource(R.string.settings_clear_cache_help), onClick = vm::clearCache)
                SettingRow(Icons.Outlined.Backup, stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_help)) {
                    exporter.launch("cost3d-backup-${LocalDate.now()}.zip")
                }
                SettingRow(Icons.Outlined.TableChart, stringResource(R.string.settings_csv), stringResource(R.string.settings_csv_help)) {
                    scope.launch {
                        val files = CsvExport.export(context, context.container.db, fmt.locale)
                        val uris = ArrayList(files.map { FileProvider.getUriForFile(context, context.packageName + ".files", it) })
                        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = "text/csv"
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.settings_csv)))
                    }
                }
                SettingRow(Icons.Outlined.Restore, stringResource(R.string.settings_restore), stringResource(R.string.settings_restore_help)) {
                    importer.launch(arrayOf("application/zip", "application/octet-stream"))
                }
                SettingRow(Icons.Outlined.DeleteForever, stringResource(R.string.settings_clear_all),
                    stringResource(R.string.settings_clear_all_help), danger = true) { confirmClear = true }
            }

            Section(stringResource(R.string.settings_help)) {
                if (adsRemoved) SettingRow(Icons.Outlined.Favorite, stringResource(R.string.remove_ads_done),
                    stringResource(R.string.remove_ads_done_help), onClick = null)
                else removeAdsProduct?.let { p ->
                    SettingRow(Icons.Outlined.Block, stringResource(R.string.remove_ads),
                        stringResource(R.string.remove_ads_help, p.oneTimePurchaseOfferDetails?.formattedPrice ?: "")) {
                        (context as? Activity)?.let(Billing::buy)
                    }
                }
                SettingRow(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.settings_feedback),
                    stringResource(R.string.settings_feedback_help), onClick = onFeedback)
                SettingRow(Icons.Outlined.StarRate, stringResource(R.string.settings_rate), stringResource(R.string.settings_rate_help)) {
                    openStore(context)
                }
                SettingRow(Icons.Outlined.Share, stringResource(R.string.settings_share), stringResource(R.string.settings_share_help)) {
                    val text = context.getString(R.string.share_text, storeUrl(context))
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
                }
                SettingRow(Icons.Outlined.PrivacyTip, stringResource(R.string.settings_privacy), stringResource(R.string.settings_privacy_help),
                    onClick = onPrivacy)
                if (privacyRequired) SettingRow(Icons.Outlined.PrivacyTip, stringResource(R.string.settings_ads_privacy),
                    stringResource(R.string.settings_ads_privacy_help)) {
                    (context as? Activity)?.let(Ads::showPrivacyOptions)
                }
                SettingRow(Icons.Outlined.Info, stringResource(R.string.settings_version), BuildConfig.VERSION_NAME, onClick = null)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmClear) ConfirmDialog(
        title = stringResource(R.string.clear_all_title),
        body = stringResource(R.string.clear_all_body),
        confirm = stringResource(R.string.clear_all_confirm),
        onConfirm = vm::clearAllData,
        onDismiss = { confirmClear = false }
    )
    confirmRestore?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.restore_title),
            body = stringResource(R.string.restore_body),
            confirm = stringResource(R.string.restore_confirm),
            onConfirm = { vm.restoreBackup(uri) },
            onDismiss = { confirmRestore = null }
        )
    }
}

fun currencyLabel(code: String, fmt: Fmt): String = runCatching {
    val c = Currency.getInstance(code)
    "$code · ${c.getDisplayName(fmt.locale).replaceFirstChar { it.titlecase(fmt.locale) }} (${c.getSymbol(fmt.locale)})"
}.getOrDefault(code)

fun storeUrl(context: android.content.Context) =
    "https://play.google.com/store/apps/details?id=" + context.packageName.removeSuffix(".debug")

fun openStore(context: android.content.Context) {
    val pkg = context.packageName.removeSuffix(".debug")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl(context)))) }
    }
}

@Composable
private fun DefaultsSection(
    s: AppSettings,
    vm: SettingsViewModel,
    printers: List<com.tdcostmanager.app.data.db.PrinterEntity>,
    channels: List<com.tdcostmanager.app.data.db.ChannelEntity>,
    pvpc: com.tdcostmanager.app.core.PvpcDay?,
    fmt: Fmt
) {
    var energy by remember(s.energyPrice) { mutableStateOf(fmt.input(s.energyPrice)) }
    var labor by remember { mutableStateOf(fmt.input(s.laborRate)) }
    var failure by remember { mutableStateOf(fmt.input(s.failurePercent)) }
    var vat by remember { mutableStateOf(fmt.input(s.vatPercent)) }
    var markup by remember { mutableStateOf(fmt.input(s.pricingValue)) }

    fun save(text: String, apply: (AppSettings, BigDecimal) -> AppSettings) {
        Fmt.parse(text)?.let { v -> vm.update { apply(it, v) } }
    }

    Section(stringResource(R.string.settings_defaults)) {
        Text(stringResource(R.string.settings_defaults_help), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumberField(stringResource(R.string.settings_energy), energy, { energy = it; save(it) { st, v -> st.copy(energyPrice = v) } },
            Modifier.fillMaxWidth(), suffix = fmt.currencySymbol + "/kWh", supporting = stringResource(R.string.settings_energy_help))
        if (fmt.locale.country == "ES" || s.currency == "EUR") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { vm.loadPvpc(apply = true) }) {
                    Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pvpc_button))
                }
            }
            pvpc?.let { day ->
                Text(
                    stringResource(R.string.pvpc_result, fmt.moneyPrecise(day.average)) +
                        (day.cheapestWindowStart?.let { h -> " " + stringResource(R.string.pvpc_cheapest, h, h + 3) } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        NumberField(stringResource(R.string.calc_rate), labor, { labor = it; save(it) { st, v -> st.copy(laborRate = v) } },
            Modifier.fillMaxWidth(), suffix = fmt.currencySymbol + "/h")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(stringResource(R.string.calc_failure), failure, { failure = it; save(it) { st, v -> st.copy(failurePercent = v.min(BigDecimal(90))) } },
                Modifier.weight(1f), suffix = "%")
            NumberField(stringResource(R.string.calc_vat), vat, { vat = it; save(it) { st, v -> st.copy(vatPercent = v) } },
                Modifier.weight(1f), suffix = "%")
        }
        NumberField(stringResource(R.string.price_markup), markup, { markup = it; save(it) { st, v -> st.copy(pricingValue = v) } },
            Modifier.fillMaxWidth(), suffix = "%", supporting = stringResource(R.string.settings_markup_help))
        Picker(
            stringResource(R.string.settings_rounding), PriceRounding.entries, s.rounding,
            labelOf = { r -> r.step?.let { stringResource(R.string.rounding_to, fmt.money(it)) } ?: stringResource(R.string.rounding_none) },
            onSelect = { r -> vm.update { it.copy(rounding = r) } },
            modifier = Modifier.fillMaxWidth()
        )
        if (printers.isNotEmpty()) Picker(
            stringResource(R.string.settings_default_printer), printers, printers.firstOrNull { it.id == s.defaultPrinterId },
            labelOf = { it.name }, onSelect = { p -> vm.update { it.copy(defaultPrinterId = p.id) } },
            modifier = Modifier.fillMaxWidth()
        )
        if (channels.isNotEmpty()) Picker(
            stringResource(R.string.settings_default_channel), channels, channels.firstOrNull { it.id == s.defaultChannelId },
            labelOf = { it.name }, onSelect = { ch -> vm.update { it.copy(defaultChannelId = ch.id) } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: (() -> Unit)?
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

/** Pantalla con flecha atrás para las subpantallas de Ajustes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { content() }
    }
}
