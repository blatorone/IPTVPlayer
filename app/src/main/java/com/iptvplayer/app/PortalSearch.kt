package com.iptvplayer.app

import androidx.core.content.edit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val searchedSections = listOf(PortalSection.MOVIES, PortalSection.SERIES)
internal const val SEARCH_HISTORY_DEFAULT_SIZE = 10
internal val SEARCH_HISTORY_SIZES = listOf(5, 10, 15, 20, 30)
private const val SEARCH_HISTORY_ENABLED = "search_history_enabled"
private const val SEARCH_HISTORY_SIZE = "search_history_size"
/** Admin and kids account each keep their own list; settings act on both. */
internal val SEARCH_HISTORY_KEYS = listOf("search_history", "kids:search_history")

/** The newest term first, without duplicates (ignoring case); very short terms are not kept. */
internal fun updatedSearchHistory(entries: List<String>, term: String, max: Int = SEARCH_HISTORY_DEFAULT_SIZE): List<String> {
    val cleaned = term.trim().replace(Regex("\\s+"), " ")
    if (cleaned.length < 2) return entries
    return (listOf(cleaned) + entries.filterNot { it.equals(cleaned, ignoreCase = true) }).take(max)
}

/** Recent search terms, kept on this device (the kids account has its own list). */
internal class SearchHistory(private val preferences: android.content.SharedPreferences?, private val key: String) {
    /** Off in the settings: nothing is kept or shown. */
    val enabled = preferences?.getBoolean(SEARCH_HISTORY_ENABLED, true) ?: true
    private val size = preferences?.getInt(SEARCH_HISTORY_SIZE, SEARCH_HISTORY_DEFAULT_SIZE) ?: SEARCH_HISTORY_DEFAULT_SIZE
    var entries by mutableStateOf(if (enabled) load().take(size) else emptyList())
        private set

    private fun load(): List<String> = runCatching {
        val data = org.json.JSONArray(preferences?.getString(key, null) ?: "[]")
        List(data.length()) { data.getString(it) }
    }.getOrDefault(emptyList())

    private fun save() {
        preferences?.edit { putString(key, org.json.JSONArray(entries).toString()) }
    }

    fun add(term: String) {
        if (!enabled) return
        val next = updatedSearchHistory(entries, term, size)
        if (next != entries) { entries = next; save() }
    }
    fun remove(term: String) { entries = entries - term; save() }
    fun clear() { entries = emptyList(); save() }
}

/** Settings rows: keep the search history (off also deletes it), how many entries, delete it. */
@Composable
internal fun SearchHistorySettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("catalog_visibility", android.content.Context.MODE_PRIVATE)
    }
    var enabled by remember { mutableStateOf(preferences.getBoolean(SEARCH_HISTORY_ENABLED, true)) }
    var size by remember { mutableIntStateOf(preferences.getInt(SEARCH_HISTORY_SIZE, SEARCH_HISTORY_DEFAULT_SIZE)) }
    var cleared by remember { mutableStateOf(false) }
    fun clearAll() {
        preferences.edit { SEARCH_HISTORY_KEYS.forEach { remove(it) } }
    }
    SettingRow(stringResource(R.string.search_history_setting), selected = enabled) {
        enabled = !enabled
        preferences.edit { putBoolean(SEARCH_HISTORY_ENABLED, enabled) }
        if (!enabled) clearAll()
    }
    if (enabled) {
        SettingRow(stringResource(R.string.search_history_size, size)) {
            size = SEARCH_HISTORY_SIZES[(SEARCH_HISTORY_SIZES.indexOf(size) + 1) % SEARCH_HISTORY_SIZES.size]
            preferences.edit { putInt(SEARCH_HISTORY_SIZE, size) }
            // A smaller number shortens the saved lists right away.
            preferences.edit {
                SEARCH_HISTORY_KEYS.forEach { key ->
                    val kept = runCatching {
                        val data = org.json.JSONArray(preferences.getString(key, null) ?: return@forEach)
                        List(data.length()) { data.getString(it) }.take(size)
                    }.getOrNull() ?: return@forEach
                    putString(key, org.json.JSONArray(kept).toString())
                }
            }
        }
        SettingRow(stringResource(R.string.clear_search_history)) {
            clearAll()
            cleared = true
        }
        if (cleared) Text(stringResource(R.string.search_history_cleared), color = MaterialTheme.colorScheme.primary)
    }
}
internal data class PortalResults(
    val titles: List<SourceItem<PortalTitle>>,
    val total: Int?,
    val incomplete: Boolean
)

