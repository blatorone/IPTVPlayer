package com.iptvplayer.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
    private val file = AtomicFile(File(context.noBackupFilesDir, "stalker-source.v1"))
    private val metadataFile = AtomicFile(File(context.noBackupFilesDir, "metadata-token.v1"))
    private val traktSessionFile = AtomicFile(File(context.noBackupFilesDir, "trakt-session.v1"))
    private val epgSourcesFile = AtomicFile(File(context.noBackupFilesDir, "epg-sources.v1"))
    private val guideCacheFile = AtomicFile(File(context.noBackupFilesDir, "portal-guide.v1"))
    private val startupChannelFile = AtomicFile(File(context.noBackupFilesDir, "last-live.v1"))
    private val guideCacheMutex = Mutex()
    private val startupMutex = Mutex()
    private companion object {
        val mutex = Mutex()
    }

    suspend fun read(): SavedPortals? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val plain = readEncrypted(file) ?: return@withContext null
            val data = JSONObject(plain)
            val sources = data.optJSONArray("sources")
            if (sources == null) {
                val source = decode(data)
                SavedPortals(listOf(source), source.identity)
            } else {
                SavedPortals(
                    List(sources.length()) { decode(sources.getJSONObject(it)) },
                    data.getString("activeId")
                )
            }
        }
    }

    suspend fun write(library: SavedPortals) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val data = JSONObject().apply {
                put("activeId", library.activeId)
                put("sources", JSONArray().apply {
                    for (source in library.sources) put(encode(source))
                })
            }
            writeEncrypted(file, data.toString())
        }
    }

    suspend fun readMetadataToken(): String? = mutex.withLock {
        withContext(Dispatchers.IO) { readEncrypted(metadataFile)?.ifBlank { null } }
    }

    suspend fun writeMetadataToken(token: String) = mutex.withLock {
        withContext(Dispatchers.IO) { writeEncrypted(metadataFile, token) }
    }

    suspend fun readTraktSession(): TraktSession? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val data = readEncrypted(traktSessionFile)?.let(::JSONObject) ?: return@withContext null
            TraktSession(
                data.getString("clientId"), data.getString("accessToken"), data.getString("refreshToken"),
                data.getLong("expiresAtMillis"), data.optString("username", "Trakt")
            )
        }
    }

    suspend fun writeTraktSession(session: TraktSession) = mutex.withLock {
        withContext(Dispatchers.IO) {
            writeEncrypted(traktSessionFile, JSONObject().apply {
                put("clientId", session.clientId)
                put("accessToken", session.accessToken)
                put("refreshToken", session.refreshToken)
                put("expiresAtMillis", session.expiresAtMillis)
                put("username", session.username)
            }.toString())
        }
    }

    suspend fun clearTraktSession() = mutex.withLock {
        withContext(Dispatchers.IO) { traktSessionFile.delete() }
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
            val data = JSONObject(readEncrypted(guideCacheFile) ?: "{}")
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
            writeEncrypted(guideCacheFile, data.toString())
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

    private fun encode(source: SavedPortal): JSONObject = JSONObject().apply {
        put("address", source.address)
        put("mac", source.mac)
        put("genres", JSONObject(source.genres))
        put("epgChannelIds", JSONArray(source.epgChannelIds))
        put("subscriptionExpiry", source.subscriptionExpiry?.toString())
        put("enabled", source.enabled)
        put("autoDisabled", source.autoDisabled)
        source.reachable?.let { put("reachable", it) }
        put("channels", JSONArray().apply {
            for (channel in source.channels) {
                put(JSONObject().apply {
                    put("name", channel.name)
                    put("command", channel.command)
                    put("genreId", channel.genreId)
                    put("genreName", channel.genreName)
                    put("id", channel.id)
                    put("logo", channel.logo)
                    put("direct", channel.direct)
                    put("epgIdentity", channel.epgIdentity)
                    put("number", channel.number)
                })
            }
        })
        put("catalogs", JSONObject().apply {
            for ((section, catalog) in source.catalogs) {
                put(section.name, JSONObject().apply {
                    put("titles", JSONArray().apply {
                        for (title in catalog.titles) put(JSONObject().apply {
                            put("name", title.name)
                            put("id", title.id)
                            put("command", title.command)
                            put("categoryId", title.categoryId)
                            put("categoryName", title.categoryName)
                            put("poster", title.poster)
                            put("year", title.year)
                            put("addedAt", title.addedAt)
                            put("description", title.description)
                            put("imdbId", title.imdbId)
                            put("tmdbId", title.tmdbId)
                            put("popularity", title.popularity)
                            put("service", title.service)
                        })
                    })
                    put("total", catalog.total)
                    put("nextPage", catalog.nextPage)
                    put("groups", JSONArray().apply {
                        for (group in catalog.groups) put(JSONObject().apply {
                            put("id", group.id)
                            put("name", group.name)
                            put("country", group.country)
                        })
                    })
                })
            }
        })
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
