package com.iptvplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val GUIDE_INTERVAL = 1_800L
private const val GUIDE_WINDOW = 4 * GUIDE_INTERVAL
private data class GuideSlot(val programme: PortalProgramme, val from: Long, val stop: Long)

@Composable
internal fun ProgrammeTimeline(
    channels: List<StalkerChannel>,
    pinnedIds: Set<String>,
    highlighted: StalkerChannel?,
    playing: StalkerChannel?,
    programmes: (StalkerChannel) -> List<PortalProgramme>?,
    programmeKey: (StalkerChannel) -> String?,
    scroll: LazyListState,
    compact: Boolean,
    refreshKey: Int,
    horizonHours: Int,
    active: Boolean,
    channelColumnFocus: FocusRequester,
    onChannelLeft: () -> Unit,
    onVisibleChannels: suspend (List<StalkerChannel>) -> Unit,
    onHighlight: (StalkerChannel) -> Unit,
    onProgrammeFocus: (StalkerChannel, PortalProgramme) -> Unit,
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
    val latest = ((horizonHours * 3_600L - 1) / GUIDE_WINDOW) * GUIDE_WINDOW
    val channelEntry = Modifier.focusProperties {
        canFocus = active
        down = channelColumnFocus
    }
    val rowHeight = if (compact) 36.dp else 48.dp
    val displayed = remember(channels, pinnedIds, onlyPinned) {
        if (onlyPinned) channels.filter { programmeKey(it) in pinnedIds } else channels
    }
    val loadVisible by rememberUpdatedState(onVisibleChannels)
    LaunchedEffect(displayed, scroll, refreshKey) {
        snapshotFlow { scroll.layoutInfo.visibleItemsInfo.map { it.index } }
            .collectLatest { indices ->
                val first = indices.firstOrNull() ?: return@collectLatest
                val last = indices.last()
                delay(180)
                loadVisible(displayed.subList((first - 2).coerceIn(0, displayed.size), (last + 4).coerceIn(0, displayed.size)))
            }
    }
    LaunchedEffect(displayed, highlighted) {
        if (highlighted !in displayed) displayed.firstOrNull()?.let(onHighlight)
    }
    LaunchedEffect(active, displayed.isEmpty()) {
        if (active && displayed.isNotEmpty()) {
            withFrameNanos { }
            channelColumnFocus.requestFocus()
        }
    }
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(guideDate(start), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        MenuButton(stringResource(R.string.guide_previous_window), channelEntry) { offset = (offset - GUIDE_WINDOW).coerceAtLeast(-GUIDE_WINDOW) }
        MenuButton(stringResource(R.string.guide_now), channelEntry) { offset = 0 }
        MenuButton(stringResource(R.string.guide_next_window), channelEntry) {
            offset = (offset + GUIDE_WINDOW).coerceAtMost(latest)
        }
        MenuButton(
            stringResource(R.string.guide_pinned_only), channelEntry, selected = onlyPinned
        ) { onlyPinned = !onlyPinned }
    }
    if (displayed.isEmpty()) {
        Text(stringResource(if (onlyPinned) R.string.guide_no_pinned else R.string.no_channels))
        return@Column
    }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = if (compact) 3.dp else 6.dp)
    ) {
        Text(
            stringResource(R.string.channels), Modifier.weight(1.25f).padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
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
        Modifier.weight(1f), state = scroll,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(displayed) { index, channel ->
            val schedule = programmes(channel)
            val channelFocus = remember { FocusRequester() }
            val timelineFocus = remember { FocusRequester() }
            val entryFocus = if (index == scroll.firstVisibleItemIndex) Modifier.focusRequester(channelColumnFocus) else Modifier
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GuideChannelCell(
                    channel, channel.number ?: index + 1L,
                    Modifier.weight(1.25f).then(entryFocus).focusRequester(channelFocus)
                        .focusProperties { canFocus = active }
                        .onHorizontalRemote { right ->
                            if (right) timelineFocus.requestFocus() else onChannelLeft()
                        },
                    channel === highlighted, channel === playing,
                    rowHeight = rowHeight,
                    compact = compact,
                    onFocus = { onHighlight(channel) }
                ) {
                    onHighlight(channel)
                    onPlay(channel)
                }
                TimelineRow(
                    Modifier.weight(3f), schedule, start, now, offset,
                    rowHeight = rowHeight,
                    compact = compact,
                    loading = schedule == null && programmeKey(channel) != null,
                    active = active,
                    channelFocus = channelFocus,
                    timelineFocus = timelineFocus,
                    canPageRight = offset < latest,
                    onPage = { right -> offset += if (right) GUIDE_WINDOW else -GUIDE_WINDOW },
                    onFocus = { programme -> onProgrammeFocus(channel, programme) },
                    onProgramme = { programme ->
                        onProgrammeFocus(channel, programme)
                        details = channel to programme
                    }
                )
            }
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
private fun GuideChannelCell(
    channel: StalkerChannel,
    number: Long,
    modifier: Modifier,
    highlighted: Boolean,
    playing: Boolean,
    rowHeight: Dp,
    compact: Boolean,
    onFocus: () -> Unit,
    onPlay: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.height(rowHeight)
            .padding(end = 2.dp)
            .background(when {
                focused -> colors.tertiaryContainer
                playing -> colors.primaryContainer
                highlighted -> colors.secondaryContainer
                else -> colors.surfaceContainerHigh
            })
            .onFocusChanged {
                if (it.isFocused && !focused) onFocus()
                focused = it.isFocused
            }
            .clickable(onClick = onPlay)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(number.toString(), Modifier.width(if (compact) 24.dp else 32.dp), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        ChannelLogo(
            channel.logo,
            Modifier.width(if (compact) 22.dp else 28.dp).height(if (compact) 18.dp else 24.dp)
        )
        Text(
            channel.name,
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TimelineRow(
    modifier: Modifier,
    schedule: List<PortalProgramme>?,
    start: Long,
    now: Long,
    offset: Long,
    rowHeight: Dp,
    compact: Boolean,
    loading: Boolean,
    active: Boolean,
    channelFocus: FocusRequester,
    timelineFocus: FocusRequester,
    canPageRight: Boolean,
    onPage: (Boolean) -> Unit,
    onFocus: (PortalProgramme) -> Unit,
    onProgramme: (PortalProgramme) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val end = start + GUIDE_WINDOW
    val slots = remember(schedule, start, offset) {
        val timed = schedule.orEmpty().filter { it.startSeconds != null }.sortedBy { it.startSeconds }
        val timedSlots = timed.mapIndexedNotNull { index, programme ->
            val from = programme.startSeconds ?: return@mapIndexedNotNull null
            val stop = programme.endSeconds?.takeIf { it > from }
                ?: timed.getOrNull(index + 1)?.startSeconds?.takeIf { it > from }
                ?: (from + GUIDE_INTERVAL)
            GuideSlot(programme, from, stop).takeIf { from < end && stop > start }
        }
        if (timedSlots.isNotEmpty() || offset != 0L) timedSlots
        else schedule?.firstOrNull { it.startSeconds == null }
            ?.let { listOf(GuideSlot(it, start, end)) }.orEmpty()
    }
    val entryIndex = if (offset == 0L) {
        slots.indexOfFirst { it.stop > now }.takeIf { it >= 0 } ?: slots.lastIndex.coerceAtLeast(0)
    } else 0
    val cellFocus = remember(slots.size) { List(slots.size.coerceAtLeast(1)) { FocusRequester() } }
    var pageTarget by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(start, pageTarget) {
        val forward = pageTarget ?: return@LaunchedEffect
        withFrameNanos { }
        val index = if (forward || offset == 0L) entryIndex else cellFocus.lastIndex
        cellFocus[index].requestFocus()
        pageTarget = null
    }
    fun move(index: Int, right: Boolean) {
        when {
            right && index < cellFocus.lastIndex -> cellFocus[index + 1].requestFocus()
            right && canPageRight -> {
                pageTarget = true
                onPage(true)
            }
            !right && offset == 0L && index <= entryIndex -> channelFocus.requestFocus()
            !right && index > 0 -> cellFocus[index - 1].requestFocus()
            !right && offset > 0 -> {
                pageTarget = false
                onPage(false)
            }
            !right -> channelFocus.requestFocus()
        }
    }
    fun cellModifier(index: Int): Modifier {
        val entry = if (index == entryIndex) Modifier.focusRequester(timelineFocus) else Modifier
        return entry.focusRequester(cellFocus[index]).focusProperties { canFocus = active }
            .onHorizontalRemote { move(index, it) }
    }
    BoxWithConstraints(modifier.height(rowHeight).background(colors.surfaceContainer)) {
        val width = maxWidth
        for ((index, slot) in slots.withIndex()) {
            val (programme, from, stop) = slot
            val left = (maxOf(from, start) - start).toFloat() / GUIDE_WINDOW
            val span = (minOf(stop, end) - maxOf(from, start)).toFloat() / GUIDE_WINDOW
            ProgrammeCell(
                programme, Modifier.offset(x = width * left).width(width * span).fillMaxHeight()
                    .then(cellModifier(index)),
                current = now in from until stop, compact = compact,
                onFocus = { onFocus(programme) }, onSelect = onProgramme
            )
        }
        if (slots.isEmpty()) {
            var focused by remember { mutableStateOf(false) }
            Box(
                Modifier.fillMaxWidth().fillMaxHeight().then(cellModifier(0))
                    .background(if (focused) colors.tertiaryContainer else colors.surfaceContainer)
                    .onFocusChanged { focused = it.isFocused }.focusable(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    stringResource(if (loading) R.string.guide_loading else R.string.no_guide),
                    Modifier.padding(horizontal = 8.dp),
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
    compact: Boolean,
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
            programme.title, style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = if (focused) colors.onTertiaryContainer else colors.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

private fun guideTime(seconds: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))

private fun guideDate(seconds: Long): String =
    DateTimeFormatter.ofPattern("EEE, dd.MM.").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))
