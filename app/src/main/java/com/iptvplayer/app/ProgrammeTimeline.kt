package com.iptvplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

private const val GUIDE_INTERVAL = 1_800L
private const val GUIDE_WINDOW = 4 * GUIDE_INTERVAL

/** Time window and filter of the guide; shared by the grid and the controls in the top bar. */
internal class GuideWindow(offset: Long = 0L, onlyPinned: Boolean = false) {
    var offset by mutableStateOf(offset)
    var onlyPinned by mutableStateOf(onlyPinned)

    companion object {
        val Saver = listSaver<GuideWindow, Any>(
            save = { listOf(it.offset, it.onlyPinned) },
            restore = { GuideWindow(it[0] as Long, it[1] as Boolean) }
        )
    }
}

@Composable
internal fun rememberGuideWindow(): GuideWindow = rememberSaveable(saver = GuideWindow.Saver) { GuideWindow() }

/**
 * Portals often deliver overlapping or duplicated entries (the same show twice with shifted
 * times, or a long block covering the next shows). The guide shows one clean sequence: equal
 * starts collapse to the entry with the most information, and every show ends at the latest
 * where the next one begins. Entries without a start time stay at the end unchanged.
 */
internal fun normalizeSchedule(schedule: List<PortalProgramme>): List<PortalProgramme> {
    val timed = schedule.filter { it.startSeconds != null }
    if (timed.size < 2) return schedule
    val unique = timed.groupBy { it.startSeconds!! }.toSortedMap().values.map { same ->
        same.maxWith(compareBy<PortalProgramme>({ it.title.isNotBlank() }, { it.description?.length ?: 0 }, { it.endSeconds ?: 0 }))
    }
    val cleaned = unique.mapIndexed { index, programme ->
        val next = unique.getOrNull(index + 1)?.startSeconds
        val end = programme.endSeconds
        if (next != null && (end == null || end > next)) programme.copy(endSeconds = next) else programme
    }
    return cleaned + schedule.filter { it.startSeconds == null }
}

private fun latestGuideOffset(horizonHours: Int): Long = ((horizonHours * 3_600L - 1) / GUIDE_WINDOW) * GUIDE_WINDOW

/** Früher / Jetzt / Später / Nur vorgemerkte, shown in the top bar next to the category. */
@Composable
internal fun GuideWindowControls(window: GuideWindow, horizonHours: Int, entry: Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        MenuButton(stringResource(R.string.guide_previous_window), entry) {
            window.offset = (window.offset - GUIDE_WINDOW).coerceAtLeast(-GUIDE_WINDOW)
        }
        MenuButton(stringResource(R.string.guide_now), entry, selected = window.offset == 0L) { window.offset = 0 }
        MenuButton(stringResource(R.string.guide_next_window), entry) {
            window.offset = (window.offset + GUIDE_WINDOW).coerceAtMost(latestGuideOffset(horizonHours))
        }
        MenuButton(stringResource(R.string.guide_pinned_only), entry, selected = window.onlyPinned) {
            window.onlyPinned = !window.onlyPinned
        }
    }
}
private data class GuideSlot(val programme: PortalProgramme, val from: Long, val stop: Long)

