package com.iptvplayer.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val searchedSections = listOf(PortalSection.MOVIES, PortalSection.SERIES)
private data class PortalResults(
    val titles: List<SourceItem<PortalTitle>>,
    val total: Int?,
    val incomplete: Boolean
)

@Composable
internal fun ColumnScope.PortalSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    sources: List<SavedPortal>,
    channels: List<StalkerChannel>,
    liveSources: (StalkerChannel) -> List<SavedPortal>,
    liveProgramme: (StalkerChannel) -> String?,
    onHighlightChannel: (StalkerChannel) -> Unit,
    metadataClient: TmdbClient?,
    portalFor: (SavedPortal) -> StalkerPortal,
    countries: Set<String>,
    combine: Boolean,
    onChannel: (StalkerChannel) -> Unit,
    onTitle: (PortalSection, List<SourceItem<PortalTitle>>) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.search)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    val term = query.trim()
    if (term.isEmpty() || sources.isEmpty()) return
    var selection by remember { mutableStateOf(PortalSection.LIVE) }
    val metadataLimiter = remember { Semaphore(3) }

    val live = remember(channels, term) { channels.filter { it.name.contains(term, ignoreCase = true) } }
    val labels = remember(sources) {
        sources.associate { source ->
            val portal = portalFor(source)
            source.identity to portal.sourceLabel
        }
    }
    fun sourceNames(items: List<SavedPortal>): String =
        items.mapNotNull { labels[it.identity] }.distinct().joinToString(", ")

    val remote by produceState<Map<PortalSection, List<PortalResults>>?>(null, sources, term, countries) {
        value = null
        delay(400)
        val limiter = Semaphore(4)
        value = coroutineScope {
            searchedSections.associateWith { section ->
                sources.map { source ->
                    async { searchPortal(source, portalFor(source), section, term, countries, limiter) }
                }
            }.mapValues { (_, jobs) -> jobs.map { it.await() } }
        }
    }
    val pagesBySection = searchedSections.associateWith { section ->
            val pages = remote?.get(section) ?: sources.map {
                localResults(it, section, term, countries)
            }
            pages
    }
    val hitsBySection = pagesBySection.mapValues { (section, pages) ->
        val items = pages.flatMap { it.titles }
        if (combine) mergeCatalogueTitles(items.distinct(), requireVerifiedIdentity = section == PortalSection.SERIES)
        else items.map(::listOf)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (section in PortalSection.entries) {
            val count = if (section == PortalSection.LIVE) live.size else {
                pagesBySection.getValue(section).singleOrNull()?.total ?: hitsBySection.getValue(section).size
            }
            MenuButton(
                stringResource(section.label), Modifier.weight(1f),
                selected = selection == section, count = count
            ) { selection = section }
        }
    }
    if (remote == null && selection != PortalSection.LIVE) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (selection == PortalSection.LIVE) {
        if (live.isEmpty()) Text(stringResource(R.string.search_empty))
        LazyColumn(Modifier.weight(1f)) {
            items(live) { channel ->
                val origin = sourceNames(liveSources(channel))
                Column(Modifier.fillMaxWidth()) {
                    MenuButton(
                        channel.name, Modifier.fillMaxWidth(), logo = channel.logo,
                        onFocused = { onHighlightChannel(channel) }
                    ) { onChannel(channel) }
                    val info = listOfNotNull(liveProgramme(channel), origin.takeIf { it.isNotBlank() })
                    if (info.isNotEmpty()) Text(
                        info.joinToString(" · "), Modifier.padding(start = 10.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        val hits = hitsBySection.getValue(selection)
        if (hits.isEmpty() && remote != null) Text(stringResource(R.string.search_empty))
        if (remote != null && pagesBySection.getValue(selection).any { it.incomplete }) {
            Text(stringResource(R.string.search_partial), style = MaterialTheme.typography.bodySmall)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(hits) { items ->
                val origin = sourceNames(items.map { it.source })
                val original = items.first().value
                val external by produceState<TmdbTitle?>(null, metadataClient, selection, original) {
                    value = null
                    val client = metadataClient ?: return@produceState
                    value = try {
                        metadataLimiter.withPermit { client.lookup(selection, original) }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }
                }
                val title = external?.applyTo(original) ?: original
                var focused by remember { mutableStateOf(false) }
                Surface(
                    color = if (focused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.onFocusChanged { focused = it.isFocused }
                        .clickable { onTitle(selection, items) }
                ) {
                    Column {
                        Poster(title, Modifier.fillMaxWidth())
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(displayVodTitle(title), style = MaterialTheme.typography.labelLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val metadata = listOfNotNull(vodReleaseYear(title)?.toString(), origin.takeIf { it.isNotBlank() })
                            if (metadata.isNotEmpty()) Text(metadata.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            germanDescription(title.description)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun searchPortal(
    source: SavedPortal,
    portal: StalkerPortal,
    section: PortalSection,
    term: String,
    countries: Set<String>,
    limiter: Semaphore
): PortalResults {
    val local = localResults(source, section, term, countries)
    val ids = if (countries.isEmpty()) listOf(ALL_CATALOG_ITEMS) else {
        source.catalogs[section]?.availableGroups("").orEmpty()
            .filter { it.id != ALL_CATALOG_ITEMS && categoryCountry(it.name, it.country) in countries }
            .map { it.id }
    }
    if (ids.isEmpty()) return local
    val pages = coroutineScope {
        ids.map { group ->
            async {
                try {
                    limiter.withPermit {
                        val page = portal.loadCatalog(
                            section, portal.openSession(), category = group, search = term
                        )
                        page.takeIf {
                            it.titles.all { title ->
                                title.name.contains(term, ignoreCase = true) &&
                                    (group == ALL_CATALOG_ITEMS || title.categoryId == null || title.categoryId == group)
                            }
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            }
        }.map { it.await() }
    }
    val available = pages.filterNotNull()
    if (available.isEmpty()) return local
    val remote = available.flatMap { it.titles }.map { SourceItem(source, it) }
    val missing = pages.any { it == null }
    return PortalResults(
        (remote + if (missing) local.titles else emptyList()).distinct(),
        if (missing) null else available.sumOf { it.total ?: it.titles.size },
        incomplete = missing || available.any { it.nextPage != null }
    )
}

private fun localResults(
    source: SavedPortal, section: PortalSection, term: String, countries: Set<String>
): PortalResults {
    val catalog = source.catalogs[section]
    val groups = catalog?.groups.orEmpty().associateBy { it.id }
    val hits = catalog?.titles.orEmpty().filter {
        val group = groups[it.categoryId]
        it.name.contains(term, ignoreCase = true) &&
            (countries.isEmpty() || categoryCountry(
                group?.name ?: it.categoryName ?: it.categoryId.orEmpty(), group?.country
            ) in countries)
    }
    return PortalResults(hits.map { SourceItem(source, it) }, null, incomplete = true)
}
