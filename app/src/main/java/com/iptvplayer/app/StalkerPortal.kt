package com.iptvplayer.app

import android.util.JsonReader
import android.util.JsonToken
import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class StalkerChannel(
    val name: String,
    val command: String,
    val genreId: String?,
    val genreName: String?,
    val id: String? = null,
    val logo: String? = null,
    val direct: Boolean = false
)

internal data class PortalProgramme(val title: String, val startSeconds: Long?)

internal class PortalRequestException(val status: Int) : IllegalStateException()
internal class PortalAuthenticationException : IllegalStateException()
internal class PortalHandshakeException : IllegalStateException()
internal class InvalidPortalAddressException : IllegalArgumentException()
internal class PortalChannelListException : IllegalStateException()
internal class ChannelImport(val channels: List<StalkerChannel>, val partial: Boolean = false)
internal data class PortalTitle(
    val name: String,
    val id: String? = null,
    val command: String? = null,
    val episode: String? = null
)

internal data class PortalCatalog(
    val titles: List<PortalTitle>,
    val total: Int?,
    val nextPage: Int?
)

internal fun normalizePortalMac(address: String): String {
    val value = address.trim().uppercase(Locale.ROOT)
    val hexPair = "[0-9A-F]{2}"
    require(
        value.matches(Regex("[0-9A-F]{12}")) ||
            value.matches(Regex("(?:$hexPair:){5}$hexPair")) ||
            value.matches(Regex("(?:$hexPair-){5}$hexPair"))
    )
    return value.filter { it != ':' && it != '-' }.chunked(2).joinToString(":")
}

internal enum class PortalSection(val type: String, val label: Int) {
    LIVE("itv", R.string.destination_live),
    MOVIES("vod", R.string.destination_movies),
    SERIES("series", R.string.destination_series)
}

internal sealed interface ImportStatus {
    data object Loading : ImportStatus
    data object Failed : ImportStatus
    data class Progress(val loaded: Int, val total: Int?) : ImportStatus
    data class Count(val value: Int) : ImportStatus
    data class Loaded(val value: Int) : ImportStatus
    data class Partial(val value: Int) : ImportStatus
    data class Error(val message: Int) : ImportStatus
}

internal class StalkerPortal(address: String, macAddress: String) {
    private val mac = normalizePortalMac(macAddress)
    private val endpoints = endpointsFor(address.trim())
    private var endpoint = endpoints.first()
    private val sessionMutex = Mutex()
    private var sessionToken: String? = null

    suspend fun openSession(): String = sessionMutex.withLock {
        sessionToken ?: withContext(Dispatchers.IO) { authenticate() }.also { sessionToken = it }
    }

