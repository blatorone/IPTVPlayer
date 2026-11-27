package com.iptvplayer.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal val LIGHT_PLAY_LEVELS = listOf(0, 10, 20, 30, 50)
internal val LIGHT_PAUSE_DELAYS = listOf(-1, 0, 3, 5, 10)
internal val LIGHT_ON_LEVELS = listOf(100, 70, 50, 30)
internal val LIGHT_SUN_LEADS = listOf(0, 15, 30, 60)

internal fun lightHour(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** The label of what happens while playing (off or dimmed). */
@Composable
internal fun lightPlayLabel(level: Int) =
    if (level == 0) stringResource(R.string.smart_home_off) else stringResource(R.string.smart_home_dim, level)

@Composable
internal fun lightPauseLabel(delay: Int) =
    if (delay < 0) stringResource(R.string.smart_home_leave) else stringResource(R.string.smart_home_on_after, delay)

@Composable
internal fun lightWindowLabel(rule: LightRule) = when (rule.window) {
    LightWindow.ALWAYS -> stringResource(R.string.smart_home_always)
    LightWindow.DARK -> stringResource(R.string.smart_home_dark)
    LightWindow.HOURS -> stringResource(R.string.smart_home_hours, lightHour(rule.fromMinutes), lightHour(rule.toMinutes))
}

/** "Zuletzt geschaltet: aus · 21:03", or null before the first switch. */
@Composable
internal fun lightLastLabel(): String? = LightAutomation.last?.let { (state, time) ->
    stringResource(
        R.string.smart_home_last,
        stringResource(when (state) { true -> R.string.smart_home_on; false -> R.string.smart_home_off; null -> R.string.smart_home_dimmed }),
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(time))
    )
}

private fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1) % size]

/**
 * "Smart Home" section of the settings: connect Home Assistant (search in the home network,
 * address and token here or via the phone) and the light rule.
 */
