package com.iptvplayer.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException

/** An actor or actress of a film or series. IDs are kept to load their other work. */
internal data class CastMember(
    val name: String,
    val role: String?,
    val photo: String?,
    val tmdbId: Long?,
    val traktId: Long?
)

/** A film or series a person appeared in, as reported by TMDB or Trakt. */
internal data class PersonCredit(
    val section: PortalSection,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val imdbId: String?,
    val tmdbId: String?
)

/**
 * The person's work that is available in the portals, newest first. Only titles found in
 * the stored catalogues are listed, so everything shown can be played.
 */
internal fun matchPersonCredits(
    credits: List<PersonCredit>,
    sources: List<SavedPortal>,
    allowed: (PortalSection, List<SourceItem<PortalTitle>>) -> List<SourceItem<PortalTitle>>
): List<Pair<PortalSection, PortalTitle>> {
    val matchers = HashMap<Pair<String, PortalSection>, CatalogueMatcher>()
    fun matcher(source: SavedPortal, section: PortalSection) = matchers.getOrPut(source.identity to section) {
        CatalogueMatcher(source.catalogs[section]?.titles.orEmpty())
    }
    return credits.sortedByDescending { it.year ?: 0 }.mapNotNull { credit ->
        val names = listOfNotNull(credit.title, credit.originalTitle).distinct()
        val found = sources.mapNotNull { source ->
            names.firstNotNullOfOrNull { name ->
                matcher(source, credit.section).find(
                    PortalTitle(name, year = credit.year, imdbId = credit.imdbId, tmdbId = credit.tmdbId),
                    requireVerifiedIdentity = false
                )
            }?.let { SourceItem(source, it) }
        }
        allowed(credit.section, found).firstOrNull()?.let { credit.section to it.value }
    }.distinctBy { (section, title) -> "$section:${normalizedLabel(displayVodTitle(title))}:${vodReleaseYear(title)}" }
}

@Composable
private fun ScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun FocusCard(
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    content: @Composable () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.onFocusChanged { focused = it.isFocused }.clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        color = if (focused) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) { content() }
}

/** Cast of a film or series with photos; selecting a person opens their work. */
@Composable
internal fun CastDialog(
    title: String,
    load: suspend () -> List<CastMember>,
    onPerson: (CastMember) -> Unit,
    onDismiss: () -> Unit
) {
    val cast by produceState<List<CastMember>?>(null, title) {
        value = try {
            load()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
    }
    ScreenDialog(onDismiss) {
        Text(stringResource(R.string.cast_title, title), style = MaterialTheme.typography.headlineSmall)
        val members = cast
        when {
            members == null -> {
                Text(stringResource(R.string.cast_loading))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            members.isEmpty() -> Text(stringResource(R.string.cast_empty))
            else -> {
                val first = remember { FocusRequester() }
                LaunchedEffect(members) { runCatching { first.requestFocus() } }
                LazyVerticalGrid(GridCells.Adaptive(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(members, key = { it.tmdbId ?: it.traktId ?: it.name.hashCode().toLong() }) { member ->
                        FocusCard(
                            if (member == members.first()) Modifier.focusRequester(first) else Modifier,
                            onSelect = { onPerson(member) }
                        ) {
                            Column {
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(member.name.take(1), style = MaterialTheme.typography.headlineMedium)
                                    PortalImage(member.photo, Modifier.fillMaxSize())
                                }
                                Text(member.name, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                member.role?.let {
                                    Text(it, Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                        style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Films and series of one person that are available in the portals, with tabs and the year filter. */
@Composable
internal fun PersonDialog(
    person: CastMember,
    load: suspend () -> List<Pair<PortalSection, PortalTitle>>,
    onOpen: (PortalSection, PortalTitle) -> Unit,
    onDismiss: () -> Unit
) {
    val works by produceState<List<Pair<PortalSection, PortalTitle>>?>(null, person) {
        value = try {
            load()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyList()
        }
    }
    var section by remember { mutableStateOf(PortalSection.MOVIES) }
    var span by remember { mutableStateOf<YearSpan?>(null) }
    ScreenDialog(onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(96.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(person.name.take(1), style = MaterialTheme.typography.headlineMedium)
                PortalImage(person.photo, Modifier.fillMaxSize())
            }
            Column {
                Text(person.name, style = MaterialTheme.typography.headlineSmall)
                person.role?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        val all = works
        if (all == null) {
            Text(stringResource(R.string.person_loading))
            LinearProgressIndicator(Modifier.fillMaxWidth())
            return@ScreenDialog
        }
        val movies = all.filter { it.first == PortalSection.MOVIES }
        val series = all.filter { it.first == PortalSection.SERIES }
        val shown = (if (section == PortalSection.MOVIES) movies else series)
        val years = shown.mapNotNull { vodReleaseYear(it.second) }.distinct().sortedDescending()
        val filtered = shown.filter { span == null || span?.contains(vodReleaseYear(it.second)) == true }
        val firstTab = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { firstTab.requestFocus() } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MenuButton(stringResource(R.string.person_movies, movies.size), Modifier.focusRequester(firstTab),
                selected = section == PortalSection.MOVIES) {
                section = PortalSection.MOVIES
                span = null
            }
            MenuButton(stringResource(R.string.person_series, series.size), Modifier,
                selected = section == PortalSection.SERIES) {
                section = PortalSection.SERIES
                span = null
            }
            if (years.isNotEmpty()) YearFilterButton(years, span) { span = it }
        }
        if (filtered.isEmpty()) Text(stringResource(R.string.person_empty))
        LazyVerticalGrid(GridCells.Adaptive(140.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { (kind, title) -> "$kind:${title.id ?: title.name}" }) { (kind, title) ->
                FocusCard(onSelect = { onOpen(kind, title) }) {
                    Column {
                        Poster(title, Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)))
                        Text(displayVodTitle(title), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        vodReleaseYear(title)?.let {
                            Text(it.toString(), Modifier.padding(start = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
