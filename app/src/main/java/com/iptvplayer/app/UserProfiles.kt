package com.iptvplayer.app

import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Which account is signed in when the app starts (only asked once a kids account exists). */
internal enum class StartProfile(val label: Int) {
    ASK(R.string.start_profile_ask), ADMIN(R.string.start_profile_admin), KIDS(R.string.start_profile_kids)
}

/**
 * Accounts: the admin account always exists (also after updating, without any change for the
 * user); one kids account can be added. Both share portals and functions. While the kids account
 * is signed in, the kids mode of [ParentalControls] is on (age limit, no settings).
 */
internal class UserProfiles(private val preferences: SharedPreferences?) {
    var kidsName by mutableStateOf(preferences?.getString("kids_profile_name", null))
        private set
    val hasKids: Boolean get() = kidsName != null
    /** Areas the kids account sees (LIVE, MOVIES, SERIES); one always stays. */
    var kidsAreas by mutableStateOf(preferences?.getStringSet("kids_profile_areas", null)?.toSet() ?: MAIN_AREAS.toSet())
        private set
    /**
     * The kids account uses the admin's watch progress, watchlist, favourites and Trakt account
     * (true, default) or its own (false; then it can connect its own Trakt account).
     */
    var kidsShareData by mutableStateOf(preferences?.getBoolean("kids_profile_share", true) ?: true)
        private set

    fun updateKidsShareData(value: Boolean) {
        kidsShareData = value
        preferences?.edit()?.putBoolean("kids_profile_share", value)?.apply()
    }

    var startProfile by mutableStateOf(
        StartProfile.entries.firstOrNull { it.name == preferences?.getString("start_profile", null) } ?: StartProfile.ADMIN
    )
        private set

    fun createKids(name: String) {
        kidsName = name
        preferences?.edit()?.putString("kids_profile_name", name)?.apply()
    }

    fun removeKids() {
        kidsName = null
        startProfile = StartProfile.ADMIN
        preferences?.edit()?.remove("kids_profile_name")?.remove("kids_profile_areas")?.remove("kids_profile_share")
            ?.putString("start_profile", StartProfile.ADMIN.name)?.apply()
    }

    fun setKidsArea(area: String, shown: Boolean): Boolean {
        val next = if (shown) kidsAreas + area else kidsAreas - area
        if (next.none { it in MAIN_AREAS }) return false
        kidsAreas = next
        preferences?.edit()?.putStringSet("kids_profile_areas", next)?.apply()
        return true
    }

    fun updateStartProfile(value: StartProfile) {
        startProfile = value
        preferences?.edit()?.putString("start_profile", value.name)?.apply()
    }

    companion object {
        const val MAX_NAME = 24
        /** Each account has its own accent colour. */
        fun accentKey(kids: Boolean) = if (kids) "accent_color_kids" else "accent_color"
    }
}

/**
 * Account selection (after "Abmelden" and, if set, at start). The admin account always needs
 * the PIN once a kids account exists; Back leaves the app.
 */
@Composable
internal fun ProfilePicker(kidsName: String, onAdmin: () -> Unit, onKids: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.profile_picker_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuButton(stringResource(R.string.profile_admin), Modifier.width(220.dp).focusRequester(first), onClick = onAdmin)
                MenuButton(kidsName, Modifier.width(220.dp), onClick = onKids)
            }
        },
        confirmButton = {}
    )
}

/** Name for the kids account (TV keyboard). */
@Composable
internal fun ProfileNameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.kids_profile_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(UserProfiles.MAX_NAME) }, singleLine = true,
                    label = { Text(stringResource(R.string.kids_profile_name)) }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.trakt_app_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } }
    )
}

/** Kids account part of the Kinderschutz page (only for the admin). */
@Composable
internal fun KidsProfileSettings(
    profiles: UserProfiles, parental: ParentalControls,
    onCreate: () -> Unit, onRename: () -> Unit, onColor: () -> Unit, onTrakt: () -> Unit, kidsTraktName: String?,
    onRemove: () -> Unit
) {
    Text(stringResource(R.string.kids_profile), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    val name = profiles.kidsName
    if (name == null) {
        Text(stringResource(R.string.kids_profile_hint), style = MaterialTheme.typography.bodySmall)
        SettingRow(stringResource(R.string.kids_profile_create), enabled = parental.hasPin, onClick = onCreate)
        if (!parental.hasPin) Text(stringResource(R.string.kids_profile_needs_pin), style = MaterialTheme.typography.bodySmall)
        return
    }
    SettingRow(stringResource(R.string.kids_profile_rename, name), onClick = onRename)
    var lastArea by remember { mutableStateOf(false) }
    listOf("LIVE" to R.string.destination_live, "MOVIES" to R.string.destination_movies, "SERIES" to R.string.destination_series)
        .forEach { (area, label) ->
            val shown = area in profiles.kidsAreas
            SettingRow(stringResource(R.string.kids_profile_area, name, stringResource(label)), selected = shown) {
                lastArea = !profiles.setKidsArea(area, !shown)
            }
        }
    if (lastArea) Text(stringResource(R.string.settings_area_last), color = MaterialTheme.colorScheme.error)
    SettingRow(stringResource(R.string.kids_profile_color, name), onClick = onColor)
    SettingRow(stringResource(R.string.kids_profile_share, name), selected = profiles.kidsShareData) {
        profiles.updateKidsShareData(!profiles.kidsShareData)
    }
    Text(
        stringResource(if (profiles.kidsShareData) R.string.kids_profile_share_on else R.string.kids_profile_share_off, name),
        style = MaterialTheme.typography.bodySmall
    )
    if (!profiles.kidsShareData) SettingRow(
        listOf(
            stringResource(R.string.kids_profile_trakt, name),
            kidsTraktName?.let { stringResource(R.string.trakt_row_connected, it) } ?: stringResource(R.string.trakt_row_not_connected)
        ).joinToString(" · "),
        onClick = onTrakt
    )
    SettingRow(
        stringResource(R.string.start_profile_setting, when (profiles.startProfile) {
            StartProfile.KIDS -> name
            else -> stringResource(profiles.startProfile.label)
        })
    ) { profiles.updateStartProfile(StartProfile.entries[(profiles.startProfile.ordinal + 1) % StartProfile.entries.size]) }
    Text(stringResource(R.string.start_profile_hint), style = MaterialTheme.typography.bodySmall)
    SettingRow(stringResource(R.string.kids_profile_remove, name), onClick = onRemove)
}
