package com.iptvplayer.app

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Minimal in-memory SharedPreferences for the PIN logic. */
private class MemoryPreferences : SharedPreferences {
    val values = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String, defValue: String?) = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = values[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String, defValue: Int) = values[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
    override fun contains(key: String) = key in values
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        val changes = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { changes[key] = values?.toMutableSet() }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putLong(key: String, value: Long) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun clear() = apply { values.keys.forEach { changes[it] = null } }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        }
    }
}

class SettingsProtectionTest {
    @Test
    fun ownSettingsPinIsSeparateFromTheKindersicherung() {
        val parental = ParentalControls(MemoryPreferences())
        parental.setPin("1111")
        parental.setSettingsPin("2222")
        assertTrue(parental.ownSettingsPin)
        assertTrue(parental.checkSettingsPin("2222"))
        assertFalse(parental.checkSettingsPin("1111"))
        // Removing the Kindersicherung PIN keeps an own settings PIN.
        parental.removePin()
        assertTrue(parental.hasSettingsPin)
    }

    @Test
    fun sharedPinFollowsTheKindersicherung() {
        val parental = ParentalControls(MemoryPreferences())
        parental.setPin("1234")
        parental.setSettingsPin(null)
        assertTrue(parental.checkSettingsPin("1234"))
        parental.updateLockPortals(true)
        assertTrue(parental.needsSettingsPin(parental.lockPortals))
        parental.settingsUnlocked = true
        assertFalse(parental.needsSettingsPin(parental.lockPortals))
        // Without the shared PIN nothing stays locked.
        parental.removePin()
        assertFalse(parental.hasSettingsPin)
        assertFalse(parental.lockPortals)
    }

    @Test
    fun earlierProtectedSettingsKeepUsingTheKindersicherungPin() {
        val preferences = MemoryPreferences()
        ParentalControls(preferences).apply { setPin("4321"); updateProtectSettings(true) }
        val upgraded = ParentalControls(preferences)
        assertTrue(upgraded.hasSettingsPin)
        assertTrue(upgraded.checkSettingsPin("4321"))
    }

    @Test
    fun oneAreaAlwaysStaysVisible() {
        val parental = ParentalControls(MemoryPreferences())
        assertTrue(parental.setAreaHidden("MOVIES", true))
        assertTrue(parental.setAreaHidden("SERIES", true))
        assertFalse(parental.setAreaHidden("LIVE", true))
        assertEquals(setOf("MOVIES", "SERIES"), parental.hiddenAreas)
    }
}
