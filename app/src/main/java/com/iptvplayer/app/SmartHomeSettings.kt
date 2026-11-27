package com.iptvplayer.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlin.math.ln
import kotlin.math.pow

internal val LIGHT_DELAYS = listOf(0, 1, 2, 3, 5, 10, 15, 30)
internal val LIGHT_SUN_LEADS = listOf(0, 15, 30, 60)
internal val LIGHT_BRIGHTNESS = (1..10).map { it * 10 }
private val LIGHT_KELVINS = listOf(2_200, 2_700, 3_000, 3_500, 4_000, 5_000, 6_500)
private val LIGHT_HUES = (0 until 12).map { it * 30 }

internal fun lightHour(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

/** Name of a weekday (1 = Monday) in the language of the app. */
internal fun dayName(context: android.content.Context, day: Int): String =
    DayOfWeek.of(day).getDisplayName(TextStyle.FULL, context.resources.configuration.locales[0])
        .replaceFirstChar { it.titlecase(context.resources.configuration.locales[0]) }

/** Approximate colour of a white light of [kelvin] (for the palette). */
internal fun kelvinColor(kelvin: Int): Color {
    val t = kelvin / 100.0
    val red = if (t <= 66) 255.0 else 329.7 * (t - 60).pow(-0.1332)
    val green = if (t <= 66) 99.47 * ln(t) - 161.12 else 288.12 * (t - 60).pow(-0.0755)
    val blue = if (t >= 66) 255.0 else if (t <= 19) 0.0 else 138.52 * ln(t - 10) - 305.04
    fun c(v: Double) = (v.coerceIn(0.0, 255.0) / 255.0).toFloat()
    return Color(c(red), c(green), c(blue))
}

private fun hueColor(hue: Int) = Color.hsv(hue.toFloat(), 1f, 1f)

private fun Color.hex() = "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

/** The colour temperatures a light offers (within its range). */
private fun HaEntity.kelvins() = LIGHT_KELVINS.filter { it in minKelvin..maxKelvin }.ifEmpty { listOf(minKelvin, maxKelvin).distinct() }

/** Which page of the smart home section is open; Back goes one step back (see closeSettingsPages). */
internal object SmartHomePages {
    var state by mutableStateOf<LightState?>(null)
    var entity by mutableStateOf<String?>(null)
    val open: Boolean get() = state != null
    /** The row that stays on every page; Back moves the focus there before the page changes. */
    var anchor: FocusRequester? = null

    fun back(): Boolean = (state != null).also { if (it) runCatching { anchor?.requestFocus() } } && when {
        entity != null -> { entity = null; true }
        state != null -> { state = null; true }
        else -> false
    }
}

@Composable
internal fun lightStateLabel(state: LightState) = stringResource(lightStateRes(state))

private fun lightStateRes(state: LightState) = when (state) {
    LightState.PLAY -> R.string.smart_home_play
    LightState.PAUSE -> R.string.smart_home_pause
    LightState.STOP -> R.string.smart_home_stop
    LightState.SMOKE -> R.string.smart_home_smoke
}

/** Short description of one light in a scene: "nicht ändern", "aus", "an · 40 % · 2700 K". */
/** "an"/"aus", for blinds "auf"/"zu". */
private fun onLabel(entity: HaEntity?, on: Boolean) =
    if (entity?.domain == "cover") (if (on) R.string.smart_home_open else R.string.smart_home_close)
    else (if (on) R.string.smart_home_on else R.string.smart_home_off)

private fun lightSummary(context: android.content.Context, entity: HaEntity, setting: LightSetting?): String = when {
    setting == null -> context.getString(R.string.smart_home_unchanged)
    !setting.on -> context.getString(onLabel(entity, false))
    else -> listOfNotNull(
        context.getString(onLabel(entity, true)),
        setting.brightness?.let { "$it %" },
        if (setting.hue != null) context.getString(R.string.smart_home_colors) else setting.kelvin?.let { "$it K" }
    ).joinToString(" · ")
}

@Composable
internal fun lightWindowLabel(rule: LightRule) = when (rule.window) {
    LightWindow.ALWAYS -> stringResource(R.string.smart_home_always)
    LightWindow.DARK -> stringResource(R.string.smart_home_dark)
    LightWindow.HOURS -> stringResource(R.string.smart_home_hours, lightHour(rule.fromMinutes), lightHour(rule.toMinutes))
}

private fun lastText(context: android.content.Context): String? = LightAutomation.last?.let { (state, time) ->
    context.getString(R.string.smart_home_last, context.getString(lightStateRes(state)),
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(time)))
}

private fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1) % size]

/** Focus on the first row whenever [page] changes (the rows below it are new). */
@Composable
private fun rememberPageFocus(page: Any?): FocusRequester {
    val first = remember { FocusRequester() }
    LaunchedEffect(page) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    return first
}

/**
 * "Smart Home" section of the settings: connect Home Assistant (search in the home network,
 * address and token here or via the phone) and the light rule with one scene per state.
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
    val state = SmartHomePages.state
    val entityId = SmartHomePages.entity
    val first = rememberPageFocus(state to entityId)
    // Before a page changes, the focus goes to the top row, which stays on every page: a row that
    // disappears would hand it to the section bar, which then switched the section.
    fun go(change: () -> Unit) {
        runCatching { first.requestFocus() }
        change()
    }
    SmartHomePages.anchor = first

    val entity = if (connected) state?.let { LightAutomation.known?.firstOrNull { it.id == entityId } } else null
    when {
        !connected -> {
            Text(stringResource(R.string.smart_home_hint), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.smart_home_not_connected), color = MaterialTheme.colorScheme.primary)
        }
        else -> Unit
    }
    if (connected) {
        when {
            state == null -> {
                Text(stringResource(R.string.smart_home_hint), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.smart_home_connected, LightAutomation.known?.size ?: 0), color = MaterialTheme.colorScheme.primary)
                lastText(context)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            entity != null -> Text(lightStateLabel(state) + " · " + entity.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            else -> {
                Text(lightStateLabel(state), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                if (state == LightState.SMOKE) Text(stringResource(R.string.smart_home_smoke_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    // The one row that stays on every page (same place, same row): "Suchen" before connecting,
    // "Licht-Automatik" on the overview, "Zurück" on the pages below it.
    SettingRow(
        stringResource(when {
            !connected -> R.string.smart_home_search
            state == null -> R.string.smart_home_enabled
            else -> R.string.navigation_back
        }),
        Modifier.focusRequester(first), selected = if (connected && state == null) rule.enabled else null
    ) {
        when {
            !connected -> busy {
                found = discoverHomeAssistant(context)
                status = if (found.isNullOrEmpty()) context.getString(R.string.smart_home_none_found) else null
            }
            state == null -> LightAutomation.update { it.copy(enabled = !it.enabled) }
            else -> go { SmartHomePages.back() }
        }
    }
    if (connected && state != null) {
        if (entity != null) LightEditor(state, entity)
        else SceneEditor(state, ::go) { busy { LightAutomation.test(state) } }
    }
    if (!connected) {
        found?.forEach { candidate -> SettingRow(candidate, selected = candidate == address) { address = candidate } }
        OutlinedTextField(
            value = address, onValueChange = { address = it.trim().take(200) }, singleLine = true,
            label = { Text(stringResource(R.string.smart_home_address)) }, modifier = Modifier.fillMaxWidth()
        )
        SecretField(token, { token = it.trim().take(400) }, stringResource(R.string.smart_home_token), Modifier.fillMaxWidth())
        SettingRow(stringResource(R.string.smart_home_connect), enabled = address.isNotBlank() && token.isNotBlank()) {
            // The connect row disappears with the connection: the focus waits on the top row.
            runCatching { first.requestFocus() }
            busy {
                val count = LightAutomation.connect(address, token)
                token = ""
                status = context.getString(R.string.smart_home_connected, count)
            }
        }
        // The token is long: typing it on the phone is easier.
        SettingRow(stringResource(R.string.smart_home_via_phone)) { onPhone() }
    } else if (state == null) {
        // One page per state: which lights do what.
        LightState.entries.forEach { each ->
            val scene = rule.scene(each)
            val summary = if (scene.isEmpty()) stringResource(R.string.smart_home_scene_empty)
                else stringResource(R.string.smart_home_scene_lights, scene.size)
            SettingRow(lightStateLabel(each) + ": " + summary) { go { SmartHomePages.state = each } }
        }
        SettingRow(stringResource(R.string.smart_home_pause_delay) + ": " + stringResource(R.string.smart_home_seconds, rule.pauseDelay)) {
            LightAutomation.update { it.copy(pauseDelay = LIGHT_DELAYS.after(it.pauseDelay.takeIf { d -> d in LIGHT_DELAYS } ?: 0)) }
        }
        SettingRow(stringResource(R.string.smart_home_stop_delay) + ": " + stringResource(R.string.smart_home_seconds, rule.stopDelay)) {
            LightAutomation.update { it.copy(stopDelay = LIGHT_DELAYS.after(it.stopDelay.takeIf { d -> d in LIGHT_DELAYS } ?: 0)) }
        }
        listOf(
            Triple(R.string.destination_movies, rule.movies) { r: LightRule -> r.copy(movies = !r.movies) },
            Triple(R.string.destination_series, rule.series) { r: LightRule -> r.copy(series = !r.series) },
            Triple(R.string.destination_live, rule.live) { r: LightRule -> r.copy(live = !r.live) }
        ).forEach { (label, on, toggle) ->
            SettingRow(stringResource(R.string.smart_home_for, stringResource(label)), selected = on) { LightAutomation.update(toggle) }
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
        // Weekdays on which the rule applies (names from the system, in the app language).
        Text(stringResource(R.string.smart_home_days), style = MaterialTheme.typography.labelLarge)
        (1..7).forEach { day ->
            SettingRow(dayName(context, day), selected = day in rule.days) {
                LightAutomation.update { it.copy(days = if (day in it.days) it.days - day else it.days + day) }
            }
        }
        SettingRow(stringResource(R.string.smart_home_via_phone)) { onPhone() }
        SettingRow(stringResource(R.string.smart_home_disconnect)) {
            runCatching { first.requestFocus() }
            busy { LightAutomation.disconnect(); status = null }
        }
    }
    if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
    status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

/** All lights for one state; a light opens its own page. */
@Composable
private fun SceneEditor(state: LightState, go: (() -> Unit) -> Unit, onTry: () -> Unit) {
    val context = LocalContext.current
    val scene = LightAutomation.rule.scene(state)
    SettingRow(stringResource(R.string.smart_home_try), enabled = scene.isNotEmpty()) { onTry() }
    LightAutomation.known?.forEach { entity ->
        SettingRow(entity.name + ": " + lightSummary(context, entity, scene[entity.id])) { go { SmartHomePages.entity = entity.id } }
    }
}

