package com.musicplayer.app.data.mediastore

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class TrackTags(
    val title: String? = null,
    val artists: List<String> = emptyList(),
    val albumArtists: List<String> = emptyList(),
    val album: String? = null,
    val trackNumber: Int? = null,
    val trackTotal: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null
) {
    val primaryArtist: String?
        get() = artists.primary()

    companion object {
        val EMPTY = TrackTags()
    }
}

private val FEAT_REGEX = Regex("\\s+(ft\\.?|feat\\.?|featuring)\\b", RegexOption.IGNORE_CASE)

/** Toma el artista principal: primero de la lista, cortado en separadores o "feat". */
fun List<String>.primary(): String? = firstOrNull { it.isNotBlank() }?.let { value ->
    var result = FEAT_REGEX.split(value).first().trim()
    for (sep in charArrayOf(';', ',', '/')) {
        val idx = result.indexOf(sep)
        if (idx > 0) result = result.substring(0, idx)
    }
    result.trim().takeIf { it.isNotBlank() }
}

class TagReader {

    fun read(path: String): TrackTags {
        val file = File(path)
        if (!file.exists() || !file.canRead() || file.length() < 8) return TrackTags.EMPTY
        return try {
            BufferedInputStream(FileInputStream(file)).use { input ->
                val reader = StreamReader(input)
                when {
                    reader.startsWith(MAGIC_FLAC) -> readFlac(reader)
                    reader.startsWith(MAGIC_OGG) -> readOgg(reader)
                    reader.startsWith(MAGIC_ID3) -> readId3(reader)
                    reader.startsWith(MAGIC_MP4) -> readMp4(reader)
                    else -> TrackTags.EMPTY
                }
            }
        } catch (_: Exception) {
            TrackTags.EMPTY
        }
    }

    // ---------- FLAC ----------

    private fun readFlac(reader: StreamReader): TrackTags {
        reader.skip(4)
        while (true) {
            val header = reader.readExactOrNull(4) ?: return TrackTags.EMPTY
            val last = (header[0].toInt() and 0x80) != 0
            val type = header[0].toInt() and 0x7f
            val size = u24be(header, 1)
            if (type == 4) {
                val tags = parseVorbisComments(
                    reader.readExactOrNull(size) ?: return TrackTags.EMPTY, 0
                )
                return tags ?: TrackTags.EMPTY
            }
            reader.skip(size.toLong())
            if (last) return TrackTags.EMPTY
        }
    }

    // ---------- OGG (Vorbis / Opus) ----------

    private fun readOgg(reader: StreamReader): TrackTags {
        reader.skip(4)
        val pending = java.io.ByteArrayOutputStream()
        var iter = 0
        while (iter++ < 200) {
            val pageHeader = reader.readExactOrNull(27) ?: return TrackTags.EMPTY
            val segments = pageHeader[26].toInt() and 0xff
            var dataLen = 0
            val table = IntArray(segments)
            for (i in 0 until segments) {
                val v = reader.u8()
                if (v < 0) return TrackTags.EMPTY
                table[i] = v
                dataLen += v
            }
            val body = reader.readExactOrNull(dataLen) ?: return TrackTags.EMPTY
            var bodyPos = 0
            for (segmentSize in table) {
                pending.write(body, bodyPos, segmentSize)
                bodyPos += segmentSize
                if (segmentSize < 255) {
                    val packet = pending.toByteArray()
                    pending.reset()
                    parseCommentPacket(packet)?.let { return it }
                }
            }
        }
        return TrackTags.EMPTY
    }

    private fun parseCommentPacket(packet: ByteArray): TrackTags? {
        if (packet.size >= 7 && packet[0] == 0x03.toByte() &&
            packet[1] == 'v'.code.toByte() && packet[2] == 'o'.code.toByte() &&
            packet[3] == 'r'.code.toByte() && packet[4] == 'b'.code.toByte() &&
            packet[5] == 'i'.code.toByte() && packet[6] == 's'.code.toByte()
        ) return parseVorbisComments(packet, 7)
        if (packet.size >= 8 && packet[0] == 'O'.code.toByte() && packet[1] == 'p'.code.toByte() &&
            packet[2] == 'u'.code.toByte() && packet[3] == 's'.code.toByte() &&
            packet[4] == 'T'.code.toByte() && packet[5] == 'a'.code.toByte() &&
            packet[6] == 'g'.code.toByte() && packet[7] == 's'.code.toByte()
        ) return parseVorbisComments(packet, 8)
        return null
    }

