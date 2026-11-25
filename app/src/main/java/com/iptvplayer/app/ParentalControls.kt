package com.iptvplayer.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/** Age levels of the kids mode (German FSK steps). */
internal val KIDS_AGES = listOf(0, 6, 12, 16, 18)

private val ADULT_CATEGORY = Regex(
    """(?i)(erotik|erotic|adult|xxx|porn|\bsex\b|\+18\b|\b18\+|for adults|nur für erwachsene)"""
)
private val KIDS_CATEGORY = Regex(
    """(?i)(kinder|\bkids?\b|family|familie|cartoon|\btoons?\b|junior|kika|nick(elodeon)?|disney|\bbaby\b|jugend|kinderfilm|zeichentrick)"""
)

internal fun isAdultCategory(name: String): Boolean = ADULT_CATEGORY.containsMatchIn(name)
internal fun isKidsCategory(name: String): Boolean = KIDS_CATEGORY.containsMatchIn(name) && !isAdultCategory(name)

/** German FSK / US ratings to an age; null when the label is unknown. */
internal fun ageFromRating(label: String?): Int? {
    val value = label?.trim()?.uppercase(Locale.ROOT)?.removePrefix("FSK")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    value.toIntOrNull()?.let { age -> return KIDS_AGES.firstOrNull { it >= age } ?: 18 }
    return when (value) {
        "G", "TV-Y", "TV-G" -> 0
        "PG", "TV-Y7", "TV-Y7-FV", "TV-PG" -> 6
        "PG-13", "TV-14" -> 12
        "R", "TV-MA" -> 16
        "NC-17" -> 18
        else -> null
    }
}

/**
 * PIN protection and kids mode. PINs are only kept as salted SHA-256 hashes on this device.
 * A missing PIN means: nothing is protected (kids mode requires one to be set first).
 */
internal class ParentalControls(private val preferences: SharedPreferences?) {
    var hasPin by mutableStateOf(preferences?.getString("parental_pin_hash", null) != null)
        private set
    var separateKidsPin by mutableStateOf(preferences?.getString("kids_pin_hash", null) != null)
        private set
    var protectSettings by mutableStateOf(preferences?.getBoolean("parental_protect_settings", false) ?: false)
        private set
    var lockAdult by mutableStateOf(preferences?.getBoolean("parental_lock_adult", true) ?: true)
        private set
    var kidsMode by mutableStateOf(preferences?.getBoolean("kids_mode", false) ?: false)
        private set
    var kidsAge by mutableStateOf(preferences?.getInt("kids_age", 6) ?: 6)
        private set
    var lockedCategories by mutableStateOf(preferences?.getStringSet("parental_locked_categories", null)?.toSet().orEmpty())
        private set

    /** Locked content stays visible until the app is closed after entering the PIN. */
    var unlocked by mutableStateOf(false)
    /** Titles released once with the PIN in kids mode during this session. */
    val allowedTitles = mutableSetOf<String>()

