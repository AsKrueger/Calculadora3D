package com.tdcostmanager.app.ui.catalog

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.data.PrinterTemplate
import com.tdcostmanager.app.data.Templates
import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.MaterialKind
import com.tdcostmanager.app.data.db.MaterialUnit
import com.tdcostmanager.app.data.db.PrinterEntity
import com.tdcostmanager.app.data.db.Technology
import com.tdcostmanager.app.ui.common.ConfirmDialog
import com.tdcostmanager.app.ui.common.EmptyState
import com.tdcostmanager.app.ui.common.LocalFmt
import com.tdcostmanager.app.ui.common.NumberField
import com.tdcostmanager.app.ui.common.Picker
import com.tdcostmanager.app.ui.common.Swatch
import com.tdcostmanager.app.ui.common.TextInput
import com.tdcostmanager.app.ui.theme.AppThemeExtras
import com.tdcostmanager.app.ui.theme.Tone
import com.tdcostmanager.app.ui.theme.tabular
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class CatalogViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.state() =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val printers = c.db.printers().observeActive().state()
    val materials = c.db.materials().observeActive().state()
    val extras = c.db.extras().observeActive().state()
    val channels = c.db.channels().observeActive().state()

    fun save(p: PrinterEntity) = viewModelScope.launch { c.db.printers().upsert(p) }
    fun save(m: MaterialEntity) = viewModelScope.launch { c.db.materials().upsert(m) }
    fun save(e: ExtraEntity) = viewModelScope.launch { c.db.extras().upsert(e) }
    fun save(ch: ChannelEntity) = viewModelScope.launch { c.db.channels().upsert(ch) }
    fun archivePrinter(id: Long) = viewModelScope.launch { c.db.printers().archive(id) }
    fun archiveMaterial(id: Long) = viewModelScope.launch { c.db.materials().archive(id) }
    fun archiveExtra(id: Long) = viewModelScope.launch { c.db.extras().archive(id) }
    fun archiveChannel(id: Long) = viewModelScope.launch { c.db.channels().archive(id) }

    fun addSpool(m: MaterialEntity) = save(m.copy(stock = m.stock + m.packageSize))
}

private enum class Tab(val label: Int) { PRINTERS(R.string.catalog_printers), MATERIALS(R.string.catalog_materials), EXTRAS(R.string.catalog_extras), CHANNELS(R.string.catalog_channels) }

