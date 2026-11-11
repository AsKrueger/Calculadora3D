package com.tdcostmanager.app.data

import android.content.Context
import com.tdcostmanager.app.R
import com.tdcostmanager.app.data.db.AppDatabase
import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.MaterialKind
import com.tdcostmanager.app.data.db.PrinterEntity
import com.tdcostmanager.app.data.db.Technology
import java.math.BigDecimal

/**
 * Plantilla de impresora con valores aproximados: consumo medio imprimiendo PLA (no el máximo
 * de la fuente), precio orientativo de mercado y horas de vida útil prudentes.
 */
data class PrinterTemplate(
    val name: String,
    val technology: Technology,
    val watts: Int,
    val price: Int,
    val lifetimeHours: Int,
    val maintenancePerHour: String
) {
    fun toEntity(displayName: String = name) = PrinterEntity(
        name = displayName,
        technology = technology,
        averagePowerW = BigDecimal(watts),
        purchasePrice = BigDecimal(price),
        lifetimeHours = BigDecimal(lifetimeHours),
        maintenancePerHour = BigDecimal(maintenancePerHour)
    )
}

object Templates {
    val GENERIC_FDM = PrinterTemplate("FDM", Technology.FDM, 120, 300, 5000, "0.05")

    val printers = listOf(
        PrinterTemplate("Flashforge Adventurer 5M", Technology.FDM, 110, 299, 5000, "0.05"),
        PrinterTemplate("Flashforge Adventurer 5M Pro", Technology.FDM, 120, 399, 5000, "0.05"),
        PrinterTemplate("Bambu Lab A1 mini", Technology.FDM, 80, 199, 4000, "0.04"),
        PrinterTemplate("Bambu Lab A1", Technology.FDM, 95, 299, 5000, "0.05"),
        PrinterTemplate("Bambu Lab P1S", Technology.FDM, 110, 599, 6000, "0.06"),
        PrinterTemplate("Bambu Lab X1 Carbon", Technology.FDM, 130, 1199, 8000, "0.08"),
        PrinterTemplate("Prusa MK4S", Technology.FDM, 80, 1099, 10000, "0.05"),
        PrinterTemplate("Prusa Core One", Technology.FDM, 110, 1299, 10000, "0.06"),
        PrinterTemplate("Creality K1C", Technology.FDM, 150, 399, 5000, "0.05"),
        PrinterTemplate("Creality Ender-3 V3 SE", Technology.FDM, 110, 199, 4000, "0.05"),
        PrinterTemplate("Elegoo Centauri Carbon", Technology.FDM, 120, 299, 5000, "0.05"),
        PrinterTemplate("Elegoo Saturn 4 Ultra", Technology.RESIN, 60, 399, 4000, "0.10"),
        PrinterTemplate("Anycubic Photon Mono M7", Technology.RESIN, 55, 299, 4000, "0.10")
    )
}

/** Bobina o botella de una marca conocida, con su precio orientativo de tienda (2026). */
data class MaterialTemplate(
    val brand: String,
    val name: String,
    val kind: MaterialKind,
    val price: String,
    val size: String = "1000",
    val color: Long = 0xFF1F1F1F
) {
    fun toEntity() = MaterialEntity(
        name = "$brand $name",
        kind = kind,
        brand = brand,
        color = color,
        unit = kind.unit,
        packagePrice = BigDecimal(price),
        packageSize = BigDecimal(size),
        density = BigDecimal(kind.defaultDensity)
    )
}

