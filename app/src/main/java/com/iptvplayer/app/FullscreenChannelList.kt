package com.iptvplayer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

internal fun currentGuideProgramme(schedule: List<PortalProgramme>, now: Long): PortalProgramme? =
    schedule.firstOrNull {
        val start = it.startSeconds
        val end = it.endSeconds ?: start?.plus(1800)
        start != null && end != null && now in start until end
    } ?: schedule.firstOrNull { it.startSeconds == null }

internal fun guideProgrammeProgress(show: PortalProgramme?, now: Long): Float? {
    val start = show?.startSeconds ?: return null
    val end = show.endSeconds ?: return null
    return if (end > start && now in start until end) (now - start).toFloat() / (end - start) else null
}

@Composable
internal fun FullscreenChannelList(
    modifier: Modifier,
    channels: List<StalkerChannel>,
    playing: StalkerChannel?,
    appearance: GuideAppearance,
    showSchedule: Boolean,
    refreshKey: Int,
    programmes: (StalkerChannel) -> List<PortalProgramme>?,
    onVisible: suspend (List<StalkerChannel>) -> Unit,
    onInteraction: () -> Unit,
    onScheduleFocused: (Boolean) -> Unit,
    onSelect: (StalkerChannel) -> Unit
) {
    if (channels.isEmpty()) {
        Text(stringResource(R.string.no_channels))
        return
    }
    val startIndex = channels.indexOf(playing).coerceAtLeast(0)
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val initialFocus = remember { FocusRequester() }
    var highlighted by remember(channels) { mutableStateOf(channels[startIndex]) }
    var selectedProgramme by remember(highlighted) { mutableStateOf<PortalProgramme?>(null) }
    val now by produceState(System.currentTimeMillis() / 1000) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1000
        }
    }
    val loadVisible by rememberUpdatedState(onVisible)
    LaunchedEffect(channels, scroll, refreshKey) {
        snapshotFlow { scroll.layoutInfo.visibleItemsInfo.map { it.index } }.collectLatest { visible ->
            if (visible.isEmpty()) return@collectLatest
            delay(180)
            loadVisible(channels.subList(visible.first().coerceIn(0, channels.size), (visible.last() + 3).coerceIn(0, channels.size)))
        }
    }
    LaunchedEffect(channels) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyColumn(
            Modifier.weight(if (showSchedule) 1f else 2f).fillMaxHeight(),
            state = scroll,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(channels) { index, channel ->
                val schedule = programmes(channel)
                val show = currentGuideProgramme(schedule.orEmpty(), now)
                val progress = guideProgrammeProgress(show, now)
                var focused by remember { mutableStateOf(false) }
                val colors = MaterialTheme.colorScheme
                val focus = if (index == startIndex) Modifier.focusRequester(initialFocus) else Modifier
                Surface(
                    color = when {
                        focused -> colors.tertiaryContainer
                        playing === channel -> colors.secondaryContainer
                        else -> colors.surfaceContainerLow
                    },
                    shape = MaterialTheme.shapes.small,
                    modifier = focus.fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused && !focused) {
                                highlighted = channel
                                onScheduleFocused(false)
                                onInteraction()
                            }
                            focused = it.isFocused
                        }
                        .clickable { onSelect(channel) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appearance.showLogos) ChannelLogo(channel.logo, Modifier.padding(end = 8.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            val number = if (appearance.showNumbers) "${channel.number ?: index + 1}  " else ""
                            Text(
                                number + channel.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                show?.title ?: stringResource(if (schedule == null) R.string.guide_row_loading else R.string.no_guide),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (appearance.showProgress && progress != null) {
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
        if (showSchedule) {
            val schedule = programmes(highlighted)
            val current = currentGuideProgramme(schedule.orEmpty(), now)
            val visible = schedule.orEmpty().filter {
                it == current || (it.endSeconds ?: it.startSeconds ?: 0) >= now
            }.sortedBy { it.startSeconds ?: Long.MIN_VALUE }
            val details = selectedProgramme ?: current
            Column(
                Modifier.weight(1.1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(highlighted.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (visible.isEmpty()) Text(
                    stringResource(if (schedule == null) R.string.guide_row_loading else R.string.no_guide),
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(Modifier.weight(1f)) {
                    items(visible) { show ->
                        val time = show.startSeconds?.let(::guideTime).orEmpty()
                        MenuButton(
                            listOf(time, show.title).filter(String::isNotBlank).joinToString("  "),
                            Modifier.fillMaxWidth(),
                            selected = show == details,
                            onFocused = {
                                selectedProgramme = show
                                onScheduleFocused(true)
                                onInteraction()
                            }
                        ) {
                            selectedProgramme = show
                            onInteraction()
                        }
                    }
                }
                if (appearance.showDescription && details != null) {
                    Text(
                        details.description ?: stringResource(R.string.no_title_description),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(8.dp),
                        style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
