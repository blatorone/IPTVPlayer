package com.iptvplayer.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class SavedPortal(
    val address: String,
    val mac: String,
    val channels: List<StalkerChannel>,
    val genres: Map<String, String>,
    val catalogs: Map<PortalSection, PortalCatalog>,
    val epgChannelIds: Set<String> = emptySet(),
    val subscriptionExpiry: java.time.LocalDate? = null,
    val enabled: Boolean = true,
    val reachable: Boolean? = null,
    /** True only when the app disabled the source after an unsuccessful check. */
    val autoDisabled: Boolean = false
) {
    val identity: String by lazy {
        val input = "$address\n${normalizePortalMac(mac)}".encodeToByteArray()
        MessageDigest.getInstance("SHA-256").digest(input).joinToString("") { "%02x".format(it) }
    }
}

internal data class SavedPortals(val sources: List<SavedPortal>, val activeId: String)
internal data class CachedProgramme(val fetchedAt: Long, val programmes: List<PortalProgramme>)
internal data class StartupStream(
    val address: String, val mac: String, val id: String?, val command: String, val direct: Boolean
)
internal data class StartupChannel(
    val key: String, val selectionSource: String, val selectionId: String, val scope: String,
    val name: String, val streams: List<StartupStream>
)

internal class PortalStorage(context: Context) {
    /** Keystore-encrypted single document of versions up to 1.0.73; read only for migration. */
    private val file = AtomicFile(File(context.noBackupFilesDir, "stalker-source.v1"))
    private val migratedLegacyFile = File(context.noBackupFilesDir, "stalker-source.v1.migrated")
    private val libraryFile = AtomicFile(File(context.noBackupFilesDir, "portal-library.v2"))
    private val metadataFile = AtomicFile(File(context.noBackupFilesDir, "metadata-token.v1"))
    private val homeAssistantFile = AtomicFile(File(context.noBackupFilesDir, "home-assistant-token.v1"))
    private val traktSessionFile = AtomicFile(File(context.noBackupFilesDir, "trakt-session.v1"))
    private val traktAppFile = AtomicFile(File(context.noBackupFilesDir, "trakt-app.v1"))
    /** Trakt session of a kids account that keeps its own data. */
    private val kidsTraktSessionFile = AtomicFile(File(context.noBackupFilesDir, "trakt-session-kids.v1"))
    private fun traktFile(kids: Boolean) = if (kids) kidsTraktSessionFile else traktSessionFile
    private val epgSourcesFile = AtomicFile(File(context.noBackupFilesDir, "epg-sources.v1"))
    private val legacyGuideCacheFile = AtomicFile(File(context.noBackupFilesDir, "portal-guide.v1"))
    private val guideCacheFile = AtomicFile(File(context.noBackupFilesDir, "portal-guide.v2"))
    private val startupChannelFile = AtomicFile(File(context.noBackupFilesDir, "last-live.v1"))
    private val guideCacheMutex = Mutex()
    private val startupMutex = Mutex()
    private companion object {
        val mutex = Mutex()
    }

    suspend fun read(): SavedPortals? = mutex.withLock {
        withContext(Dispatchers.IO) {
            var failure: Exception? = null
            if (libraryFile.baseFile.exists()) {
                try {
                    val library = readEnvelope(libraryFile, ::readPortalLibrary)
                    // The current format was read successfully; the migrated copy is no longer needed.
                    migratedLegacyFile.delete()
                    file.delete()
                    return@withContext library
                } catch (error: Exception) {
                    failure = error
                }
            }
            // Earlier versions stored one Keystore-encrypted JSON document. It is converted
            // once and kept as a copy until the converted library has been read again.
            val legacy = when {
                file.baseFile.exists() -> file
                migratedLegacyFile.exists() -> AtomicFile(migratedLegacyFile)
                else -> failure?.let { throw it } ?: return@withContext null
            }
            val library = decodeLegacyLibrary(readEncrypted(legacy) ?: return@withContext null)
            val converted = try {
                writeLibrary(library)
                true
            } catch (error: Exception) {
                // Keep using the earlier file; conversion is retried on the next start.
                false
            }
            if (converted && legacy === file) {
                migratedLegacyFile.delete()
                if (!file.baseFile.renameTo(migratedLegacyFile)) file.delete()
            }
            library
        }
    }

