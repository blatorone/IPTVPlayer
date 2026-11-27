package com.iptvplayer.app

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** What a backup can hold; the user picks the parts on export and on import. */
internal enum class BackupPart(val label: Int) {
    PORTALS(R.string.backup_part_portals),
    SETTINGS(R.string.backup_part_settings),
    CATEGORIES(R.string.backup_part_categories),
    LOCKS(R.string.backup_part_locks),
    ACCOUNTS(R.string.backup_part_accounts),
    EPG(R.string.backup_part_epg),
    WATCH_DATA(R.string.backup_part_watch)
}

/** Values that only make sense on the device that wrote them. */
private val deviceOnlySettings = setOf("update_window_shown", "active_portal_cache")
private val watchDataSettings = setOf("watchlist", "favorites", "search_history")

/** Which part a key of the main settings file ("catalog_visibility") belongs to; null = not backed up. */
internal fun backupPartOfSetting(key: String): BackupPart? = when {
    key in deviceOnlySettings -> null
    key.removePrefix("kids:") in watchDataSettings -> BackupPart.WATCH_DATA
    key == "hidden" || key == "countries" || key == "countryOrder" ||
        key.startsWith("category_order_") || key.startsWith("last_vod_category_") -> BackupPart.CATEGORIES
    key.startsWith("parental_") || key.startsWith("settings_pin") || key.startsWith("settings_lock") ||
        key.startsWith("kids_") || key == "hidden_areas" || key == "start_profile" || key.endsWith("_salt") -> BackupPart.LOCKS
    else -> BackupPart.SETTINGS
}

/** Preference values with their type, so they come back exactly (Int stays Int, sets stay sets). */
internal fun preferencesToJson(values: Map<String, *>): JsonObject = JsonObject().apply {
    for ((key, value) in values.toSortedMap()) {
        val entry = JsonObject()
        when (value) {
            is String -> { entry.addProperty("t", "s"); entry.addProperty("v", value) }
            is Boolean -> { entry.addProperty("t", "b"); entry.addProperty("v", value) }
            is Int -> { entry.addProperty("t", "i"); entry.addProperty("v", value) }
            is Long -> { entry.addProperty("t", "l"); entry.addProperty("v", value) }
            is Float -> { entry.addProperty("t", "f"); entry.addProperty("v", value) }
            is Set<*> -> {
                entry.addProperty("t", "set")
                entry.add("v", JsonArray().apply { value.filterIsInstance<String>().sorted().forEach(::add) })
            }
            else -> continue
        }
        add(key, entry)
    }
}

internal fun preferencesFromJson(json: JsonObject): Map<String, Any> = buildMap {
    for ((key, element) in json.entrySet()) {
        val entry = element.takeIf { it.isJsonObject }?.asJsonObject ?: continue
        val value = entry.get("v") ?: continue
        put(key, when (entry.get("t")?.asString) {
            "s" -> value.asString
            "b" -> value.asBoolean
            "i" -> value.asInt
            "l" -> value.asLong
            "f" -> value.asFloat
            "set" -> value.asJsonArray.map { it.asString }.toSet()
            else -> continue
        })
    }
}

/** A decoded backup: when and by which version it was made, and the parts it holds. */
internal class BackupDocument(
    val createdAt: String,
    val appVersion: String,
    val parts: Map<BackupPart, JsonElement>
)

private const val BACKUP_FORMAT = "streamwatch-backup"
private val BACKUP_MAGIC = "SWB1".toByteArray(Charsets.US_ASCII)
private const val NONCE_BYTES = 12

/**
 * The file is compressed and encrypted (AES-GCM). There is no password: the key is part of the
 * app, so a backup opens again after a new install or on another device. It keeps the content
 * from being read as plain text; it is not a protection against someone analysing the app.
 */
private val backupKey: SecretKeySpec by lazy {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("StreamWatch backup v1 · 7f3c9a1e-5b42-4d8e-a6f0-2c9e81d4b7a5".toByteArray(Charsets.UTF_8))
    SecretKeySpec(digest, "AES")
}

/*
 * With a password ("SWB2"): the key comes from the password (PBKDF2-HMAC-SHA256, own random salt
 * per file, the number of rounds stored in the file). Without the password the file cannot be
 * opened, not even by the app; the password is not stored anywhere.
 */
private val PROTECTED_MAGIC = "SWB2".toByteArray(Charsets.US_ASCII)
private const val SALT_BYTES = 16
private const val PASSWORD_ROUNDS = 120_000
internal const val BACKUP_PASSWORD_MIN = 8

/** The file is protected with a password that is missing or wrong. */
internal class BackupPasswordException(val missing: Boolean) : Exception()

private fun passwordKey(password: CharArray, salt: ByteArray, rounds: Int): SecretKeySpec {
    val spec = javax.crypto.spec.PBEKeySpec(password, salt, rounds, 256)
    try {
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(key, "AES")
    } finally {
        spec.clearPassword()
    }
}

private fun pack(document: BackupDocument): ByteArray {
    val json = JsonObject().apply {
        addProperty("format", BACKUP_FORMAT)
        addProperty("version", 1)
        addProperty("createdAt", document.createdAt)
        addProperty("appVersion", document.appVersion)
        add("parts", JsonObject().apply { document.parts.forEach { (part, value) -> add(part.name, value) } })
    }
    return ByteArrayOutputStream().also { out ->
        GZIPOutputStream(out).use { it.write(json.toString().toByteArray(Charsets.UTF_8)) }
    }.toByteArray()
}

