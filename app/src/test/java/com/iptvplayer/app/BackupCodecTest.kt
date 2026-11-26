package com.iptvplayer.app

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun backupComesBackUnchangedAndIsNotReadableAsText() {
        val parts = mapOf(
            BackupPart.EPG to JsonObject().apply { add("sources", jsonStrings(listOf("https://guide.example/epg.xml"))) },
            BackupPart.SETTINGS to JsonObject().apply { add("catalog_visibility", preferencesToJson(mapOf("key_sounds" to true))) }
        )
        val bytes = encodeBackup(BackupDocument("2026-11-26T15:00", "1.0.97", parts))
        assertTrue(isBackupFile(bytes))
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("guide.example"))
        val decoded = decodeBackup(bytes)
        assertEquals("2026-11-26T15:00", decoded.createdAt)
        assertEquals("1.0.97", decoded.appVersion)
        assertEquals(parts.keys, decoded.parts.keys)
        assertEquals(parts[BackupPart.EPG], decoded.parts[BackupPart.EPG])
    }

    @Test(expected = Exception::class)
    fun changedFileIsRejected() {
        val bytes = encodeBackup(BackupDocument("", "", emptyMap()))
        bytes[bytes.size - 1] = (bytes[bytes.size - 1] + 1).toByte()
        decodeBackup(bytes)
    }

    @Test
    fun preferenceTypesSurvive() {
        val values = mapOf<String, Any>(
            "text" to "a", "flag" to true, "count" to 7, "time" to 9_000_000_000L, "share" to 0.5f, "set" to setOf("x", "y")
        )
        assertEquals(values, preferencesFromJson(preferencesToJson(values)))
    }

    @Test
    fun settingsAreSortedIntoParts() {
        assertEquals(BackupPart.WATCH_DATA, backupPartOfSetting("kids:watchlist"))
        assertEquals(BackupPart.WATCH_DATA, backupPartOfSetting("search_history"))
        assertEquals(BackupPart.CATEGORIES, backupPartOfSetting("category_order_combined_LIVE"))
        assertEquals(BackupPart.CATEGORIES, backupPartOfSetting("hidden"))
        assertEquals(BackupPart.LOCKS, backupPartOfSetting("parental_pin_hash"))
        assertEquals(BackupPart.LOCKS, backupPartOfSetting("settings_pin_length"))
        assertEquals(BackupPart.LOCKS, backupPartOfSetting("kids_profile_name"))
        assertEquals(BackupPart.SETTINGS, backupPartOfSetting("key_sounds"))
        assertEquals(BackupPart.SETTINGS, backupPartOfSetting("start_destination"))
        assertNull(backupPartOfSetting("update_window_shown"))
    }

    @Test
    fun earlierPortalFileOpensAsPortalBackup() {
        val legacy = """{"format":"streamwatch-portals","version":1,"portals":[{"address":"http://portal.example/c/","mac":"00:1a:79:00:00:01"}]}"""
        val document = legacyPortalBackup(legacy)!!
        assertEquals(setOf(BackupPart.PORTALS), document.parts.keys)
        val portals = document.parts.getValue(BackupPart.PORTALS).asJsonObject.getAsJsonArray("portals")
        assertEquals("http://portal.example/c/", portals[0].asJsonObject.text("address"))
        assertNull(legacyPortalBackup("not json"))
    }
}