    private fun decodeLegacyLibrary(plain: String): SavedPortals {
        val data = JSONObject(plain)
        val sources = data.optJSONArray("sources")
        return if (sources == null) {
            val source = decode(data)
            SavedPortals(listOf(source), source.identity)
        } else {
            SavedPortals(
                List(sources.length()) { decode(sources.getJSONObject(it)) },
                data.getString("activeId")
            )
        }
    }

    suspend fun write(library: SavedPortals) = mutex.withLock {
        withContext(Dispatchers.IO) { writeLibrary(library) }
    }

    private fun writeLibrary(library: SavedPortals) = writeEnvelope(libraryFile) { writer ->
        JsonWriter(writer).use { json ->
            writePortalLibrary(json, library)
            json.flush()
        }
    }

    private fun <T> readEnvelope(target: AtomicFile, read: (JsonReader) -> T): T =
        EnvelopeInputStream(BufferedInputStream(target.openRead(), 64 * 1024), ::unwrapDataKey).use { input ->
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use(read)
        }

    private fun readEnvelopeText(target: AtomicFile): String =
        EnvelopeInputStream(BufferedInputStream(target.openRead(), 64 * 1024), ::unwrapDataKey).use { input ->
            input.readBytes().decodeToString()
        }

    /** Writes [target] atomically; [write] receives a writer whose closing finishes the envelope. */
    private fun writeEnvelope(target: AtomicFile, write: (java.io.Writer) -> Unit) {
        val dataKey = newEnvelopeDataKey()
        val output = target.startWrite()
        try {
            val buffered = BufferedOutputStream(output, 64 * 1024)
            val envelope = EnvelopeOutputStream(buffered, dataKey, wrapDataKey(dataKey))
            // The writer chain must not close the file stream before finishWrite.
            val shield = object : java.io.FilterOutputStream(envelope) {
                override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
                override fun close() = flush()
            }
            write(OutputStreamWriter(shield, Charsets.UTF_8))
            envelope.close()
            buffered.flush()
            target.finishWrite(output)
        } catch (error: Exception) {
            target.failWrite(output)
            throw error
        }
    }

    private fun wrapDataKey(dataKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        check(cipher.iv.size == 12)
        return cipher.iv + cipher.doFinal(dataKey.encoded)
    }