    suspend fun loadCatalog(
        section: PortalSection,
        token: String,
        page: Int = 1,
        previous: PortalCatalog? = null
    ): PortalCatalog = withContext(Dispatchers.IO) {
        require(section != PortalSection.LIVE)
        val response = request(
            section.type, "get_ordered_list", token,
            mapOf("category" to "*", "p" to page.toString())
        )
        val entries = entriesFrom(response)
        val newTitles = buildList {
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                val title = entry.text("name", "title") ?: continue
                add(PortalTitle(title, entry.text("id", "movie_id", "stream_id"), entry.text("cmd", "command")))
            }
        }
        check(entries.length() == 0 || newTitles.isNotEmpty())
        val total = (response as? JSONObject)?.optInt("total_items", -1)
            ?.takeIf { it >= entries.length() } ?: previous?.total
        val titles = previous?.titles.orEmpty() + newTitles
        val hasMore = newTitles.isNotEmpty() && (total == null || titles.size < total)
        PortalCatalog(titles, total, if (hasMore) page + 1 else null)
    }

    suspend fun loadEpisodes(series: PortalTitle, token: String): List<PortalTitle> =
        withContext(Dispatchers.IO) {
            val id = series.id ?: return@withContext emptyList()
            val result = linkedSetOf<PortalTitle>()
            var rows = 0
            for (page in 1..50) {
                coroutineContext.ensureActive()
                val response = request(
                    "series", "get_ordered_list", token,
                    mapOf("movie_id" to id, "p" to page.toString())
                )
                val entries = entriesFrom(response)
                if (entries.length() == 0) break
                val previousSize = result.size
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    val command = entry.text("cmd", "command") ?: continue
                    val name = entry.text("name", "title") ?: series.name
                    val episodes = entry.optJSONArray("series")
                    if (episodes == null) {
                        result.add(PortalTitle(name, command = command, episode = entry.text("episode")))
                    } else for (number in 0 until episodes.length()) {
                        val episode = episodes.optString(number)
                        if (episode.isNotBlank()) {
                            result.add(PortalTitle(name, command = command, episode = episode))
                        }
                    }
                }
                rows += entries.length()
                val total = (response as? JSONObject)?.optInt("total_items", -1) ?: -1
                if (result.size == previousSize || (total >= 0 && rows >= total)) break
            }
            result.toList()
        }

    suspend fun loadGenres(token: String): Map<String, String> = withContext(Dispatchers.IO) {
        val entries = entriesFrom(request("itv", "get_genres", token))
        buildMap {
            for (index in 0 until entries.length()) {
                val genre = entries.optJSONObject(index) ?: continue
                val id = genre.optString("id")
                val title = genre.optString("title")
                if (id.isNotBlank() && id != "*" && title.isNotBlank()) put(id, title)
            }
        }
    }

    suspend fun loadChannels(
        token: String,
        onProgress: suspend (Int, Int?) -> Unit
    ): ChannelImport = withContext(Dispatchers.IO) {
        val allChannels = try {
            streamChannels(token, onProgress)
        } catch (error: Exception) {
            if (error is PortalRequestException && error.status in listOf(401, 403)) throw error
            emptyList()
        }
        if (allChannels.isNotEmpty()) return@withContext ChannelImport(allChannels)

        var emptyConfirmed = false
        for (genre in listOf("*", "0", null)) {
            coroutineContext.ensureActive()
            val result = try {
                loadChannelPages(token, genre, onProgress)
            } catch (error: PortalRequestException) {
                if (error.status in listOf(401, 403)) throw error
                null
            } catch (_: Exception) {
                coroutineContext.ensureActive()
                null
            } ?: continue
            if (result.channels.isNotEmpty()) return@withContext result
            emptyConfirmed = true
        }
        if (emptyConfirmed) ChannelImport(emptyList()) else throw PortalChannelListException()
    }

    private suspend fun loadChannelPages(
        token: String,
        genre: String?,
        onProgress: suspend (Int, Int?) -> Unit
    ): ChannelImport {
        val channels = mutableListOf<StalkerChannel>()
        var scanned = 0
        val seenPages = mutableSetOf<List<Pair<String, String>>>()
        try {
            for (page in 1..10_000) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val parameters = mutableMapOf("p" to page.toString(), "fav" to "0")
                if (genre != null) parameters["genre"] = genre
                val response = request("itv", "get_ordered_list", token, parameters)
                val entries = entriesFrom(response)
                val total = (response as? JSONObject)?.optInt("total_items", -1) ?: -1
                if (entries.length() == 0) {
                    check(total <= scanned && (scanned > 0 || total == 0))
                    check(scanned == 0 || channels.isNotEmpty())
                    return ChannelImport(channels)
                }
                val parsed = parseChannels(entries)
                check(parsed.isNotEmpty())
                check(seenPages.add(parsed.map { it.name to it.command }))
                scanned += entries.length()
                channels += parsed
                onProgress(channels.size, total.takeIf { it >= scanned })
                if (total >= 0 && scanned >= total) return ChannelImport(channels)
            }
        } catch (error: Exception) {
            kotlin.coroutines.coroutineContext.ensureActive()
            if (channels.isEmpty()) throw error
        }
        return ChannelImport(channels, partial = true)
    }

    private suspend fun streamChannels(
        token: String,
        onProgress: suspend (Int, Int?) -> Unit
    ): List<StalkerChannel> {
        val connection = requestConnection("itv", "get_all_channels", token)
        try {
            val status = connection.responseCode
            if (status != 200) throw PortalRequestException(status)
            val channels = mutableListOf<StalkerChannel>()
            JsonReader(LimitedCatalogueStream(connection.inputStream).reader(Charsets.UTF_8)).use {
                readChannelContainer(it, channels, onProgress, depth = 0)
                check(it.peek() == JsonToken.END_DOCUMENT)
            }
            return channels
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun readChannelContainer(
        reader: JsonReader,
        channels: MutableList<StalkerChannel>,
        onProgress: suspend (Int, Int?) -> Unit,
        depth: Int
    ) {
        check(depth < 6)
        when (reader.peek()) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    kotlin.coroutines.coroutineContext.ensureActive()
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        val channel = parseChannel(readChannelFields(reader))
                        if (channel != null) {
                            channels.add(channel)
                            if (channels.size % 250 == 0) onProgress(channels.size, null)
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
            }
            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.hasNext()) {
                    kotlin.coroutines.coroutineContext.ensureActive()
                    val name = reader.nextName()
                    when {
                        name == "js" || name == "data" ->
                            readChannelContainer(reader, channels, onProgress, depth + 1)
                        name.toLongOrNull() != null && reader.peek() == JsonToken.BEGIN_OBJECT -> {
                            val channel = parseChannel(readChannelFields(reader))
                            if (channel != null) {
                                channels.add(channel)
                                if (channels.size % 250 == 0) onProgress(channels.size, null)
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            else -> reader.skipValue()
        }
    }

    private fun readChannelFields(reader: JsonReader): JSONObject {
        val fields = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when {
                name in channelTextFields && reader.peek() in setOf(JsonToken.STRING, JsonToken.NUMBER) ->
                    fields.put(name, reader.nextString())
                name == "cmds" && reader.peek() == JsonToken.BEGIN_ARRAY -> {
                    reader.beginArray()
                    if (reader.hasNext() && reader.peek() == JsonToken.BEGIN_OBJECT) {
                        fields.put("cmds", JSONArray().put(readCommandFields(reader)))
                    }
                    while (reader.hasNext()) reader.skipValue()
                    reader.endArray()
                }
                name == "cmds" && reader.peek() == JsonToken.BEGIN_OBJECT ->
                    fields.put("cmds", readCommandFields(reader))
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return fields
    }

    private fun readCommandFields(reader: JsonReader): JSONObject {
        val fields = JSONObject()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (name == "cmd" && reader.peek() == JsonToken.STRING) fields.put(name, reader.nextString())
            else reader.skipValue()
        }
        reader.endObject()
        return fields
    }

    private fun entriesFrom(response: Any): JSONArray = when (response) {
        is JSONArray -> response
        is JSONObject -> {
            if (response.has("data")) returnEntries(response.get("data"))
            else returnEntries(response)
        }
        else -> error("Invalid channel list")
    }

    private fun returnEntries(value: Any): JSONArray = when (value) {
        is JSONArray -> value
        is JSONObject -> JSONArray().apply {
            for (key in value.keys()) {
                if (key.toLongOrNull() != null) put(value.get(key))
            }
            check(length() > 0 || value.length() == 0)
        }
        else -> error("Invalid channel list")
    }

    private fun parseChannels(entries: JSONArray): List<StalkerChannel> = buildList {
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            parseChannel(entry)?.let { add(it) }
        }
    }

    private fun parseChannel(entry: JSONObject): StalkerChannel? {
        val name = entry.text("name", "title") ?: return null
        val command = entry.text("cmd", "command")
            ?: entry.optJSONObject("cmds")?.text("cmd")
            ?: entry.optJSONArray("cmds")?.optJSONObject(0)?.text("cmd")
            ?: return null
        return StalkerChannel(
            name = name,
            command = command,
            genreId = entry.text("tv_genre_id", "genre_id", "category_id"),
            genreName = entry.text("group_title", "genre_name"),
            id = entry.text("id", "ch_id", "stream_id"),
            logo = entry.text("logo", "logo_url", "stream_icon")?.let(::resolveLogo),
            direct = entry.text("use_http_tmp_link") == "0" &&
                listOf("wowza_tmp_link", "use_load_balancing", "nginx_secure_link").all {
                    entry.text(it) in listOf(null, "0")
                }
        )
    }

    private fun resolveLogo(value: String): String? {
        if (value.isBlank()) return null
        val uri = try {
            endpoint.resolve(value.trim().replace(" ", "%20"))
        } catch (_: IllegalArgumentException) {
            return null
        }
        return uri.takeIf {
            it.scheme in listOf("http", "https") && it.host != null && it.userInfo == null
        }?.toString()
    }

    suspend fun loadProgramme(channel: StalkerChannel, token: String): List<PortalProgramme> =
        withContext(Dispatchers.IO) {
            val id = channel.id ?: return@withContext emptyList()
            val entries = entriesFrom(request(
                "itv", "get_short_epg", token, mapOf("ch_id" to id, "size" to "100")
            ))
            buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    val title = entry.optString("name").ifBlank { entry.optString("title") }
                    if (title.isNotBlank()) add(PortalProgramme(
                        title, entry.optLong("start_timestamp", 0).takeIf { it > 0 }
                    ))
                }
            }
        }

    suspend fun resolve(channel: StalkerChannel, token: String): String {
        if (channel.direct) {
            val direct = runCatching { URI(cleanPlaybackCommand(channel.command)) }.getOrNull()
            if (direct?.scheme in listOf("http", "https") && direct?.host != null &&
                direct.host.lowercase(Locale.ROOT) !in setOf("localhost", "127.0.0.1", "0.0.0.0", "[::1]")
            ) return direct.toASCIIString()
        }
        return resolveLink("itv", channel.command, "", token)
    }

    suspend fun resolveVod(title: PortalTitle, token: String): String =
        resolveLink("vod", requireNotNull(title.command), title.episode.orEmpty(), token)

    private suspend fun resolveLink(
        type: String,
        command: String,
        episode: String,
        token: String
    ): String = withContext(Dispatchers.IO) {
        val response = request(
            type, "create_link", token,
            mapOf(
                "cmd" to command,
                "series" to episode,
                "forced_storage" to "",
                "disable_ad" to "0",
                "download" to "0"
            )
        )
        val link = when (response) {
            is JSONObject -> {
                val error = response.opt("error")
                check(error == null || error == JSONObject.NULL ||
                    error.toString() in setOf("", "0", "false"))
                response.text("cmd", "command", "url") ?: error("Missing playback link")
            }
            is String -> response
            else -> error("Missing playback link")
        }
        val stream = cleanPlaybackCommand(link)
        require(stream.startsWith("/") || stream.startsWith("http://", ignoreCase = true) ||
            stream.startsWith("https://", ignoreCase = true))
        val uri = endpoint.resolve(stream)
        require(uri.scheme in listOf("http", "https") && uri.host != null)
        uri.toASCIIString()
    }

    private fun authenticate(): String {
        var lastFailure: Exception? = null
        for (candidate in (listOf(endpoint) + endpoints).distinct()) {
            try {
                val response = request(
                    "stb", "handshake", parameters = mapOf("token" to "", "prehash" to "false"),
                    target = candidate
                ) as? JSONObject ?: throw PortalHandshakeException()
                val token = response.optString("token")
                if (token.isBlank() || token.any { it.isISOControl() }) {
                    throw PortalHandshakeException()
                }
                val profile = request("stb", "get_profile", token, target = candidate) as? JSONObject
                if (profile?.optInt("blocked", 0) == 1 || profile?.optBoolean("blocked") == true) {
                    throw PortalAuthenticationException()
                }
                endpoint = candidate
                return token
            } catch (error: Exception) {
                if (error is PortalAuthenticationException) throw error
                if (lastFailure is PortalHandshakeException && error is PortalRequestException &&
                    error.status == 404
                ) continue
                lastFailure = error
            }
        }
        val failure = lastFailure ?: throw PortalHandshakeException()
        if (failure is PortalRequestException && failure.status in listOf(401, 403)) {
            throw PortalAuthenticationException()
        }
        throw failure
    }

    private fun request(
        type: String,
        action: String,
        token: String? = null,
        parameters: Map<String, String> = emptyMap(),
        target: URI = endpoint
    ): Any {
        val connection = requestConnection(type, action, token, parameters, target)
        try {
            val status = connection.responseCode
            if (action == "create_link" && status in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location") ?: throw PortalRequestException(status)
                return target.resolve(location).toASCIIString()
            }
            if (status != 200) throw PortalRequestException(status)
            val body = connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                val output = ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    check(output.size() + count <= 8 * 1024 * 1024)
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
            val response = JSONObject(body)
            return if (response.has("js")) response.get("js") else response
        } finally {
            connection.disconnect()
        }
    }

    private fun requestConnection(
        type: String,
        action: String,
        token: String?,
        parameters: Map<String, String> = emptyMap(),
        target: URI = endpoint
    ): HttpURLConnection {
        val query = (mapOf("type" to type, "action" to action, "JsHttpRequest" to "1-xml") +
            parameters).entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }
        return (URI("$target?$query").toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Cookie", "mac=$mac")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
    }

    private fun endpointsFor(address: String): List<URI> {
        val uri = try { URI(address) } catch (_: java.net.URISyntaxException) {
            throw InvalidPortalAddressException()
        }
        if (uri.scheme !in listOf("http", "https") || uri.host == null ||
            uri.userInfo != null || uri.query != null || uri.fragment != null
        ) throw InvalidPortalAddressException()
        val path = uri.path.trimEnd('/').removeSuffix("/index.html").removeSuffix("/c")
        val paths = when {
            path.endsWith(".php") -> listOf(path)
            path.endsWith("/stalker_portal") -> listOf("$path/server/load.php", "$path/portal.php")
            else -> listOf("$path/portal.php", "$path/server/load.php", "$path/stalker_portal/server/load.php")
        }
        return paths.map { URI(uri.scheme, uri.authority, it, null, null) }
    }
}

private fun cleanPlaybackCommand(command: String): String = command.trim()
    .replaceFirst(Regex("^(?:ffmpeg|ffrt|auto|ffplay)\\s+", RegexOption.IGNORE_CASE), "")
    .removeSurrounding("\"")
    .removeSurrounding("'")

private fun JSONObject.text(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
    opt(name)?.takeUnless { it == JSONObject.NULL }?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}

private val channelTextFields = setOf(
    "name", "title", "cmd", "command", "tv_genre_id", "genre_id", "category_id",
    "group_title", "genre_name", "id", "ch_id", "stream_id", "logo", "logo_url", "stream_icon",
    "use_http_tmp_link", "wowza_tmp_link", "use_load_balancing", "nginx_secure_link"
)

private class LimitedCatalogueStream(input: InputStream) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int = super.read().also { if (it >= 0) addBytes(1) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) addBytes(it) }

    private fun addBytes(count: Int) {
        bytesRead += count
        check(bytesRead <= 96L * 1024 * 1024)
    }
}
