package com.iptvplayer.app

import android.util.Xml
import java.io.File
import java.io.InputStream
import java.io.FilterInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser

private val xmlTvTimestamp = DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
private const val GUIDE_DOWNLOAD_LIMIT = 48L * 1024 * 1024
private const val GUIDE_PARSE_LIMIT = 256L * 1024 * 1024

internal fun guideSourceUri(input: String): URI? = runCatching { URI(input.trim()) }.getOrNull()
    ?.takeIf { it.scheme in setOf("http", "https") && it.host != null && it.userInfo == null && it.fragment == null }

internal fun xmlTvTime(value: String?): Long? {
    val parts = value?.trim()?.split(Regex("\\s+"), limit = 2) ?: return null
    val time = parts.first()
    if (time.length !in setOf(12, 14) || !time.all(Char::isDigit)) return null
    return runCatching {
        val offset = ZoneOffset.of(parts.getOrElse(1) { "+0000" })
        LocalDateTime.parse(time.padEnd(14, '0'), xmlTvTimestamp).toEpochSecond(offset)
    }.getOrNull()
}

internal fun portalProgrammeTime(value: String?): Long? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    text.toLongOrNull()?.let { number ->
        if (number in 1_000_000_000L..9_999_999_999L) return number
        if (number in 1_000_000_000_000L..9_999_999_999_999L) return number / 1_000
    }
    return xmlTvTime(text) ?: runCatching {
        java.time.OffsetDateTime.parse(text.replace(' ', 'T')).toEpochSecond()
    }.getOrNull() ?: runCatching {
        LocalDateTime.parse(text.replace(' ', 'T')).atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
    }.getOrNull()
}

internal fun decodeProgrammeText(value: String): String {
    if (value.length < 8 || !value.matches(Regex("[A-Za-z0-9+/]+={0,2}"))) return value
    val decoded = runCatching {
        java.util.Base64.getDecoder().decode(value).decodeToString(throwOnInvalidSequence = true).trim()
    }.getOrNull() ?: return value
    return decoded.takeIf { text ->
        text.any(Char::isLetter) && text.none { it.isISOControl() && !it.isWhitespace() }
    } ?: value
}

/** How far back the guide keeps shows: one day (plus [hours] ahead, e.g. 24 + 24 = 48 hours). */
internal const val GUIDE_PAST_SECONDS = 24 * 3_600L

internal fun List<PortalProgramme>.guideWindow(now: Long, hours: Int): List<PortalProgramme> =
    filter { item ->
        val start = item.startSeconds ?: return@filter true
        start < now + hours * 3_600L && (item.endSeconds ?: start + 3_600L) > now - GUIDE_PAST_SECONDS
    }.sortedBy { it.startSeconds ?: Long.MAX_VALUE }

internal data class XmlTvGuide(
    val schedules: Map<String, List<PortalProgramme>>,
    val aliases: Map<String, Set<String>>
) {
    fun programmes(channel: StalkerChannel, displayName: String, override: String? = null): List<PortalProgramme> {
        // A name set by the viewer (channel menu "EPG-Name ändern") wins: XMLTV id or display name.
        override?.let { name ->
            val key = normalizedLabel(name)
            return schedules[key] ?: aliases[key].orEmpty().firstNotNullOfOrNull { schedules[it] }.orEmpty()
        }
        channel.epgIdentity?.let { schedules[normalizedLabel(it)] }?.let { return it }
        val key = normalizedLabel(displayName)
        val id = (aliases[key].orEmpty() + key.takeIf { it in schedules }.let(::listOfNotNull)).singleOrNull()
        return id?.let { schedules[it] }.orEmpty()
    }
}

internal data class XmlTvResult(val guide: XmlTvGuide?, val failed: Boolean, val needsRefresh: Boolean = false)

