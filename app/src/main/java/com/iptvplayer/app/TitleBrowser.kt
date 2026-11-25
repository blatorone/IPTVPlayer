package com.iptvplayer.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class TitleSort(val label: Int) {
    NAME(R.string.sort_title),
    YEAR(R.string.sort_year),
    UPLOADED(R.string.sort_uploaded),
    POPULARITY(R.string.vod_category_popular)
}

private enum class TitleFilter(val label: Int) {
    ALL(R.string.all_titles),
    WATCHLIST(R.string.watchlist)
}

@Composable
internal fun ColumnScope.TitleGrid(
    titles: List<PortalTitle>,
    categoryTitle: String,
    isFavorite: (PortalTitle) -> Boolean,
    isSaved: (PortalTitle) -> Boolean,
    preferPopularity: Boolean,
    newEpisodes: (PortalTitle) -> Int,
    returnTitle: PortalTitle?,
    focusFirstTitle: Boolean,
    onTitleFocusRestored: () -> Unit,
    onSelect: (PortalTitle) -> Unit
) {
    var sort by rememberSaveable(preferPopularity) {
        mutableStateOf(if (preferPopularity) TitleSort.POPULARITY else TitleSort.NAME)
    }
    var filter by rememberSaveable { mutableStateOf(TitleFilter.ALL) }
    var yearFrom by rememberSaveable { mutableStateOf<Int?>(null) }
    var yearTo by rememberSaveable { mutableStateOf<Int?>(null) }
    val yearSpan = yearFrom?.let { from -> YearSpan(from, yearTo ?: from) }
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    val years = remember(titles) { titles.mapNotNull(::vodReleaseYear).distinct().sortedDescending() }
    val matches = titles.filter {
        (yearSpan == null || yearSpan.contains(vodReleaseYear(it))) && when (filter) {
            TitleFilter.ALL -> true
            TitleFilter.WATCHLIST -> isSaved(it)
        }
    }
    val sorted = remember(matches, sort) {
        val names = matches.associateWith { displayVodTitle(it).lowercase(Locale.ROOT) }
        val byName = compareBy<PortalTitle> { names.getValue(it) }
        when (sort) {
            TitleSort.NAME -> matches.sortedWith(byName)
            TitleSort.YEAR -> {
                val years = matches.associateWith { vodReleaseYear(it) }
                matches.sortedWith(compareByDescending<PortalTitle> { years[it] ?: Int.MIN_VALUE }.then(byName))
            }
            TitleSort.UPLOADED -> matches.sortedWith(compareByDescending<PortalTitle> { it.addedAt ?: Long.MIN_VALUE }.then(byName))
            TitleSort.POPULARITY -> matches.sortedByDescending { it.popularity ?: Double.NEGATIVE_INFINITY }
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            MenuButton(stringResource(R.string.sort_by, stringResource(sort.label)), Modifier) { sorting = true }
            DropdownMenu(expanded = sorting, onDismissRequest = { sorting = false }) {
                TitleSort.entries.forEach {
                    DropdownMenuItem(text = { Text(stringResource(it.label)) }, onClick = { sort = it; sorting = false })
                }
            }
        }
        Box {
            MenuButton(stringResource(filter.label), Modifier) { filtering = true }
            DropdownMenu(expanded = filtering, onDismissRequest = { filtering = false }) {
                TitleFilter.entries.forEach {
                    DropdownMenuItem(text = { Text(stringResource(it.label)) }, onClick = { filter = it; filtering = false })
                }
            }
        }
        if (years.isNotEmpty()) YearFilterButton(years, yearSpan) { span ->
            yearFrom = span?.from
            yearTo = span?.to
        }
    }
    val allRow = rememberLazyListState()
    val returnIndex = returnTitle?.let { target -> sorted.indexOfFirst { it == target } } ?: -1
    val returnFocus = remember { FocusRequester() }
    LaunchedEffect(returnTitle, returnIndex, focusFirstTitle) {
        if (returnIndex >= 0) {
            if (allRow.layoutInfo.visibleItemsInfo.none { it.index == returnIndex }) allRow.scrollToItem(returnIndex)
        } else if (returnTitle != null) onTitleFocusRestored()
    }
    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        val compact = maxWidth < 600.dp || maxHeight < 430.dp
        val cardWidth = if (compact) 112.dp else 150.dp
        val popular = sorted.filter { it.popularity != null }.sortedByDescending { it.popularity }
            .ifEmpty { sorted }.take(2)
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 18.dp)
        ) {
            if (popular.isNotEmpty()) item(key = "hero-popular") {
                LazyRow(
                    Modifier.fillMaxWidth().height((maxHeight * if (compact) 0.30f else 0.36f).coerceIn(150.dp, 300.dp)),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(popular, key = { _, title -> "hero-${posterShelfKey(title)}" }) { index, title ->
                        FeaturedTitle(
                            title = title,
                            height = (maxHeight * if (compact) 0.30f else 0.36f).coerceIn(150.dp, 300.dp),
                            modifier = Modifier.width(maxWidth * 0.86f),
                            onSelect = { onSelect(title) }
                        )
                    }
                }
            }
            if (matches.isEmpty()) item { Text(stringResource(R.string.no_matching_titles)) }
            if (sorted.isNotEmpty()) item(key = "shelf-all") {
                TitleShelf(
                    title = categoryTitle,
                    titles = sorted, cardWidth = cardWidth, state = allRow,
                    focusIndex = returnIndex.takeIf { it >= 0 } ?: if (focusFirstTitle) 0 else -1,
                    focusRequester = returnFocus,
                    newEpisodes = newEpisodes, onFocusRestored = onTitleFocusRestored,
                    onSelect = onSelect
                )
            }
            val withNewEpisodes = sorted.filter { newEpisodes(it) > 0 }
            if (withNewEpisodes.isNotEmpty()) item(key = "shelf-new-episodes") {
                TitleShelf(
                    title = stringResource(R.string.catalog_shelf_new_episodes),
                    titles = withNewEpisodes, cardWidth = cardWidth,
                    newEpisodes = newEpisodes, onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun FeaturedTitle(
    title: PortalTitle,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (focused || hovered) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxWidth().height(height).onFocusChanged { focused = it.isFocused }
            .hoverable(interactions, enabled = true).clickable(interactionSource = interactions, indication = null) { onSelect() }
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(title.name.take(1), Modifier.align(Alignment.Center), style = MaterialTheme.typography.displayLarge)
            PortalImage(title.poster, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color(0xF2080C12), 0.48f to Color(0xA8080C12), 1f to Color(0x18080C12)
                    )
                )
            )
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth(0.78f).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(displayVodTitle(title), style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val details = listOfNotNull(vodReleaseYear(title)?.toString(), title.description?.takeIf { it.isNotBlank() })
                if (details.isNotEmpty()) Text(
                    details.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(stringResource(R.string.catalog_featured_action), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TitleShelf(
    title: String,
    titles: List<PortalTitle>,
    cardWidth: androidx.compose.ui.unit.Dp,
    state: LazyListState = rememberLazyListState(),
    focusIndex: Int = -1,
    focusRequester: FocusRequester? = null,
    newEpisodes: (PortalTitle) -> Int,
    onFocusRestored: () -> Unit = {},
    onSelect: (PortalTitle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        LazyRow(
            Modifier.fillMaxWidth().height(cardWidth * 1.67f + 48.dp),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(titles, key = { index, item -> "${posterShelfKey(item)}-$index" }) { index, item ->
                val shouldRestore = index == focusIndex && focusRequester != null
                if (shouldRestore) LaunchedEffect(item) {
                    withFrameNanos { }
                    if (focusRequester!!.requestFocus()) onFocusRestored()
                }
                PosterShelfCard(
                    item, cardWidth, newEpisodes(item),
                    modifier = if (shouldRestore) Modifier.focusRequester(focusRequester!!) else Modifier,
                    onFocused = if (shouldRestore) onFocusRestored else null,
                    onSelect = { onSelect(item) }
                )
            }
        }
    }
}

private fun posterShelfKey(title: PortalTitle): String = listOfNotNull(
    title.imdbId, title.tmdbId, title.id, title.year?.toString(), displayVodTitle(title)
).joinToString("|")

@Composable
private fun PosterShelfCard(
    title: PortalTitle,
    width: androidx.compose.ui.unit.Dp,
    unseen: Int,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
    onSelect: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val highlighted = focused || hovered
    Surface(
        modifier = modifier.width(width).fillMaxHeight().onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocused?.invoke()
        }.hoverable(interactions, enabled = true).clickable(interactionSource = interactions, indication = null) { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (highlighted) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = if (highlighted) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Text(title.name.take(1), style = MaterialTheme.typography.headlineMedium)
                }
                PortalImage(title.poster, Modifier.fillMaxSize())
                if (unseen > 0) Surface(
                    Modifier.align(Alignment.TopStart).padding(5.dp),
                    color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)
                ) { Text(stringResource(R.string.new_episode_count, unseen), Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1) }
            }
            Text(
                displayVodTitle(title), Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun Poster(title: PortalTitle, modifier: Modifier) {
    Box(
        modifier.aspectRatio(2f / 3).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(title.name.take(1), style = MaterialTheme.typography.headlineLarge)
        PortalImage(title.poster, Modifier.fillMaxSize())
    }
}

@Composable
internal fun TitleDetails(
    title: PortalTitle,
    section: PortalSection,
    metadataRating: Double?,
    metadataProvider: String?,
    favorite: Boolean,
    saved: Boolean,
    newEpisodes: Int,
    upcomingEpisode: String?,
    resumeLabel: String?,
    onResume: (() -> Unit)?,
    firstEpisodeAction: (() -> Unit)?,
    firstEpisodeLabel: String?,
    focusPrimary: Boolean,
    onPrimaryFocused: () -> Unit,
    compact: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    watchProgress: EpisodeCardProgress?,
    onWatchStatus: (() -> Unit)?,
    onFavorite: () -> Unit,
    onWatch: () -> Unit,
    onCast: (() -> Unit)? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Poster(title, Modifier.width(if (compact) 72.dp else 116.dp).clip(MaterialTheme.shapes.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            Text(
                displayVodTitle(title), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis
            )
            vodReleaseYear(title)?.let { Text(stringResource(R.string.title_year, it)) }
            if (metadataProvider != null) {
                Text(metadataRating?.let { stringResource(R.string.metadata_rating, metadataProvider, it) }
                    ?: stringResource(R.string.metadata_details_source, metadataProvider),
                    color = MaterialTheme.colorScheme.primary)
            }
            if (newEpisodes > 0) Text(
                stringResource(R.string.new_episode_count, newEpisodes),
                color = MaterialTheme.colorScheme.primary
            )
            upcomingEpisode?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            title.addedAt?.let {
                val date = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(it))
                Text(stringResource(R.string.title_added, date))
            }
            resumeLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            val leftEdge = Modifier.onHorizontalRemote(allowRight = false) { onBack() }
            val primaryFocus = remember { FocusRequester() }
            val primaryAction = onResume ?: firstEpisodeAction
                ?: onPlay.takeIf { section == PortalSection.MOVIES }
            LaunchedEffect(focusPrimary, primaryAction != null) {
                if (focusPrimary && primaryAction != null) {
                    withFrameNanos { }
                    if (primaryFocus.requestFocus()) onPrimaryFocused()
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (primaryAction != null) MenuButton(
                    when {
                        onResume != null -> stringResource(R.string.resume_playback)
                        firstEpisodeAction != null -> firstEpisodeLabel.orEmpty()
                        else -> stringResource(R.string.play_movie)
                    },
                    leftEdge.focusRequester(primaryFocus),
                    onClick = primaryAction
                )
                if (section == PortalSection.MOVIES && onResume != null) {
                    MenuButton(stringResource(R.string.play_movie), Modifier, onClick = onPlay)
                }
                // Favourites and watchlist are one list ("Merkliste"), synchronised with Trakt when connected.
                val watchlistModifier = if (section == PortalSection.SERIES && primaryAction == null) leftEdge else Modifier
                MenuButton(stringResource(R.string.watchlist), watchlistModifier, selected = saved, onClick = onWatch)
                onCast?.let { MenuButton(stringResource(R.string.cast), Modifier, onClick = it) }
                onWatchStatus?.let { MenuButton(stringResource(R.string.watched_status), Modifier, onClick = it) }
            }
            watchProgress?.let { WatchProgress(it) }
            Text(
                germanDescription(title.description) ?: stringResource(R.string.no_german_description),
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                maxLines = if (section == PortalSection.SERIES) Int.MAX_VALUE else 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal data class EpisodeCardProgress(val completed: Boolean, val position: Long, val duration: Long)

@Composable
private fun WatchProgress(progress: EpisodeCardProgress) {
    Text(stringResource(when {
        progress.completed -> R.string.episode_watched
        progress.position > 0 -> R.string.episode_in_progress
        else -> R.string.episode_unwatched
    }), style = MaterialTheme.typography.labelSmall)
    if (progress.completed || progress.duration > 0) {
        LinearProgressIndicator(
            progress = { if (progress.completed) 1f else (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun SeriesSeasons(
    modifier: Modifier,
    scroll: LazyListState,
    series: PortalTitle,
    seasons: List<PortalSeason>?,
    selected: PortalSeason?,
    failed: Boolean,
    unseen: Set<String>,
    onSeason: (PortalSeason) -> Unit,
    onSeasonWatchStatus: (PortalSeason) -> Unit,
    onEpisodeWatchStatus: (PortalSeason, PortalTitle) -> Unit,
    episodeProgress: (PortalSeason, PortalTitle) -> EpisodeCardProgress,
    metadata: Map<Pair<Int, Int>, TmdbEpisodeDetails>,
    onEpisode: (PortalSeason, PortalTitle) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    header: @Composable () -> Unit
) {
    val leftEdge = Modifier.onHorizontalRemote(allowRight = false) { onBack() }
    LazyColumn(modifier.fillMaxWidth(), state = scroll, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "overview") { header() }
        item(key = "seasons") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.seasons), style = MaterialTheme.typography.titleMedium)
                if (seasons == null && !failed) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (failed || seasons?.isEmpty() == true) {
                    Text(stringResource(R.string.no_episodes))
                    MenuButton(stringResource(R.string.series_retry_seasons), leftEdge, onClick = onRetry)
                }
                if (!seasons.isNullOrEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(seasons) { index, season ->
                            MenuButton(seasonLabel(season, series), if (index == 0) leftEdge else Modifier, selected = selected == season) {
                                onSeason(season)
                            }
                        }
                    }
                }
            }
        }
        for (season in selected?.let(::listOf) ?: seasons.orEmpty()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(seasonLabel(season, series), style = MaterialTheme.typography.titleMedium)
                    if (season.episodes.isNotEmpty()) {
                        MenuButton(stringResource(R.string.season_watched_status), Modifier) { onSeasonWatchStatus(season) }
                    }
                }
            }
            items(season.episodes) { episode ->
                val label = episode.episode?.let { number ->
                    if (episode.name == season.title.name || episode.name == series.name) {
                        stringResource(R.string.episode_number, number)
                    } else stringResource(R.string.episode_title, episode.name, number)
                } ?: episode.name
                val progress = episodeProgress(season, episode)
                val details = season.number?.let { number ->
                    episode.episode?.toIntOrNull()?.let { metadata[number to it] }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.width(112.dp).aspectRatio(16f / 9f).clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        PortalImage(details?.picture ?: episode.poster ?: series.poster ?: season.title.poster, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val entry = if (seriesEpisodeKey(season, episode) in unseen) {
                            stringResource(R.string.new_episode_entry, label)
                        } else label
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MenuButton(entry, leftEdge.weight(1f)) { onEpisode(season, episode) }
                            MenuButton(stringResource(R.string.watched_status), Modifier) { onEpisodeWatchStatus(season, episode) }
                        }
                        val description = germanDescription(details?.description) ?: germanDescription(episode.description)
                        Text(
                            description ?: stringResource(R.string.no_german_episode_description),
                            style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                        WatchProgress(progress)
                    }
                }
            }
        }
    }
}

@Composable
private fun seasonLabel(season: PortalSeason, series: PortalTitle): String = when {
    season.number != null -> stringResource(R.string.season_number, season.number)
    season.title.name != series.name -> season.title.name
    else -> stringResource(R.string.unnumbered_season)
}