    private fun hash(pin: String, saltKey: String): String {
        val salt = preferences?.getString(saltKey, null) ?: ByteArray(16).also(SecureRandom()::nextBytes)
            .joinToString("") { "%02x".format(it) }.also { preferences?.edit()?.putString(saltKey, it)?.apply() }
        val digest = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun checkPin(pin: String): Boolean =
        hasPin && preferences?.getString("parental_pin_hash", null) == hash(pin, "parental_pin_salt")

    /** Leaving kids mode: the kids PIN when one is set, otherwise the main PIN. */
    fun checkKidsPin(pin: String): Boolean =
        if (separateKidsPin) preferences?.getString("kids_pin_hash", null) == hash(pin, "kids_pin_salt") else checkPin(pin)

    fun setPin(pin: String) {
        preferences?.edit()?.putString("parental_pin_hash", hash(pin, "parental_pin_salt"))?.apply()
        hasPin = true
    }

    fun setKidsPin(pin: String?) {
        if (pin == null) preferences?.edit()?.remove("kids_pin_hash")?.apply()
        else preferences?.edit()?.putString("kids_pin_hash", hash(pin, "kids_pin_salt"))?.apply()
        separateKidsPin = pin != null
    }

    fun removePin() {
        preferences?.edit()?.remove("parental_pin_hash")?.remove("kids_pin_hash")
            ?.putBoolean("parental_protect_settings", false)?.putBoolean("kids_mode", false)?.apply()
        hasPin = false
        separateKidsPin = false
        protectSettings = false
        kidsMode = false
    }

    fun updateProtectSettings(value: Boolean) {
        protectSettings = value
        preferences?.edit()?.putBoolean("parental_protect_settings", value)?.apply()
    }

    fun updateLockAdult(value: Boolean) {
        lockAdult = value
        preferences?.edit()?.putBoolean("parental_lock_adult", value)?.apply()
    }

    fun updateKidsMode(value: Boolean) {
        kidsMode = value
        unlocked = false
        allowedTitles.clear()
        preferences?.edit()?.putBoolean("kids_mode", value)?.apply()
    }

    fun updateKidsAge(value: Int) {
        kidsAge = value
        preferences?.edit()?.putInt("kids_age", value)?.apply()
    }

    fun toggleLocked(key: String) {
        lockedCategories = if (key in lockedCategories) lockedCategories - key else lockedCategories + key
        preferences?.edit()?.putStringSet("parental_locked_categories", lockedCategories)?.apply()
    }

    /** Whether a category may be listed right now. */
    fun allowsCategory(key: String, name: String, kidsFriendlyId: Boolean): Boolean {
        val adult = isAdultCategory(name)
        if (kidsMode) {
            if (adult || key in lockedCategories) return false
            return kidsAge >= 16 || kidsFriendlyId || isKidsCategory(name)
        }
        if (unlocked || !hasPin) return !(adult && lockAdult && hasPin)
        return !(adult && lockAdult) && key !in lockedCategories
    }

    /** Whether a title with this age rating may be opened in kids mode (unknown rating: no). */
    fun allowsAge(age: Int?): Boolean = !kidsMode || (age != null && age <= kidsAge)
}

/** PIN entry with a digit pad, usable with the remote's D-pad. */
@Composable
internal fun PinDialog(
    title: String,
    message: String?,
    confirmTwice: Boolean = false,
    onPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val wrong = stringResource(R.string.pin_wrong)
    val mismatch = stringResource(R.string.pin_mismatch)
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstKey.requestFocus() }
    }
    fun submit() {
        if (pin.length < 4) return
        if (confirmTwice && first == null) {
            first = pin
            pin = ""
            error = null
            return
        }
        if (confirmTwice && first != pin) {
            first = null
            pin = ""
            error = mismatch
            return
        }
        if (!onPin(pin)) {
            pin = ""
            error = wrong
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                message?.let { Text(it) }
                if (confirmTwice) Text(
                    stringResource(if (first == null) R.string.pin_enter_new else R.string.pin_repeat),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    if (pin.isEmpty()) "– – – –" else "●".repeat(pin.length),
                    Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            MenuButton(
                                key,
                                Modifier.width(72.dp).then(if (key == "1") Modifier.focusRequester(firstKey) else Modifier),
                                enabled = key != "OK" || pin.length >= 4
                            ) {
                                when (key) {
                                    "⌫" -> pin = pin.dropLast(1)
                                    "OK" -> submit()
                                    else -> if (pin.length < 8) pin += key
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

/** "Kindersicherung" page of the settings. */
@Composable
internal fun ParentalSettings(parental: ParentalControls, request: (PinRequest) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun withPin(action: () -> Unit) = request(
        PinRequest(context.getString(R.string.pin_enter), null, check = parental::checkPin) { action() }
    )
    fun newPin(title: Int, onPin: (String) -> Unit) = request(
        PinRequest(context.getString(title), null, newPin = true, check = { true }, onSuccess = onPin)
    )
    if (!parental.hasPin) {
        Text(stringResource(R.string.pin_status_none))
        SettingRow(stringResource(R.string.pin_set)) { newPin(R.string.pin_set, parental::setPin) }
        Text(stringResource(R.string.kids_mode_hint), style = MaterialTheme.typography.bodySmall)
        return
    }
    if (parental.kidsMode) Text(stringResource(R.string.kids_mode_active), color = MaterialTheme.colorScheme.primary)
    SettingRow(stringResource(R.string.pin_change)) { withPin { newPin(R.string.pin_change, parental::setPin) } }
    SettingRow(stringResource(R.string.parental_protect_settings), selected = parental.protectSettings) {
        parental.updateProtectSettings(!parental.protectSettings)
    }
    SettingRow(stringResource(R.string.parental_lock_adult), selected = parental.lockAdult) {
        if (parental.lockAdult) withPin { parental.updateLockAdult(false) } else parental.updateLockAdult(true)
    }
    if (parental.unlocked) SettingRow(stringResource(R.string.parental_relock)) { parental.unlocked = false }
    else SettingRow(stringResource(R.string.parental_unlock)) { withPin { parental.unlocked = true } }
    Text(stringResource(R.string.parental_locked_hint), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.kids_mode), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    SettingRow(stringResource(R.string.kids_age, stringResource(R.string.kids_age_value, parental.kidsAge))) {
        parental.updateKidsAge(KIDS_AGES[(KIDS_AGES.indexOf(parental.kidsAge) + 1) % KIDS_AGES.size])
    }
    SettingRow(stringResource(R.string.kids_separate_pin), selected = parental.separateKidsPin) {
        if (parental.separateKidsPin) withPin { parental.setKidsPin(null) }
        else newPin(R.string.kids_separate_pin) { parental.setKidsPin(it) }
    }
    if (parental.kidsMode) SettingRow(stringResource(R.string.kids_mode_exit)) {
        request(PinRequest(context.getString(R.string.kids_mode_exit), null, check = parental::checkKidsPin) {
            parental.updateKidsMode(false)
        })
    } else SettingRow(stringResource(R.string.kids_mode_start)) { parental.updateKidsMode(true) }
    Text(stringResource(R.string.kids_mode_hint), style = MaterialTheme.typography.bodySmall)
    SettingRow(stringResource(R.string.pin_remove)) { withPin { parental.removePin() } }
}
