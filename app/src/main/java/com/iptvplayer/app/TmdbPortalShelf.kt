package com.iptvplayer.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class TmdbShelfResult(
    val titles: Map<Int, Pair<PortalTitle, List<SourceItem<PortalTitle>>>>,
    val nextPages: Map<String, Int>,
    val incomplete: Boolean
)

internal fun VodCategory.requiresTmdb(): Boolean = service || this in setOf(
    VodCategory.POPULAR, VodCategory.RECENT, VodCategory.THIS_YEAR,
    VodCategory.CINEMA, VodCategory.CINEMA_THIS_YEAR, VodCategory.CINEMA_LAST_YEAR
)

internal fun tmdbShelfRegions(
    category: VodCategory, selectedCountries: Set<String>, fallback: String
): List<String> {
    val regional = category.service || category in setOf(
        VodCategory.CINEMA, VodCategory.CINEMA_THIS_YEAR, VodCategory.CINEMA_LAST_YEAR
    )
    val selected = selectedCountries.filter { it.matches(Regex("[A-Z]{2}")) }
    return if (regional && selected.isNotEmpty()) selected
    else listOf(fallback.takeIf { it.matches(Regex("[A-Z]{2}")) } ?: "DE")
}

/*
 * A portal catalogue indexed by normalised title, built once per catalogue. Before, every TMDB
 * title was compared with every catalogue entry (tens of thousands) of every portal.
 */
private class TitleIndex(val titles: List<PortalTitle>, val byName: Map<String, List<PortalTitle>>)
private val titleIndexes = ArrayDeque<TitleIndex>()

private fun titleIndex(titles: List<PortalTitle>): Map<String, List<PortalTitle>> = synchronized(titleIndexes) {
    titleIndexes.firstOrNull { it.titles === titles }?.byName ?: titles.groupBy { normalizedLabel(displayVodTitle(it)) }.also {
        titleIndexes.addFirst(TitleIndex(titles, it))
        while (titleIndexes.size > 12) titleIndexes.removeLast()
    }
}

/** Shelves already loaded in this session: opening the category again shows them at once. */
internal object TmdbShelfCache {
    private const val MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private class Entry(val result: TmdbShelfResult, val savedAt: Long)
    private val entries = HashMap<String, Entry>()

    fun get(key: String): TmdbShelfResult? = synchronized(entries) {
        entries[key]?.takeIf { System.currentTimeMillis() - it.savedAt < MAX_AGE_MS }?.result
    }

    fun put(key: String, result: TmdbShelfResult) = synchronized(entries) {
        entries[key] = Entry(result, System.currentTimeMillis())
        if (entries.size > 40) entries.remove(entries.minByOrNull { it.value.savedAt }?.key)
    }
}

internal suspend fun loadTmdbPortalShelf(
    client: TmdbClient,
    section: PortalSection,
    category: VodCategory,
    regionPages: Map<String, Int>,
    sources: List<SavedPortal>,
    countries: Set<String>,
    portalFor: (SavedPortal) -> StalkerPortal
): TmdbShelfResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
    val limiter = Semaphore(4)
    // Searches run per portal (3 at a time each) instead of 4 for all portals together.
    val searchLimits = sources.associateWith { Semaphore(3) }
    val references = coroutineScope {
        regionPages.map { (region, page) ->
            async {
                try {
                    region to limiter.withPermit { client.shelf(section, category, region, page) }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    region to null
                }
            }
        }.map { it.await() }
    }
    check(references.any { it.second != null })
    val referenceItems = references.flatMap { it.second?.items.orEmpty() }.distinctBy { it.id }
    val bySource = sources.associateWith { source ->
        val catalog = source.catalogs[section]
        val groups = catalog?.groups.orEmpty().associateBy { it.id }
        fun allowed(title: PortalTitle): Boolean {
            val group = groups[title.categoryId]
            return countries.isEmpty() || categoryCountry(
                group?.name ?: title.categoryName ?: title.categoryId.orEmpty(), group?.country
            ) in countries
        }
        titleIndex(catalog?.titles.orEmpty()) to { title: PortalTitle -> allowed(title) }
    }

    val matched = coroutineScope {
        referenceItems.map { item ->
            async {
                val hits = sources.map { source ->
                    async sourceLookup@ {
                        val (index, allowed) = bySource.getValue(source)
                        val cached = setOf(item.title, item.originalTitle).filterNotNull().map(::normalizedLabel)
                            .flatMap { index[it].orEmpty() }.distinct().filter(allowed)
                        val known = matchTmdbTitle(item, cached, requireVerifiedIdentity = section == PortalSection.SERIES)
                        if (known != null) return@sourceLookup SourceItem(source, known)
                        try {
                            val found = searchLimits.getValue(source).withPermit {
                                val portal = portalFor(source)
                                portal.loadCatalog(section, portal.openSession(), search = item.title)
                            }
                            matchTmdbTitle(item, found.titles.filter(allowed), requireVerifiedIdentity = section == PortalSection.SERIES)
                                ?.let { SourceItem(source, it) }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.mapNotNull { it.await() }
                hits.firstOrNull()?.let { first -> item.id to (item.applyTo(first.value) to hits) }
            }
        }.mapNotNull { it.await() }
    }
    val next = references.mapNotNull { (region, page) ->
        (if (page == null) regionPages[region] else page.nextPage)?.let { region to it }
    }.toMap()
    TmdbShelfResult(matched.toMap(), next, references.any { it.second == null })
}

internal fun matchTmdbTitle(
    reference: TmdbTitle, candidates: List<PortalTitle>, requireVerifiedIdentity: Boolean = false
): PortalTitle? {
    val relevant = candidates.filter(reference::matches)
    relevant.firstOrNull { it.tmdbId?.toIntOrNull() == reference.id }?.let { return it }
    val exactYear = relevant.filter { reference.year != null && vodReleaseYear(it) == reference.year }
    if (requireVerifiedIdentity && exactYear.isEmpty()) return null
    val choices = exactYear.ifEmpty { relevant }
    val externalIds = choices.mapNotNull { it.tmdbId?.toIntOrNull()?.takeIf { id -> id > 0 } }.distinct()
    if (externalIds.size > 1 || externalIds.any { it != reference.id }) return null
    if (choices.mapNotNull(::vodReleaseYear).distinct().size > 1) return null
    return choices.firstOrNull()
}