/** Lo que se está editando en el diálogo. */
private sealed interface Editing {
    data class Printer(val p: PrinterEntity?) : Editing
    data class Material(val m: MaterialEntity?) : Editing
    data class Extra(val e: ExtraEntity?) : Editing
    data class Channel(val ch: ChannelEntity?) : Editing
    data object Templates : Editing
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(vm: CatalogViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.PRINTERS) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    val fmt = LocalFmt.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.tab_catalog)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.background) {
                    Tab.entries.forEach { t ->
                        Tab(selected = tab == t, onClick = { tab = t }, text = { Text(stringResource(t.label)) })
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = when (tab) {
                    Tab.PRINTERS -> Editing.Templates
                    Tab.MATERIALS -> Editing.Material(null)
                    Tab.EXTRAS -> Editing.Extra(null)
                    Tab.CHANNELS -> Editing.Channel(null)
                }
            }) { Icon(Icons.Outlined.Add, stringResource(R.string.action_add)) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.PRINTERS -> CatalogList(
                    vm.printers.collectAsStateWithLifecycle().value, Icons.Outlined.Print,
                    R.string.catalog_printers_empty
                ) { p ->
                    CatalogItem(
                        title = p.name,
                        subtitle = stringResource(R.string.printer_summary, p.averagePowerW.toInt(), fmt.money(wearPerHour(p))),
                        onClick = { editing = Editing.Printer(p) }
                    )
                }
                Tab.MATERIALS -> CatalogList(
                    vm.materials.collectAsStateWithLifecycle().value, Icons.Outlined.Inventory2,
                    R.string.catalog_materials_empty
                ) { m -> MaterialItem(m, fmt, onClick = { editing = Editing.Material(m) }, onAddSpool = { vm.addSpool(m) }) }
                Tab.EXTRAS -> CatalogList(
                    vm.extras.collectAsStateWithLifecycle().value, Icons.Outlined.Sell,
                    R.string.catalog_extras_empty
                ) { e ->
                    CatalogItem(e.name, fmt.money(e.unitCost), onClick = { editing = Editing.Extra(e) })
                }
                Tab.CHANNELS -> CatalogList(
                    vm.channels.collectAsStateWithLifecycle().value, Icons.Outlined.Storefront,
                    R.string.catalog_channels_empty,
                    header = {
                        Text(
                            stringResource(R.string.channels_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                ) { ch ->
                    CatalogItem(
                        ch.name,
                        if (ch.feePercent.signum() == 0 && ch.fixedFee.signum() == 0) stringResource(R.string.channel_no_fees)
                        else stringResource(R.string.channel_summary, fmt.number(ch.feePercent), fmt.money(ch.fixedFee)),
                        onClick = { editing = Editing.Channel(ch) }
                    )
                }
            }
        }
    }

    when (val e = editing) {
        null -> Unit
        Editing.Templates -> TemplatePicker(
            onPick = { t -> editing = Editing.Printer(t?.toEntity() ?: Templates.GENERIC_FDM.toEntity("")) },
            onDismiss = { editing = null }
        )
        is Editing.Printer -> PrinterEditor(e.p, onSave = { vm.save(it); editing = null },
            onDelete = { vm.archivePrinter(it); editing = null }, onDismiss = { editing = null })
        is Editing.Material -> MaterialEditor(e.m, onSave = { vm.save(it); editing = null },
            onDelete = { vm.archiveMaterial(it); editing = null }, onDismiss = { editing = null })
        is Editing.Extra -> ExtraEditor(e.e, onSave = { vm.save(it); editing = null },
            onDelete = { vm.archiveExtra(it); editing = null }, onDismiss = { editing = null })
        is Editing.Channel -> ChannelEditor(e.ch, onSave = { vm.save(it); editing = null },
            onDelete = { vm.archiveChannel(it); editing = null }, onDismiss = { editing = null })
    }
}

fun wearPerHour(p: PrinterEntity): BigDecimal {
    val amort = if (p.lifetimeHours.signum() > 0) p.purchasePrice.divide(p.lifetimeHours, 4, RoundingMode.HALF_UP) else BigDecimal.ZERO
    return amort + p.maintenancePerHour
}

@Composable
private fun <T> CatalogList(
    items: List<T>?,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyText: Int,
    header: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit
) {
    items ?: return
    if (items.isEmpty()) {
        EmptyState(emptyIcon, stringResource(R.string.catalog_empty_title), stringResource(emptyText))
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        header?.let { h -> item { h() } }
        items(items) { row(it) }
    }
}