object MaterialLibrary {
    val items = listOf(
        MaterialTemplate("Bambu Lab", "PLA Basic", MaterialKind.PLA, "19.99"),
        MaterialTemplate("Bambu Lab", "PLA Matte", MaterialKind.PLA, "19.99", color = 0xFFF2F2EE),
        MaterialTemplate("Bambu Lab", "PETG HF", MaterialKind.PETG, "19.99", color = 0xFF4A6FA5),
        MaterialTemplate("Flashforge", "PLA", MaterialKind.PLA, "19.90"),
        MaterialTemplate("Flashforge", "PLA Silk", MaterialKind.PLA, "22.90", color = 0xFFC9A66B),
        MaterialTemplate("Flashforge", "PETG", MaterialKind.PETG, "19.90", color = 0xFF4A6FA5),
        MaterialTemplate("Sunlu", "PLA", MaterialKind.PLA, "15.99"),
        MaterialTemplate("Sunlu", "PLA+", MaterialKind.PLA, "16.99", color = 0xFFF2F2EE),
        MaterialTemplate("Sunlu", "PETG", MaterialKind.PETG, "16.99", color = 0xFF2E8B57),
        MaterialTemplate("eSun", "PLA+", MaterialKind.PLA, "19.99"),
        MaterialTemplate("eSun", "PETG", MaterialKind.PETG, "19.99", color = 0xFF4A6FA5),
        MaterialTemplate("eSun", "ABS+", MaterialKind.ABS, "19.99", color = 0xFFB23A2E),
        MaterialTemplate("Polymaker", "PolyTerra PLA", MaterialKind.PLA, "21.99", color = 0xFF2E8B57),
        MaterialTemplate("Polymaker", "PolyLite ASA", MaterialKind.ASA, "27.99", color = 0xFF8C8C8C),
        MaterialTemplate("Elegoo", "PLA", MaterialKind.PLA, "15.99"),
        MaterialTemplate("Elegoo", "PETG Pro", MaterialKind.PETG, "17.99", color = 0xFF4A6FA5),
        MaterialTemplate("Creality", "Hyper PLA", MaterialKind.PLA, "17.99"),
        MaterialTemplate("Prusament", "PLA", MaterialKind.PLA, "29.99", color = 0xFFE2561A),
        MaterialTemplate("Prusament", "PETG", MaterialKind.PETG, "29.99", color = 0xFFE2561A),
        MaterialTemplate("Overture", "TPU 95A", MaterialKind.TPU, "29.99", color = 0xFF2E8B57),
        MaterialTemplate("Elegoo", "Standard Resin", MaterialKind.RESIN, "29.99", color = 0xFF8C8C8C),
        MaterialTemplate("Anycubic", "Standard Resin", MaterialKind.RESIN, "27.99", color = 0xFF8C8C8C)
    )
}

/** Carga el catálogo inicial una sola vez, con los nombres en el idioma del usuario. */
class Seeder(private val context: Context, private val db: AppDatabase) {

    suspend fun seed(printer: PrinterTemplate?) {
        val r = context.resources
        val p = printer ?: Templates.GENERIC_FDM
        db.printers().insertAll(
            listOf(p.toEntity(if (printer == null) r.getString(R.string.seed_generic_printer) else p.name))
        )

        fun mat(nameRes: Int, kind: MaterialKind, price: String, color: Long, size: String = "1000") =
            MaterialEntity(
                name = r.getString(nameRes),
                kind = kind,
                color = color,
                unit = kind.unit,
                packagePrice = BigDecimal(price),
                packageSize = BigDecimal(size),
                density = BigDecimal(kind.defaultDensity)
            )
        db.materials().insertAll(
            listOf(
                mat(R.string.seed_pla_black, MaterialKind.PLA, "20", 0xFF1F1F1F),
                mat(R.string.seed_pla_white, MaterialKind.PLA, "20", 0xFFF2F2EE),
                mat(R.string.seed_petg, MaterialKind.PETG, "22", 0xFF4A6FA5),
                mat(R.string.seed_abs, MaterialKind.ABS, "20", 0xFFB23A2E),
                mat(R.string.seed_tpu, MaterialKind.TPU, "30", 0xFF2E8B57),
                mat(R.string.seed_resin, MaterialKind.RESIN, "35", 0xFF8C8C8C)
            )
        )

        fun extra(nameRes: Int, cost: String) = ExtraEntity(name = r.getString(nameRes), unitCost = BigDecimal(cost))
        db.extras().insertAll(
            listOf(
                extra(R.string.seed_extra_magnet, "0.05"),
                extra(R.string.seed_extra_insert, "0.06"),
                extra(R.string.seed_extra_box, "0.60"),
                extra(R.string.seed_extra_bag, "0.05")
            )
        )

        // Comisiones orientativas (2026). Cambian a menudo y según el país: el usuario las edita.
        fun ch(name: String, pct: String, fixed: String) =
            ChannelEntity(name = name, feePercent = BigDecimal(pct), fixedFee = BigDecimal(fixed))
        db.channels().insertAll(
            listOf(
                ch(r.getString(R.string.seed_channel_direct), "0", "0"),
                ch("Etsy", "9.5", "0.45"),
                ch("Amazon Handmade", "15", "0"),
                ch("eBay", "13", "0.35"),
                ch("Wallapop / Vinted", "0", "0"),
                ch(r.getString(R.string.seed_channel_shop), "2.9", "0.30")
            )
        )
    }
}

/** Conversión entre longitud de filamento y gramos (diámetro 1,75 mm). */
object Filament {
    private val AREA_CM2_175 = BigDecimal("0.0240528") // π · (0,0875 cm)²

    fun gramsFromMeters(meters: BigDecimal, density: BigDecimal): BigDecimal =
        meters.multiply(BigDecimal(100)).multiply(AREA_CM2_175).multiply(density)

    fun mlFromGrams(grams: BigDecimal, density: BigDecimal): BigDecimal =
        if (density.signum() > 0) grams.divide(density, 4, java.math.RoundingMode.HALF_UP) else grams
}