    private fun unwrapDataKey(wrapped: ByteArray): SecretKey {
        require(wrapped.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, wrapped, 0, 12))
        return SecretKeySpec(cipher.doFinal(wrapped, 12, wrapped.size - 12), "AES")
    }

    suspend fun readMetadataToken(): String? = mutex.withLock {
        withContext(Dispatchers.IO) { readEncrypted(metadataFile)?.ifBlank { null } }
    }

    suspend fun writeMetadataToken(token: String) = mutex.withLock {
        withContext(Dispatchers.IO) { writeEncrypted(metadataFile, token) }
    }

    /** Home Assistant access token (smart home); encrypted like the other accounts. */
    suspend fun readHomeAssistantToken(): String? = mutex.withLock {
        withContext(Dispatchers.IO) { readEncrypted(homeAssistantFile)?.ifBlank { null } }
    }

    suspend fun writeHomeAssistantToken(token: String?) = mutex.withLock {
        withContext(Dispatchers.IO) { if (token == null) homeAssistantFile.delete() else writeEncrypted(homeAssistantFile, token) }
    }

    suspend fun readTraktSession(kids: Boolean = false): TraktSession? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val data = readEncrypted(traktFile(kids))?.let(::JSONObject) ?: return@withContext null
            TraktSession(
                data.getString("clientId"), data.getString("accessToken"), data.getString("refreshToken"),
                data.getLong("expiresAtMillis"), data.optString("username", "Trakt"),
                data.optString("clientSecret").ifBlank { null }
            )
        }
    }

    suspend fun writeTraktSession(session: TraktSession, kids: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) {
            writeEncrypted(traktFile(kids), JSONObject().apply {
                put("clientId", session.clientId)
                put("accessToken", session.accessToken)
                put("refreshToken", session.refreshToken)
                put("expiresAtMillis", session.expiresAtMillis)
                put("username", session.username)
                session.clientSecret?.let { put("clientSecret", it) }
            }.toString())
        }
    }

    suspend fun readTraktApp(): TraktApp? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val data = readEncrypted(traktAppFile)?.let(::JSONObject) ?: return@withContext null
            TraktApp(data.getString("clientId"), data.optString("clientSecret").ifBlank { null })
        }
    }

    suspend fun writeTraktApp(app: TraktApp?) = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (app == null) traktAppFile.delete()
            else writeEncrypted(traktAppFile, JSONObject().apply {
                put("clientId", app.clientId)
                app.clientSecret?.let { put("clientSecret", it) }
            }.toString())
        }
    }

    suspend fun clearTraktSession(kids: Boolean = false) = mutex.withLock {
        withContext(Dispatchers.IO) { traktFile(kids).delete() }
    }

    suspend fun readEpgSources(): List<String> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val urls = JSONArray(readEncrypted(epgSourcesFile) ?: "[]")
            List(urls.length()) { urls.getString(it) }
        }
    }

    suspend fun writeEpgSources(urls: List<String>) = mutex.withLock {
        withContext(Dispatchers.IO) { writeEncrypted(epgSourcesFile, JSONArray(urls).toString()) }
    }

    suspend fun readStartupChannel(): StartupChannel? = startupMutex.withLock {
        withContext(Dispatchers.IO) {
            val data = JSONObject(readEncrypted(startupChannelFile) ?: return@withContext null)
            val streams = data.getJSONArray("streams")
            StartupChannel(
                data.getString("key"), data.getString("source"), data.getString("selection"), data.getString("scope"),
                data.getString("name"), List(streams.length()) {
                    val stream = streams.getJSONObject(it)
                    StartupStream(stream.getString("address"), stream.getString("mac"),
                        stream.optString("id").ifBlank { null }, stream.getString("command"), stream.optBoolean("direct"))
                }
            )
        }
    }

    suspend fun writeStartupChannel(channel: StartupChannel) = startupMutex.withLock {
        withContext(Dispatchers.IO) {
            val data = JSONObject().apply {
                put("key", channel.key)
                put("source", channel.selectionSource)
                put("selection", channel.selectionId)
                put("scope", channel.scope)
                put("name", channel.name)
                put("streams", JSONArray().apply {
                    for (stream in channel.streams) put(JSONObject().apply {
                        put("address", stream.address)
                        put("mac", stream.mac)
                        put("id", stream.id)
                        put("command", stream.command)
                        put("direct", stream.direct)
                    })
                })
            }
            writeEncrypted(startupChannelFile, data.toString())
        }
    }

    suspend fun readGuideCache(): Map<String, CachedProgramme> = guideCacheMutex.withLock {
        withContext(Dispatchers.IO) {
            val text = if (guideCacheFile.baseFile.exists()) {
                runCatching { readEnvelopeText(guideCacheFile) }.getOrNull()
            } else readEncrypted(legacyGuideCacheFile)
            val data = JSONObject(text ?: "{}")
            data.keys().asSequence().associateWith { key ->
                val entry = data.getJSONObject(key)
                val shows = entry.getJSONArray("shows")
                CachedProgramme(entry.getLong("time"), List(shows.length()) { index ->
                    val show = shows.getJSONObject(index)
                    PortalProgramme(
                        show.getString("title"), show.optLong("start").takeIf { it != 0L },
                        show.optLong("end").takeIf { it != 0L },
                        show.optString("description").ifBlank { null }
                    )
                })
            }
        }
    }

    suspend fun writeGuideCache(cache: Map<String, CachedProgramme>) = guideCacheMutex.withLock {
        withContext(Dispatchers.IO) {
            val data = JSONObject()
            var remainingShows = 20_000
            for ((key, entry) in cache.entries.sortedByDescending { it.value.fetchedAt }.take(750)) {
                if (remainingShows == 0) break
                val shows = entry.programmes.take(minOf(256, remainingShows))
                remainingShows -= shows.size
                data.put(key, JSONObject().apply {
                    put("time", entry.fetchedAt)
                    put("shows", JSONArray().apply {
                        for (show in shows) put(JSONObject().apply {
                            put("title", show.title.take(256))
                            put("start", show.startSeconds)
                            put("end", show.endSeconds)
                            put("description", show.description?.take(1000))
                        })
                    })
                })
            }
            val text = data.toString()
            writeEnvelope(guideCacheFile) { writer -> writer.use { it.write(text) } }
            legacyGuideCacheFile.delete()
        }
    }

    private fun readEncrypted(target: AtomicFile): String? {
        val bytes = try {
            target.readFully()
        } catch (_: FileNotFoundException) {
            return null
        }
        require(bytes.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes, 0, 12))
        return cipher.doFinal(bytes, 12, bytes.size - 12).decodeToString()
    }

    private fun writeEncrypted(target: AtomicFile, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        check(cipher.iv.size == 12)
        val encrypted = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        val output = target.startWrite()
        try {
            output.write(encrypted)
            target.finishWrite(output)
        } catch (error: Exception) {
            target.failWrite(output)
            throw error
        }
    }

    private fun key(create: Boolean): SecretKey {
        val alias = "iptvplayer.stalker.v1"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private fun decode(data: JSONObject): SavedPortal {
        val entries = data.getJSONArray("channels")
        val channels = List(entries.length()) { index ->
            val entry = entries.getJSONObject(index)
            StalkerChannel(
                entry.getString("name"),
                entry.getString("command"),
                entry.optString("genreId").ifEmpty { null },
                entry.optString("genreName").ifEmpty { null },
                entry.optString("id").ifEmpty { null },
                entry.optString("logo").ifEmpty { null },
                entry.optBoolean("direct"),
                entry.optString("epgIdentity").ifEmpty { null },
                entry.optLong("number").takeIf { it > 0 }
            )
        }
        val labels = data.getJSONObject("genres")
        val genres = labels.keys().asSequence().associateWith { labels.getString(it) }
        val entriesBySection = data.getJSONObject("catalogs")
        val catalogs = buildMap {
            for (section in PortalSection.entries) {
                val entry = entriesBySection.optJSONObject(section.name) ?: continue
                val titles = entry.getJSONArray("titles")
                put(section, PortalCatalog(
                    List(titles.length()) { index ->
                        val title = titles.optJSONObject(index)
                        if (title == null) PortalTitle(titles.getString(index))
                        else PortalTitle(
                            title.getString("name"),
                            title.optString("id").ifEmpty { null },
                            title.optString("command").ifEmpty { null },
                            categoryId = title.optString("categoryId").ifEmpty { null },
                            categoryName = title.optString("categoryName").ifEmpty { null },
                            poster = title.optString("poster").ifEmpty { null },
                            year = title.optInt("year").takeIf { it > 0 },
                            addedAt = title.optLong("addedAt").takeIf { it > 0 },
                            description = title.optString("description").ifEmpty { null },
                            imdbId = title.optString("imdbId").ifEmpty { null },
                            tmdbId = title.optString("tmdbId").ifEmpty { null },
                            popularity = title.optDouble("popularity").takeIf { it.isFinite() && it > 0 },
                            service = title.optString("service").ifEmpty { null }
                        )
                    },
                    entry.optInt("total", -1).takeIf { it >= 0 },
                    entry.optInt("nextPage", -1).takeIf { it > 0 },
                    entry.optJSONArray("groups")?.let { groups ->
                        List(groups.length()) { index ->
                            val group = groups.getJSONObject(index)
                            PortalGroup(
                                group.getString("id"), group.getString("name"),
                                group.optString("country").ifEmpty { null }
                            )
                        }
                    }.orEmpty()
                ))
            }
        }
        val epgIds = data.optJSONArray("epgChannelIds")
        return SavedPortal(
            data.getString("address"), data.getString("mac"), channels, genres, catalogs,
            if (epgIds == null) emptySet() else List(epgIds.length()) { epgIds.getString(it) }.toSet(),
            parsePortalExpiry(data.optString("subscriptionExpiry")),
            data.optBoolean("enabled", true),
            if (data.has("reachable") && !data.isNull("reachable")) data.optBoolean("reachable") else null,
            data.optBoolean("autoDisabled", false)
        )
    }
}
