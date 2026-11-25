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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    Surface(
        modifier = modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .focusProperties { canFocus = enabled }
            .clickable(enabled = enabled, onClick = onClick),
        color = if (focused) colors.surfaceContainerHighest else colors.surfaceContainerHigh,
        contentColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
        shape = RoundedCornerShape(12.dp),
        border = if (focused) BorderStroke(2.dp, colors.primary) else null
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

/** Display-only switch; the whole row is the focus target on TV. */
@Composable
private fun SettingSwitch(on: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.width(44.dp).height(24.dp)
            .background(if (on) colors.primary else colors.outlineVariant, RoundedCornerShape(50)),
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
        border = if (focused) BorderStroke(2.dp, colors.primary) else null
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