/** One light in one state: not changed, off, or on with brightness and colour. */
@Composable
private fun LightEditor(state: LightState, entity: HaEntity) {
    val setting = LightAutomation.rule.scene(state)[entity.id]
    fun set(value: LightSetting?) = LightAutomation.update { it.withSetting(state, entity.id, value) }
    SettingRow(stringResource(R.string.smart_home_unchanged), selected = setting == null) { set(null) }
    if (entity.domain != "scene") SettingRow(stringResource(onLabel(entity, false)), selected = setting?.on == false) { set(LightSetting(false)) }
    SettingRow(stringResource(onLabel(entity, true)), selected = setting?.on == true) {
        if (setting?.on != true) set(LightSetting(true, if (entity.dimmable) 100 else null))
    }
    if (setting?.on != true) return
    if (entity.dimmable) {
        SettingRow(stringResource(R.string.smart_home_brightness) + ": " + (setting.brightness ?: 100) + " %") {
            set(setting.copy(brightness = LIGHT_BRIGHTNESS.after(setting.brightness ?: 100)))
        }
    }
    if (!entity.temperature && !entity.color) return
    // One palette: colour temperatures (warm to cold) and, for colour lights, colours as well.
    Text(stringResource(R.string.smart_home_color), style = MaterialTheme.typography.labelLarge)
    val swatches = buildList {
        add(Triple<Color?, Boolean, () -> Unit>(null, setting.kelvin == null && setting.hue == null) {
            set(setting.copy(kelvin = null, hue = null, saturation = null))
        })
        entity.kelvins().forEach { kelvin ->
            add(Triple(kelvinColor(kelvin), setting.hue == null && setting.kelvin == kelvin) {
                set(setting.copy(kelvin = kelvin, hue = null, saturation = null))
            })
        }
        if (entity.color) LIGHT_HUES.forEach { hue ->
            add(Triple(hueColor(hue), setting.hue == hue) { set(setting.copy(hue = hue, saturation = 100, kelvin = null)) })
        }
    }
    swatches.chunked(7).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (color, selected, pick) -> Swatch(color, selected, pick) }
        }
    }
}

