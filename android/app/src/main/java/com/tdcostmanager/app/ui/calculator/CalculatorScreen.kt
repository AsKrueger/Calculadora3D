package com.tdcostmanager.app.ui.calculator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tdcostmanager.app.R
import com.tdcostmanager.app.core.calc.CostPart
import com.tdcostmanager.app.core.calc.CostResult
import com.tdcostmanager.app.core.calc.PricingMode
import com.tdcostmanager.app.data.db.MaterialUnit
import com.tdcostmanager.app.ui.common.FoldSection
import com.tdcostmanager.app.ui.common.LedgerRow
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.NumberField
import com.tdcostmanager.app.ui.common.PieceImage
import com.tdcostmanager.app.ui.common.Picker
import com.tdcostmanager.app.ui.common.ProportionBar
import com.tdcostmanager.app.ui.common.Section
import com.tdcostmanager.app.ui.common.Swatch
import com.tdcostmanager.app.ui.common.TextInput
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.tabular
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    vm: CalculatorViewModel,
    onOpenCatalog: () -> Unit,
    onOpenPieces: () -> Unit
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val f = state.form
    val fmt = LocalFmt.current
    val snackbar = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var breakdownY by remember { mutableStateOf(0) }
    var showSave by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    val savedText = stringResource(R.string.calc_saved)
    val seePieces = stringResource(R.string.calc_see_pieces)

    LaunchedEffect(Unit) {
        vm.eventFlow.collect { e ->
            when (e) {
                is CalcEvent.Message -> snackbar.showSnackbar(e.text)
                is CalcEvent.Saved -> {
                    (context as? android.app.Activity)?.let(com.tdcostmanager.app.core.Review::onPieceSaved)
                    val r = snackbar.showSnackbar(savedText.format(e.name), actionLabel = seePieces)
                    if (r == androidx.compose.material3.SnackbarResult.ActionPerformed) onOpenPieces()
                }
            }
        }
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFile)
    }
    LaunchedEffect(Unit) { vm.importRequestFlow.collect { importer.launch(arrayOf("*/*")) } }
    val jobs by com.tdcostmanager.app.core.PrintTimer.jobs.collectAsStateWithLifecycle()
    val timerStarted = stringResource(R.string.timer_started)
    val timerNoPermission = stringResource(R.string.timer_no_permission)
    fun startTimer() {
        val ms = CalculatorViewModel.hoursOf(f).multiply(BigDecimal(3_600_000)).toLong()
        val job = com.tdcostmanager.app.core.PrintTimer.start(context, f.name.ifBlank { context.getString(R.string.timer_default_name) }, ms)
        val time = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            .format(java.time.Instant.ofEpochMilli(job.endAt).atZone(java.time.ZoneId.systemDefault()))
        scope.launch { snackbar.showSnackbar(timerStarted.format(time)) }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        startTimer()
        if (!granted) scope.launch { snackbar.showSnackbar(timerNoPermission) }
    }
    fun onTimerClick() {
        val needs = android.os.Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needs) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) else startTimer()
    }
    val photo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::pickImage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            f.name.ifBlank { stringResource(R.string.calc_title) },
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (f.pieceId != null) Text(
                            stringResource(R.string.calc_editing),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { importer.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Outlined.FileOpen, stringResource(R.string.calc_import))
                    }
                    IconButton(onClick = { if (state.hasInput) confirmNew = true else vm.newCalculation() }) {
                        Icon(Icons.AutoMirrored.Outlined.NoteAdd, stringResource(R.string.calc_new))
                    }
                    IconButton(onClick = { showSave = true }, enabled = state.result != null && state.hasInput) {
                        Icon(Icons.Outlined.Save, stringResource(R.string.calc_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            ResultBar(state.result, state.hasInput) {
                scope.launch { scroll.animateScrollTo(breakdownY) }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Se queda mientras el cálculo sea nuevo: si desapareciera al teclear, el formulario saltaría bajo el dedo
            jobs.forEach { job -> RunningJob(job) { com.tdcostmanager.app.core.PrintTimer.cancel(context, job.id) } }
            if (f.pieceId == null && f.image == null) ImportHint(
                onImport = { importer.launch(arrayOf("*/*")) },
                onExample = if (!state.hasInput) vm::loadExample else null
            )

            PrintSection(state, vm, onOpenCatalog)
            if (CalculatorViewModel.hoursOf(f).signum() > 0) {
                OutlinedButton(onClick = ::onTimerClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.timer_start))
                }
            }
            MaterialSection(state, vm, onOpenCatalog)
            LaborSection(state, vm)
            ExtrasSection(state, vm)
            PriceSection(state, vm)

            Box(Modifier.onGloballyPositioned { breakdownY = it.positionInParent().y.toInt() }) {
                Breakdown(state)
            }
            if (state.hasInput && state.catalog.channels.size > 1) {
                ChannelComparison(state) { id -> vm.edit { it.copy(channelId = id) } }
            }

            Section(stringResource(R.string.calc_section_piece)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PieceImage(f.image, 72.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(onClick = {
                            photo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text(stringResource(if (f.image == null) R.string.calc_add_photo else R.string.calc_change_photo)) }
                        if (f.image != null) TextButton(onClick = { vm.setImage(null) }) {
                            Text(stringResource(R.string.calc_remove_photo))
                        }
                    }
                }
                TextInput(stringResource(R.string.calc_notes), f.notes, { v -> vm.edit { it.copy(notes = v) } }, singleLine = false)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showSave) SaveDialog(
        initial = f.name,
        existing = f.pieceId != null,
        onDismiss = { showSave = false },
        onSave = { name, asNew -> vm.save(name, asNew); showSave = false }
    )
    if (confirmNew) AlertDialog(
        onDismissRequest = { confirmNew = false },
        title = { Text(stringResource(R.string.calc_new_title)) },
        text = { Text(stringResource(if (f.pieceId != null) R.string.calc_new_body_saved else R.string.calc_new_body)) },
        confirmButton = { TextButton(onClick = { vm.newCalculation(); confirmNew = false }) { Text(stringResource(R.string.calc_new)) } },
        dismissButton = { TextButton(onClick = { confirmNew = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Impresión en marcha: a qué hora termina y cómo cancelar el aviso. */
@Composable
private fun RunningJob(job: com.tdcostmanager.app.core.PrintJob, onCancel: () -> Unit) {
    val end = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        .format(java.time.Instant.ofEpochMilli(job.endAt).atZone(java.time.ZoneId.systemDefault()))
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Timer, null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(job.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.timer_running, end), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onCancel) { Icon(Icons.Outlined.Close, stringResource(R.string.timer_cancel)) }
        }
    }
}

@Composable
private fun ImportHint(onImport: () -> Unit, onExample: (() -> Unit)?) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.calc_import_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.calc_import_body), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalButton(onClick = onImport) { Text(stringResource(R.string.calc_import_short)) }
                if (onExample != null) TextButton(onClick = onExample) { Text(stringResource(R.string.calc_example)) }
            }
        }
    }
}