@Composable
internal fun ColumnScope.PortalSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    sources: List<SavedPortal>,
    channels: List<StalkerChannel>,
    liveSources: (StalkerChannel) -> List<SavedPortal>,
    liveProgramme: (StalkerChannel) -> String?,
    onHighlightChannel: (StalkerChannel) -> Unit,
    metadataClient: TmdbClient?,
    portalFor: (SavedPortal) -> StalkerPortal,
    countries: Set<String>,
    combine: Boolean,
    historyKey: String,
    onChannel: (StalkerChannel) -> Unit,
    onTitle: (PortalSection, List<SourceItem<PortalTitle>>) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val history = remember(historyKey) {
        SearchHistory(context.getSharedPreferences("catalog_visibility", android.content.Context.MODE_PRIVATE), historyKey)
    }
    // After searching (keyboard "Suchen" or an entry of the history) the focus goes to the first
    // result area, Live-TV; while typing it stays in the field.
    val firstSection = remember { FocusRequester() }
    var focusResults by remember { mutableIntStateOf(0) }
    var selection by remember { mutableStateOf(PortalSection.LIVE) }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    // The history: below the empty field, or with "Letzte Suchen" next to the result areas.
    val firstHistory = remember { FocusRequester() }
    var focusHistory by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    val clearHistory = remember { FocusRequester() }
    LaunchedEffect(focusHistory) {
        if (focusHistory == 0) return@LaunchedEffect
        keyboard?.hide()
        repeat(2) { androidx.compose.runtime.withFrameNanos { } }
        runCatching { firstHistory.requestFocus() }
    }
    // The keyboard closed (e.g. with Back) while something is entered: on to the results, not up.
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    val currentQuery by androidx.compose.runtime.rememberUpdatedState(query)
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible) { if (currentQuery.isNotBlank()) focusResults++ else focusHistory++ }
        imeWasVisible = imeVisible
    }
    LaunchedEffect(focusResults) {
        if (focusResults == 0) return@LaunchedEffect
        selection = PortalSection.LIVE
        keyboard?.hide()
        repeat(2) { androidx.compose.runtime.withFrameNanos { } }
        runCatching { firstSection.requestFocus() }
    }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.search)) },
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        // Every finishing key of the on-screen keyboard searches: Fire TV shows "Weiter" (next) instead
        // of "Suchen".
        // It always closes the keyboard: empty, the focus goes to the history.
        keyboardActions = androidx.compose.foundation.text.KeyboardActions {
            keyboard?.hide()
            if (query.isBlank()) focusHistory++
            else {
                history.add(query)
                showHistory = false
                focusResults++
            }
        }
    )
    val term = query.trim()
    if (term.isEmpty() || showHistory) {
        // Recent searches: OK searches again, a long press removes the entry.
        if (history.enabled && history.entries.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(stringResource(R.string.recent_searches), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                // Up from the first entry lands here, up once more in the search field.
                MenuButton(stringResource(R.string.clear_search_history), Modifier.focusRequester(clearHistory)
                    .focusProperties { up = focusRequester; down = firstHistory }) { history.clear() }
            }
            Text(stringResource(R.string.search_history_hint), style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f)) {
                itemsIndexed(history.entries, key = { _, entry -> entry }) { index, entry ->
                    MenuButton(
                        entry, Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(firstHistory).focusProperties { up = clearHistory } else Modifier),
                        leadingIcon = R.drawable.ic_menu_search,
                        onLongPress = { history.remove(entry) }
                    ) {
                        onQueryChange(entry)
                        showHistory = false
                        focusResults++
                    }
                }
            }
        }
        return
    }
    if (sources.isEmpty()) return
    val metadataLimiter = remember { Semaphore(3) }

    val live = remember(channels, term) { channels.filter { matchesSearch(it.name, term) } }
    val labels = remember(sources) {
        sources.associate { source ->
            val portal = portalFor(source)
            source.identity to portal.sourceLabel
        }
    }
    fun sourceNames(items: List<SavedPortal>): String =
        items.mapNotNull { labels[it.identity] }.distinct().joinToString(", ")

    var remote by remember(sources, term, countries) {
        mutableStateOf(emptyMap<PortalSection, List<PortalResults>>())
    }
    LaunchedEffect(sources, term, countries) {
        delay(400)
        val limiter = Semaphore(4)
        for (section in searchedSections) {
            launch {
                val pages = coroutineScope {
                    sources.map { source ->
                        async { searchPortal(source, portalFor(source), section, term, countries, limiter) }
                    }.map { it.await() }
                }
                remote = remote + (section to pages)
            }
        }
    }
    val cached = remember(sources, term, countries) {
        searchedSections.associateWith { section -> sources.map { localResults(it, section, term, countries) } }
    }
    val pagesBySection = cached + remote
    val hitsBySection = remember(pagesBySection, combine) {
        pagesBySection.mapValues { (section, pages) ->
            val items = pages.flatMap { it.titles }
            if (combine) mergeCatalogueTitles(items.distinct(), requireVerifiedIdentity = section == PortalSection.SERIES)
            else items.map(::listOf)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (section in PortalSection.entries) {
            val loading = section != PortalSection.LIVE && section !in remote
            val count = if (section == PortalSection.LIVE) live.size else {
                pagesBySection.getValue(section).singleOrNull()?.total ?: hitsBySection.getValue(section).size
            }
            MenuButton(
                stringResource(if (loading) R.string.search_section_loading else R.string.search_section_count,
                    stringResource(section.label), count),
                Modifier.weight(1f).then(if (section == PortalSection.LIVE) Modifier.focusRequester(firstSection) else Modifier),
                selected = selection == section
            ) { selection = section }
        }
        // The search history next to the areas (the field is not empty here).
        if (history.enabled && history.entries.isNotEmpty()) MenuButton(stringResource(R.string.recent_searches), Modifier.weight(1f)) {
            showHistory = true
            focusHistory++
        }
    }
    if (remote.size < searchedSections.size) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (selection == PortalSection.LIVE) {
        if (live.isEmpty()) Text(stringResource(R.string.search_empty))
        LazyColumn(Modifier.weight(1f)) {
            items(live) { channel ->
                val origin = sourceNames(liveSources(channel))
                Column(Modifier.fillMaxWidth()) {
                    MenuButton(
                        channel.name, Modifier.fillMaxWidth(), logo = channel.logo,
                        onFocused = { onHighlightChannel(channel) }
                    ) {
                        history.add(term)
                        onChannel(channel)
                    }
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
        if (hits.isEmpty() && selection in remote) Text(stringResource(R.string.search_empty))
        if (selection in remote && pagesBySection.getValue(selection).any { it.incomplete }) {
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
                        .clickable {
                            history.add(term)
                            onTitle(selection, items)
                        }
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

internal suspend fun searchPortal(
    source: SavedPortal,
    portal: StalkerPortal,
    section: PortalSection,
    term: String,
    countries: Set<String>,
    limiter: Semaphore
): PortalResults {
    val local = localResults(source, section, term, countries)
    val portalTerm = portalSearchTerm(term)
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
                            section, portal.openSession(), category = group, search = portalTerm
                        )
                        // A portal that ignores the search parameter returns unrelated titles.
                        page.takeIf {
                            it.titles.all { title ->
                                matchesSearch(title.name, portalTerm) &&
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
    // The portal was asked for one word only; all words of the search must match.
    val remote = available.flatMap { it.titles }.filter { matchesSearch(it.name, term) }.map { SourceItem(source, it) }
    val missing = pages.any { it == null }
    val titles = (remote + local.titles).distinctBy { "${it.value.id ?: it.value.name}:${it.value.categoryId}" }
    val morePages = available.any { it.nextPage != null }
    // The portal total counts the one-word query; it only applies when no narrowing happened.
    val narrowed = portalTerm != term.trim()
    return PortalResults(
        titles,
        when {
            missing -> null
            !narrowed -> available.sumOf { it.total ?: it.titles.size }
            !morePages -> titles.size
            else -> null
        },
        incomplete = missing || morePages
    )
}

private fun localResults(
    source: SavedPortal, section: PortalSection, term: String, countries: Set<String>
): PortalResults {
    val catalog = source.catalogs[section]
    val groups = catalog?.groups.orEmpty().associateBy { it.id }
    val hits = catalog?.titles.orEmpty().filter {
        val group = groups[it.categoryId]
        matchesSearch(it.name, term) &&
            (countries.isEmpty() || categoryCountry(
                group?.name ?: it.categoryName ?: it.categoryId.orEmpty(), group?.country
            ) in countries)
    }
    return PortalResults(hits.map { SourceItem(source, it) }, null, incomplete = true)
}

private val searchSeparators = Regex("""[^\p{L}\p{N}]+""")

private fun searchWords(text: String): List<String> =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC).lowercase(java.util.Locale.ROOT)
        .split(searchSeparators).filter(String::isNotEmpty)

/**
 * Whether [name] matches every word of [term], ignoring case and punctuation, so
 * "spider-man brand new" and "spiderman" both find "Spider-Man: Brand New Day".
 */
internal fun matchesSearch(name: String, term: String): Boolean {
    val query = searchWords(term)
    if (query.isEmpty()) return false
    val words = searchWords(name)
    val joined = words.joinToString("")
    return query.all { part -> words.any { it.startsWith(part) } || joined.contains(part) }
}

/**
 * Portals usually search for the literal text. The longest word of the search is sent
 * and the results are narrowed down to all words locally.
 */
internal fun portalSearchTerm(term: String): String =
    term.trim().split(Regex("""\s+""")).maxByOrNull { word -> searchWords(word).sumOf { it.length } }
        ?.trim { !it.isLetterOrDigit() }?.takeIf { it.isNotEmpty() } ?: term.trim()

/** Deletes the search history of the admin and the kids account. */
internal fun clearSearchHistories(preferences: android.content.SharedPreferences) {
    preferences.edit { SEARCH_HISTORY_KEYS.forEach { remove(it) } }
}
