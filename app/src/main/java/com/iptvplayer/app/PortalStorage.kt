package com.iptvplayer.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
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
    val epgChannelIds: Set<String> = emptySet()
)

internal class PortalStorage(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "stalker-source.v1"))
    private companion object {
        val mutex = Mutex()
    }

    suspend fun read(): SavedPortal? = mutex.withLock {
        withContext(Dispatchers.IO) {
            val bytes = try {
                file.readFully()
            } catch (_: FileNotFoundException) {
                return@withContext null
            }
            require(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes, 0, 12))
            decode(JSONObject(cipher.doFinal(bytes, 12, bytes.size - 12).decodeToString()))
        }
    }

    suspend fun write(source: SavedPortal) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
            check(cipher.iv.size == 12)
            val encrypted = cipher.iv + cipher.doFinal(encode(source).toString().encodeToByteArray())
            val output = file.startWrite()
            try {
                output.write(encrypted)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
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
                entry.optBoolean("direct")
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
                            categoryName = title.optString("categoryName").ifEmpty { null }
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
            if (epgIds == null) emptySet() else List(epgIds.length()) { epgIds.getString(it) }.toSet()
        )
    }
}