@Composable
private fun PrintSection(state: CalcUi, vm: CalculatorViewModel, onOpenCatalog: () -> Unit) {
    val f = state.form
    Section(stringResource(R.string.calc_section_print)) {
        Picker(
            label = stringResource(R.string.calc_printer),
            options = state.catalog.printers,
            selected = state.printer,
            labelOf = { it.name },
            onSelect = { p -> vm.edit { it.copy(printerId = p.id) } },
            placeholder = stringResource(R.string.calc_no_printer),
            modifier = Modifier.fillMaxWidth(),
            footer = { close ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.calc_manage_printers)) },
                    leadingIcon = { Icon(Icons.Outlined.Add, null) },
                    onClick = { close(); onOpenCatalog() }
                )
            }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(stringResource(R.string.calc_hours), f.hours, { v -> vm.edit { it.copy(hours = v) } },
                Modifier.weight(1f), suffix = "h", integer = true)
            NumberField(stringResource(R.string.calc_minutes), f.minutes, { v -> vm.edit { it.copy(minutes = v) } },
                Modifier.weight(1f), suffix = "min", integer = true)
        }
        NumberField(
            stringResource(R.string.calc_per_plate), f.perPlate, { v -> vm.edit { it.copy(perPlate = v) } },
            Modifier.fillMaxWidth(), integer = true,
            supporting = stringResource(R.string.calc_per_plate_help)
        )
    }
}