    private fun parseVorbisComments(bytes: ByteArray, offset: Int): TrackTags? {
        val buf = ArrayReader(bytes, offset)
        if (buf.remaining() < 4) return null
        val vendorLen = buf.u32le()
        if (vendorLen < 0 || vendorLen > buf.remaining()) return null
        buf.skip(vendorLen.toLong())
        val count = buf.u32le()
        if (count < 0 || count > 4096 || buf.remaining() < count * 16) return null
        val map = mutableMapOf<String, MutableList<String>>()
        for (i in 0 until count) {
            val len = buf.u32le()
            if (len <= 0 || len > buf.remaining()) break
            val entry = String(buf.read(len), StandardCharsets.UTF_8).trim()
            val eq = entry.indexOf('=')
            if (eq > 0) {
                val key = entry.substring(0, eq).uppercase()
                val value = entry.substring(eq + 1)
                if (value.isNotEmpty()) map.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        return TrackTags(
            title = map["TITLE"]?.firstOrNull(),
            artists = map["ARTIST"].orEmpty(),
            albumArtists = if (map.containsKey("ALBUMARTIST")) map["ALBUMARTIST"].orEmpty()
            else listOfNotNull(map["ALBUM ARTIST"]?.firstOrNull()),
            album = map["ALBUM"]?.firstOrNull(),
            trackNumber = trackPart(map["TRACKNUMBER"]?.firstOrNull()),
            trackTotal = totalPart(map["TRACKNUMBER"]?.firstOrNull()),
            discNumber = trackPart(map["DISCNUMBER"]?.firstOrNull()),
            year = map["DATE"]?.firstOrNull()?.take(4)?.toIntOrNull()
                ?: map["YEAR"]?.firstOrNull()?.take(4)?.toIntOrNull(),
            genre = map["GENRE"]?.firstOrNull()
        )
    }

    // ---------- MP3 ID3v2 ----------

    private fun readId3(reader: StreamReader): TrackTags {
        reader.skip(3)
        val major = reader.u8()
        if (major < 2 || major > 4) return TrackTags.EMPTY
        reader.u8()
        val flags = reader.u8()
        val size = reader.syncsafe()
        if (size < 0 || size > 16 * 1024 * 1024) return TrackTags.EMPTY
        var data = reader.readExactOrNull(size) ?: return TrackTags.EMPTY
        if (flags and 0x80 != 0) data = deunsync(data)
        if (flags and 0x10 != 0) reader.skip(10)
        return parseId3Frames(data, major)
    }

    private fun parseId3Frames(data: ByteArray, major: Int): TrackTags {
        val buf = ArrayReader(data, 0)
        var title: String? = null
        var album: String? = null
        var genre: String? = null
        var year: Int? = null
        var track: Int? = null
        var trackTotal: Int? = null
        val artists = mutableListOf<String>()
        val albumArtists = mutableListOf<String>()
        var counter = 0

        while (buf.remaining() >= 10 && counter++ < 2048) {
            val id = String(buf.read(4), StandardCharsets.ISO_8859_1)
            if (id.firstOrNull() == '\u0000') break
            val frameSize = if (major >= 4) buf.syncsafe() else buf.u32be()
            val frameFlags = if (major >= 3 && buf.remaining() >= 2) {
                val f1 = buf.u8()
                val f2 = buf.u8()
                f1 to f2
            } else {
                null to null
            }
            if (frameSize <= 0 || frameSize > buf.remaining()) break

            val extra = when {
                major >= 4 -> {
                    val format = frameFlags?.second ?: 0
                    var n = 0
                    if (format and 0x40 != 0) n += 1      // group id
                    if (format and 0x20 != 0) n += 4      // decompressed size
                    if (format and 0x10 != 0) n += 1      // encryption
                    if (format and 0x04 != 0) n += 4      // data length
                    n
                }
                major == 3 -> {
                    val format = frameFlags?.second ?: 0
                    var n = 0
                    if (format and 0x80 != 0) n += 4      // compressed size
                    if (format and 0x40 != 0) n += 1      // encryption
                    if (format and 0x20 != 0) n += 1      // group id
                    n
                }
                else -> 0
            }
            if (extra > frameSize || frameSize - extra > buf.remaining()) break
            buf.skip(extra.toLong())
            val content = buf.read(frameSize - extra)

            when (id) {
                "TIT2", "TT2" -> title = id3Text(content, major).firstOrNull()
                "TALB", "TAL" -> album = id3Text(content, major).firstOrNull()
                "TCON", "TCO" -> genre = id3Text(content, major).firstOrNull()
                "TPE1", "TP1" -> artists += id3Text(content, major)
                "TPE2", "TP2" -> albumArtists += id3Text(content, major)
                "TRCK", "TRK" -> {
                    val value = id3Text(content, major).firstOrNull().orEmpty()
                    track = trackPart(value)
                    trackTotal = totalPart(value)
                }
                "TDRC", "TYER", "TYE" -> if (year == null) {
                    year = id3Text(content, major).firstOrNull()?.take(4)?.toIntOrNull()
                }
            }
        }

        return TrackTags(
            title = title,
            artists = artists,
            albumArtists = albumArtists,
            album = album,
            trackNumber = track,
            trackTotal = trackTotal,
            year = year,
            genre = genre
        )
    }

    private fun id3Text(data: ByteArray, major: Int): List<String> {
        if (data.isEmpty()) return emptyList()
        val encoding = data[0].toInt() and 0xff
        val charset = when (encoding) {
            0 -> StandardCharsets.ISO_8859_1
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> return emptyList()
        }
        val body = data.copyOfRange(1, data.size)
        if (encoding == 0 || encoding == 3) {
            return body.toString(charset).split('\u0000').map { it.trim() }.filter { it.isNotEmpty() }
        }
        return body.toString(charset)
            .split('\u0000')
            .map { it.replace("\uFEFF", "").trim() }
            .filter { it.isNotEmpty() }
    }

    // ---------- MP4 / M4A ----------

    private fun readMp4(reader: StreamReader): TrackTags {
        reader.skip(8)
        val moov = findTopLevel("moov", reader) ?: return TrackTags.EMPTY
        val udta = findChild(moov, "udta") ?: return TrackTags.EMPTY
        val meta = findChild(udta, "meta", skipVersionFlags = true) ?: return TrackTags.EMPTY
        val ilst = findChild(meta, "ilst") ?: return TrackTags.EMPTY
        return parseIlst(ilst)
    }

    private fun findTopLevel(type: String, reader: StreamReader): ByteArray? {
        var iter = 0
        while (iter++ < 65536) {
            val size = reader.u32beOrNull() ?: return null
            val atomType = String(reader.readExactOrNull(4) ?: return null, StandardCharsets.ISO_8859_1)
            if (size < 8) return null
            val bodySize = size - 8
            if (atomType == type) {
                return reader.readExactOrNull(bodySize) ?: return null
            }
            if (!reader.skip(bodySize.toLong())) return null
        }
        return null
    }

    private fun findChild(box: ByteArray, type: String, skipVersionFlags: Boolean = false): ByteArray? {
        val buf = ArrayReader(box, 0)
        if (skipVersionFlags && buf.remaining() >= 4) buf.skip(4)
        while (buf.remaining() >= 8) {
            val size = buf.u32be()
            val atomType = String(buf.read(4), StandardCharsets.ISO_8859_1)
            if (size < 8) return null
            val bodySize = size - 8
            if (bodySize > buf.remaining()) return null
            val body = buf.read(bodySize)
            if (atomType == type) return body
        }
        return null
    }

    private fun parseIlst(ilst: ByteArray): TrackTags {
        val buf = ArrayReader(ilst, 0)
        var title: String? = null
        var album: String? = null
        var genre: String? = null
        var year: Int? = null
        var track: Int? = null
        var trackTotal: Int? = null
        val artists = mutableListOf<String>()
        val albumArtists = mutableListOf<String>()

        while (buf.remaining() >= 8) {
            val size = buf.u32be()
            val itemType = String(buf.read(4), StandardCharsets.ISO_8859_1)
            if (size < 8 || size - 8 > buf.remaining()) return TrackTags.EMPTY
            val data = parseDataAtom(buf.read(size - 8)) ?: continue
            when (itemType) {
                "\u00A9ART" -> artists += data
                "aART" -> albumArtists += data
                "\u00A9nam" -> title = data
                "\u00A9alb" -> album = data
                "\u00A9gen" -> genre = data
                "\u00A9day" -> if (year == null) year = data.take(4).toIntOrNull()
                "trkn" -> {
                    val raw = data.mapToBytes()
                    if (raw.size >= 6 && track == null) {
                        track = ((raw[2].toInt() and 0xff) shl 8) or (raw[3].toInt() and 0xff)
                        trackTotal = ((raw[4].toInt() and 0xff) shl 8) or (raw[5].toInt() and 0xff)
                    }
                }
            }
        }

        return TrackTags(
            title = title,
            artists = artists,
            albumArtists = albumArtists,
            album = album,
            trackNumber = track,
            trackTotal = trackTotal,
            year = year,
            genre = genre
        )
    }

    private fun parseDataAtom(item: ByteArray): String? {
        val buf = ArrayReader(item, 0)
        while (buf.remaining() >= 8) {
            val size = buf.u32be()
            val type = String(buf.read(4), StandardCharsets.ISO_8859_1)
            if (size < 8 || size - 8 > buf.remaining()) return null
            val payload = buf.read(size - 8)
            if (type != "data") continue
            if (payload.size <= 8) return null
            val flag = payload[3].toInt() and 0xff
            if (flag == 13) return null
            val value = payload.copyOfRange(8, payload.size)
            return String(value, StandardCharsets.UTF_8)
                .replace("\u0000", "")
                .trim()
                .takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun trackPart(value: String?): Int? =
        value?.substringBefore('/')?.trim()?.toIntOrNull()?.takeIf { it > 0 }

    private fun totalPart(value: String?): Int? =
        value?.substringAfter('/', "")?.trim()?.toIntOrNull()?.takeIf { it > 0 }
}

private fun String.mapToBytes(): ByteArray = toByteArray(Charsets.ISO_8859_1)

private fun u24be(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xff) shl 16) or
        ((bytes[offset + 1].toInt() and 0xff) shl 8) or
        (bytes[offset + 2].toInt() and 0xff)

private fun deunsync(data: ByteArray): ByteArray {
    val out = java.io.ByteArrayOutputStream(data.size)
    var i = 0
    while (i < data.size) {
        if (i + 1 < data.size &&
            (data[i].toInt() and 0xff) == 0xff && (data[i + 1].toInt() and 0xff) == 0x00
        ) {
            out.write(0xff)
            i += 2
        } else {
            out.write(data[i].toInt() and 0xff)
            i += 1
        }
    }
    return out.toByteArray()
}

private val MAGIC_FLAC = byteArrayOf(
    'f'.code.toByte(), 'L'.code.toByte(), 'a'.code.toByte(), 'C'.code.toByte()
)
private val MAGIC_OGG = byteArrayOf(
    'O'.code.toByte(), 'g'.code.toByte(), 'g'.code.toByte(), 'S'.code.toByte()
)
private val MAGIC_ID3 = byteArrayOf(
    'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte()
)
private val MAGIC_MP4 = byteArrayOf(
    0x00, 0x00, 0x00, 0x00, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()
)

private class StreamReader(private val input: InputStream) {

    fun startsWith(magic: ByteArray): Boolean {
        input.mark(8)
        val head = ByteArray(8)
        var read = 0
        while (read < 8) {
            val r = input.read(head, read, 8 - read)
            if (r < 0) break
            read += r
        }
        input.reset()
        for (i in magic.indices) {
            if (i >= read || head[i] != magic[i]) return false
        }
        return true
    }

    fun u8(): Int = input.read()

    fun u32be(): Int {
        val a = u8(); val b = u8(); val c = u8(); val d = u8()
        if (a < 0 || b < 0 || c < 0 || d < 0) return -1
        return (a shl 24) or (b shl 16) or (c shl 8) or d
    }

    fun u32beOrNull(): Int? {
        val v = u32be()
        return if (v < 0) null else v
    }

    fun syncsafe(): Int {
        val a = u8() and 0x7f; val b = u8() and 0x7f
        val c = u8() and 0x7f; val d = u8() and 0x7f
        if (a < 0 || b < 0 || c < 0 || d < 0) return -1
        return (a shl 21) or (b shl 14) or (c shl 7) or d
    }

    fun readExactOrNull(n: Int): ByteArray? {
        if (n < 0) return null
        val out = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(out, read, n - read)
            if (r < 0) {
                if (read == 0) return null
                return out.copyOf(read)
            }
            read += r
        }
        return out
    }

    fun skip(n: Long): Boolean {
        var remaining = n
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                if (input.read() < 0) return false
                remaining -= 1
            } else {
                remaining -= skipped
            }
        }
        return true
    }
}

