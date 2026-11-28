package com.iptvplayer.app

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent as AndroidKeyEvent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/*
 * Per-channel edits of the Live-TV list (long press OK on a channel): its own order inside the
 * category, hidden channels and the name the XMLTV guide is searched with. Categories get the
 * same move mode. Stored by channel name, so a portal refresh keeps them.
 */

internal object ChannelEdits {
    private var prefs: SharedPreferences? = null

    var hidden by mutableStateOf<Set<String>>(emptySet())
        private set
    var orders by mutableStateOf<Map<String, List<String>>>(emptyMap())
        private set
    var epgNames by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** The channel whose menu is open. */
    var menu by mutableStateOf<StalkerChannel?>(null)

    /** Name of the channel being moved with the arrow keys. */
    var moving by mutableStateOf<String?>(null)

    /** The category being moved: section and id. */
    var movingCategory by mutableStateOf<Pair<PortalSection, String>?>(null)

    fun init(context: Context) {
        if (prefs != null) return
        val store = context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
        prefs = store
        hidden = store.getStringSet("hidden_channels", emptySet()).orEmpty().toSet()
        orders = runCatching {
            val json = JSONObject(store.getString("channel_orders", null) ?: "{}")
            json.keys().asSequence().associateWith { key ->
                val list = json.getJSONArray(key)
                List(list.length()) { list.getString(it) }
            }
        }.getOrDefault(emptyMap())
        epgNames = runCatching {
            val json = JSONObject(store.getString("epg_names", null) ?: "{}")
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())
    }

    fun isHidden(channel: StalkerChannel) = channel.name in hidden

    fun setHiddenNames(names: Set<String>, hide: Boolean) {
        hidden = if (hide) hidden + names else hidden - names
        prefs?.edit { putStringSet("hidden_channels", hidden) }
    }

    fun setHidden(channel: StalkerChannel, hide: Boolean) {
        hidden = if (hide) hidden + channel.name else hidden - channel.name
        prefs?.edit { putStringSet("hidden_channels", hidden) }
    }

    fun epgName(channel: StalkerChannel): String? = epgNames[channel.name]

    fun setEpgName(channel: StalkerChannel, name: String?) {
        val value = name?.trim()?.takeIf { it.isNotEmpty() }
        epgNames = if (value == null) epgNames - channel.name else epgNames + (channel.name to value)
        prefs?.edit { putString("epg_names", JSONObject(epgNames).toString()) }
    }

    /** The channels of [category] in the stored order (new channels at the end), hidden ones left out unless [showHidden]. */
    fun arrange(category: String, channels: List<StalkerChannel>, showHidden: Boolean): List<StalkerChannel> {
        val order = orders[category]
        val sorted = if (order.isNullOrEmpty()) channels else {
            val positions = order.withIndex().associate { it.value to it.index }
            channels.sortedBy { positions[it.name] ?: Int.MAX_VALUE }
        }
        return if (showHidden || hidden.isEmpty()) sorted else sorted.filter { it.name !in hidden }
    }

    /** Moves [channel] one place up or down among the [shown] channels of [category] ([all]: also the hidden ones). */
    fun move(category: String, all: List<StalkerChannel>, shown: List<StalkerChannel>, channel: StalkerChannel, up: Boolean) {
        val order = moveAmong(arrange(category, all, true).map { it.name }.distinct(), shown.map { it.name }, channel.name, up)
            ?: return
        orders = orders + (category to order)
        prefs?.edit { putString("channel_orders", JSONObject(orders.mapValues { JSONArray(it.value) }).toString()) }
    }
}

/**
 * [id] one step up or down in [order], past the next entry that is [shown] (hidden ones in between
 * are skipped). Null when it is already at that end.
 */
internal fun moveAmong(order: List<String>, shown: List<String>, id: String, up: Boolean): List<String>? {
    val position = shown.indexOf(id)
    if (position < 0) return null
    val neighbour = shown.getOrNull(if (up) position - 1 else position + 1) ?: return null
    val rest = order.filter { it != id }.toMutableList()
    val target = rest.indexOf(neighbour)
    if (target < 0) return null
    rest.add(if (up) target else target + 1, id)
    return rest
}

/**
 * While [active]: the arrow keys along the list ([vertical]: up/down, else left/right) move the
 * entry, OK or Back ends; the other keys do nothing. The entry takes the focus when it starts.
 */