/** One colour of the palette; null: keep the colour as it is. */
@Composable
private fun Swatch(color: Color?, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(40.dp).onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick),
        shape = CircleShape,
        color = color ?: MaterialTheme.colorScheme.surfaceContainerHigh,
        border = when {
            focused -> BorderStroke(3.dp, accent)
            selected -> BorderStroke(3.dp, Color.White)
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        }
    ) {
        if (color == null) Box(contentAlignment = Alignment.Center) { Text("–", style = MaterialTheme.typography.titleMedium) }
    }
}

/** The "Smart Home" sections for the phone page: status and rule, and one section per state. */
internal fun smartHomeRemoteEntries(context: android.content.Context): List<RemoteEntry> = buildList {
    fun text(id: Int, vararg args: Any) = context.getString(id, *args)
    val section = text(R.string.settings_smart_home)
    val rule = LightAutomation.rule
    val connected = LightAutomation.token != null && rule.url.isNotBlank()
    add(RemoteEntry.Info("smartHome.status", section, text(R.string.settings_smart_home), when {
        LightAutomation.connecting -> text(R.string.smart_home_connecting)
        connected -> listOfNotNull(text(R.string.smart_home_connected, LightAutomation.known?.size ?: 0), lastText(context)).joinToString(" · ")
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
    add(RemoteEntry.Toggle("smartHome.movies", section, text(R.string.smart_home_for, text(R.string.destination_movies)),
        { LightAutomation.rule.movies }) { on -> LightAutomation.update { it.copy(movies = on) } })
    add(RemoteEntry.Toggle("smartHome.series", section, text(R.string.smart_home_for, text(R.string.destination_series)),
        { LightAutomation.rule.series }) { on -> LightAutomation.update { it.copy(series = on) } })
    add(RemoteEntry.Toggle("smartHome.live", section, text(R.string.smart_home_for, text(R.string.destination_live)),
        { LightAutomation.rule.live }) { on -> LightAutomation.update { it.copy(live = on) } })
    // Delays to the second (the TV offers steps).
    add(RemoteEntry.Number("smartHome.pauseDelay", section, text(R.string.smart_home_pause_delay), 0, 30, 1, "s",
        { LightAutomation.rule.pauseDelay }) { value -> LightAutomation.update { it.copy(pauseDelay = value.coerceIn(0, 30)) } })
    add(RemoteEntry.Number("smartHome.stopDelay", section, text(R.string.smart_home_stop_delay), 0, 30, 1, "s",
        { LightAutomation.rule.stopDelay }) { value -> LightAutomation.update { it.copy(stopDelay = value.coerceIn(0, 30)) } })
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
    (1..7).forEach { day ->
        add(RemoteEntry.Toggle("smartHome.day$day", section, text(R.string.smart_home_days) + ": " + dayName(context, day),
            { day in LightAutomation.rule.days }) { on -> LightAutomation.update { it.copy(days = if (on) it.days + day else it.days - day) } })
    }
    add(RemoteEntry.Action("smartHome.disconnect", section, text(R.string.smart_home_disconnect), text(R.string.smart_home_disconnect)) {
        LightAutomation.disconnectInBackground(); null
    })
    // One section per state: for each light what it does, its brightness and colour.
    for (state in LightState.entries) {
        val page = text(R.string.settings_smart_home) + " · " + text(lightStateRes(state))
        val key = "smartHome.${state.name.lowercase()}"
        if (state == LightState.SMOKE) add(RemoteEntry.Info("$key.hint", page, text(R.string.smart_home_smoke), text(R.string.smart_home_smoke_hint)))
        add(RemoteEntry.Action("$key.try", page, text(R.string.smart_home_try), text(R.string.smart_home_try)) {
            LightAutomation.testInBackground(state); null
        })
        LightAutomation.known?.forEach { entity ->
            fun current() = LightAutomation.rule.scene(state)[entity.id]
            fun set(value: LightSetting?) = LightAutomation.update { it.withSetting(state, entity.id, value) }
            val modes = listOfNotNull(
                RemoteOption("keep", text(R.string.smart_home_unchanged)),
                RemoteOption("off", text(onLabel(entity, false))).takeIf { entity.domain != "scene" },
                RemoteOption("on", text(onLabel(entity, true)))
            )
            add(RemoteEntry.Choice("$key.${entity.id}.mode", page, entity.name, modes,
                { current()?.let { if (it.on) "on" else "off" } ?: "keep" }) { value ->
                when (value) {
                    "keep" -> set(null)
                    "off" -> set(LightSetting(false))
                    "on" -> if (current()?.on != true) set(LightSetting(true, if (entity.dimmable) 100 else null))
                }
            })
            if (entity.dimmable) add(RemoteEntry.Number("$key.${entity.id}.brightness", page,
                entity.name + " · " + text(R.string.smart_home_brightness), 10, 100, 10, "%",
                { current()?.brightness ?: 100 }) { value -> current()?.takeIf { it.on }?.let { set(it.copy(brightness = value.coerceIn(10, 100))) } }
                .shownIf("$key.${entity.id}.mode", "on"))
            if (entity.temperature || entity.color) {
                val options = buildList {
                    add(RemoteOption("keep", text(R.string.smart_home_unchanged)))
                    entity.kelvins().forEach { add(RemoteOption("k$it", "$it K", kelvinColor(it).hex())) }
                    if (entity.color) LIGHT_HUES.forEach { add(RemoteOption("h$it", "$it°", hueColor(it).hex())) }
                }
                add(RemoteEntry.Choice("$key.${entity.id}.color", page, entity.name + " · " + text(R.string.smart_home_color), options,
                    { current()?.let { s -> s.hue?.let { "h$it" } ?: s.kelvin?.let { "k$it" } } ?: "keep" }) { value ->
                    val on = current()?.takeIf { it.on } ?: return@Choice
                    when {
                        value == "keep" -> set(on.copy(kelvin = null, hue = null, saturation = null))
                        value.startsWith("k") -> value.drop(1).toIntOrNull()?.let { set(on.copy(kelvin = it, hue = null, saturation = null)) }
                        value.startsWith("h") -> value.drop(1).toIntOrNull()?.takeIf { it in 0..359 }?.let { set(on.copy(hue = it, saturation = 100, kelvin = null)) }
                    }
                }.shownIf("$key.${entity.id}.mode", "on"))
            }
        }
    }
}
