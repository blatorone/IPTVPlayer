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

internal suspend fun loadTmdbPortalShelf(
    client: TmdbClient,
    section: PortalSection,
    category: VodCategory,
    regionPages: Map<String, Int>,
    sources: List<SavedPortal>,
    countries: Set<String>,
    portalFor: (SavedPortal) -> StalkerPortal
): TmdbShelfResult {
    val limiter = Semaphore(4)
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
        catalog?.titles.orEmpty().filter(::allowed) to { title: PortalTitle -> allowed(title) }
    }

    val matched = coroutineScope {
        referenceItems.map { item ->
            async {
                val hits = sources.map { source ->
                    async sourceLookup@ {
                        val (cached, allowed) = bySource.getValue(source)
                        val known = matchTmdbTitle(item, cached.filter(item::matches))
                        if (known != null) return@sourceLookup SourceItem(source, known)
                        try {
                            val found = limiter.withPermit {
                                val portal = portalFor(source)
                                portal.loadCatalog(section, portal.openSession(), search = item.title)
                            }
                            matchTmdbTitle(item, found.titles.filter(allowed).filter(item::matches))
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
    return TmdbShelfResult(matched.toMap(), next, references.any { it.second == null })
}

internal fun matchTmdbTitle(reference: TmdbTitle, candidates: List<PortalTitle>): PortalTitle? {
    val relevant = candidates.filter(reference::matches)
    relevant.firstOrNull { it.tmdbId?.toIntOrNull() == reference.id }?.let { return it }
    val exactYear = relevant.filter { reference.year != null && vodReleaseYear(it) == reference.year }
    val choices = exactYear.ifEmpty { relevant }
    val externalIds = choices.mapNotNull { it.tmdbId?.toIntOrNull()?.takeIf { id -> id > 0 } }.distinct()
    if (externalIds.size > 1 || externalIds.any { it != reference.id }) return null
    if (choices.mapNotNull(::vodReleaseYear).distinct().size > 1) return null
    return choices.firstOrNull()
}