@Composable
internal fun Modifier.moveKeys(active: Boolean, vertical: Boolean, onStep: (Boolean) -> Unit, onEnd: () -> Unit): Modifier =
    if (!active) this else {
        val requester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
        }
        val back = if (vertical) AndroidKeyEvent.KEYCODE_DPAD_UP else AndroidKeyEvent.KEYCODE_DPAD_LEFT
        val forward = if (vertical) AndroidKeyEvent.KEYCODE_DPAD_DOWN else AndroidKeyEvent.KEYCODE_DPAD_RIGHT
        this.focusRequester(requester).onPreviewKeyEvent { event ->
            when (event.nativeKeyEvent.keyCode) {
                back, forward -> {
                    if (event.type == KeyEventType.KeyDown) onStep(event.nativeKeyEvent.keyCode == back)
                    true
                }
                AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_ENTER, AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.KEYCODE_ESCAPE -> {
                    if (event.type == KeyEventType.KeyUp) onEnd()
                    true
                }
                else -> true
            }
        }
    }

internal fun showMoveHint(context: Context, vertical: Boolean) {
    Toast.makeText(
        context, context.getString(if (vertical) R.string.move_hint_vertical else R.string.move_hint_horizontal),
        Toast.LENGTH_LONG
    ).show()
}

/** The menu of a channel: move, visibility, the XMLTV name. [guideName]: the name the guide uses without a change. */
@Composable
internal fun ChannelMenuDialog(channel: StalkerChannel, guideName: String, categoryChannels: List<StalkerChannel>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var page by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val first = remember(page) { FocusRequester() }
    SettingsDialog(channel.name, onDismiss) {
    FreshPressOnly {
        when (page) {
            0 -> {
                SettingRow(stringResource(R.string.edit_move), Modifier.focusRequester(first)) {
                    onDismiss()
                    ChannelEdits.moving = channel.name
                    showMoveHint(context, true)
                }
                SettingRow(stringResource(R.string.edit_visibility)) { page = 1 }
                SettingRow(stringResource(R.string.epg_name_edit)) { page = 2 }
            }
            1 -> {
                SettingRow(stringResource(R.string.visibility_only_channel), Modifier.focusRequester(first)) { page = 3 }
                SettingRow(stringResource(R.string.visibility_all_channels)) { page = 4 }
            }
            4 -> VisibilityList(
                categoryChannels.map { it.name }.distinct().map { it to it }, first,
                isShown = { it !in ChannelEdits.hidden }
            ) { names, show -> ChannelEdits.setHiddenNames(names, !show) }
            3 -> {
                val hidden = ChannelEdits.isHidden(channel)
                SettingRow(stringResource(R.string.show_categories), Modifier.focusRequester(first), selected = !hidden) {
                    ChannelEdits.setHidden(channel, false)
                    onDismiss()
                }
                SettingRow(stringResource(R.string.hide_categories), selected = hidden) {
                    ChannelEdits.setHidden(channel, true)
                    onDismiss()
                }
            }
            else -> {
                var name by remember { mutableStateOf(ChannelEdits.epgName(channel) ?: guideName) }
                Text(stringResource(R.string.epg_name_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.epg_name_edit)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(first)
                )
                SettingRow(stringResource(R.string.epg_name_save)) {
                    ChannelEdits.setEpgName(channel, name.takeUnless { it.trim() == guideName })
                    onDismiss()
                }
                if (ChannelEdits.epgName(channel) != null) SettingRow(stringResource(R.string.epg_name_reset)) {
                    ChannelEdits.setEpgName(channel, null)
                    onDismiss()
                }
            }
        }
        SettingRow(stringResource(android.R.string.cancel)) { if (page == 0) onDismiss() else page = 0 }
    }
    }
    LaunchedEffect(page) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
}