@Composable
private fun CatalogItem(title: String, subtitle: String, onClick: () -> Unit, leading: (@Composable () -> Unit)? = null) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.let { it(); Spacer(Modifier.width(12.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MaterialItem(m: MaterialEntity, fmt: Fmt, onClick: () -> Unit, onAddSpool: () -> Unit) {
    val unit = if (m.unit == MaterialUnit.ML) "ml" else "g"
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, if (m.isLowStock) AppThemeExtras.tone(Tone.Warning).content else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Swatch(m.color, 28.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(m.kind.name, m.brand).filter { it.isNotBlank() }.joinToString(" · ") + " · " +
                            stringResource(R.string.material_price_per_kg, fmt.money(m.unitPrice * BigDecimal(1000)), if (m.unit == MaterialUnit.ML) "L" else "kg"),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (m.trackStock) {
                val fraction = if (m.packageSize.signum() > 0)
                    (m.stock.toFloat() / m.packageSize.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().clip(CircleShape),
                    color = if (m.isLowStock) AppThemeExtras.tone(Tone.Warning).content else MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (m.isLowStock) {
                        Icon(Icons.Outlined.WarningAmber, null, Modifier.size(16.dp), tint = AppThemeExtras.tone(Tone.Warning).content)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        stringResource(if (m.isLowStock) R.string.material_stock_low else R.string.material_stock, fmt.number(m.stock, 0) + " " + unit),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onAddSpool) { Text(stringResource(R.string.material_add_spool)) }
                }
            }
        }
    }
}

@Composable
private fun TemplatePicker(onPick: (PrinterTemplate?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.printer_from_template)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.printer_blank)) },
                        leadingContent = { Icon(Icons.Outlined.Add, null) },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { onPick(null) }
                    )
                    HorizontalDivider()
                }
                items(Templates.printers) { t ->
                    ListItem(
                        headlineContent = { Text(t.name) },
                        supportingContent = {
                            Text(stringResource(R.string.template_summary, t.watts, t.price) +
                                if (t.technology == Technology.RESIN) " · " + stringResource(R.string.tech_resin) else "")
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier.clickable { onPick(t) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** Diálogo de edición común: campos con scroll, Guardar, y Eliminar si ya existe. */
@Composable
private fun EditorDialog(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    deleteName: String?,
    onDelete: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
    if (confirmDelete && onDelete != null) ConfirmDialog(
        title = stringResource(R.string.catalog_delete_title, deleteName ?: ""),
        body = stringResource(R.string.catalog_delete_body),
        confirm = stringResource(R.string.action_delete),
        onConfirm = onDelete,
        onDismiss = { confirmDelete = false }
    )
}

private fun d(s: String) = Fmt.parse(s)

@Composable
private fun PrinterEditor(p: PrinterEntity?, onSave: (PrinterEntity) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    val fmt = LocalFmt.current
    var name by remember { mutableStateOf(p?.name.orEmpty()) }
    var tech by remember { mutableStateOf(p?.technology ?: Technology.FDM) }
    var watts by remember { mutableStateOf(fmt.input(p?.averagePowerW)) }
    var price by remember { mutableStateOf(fmt.input(p?.purchasePrice)) }
    var life by remember { mutableStateOf(fmt.input(p?.lifetimeHours)) }
    var maint by remember { mutableStateOf(fmt.input(p?.maintenancePerHour)) }
    val valid = name.isNotBlank() && d(watts) != null && d(price) != null && (d(life) ?: BigDecimal.ZERO).signum() > 0 && d(maint) != null
    EditorDialog(
        title = stringResource(if (p == null || p.id == 0L) R.string.printer_new else R.string.printer_edit),
        canSave = valid,
        onSave = {
            val base = p ?: PrinterEntity(name = "", averagePowerW = BigDecimal.ZERO, purchasePrice = BigDecimal.ZERO,
                lifetimeHours = BigDecimal.ONE, maintenancePerHour = BigDecimal.ZERO)
            onSave(base.copy(name = name.trim(), technology = tech, averagePowerW = d(watts)!!, purchasePrice = d(price)!!,
                lifetimeHours = d(life)!!, maintenancePerHour = d(maint)!!))
        },
        onDismiss = onDismiss,
        deleteName = p?.name,
        onDelete = p?.takeIf { it.id != 0L }?.let { { onDelete(it.id) } }
    ) {
        TextInput(stringResource(R.string.field_name), name, { name = it })
        Picker(
            stringResource(R.string.printer_technology), Technology.entries, tech,
            labelOf = { stringResource(if (it == Technology.FDM) R.string.tech_fdm else R.string.tech_resin) },
            onSelect = { tech = it }, modifier = Modifier.fillMaxWidth()
        )
        NumberField(stringResource(R.string.printer_watts), watts, { watts = it }, Modifier.fillMaxWidth(), suffix = "W",
            supporting = stringResource(R.string.printer_watts_help))
        NumberField(stringResource(R.string.printer_price), price, { price = it }, Modifier.fillMaxWidth(), suffix = fmt.currencySymbol)
        NumberField(stringResource(R.string.printer_life), life, { life = it }, Modifier.fillMaxWidth(), suffix = "h",
            supporting = stringResource(R.string.printer_life_help))
        NumberField(stringResource(R.string.printer_maintenance), maint, { maint = it }, Modifier.fillMaxWidth(),
            suffix = fmt.currencySymbol + "/h", supporting = stringResource(R.string.printer_maintenance_help))
    }
}

private val palette = listOf(
    0xFF1F1F1F, 0xFFF2F2EE, 0xFF8C8C8C, 0xFFB23A2E, 0xFFE2561A, 0xFFF2C230,
    0xFF2E8B57, 0xFF4A6FA5, 0xFF1D3F8A, 0xFF7B4FA0, 0xFFE58FB4, 0xFF8B5A2B,
    0xFFC9A66B, 0xFFD4D4D4, 0xFF5ED3C4, 0xFFC0C0C0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialEditor(m: MaterialEntity?, onSave: (MaterialEntity) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    val fmt = LocalFmt.current
    var name by remember { mutableStateOf(m?.name.orEmpty()) }
    var kind by remember { mutableStateOf(m?.kind ?: MaterialKind.PLA) }
    var brand by remember { mutableStateOf(m?.brand.orEmpty()) }
    var color by remember { mutableStateOf(m?.color ?: palette.first()) }
    var price by remember { mutableStateOf(fmt.input(m?.packagePrice)) }
    var size by remember { mutableStateOf(fmt.input(m?.packageSize ?: BigDecimal(1000))) }
    var density by remember { mutableStateOf(fmt.input(m?.density ?: BigDecimal(MaterialKind.PLA.defaultDensity))) }
    var track by remember { mutableStateOf(m?.trackStock ?: false) }
    var stock by remember { mutableStateOf(fmt.input(m?.stock ?: m?.packageSize ?: BigDecimal(1000))) }
    var low by remember { mutableStateOf(fmt.input(m?.lowStockThreshold ?: BigDecimal(150))) }
    val unit = kind.unit
    val u = if (unit == MaterialUnit.ML) "ml" else "g"
    val valid = name.isNotBlank() && d(price) != null && (d(size) ?: BigDecimal.ZERO).signum() > 0 &&
        (d(density) ?: BigDecimal.ZERO).signum() > 0 && (!track || (d(stock) != null && d(low) != null))
    EditorDialog(
        title = stringResource(if (m == null) R.string.material_new else R.string.material_edit),
        canSave = valid,
        onSave = {
            val base = m ?: MaterialEntity(name = "", packagePrice = BigDecimal.ZERO, packageSize = BigDecimal.ONE, density = BigDecimal.ONE)
            onSave(base.copy(name = name.trim(), kind = kind, brand = brand.trim(), color = color, unit = unit,
                packagePrice = d(price)!!, packageSize = d(size)!!, density = d(density)!!, trackStock = track,
                stock = d(stock) ?: BigDecimal.ZERO, lowStockThreshold = d(low) ?: BigDecimal.ZERO))
        },
        onDismiss = onDismiss,
        deleteName = m?.name,
        onDelete = m?.let { { onDelete(it.id) } }
    ) {
        TextInput(stringResource(R.string.field_name), name, { name = it })
        Picker(
            stringResource(R.string.material_kind), MaterialKind.entries, kind,
            labelOf = { if (it == MaterialKind.OTHER) stringResource(R.string.kind_other) else if (it == MaterialKind.RESIN) stringResource(R.string.tech_resin) else it.name },
            onSelect = {
                kind = it
                density = fmt.input(BigDecimal(it.defaultDensity))
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextInput(stringResource(R.string.material_brand), brand, { brand = it })
        Text(stringResource(R.string.material_color), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            palette.forEach { c ->
                Box(
                    Modifier
                        .clip(CircleShape)
                        .then(if (c == color) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .clickable { color = c }
                        .padding(3.dp)
                ) { Swatch(c, 28.dp) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(stringResource(R.string.material_package_price), price, { price = it }, Modifier.weight(1f), suffix = fmt.currencySymbol)
            NumberField(stringResource(R.string.material_package_size), size, { size = it }, Modifier.weight(1f), suffix = u)
        }
        val unitPrice = d(price)?.let { pr -> d(size)?.takeIf { it.signum() > 0 }?.let { pr.divide(it, 6, RoundingMode.HALF_UP) } }
        if (unitPrice != null) Text(
            stringResource(R.string.material_unit_price, fmt.moneyPrecise(unitPrice), u),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        NumberField(stringResource(R.string.material_density), density, { density = it }, Modifier.fillMaxWidth(), suffix = "g/cm³",
            supporting = stringResource(R.string.material_density_help))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.material_track_stock), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.material_track_stock_help), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = track, onCheckedChange = { track = it })
        }
        if (track) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(stringResource(R.string.material_stock_field), stock, { stock = it }, Modifier.weight(1f), suffix = u)
            NumberField(stringResource(R.string.material_low_field), low, { low = it }, Modifier.weight(1f), suffix = u)
        }
    }
}

@Composable
private fun ExtraEditor(e: ExtraEntity?, onSave: (ExtraEntity) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    val fmt = LocalFmt.current
    var name by remember { mutableStateOf(e?.name.orEmpty()) }
    var cost by remember { mutableStateOf(fmt.input(e?.unitCost)) }
    EditorDialog(
        title = stringResource(if (e == null) R.string.extra_new else R.string.extra_edit),
        canSave = name.isNotBlank() && d(cost) != null,
        onSave = { onSave((e ?: ExtraEntity(name = "", unitCost = BigDecimal.ZERO)).copy(name = name.trim(), unitCost = d(cost)!!)) },
        onDismiss = onDismiss,
        deleteName = e?.name,
        onDelete = e?.let { { onDelete(it.id) } }
    ) {
        TextInput(stringResource(R.string.field_name), name, { name = it }, supporting = stringResource(R.string.extra_name_help))
        NumberField(stringResource(R.string.extra_cost), cost, { cost = it }, Modifier.fillMaxWidth(), suffix = fmt.currencySymbol)
    }
}

@Composable
private fun ChannelEditor(ch: ChannelEntity?, onSave: (ChannelEntity) -> Unit, onDelete: (Long) -> Unit, onDismiss: () -> Unit) {
    val fmt = LocalFmt.current
    var name by remember { mutableStateOf(ch?.name.orEmpty()) }
    var pct by remember { mutableStateOf(fmt.input(ch?.feePercent ?: BigDecimal.ZERO)) }
    var fixed by remember { mutableStateOf(fmt.input(ch?.fixedFee ?: BigDecimal.ZERO)) }
    EditorDialog(
        title = stringResource(if (ch == null) R.string.channel_new else R.string.channel_edit),
        canSave = name.isNotBlank() && d(pct) != null && (d(pct) ?: BigDecimal.ZERO) < BigDecimal(100) && d(fixed) != null,
        onSave = {
            onSave((ch ?: ChannelEntity(name = "", feePercent = BigDecimal.ZERO, fixedFee = BigDecimal.ZERO))
                .copy(name = name.trim(), feePercent = d(pct)!!, fixedFee = d(fixed)!!))
        },
        onDismiss = onDismiss,
        deleteName = ch?.name,
        onDelete = ch?.let { { onDelete(it.id) } }
    ) {
        TextInput(stringResource(R.string.field_name), name, { name = it })
        NumberField(stringResource(R.string.channel_fee_percent), pct, { pct = it }, Modifier.fillMaxWidth(), suffix = "%",
            supporting = stringResource(R.string.channel_fee_percent_help))
        NumberField(stringResource(R.string.channel_fixed_fee), fixed, { fixed = it }, Modifier.fillMaxWidth(), suffix = fmt.currencySymbol,
            supporting = stringResource(R.string.channel_fixed_fee_help))
    }
}
