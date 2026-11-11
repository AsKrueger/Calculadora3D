package com.tdcostmanager.app.core

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.tdcostmanager.app.data.db.AppDatabase
import com.tdcostmanager.app.data.db.ChannelEntity
import com.tdcostmanager.app.data.db.ExtraEntity
import com.tdcostmanager.app.data.db.MaterialEntity
import com.tdcostmanager.app.data.db.OrderEntity
import com.tdcostmanager.app.data.db.OrderLineEntity
import com.tdcostmanager.app.data.db.PieceEntity
import com.tdcostmanager.app.data.db.PrinterEntity
import com.tdcostmanager.app.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupData(
    val format: Int = FORMAT,
    val exportedAt: Long,
    val settings: Map<String, String>,
    val printers: List<PrinterEntity>,
    val materials: List<MaterialEntity>,
    val extras: List<ExtraEntity>,
    val channels: List<ChannelEntity>,
    val pieces: List<PieceEntity>,
    val orders: List<OrderEntity>,
    val orderLines: List<OrderLineEntity>
) {
    companion object {
        const val FORMAT = 1
    }
}

class InvalidBackupException : Exception("Not a valid backup file")

/** Copia de seguridad en un único .zip: backup.json + las imágenes propias. */
class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val images: ImageStore
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(target: Uri) = withContext(Dispatchers.IO) {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            settings = settings.exportAll(),
            printers = db.printers().all(),
            materials = db.materials().all(),
            extras = db.extras().all(),
            channels = db.channels().all(),
            pieces = db.pieces().all(),
            orders = db.orders().all(),
            orderLines = db.orders().allLines()
        )
        val out = context.contentResolver.openOutputStream(target, "wt") ?: error("Cannot write")
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(json.encodeToString(data).toByteArray())
            zip.closeEntry()
            images.dir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry("images/" + f.name))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Sustituye todos los datos por los de la copia. Falla sin tocar nada si el archivo no es válido. */
    suspend fun restore(source: Uri) = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var data: BackupData? = null
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == JSON_NAME -> data = runCatching {
                            json.decodeFromString<BackupData>(zip.readBytes().toString(Charsets.UTF_8))
                        }.getOrNull()
                        e.name.startsWith("images/") && !e.isDirectory -> {
                            val name = File(e.name).name // evita rutas con «..»
                            File(staging, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        }
        val backup = data?.takeIf { it.format <= BackupData.FORMAT } ?: run {
            staging.deleteRecursively()
            throw InvalidBackupException()
        }
        db.withTransaction {
            db.clearAllTables()
            db.printers().insertAll(backup.printers)
            db.materials().insertAll(backup.materials)
            db.extras().insertAll(backup.extras)
            db.channels().insertAll(backup.channels)
            db.pieces().insertAll(backup.pieces)
            db.orders().insertAll(backup.orders)
            db.orders().insertAllLines(backup.orderLines)
        }
        images.clearAll()
        staging.listFiles()?.forEach { it.copyTo(File(images.dir, it.name), overwrite = true) }
        staging.deleteRecursively()
        settings.importAll(backup.settings)
    }

    companion object {
        private const val JSON_NAME = "backup.json"
    }
}
