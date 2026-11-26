package com.iptvplayer.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Remembers, per settings section, which row had focus last. Entering the rows from the section
 * bar (or after a page closes) returns there; the first time it is the top row. Reset whenever
 * the settings are opened anew.
 */
internal class SettingsFocusMemory {
    /** The section whose rows are shown. */
    var section: Any? = null
    /** While a settings page is open its rows are not remembered: closing it returns to the row that opened it. */
    var pageOpen: Boolean = false
    private val lastRow = mutableMapOf<Any, String>()
    private val rows = mutableMapOf<String, FocusRequester>()

    fun register(key: String, requester: FocusRequester) { rows[key] = requester }
    fun unregister(key: String, requester: FocusRequester) { if (rows[key] === requester) rows.remove(key) }
    fun focused(key: String) { if (!pageOpen) section?.let { lastRow[it] = key } }
    fun target(): FocusRequester? = section?.let(lastRow::get)?.let(rows::get)
    fun reset() { lastRow.clear() }
}

internal val LocalSettingsFocusMemory = staticCompositionLocalOf<SettingsFocusMemory?> { null }

/**
 * One line of a settings page. With [selected] it is an on/off switch; otherwise it opens or
 * cycles something, and a "Name: Wert" label shows its value on the right.
 */
@Composable
internal fun SettingRow(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val split = label.indexOf(": ").takeIf { selected == null && it > 0 }
    val title = split?.let { label.substring(0, it) } ?: label
    val value = split?.let { label.substring(it + 2) }
    // The title stays the same when a value row is switched, so the row is still found again.
    val memory = LocalSettingsFocusMemory.current
    val requester = remember { FocusRequester() }
    if (memory != null) DisposableEffect(memory, title) {
        memory.register(title, requester)
        onDispose { memory.unregister(title, requester) }
    }
    Surface(
        modifier = modifier.fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) memory?.focused(title)
            }
            .focusProperties { canFocus = enabled }
            .clickable(enabled = enabled, onClick = onClick),
        color = if (focused) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        contentColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, accentBrush()) else null
    ) {
        Row(
            Modifier.heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            when {
                selected != null -> SettingSwitch(selected)
                value != null -> Text(
                    value, style = MaterialTheme.typography.labelLarge, color = colors.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                else -> Text("›", style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
            }
        }
    }
}

/**
 * A switch that is its own focus target, for rows whose main click does something else
 * (e.g. a portal: the row opens it, the switch turns it on or off).
 */
@Composable
internal fun SettingToggleButton(on: Boolean, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .focusProperties { canFocus = enabled }
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        color = if (focused) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, accentBrush()) else null
    ) {
        Box(Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            SettingSwitch(on)
        }
    }
}

/** Display-only switch; the whole row is the focus target on TV. */
@Composable
private fun SettingSwitch(on: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.width(44.dp).height(24.dp)
            .then(if (on) Modifier.background(accentBrush(), RoundedCornerShape(50)) else Modifier.background(colors.outlineVariant, RoundedCornerShape(50))),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.padding(3.dp).size(18.dp).background(Color.White, RoundedCornerShape(50)))
    }
}

/** Entry of the settings navigation on the left. */
@Composable
internal fun SettingsNavItem(
    label: String,
    icon: Int,
    selected: Boolean,
    badge: Int?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick),
        color = when {
            selected -> colors.primary.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        contentColor = if (selected || focused) colors.onSurface else colors.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, accentBrush()) else null
    ) {
        Row(
            Modifier.heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painterResource(icon), contentDescription = null, Modifier.size(22.dp),
                tint = if (selected) colors.primary else colors.onSurfaceVariant
            )
            Text(
                label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            badge?.let {
                Box(
                    Modifier.background(colors.primary, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 1.dp)
                ) { Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onPrimary) }
            }
        }
    }
}

/** Card holding the rows of one settings page. */
@Composable
internal fun SettingsPage(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

/** True inside the right-hand settings panel: settings pages render there instead of as a popup. */
internal val LocalSettingsInline = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * A settings page. In the settings panel it replaces the rows of the section (title, rows,
 * buttons below; "Zurück" returns to the section); elsewhere it is a dialog.
 */
@Composable
internal fun SettingsDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (LocalSettingsInline.current) {
        val entry = remember { androidx.compose.ui.focus.FocusRequester() }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            androidx.compose.runtime.withFrameNanos { }
            runCatching { entry.requestFocus() }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Column(
                Modifier.fillMaxWidth().focusRequester(entry).focusGroup(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                content()
                buttons()
            }
        }
    } else androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) { content() }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                buttons()
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    )
}