@Composable
private fun MaterialSection(state: CalcUi, vm: CalculatorViewModel, onOpenCatalog: () -> Unit) {
    val f = state.form
    val materials = state.catalog.materials
    Section(
        stringResource(R.string.calc_section_material),
        trailing = {
            TextButton(onClick = vm::addMaterial) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.calc_add_material))
            }
        }
    ) {
        f.materials.forEach { row ->
            val m = materials.firstOrNull { it.id == row.materialId }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Picker(
                    label = stringResource(R.string.calc_material),
                    options = materials,
                    selected = m,
                    labelOf = { it.name },
                    leading = { Swatch(it.color) },
                    onSelect = { sel -> vm.edit { fm -> fm.copy(materials = fm.materials.map { if (it.key == row.key) it.copy(materialId = sel.id) else it }) } },
                    placeholder = stringResource(R.string.calc_pick_material),
                    modifier = Modifier.weight(1.4f),
                    footer = { close ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.calc_manage_materials)) },
                            leadingIcon = { Icon(Icons.Outlined.Add, null) },
                            onClick = { close(); onOpenCatalog() }
                        )
                    }
                )
                NumberField(
                    label = stringResource(R.string.calc_amount),
                    value = row.amount,
                    onValueChange = { v -> vm.edit { fm -> fm.copy(materials = fm.materials.map { if (it.key == row.key) it.copy(amount = v) else it }) } },
                    suffix = if (m?.unit == MaterialUnit.ML) "ml" else "g",
                    modifier = Modifier.weight(1f)
                )
                if (f.materials.size > 1) IconButton(onClick = { vm.removeMaterial(row.key) }) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.action_remove))
                }
            }
        }
        val unit = if (f.materials.firstNotNullOfOrNull { r -> materials.firstOrNull { it.id == r.materialId } }?.unit == MaterialUnit.ML) "ml" else "g"
        NumberField(
            stringResource(R.string.calc_waste), f.waste, { v -> vm.edit { it.copy(waste = v) } },
            Modifier.fillMaxWidth(), suffix = unit,
            supporting = stringResource(R.string.calc_waste_help)
        )
    }
}

@Composable
private fun LaborSection(state: CalcUi, vm: CalculatorViewModel) {
    val f = state.form
    val fmt = LocalFmt.current
    var open by rememberSaveable { mutableStateOf(false) }
    val dash = { v: String -> v.ifBlank { "0" } }
    FoldSection(
        title = stringResource(R.string.calc_section_labor),
        summary = listOf(
            stringResource(R.string.calc_prep) + " " + dash(f.prep) + " min",
            stringResource(R.string.calc_post) + " " + dash(f.post) + " min",
            dash(f.laborRate) + " " + fmt.currencySymbol + "/h",
            stringResource(R.string.calc_failure) + " " + dash(f.failure) + " %"
        ).joinToString(" · "),
        expanded = open,
        onToggle = { open = !open }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(stringResource(R.string.calc_prep), f.prep, { v -> vm.edit { it.copy(prep = v) } },
                Modifier.weight(1f), suffix = "min", supporting = stringResource(R.string.calc_prep_help))
            NumberField(stringResource(R.string.calc_post), f.post, { v -> vm.edit { it.copy(post = v) } },
                Modifier.weight(1f), suffix = "min", supporting = stringResource(R.string.calc_post_help))
        }
        NumberField(stringResource(R.string.calc_rate), f.laborRate, { v -> vm.edit { it.copy(laborRate = v) } },
            Modifier.fillMaxWidth(), suffix = fmt.currencySymbol + "/h")
        NumberField(
            stringResource(R.string.calc_failure), f.failure, { v -> vm.edit { it.copy(failure = v) } },
            Modifier.fillMaxWidth(), suffix = "%", supporting = stringResource(R.string.calc_failure_help),
            isError = (com.tdcostmanager.app.core.Fmt.parse(f.failure) ?: BigDecimal.ZERO) > BigDecimal(90)
        )
    }
}