@Composable
internal fun SmartHomeSettings(onPhone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rule = LightAutomation.rule
    val connected = LightAutomation.token != null && rule.url.isNotBlank()
    var working by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<String>?>(null) }
    var address by remember { mutableStateOf(rule.url.ifBlank { "http://homeassistant.local:8123" }) }
    var token by remember { mutableStateOf("") }
    fun busy(action: suspend () -> Unit) {
        if (working) return
        scope.launch {
            working = true
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                status = context.getString(R.string.smart_home_failed)
            } finally {
                working = false
            }
        }
    }

    Text(stringResource(R.string.smart_home_hint), style = MaterialTheme.typography.bodySmall)
    if (!connected) {
        Text(stringResource(R.string.smart_home_not_connected), color = MaterialTheme.colorScheme.primary)
        SettingRow(stringResource(R.string.smart_home_search)) {
            busy {
                found = discoverHomeAssistant(context)
                status = if (found.isNullOrEmpty()) context.getString(R.string.smart_home_none_found) else null
            }
        }
        found?.forEach { candidate -> SettingRow(candidate, selected = candidate == address) { address = candidate } }
        OutlinedTextField(
            value = address, onValueChange = { address = it.trim().take(200) }, singleLine = true,
            label = { Text(stringResource(R.string.smart_home_address)) }, modifier = Modifier.fillMaxWidth()
        )
        SecretField(token, { token = it.trim().take(400) }, stringResource(R.string.smart_home_token), Modifier.fillMaxWidth())
        SettingRow(stringResource(R.string.smart_home_connect), enabled = address.isNotBlank() && token.isNotBlank()) {
            busy {
                val count = LightAutomation.connect(address, token)
                token = ""
                status = context.getString(R.string.smart_home_connected, count)
            }
        }
        // The token is long: typing it on the phone is easier.
        SettingRow(stringResource(R.string.smart_home_via_phone)) { onPhone() }
    } else {
        Text(stringResource(R.string.smart_home_connected, LightAutomation.known?.size ?: 0), color = MaterialTheme.colorScheme.primary)
        lightLastLabel()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        SettingRow(stringResource(R.string.smart_home_enabled), selected = rule.enabled) {
            LightAutomation.update { it.copy(enabled = !it.enabled) }
        }
        Text(stringResource(R.string.smart_home_lights), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        LightAutomation.known?.forEach { entity ->
            SettingRow(entity.name, selected = entity.id in rule.entities) {
                LightAutomation.update { it.copy(entities = if (entity.id in it.entities) it.entities - entity.id else it.entities + entity.id) }
            }
        }
        listOf(
            Triple(R.string.destination_movies, rule.movies) { r: LightRule -> r.copy(movies = !r.movies) },
            Triple(R.string.destination_series, rule.series) { r: LightRule -> r.copy(series = !r.series) },
            Triple(R.string.destination_live, rule.live) { r: LightRule -> r.copy(live = !r.live) }
        ).forEach { (label, on, toggle) ->
            SettingRow(stringResource(R.string.smart_home_for, stringResource(label)), selected = on) { LightAutomation.update(toggle) }
        }
        SettingRow(stringResource(R.string.smart_home_play) + ": " + lightPlayLabel(rule.playLevel)) {
            LightAutomation.update { it.copy(playLevel = LIGHT_PLAY_LEVELS.after(it.playLevel)) }
        }
        SettingRow(stringResource(R.string.smart_home_pause) + ": " + lightPauseLabel(rule.pauseDelay)) {
            LightAutomation.update { it.copy(pauseDelay = LIGHT_PAUSE_DELAYS.after(it.pauseDelay)) }
        }
        SettingRow(stringResource(R.string.smart_home_on_stop), selected = rule.onStop) {
            LightAutomation.update { it.copy(onStop = !it.onStop) }
        }
        SettingRow(stringResource(R.string.smart_home_level) + ": " + rule.onLevel + " %") {
            LightAutomation.update { it.copy(onLevel = LIGHT_ON_LEVELS.after(it.onLevel)) }
        }
        SettingRow(stringResource(R.string.smart_home_active) + ": " + lightWindowLabel(rule)) {
            LightAutomation.update { it.copy(window = LightWindow.entries.after(it.window)) }
        }
        when (rule.window) {
            LightWindow.HOURS -> {
                SettingRow(stringResource(R.string.smart_home_from) + ": " + lightHour(rule.fromMinutes)) {
                    LightAutomation.update { it.copy(fromMinutes = (it.fromMinutes + 60) % (24 * 60)) }
                }
                SettingRow(stringResource(R.string.smart_home_to) + ": " + lightHour(rule.toMinutes)) {
                    LightAutomation.update { it.copy(toMinutes = (it.toMinutes + 60) % (24 * 60)) }
                }
            }
            LightWindow.DARK -> SettingRow(stringResource(R.string.smart_home_sun_lead_label) + ": " + stringResource(R.string.smart_home_minutes, rule.sunLead)) {
                LightAutomation.update { it.copy(sunLead = LIGHT_SUN_LEADS.after(it.sunLead)) }
            }
            LightWindow.ALWAYS -> Unit
        }
        SettingRow(stringResource(R.string.smart_home_test), enabled = rule.entities.isNotEmpty()) { busy { LightAutomation.test() } }
        SettingRow(stringResource(R.string.smart_home_via_phone)) { onPhone() }
        SettingRow(stringResource(R.string.smart_home_disconnect)) { busy { LightAutomation.disconnect(); status = null } }
    }
    if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
    status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

/** The "Smart Home" section for the phone page: status, connecting and the whole light rule. */
internal fun smartHomeRemoteEntries(context: android.content.Context): List<RemoteEntry> = buildList {
    fun text(id: Int, vararg args: Any) = context.getString(id, *args)
    val section = text(R.string.settings_smart_home)
    val rule = LightAutomation.rule
    val connected = LightAutomation.token != null && rule.url.isNotBlank()
    val last = LightAutomation.last?.let { (state, time) ->
        text(R.string.smart_home_last,
            text(when (state) { true -> R.string.smart_home_on; false -> R.string.smart_home_off; null -> R.string.smart_home_dimmed }),
            java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(time)))
    }
    add(RemoteEntry.Info("smartHome.status", section, text(R.string.settings_smart_home), when {
        LightAutomation.connecting -> text(R.string.smart_home_connecting)
        connected -> listOfNotNull(text(R.string.smart_home_connected, LightAutomation.known?.size ?: 0), last).joinToString(" · ")
        LightAutomation.failed -> text(R.string.smart_home_failed)
        else -> text(R.string.smart_home_not_connected)
    }))
    if (!connected) {
        add(RemoteEntry.Input("smartHome.address", section, text(R.string.smart_home_address),
            rule.url.ifBlank { "http://homeassistant.local:8123" }, false, text(R.string.smart_home_save)) { value ->
            LightAutomation.update { it.copy(url = value.trim()) }
            null
        })
        add(RemoteEntry.Input("smartHome.token", section, text(R.string.smart_home_token), text(R.string.smart_home_token_hint), true,
            text(R.string.smart_home_connect)) { value ->
            if (value.isBlank() || rule.url.isBlank()) text(R.string.smart_home_failed)
            else { LightAutomation.connectInBackground(rule.url, value); text(R.string.smart_home_connecting) }
        })
        add(RemoteEntry.Action("smartHome.search", section, text(R.string.smart_home_search), text(R.string.smart_home_search)) {
            LightAutomation.searchInBackground(context); text(R.string.smart_home_searching)
        })
        return@buildList
    }
    add(RemoteEntry.Toggle("smartHome.enabled", section, text(R.string.smart_home_enabled), { LightAutomation.rule.enabled }) { on ->
        LightAutomation.update { it.copy(enabled = on) }
    })
    LightAutomation.known?.forEach { entity ->
        add(RemoteEntry.Toggle("smartHome.entity.${entity.id}", section, entity.name, { entity.id in LightAutomation.rule.entities }) { on ->
            LightAutomation.update { it.copy(entities = if (on) it.entities + entity.id else it.entities - entity.id) }
        })
    }
    add(RemoteEntry.Toggle("smartHome.movies", section, text(R.string.smart_home_for, text(R.string.destination_movies)),
        { LightAutomation.rule.movies }) { on -> LightAutomation.update { it.copy(movies = on) } })
    add(RemoteEntry.Toggle("smartHome.series", section, text(R.string.smart_home_for, text(R.string.destination_series)),
        { LightAutomation.rule.series }) { on -> LightAutomation.update { it.copy(series = on) } })
    add(RemoteEntry.Toggle("smartHome.live", section, text(R.string.smart_home_for, text(R.string.destination_live)),
        { LightAutomation.rule.live }) { on -> LightAutomation.update { it.copy(live = on) } })
    add(RemoteEntry.Choice("smartHome.play", section, text(R.string.smart_home_play),
        LIGHT_PLAY_LEVELS.map { RemoteOption(it.toString(), if (it == 0) text(R.string.smart_home_off) else text(R.string.smart_home_dim, it)) },
        { LightAutomation.rule.playLevel.toString() }) { value -> value.toIntOrNull()?.takeIf { it in LIGHT_PLAY_LEVELS }?.let { level -> LightAutomation.update { it.copy(playLevel = level) } } })
    add(RemoteEntry.Choice("smartHome.pause", section, text(R.string.smart_home_pause),
        LIGHT_PAUSE_DELAYS.map { RemoteOption(it.toString(), if (it < 0) text(R.string.smart_home_leave) else text(R.string.smart_home_on_after, it)) },
        { LightAutomation.rule.pauseDelay.toString() }) { value -> value.toIntOrNull()?.takeIf { it in LIGHT_PAUSE_DELAYS }?.let { delay -> LightAutomation.update { it.copy(pauseDelay = delay) } } })
    add(RemoteEntry.Toggle("smartHome.stop", section, text(R.string.smart_home_on_stop), { LightAutomation.rule.onStop }) { on ->
        LightAutomation.update { it.copy(onStop = on) }
    })
    add(RemoteEntry.Choice("smartHome.level", section, text(R.string.smart_home_level),
        LIGHT_ON_LEVELS.map { RemoteOption(it.toString(), "$it %") },
        { LightAutomation.rule.onLevel.toString() }) { value -> value.toIntOrNull()?.takeIf { it in LIGHT_ON_LEVELS }?.let { level -> LightAutomation.update { it.copy(onLevel = level) } } })
    add(RemoteEntry.Choice("smartHome.window", section, text(R.string.smart_home_active),
        listOf(
            RemoteOption(LightWindow.ALWAYS.name, text(R.string.smart_home_always)),
            RemoteOption(LightWindow.DARK.name, text(R.string.smart_home_dark)),
            RemoteOption(LightWindow.HOURS.name, text(R.string.smart_home_hours, lightHour(rule.fromMinutes), lightHour(rule.toMinutes)))
        ),
        { LightAutomation.rule.window.name }) { value -> LightWindow.entries.firstOrNull { it.name == value }?.let { window -> LightAutomation.update { it.copy(window = window) } } })
    val hours = (0 until 24).map { RemoteOption((it * 60).toString(), lightHour(it * 60)) }
    add(RemoteEntry.Choice("smartHome.from", section, text(R.string.smart_home_from), hours,
        { LightAutomation.rule.fromMinutes.toString() }) { value -> value.toIntOrNull()?.takeIf { it in 0 until 24 * 60 }?.let { m -> LightAutomation.update { it.copy(fromMinutes = m) } } }
        .shownIf("smartHome.window", LightWindow.HOURS.name))
    add(RemoteEntry.Choice("smartHome.to", section, text(R.string.smart_home_to), hours,
        { LightAutomation.rule.toMinutes.toString() }) { value -> value.toIntOrNull()?.takeIf { it in 0 until 24 * 60 }?.let { m -> LightAutomation.update { it.copy(toMinutes = m) } } }
        .shownIf("smartHome.window", LightWindow.HOURS.name))
    add(RemoteEntry.Choice("smartHome.sunLead", section, text(R.string.smart_home_sun_lead_label),
        LIGHT_SUN_LEADS.map { RemoteOption(it.toString(), text(R.string.smart_home_minutes, it)) },
        { LightAutomation.rule.sunLead.toString() }) { value -> value.toIntOrNull()?.takeIf { it in LIGHT_SUN_LEADS }?.let { lead -> LightAutomation.update { it.copy(sunLead = lead) } } }
        .shownIf("smartHome.window", LightWindow.DARK.name))
    add(RemoteEntry.Action("smartHome.test", section, text(R.string.smart_home_test), text(R.string.smart_home_test_button)) {
        LightAutomation.testInBackground(); null
    })
    add(RemoteEntry.Action("smartHome.disconnect", section, text(R.string.smart_home_disconnect), text(R.string.smart_home_disconnect)) {
        LightAutomation.disconnectInBackground(); null
    })
}