private class ArrayReader(private val bytes: ByteArray, private var position: Int) {

    fun remaining(): Int = bytes.size - position

    fun u8(): Int {
        if (position >= bytes.size) return -1
        return bytes[position++].toInt() and 0xff
    }

    fun u32be(): Int {
        if (position + 4 > bytes.size) return -1
        val v = ((bytes[position].toInt() and 0xff) shl 24) or
            ((bytes[position + 1].toInt() and 0xff) shl 16) or
            ((bytes[position + 2].toInt() and 0xff) shl 8) or
            (bytes[position + 3].toInt() and 0xff)
        position += 4
        return v
    }

    fun u32le(): Int {
        if (position + 4 > bytes.size) return -1
        val v = (bytes[position].toInt() and 0xff) or
            ((bytes[position + 1].toInt() and 0xff) shl 8) or
            ((bytes[position + 2].toInt() and 0xff) shl 16) or
            ((bytes[position + 3].toInt() and 0xff) shl 24)
        position += 4
        return v
    }

    fun syncsafe(): Int {
        if (position + 4 > bytes.size) return -1
        val v = ((bytes[position].toInt() and 0x7f) shl 21) or
            ((bytes[position + 1].toInt() and 0x7f) shl 14) or
            ((bytes[position + 2].toInt() and 0x7f) shl 7) or
            (bytes[position + 3].toInt() and 0x7f)
        position += 4
        return v
    }

    fun skip(n: Long) {
        position = (position + n).toInt().coerceIn(0, bytes.size)
    }

    fun read(n: Int): ByteArray {
        val out = bytes.copyOfRange(position, position + n)
        position += n
        return out
    }
}