package com.tdcostmanager.app.ui.calculator

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdcostmanager.app.R
import com.tdcostmanager.app.container
import com.tdcostmanager.app.core.Fmt
import com.tdcostmanager.app.core.calc.ChannelFees
import com.tdcostmanager.app.core.calc.CostEngine
import com.tdcostmanager.app.core.calc.CostInput
import com.tdcostmanager.app.core.calc.CostResult
import com.tdcostmanager.app.core.calc.ExtraLine
import com.tdcostmanager.app.core.calc.MaterialLine
import com.tdcostmanager.app.core.calc.Pricing
import com.tdcostmanager.app.core.calc.PricingMode
import com.tdcostmanager.app.core.calc.PrinterSpec
import com.tdcostmanager.app.core.importer.NotSlicedException
import com.tdcostmanager.app.core.importer.SliceInfo
import com.tdcostmanager.app.core.importer.SliceParser
import com.tdcostmanager.app.data.Filament
import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.ExtraUse
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.MaterialKind
import com.tdcostmanager.app.data.db.MaterialUnit
import com.tdcostmanager.app.data.db.MaterialUse
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.data.db.PrinterEntity
import com.tdcostmanager.app.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode

@Serializable
data class MatRow(val key: Long, val materialId: Long? = null, val amount: String = "")

@Serializable
data class ExtraRow(val key: Long, val extraId: Long? = null, val qty: String = "1")

/** Lo que el usuario está escribiendo, tal cual (texto), para no perder nada al teclear. */
@Serializable
data class CalcForm(
    val pieceId: Long? = null,
    val name: String = "",
    val notes: String = "",
    val image: String? = null,
    val printerId: Long? = null,
    val hours: String = "",
    val minutes: String = "",
    val perPlate: String = "1",
    val materials: List<MatRow> = listOf(MatRow(1)),
    val waste: String = "",
    val prep: String = "",
    val post: String = "",
    val laborRate: String = "",
    val extras: List<ExtraRow> = emptyList(),
    val failure: String = "",
    val mode: PricingMode = PricingMode.MARKUP,
    val priceValue: String = "",
    val channelId: Long? = null,
    val vat: String = ""
)

data class Catalog(
    val printers: List<PrinterEntity> = emptyList(),
    val materials: List<MaterialEntity> = emptyList(),
    val extras: List<ExtraEntity> = emptyList(),
    val channels: List<ChannelEntity> = emptyList()
)

data class CalcUi(
    val form: CalcForm = CalcForm(),
    val catalog: Catalog = Catalog(),
    val settings: AppSettings? = null,
    val result: CostResult? = null
) {
    val channel: ChannelEntity? get() = catalog.channels.firstOrNull { it.id == form.channelId }
    val printer: PrinterEntity? get() = catalog.printers.firstOrNull { it.id == form.printerId }
    val hasInput: Boolean
        get() = (Fmt.parse(form.hours) ?: BigDecimal.ZERO).signum() > 0 ||
            (Fmt.parse(form.minutes) ?: BigDecimal.ZERO).signum() > 0 ||
            form.materials.any { (Fmt.parse(it.amount) ?: BigDecimal.ZERO).signum() > 0 }
}

sealed interface CalcEvent {
    data class Message(val text: String) : CalcEvent
    data class Saved(val pieceId: Long, val name: String) : CalcEvent
}

