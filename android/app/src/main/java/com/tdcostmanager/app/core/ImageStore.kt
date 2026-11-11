package com.tdcostmanager.app.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Imágenes de las piezas y del logo.
 *
 * Una referencia es «img:<archivo>» (copia propia en el almacenamiento de la app, disponible sin
 * conexión y en la copia de seguridad) o una URI «content://» de la galería (no ocupa espacio,
 * pero desaparece si el usuario borra la foto original).
 */
class ImageStore(private val context: Context) {

    val dir: File get() = File(context.filesDir, "images").apply { mkdirs() }

    fun fileOf(ref: String): File? =
        if (ref.startsWith(PREFIX)) File(dir, ref.removePrefix(PREFIX)) else null

    /** Guarda una imagen elegida por el usuario: copia propia si [offline], si no la URI. */
    suspend fun importFromUri(uri: Uri, offline: Boolean): String? = withContext(Dispatchers.IO) {
        if (offline) {
            val bmp = decode(uri, MAX_PX) ?: return@withContext null
            saveBitmap(bmp)
        } else {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            uri.toString()
        }
    }

    /** Miniatura que viene dentro de un archivo del laminador. */
    suspend fun saveBytes(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext null
        saveBitmap(bmp)
    }

    private fun saveBitmap(bmp: Bitmap): String {
        val name = UUID.randomUUID().toString() + ".webp"
        File(dir, name).outputStream().use {
            @Suppress("DEPRECATION")
            bmp.compress(Bitmap.CompressFormat.WEBP, 85, it)
        }
        return PREFIX + name
    }

    suspend fun load(ref: String, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileOf(ref)
            if (file != null) {
                if (!file.exists()) return@runCatching null
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, opts)
                opts.inSampleSize = sample(opts.outWidth, opts.outHeight, maxPx)
                opts.inJustDecodeBounds = false
                BitmapFactory.decodeFile(file.path, opts)
            } else {
                decode(Uri.parse(ref), maxPx)
            }
        }.getOrNull()
    }

    private fun decode(uri: Uri, maxPx: Int): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        opts.inSampleSize = sample(opts.outWidth, opts.outHeight, maxPx)
        opts.inJustDecodeBounds = false
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()

    private fun sample(w: Int, h: Int, maxPx: Int): Int {
        var s = 1
        while (w / (s * 2) >= maxPx || h / (s * 2) >= maxPx) s *= 2
        return s
    }

    /** Pasa a copia propia una imagen de la galería. Null si ya no se puede leer. */
    suspend fun localize(ref: String): String? =
        if (ref.startsWith(PREFIX)) ref else withContext(Dispatchers.IO) {
            decode(Uri.parse(ref), MAX_PX)?.let(::saveBitmap)
        }

    suspend fun delete(ref: String?) = withContext(Dispatchers.IO) {
        ref?.let { fileOf(it)?.delete() }
    }

    /** Borra las copias que ya no usa ninguna pieza ni el logo. */
    suspend fun purgeUnused(inUse: Set<String>) = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { f -> if (PREFIX + f.name !in inUse) f.delete() }
    }

    fun imagesBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun cacheBytes(): Long = context.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dir.deleteRecursively() }

    companion object {
        const val PREFIX = "img:"
        private const val MAX_PX = 1280
    }
}
