package com.tdcostmanager.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun decToString(value: BigDecimal?): String? = value?.toPlainString()
    @TypeConverter fun stringToDec(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter fun materialUsesToJson(v: List<MaterialUse>): String = json.encodeToString(v)
    @TypeConverter fun jsonToMaterialUses(v: String): List<MaterialUse> = json.decodeFromString(v)

    @TypeConverter fun extraUsesToJson(v: List<ExtraUse>): String = json.encodeToString(v)
    @TypeConverter fun jsonToExtraUses(v: String): List<ExtraUse> = json.decodeFromString(v)

    @TypeConverter fun amountsToJson(v: List<MaterialAmount>): String = json.encodeToString(v)
    @TypeConverter fun jsonToAmounts(v: String): List<MaterialAmount> = json.decodeFromString(v)
}

@Dao
interface PrinterDao {
    @Query("SELECT * FROM printers WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers") suspend fun all(): List<PrinterEntity>
    @Query("SELECT COUNT(*) FROM printers") suspend fun count(): Int
    @Upsert suspend fun upsert(p: PrinterEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<PrinterEntity>)
    @Query("UPDATE printers SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
}

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE archived = 0 ORDER BY kind, name COLLATE NOCASE")
    fun observeActive(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials") suspend fun all(): List<MaterialEntity>
    @Query("SELECT * FROM materials WHERE id = :id") suspend fun byId(id: Long): MaterialEntity?
    @Upsert suspend fun upsert(m: MaterialEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<MaterialEntity>)
    @Update suspend fun update(m: MaterialEntity)
    @Query("UPDATE materials SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
}

@Dao
interface ExtraDao {
    @Query("SELECT * FROM extras WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ExtraEntity>>

    @Query("SELECT * FROM extras") suspend fun all(): List<ExtraEntity>
    @Upsert suspend fun upsert(e: ExtraEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ExtraEntity>)
    @Query("UPDATE extras SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE archived = 0 ORDER BY id")
    fun observeActive(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels") suspend fun all(): List<ChannelEntity>
    @Upsert suspend fun upsert(c: ChannelEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<ChannelEntity>)
    @Query("UPDATE channels SET archived = 1 WHERE id = :id") suspend fun archive(id: Long)
}

@Dao
interface PieceDao {
    @Query("SELECT * FROM pieces ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PieceEntity>>

    @Query("SELECT * FROM pieces") suspend fun all(): List<PieceEntity>
    @Query("SELECT * FROM pieces WHERE id = :id") suspend fun byId(id: Long): PieceEntity?
    @Upsert suspend fun upsert(p: PieceEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<PieceEntity>)
    @Delete suspend fun delete(p: PieceEntity)
    @Query("SELECT image FROM pieces WHERE image IS NOT NULL") suspend fun images(): List<String>
    @Query("UPDATE pieces SET image = :newImage WHERE image = :oldImage")
    suspend fun replaceImage(oldImage: String, newImage: String)
}

/** Pedido con sus líneas, para listas y resumen. */
data class OrderWithLines(
    @androidx.room.Embedded val order: OrderEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "orderId")
    val lines: List<OrderLineEntity>
)

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<OrderWithLines>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observe(id: Long): Flow<OrderWithLines?>

    @Query("SELECT * FROM orders WHERE id = :id") suspend fun byId(id: Long): OrderEntity?
    @Query("SELECT * FROM orders") suspend fun all(): List<OrderEntity>
    @Query("SELECT * FROM order_lines") suspend fun allLines(): List<OrderLineEntity>
    @Query("SELECT * FROM order_lines WHERE orderId = :orderId ORDER BY position, id")
    suspend fun lines(orderId: Long): List<OrderLineEntity>
    @Query("SELECT COALESCE(MAX(number), 0) + 1 FROM orders") suspend fun nextNumber(): Int

    @Insert suspend fun insert(o: OrderEntity): Long
    @Update suspend fun update(o: OrderEntity)
    @Query("DELETE FROM orders WHERE id = :id") suspend fun delete(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<OrderEntity>)

    @Insert suspend fun insertLine(l: OrderLineEntity): Long
    @Update suspend fun updateLine(l: OrderLineEntity)
    @Query("DELETE FROM order_lines WHERE id = :id") suspend fun deleteLine(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAllLines(items: List<OrderLineEntity>)
    @Query("UPDATE order_lines SET pieceId = NULL WHERE pieceId = :pieceId")
    suspend fun detachPiece(pieceId: Long)
}

@Database(
    entities = [
        PrinterEntity::class, MaterialEntity::class, ExtraEntity::class, ChannelEntity::class,
        PieceEntity::class, OrderEntity::class, OrderLineEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun printers(): PrinterDao
    abstract fun materials(): MaterialDao
    abstract fun extras(): ExtraDao
    abstract fun channels(): ChannelDao
    abstract fun pieces(): PieceDao
    abstract fun orders(): OrderDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cost3d.db").build()
    }
}
