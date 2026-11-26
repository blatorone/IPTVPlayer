package com.iptvplayer.app

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/** The main areas that can be hidden ("Einstellungsschutz"); at least one stays visible. */
internal val MAIN_AREAS = listOf("LIVE", "MOVIES", "SERIES")

/** Age levels of the kids mode (German FSK steps). */
internal val KIDS_AGES = listOf(0, 6, 12, 16, 18)

// Category names of IPTV portals come in many languages (independent of the app language).
private val ADULT_CATEGORY = Regex(
    """(?iu)(erotik|erotic|érotique|erótic|erotyk|erotiek|erotika|erotikus|ερωτικ|эротик|еротик|adult|xxx|porn|порн|\bsex\b|\+\s?18\b|\b18\s?\+|\b18\s?plus\b|for adults|nur für erwachsene|yetişkin|dla dorosłych|para adultos|pour adultes|per adulti|pentru adulți|za odrasle|за одрасле|për të rritur|для взрослых|для дорослих|за възрастни|felnőtt|pro dospělé|pre dospelých|voor volwassenen|för vuxna|for voksne|aikuis|ενηλίκων|للكبار|للبالغين|למבוגרים|成人)"""
)
private val KIDS_CATEGORY = Regex(
    """(?iu)(kinder|\bkids?\b|family|familie|famille|familia|família|famiglia|rodzin|cartoon|\btoons?\b|junior|kika|nick(elodeon)?|disney|\bbaby\b|jugend|kinderfilm|zeichentrick|çocuk|dzieci|bajki|детск|дитяч|мульт|enfants|jeunesse|niños|infantil|bambini|ragazzi|copii|desene|bdjecab|bdecab|dječ|dečij|fëmij|vizatimor|kinderen|bbarnb|lapset|gyerek|dětsk|bdetib|παιδ|أطفال|كرتون|ילדים|儿童|少儿|动画)"""
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

    // ---- Einstellungsschutz: what someone without the settings PIN may change ----
    /** The settings use the Kindersicherung PIN (true) or their own PIN (settings_pin_hash). */
    var settingsPinShared by mutableStateOf(
        preferences?.getBoolean("settings_pin_shared", false) ?: false ||
            // Earlier versions protected the settings with the Kindersicherung PIN only.
            (preferences?.contains("settings_pin_shared") == false && protectSettings && hasPin)
    )
        private set
    var ownSettingsPin by mutableStateOf(preferences?.getString("settings_pin_hash", null) != null)
        private set
    val hasSettingsPin: Boolean get() = ownSettingsPin || settingsPinShared && hasPin
    var lockPortals by mutableStateOf(preferences?.getBoolean("settings_lock_portals", false) ?: false)
        private set
    var lockCategories by mutableStateOf(preferences?.getBoolean("settings_lock_categories", false) ?: false)
        private set
    /** Main areas (LIVE, MOVIES, SERIES) hidden from the menu; one always stays. */
    var hiddenAreas by mutableStateOf(preferences?.getStringSet("hidden_areas", null)?.toSet().orEmpty())
        private set
    /** Settings PIN entered: protected settings stay open until the settings are left. */
    var settingsUnlocked by mutableStateOf(false)

    fun checkSettingsPin(pin: String): Boolean =
        if (ownSettingsPin) preferences?.getString("settings_pin_hash", null) == hash(pin, "settings_pin_salt")
        else settingsPinShared && checkPin(pin)

    val settingsPinLength: Int? get() =
        if (ownSettingsPin) preferences?.getInt("settings_pin_length", 0)?.takeIf { it >= 4 } else pinLength

    /** Own settings PIN ([pin]) or the Kindersicherung PIN ([pin] null, shared). */
    fun setSettingsPin(pin: String?) {
        val editor = preferences?.edit()
        if (pin == null) {
            editor?.remove("settings_pin_hash")?.putBoolean("settings_pin_shared", true)
        } else {
            editor?.putString("settings_pin_hash", hash(pin, "settings_pin_salt"))?.putInt("settings_pin_length", pin.length)
                ?.putBoolean("settings_pin_shared", false)
        }
        editor?.apply()
        ownSettingsPin = pin != null
        settingsPinShared = pin == null
    }

    /** Removes the settings PIN and with it every settings restriction. */
    fun removeSettingsPin() {
        preferences?.edit()?.remove("settings_pin_hash")?.putBoolean("settings_pin_shared", false)
            ?.putBoolean("parental_protect_settings", false)?.putBoolean("settings_lock_portals", false)
            ?.putBoolean("settings_lock_categories", false)?.apply()
        ownSettingsPin = false
        settingsPinShared = false
        protectSettings = false
        lockPortals = false
        lockCategories = false
    }

    fun updateLockPortals(value: Boolean) {
        lockPortals = value
        preferences?.edit()?.putBoolean("settings_lock_portals", value)?.apply()
    }

    fun updateLockCategories(value: Boolean) {
        lockCategories = value
        preferences?.edit()?.putBoolean("settings_lock_categories", value)?.apply()
    }

    /** Hides or shows a main area; the last visible one cannot be hidden. */
    fun setAreaHidden(area: String, hidden: Boolean): Boolean {
        val next = if (hidden) hiddenAreas + area else hiddenAreas - area
        if (MAIN_AREAS.all { it in next }) return false
        hiddenAreas = next
        preferences?.edit()?.putStringSet("hidden_areas", next)?.apply()
        return true
    }

    /** Whether changing something guarded by [flag] needs the settings PIN right now. */
    fun needsSettingsPin(flag: Boolean): Boolean = flag && hasSettingsPin && !settingsUnlocked

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

    /** Length of the stored PINs: entering one is confirmed as soon as it is reached. */
    val pinLength: Int? get() = preferences?.getInt("parental_pin_length", 0)?.takeIf { it >= 4 }
    val kidsExitPinLength: Int? get() =
        if (separateKidsPin) preferences?.getInt("kids_pin_length", 0)?.takeIf { it >= 4 } else pinLength

    fun setPin(pin: String) {
        preferences?.edit()?.putString("parental_pin_hash", hash(pin, "parental_pin_salt"))
            ?.putInt("parental_pin_length", pin.length)?.apply()
        hasPin = true
    }

    fun setKidsPin(pin: String?) {
        if (pin == null) preferences?.edit()?.remove("kids_pin_hash")?.apply()
        else preferences?.edit()?.putString("kids_pin_hash", hash(pin, "kids_pin_salt"))
            ?.putInt("kids_pin_length", pin.length)?.apply()
        separateKidsPin = pin != null
    }

    fun removePin() {
        // Settings that share this PIN lose their protection with it; an own settings PIN stays.
        if (settingsPinShared && !ownSettingsPin) removeSettingsPin()
        preferences?.edit()?.remove("parental_pin_hash")?.remove("kids_pin_hash")
            ?.putBoolean("kids_mode", false)?.apply()
        hasPin = false
        separateKidsPin = false
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
    length: Int? = null,
    onPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    // A new PIN first asks for its length; an existing one is confirmed when its length is reached.
    var chosenLength by remember { mutableStateOf(if (confirmTwice) null else length) }
    var choosing by remember { mutableStateOf(confirmTwice) }
    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var phoneEntry by remember { mutableStateOf(false) }
    val wrong = stringResource(R.string.pin_wrong)
    val mismatch = stringResource(R.string.pin_mismatch)
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(choosing) {
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
    fun type(digit: String) {
        val limit = chosenLength ?: 12
        if (pin.length >= limit) return
        pin += digit
        if (pin.length == chosenLength) submit()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                    // The number keys of a remote (or keyboard) type the PIN directly.
                    if (choosing || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = event.nativeKeyEvent.keyCode
                    when (code) {
                        in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> {
                            type((code - android.view.KeyEvent.KEYCODE_0).toString()); true
                        }
                        in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> {
                            type((code - android.view.KeyEvent.KEYCODE_NUMPAD_0).toString()); true
                        }
                        android.view.KeyEvent.KEYCODE_DEL -> { pin = pin.dropLast(1); true }
                        else -> false
                    }
                },
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                message?.let { Text(it, textAlign = TextAlign.Center) }
                // A new PIN is typed twice on the phone and compared there.
                if (phoneEntry) PhonePinEntry(confirmTwice, onPin)
                if (choosing) {
                    Text(stringResource(R.string.pin_length_question), style = MaterialTheme.typography.labelLarge)
                    listOf(4, 6, 8, null).forEachIndexed { index, option ->
                        PinKey(
                            option?.let { stringResource(R.string.pin_length_digits, it) } ?: stringResource(R.string.pin_length_custom),
                            Modifier.width(220.dp).then(if (index == 0) Modifier.focusRequester(firstKey) else Modifier)
                        ) {
                            chosenLength = option
                            choosing = false
                        }
                    }
                    return@Column
                }
                if (confirmTwice) Text(
                    stringResource(if (first == null) R.string.pin_enter_new else R.string.pin_repeat),
                    style = MaterialTheme.typography.labelLarge
                )
                val slots = chosenLength ?: maxOf(4, pin.length)
                Text(
                    (0 until slots).joinToString(" ") { if (it < pin.length) "●" else "–" },
                    style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                val confirmKey = if (chosenLength == null) "OK" else ""
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", confirmKey)).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            if (key.isEmpty()) Box(Modifier.width(72.dp)) else PinKey(
                                key,
                                Modifier.width(72.dp).then(if (key == "1") Modifier.focusRequester(firstKey) else Modifier),
                                enabled = key != "OK" || pin.length >= 4
                            ) {
                                when (key) {
                                    "⌫" -> pin = pin.dropLast(1)
                                    "OK" -> submit()
                                    else -> type(key)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Typing the PIN on the phone: nobody next to the TV sees it (only when checking a PIN).
            TextButton(onClick = { phoneEntry = !phoneEntry }) {
                Text(stringResource(if (phoneEntry) R.string.pin_phone_close else R.string.pin_phone))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

/** A PIN-only phone session: its QR code, and every PIN typed there is checked with [onPin]. */
@Composable
private fun PhonePinEntry(newPin: Boolean, onPin: (String) -> Boolean) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val wrongMessage = stringResource(R.string.pin_wrong)
    val ended = stringResource(R.string.remote_msg_ended)
    val failed = stringResource(R.string.remote_msg_change_failed)
    var attempts by remember { mutableStateOf(0) }
    val session = remember {
        var self: RemoteSession? = null
        val handler = object : RemoteHandler {
            override fun remoteState(): RemoteState = error("PIN-only session")
            override fun applyRemote(command: RemoteCommand): RemoteReply {
                val entry = command as? RemoteCommand.PinEntry ?: return RemoteReply(false)
                // A few tries per QR code; then the code is worthless.
                if (++attempts > PHONE_PIN_ATTEMPTS) { self?.end(); return RemoteReply(false, wrongMessage) }
                // On success the PIN window closes; leaving it ends the session after this answer.
                return if (onPin(entry.pin)) RemoteReply(true) else RemoteReply(false, wrongMessage)
            }
        }
        RemoteSession(scope, handler, ended, failed, pinMode = if (newPin) "pinNew" else "pin").also { self = it; it.start() }
    }
    androidx.compose.runtime.DisposableEffect(session) { onDispose { session.end() } }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        QrCode(session.pageUrl, Modifier.width(110.dp).height(110.dp))
        Text(stringResource(R.string.pin_phone_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(200.dp))
    }
}

private const val PHONE_PIN_ATTEMPTS = 5

/** One key of the PIN pad: label centred, focus frame like the other buttons. */
@Composable
private fun PinKey(label: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusProperties { canFocus = enabled }
            .clickable(enabled = enabled, onClick = onClick),
        color = if (focused) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        contentColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, accentBrush()) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
    }
}

/** "Kindersicherung" page of the settings. */
@Composable
internal fun ParentalSettings(
    parental: ParentalControls, request: (PinRequest) -> Unit, hasKidsAccount: Boolean,
    askSharedPin: (onShared: () -> Unit, onOwn: () -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun withPin(action: () -> Unit) = request(
        PinRequest(context.getString(R.string.pin_enter), null, check = parental::checkPin, length = parental.pinLength) { action() }
    )
    fun newPin(title: Int, onPin: (String) -> Unit) = request(
        PinRequest(context.getString(title), null, newPin = true, check = { true }, onSuccess = onPin)
    )
    if (!parental.hasPin) {
        Text(stringResource(R.string.pin_status_none))
        SettingRow(stringResource(R.string.pin_set)) {
            // With an own settings PIN, the Kindersicherung can use the same one.
            if (parental.ownSettingsPin) askSharedPin({
                request(PinRequest(context.getString(R.string.parental_pin_confirm_shared), null,
                    check = parental::checkSettingsPin, length = parental.settingsPinLength) { pin ->
                    parental.setPin(pin)
                    parental.setSettingsPin(null)
                })
            }) { newPin(R.string.pin_set, parental::setPin) }
            else newPin(R.string.pin_set, parental::setPin)
        }
        Text(stringResource(R.string.kids_mode_hint), style = MaterialTheme.typography.bodySmall)
        return
    }
    if (parental.kidsMode) Text(stringResource(R.string.kids_mode_active), color = MaterialTheme.colorScheme.primary)
    SettingRow(stringResource(R.string.pin_change)) { withPin { newPin(R.string.pin_change, parental::setPin) } }
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
    // With a kids account, kids mode is switched by changing the account ("Abmelden").
    if (hasKidsAccount) Unit
    else if (parental.kidsMode) SettingRow(stringResource(R.string.kids_mode_exit)) {
        request(PinRequest(context.getString(R.string.kids_mode_exit), null, check = parental::checkKidsPin, length = parental.kidsExitPinLength) {
            parental.updateKidsMode(false)
        })
    } else SettingRow(stringResource(R.string.kids_mode_start)) { parental.updateKidsMode(true) }
    Text(stringResource(R.string.kids_mode_hint), style = MaterialTheme.typography.bodySmall)
    SettingRow(stringResource(R.string.pin_remove)) { withPin { parental.removePin() } }
}

/** "Einstellungsschutz" page: settings PIN (own or shared) and what it protects. */
@Composable
internal fun SettingsProtection(
    parental: ParentalControls, request: (PinRequest) -> Unit, askSharedPin: (onShared: () -> Unit, onOwn: () -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun withSettingsPin(action: () -> Unit) {
        if (parental.settingsUnlocked || !parental.hasSettingsPin) action()
        else request(PinRequest(context.getString(R.string.pin_enter), null, check = parental::checkSettingsPin, length = parental.settingsPinLength) {
            parental.settingsUnlocked = true
            action()
        })
    }
    fun newOwnPin() = request(PinRequest(context.getString(R.string.settings_pin_set), null, newPin = true, check = { true }) {
        parental.setSettingsPin(it)
        parental.settingsUnlocked = true
    })
    fun choosePin() {
        // With a Kindersicherung PIN the same one can be used, or an own one for the settings.
        if (parental.hasPin) askSharedPin({
            request(PinRequest(context.getString(R.string.settings_pin_confirm_shared), null, check = parental::checkPin, length = parental.pinLength) {
                parental.setSettingsPin(null)
                parental.settingsUnlocked = true
            })
        }, ::newOwnPin) else newOwnPin()
    }
    Text(stringResource(R.string.settings_protection_hint), style = MaterialTheme.typography.bodySmall)
    if (!parental.hasSettingsPin) {
        Text(stringResource(R.string.settings_pin_none))
        SettingRow(stringResource(R.string.settings_pin_set)) { choosePin() }
    } else {
        Text(stringResource(if (parental.ownSettingsPin) R.string.settings_pin_own else R.string.settings_pin_shared_status))
        SettingRow(stringResource(R.string.settings_pin_change)) { withSettingsPin { choosePin() } }
        SettingRow(stringResource(R.string.parental_protect_settings), selected = parental.protectSettings) {
            withSettingsPin { parental.updateProtectSettings(!parental.protectSettings) }
        }
        SettingRow(stringResource(R.string.settings_lock_portals), selected = parental.lockPortals) {
            withSettingsPin { parental.updateLockPortals(!parental.lockPortals) }
        }
        SettingRow(stringResource(R.string.settings_lock_categories), selected = parental.lockCategories) {
            withSettingsPin { parental.updateLockCategories(!parental.lockCategories) }
        }
    }
    Text(stringResource(R.string.settings_areas), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    var lastAreaMessage by remember { mutableStateOf(false) }
    listOf("LIVE" to R.string.destination_live, "MOVIES" to R.string.destination_movies, "SERIES" to R.string.destination_series)
        .forEach { (area, label) ->
            val shown = area !in parental.hiddenAreas
            SettingRow(stringResource(R.string.settings_area_shown, stringResource(label)), selected = shown) {
                withSettingsPin { lastAreaMessage = !parental.setAreaHidden(area, shown) }
            }
        }
    if (lastAreaMessage) Text(stringResource(R.string.settings_area_last), color = MaterialTheme.colorScheme.error)
    if (parental.hasSettingsPin) SettingRow(stringResource(R.string.settings_pin_remove)) {
        withSettingsPin { parental.removeSettingsPin() }
    }
}
