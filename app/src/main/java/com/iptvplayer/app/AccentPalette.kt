package com.iptvplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Colour palette for the accent colour with an example screen beside it. The example shows
 * the colour that has focus (or was tapped last); OK or a tap applies it.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AccentPalette(current: AccentChoice, onChoose: (AccentChoice) -> Unit) {
    var preview by remember { mutableStateOf(current) }
    // Focus starts on the colour that is set, so the remote can move through the palette at once.
    val currentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { currentFocus.requestFocus() }
    }
    Text(stringResource(R.string.accent_palette_hint), style = MaterialTheme.typography.bodySmall)
    @Composable
    fun swatch(choice: AccentChoice) = AccentSwatch(
        choice, selected = choice == current, onFocus = { preview = choice },
        modifier = if (choice == current) Modifier.focusRequester(currentFocus) else Modifier
    ) {
        preview = choice
        onChoose(choice)
    }
    // Beside the example: five colours per row (as before, the example keeps its width);
    // the remaining colours continue below it over the full width.
    val besidePreview = AccentChoice.entries.take(20)
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            besidePreview.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { swatch(it) } }
            }
        }
        MaterialTheme(colorScheme = playerColors(preview)) {
            AccentPreview(Modifier.weight(1f))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AccentChoice.entries.drop(besidePreview.size).forEach { swatch(it) }
    }
    Text(
        stringResource(preview.label) +
            if (preview == current) " · " + stringResource(R.string.accent_palette_active) else "",
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun AccentSwatch(
    choice: AccentChoice, selected: Boolean, onFocus: () -> Unit, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val name = stringResource(choice.label)
    Box(
        Modifier.size(50.dp)
            .border(
                width = if (focused) 3.dp else 2.dp,
                color = when {
                    focused -> Color.White
                    selected -> Color.White.copy(alpha = 0.55f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .padding(5.dp)
            .then(modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .semantics {
                contentDescription = name
                this.selected = selected
            }
            .clip(CircleShape)
            .background(choice.color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Text("✓", color = choice.onColor, style = MaterialTheme.typography.titleMedium)
    }
}

/** A small, non-interactive picture of the app in the chosen colour. */
@Composable
private fun AccentPreview(modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = Color(0xFF0B0B0D)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.accent_palette_example), style = MaterialTheme.typography.labelSmall, color = colors.primary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StreamWatchLogo(18.dp)
                Box(Modifier.width(3.dp).height(18.dp).background(colors.primary, RoundedCornerShape(2.dp)))
                PreviewPill(stringResource(R.string.destination_live), active = true)
                PreviewPill(stringResource(R.string.destination_movies), active = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp, 42.dp).clip(RoundedCornerShape(8.dp)).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = colors.onPrimaryContainer) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("TV Sender", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    LinearProgressIndicator(
                        progress = { 0.62f }, modifier = Modifier.fillMaxWidth(),
                        color = colors.primary, trackColor = Color(0xFF2C2C33)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(colors.primary).padding(horizontal = 14.dp, vertical = 7.dp)
                ) { Text(stringResource(R.string.play_movie), color = colors.onPrimary, style = MaterialTheme.typography.labelLarge) }
                Box(
                    Modifier.border(2.dp, colors.primary, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 7.dp)
                ) { Text(stringResource(R.string.watchlist), color = Color.White, style = MaterialTheme.typography.labelLarge) }
                Box(
                    Modifier.size(40.dp, 22.dp).clip(CircleShape).background(colors.primary).padding(3.dp),
                    contentAlignment = Alignment.CenterEnd
                ) { Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White)) }
            }
        }
    }
}

@Composable
private fun PreviewPill(text: String, active: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(if (active) colors.primary else Color(0xFF17171B))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (active) colors.onPrimary else Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