@OptIn(FlowPreview::class)
class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val c = app.container
    private val json = Json { ignoreUnknownKeys = true }
    private var nextKey = 100L

    private val form = MutableStateFlow(CalcForm())
    private val events = Channel<CalcEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val catalog = combine(
        c.db.printers().observeActive(),
        c.db.materials().observeActive(),
        c.db.extras().observeActive(),
        c.db.channels().observeActive()
    ) { p, m, e, ch -> Catalog(p, m, e, ch) }

    val ui: StateFlow<CalcUi> = combine(form, catalog, c.settings.settings) { f, cat, s ->
        CalcUi(f, cat, s, compute(f, cat, s))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CalcUi())

    init {
        viewModelScope.launch {
            val draft = c.settings.draft.first()?.let { runCatching { json.decodeFromString<CalcForm>(it) }.getOrNull() }
            if (draft != null) {
                form.value = draft
                nextKey = (draft.materials.map { it.key } + draft.extras.map { it.key }).maxOrNull()?.plus(1) ?: 100L
            } else {
                newCalculation()
            }
            // Guarda el borrador mientras se escribe
            form.drop(1).debounce(600).collect { c.settings.saveDraft(json.encodeToString(it)) }
        }
        // En cuanto haya catálogo (tras la bienvenida o una restauración), un formulario vacío lo usa
        viewModelScope.launch {
            catalog.collect { cat ->
                val f = form.value
                val stale = f.printerId != null && cat.printers.none { it.id == f.printerId }
                val unset = f.printerId == null && f.pieceId == null && cat.printers.isNotEmpty() && !ui.value.hasInput
                if (stale || unset) newCalculation()
            }
        }
        viewModelScope.launch {
            c.pendingImport.filterNotNull().collect { uri ->
                c.pendingImport.value = null
                importFile(uri)
            }
        }
    }

    fun edit(transform: (CalcForm) -> CalcForm) = form.update(transform)

    /** Formulario limpio con los valores por defecto de Ajustes y del catálogo. */
    fun newCalculation() {
        viewModelScope.launch {
            val s = c.settings.current()
            val cat = catalog.first()
            val fmt = Fmt(java.util.Locale.getDefault(), s.currency)
            val printer = cat.printers.firstOrNull { it.id == s.defaultPrinterId } ?: cat.printers.firstOrNull()
            val compatible = cat.materials.filter { printer == null || it.kind.technology == printer.technology }
            val material = compatible.firstOrNull { it.kind == MaterialKind.PLA } ?: compatible.firstOrNull()
                ?: cat.materials.firstOrNull()
            form.value = CalcForm(
                printerId = printer?.id,
                materials = listOf(MatRow(key(), material?.id)),
                laborRate = fmt.input(s.laborRate),
                failure = fmt.input(s.failurePercent),
                mode = s.pricingMode,
                priceValue = fmt.input(s.pricingValue),
                channelId = cat.channels.firstOrNull { it.id == s.defaultChannelId }?.id ?: cat.channels.firstOrNull()?.id,
                vat = fmt.input(s.vatPercent)
            )
        }
    }

    fun loadPiece(pieceId: Long) {
        viewModelScope.launch {
            val p = c.db.pieces().byId(pieceId) ?: return@launch
            val fmt = Fmt(java.util.Locale.getDefault(), "EUR")
            val totalMin = p.printHours.multiply(BigDecimal(60)).setScale(0, RoundingMode.HALF_UP).toInt()
            form.value = CalcForm(
                pieceId = p.id,
                name = p.name,
                notes = p.notes,
                image = p.image,
                printerId = p.printerId,
                hours = (totalMin / 60).toString(),
                minutes = (totalMin % 60).toString(),
                perPlate = p.piecesPerPlate.toString(),
                materials = p.materials.map { MatRow(key(), it.materialId, fmt.input(it.amount)) }.ifEmpty { listOf(MatRow(key())) },
                waste = fmt.input(p.wasteAmount.takeIf { it.signum() > 0 }),
                prep = fmt.input(p.prepMinutes.takeIf { it.signum() > 0 }),
                post = fmt.input(p.postMinutes.takeIf { it.signum() > 0 }),
                laborRate = fmt.input(p.laborRate),
                extras = p.extras.map { ExtraRow(key(), it.extraId, fmt.input(it.quantity)) },
                failure = fmt.input(p.failurePercent),
                mode = runCatching { PricingMode.valueOf(p.pricingMode) }.getOrDefault(PricingMode.MARKUP),
                priceValue = fmt.input(p.pricingValue),
                channelId = p.channelId,
                vat = fmt.input(p.vatPercent)
            )
        }
    }

    fun addMaterial() = form.update { f ->
        val used = f.materials.mapNotNull { it.materialId }.toSet()
        val next = ui.value.catalog.materials.firstOrNull { it.id !in used }?.id
        f.copy(materials = f.materials + MatRow(key(), next))
    }

    fun removeMaterial(key: Long) = form.update { f ->
        f.copy(materials = f.materials.filterNot { it.key == key }.ifEmpty { listOf(MatRow(key())) })
    }

    fun addExtra(extraId: Long) = form.update { f -> f.copy(extras = f.extras + ExtraRow(key(), extraId)) }

    fun removeExtra(key: Long) = form.update { f -> f.copy(extras = f.extras.filterNot { it.key == key }) }

    fun setImage(ref: String?) {
        val old = form.value.image
        form.update { it.copy(image = ref) }
        // Una imagen propia que no estaba guardada en ninguna pieza se borra al cambiarla
        if (old != null && old != ref) viewModelScope.launch {
            val inUse = c.db.pieces().images().toSet()
            if (old !in inUse) c.images.delete(old)
        }
    }

    fun pickImage(uri: Uri) {
        viewModelScope.launch {
            val offline = c.settings.current().saveImagesOffline
            c.images.importFromUri(uri, offline)?.let(::setImage)
        }
    }

    /** Guarda la pieza; si ya existía, la actualiza. Con [asNew] crea otra. */
    fun save(name: String, asNew: Boolean = false) {
        viewModelScope.launch {
            val state = ui.value
            val entity = toEntity(state, name.trim(), asNew) ?: return@launch
            val id = c.repository.savePiece(entity)
            form.update { it.copy(pieceId = id, name = name.trim()) }
            events.send(CalcEvent.Saved(id, name.trim()))
        }
    }

    // ---------- Importar del laminador ----------

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val info: SliceInfo = try {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { SliceParser.parse(it) } ?: throw NotSlicedException()
                }
            } catch (e: NotSlicedException) {
                events.send(CalcEvent.Message(ctx.getString(R.string.import_not_sliced)))
                return@launch
            } catch (e: Exception) {
                events.send(CalcEvent.Message(ctx.getString(R.string.import_failed)))
                return@launch
            }
            val cat = catalog.first()
            val fmt = Fmt(java.util.Locale.getDefault(), "EUR")
            val rows = info.filaments.map { f ->
                val match = matchMaterial(f.type, f.colorHex, cat.materials)
                val density = match?.density ?: BigDecimal(MaterialKind.fromSlicer(f.type)?.defaultDensity ?: "1.24")
                val grams = f.grams ?: f.meters?.let { Filament.gramsFromMeters(it, density) } ?: BigDecimal.ZERO
                val amount = if (match?.unit == MaterialUnit.ML) Filament.mlFromGrams(grams, density) else grams
                MatRow(key(), match?.id, fmt.input(amount.setScale(1, RoundingMode.HALF_UP)))
            }
            val thumb = info.thumbnail?.let { c.images.saveBytes(it) }
            val secs = info.printSeconds ?: 0
            val name = runCatching {
                ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull()?.substringBefore(".gcode")?.substringBefore(".3mf")?.substringBefore(".gco")

            form.update { f ->
                f.copy(
                    hours = if (info.printSeconds != null) (secs / 3600).toString() else f.hours,
                    minutes = if (info.printSeconds != null) ((secs % 3600 + 59) / 60).toString() else f.minutes,
                    materials = rows.ifEmpty { f.materials },
                    image = thumb ?: f.image,
                    name = if (f.name.isBlank() && f.pieceId == null) name.orEmpty() else f.name
                )
            }
            val grams = info.filaments.mapNotNull { it.grams }.fold(BigDecimal.ZERO, BigDecimal::add)
            events.send(
                CalcEvent.Message(
                    ctx.getString(
                        R.string.import_done,
                        if (info.printSeconds != null) fmt.hours(secs) else "—",
                        fmt.number(grams, 1),
                        info.filaments.size
                    ) + if (info.plates > 1) " · " + ctx.getString(R.string.import_plates, info.plates) else ""
                )
            )
        }
    }

    /** El material del catálogo del mismo tipo y con el color más parecido. */
    private fun matchMaterial(type: String?, colorHex: String?, materials: List<MaterialEntity>): MaterialEntity? {
        if (materials.isEmpty()) return null
        val kind = MaterialKind.fromSlicer(type)
        val sameKind = materials.filter { it.kind == kind }.ifEmpty {
            materials.filter { it.kind.technology == (kind?.technology ?: it.kind.technology) && it.unit == MaterialUnit.G }
        }.ifEmpty { materials }
        val target = parseHex(colorHex) ?: return sameKind.first()
        return sameKind.minBy { colorDistance(it.color, target) }
    }

    private fun parseHex(hex: String?): Long? {
        val h = hex?.trim()?.removePrefix("#") ?: return null
        val rgb = h.take(6).toLongOrNull(16) ?: return null
        return 0xFF000000 or rgb
    }

    private fun colorDistance(a: Long, b: Long): Long {
        fun ch(x: Long, s: Int) = (x shr s) and 0xFF
        val dr = ch(a, 16) - ch(b, 16)
        val dg = ch(a, 8) - ch(b, 8)
        val db = ch(a, 0) - ch(b, 0)
        return dr * dr * 3 + dg * dg * 4 + db * db * 2
    }

    private fun key() = nextKey++

    companion object {
        private fun dec(s: String) = Fmt.parse(s) ?: BigDecimal.ZERO

        fun hoursOf(f: CalcForm): BigDecimal =
            dec(f.hours) + dec(f.minutes).divide(BigDecimal(60), 6, RoundingMode.HALF_UP)

        fun compute(f: CalcForm, cat: Catalog, s: AppSettings?): CostResult? {
            s ?: return null
            val printer = cat.printers.firstOrNull { it.id == f.printerId }
            val channel = cat.channels.firstOrNull { it.id == f.channelId }
            val input = CostInput(
                materials = f.materials.mapNotNull { row ->
                    cat.materials.firstOrNull { it.id == row.materialId }?.let { MaterialLine(dec(row.amount), it.unitPrice) }
                },
                wasteAmount = dec(f.waste),
                printHours = hoursOf(f),
                printer = printer?.let { PrinterSpec(it.averagePowerW, it.purchasePrice, it.lifetimeHours, it.maintenancePerHour) },
                energyPricePerKwh = s.energyPrice,
                piecesPerPlate = (f.perPlate.toIntOrNull() ?: 1).coerceAtLeast(1),
                prepMinutes = dec(f.prep),
                postMinutesPerPiece = dec(f.post),
                laborRatePerHour = dec(f.laborRate),
                extras = f.extras.mapNotNull { row ->
                    cat.extras.firstOrNull { it.id == row.extraId }?.let { ExtraLine(it.unitCost, dec(row.qty)) }
                },
                failurePercent = dec(f.failure),
                pricing = Pricing(f.mode, dec(f.priceValue)),
                channel = channel?.let { ChannelFees(it.feePercent, it.fixedFee) } ?: ChannelFees.NONE,
                vatPercent = dec(f.vat),
                rounding = s.rounding
            )
            return CostEngine.calculate(input)
        }

        fun toEntity(state: CalcUi, name: String, asNew: Boolean): PieceEntity? {
            val f = state.form
            val r = state.result ?: return null
            val now = System.currentTimeMillis()
            return PieceEntity(
                id = if (asNew) 0 else f.pieceId ?: 0,
                name = name,
                notes = f.notes,
                image = f.image,
                printerId = f.printerId,
                printHours = hoursOf(f).setScale(4, RoundingMode.HALF_UP),
                piecesPerPlate = (f.perPlate.toIntOrNull() ?: 1).coerceAtLeast(1),
                materials = f.materials.filter { it.materialId != null && dec(it.amount).signum() > 0 }
                    .map { MaterialUse(it.materialId!!, dec(it.amount)) },
                wasteAmount = dec(f.waste),
                prepMinutes = dec(f.prep),
                postMinutes = dec(f.post),
                laborRate = dec(f.laborRate),
                extras = f.extras.filter { it.extraId != null && dec(it.qty).signum() > 0 }
                    .map { ExtraUse(it.extraId!!, dec(it.qty)) },
                failurePercent = dec(f.failure),
                pricingMode = f.mode.name,
                pricingValue = dec(f.priceValue),
                channelId = f.channelId,
                vatPercent = dec(f.vat),
                unitCost = r.totalCost,
                unitPrice = r.price,
                unitProfit = r.profit,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
