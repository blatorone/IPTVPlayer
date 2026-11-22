package com.iptvplayer.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    FAVORITES(R.string.favorites),
    WATCHLIST(R.string.watchlist)
}

@Composable
internal fun ColumnScope.TitleGrid(
    titles: List<PortalTitle>,
    isFavorite: (PortalTitle) -> Boolean,
    isSaved: (PortalTitle) -> Boolean,
    preferPopularity: Boolean,
    newEpisodes: (PortalTitle) -> Int,
    onSelect: (PortalTitle) -> Unit
) {
    var sort by remember(preferPopularity) {
        mutableStateOf(if (preferPopularity) TitleSort.POPULARITY else TitleSort.NAME)
    }
    var filter by remember { mutableStateOf(TitleFilter.ALL) }
    var year by remember { mutableStateOf<Int?>(null) }
    var sorting by remember { mutableStateOf(false) }
    var filtering by remember { mutableStateOf(false) }
    var selectingYear by remember { mutableStateOf(false) }
    val years = remember(titles) { titles.mapNotNull(::vodReleaseYear).distinct().sortedDescending() }
    val matches = titles.filter {
        (year == null || vodReleaseYear(it) == year) && when (filter) {
            TitleFilter.ALL -> true
            TitleFilter.FAVORITES -> isFavorite(it)
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
        if (years.isNotEmpty()) Box {
            MenuButton(year?.toString() ?: stringResource(R.string.all_years), Modifier) { selectingYear = true }
            DropdownMenu(expanded = selectingYear, onDismissRequest = { selectingYear = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.all_years)) },
                    onClick = { year = null; selectingYear = false }
                )
                years.forEach { available ->
                    DropdownMenuItem(
                        text = { Text(available.toString()) },
                        onClick = { year = available; selectingYear = false }
                    )
                }
            }
        }
    }
    if (sorted.isEmpty()) Text(stringResource(R.string.no_matching_titles))
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 92.dp),
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorted) { title ->
            var focused by remember { mutableStateOf(false) }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (focused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                modifier = Modifier.clip(MaterialTheme.shapes.medium)
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onSelect(title) }
            ) {
                Column {
                    Poster(title, Modifier.fillMaxWidth())
                    Text(
                        displayVodTitle(title), Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    val unseen = newEpisodes(title)
                    if (unseen > 0) Text(
                        stringResource(R.string.new_episode_count, unseen),
                        Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
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
    fromTmdb: Boolean,
    favorite: Boolean,
    saved: Boolean,
    newEpisodes: Int,
    resumeLabel: String?,
    onResume: (() -> Unit)?,
    compact: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onWatch: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Poster(title, Modifier.width(if (compact) 72.dp else 116.dp).clip(MaterialTheme.shapes.medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            Text(
                displayVodTitle(title), style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis
            )
            vodReleaseYear(title)?.let { Text(stringResource(R.string.title_year, it)) }
            if (fromTmdb) {
                Text(
                    metadataRating?.let { stringResource(R.string.tmdb_rating, it) }
                        ?: stringResource(R.string.tmdb_details_source),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (newEpisodes > 0) Text(
                stringResource(R.string.new_episode_count, newEpisodes),
                color = MaterialTheme.colorScheme.primary
            )
            title.addedAt?.let {
                val date = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(it))
                Text(stringResource(R.string.title_added, date))
            }
            resumeLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onResume != null) MenuButton(stringResource(R.string.resume_playback), Modifier, onClick = onResume)
                if (section == PortalSection.MOVIES) {
                    MenuButton(stringResource(R.string.play_movie), Modifier, onClick = onPlay)
                }
                MenuButton(stringResource(R.string.favorites), Modifier, selected = favorite, onClick = onFavorite)
                MenuButton(stringResource(R.string.watchlist), Modifier, selected = saved, onClick = onWatch)
            }
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
internal fun SeriesSeasons(
    modifier: Modifier,
    series: PortalTitle,
    seasons: List<PortalSeason>?,
    selected: PortalSeason?,
    failed: Boolean,
    unseen: Set<String>,
    onSeason: (PortalSeason) -> Unit,
    episodeProgress: (PortalSeason, PortalTitle) -> EpisodeCardProgress,
    metadata: Map<Pair<Int, Int>, TmdbEpisodeDetails>,
    onEpisode: (PortalSeason, PortalTitle) -> Unit,
    onRetry: () -> Unit,
    header: @Composable () -> Unit
) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "overview") { header() }
        item(key = "seasons") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.seasons), style = MaterialTheme.typography.titleMedium)
                if (seasons == null && !failed) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (failed || seasons?.isEmpty() == true) {
                    Text(stringResource(R.string.no_episodes))
                    MenuButton(stringResource(R.string.series_retry_seasons), Modifier, onClick = onRetry)
                }
                if (!seasons.isNullOrEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasons) { season ->
                            MenuButton(seasonLabel(season, series), Modifier, selected = selected == season) {
                                onSeason(season)
                            }
                        }
                    }
                }
            }
        }
        for (season in selected?.let(::listOf) ?: seasons.orEmpty()) {
            item { Text(seasonLabel(season, series), style = MaterialTheme.typography.titleMedium) }
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
                        PortalImage(episode.poster ?: details?.picture ?: series.poster ?: season.title.poster, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val entry = if (seriesEpisodeKey(season, episode) in unseen) {
                            stringResource(R.string.new_episode_entry, label)
                        } else label
                        MenuButton(entry, Modifier.fillMaxWidth()) { onEpisode(season, episode) }
                        val description = germanDescription(episode.description) ?: details?.description
                        Text(
                            description ?: stringResource(R.string.no_german_episode_description),
                            style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                        Text(stringResource(when {
                            progress.completed -> R.string.episode_watched
                            progress.position > 0 -> R.string.episode_in_progress
                            else -> R.string.episode_unwatched
                        }), style = MaterialTheme.typography.labelSmall)
                        if (progress.completed || progress.duration > 0) {
                            LinearProgressIndicator(
                                progress = {
                                    if (progress.completed) 1f else (progress.position.toFloat() / progress.duration).coerceIn(0f, 1f)
                                }, modifier = Modifier.fillMaxWidth()
                            )
                        }
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
