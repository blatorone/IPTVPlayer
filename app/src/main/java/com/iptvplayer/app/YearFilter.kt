package com.iptvplayer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** A year or a range of years, both ends included. */
internal data class YearSpan(val from: Int, val to: Int) {
    fun contains(year: Int?): Boolean = year != null && year in from..to
    fun label(): String = if (from == to) "$from" else "$from–$to"
}

/**
 * The year filter used by every title list: all years, a single year from the list, or a
 * custom range chosen with two remote-friendly pickers ("Von", "Bis").
 */
@Composable
internal fun YearFilterButton(years: List<Int>, value: YearSpan?, onChange: (YearSpan?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(false) }
    Box {
        MenuButton(value?.label() ?: stringResource(R.string.all_years), Modifier) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 420.dp)) {
            DropdownMenuItem(text = { Text(stringResource(R.string.all_years)) }, onClick = {
                onChange(null)
                open = false
            })
            DropdownMenuItem(text = { Text(stringResource(R.string.custom_years)) }, onClick = {
                open = false
                custom = true
            })
            years.forEach { year ->
                DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                    onChange(YearSpan(year, year))
                    open = false
                })
            }
        }
    }
    if (custom) YearSpanDialog(years, value, onApply = {
        onChange(it)
        custom = false
    }, onDismiss = { custom = false })
}

@Composable
private fun YearSpanDialog(years: List<Int>, value: YearSpan?, onApply: (YearSpan) -> Unit, onDismiss: () -> Unit) {
    val current = java.time.Year.now().value
    val newest = maxOf(years.maxOrNull() ?: current, current)
    val oldest = minOf(years.minOrNull() ?: 1950, newest)
    val options = (newest downTo oldest).toList()
    var from by remember { mutableStateOf(value?.from ?: oldest) }
    var to by remember { mutableStateOf(value?.to ?: newest) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_years)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                YearPicker(stringResource(R.string.year_from, from), options) { from = it }
                YearPicker(stringResource(R.string.year_to, to), options) { to = it }
            }
        },
        confirmButton = {
            MenuButton(stringResource(R.string.apply_filter), Modifier) { onApply(YearSpan(minOf(from, to), maxOf(from, to))) }
        },
        dismissButton = { MenuButton(stringResource(R.string.close), Modifier) { onDismiss() } }
    )
}

@Composable
private fun YearPicker(label: String, options: List<Int>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        MenuButton(label, Modifier) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 420.dp)) {
            options.forEach { year ->
                DropdownMenuItem(text = { Text(year.toString()) }, onClick = {
                    onSelect(year)
                    open = false
                })
            }
        }
    }
}
