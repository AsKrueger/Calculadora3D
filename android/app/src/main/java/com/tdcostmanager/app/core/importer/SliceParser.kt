package com.tdcostmanager.app.core.importer

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.util.Base64
import java.util.zip.ZipInputStream

/** Un filamento según el laminador: la cantidad puede venir en gramos o en metros. */
data class SlicedFilament(
    val type: String? = null,
    val colorHex: String? = null,
    val grams: BigDecimal? = null,
    val meters: BigDecimal? = null
)

data class SliceInfo(
    val printSeconds: Long?,
    val filaments: List<SlicedFilament>,
    val thumbnail: ByteArray? = null,
    val plates: Int = 1
) {
    val isEmpty: Boolean get() = printSeconds == null && filaments.isEmpty()
}

class NotSlicedException : Exception("The file has no slicing data")

/**
 * Lee los datos de laminado de un G-code o de un 3MF laminado.
 * Compatible con Flash Studio / Orca-Flashforge, OrcaSlicer, Bambu Studio, PrusaSlicer, SuperSlicer y Cura.
 */
object SliceParser {

    private const val HEAD = 3 * 1024 * 1024
    private const val TAIL = 768 * 1024

    fun parse(input: InputStream): SliceInfo {
        val stream = input.buffered()
        stream.mark(4)
        val magic = ByteArray(4)
        val n = stream.read(magic)
        stream.reset()
        val info = if (n == 4 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()) {
            parse3mf(stream)
        } else {
            parseGcode(readHeadAndTail(stream))
        }
        if (info.isEmpty) throw NotSlicedException()
        return info
    }

    /** El resumen del laminado está al principio (Cura, Bambu, Orca) o al final (Prusa, Orca). */
    internal fun readHeadAndTail(input: InputStream): String {
        val head = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        val ring = ByteArray(TAIL)
        var ringPos = 0
        var ringFilled = false
        var read: Int
        while (input.read(buf).also { read = it } > 0) {
            var offset = 0
            if (head.size() < HEAD) {
                val take = minOf(read, HEAD - head.size())
                head.write(buf, 0, take)
                offset = take
            }
            for (i in offset until read) {
                ring[ringPos] = buf[i]
                ringPos = (ringPos + 1) % TAIL
                if (ringPos == 0) ringFilled = true
            }
        }
        val tail = if (ringFilled) ring.copyOfRange(ringPos, TAIL) + ring.copyOfRange(0, ringPos)
        else ring.copyOfRange(0, ringPos)
        return head.toString(Charsets.UTF_8.name()) + "\n" + String(tail, Charsets.UTF_8)
    }

    // ---------- G-code ----------

    private val timePatterns = listOf(
        Regex("""^;\s*estimated printing time \(normal mode\)\s*=\s*(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""total estimated time\s*[:=]\s*([^;\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""model printing time\s*[:=]\s*([^;\n]+)""", RegexOption.IGNORE_CASE),
        Regex("""^;\s*TIME\s*:\s*(\d+)\s*$""", RegexOption.IGNORE_CASE),
        Regex("""^;\s*(?:estimated[ _]?time|print[ _]?time|build[ _]?time|printing[ _]?time)(?:\s*\(s\))?\s*[:=]\s*(.+)$""", RegexOption.IGNORE_CASE)
    )

