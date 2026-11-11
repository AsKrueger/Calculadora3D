package com.tdcostmanager.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.data.PrinterTemplate
import com.tdcostmanager.app.data.Templates
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.NumberField
import com.tdcostmanager.app.ui.common.Picker
import com.tdcostmanager.app.ui.common.Section
import com.tdcostmanager.app.ui.settings.CURRENCIES
import com.tdcostmanager.app.ui.settings.SettingsViewModel
import com.tdcostmanager.app.ui.settings.currencyLabel
import java.math.BigDecimal

/** Configuración en un paso: lo mínimo para que el primer cálculo ya sea realista. Todo se puede saltar. */
@Composable
fun WelcomeScreen(vm: SettingsViewModel = viewModel()) {
    val fmt = LocalFmt.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val pvpc by vm.pvpc.collectAsStateWithLifecycle()
    val spain = fmt.locale.country == "ES"

    var currency by rememberSaveable { mutableStateOf(s?.currency ?: "EUR") }
    var printer by remember { mutableStateOf<PrinterTemplate?>(Templates.printers.first()) }
    var energy by rememberSaveable { mutableStateOf(fmt.input(BigDecimal(if (spain) "0.15" else "0.20"))) }
    var labor by rememberSaveable { mutableStateOf(fmt.input(BigDecimal(15))) }
    var vat by rememberSaveable { mutableStateOf(if (spain) "21" else "0") }

    LaunchedEffect(pvpc) { pvpc?.let { energy = fmt.input(it.average) } }

    fun finish(skip: Boolean) = vm.finishOnboarding(
        printer = if (skip) null else printer,
        currency = currency,
        energy = if (skip) null else Fmt.parse(energy),
        labor = if (skip) null else Fmt.parse(labor),
        vat = if (skip) null else Fmt.parse(vat)
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(MaterialTheme.shapes.large).background(Color(0xFF1F252C)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(80.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { finish(skip = true) }) { Text(stringResource(R.string.welcome_skip)) }
            }
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Section(stringResource(R.string.welcome_step_language)) {
                val languages = listOf("es" to R.string.lang_es, "en" to R.string.lang_en)
                val current = vm.currentLanguage().ifEmpty { fmt.locale.language }
                Picker(
                    stringResource(R.string.settings_language), languages,
                    languages.firstOrNull { it.first == current },
                    labelOf = { stringResource(it.second) },
                    onSelect = { vm.setLanguage(it.first) },
                    modifier = Modifier.fillMaxWidth()
                )
                Picker(
                    stringResource(R.string.settings_currency), (CURRENCIES + currency).distinct(), currency,
                    labelOf = { currencyLabel(it, fmt) }, onSelect = { currency = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Section(stringResource(R.string.welcome_step_printer)) {
                Picker(
                    stringResource(R.string.calc_printer), listOf<PrinterTemplate?>(null) + Templates.printers, printer,
                    labelOf = { it?.name ?: stringResource(R.string.welcome_other_printer) },
                    onSelect = { printer = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.welcome_printer_help), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Section(stringResource(R.string.welcome_step_costs)) {
                NumberField(stringResource(R.string.settings_energy), energy, { energy = it }, Modifier.fillMaxWidth(),
                    suffix = "/kWh", supporting = stringResource(R.string.settings_energy_help))
                if (spain || currency == "EUR") FilledTonalButton(onClick = { vm.loadPvpc(apply = false) }) {
                    Icon(Icons.Outlined.Bolt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pvpc_button))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(stringResource(R.string.calc_rate), labor, { labor = it }, Modifier.weight(1f), suffix = "/h")
                    NumberField(stringResource(R.string.calc_vat), vat, { vat = it }, Modifier.weight(1f), suffix = "%")
                }
                Text(stringResource(R.string.welcome_costs_help), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = { finish(skip = false) },
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp).height(52.dp)
        ) { Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium) }
    }
}