@Composable
private fun ExtrasSection(state: CalcUi, vm: CalculatorViewModel) {
    val f = state.form
    val fmt = LocalFmt.current
    var menu by remember { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf(false) }
    FoldSection(
        title = stringResource(R.string.calc_section_extras),
        summary = f.extras.mapNotNull { row ->
            state.catalog.extras.firstOrNull { it.id == row.extraId }?.let { it.name + " ×" + row.qty.ifBlank { "0" } }
        }.joinToString(", ").ifEmpty { stringResource(R.string.calc_extras_empty) },
        expanded = open,
        onToggle = { open = !open }
    ) {
        Box {
            TextButton(onClick = { menu = true }, enabled = state.catalog.extras.isNotEmpty()) {
                Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.calc_add_extra))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                state.catalog.extras.forEach { e ->
                    DropdownMenuItem(
                        text = { Text(e.name + " · " + fmt.money(e.unitCost)) },
                        onClick = { vm.addExtra(e.id); menu = false }
                    )
                }
            }
        }
        if (f.extras.isEmpty()) Text(
            stringResource(R.string.calc_extras_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        f.extras.forEach { row ->
            val e = state.catalog.extras.firstOrNull { it.id == row.extraId }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1.4f)) {
                    Text(e?.name ?: "—", style = MaterialTheme.typography.bodyLarge)
                    Text(e?.let { fmt.money(it.unitCost) } ?: "", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                NumberField(
                    stringResource(R.string.calc_per_piece), row.qty,
                    { v -> vm.edit { fm -> fm.copy(extras = fm.extras.map { if (it.key == row.key) it.copy(qty = v) else it }) } },
                    Modifier.weight(1f)
                )
                IconButton(onClick = { vm.removeExtra(row.key) }) { Icon(Icons.Outlined.Close, stringResource(R.string.action_remove)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceSection(state: CalcUi, vm: CalculatorViewModel) {
    val f = state.form
    val fmt = LocalFmt.current
    var open by rememberSaveable { mutableStateOf(false) }
    val modeLabel = stringResource(
        when (f.mode) {
            PricingMode.MARKUP -> R.string.mode_markup
            PricingMode.TARGET_PROFIT -> R.string.mode_profit
            PricingMode.FIXED_PRICE -> R.string.mode_fixed
        }
    ).removeSuffix(" %")
    FoldSection(
        title = stringResource(R.string.calc_section_price),
        summary = listOfNotNull(
            modeLabel + " " + f.priceValue.ifBlank { "0" } + (if (f.mode == PricingMode.MARKUP) " %" else " " + fmt.currencySymbol),
            state.channel?.name,
            stringResource(R.string.calc_vat) + " " + f.vat.ifBlank { "0" } + " %"
        ).joinToString(" · "),
        expanded = open,
        onToggle = { open = !open }
    ) {
        val modes = listOf(
            PricingMode.MARKUP to R.string.mode_markup,
            PricingMode.TARGET_PROFIT to R.string.mode_profit,
            PricingMode.FIXED_PRICE to R.string.mode_fixed
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { i, (mode, label) ->
                SegmentedButton(
                    selected = f.mode == mode,
                    onClick = { vm.edit { it.copy(mode = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                    label = { Text(stringResource(label), maxLines = 1) }
                )
            }
        }
        NumberField(
            label = stringResource(
                when (f.mode) {
                    PricingMode.MARKUP -> R.string.price_markup
                    PricingMode.TARGET_PROFIT -> R.string.price_profit
                    PricingMode.FIXED_PRICE -> R.string.price_fixed
                }
            ),
            value = f.priceValue,
            onValueChange = { v -> vm.edit { it.copy(priceValue = v) } },
            suffix = if (f.mode == PricingMode.MARKUP) "%" else fmt.currencySymbol,
            supporting = stringResource(
                when (f.mode) {
                    PricingMode.MARKUP -> R.string.price_markup_help
                    PricingMode.TARGET_PROFIT -> R.string.price_profit_help
                    PricingMode.FIXED_PRICE -> R.string.price_fixed_help
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Picker(
                label = stringResource(R.string.calc_channel),
                options = state.catalog.channels,
                selected = state.channel,
                labelOf = { it.name },
                onSelect = { ch -> vm.edit { it.copy(channelId = ch.id) } },
                modifier = Modifier.weight(1.4f)
            )
            NumberField(stringResource(R.string.calc_vat), f.vat, { v -> vm.edit { it.copy(vat = v) } },
                Modifier.weight(1f), suffix = "%")
        }
        state.channel?.takeIf { it.feePercent.signum() > 0 || it.fixedFee.signum() > 0 }?.let { ch ->
            Text(
                stringResource(R.string.calc_channel_fee, fmt.number(ch.feePercent), fmt.money(ch.fixedFee)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun partLabel(part: CostPart): String = stringResource(
    when (part) {
        CostPart.MATERIAL -> R.string.part_material
        CostPart.ENERGY -> R.string.part_energy
        CostPart.WEAR -> R.string.part_wear
        CostPart.LABOR -> R.string.part_labor
        CostPart.EXTRAS -> R.string.part_extras
        CostPart.FAILURE -> R.string.part_failure
    }
)

@Composable
private fun Breakdown(state: CalcUi) {
    val r = state.result
    val fmt = LocalFmt.current
    val colors = AppThemeExtras.breakdownColors
    Section(stringResource(R.string.calc_section_result), Modifier.animateContentSize()) {
        if (r == null || !state.hasInput) {
            Text(
                stringResource(R.string.calc_result_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Section
        }
        Text(stringResource(R.string.calc_price_per_piece), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (r.feasible) fmt.money(r.price) else "—",
            style = MaterialTheme.typography.displaySmall.tabular(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Insights(r, state)
        ProportionBar(r.parts.mapIndexed { i, (_, v) -> v.toFloat() to colors[i] })
        r.parts.forEachIndexed { i, (part, value) ->
            if (value.signum() > 0 || part == CostPart.MATERIAL) LedgerRow(
                partLabel(part), fmt.money(value), dot = colors[i],
                hint = when (part) {
                    CostPart.ENERGY -> stringResource(R.string.calc_energy_hint, fmt.number(r.energyKwhPerPiece, 3),
                        fmt.moneyPrecise(state.settings?.energyPrice ?: BigDecimal.ZERO))
                    else -> null
                }
            )
        }
        HorizontalDivider()
        LedgerRow(stringResource(R.string.calc_total_cost), fmt.money(r.totalCost), strong = true)
        if (r.channelFees.signum() > 0) LedgerRow(
            stringResource(R.string.calc_fees_of, state.channel?.name ?: ""), "− " + fmt.money(r.channelFees)
        )
        if (r.vat.signum() > 0) LedgerRow(stringResource(R.string.calc_vat_amount), "− " + fmt.money(r.vat))
        LedgerRow(
            stringResource(R.string.calc_profit), fmt.money(r.profit), strong = true,
            valueColor = if (r.profit.signum() < 0) MaterialTheme.colorScheme.error else AppThemeExtras.tone(com.tdcostmanager.app.ui.theme.Tone.Success).content
        )
        if (r.piecesPerPlate > 1) {
            HorizontalDivider()
            val n = BigDecimal(r.piecesPerPlate)
            LedgerRow(
                stringResource(R.string.calc_plate_of, r.piecesPerPlate),
                fmt.money(r.price * n),
                hint = stringResource(R.string.calc_plate_hint, fmt.money(r.totalCost * n), fmt.money(r.profit * n))
            )
        }
    }
}

/**
 * El mismo cálculo en todos los canales a la vez: a cuánto hay que vender en cada uno para ganar lo
 * mismo (o, con precio fijo, cuánto queda en cada uno). Tocar un canal lo elige.
 */
@Composable
private fun ChannelComparison(state: CalcUi, onSelect: (Long) -> Unit) {
    val fmt = LocalFmt.current
    val rows = remember(state.form, state.catalog, state.settings) {
        state.catalog.channels.map { ch ->
            ch to CalculatorViewModel.compute(state.form.copy(channelId = ch.id), state.catalog, state.settings)
        }
    }
    val fixed = state.form.mode == PricingMode.FIXED_PRICE
    val best = rows.filter { it.second?.feasible == true }.let { list ->
        if (fixed) list.maxByOrNull { it.second!!.profit } else list.minByOrNull { it.second!!.price }
    }?.first?.id
    Section(stringResource(R.string.channels_compare_title)) {
        Text(
            stringResource(if (fixed) R.string.channels_compare_help_fixed else R.string.channels_compare_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        rows.forEach { (ch, r) ->
            val selected = ch.id == state.form.channelId
            Surface(
                onClick = { onSelect(ch.id) },
                shape = MaterialTheme.shapes.medium,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                ch.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                            )
                            if (ch.id == best && rows.size > 1) {
                                Spacer(Modifier.width(8.dp))
                                com.tdcostmanager.app.ui.common.Chip(
                                    stringResource(if (fixed) R.string.channels_compare_best_profit else R.string.channels_compare_best_price),
                                    com.tdcostmanager.app.ui.theme.Tone.Success
                                )
                            }
                        }
                        Text(
                            when {
                                r == null || !r.feasible -> stringResource(R.string.channels_compare_impossible)
                                else -> stringResource(R.string.channels_compare_row, fmt.money(r.channelFees), fmt.money(r.profit))
                            },
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = if (r != null && r.profit.signum() < 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (r != null && r.feasible) fmt.money(r.price) else "—",
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Frases que explican el precio: qué pesa más y cuánto queda limpio. */
@Composable
private fun Insights(r: CostResult, state: CalcUi) {
    val fmt = LocalFmt.current
    val lines = buildList {
        if (!r.feasible) add(stringResource(R.string.insight_impossible))
        else if (r.profit.signum() < 0) add(stringResource(R.string.insight_loss, fmt.money(r.profit.negate())))
        else r.marginPercent?.let { add(stringResource(R.string.insight_profit, fmt.money(r.profit), fmt.number(it, 0))) }
        r.dominantPart?.let { (part, pct) ->
            if (pct >= 40) add(
                stringResource(R.string.insight_dominant, partLabel(part).lowercase(fmt.locale), pct)
                    .replaceFirstChar { it.titlecase(fmt.locale) }
            )
        }
        if (r.channelFees.signum() > 0 && r.price.signum() > 0) add(
            stringResource(R.string.insight_fees, state.channel?.name ?: "", fmt.money(r.channelFees))
        )
        if (state.printer == null) add(stringResource(R.string.insight_no_printer))
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, Modifier.size(16.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Precio siempre visible abajo mientras se teclea. */
@Composable
private fun ResultBar(r: CostResult?, hasInput: Boolean, onClick: () -> Unit) {
    val fmt = LocalFmt.current
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth().clickable(enabled = hasInput, onClick = onClick)
    ) {
        Row(
            Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.calc_price), style = MaterialTheme.typography.labelMedium)
                // El precio sube o baja con un deslizamiento corto: se ve qué ha movido el último cambio
                val price = r?.takeIf { hasInput && it.feasible }?.price
                androidx.compose.animation.AnimatedContent(
                    targetState = price,
                    transitionSpec = {
                        val up = (targetState ?: BigDecimal.ZERO) > (initialState ?: BigDecimal.ZERO)
                        (androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(180)) { if (up) it / 2 else -it / 2 } +
                            androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180))) togetherWith
                            androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                    },
                    label = "price"
                ) { p ->
                    Text(
                        p?.let(fmt::money) ?: "—",
                        style = MaterialTheme.typography.headlineSmall.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
            if (r != null && hasInput) Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.calc_cost_short, fmt.money(r.totalCost)),
                    style = MaterialTheme.typography.bodyMedium.tabular())
                Text(stringResource(R.string.calc_profit_short, fmt.money(r.profit)),
                    style = MaterialTheme.typography.bodyMedium.tabular(), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SaveDialog(initial: String, existing: Boolean, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calc_save_title)) },
        text = {
            TextInput(stringResource(R.string.calc_piece_name), name, { name = it })
        },
        confirmButton = {
            Button(onClick = { onSave(name, false) }, enabled = name.isNotBlank()) {
                Text(stringResource(if (existing) R.string.calc_save_changes else R.string.action_save))
            }
        },
        dismissButton = {
            if (existing) TextButton(onClick = { onSave(name, true) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.calc_save_as_new))
            } else TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