private fun unpack(packed: ByteArray): BackupDocument =
    parseBackupJson(GZIPInputStream(ByteArrayInputStream(packed)).use { it.readBytes() }.toString(Charsets.UTF_8))

/** [password] null: the file opens without one (fixed app key, as before). */
internal fun encodeBackup(document: BackupDocument, password: CharArray?): ByteArray {
    if (password == null) return encodeBackup(document)
    val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
    val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
    val header = PROTECTED_MAGIC + salt + java.nio.ByteBuffer.allocate(4).putInt(PASSWORD_ROUNDS).array() + nonce
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, passwordKey(password, salt, PASSWORD_ROUNDS), GCMParameterSpec(128, nonce))
    cipher.updateAAD(header)
    return header + cipher.doFinal(pack(document))
}

internal fun isProtectedBackup(bytes: ByteArray): Boolean =
    bytes.size > PROTECTED_MAGIC.size + SALT_BYTES + 4 + NONCE_BYTES &&
        bytes.copyOfRange(0, PROTECTED_MAGIC.size).contentEquals(PROTECTED_MAGIC)

/** Opens a backup; a protected one needs [password] (else [BackupPasswordException]). */
internal fun decodeBackup(bytes: ByteArray, password: CharArray?): BackupDocument {
    if (!isProtectedBackup(bytes)) return decodeBackup(bytes)
    if (password == null) throw BackupPasswordException(missing = true)
    var at = PROTECTED_MAGIC.size
    val salt = bytes.copyOfRange(at, at + SALT_BYTES).also { at += SALT_BYTES }
    val rounds = java.nio.ByteBuffer.wrap(bytes, at, 4).int.also { at += 4 }
    require(rounds in 10_000..10_000_000)
    val nonce = bytes.copyOfRange(at, at + NONCE_BYTES).also { at += NONCE_BYTES }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, passwordKey(password, salt, rounds), GCMParameterSpec(128, nonce))
    cipher.updateAAD(bytes, 0, at)
    val packed = try {
        cipher.doFinal(bytes, at, bytes.size - at)
    } catch (_: javax.crypto.AEADBadTagException) {
        throw BackupPasswordException(missing = false)
    }
    return unpack(packed)
}

internal fun encodeBackup(document: BackupDocument): ByteArray {
    val packed = pack(document)
    val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, backupKey, GCMParameterSpec(128, nonce))
    cipher.updateAAD(BACKUP_MAGIC)
    return BACKUP_MAGIC + nonce + cipher.doFinal(packed)
}

internal fun isBackupFile(bytes: ByteArray): Boolean = isProtectedBackup(bytes) ||
    bytes.size > BACKUP_MAGIC.size + NONCE_BYTES && bytes.copyOfRange(0, BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)

internal fun decodeBackup(bytes: ByteArray): BackupDocument {
    if (isProtectedBackup(bytes)) throw BackupPasswordException(missing = true)
    require(isBackupFile(bytes))
    val nonce = bytes.copyOfRange(BACKUP_MAGIC.size, BACKUP_MAGIC.size + NONCE_BYTES)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, backupKey, GCMParameterSpec(128, nonce))
    cipher.updateAAD(BACKUP_MAGIC)
    return unpack(cipher.doFinal(bytes, BACKUP_MAGIC.size + NONCE_BYTES, bytes.size - BACKUP_MAGIC.size - NONCE_BYTES))
}

private fun parseBackupJson(text: String): BackupDocument {
    val root = JsonParser.parseString(text).asJsonObject
    require(root.get("format")?.asString == BACKUP_FORMAT && root.get("version")?.asInt == 1)
    val parts = root.getAsJsonObject("parts")
    return BackupDocument(
        root.get("createdAt")?.asString.orEmpty(),
        root.get("appVersion")?.asString.orEmpty(),
        BackupPart.entries.mapNotNull { part -> parts.get(part.name)?.let { part to it } }.toMap()
    )
}

/** A portal file from the earlier "Portale sichern" export opens as a backup with only portals. */
internal fun legacyPortalBackup(text: String): BackupDocument? = runCatching {
    val root = JsonParser.parseString(text).asJsonObject
    require(root.text("format") == "streamwatch-portals" && root.get("version")?.asInt == 1)
    val portals = root.getAsJsonArray("portals").mapNotNull { element ->
        val portal = element.asJsonObject
        val address = portal.text("address")?.trim() ?: return@mapNotNull null
        val mac = portal.text("mac")?.trim() ?: return@mapNotNull null
        if (!address.startsWith("http://") && !address.startsWith("https://") || mac.isBlank()) return@mapNotNull null
        JsonObject().apply { addProperty("address", address); addProperty("mac", mac) }
    }
    require(portals.isNotEmpty())
    BackupDocument("", "", mapOf(BackupPart.PORTALS to JsonObject().apply {
        add("portals", JsonArray().apply { portals.forEach(::add) })
    }))
}.getOrNull()

internal fun JsonObject.text(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive }?.asString

internal fun jsonStrings(values: List<String>): JsonArray = JsonArray().apply { values.forEach { add(JsonPrimitive(it)) } }