    fun parseGcode(text: String): SliceInfo {
        val comments = text.lineSequence().map { it.trim() }.filter { it.startsWith(";") }.toList()

        var seconds: Long? = null
        loop@ for (pattern in timePatterns) {
            for (line in comments) {
                val m = pattern.find(line) ?: continue
                seconds = parseDuration(m.groupValues[1])
                if (seconds != null && seconds > 0) break@loop
            }
        }

        fun values(regex: Regex): List<String>? = comments.firstNotNullOfOrNull { regex.find(it)?.groupValues?.get(1) }
            ?.split(',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() }

        val perExtruderG = values(Regex("""^;\s*filament used \[g\]\s*[=:]\s*(.+)$""", RegexOption.IGNORE_CASE))
            ?.mapNotNull { it.toBigDecimalOrNull() }
        val totalG = values(Regex("""^;\s*total filament (?:used|weight) \[g\]\s*[=:]\s*(.+)$""", RegexOption.IGNORE_CASE))
            ?.firstOrNull()?.toBigDecimalOrNull()
        val perExtruderMm = values(Regex("""^;\s*filament used \[mm\]\s*[=:]\s*(.+)$""", RegexOption.IGNORE_CASE))
            ?.mapNotNull { it.toBigDecimalOrNull() }
        val curaMeters = values(Regex("""^;\s*Filament used\s*:\s*(.+)$""", RegexOption.IGNORE_CASE))
            ?.mapNotNull { it.removeSuffix("m").trim().toBigDecimalOrNull() }

        val types = values(Regex("""^;\s*filament_type\s*=\s*(.+)$""", RegexOption.IGNORE_CASE))
        val colors = values(Regex("""^;\s*(?:filament_colou?r|extruder_colou?r)\s*=\s*(.+)$""", RegexOption.IGNORE_CASE))
            ?.map { it.trim('"') }

        val filaments: List<SlicedFilament> = when {
            !perExtruderG.isNullOrEmpty() -> perExtruderG.mapIndexed { i, g ->
                SlicedFilament(types?.getOrNull(i), colors?.getOrNull(i), grams = g)
            }
            totalG != null -> listOf(SlicedFilament(types?.firstOrNull(), colors?.firstOrNull(), grams = totalG))
            !perExtruderMm.isNullOrEmpty() -> perExtruderMm.mapIndexed { i, mm ->
                SlicedFilament(types?.getOrNull(i), colors?.getOrNull(i), meters = mm.movePointLeft(3))
            }
            !curaMeters.isNullOrEmpty() -> curaMeters.mapIndexed { i, m ->
                SlicedFilament(types?.getOrNull(i), colors?.getOrNull(i), meters = m)
            }
            else -> emptyList()
        }.filter { (it.grams ?: it.meters ?: BigDecimal.ZERO).signum() > 0 }

        return SliceInfo(seconds, filaments, extractThumbnail(text))
    }

    /** «1d 2h 3m 4s», «2h 5m», «45m 10s», «01:02:03», «3725» (segundos). */
    fun parseDuration(raw: String): Long? {
        val s = raw.trim().lowercase()
        s.toLongOrNull()?.let { return it }
        s.toDoubleOrNull()?.let { return it.toLong() }
        Regex("""^(\d+):(\d{1,2}):(\d{1,2})$""").find(s)?.let { m ->
            val (h, mi, se) = m.destructured
            return h.toLong() * 3600 + mi.toLong() * 60 + se.toLong()
        }
        var total = 0L
        var found = false
        Regex("""(\d+(?:\.\d+)?)\s*(d|h|m|s)""").findAll(s).forEach { m ->
            val v = m.groupValues[1].toDouble()
            total += when (m.groupValues[2]) {
                "d" -> (v * 86400).toLong()
                "h" -> (v * 3600).toLong()
                "m" -> (v * 60).toLong()
                else -> v.toLong()
            }
            found = true
        }
        return if (found) total else null
    }

    /** Miniatura PNG o JPG incrustada en base64; se queda con la más grande. */
    internal fun extractThumbnail(text: String): ByteArray? {
        val begin = Regex("""^;\s*thumbnail(?:_(PNG|JPG))?\s+begin\s+(\d+)x(\d+)""", RegexOption.IGNORE_CASE)
        var best: ByteArray? = null
        var bestArea = 0
        var current: StringBuilder? = null
        var area = 0
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (current == null) {
                val m = begin.find(line) ?: continue
                area = m.groupValues[2].toInt() * m.groupValues[3].toInt()
                current = StringBuilder()
            } else if (Regex("""^;\s*thumbnail(?:_(?:PNG|JPG))?\s+end""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
                if (area > bestArea) {
                    runCatching { Base64.getMimeDecoder().decode(current.toString()) }.getOrNull()?.let {
                        best = it
                        bestArea = area
                    }
                }
                current = null
            } else {
                current.append(line.removePrefix(";").trim())
            }
        }
        return best
    }

    // ---------- 3MF ----------

    private fun parse3mf(input: InputStream): SliceInfo {
        var sliceConfig: String? = null
        val gcodes = mutableListOf<String>()
        val images = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                when {
                    name.equals("Metadata/slice_info.config", ignoreCase = true) ->
                        sliceConfig = zip.readBytes().toString(Charsets.UTF_8)
                    name.startsWith("Metadata/", true) && name.endsWith(".gcode", true) ->
                        gcodes += readHeadAndTail(zip)
                    name.startsWith("Metadata/", true) && name.endsWith(".png", true) &&
                        !name.contains("_small") && !name.contains("pick_") && !name.contains("top_") ->
                        images[name.substringAfterLast('/')] = zip.readBytes()
                }
            }
        }
        val thumbnail = images["plate_1.png"] ?: images["thumbnail.png"] ?: images.values.firstOrNull()

        sliceConfig?.let { parseSliceInfo(it) }?.takeIf { !it.isEmpty }?.let {
            return it.copy(thumbnail = thumbnail)
        }
        if (gcodes.isNotEmpty()) {
            val parsed = gcodes.map { parseGcode(it) }
            return combine(parsed).copy(thumbnail = thumbnail ?: parsed.firstNotNullOfOrNull { it.thumbnail })
        }
        throw NotSlicedException()
    }

    /** slice_info.config de Bambu Studio / Orca: una entrada <plate> por placa laminada. */
    fun parseSliceInfo(xml: String): SliceInfo {
        val plates = Regex("""<plate>(.*?)</plate>""", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { it.groupValues[1] }.toList()
            .ifEmpty { listOf(xml) }
        val parsed = plates.map { plate ->
            val seconds = Regex("""key="prediction"\s+value="(\d+)"""").find(plate)?.groupValues?.get(1)?.toLongOrNull()
            val filaments = Regex("""<filament\s+([^>]*?)/?>""").findAll(plate).map { m ->
                val attrs = Regex("""(\w+)="([^"]*)"""").findAll(m.groupValues[1]).associate { it.groupValues[1] to it.groupValues[2] }
                SlicedFilament(
                    type = attrs["type"],
                    colorHex = attrs["color"],
                    grams = attrs["used_g"]?.toBigDecimalOrNull(),
                    meters = attrs["used_m"]?.toBigDecimalOrNull()
                )
            }.filter { (it.grams ?: it.meters ?: BigDecimal.ZERO).signum() > 0 }.toList()
            val weight = Regex("""key="weight"\s+value="([\d.]+)"""").find(plate)?.groupValues?.get(1)?.toBigDecimalOrNull()
            SliceInfo(
                seconds,
                filaments.ifEmpty { weight?.let { listOf(SlicedFilament(grams = it)) } ?: emptyList() }
            )
        }.filter { !it.isEmpty }
        return combine(parsed)
    }

    /** Varias placas: suma tiempos y agrupa filamentos iguales (tipo + color). */
    private fun combine(parts: List<SliceInfo>): SliceInfo {
        if (parts.isEmpty()) return SliceInfo(null, emptyList())
        if (parts.size == 1) return parts.first()
        val seconds = parts.mapNotNull { it.printSeconds }.takeIf { it.isNotEmpty() }?.sum()
        val grouped = parts.flatMap { it.filaments }
            .groupBy { (it.type ?: "") + "|" + (it.colorHex?.uppercase() ?: "") }
            .map { (_, list) ->
                SlicedFilament(
                    type = list.first().type,
                    colorHex = list.first().colorHex,
                    grams = list.mapNotNull { it.grams }.takeIf { it.size == list.size }?.reduce(BigDecimal::add),
                    meters = list.mapNotNull { it.meters }.takeIf { it.isNotEmpty() }?.reduce(BigDecimal::add)
                )
            }
        return SliceInfo(seconds, grouped, parts.firstNotNullOfOrNull { it.thumbnail }, plates = parts.size)
    }
}