internal suspend fun loadXmlTvGuide(
    directory: File, uri: URI, hours: Int, refresh: Boolean,
    allowDownload: Boolean = true,
    maxCacheAgeMillis: Long = 0
): XmlTvResult = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256").digest(uri.toASCIIString().encodeToByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    val saved = File(directory, "programme-$digest.xmltv")
    var failed = false
    val age = System.currentTimeMillis() - saved.lastModified()
    if (allowDownload && (!saved.isFile || refresh && age !in 0 until maxCacheAgeMillis)) {
        var temporary: File? = null
        try {
            val download = File.createTempFile("programme-", ".tmp", directory)
            temporary = download
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                check(connection.responseCode == 200)
                connection.inputStream.use { input ->
                    download.outputStream().use { output ->
                        val buffer = ByteArray(32_768)
                        var total = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= GUIDE_DOWNLOAD_LIMIT)
                            output.write(buffer, 0, count)
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
            val guide = readXmlTv(download, hours)
            Files.move(download.toPath(), saved.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return@withContext XmlTvResult(guide, false)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            failed = true
        } finally {
            temporary?.delete()
        }
    }
    val guide = try {
        if (saved.isFile) readXmlTv(saved, hours) else null
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        failed = true
        null
    }
    XmlTvResult(guide, failed, guide == null || age !in 0 until maxCacheAgeMillis)
}

private suspend fun readXmlTv(file: File, hours: Int): XmlTvGuide =
    file.inputStream().buffered().use { stream ->
        stream.mark(2)
        val gzip = stream.read() == 0x1f && stream.read() == 0x8b
        stream.reset()
        if (gzip) GZIPInputStream(stream).use { parseXmlTv(boundedXml(it), hours) }
        else parseXmlTv(stream, hours)
    }

private fun boundedXml(input: InputStream): InputStream = object : FilterInputStream(input) {
    private var consumed = 0L

    override fun read(): Int = super.read().also { if (it >= 0) record(1) }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
        super.read(bytes, offset, length).also { if (it > 0) record(it) }

    private fun record(count: Int) {
        consumed += count
        check(consumed <= GUIDE_PARSE_LIMIT)
    }
}

private suspend fun parseXmlTv(input: InputStream, hours: Int): XmlTvGuide {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
    parser.setInput(input, null)
    check(parser.nextTag() == XmlPullParser.START_TAG && parser.name == "tv")
    val schedules = mutableMapOf<String, MutableList<PortalProgramme>>()
    val aliases = mutableMapOf<String, MutableSet<String>>()
    val now = System.currentTimeMillis() / 1_000
    var admission = 0
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2) continue
        currentCoroutineContext().ensureActive()
        when (parser.name) {
            "channel" -> {
                val id = parser.getAttributeValue(null, "id")?.let(::normalizedLabel).orEmpty()
                while (parser.next() != XmlPullParser.END_DOCUMENT &&
                    !(parser.eventType == XmlPullParser.END_TAG && parser.depth == 2)
                ) {
                    if (id.isNotEmpty() && parser.eventType == XmlPullParser.START_TAG && parser.name == "display-name") {
                        aliases.getOrPut(normalizedLabel(parser.nextText())) { linkedSetOf() }.add(id)
                    }
                }
            }
            "programme" -> {
                val id = parser.getAttributeValue(null, "channel")?.let(::normalizedLabel).orEmpty()
                val start = xmlTvTime(parser.getAttributeValue(null, "start")) ?: continue
                val end = xmlTvTime(parser.getAttributeValue(null, "stop"))?.takeIf { it > start }
                if (id.isEmpty() || start >= now + hours * 3_600L || (end ?: start + 3_600L) <= now - GUIDE_PAST_SECONDS) continue
                var title: String? = null
                var description: String? = null
                while (parser.next() != XmlPullParser.END_DOCUMENT &&
                    !(parser.eventType == XmlPullParser.END_TAG && parser.depth == 2)
                ) {
                    if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 3) continue
                    val german = parser.getAttributeValue(null, "lang")?.startsWith("de", ignoreCase = true) == true
                    when (parser.name) {
                        "title" -> parser.nextText().trim().take(512).takeIf(String::isNotBlank)?.let {
                            if (title == null || german) title = it
                        }
                        "desc" -> parser.nextText().trim().take(4_096).takeIf(String::isNotBlank)?.let {
                            if (description == null || german) description = it
                        }
                    }
                }
                title?.let {
                    check(++admission <= 250_000)
                    schedules.getOrPut(id) { mutableListOf() }.add(PortalProgramme(it, start, end, description))
                }
            }
        }
    }
    return XmlTvGuide(schedules.mapValues { (_, items) -> items.distinct().sortedBy { it.startSeconds } }, aliases)
}