@Composable
internal fun ProgrammeTimeline(
    channels: List<StalkerChannel>,
    pinnedIds: Set<String>,
    highlighted: StalkerChannel?,
    playing: StalkerChannel?,
    focusTarget: StalkerChannel?,
    programmes: (StalkerChannel) -> List<PortalProgramme>?,
    programmeKey: (StalkerChannel) -> String?,
    scroll: LazyListState,
    compact: Boolean,
    appearance: GuideAppearance,
    refreshKey: Int,
    horizonHours: Int,
    window: GuideWindow,
    active: Boolean,
    channelColumnFocus: FocusRequester,
    restorePlayingFocus: Boolean,
    onPlayingFocusRestored: () -> Unit,
    onChannelLeft: () -> Unit,
    onVisibleChannels: suspend (List<StalkerChannel>) -> Unit,
    onHighlight: (StalkerChannel) -> Unit,
    onProgrammeFocus: (StalkerChannel, PortalProgramme) -> Unit,
    onPlay: (StalkerChannel) -> Unit,
    onTogglePin: (StalkerChannel) -> Unit
) {
    var onlyPinned by window::onlyPinned
    var offset by window::offset
    var details by remember { mutableStateOf<Pair<StalkerChannel, PortalProgramme?>?>(null) }
    val now by produceState(Instant.now().epochSecond) {
        while (true) {
            delay(30_000)
            value = Instant.now().epochSecond
        }
    }
    val start = now - now % GUIDE_INTERVAL + offset
    val latest = latestGuideOffset(horizonHours)
    val rowHeight = (if (compact) 34.dp else 42.dp) * appearance.rowSize.factor
    val displayed = remember(channels, pinnedIds, onlyPinned) {
        if (onlyPinned) channels.filter { programmeKey(it) in pinnedIds } else channels
    }
    val playingIndex = if (restorePlayingFocus) displayed.indexOf(focusTarget) else -1
    val playingFocus = remember { FocusRequester() }
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
    LaunchedEffect(active, restorePlayingFocus, focusTarget) {
        if (active && restorePlayingFocus) {
            offset = 0
            if (focusTarget in channels && focusTarget !in displayed) onlyPinned = false
        }
    }
    LaunchedEffect(active, playingIndex) {
        if (active && playingIndex >= 0) {
            val layout = snapshotFlow { scroll.layoutInfo }.first { it.totalItemsCount > playingIndex }
            if (layout.visibleItemsInfo.none { it.index == playingIndex }) scroll.scrollToItem(playingIndex)
        }
    }
    LaunchedEffect(active, displayed.isEmpty()) {
        if (active && displayed.isNotEmpty() && playingIndex < 0) {
            withFrameNanos { }
            if (channelColumnFocus.requestFocus() && restorePlayingFocus && focusTarget !in channels) {
                onPlayingFocusRestored()
            }
        }
    }
    // One "now" line through the whole guide, from the row of times at the top down; the time
    // already gone (left of it) lies under a dark veil.
    val nowFraction = ((now - start).toFloat() / GUIDE_WINDOW).takeIf { now >= start && now < start + GUIDE_WINDOW }
    val nowColor = appearance.nowColor.color
    // "Linienfarbe": darkened and lightly tinted in the line's colour, so the time gone is clearly
    // set apart but still readable; "Dunkel": darkened only.
    val veil = when (appearance.veil) {
        GuideVeil.LINE_COLOR -> nowColor.copy(alpha = 0.14f).compositeOver(Color.Black.copy(alpha = 0.40f))
        GuideVeil.DARK -> Color.Black.copy(alpha = 0.45f)
        GuideVeil.OFF -> null
    }
    Column(
        Modifier.fillMaxHeight().drawWithContent {
            drawContent()
            if (nowFraction != null && displayed.isNotEmpty()) {
                val timelineStart = size.width * (1.25f / 4.25f)
                val x = timelineStart + (size.width - timelineStart) * nowFraction
                if (veil != null) drawRect(veil, Offset(timelineStart, 0f), Size(x - timelineStart, size.height))
                val line = 2.dp.toPx()
                drawRect(nowColor, Offset(x - line / 2, 0f), Size(line, size.height))
            }
        },
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
    if (displayed.isEmpty()) {
        Text(stringResource(if (onlyPinned) R.string.guide_no_pinned else R.string.no_channels))
        return@Column
    }
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = if (compact) 3.dp else 6.dp)
    ) {
        // As in a classic TV guide the ruler carries the date where the channel column starts.
        Text(
            guideDate(start, now), Modifier.weight(1.25f).padding(start = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
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
            var focusTimelineRequest by remember { mutableIntStateOf(0) }
            LaunchedEffect(focusTimelineRequest) {
                if (focusTimelineRequest > 0) {
                    withFrameNanos { }
                    timelineFocus.requestFocus()
                }
            }
            val entryFocus = if (index == scroll.firstVisibleItemIndex) Modifier.focusRequester(channelColumnFocus) else Modifier
            val restoreFocus = if (active && index == playingIndex) {
                LaunchedEffect(channel) {
                    withFrameNanos { }
                    if (playingFocus.requestFocus()) onPlayingFocusRestored()
                }
                Modifier.focusRequester(playingFocus)
            } else Modifier
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GuideChannelCell(
                    channel, channel.number ?: index + 1L,
                    Modifier.weight(1.25f).then(entryFocus).then(restoreFocus).focusRequester(channelFocus)
                        .focusProperties {
                            canFocus = active
                            right = timelineFocus
                        }
                        .onHorizontalRemote { right ->
                            if (right) {
                                // The guide row is inside a LazyColumn; requesting focus on the
                                // next frame can lose the key event when that row is rebound.
                                // Try the explicit target immediately and retain the deferred
                                // request as a fallback for rows that are still being composed.
                                if (!timelineFocus.requestFocus()) focusTimelineRequest++
                            } else onChannelLeft()
                        },
                    channel === highlighted, channel === playing,
                    rowHeight = rowHeight,
                    compact = compact,
                    appearance = appearance,
                    onFocus = { onHighlight(channel) }
                ) {
                    onHighlight(channel)
                    onPlay(channel)
                }
                TimelineRow(
                    Modifier.weight(3f), schedule, start, now, offset,
                    rowHeight = rowHeight,
                    compact = compact,
                    textScale = appearance.textSize.factor,
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
    appearance: GuideAppearance,
    onFocus: () -> Unit,
    onPlay: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val textColor = androidx.compose.material3.LocalContentColor.current
    Row(
        modifier.height(rowHeight)
            .padding(end = 2.dp)
            .background(when {
                focused -> colors.tertiaryContainer
                playing -> colors.surfaceContainerHigh
                highlighted -> colors.secondaryContainer
                else -> colors.surfaceContainerHigh
            })
            .then(
                when {
                    focused -> Modifier.border(BorderStroke(2.dp, accentBrush()), RoundedCornerShape(4.dp))
                    playing -> Modifier.border(BorderStroke(1.5.dp, colors.primary), RoundedCornerShape(4.dp))
                    else -> Modifier
                }
            )
            .onFocusChanged {
                if (it.isFocused && !focused) onFocus()
                focused = it.isFocused
            }
            .clickable(onClick = onPlay)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appearance.showNumbers) Text(
            number.toString(), Modifier.width(if (compact) 24.dp else 32.dp),
            style = MaterialTheme.typography.labelSmall.scaled(appearance.textSize.factor), maxLines = 1
        )
        if (appearance.showLogos) ChannelLogo(
            channel.logo,
            Modifier.width(if (compact) 22.dp else 28.dp).height(if (compact) 18.dp else 24.dp)
        )
        Text(
            channel.name,
            Modifier.weight(1f),
            style = (if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium)
                .scaled(appearance.textSize.factor),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        // The running channel gets a small play symbol in the text colour, besides its frame, at the right edge.
        if (playing) androidx.compose.foundation.Canvas(Modifier.width(if (compact) 8.dp else 10.dp).height(if (compact) 10.dp else 12.dp)) {
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                },
                textColor
            )
        }
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
    textScale: Float,
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
                current = now in from until stop, compact = compact, textScale = textScale,
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
    }
}

@Composable
private fun ProgrammeCell(
    programme: PortalProgramme,
    modifier: Modifier,
    current: Boolean,
    compact: Boolean,
    textScale: Float,
    onFocus: () -> Unit,
    onSelect: (PortalProgramme) -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier.padding(end = 3.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (focused) colors.tertiaryContainer else if (current) {
                colors.secondaryContainer
            } else colors.surface)
            .then(if (focused) Modifier.border(2.dp, colors.primary, RoundedCornerShape(9.dp)) else Modifier)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocus()
                focused = it.isFocused
            }
            .clickable { onSelect(programme) }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            programme.title, style = (if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium)
                .scaled(textScale),
            color = if (focused) colors.onTertiaryContainer else colors.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

private fun TextStyle.scaled(factor: Float): TextStyle =
    copy(
        fontSize = if (fontSize.isSpecified) fontSize * factor else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * factor else lineHeight
    )

internal fun guideTime(seconds: Long): String =
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(seconds))


/** "Mi., 03:12 · 25.11.2026": weekday and date of the shown window, current clock time. */
private fun guideDate(windowStart: Long, now: Long): String {
    val zone = ZoneId.systemDefault()
    val day = DateTimeFormatter.ofPattern("EEE.,").withZone(zone).format(Instant.ofEpochSecond(windowStart)).replace("..", ".")
    val clock = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochSecond(now))
    val date = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(zone).format(Instant.ofEpochSecond(windowStart))
    return "$day $clock · $date"
}
