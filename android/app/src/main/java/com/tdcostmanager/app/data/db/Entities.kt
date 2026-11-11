package com.tdcostmanager.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

/** BigDecimal como texto, sin perder decimales, en la copia de seguridad JSON. */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())
    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

typealias Dec = @Serializable(with = BigDecimalSerializer::class) BigDecimal

enum class Technology { FDM, RESIN }

enum class MaterialUnit { G, ML }

enum class MaterialKind(val defaultDensity: String, val unit: MaterialUnit, val technology: Technology) {
    PLA("1.24", MaterialUnit.G, Technology.FDM),
    PETG("1.27", MaterialUnit.G, Technology.FDM),
    ABS("1.04", MaterialUnit.G, Technology.FDM),
    ASA("1.07", MaterialUnit.G, Technology.FDM),
    TPU("1.21", MaterialUnit.G, Technology.FDM),
    NYLON("1.14", MaterialUnit.G, Technology.FDM),
    PC("1.20", MaterialUnit.G, Technology.FDM),
    OTHER("1.24", MaterialUnit.G, Technology.FDM),
    RESIN("1.10", MaterialUnit.ML, Technology.RESIN);

    companion object {
        /** Tipo a partir del nombre que escribe el laminador («PLA», «PLA Matte», «PETG-CF»…). */
        fun fromSlicer(type: String?): MaterialKind? {
            val t = type?.uppercase()?.trim() ?: return null
            return when {
                t.isEmpty() -> null
                t.startsWith("PLA") -> PLA
                t.startsWith("PETG") || t.startsWith("PET") -> PETG
                t.startsWith("ABS") -> ABS
                t.startsWith("ASA") -> ASA
                t.startsWith("TPU") || t.startsWith("TPE") || t.contains("FLEX") -> TPU
                t.startsWith("PA") || t.contains("NYLON") -> NYLON
                t.startsWith("PC") -> PC
                t.contains("RESIN") -> RESIN
                else -> OTHER
            }
        }
    }
}

@Serializable
@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val technology: Technology = Technology.FDM,
    /** Consumo medio imprimiendo, no el máximo de la fuente. */
    val averagePowerW: Dec,
    val purchasePrice: Dec,
    val lifetimeHours: Dec,
    /** Boquillas, placas, correas, FEP… repartido por hora. */
    val maintenancePerHour: Dec,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "materials")
data class MaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: MaterialKind = MaterialKind.PLA,
    val brand: String = "",
    /** Color ARGB de la muestra. */
    val color: Long = 0xFF2B3036,
    val unit: MaterialUnit = MaterialUnit.G,
    /** Precio de una bobina o botella. */
    val packagePrice: Dec,
    /** Gramos (o ml) de una bobina o botella. */
    val packageSize: Dec,
    /** g/cm³ (= g/ml). */
    val density: Dec,
    val trackStock: Boolean = false,
    /** Cantidad que queda, en la unidad del material. */
    val stock: Dec = BigDecimal.ZERO,
    val lowStockThreshold: Dec = BigDecimal(150),
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val unitPrice: BigDecimal
        get() = if (packageSize.signum() > 0)
            packagePrice.divide(packageSize, 8, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO

    val isLowStock: Boolean get() = trackStock && stock <= lowStockThreshold
}

@Serializable
@Entity(tableName = "extras")
data class ExtraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitCost: Dec,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val feePercent: Dec,
    val fixedFee: Dec,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Material usado en una pieza: cantidad por placa en la unidad del material. */
@Serializable
data class MaterialUse(val materialId: Long, val amount: Dec)

/** Extra usado en cada pieza. */
@Serializable
data class ExtraUse(val extraId: Long, val quantity: Dec)

@Serializable
@Entity(tableName = "pieces")
data class PieceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    /** Ruta a un archivo propio («file:…») o URI de la galería («content:…»). */
    val image: String? = null,
    val printerId: Long?,
    val printHours: Dec,
    val piecesPerPlate: Int = 1,
    val materials: List<MaterialUse>,
    val wasteAmount: Dec = BigDecimal.ZERO,
    val prepMinutes: Dec = BigDecimal.ZERO,
    val postMinutes: Dec = BigDecimal.ZERO,
    val laborRate: Dec,
    val extras: List<ExtraUse> = emptyList(),
    val failurePercent: Dec,
    val pricingMode: String,
    val pricingValue: Dec,
    val channelId: Long?,
    val vatPercent: Dec,
    /** Último cálculo guardado, para listas y pedidos. */
    val unitCost: Dec,
    val unitPrice: Dec,
    val unitProfit: Dec,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class OrderStatus {
    DRAFT, SENT, ACCEPTED, PRINTED, DELIVERED, CANCELLED;

    /** Pedidos que cuentan como venta en el resumen. */
    val isWon: Boolean get() = this == ACCEPTED || this == PRINTED || this == DELIVERED
    val isOpen: Boolean get() = this != DELIVERED && this != CANCELLED
}

@Serializable
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Número visible del presupuesto (1, 2, 3…). */
    val number: Int,
    val title: String = "",
    val clientName: String = "",
    val clientContact: String = "",
    val status: OrderStatus = OrderStatus.DRAFT,
    val discountPercent: Dec = BigDecimal.ZERO,
    /** Envío cobrado al cliente, con IVA. */
    val shipping: Dec = BigDecimal.ZERO,
    val notes: String = "",
    val dueDate: Long? = null,
    /** Evita descontar dos veces el filamento al volver a marcar como impreso. */
    val stockDeducted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/** Cantidad de un material que lleva una unidad, para descontar stock. */
@Serializable
data class MaterialAmount(val materialId: Long, val amount: Dec)

@Serializable
@Entity(
    tableName = "order_lines",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val pieceId: Long?,
    val description: String,
    val quantity: Int,
    /** Precios congelados al añadir la línea: un presupuesto enviado no cambia solo. */
    val unitPrice: Dec,
    val unitCost: Dec,
    val unitProfit: Dec,
    val vatPercent: Dec,
    val materialsPerUnit: List<MaterialAmount> = emptyList(),
    val position: Int = 0
)
