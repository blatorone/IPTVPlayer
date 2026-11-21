package com.iptvplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val GUIDE_INTERVAL = 1_800L
private const val GUIDE_WINDOW = 4 * GUIDE_INTERVAL

@Composable
internal fun ProgrammeTimeline(
    channels: List<StalkerChannel>,
    pinnedIds: Set<String>,
    highlighted: StalkerChannel?,
    programmes: Map<String, List<PortalProgramme>>,
    programmeKey: (StalkerChannel) -> String?,
    scroll: LazyListState,
    onHighlight: (StalkerChannel) -> Unit,
    onPlay: (StalkerChannel) -> Unit,
    onTogglePin: (StalkerChannel) -> Unit
) {
    var onlyPinned by rememberSaveable { mutableStateOf(false) }
    var offset by rememberSaveable { mutableStateOf(0L) }
    var details by remember { mutableStateOf<Pair<StalkerChannel, PortalProgramme?>?>(null) }
    val now by produceState(Instant.now().epochSecond) {
        while (true) {
            delay(30_000)
            value = Instant.now().epochSecond
        }
    }
    val start = now - now % GUIDE_INTERVAL + offset
    val displayed = remember(channels, pinnedIds, onlyPinned) {
        if (onlyPinned) channels.filter { programmeKey(it) in pinnedIds } else channels
    }
    LaunchedEffect(displayed, highlighted) {
        if (highlighted !in displayed) displayed.firstOrNull()?.let(onHighlight)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        MenuButton(stringResource(R.string.guide_previous_window), Modifier) { offset -= GUIDE_WINDOW }
        MenuButton(stringResource(R.string.guide_now), Modifier) { offset = 0 }
        MenuButton(stringResource(R.string.guide_next_window), Modifier) { offset += GUIDE_WINDOW }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(guideDate(start), style = MaterialTheme.typography.titleMedium)
        MenuButton(
            stringResource(R.string.guide_pinned_only), Modifier, selected = onlyPinned
        ) { onlyPinned = !onlyPinned }
    }
    if (displayed.isEmpty()) {
        Text(stringResource(if (onlyPinned) R.string.guide_no_pinned else R.string.no_channels))
        return
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(stringResource(R.string.channels), Modifier.weight(1f).padding(start = 8.dp))
        Row(Modifier.weight(3f)) {
            repeat(4) { index ->
                Text(
                    guideTime(start + index * GUIDE_INTERVAL),
                    Modifier.weight(1f).padding(start = 4.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
    LazyColumn(
        Modifier.heightIn(max = 380.dp), state = scroll,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        items(displayed) { channel ->
            val schedule = programmes[programmeKey(channel)]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MenuButton(
                    channel.name, Modifier.weight(1f), selected = channel === highlighted,
                    logo = channel.logo, onFocused = { onHighlight(channel) }
                ) {
                    onHighlight(channel)
                    details = channel to null
                }
                TimelineRow(
                    Modifier.weight(3f), schedule, start, now,
                    loading = channel === highlighted && schedule == null,
                    onFocus = { onHighlight(channel) },
                    onProgramme = { programme ->
                        onHighlight(channel)
                        details = channel to programme
                    }
                )
            }
        }
    }
    details?.let { (channel, programme) ->
        AlertDialog(
            onDismissRequest = { details = null },
            title = { Text(programme?.title ?: channel.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (programme != null) {
                        Text(channel.name)
                        programme.startSeconds?.let { from ->
                            val time = listOfNotNull(guideTime(from), programme.endSeconds?.let(::guideTime))
                            Text(time.joinToString(" – "))
                        }
                        programme.description?.let { Text(it) }
                    }
                    if (channel.id != null) {
                        MenuButton(
                            stringResource(if (programmeKey(channel) in pinnedIds) R.string.epg_unpin else R.string.epg_pin),
                            Modifier
                        ) { onTogglePin(channel) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { details = null; onPlay(channel) }) {
                    Text(stringResource(R.string.play_selected_channel))
                }
            },
            dismissButton = {
                TextButton(onClick = { details = null }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TimelineRow(
    modifier: Modifier,
    schedule: List<PortalProgramme>?,
    start: Long,
    now: Long,
    loading: Boolean,
    onFocus: () -> Unit,
    onProgramme: (PortalProgramme) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val end = start + GUIDE_WINDOW
    BoxWithConstraints(modifier.height(48.dp).background(colors.surfaceContainer)) {
        val width = maxWidth
        val timed = schedule.orEmpty().filter { it.startSeconds != null }.sortedBy { it.startSeconds }
        var drawn = false
        for ((index, programme) in timed.withIndex()) {
            val from = programme.startSeconds ?: continue
            val stop = programme.endSeconds?.takeIf { it > from }
                ?: timed.getOrNull(index + 1)?.startSeconds?.takeIf { it > from }
                ?: (from + GUIDE_INTERVAL)
            if (from >= end || stop <= start) continue
            drawn = true
            val left = (maxOf(from, start) - start).toFloat() / GUIDE_WINDOW
            val span = (minOf(stop, end) - maxOf(from, start)).toFloat() / GUIDE_WINDOW
            ProgrammeCell(
                programme, Modifier.offset(x = width * left).width(width * span).fillMaxHeight(),
                current = now in from until stop, onFocus = onFocus, onSelect = onProgramme
            )
        }
        if (!drawn) {
            val untimed = schedule?.firstOrNull { it.startSeconds == null }?.takeIf { now in start until end }
            if (untimed != null) {
                ProgrammeCell(untimed, Modifier.fillMaxWidth().fillMaxHeight(), false, onFocus, onProgramme)
            } else {
                Text(
                    stringResource(if (loading) R.string.guide_loading else R.string.no_guide),
                    Modifier.align(Alignment.CenterStart).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (now in start until end) {
            Box(
                Modifier.offset(x = width * ((now - start).toFloat() / GUIDE_WINDOW))
                    .width(1.dp).fillMaxHeight().background(colors.primary)
            )
        }
    }
}

@Composable
private fun ProgrammeCell(
    programme: PortalProgramme,
    modifier: Modifier,
    current: Boolean,
    onFocus: () -> Unit,
    onSelect: (PortalProgramme) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.padding(end = 2.dp)
            .background(if (focused) colors.tertiaryContainer else if (current) {
                colors.secondaryContainer
            } else colors.surface)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocus()
                focused = it.isFocused
            }
            .clickable { onSelect(programme) }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            programme.title, style = MaterialTheme.typography.labelMedium,
            color = if (focused) colors.onTertiaryContainer else colors.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

private fun guideTime(seconds: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))

private fun guideDate(seconds: Long): String =
    DateTimeFormatter.ofPattern("EEE, dd.MM.").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))