/** The menu of one category: move, visibility and (with a PIN) the lock. */
@Composable
internal fun CategoryActionDialog(
    name: String,
    id: String,
    vertical: Boolean,
    onMove: () -> Unit,
    // All categories of the section (id to name) and their visibility.
    allGroups: List<Pair<String, String>>,
    isHidden: (String) -> Boolean,
    onHidden: (Set<String>, Boolean) -> Unit,
    locked: Boolean,
    onToggleLock: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var page by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val first = remember(page) { FocusRequester() }
    SettingsDialog(name, onDismiss) {
    FreshPressOnly {
        if (page == 0) {
            SettingRow(stringResource(R.string.edit_move), Modifier.focusRequester(first)) {
                onDismiss()
                onMove()
                showMoveHint(context, vertical)
            }
            SettingRow(stringResource(R.string.edit_visibility)) { page = 1 }
            onToggleLock?.let { toggle ->
                SettingRow(stringResource(if (locked) R.string.category_unlock else R.string.category_lock)) {
                    onDismiss()
                    toggle()
                }
            }
        } else if (page == 1) {
            SettingRow(stringResource(R.string.visibility_only_category), Modifier.focusRequester(first)) { page = 2 }
            SettingRow(stringResource(R.string.visibility_all_categories)) { page = 3 }
        } else if (page == 3) {
            VisibilityList(allGroups, first, isShown = { !isHidden(it) }) { ids, show -> onHidden(ids, !show) }
        } else {
            val hidden = isHidden(id)
            SettingRow(stringResource(R.string.show_categories), Modifier.focusRequester(first), selected = !hidden) {
                onHidden(setOf(id), false)
                onDismiss()
            }
            SettingRow(stringResource(R.string.hide_categories), selected = hidden) {
                onHidden(setOf(id), true)
                onDismiss()
            }
        }
        SettingRow(stringResource(android.R.string.cancel)) { if (page == 0) onDismiss() else page = 0 }
    }
    }
    LaunchedEffect(page) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
}

/**
 * One entry of a category list that can be moved: [content] gets the key modifier. Kept outside the
 * big screen lambdas (a key group with a composable modifier there failed the Android 11 verifier).
 */
@Composable
internal fun MovableEntry(id: Any, active: Boolean, vertical: Boolean, onStep: (Boolean) -> Unit, content: @Composable (Modifier) -> Unit) {
    androidx.compose.runtime.key(id) {
        content(Modifier.moveKeys(active, vertical, onStep) { ChannelEdits.movingCategory = null })
    }
}

/**
 * The menus open while OK is still held (long press): its release and repeats must not choose the
 * first entry. Only a new press of OK inside the menu counts.
 */
@Composable
private fun FreshPressOnly(content: @Composable () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(Modifier.onPreviewKeyEvent { event ->
        val code = event.nativeKeyEvent.keyCode
        val select = code == AndroidKeyEvent.KEYCODE_DPAD_CENTER || code == AndroidKeyEvent.KEYCODE_ENTER ||
            code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER
        when {
            !select || armed && event.nativeKeyEvent.repeatCount == 0 -> false
            event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 -> {
                armed = true
                false
            }
            else -> !armed
        }
    }) { content() }
}

/**
 * Switches for all entries ([entries]: id to label). The top switch "Alle" is on when all are shown;
 * pressing it hides all, or shows all again when one is hidden.
 */
@Composable
private fun VisibilityList(entries: List<Pair<String, String>>, first: FocusRequester, isShown: (String) -> Boolean, onShown: (Set<String>, Boolean) -> Unit) {
    val allShown = entries.all { isShown(it.first) }
    SettingRow(stringResource(R.string.visibility_all), Modifier.focusRequester(first), selected = allShown, eye = true) {
        onShown(entries.map { it.first }.toSet(), !allShown)
    }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.heightIn(max = 340.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        items(entries.size) { index ->
            val (id, label) = entries[index]
            val shown = isShown(id)
            SettingRow(label, selected = shown, eye = true) { onShown(setOf(id), !shown) }
        }
    }
}

/** The channels of [category] for the phone page: name, hidden, the XMLTV name and whether it was changed. */
internal fun encodeChannels(category: String, channels: List<StalkerChannel>, guideName: (StalkerChannel) -> String) =
    com.google.gson.JsonObject().apply {
        addProperty("channelsOf", category)
        add("channels", com.google.gson.JsonArray().apply {
            channels.distinctBy { it.name }.forEach { channel ->
                add(com.google.gson.JsonObject().apply {
                    addProperty("name", channel.name)
                    addProperty("hidden", ChannelEdits.isHidden(channel))
                    addProperty("epg", ChannelEdits.epgName(channel) ?: guideName(channel))
                    addProperty("epgChanged", ChannelEdits.epgName(channel) != null)
                })
            }
        })
    }
