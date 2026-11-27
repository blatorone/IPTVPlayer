package com.iptvplayer.app

import androidx.core.content.edit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAIN_SETTINGS = "catalog_visibility"
private val WATCH_FILES = listOf("vod_progress", "vod_progress_kids")
internal const val BACKUP_FILE_PREFIX = "StreamWatch-Backup-"
internal const val BACKUP_MIME = "application/octet-stream"

private fun appVersion(context: Context): String = runCatching {
    @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty()

/** Collects the chosen parts of this device into an encrypted backup file. */
internal suspend fun createBackup(context: Context, storage: PortalStorage, parts: Set<BackupPart>): ByteArray {
    val settings = context.getSharedPreferences(MAIN_SETTINGS, Context.MODE_PRIVATE).all
    fun settingsOf(part: BackupPart) = preferencesToJson(settings.filterKeys { backupPartOfSetting(it) == part })
    val collected = buildMap<BackupPart, JsonElement> {
        for (part in parts) when (part) {
            BackupPart.PORTALS -> {
                val library = storage.read() ?: continue
                put(part, JsonObject().apply {
                    addProperty("active", library.activeId)
                    add("portals", JsonArray().apply {
                        library.sources.forEach { source ->
                            add(JsonObject().apply {
                                addProperty("address", source.address)
                                addProperty("mac", source.mac)
                                addProperty("enabled", source.enabled || source.autoDisabled)
                                source.subscriptionExpiry?.let { addProperty("expiry", it.toString()) }
                            })
                        }
                    })
                })
            }
            BackupPart.SETTINGS, BackupPart.CATEGORIES, BackupPart.LOCKS ->
                put(part, JsonObject().apply { add(MAIN_SETTINGS, settingsOf(part)) })
            BackupPart.WATCH_DATA -> put(part, JsonObject().apply {
                add(MAIN_SETTINGS, settingsOf(part))
                WATCH_FILES.forEach { file ->
                    add(file, preferencesToJson(context.getSharedPreferences(file, Context.MODE_PRIVATE).all))
                }
            })
            BackupPart.ACCOUNTS -> put(part, JsonObject().apply {
                storage.readTraktApp()?.let { app ->
                    add("traktApp", JsonObject().apply {
                        addProperty("clientId", app.clientId)
                        app.clientSecret?.let { addProperty("clientSecret", it) }
                    })
                }
                for ((name, kids) in listOf("trakt" to false, "traktKids" to true)) {
                    val session = storage.readTraktSession(kids) ?: continue
                    add(name, JsonObject().apply {
                        addProperty("clientId", session.clientId)
                        addProperty("accessToken", session.accessToken)
                        addProperty("refreshToken", session.refreshToken)
                        addProperty("expiresAtMillis", session.expiresAtMillis)
                        addProperty("username", session.username)
                        session.clientSecret?.let { addProperty("clientSecret", it) }
                    })
                }
                storage.readMetadataToken()?.let { addProperty("tmdb", it) }
            })
            BackupPart.EPG -> put(part, JsonObject().apply { add("sources", jsonStrings(storage.readEpgSources())) })
        }
    }
    val created = java.time.LocalDateTime.now().withNano(0).toString()
    return withContext(Dispatchers.Default) { encodeBackup(BackupDocument(created, appVersion(context), collected)) }
}

/**
 * Writes the chosen parts back. Settings of a restored part replace the ones on this device;
 * portals are added to the existing ones (their channels load with the next refresh).
 */
internal suspend fun restoreBackup(context: Context, storage: PortalStorage, document: BackupDocument, parts: Set<BackupPart>) {
    for (part in parts) {
        val data = document.parts[part]?.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        when (part) {
            BackupPart.PORTALS -> {
                val existing = storage.read()
                val current = existing?.sources.orEmpty()
                val added = data.getAsJsonArray("portals")?.mapNotNull { element ->
                    val portal = element.asJsonObject
                    val address = portal.text("address")?.trim() ?: return@mapNotNull null
                    val mac = portal.text("mac")?.let(::normalizePortalMac) ?: return@mapNotNull null
                    if (!address.startsWith("http://") && !address.startsWith("https://") || mac.isBlank()) return@mapNotNull null
                    SavedPortal(
                        address, mac, emptyList(), emptyMap(), emptyMap(),
                        subscriptionExpiry = portal.text("expiry")?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                        enabled = portal.get("enabled")?.asBoolean ?: true
                    )
                }.orEmpty().filter { candidate -> current.none { it.identity == candidate.identity } }
                val merged = current + added
                if (merged.isNotEmpty()) {
                    val active = existing?.activeId?.takeIf { id -> merged.any { it.identity == id } }
                        ?: data.text("active")?.takeIf { id -> merged.any { it.identity == id } }
                        ?: merged.first().identity
                    storage.write(SavedPortals(merged, active))
                }
            }
            BackupPart.SETTINGS, BackupPart.CATEGORIES, BackupPart.LOCKS, BackupPart.WATCH_DATA -> {
                data.getAsJsonObject(MAIN_SETTINGS)?.let { values ->
                    val preferences = context.getSharedPreferences(MAIN_SETTINGS, Context.MODE_PRIVATE)
                    val old = preferences.all.keys.filter { backupPartOfSetting(it) == part }
                    preferences.edit(commit = true) {
                        old.forEach { remove(it) }
                        putAll(this, preferencesFromJson(values).filterKeys { backupPartOfSetting(it) == part })
                    }
                }
                if (part == BackupPart.WATCH_DATA) WATCH_FILES.forEach { file ->
                    val values = data.getAsJsonObject(file) ?: return@forEach
                    context.getSharedPreferences(file, Context.MODE_PRIVATE).edit(commit = true) {
                        putAll(this, preferencesFromJson(values))
                    }
                }
            }
            BackupPart.ACCOUNTS -> {
                data.getAsJsonObject("traktApp")?.let { app ->
                    app.text("clientId")?.let { storage.writeTraktApp(TraktApp(it, app.text("clientSecret"))) }
                }
                for ((name, kids) in listOf("trakt" to false, "traktKids" to true)) {
                    val session = data.getAsJsonObject(name) ?: continue
                    storage.writeTraktSession(TraktSession(
                        session.text("clientId") ?: continue, session.text("accessToken") ?: continue,
                        session.text("refreshToken") ?: continue, session.get("expiresAtMillis")?.asLong ?: 0L,
                        session.text("username") ?: "Trakt", session.text("clientSecret")
                    ), kids)
                }
                data.text("tmdb")?.takeIf { it.isNotBlank() }?.let { storage.writeMetadataToken(it) }
            }
            BackupPart.EPG -> data.getAsJsonArray("sources")?.let { sources ->
                storage.writeEpgSources(sources.map { it.asString }.filter { it.isNotBlank() })
            }
        }
    }
}

private fun putAll(editor: android.content.SharedPreferences.Editor, values: Map<String, Any>) {
    for ((key, value) in values) when (value) {
        is String -> editor.putString(key, value)
        is Boolean -> editor.putBoolean(key, value)
        is Int -> editor.putInt(key, value)
        is Long -> editor.putLong(key, value)
        is Float -> editor.putFloat(key, value)
        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
    }
}

internal fun backupFileName(): String =
    BACKUP_FILE_PREFIX + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")) + ".swbackup"

/** Saves into the public "Download" folder (Android 10+); needs no permission or folder picker. */
internal fun saveBackupToDownloads(context: Context, bytes: ByteArray): String {
    // The shared "Download" folder without a permission exists from Android 10 on.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throw IllegalStateException("Android 10 needed")
    val name = backupFileName()
    val resolver = context.contentResolver
    val values = android.content.ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)
        put(MediaStore.Downloads.MIME_TYPE, BACKUP_MIME)
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("no target")
    try {
        resolver.openOutputStream(target)?.use { it.write(bytes) } ?: error("no stream")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(target, values, null, null)
    } catch (error: Exception) {
        runCatching { resolver.delete(target, null, null) }
        throw error
    }
    return name
}

internal data class StoredBackup(val name: String, val uri: android.net.Uri)

/** Backups this app saved in "Download", newest first (files of other apps are not visible). */
internal fun backupsInDownloads(context: Context): List<StoredBackup> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    return runCatching {
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME),
            "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?", arrayOf("$BACKUP_FILE_PREFIX%"),
            "${MediaStore.Downloads.DATE_ADDED} DESC"
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(StoredBackup(
                    cursor.getString(1), android.content.ContentUris.withAppendedId(collection, cursor.getLong(0))
                ))
            }
        }.orEmpty()
    }.getOrDefault(emptyList())
}

/** Reads a backup, or a portal file of the earlier "Portale sichern" export. */
internal fun readBackup(context: Context, uri: android.net.Uri): BackupDocument {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("no stream")
    return if (isBackupFile(bytes)) decodeBackup(bytes)
    else legacyPortalBackup(bytes.toString(Charsets.UTF_8)) ?: error("not a backup")
}

/** Starts the app anew so every screen reads the restored data. */
internal fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    android.os.Process.killProcess(android.os.Process.myPid())
}
