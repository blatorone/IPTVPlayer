package com.iptvplayer.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal enum class CategoryMove { TOP, UP, DOWN, BOTTOM }

internal fun <T> List<T>.moveCategory(item: T, direction: CategoryMove): List<T> {
    val index = indexOf(item)
    if (index < 0) return this
    val destination = when (direction) {
        CategoryMove.TOP -> 0
        CategoryMove.UP -> (index - 1).coerceAtLeast(0)
        CategoryMove.DOWN -> (index + 1).coerceAtMost(lastIndex)
        CategoryMove.BOTTOM -> lastIndex
    }
    return if (destination == index) this else toMutableList().apply { add(destination, removeAt(index)) }
}

@Composable
internal fun CategoryDialog(
    groups: List<PortalGroup>,
    initiallySelected: String?,
    isHidden: (PortalGroup) -> Boolean,
    onApply: (Set<String>, Boolean) -> Unit,
    onMove: (String, CategoryMove) -> Unit,
    onDismiss: () -> Unit,
    isLocked: (PortalGroup) -> Boolean = { false },
    onToggleLock: ((PortalGroup) -> Unit)? = null
) {
    var selection by remember(initiallySelected) {
        mutableStateOf(setOfNotNull(initiallySelected))
    }
    var moving by remember(initiallySelected) { mutableStateOf(false) }
    val selected = selection.singleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manage_categories)) },
        text = {
            Column {
            if (moving && selected != null) {
                Text(groups.firstOrNull { it.id == selected }?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Row {
                    MenuButton(stringResource(R.string.category_move_top), Modifier) { onMove(selected, CategoryMove.TOP) }
                    MenuButton(stringResource(R.string.category_move_up), Modifier) { onMove(selected, CategoryMove.UP) }
                }
                Row {
                    MenuButton(stringResource(R.string.category_move_down), Modifier) { onMove(selected, CategoryMove.DOWN) }
                    MenuButton(stringResource(R.string.category_move_bottom), Modifier) { onMove(selected, CategoryMove.BOTTOM) }
                }
            }
            LazyColumn(Modifier.heightIn(max = if (moving) 145.dp else 245.dp)) {
                items(groups, key = { it.id }) { group ->
                    val hidden = isHidden(group)
                    MenuButton(
                        (if (isLocked(group)) "🔒 " else "") +
                            if (hidden) stringResource(R.string.hidden_category_label, group.name) else group.name,
                        Modifier.fillMaxWidth(),
                        selected = group.id in selection,
                        hiddenIndicator = hidden
                    ) {
                        selection = if (moving) setOf(group.id)
                        else if (group.id in selection) selection - group.id else selection + group.id
                    }
                }
            }
            if (!moving && selected != null) Row {
                MenuButton(stringResource(R.string.category_move), Modifier) { moving = true }
                val group = groups.firstOrNull { it.id == selected }
                if (group != null && onToggleLock != null) MenuButton(
                    stringResource(if (isLocked(group)) R.string.category_unlock else R.string.category_lock), Modifier
                ) { onToggleLock(group) }
            }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onApply(selection, false) }, enabled = selection.isNotEmpty()) {
                    Text(stringResource(R.string.show_categories))
                }
                TextButton(onClick = { onApply(selection, true) }, enabled = selection.isNotEmpty()) {
                    Text(stringResource(R.string.hide_categories))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}

@Composable
internal fun HiddenCategoryIcon() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(18.dp)) {
        drawOval(
            color = color, topLeft = Offset(size.width * 0.08f, size.height * 0.25f),
            size = Size(size.width * 0.84f, size.height * 0.5f), style = Stroke(1.5.dp.toPx())
        )
        drawCircle(color, radius = size.width * 0.11f)
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.84f),
            Offset(size.width * 0.85f, size.height * 0.16f), strokeWidth = 2.dp.toPx())
    }
}

@Composable
internal fun CountryDialog(
    available: Set<String>,
    selected: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_countries)) },
        text = {
            Column {
                Text(stringResource(R.string.country_selection_hint))
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    item {
                        MenuButton(stringResource(R.string.all_countries), Modifier.fillMaxWidth(),
                            selected = draft.isEmpty()) { draft = emptySet() }
                    }
                    items((available + selected).sorted(), key = { it }) { code ->
                        val label = if (code == UNKNOWN_COUNTRY) stringResource(R.string.unknown_country)
                        else countryLabel(code)
                        val rank = draft.indexOf(code)
                        MenuButton(
                            if (rank >= 0) "${rank + 1}. $label" else label,
                            Modifier.fillMaxWidth(),
                            selected = rank >= 0
                        ) { draft = if (code in draft) draft - code else draft + code }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) { Text(stringResource(R.string.apply_selection)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
