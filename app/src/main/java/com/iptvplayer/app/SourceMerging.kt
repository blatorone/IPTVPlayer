package com.iptvplayer.app

import java.text.Normalizer
import java.util.Locale

internal data class SourceItem<T>(val source: SavedPortal, val value: T)

private val labelWhitespace = Regex("\\s+")

internal fun normalizedLabel(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).trim()
        .replace(labelWhitespace, " ").lowercase(Locale.ROOT)

internal fun channelIdentity(channel: StalkerChannel): String? =
    channel.epgIdentity?.let(::normalizedLabel)
        ?.takeIf { it.any(Char::isLetter) && it !in setOf("null", "none", "unknown", "n/a") }
        ?.let { "epg:$it" }

internal fun titleIdentity(title: PortalTitle): String? {
    val info = fingerprint(title)
    return when {
        info.imdb != null -> "imdb:${info.imdb}"
        info.tmdb != null -> "tmdb:${info.tmdb}"
        info.year != null && info.name.isNotBlank() -> "title:${info.year}:${info.name}"
        else -> null
    }
}

private val titleYear = Regex("(?:(?<=\\()|(?<=\\[)|(?<=\\s))(18[89][0-9]|19[0-9]{2}|20[0-9]{2})[)\\]]?$")
private val imdbCode = Regex("\\btt[0-9]+\\b", RegexOption.IGNORE_CASE)
private val decorativeLanguage = Regex(
    "\\s*(?:[\\[(](?:GERMAN|DEUTSCH|ENGLISH|MULTI|MULTi-AUDIO|DUBBED|SUBBED)[\\])])\\s*$",
    RegexOption.IGNORE_CASE
)

internal fun displayVodTitle(title: PortalTitle): String {
    val decoratedName = title.name.replace(decorativeLanguage, "").trim()
    val channel = StalkerChannel(decoratedName, "", null, null)
    val cleaned = describeChannel(channel, null).name
        .replace(decorativeLanguage, "").trim()
    val year = titleYear.find(cleaned)
    val withoutYear = if (year == null) cleaned else cleaned.take(year.range.first).trimEnd(' ', '(', '[')
        .ifBlank { cleaned }
    // Portals often name titles "Name - 2026"; without the year a dangling separator would
    // remain ("Name -") and prevent matching the title with Trakt or TMDB.
    return withoutYear.trimEnd { it.isWhitespace() || it in trailingSeparators }.ifBlank { withoutYear }
}

private const val trailingSeparators = "-–—:|·,/"

private data class TitleFingerprint(
    val name: String, val year: Int?, val imdb: String?, val tmdb: Long?, val country: String
)

private fun fingerprint(title: PortalTitle): TitleFingerprint {
    val channel = StalkerChannel(title.name.replace(decorativeLanguage, ""), "", null, null)
    val cleaned = describeChannel(channel, null)
    val year = title.year?.takeIf { it in 1880..2200 }
        ?: titleYear.find(cleaned.name)?.groupValues?.get(1)?.toIntOrNull()
    return TitleFingerprint(
        normalizedLabel(displayVodTitle(title)), year,
        title.imdbId?.let { imdbCode.find(it)?.value?.lowercase(Locale.ROOT) },
        title.tmdbId?.toLongOrNull()?.takeIf { it > 0 },
        cleaned.country.takeUnless { it == UNKNOWN_COUNTRY } ?: categoryCountry(title.categoryName.orEmpty())
    )
}

internal fun vodReleaseYear(title: PortalTitle): Int? = fingerprint(title).year

private fun TitleFingerprint.hasConflictingId(other: TitleFingerprint): Boolean =
    (imdb != null && other.imdb != null && imdb != other.imdb) ||
        (tmdb != null && other.tmdb != null && tmdb != other.tmdb)

private fun TitleFingerprint.matches(other: TitleFingerprint, unambiguous: Boolean): Boolean {
    if (hasConflictingId(other)) return false
    if ((imdb != null && imdb == other.imdb) || (tmdb != null && tmdb == other.tmdb)) return true
    if (name.isBlank() || name != other.name) return false
    if (year != null && other.year != null) return year == other.year
    return unambiguous && (country == UNKNOWN_COUNTRY || other.country == UNKNOWN_COUNTRY || country == other.country)
}

internal fun mergeCatalogueTitles(
    items: List<SourceItem<PortalTitle>>, requireVerifiedIdentity: Boolean = false
): List<List<SourceItem<PortalTitle>>> {
    val metadata = items.map { fingerprint(it.value) }
    val unambiguousNames = metadata.groupBy { it.name }.filterValues { titles ->
        titles.mapNotNull { it.year }.distinct().size <= 1 &&
            titles.mapNotNull { it.imdb }.distinct().size <= 1 &&
            titles.mapNotNull { it.tmdb }.distinct().size <= 1
    }.keys
    val groups = mutableListOf<MutableList<Int>>()
    val lookup = mutableMapOf<String, MutableSet<Int>>()
    fun keys(info: TitleFingerprint): List<String> = listOfNotNull(
        "name:${info.name}", info.imdb?.let { "imdb:$it" }, info.tmdb?.let { "tmdb:$it" }
    )
    for ((index, info) in metadata.withIndex()) {
        val keys = keys(info)
        val contenders = keys.flatMap { lookup[it].orEmpty() }.distinct().filter { group ->
            val entries = groups[group]
            val unambiguous = info.name in unambiguousNames
            entries.none { info.hasConflictingId(metadata[it]) } &&
                entries.any { old ->
                    val previous = metadata[old]
                    val allowInferredMatch = !requireVerifiedIdentity && unambiguous &&
                        items[index].source.identity != items[old].source.identity
                    info.matches(previous, allowInferredMatch)
                }
        }
        val group = contenders.singleOrNull() ?: groups.size.also { groups.add(mutableListOf()) }
        groups[group].add(index)
        keys.forEach { lookup.getOrPut(it) { mutableSetOf() }.add(group) }
    }
    return groups.map { indices -> indices.map(items::get) }
}

private fun matchKeys(info: TitleFingerprint): List<String> = listOfNotNull(
    info.name.takeIf { it.isNotBlank() }?.let { "name:$it" },
    info.imdb?.let { "imdb:$it" },
    info.tmdb?.let { "tmdb:$it" }
)

/**
 * Lookup of catalogue titles by normalized name and external IDs. Two titles can only
 * match on one of these keys, so the index yields exactly the titles a full scan would
 * consider, in catalogue order, without fingerprinting the whole catalogue per lookup.
 */
private class CatalogueIndex(private val titles: List<PortalTitle>) {
    private val positions = HashMap<String, MutableList<Int>>()

    init {
        titles.forEachIndexed { index, title ->
            matchKeys(fingerprint(title)).forEach { positions.getOrPut(it) { ArrayList(1) }.add(index) }
        }
    }

    fun candidates(expected: TitleFingerprint): List<PortalTitle> =
        matchKeys(expected).flatMap { positions[it].orEmpty() }.distinct().sorted().map(titles::get)
}

private class CachedCatalogueIndex(titles: List<PortalTitle>) {
    val titles = java.lang.ref.WeakReference(titles)
    var index: CatalogueIndex? = null
}

private val catalogueIndexes = ArrayDeque<CachedCatalogueIndex>()

/**
 * Indexes of the most recently searched catalogue lists, compared by identity. Each list
 * is indexed only once: concurrent lookups wait for the same build instead of all
 * fingerprinting the whole catalogue in parallel.
 */
private fun catalogueIndex(titles: List<PortalTitle>): CatalogueIndex {
    val entry = synchronized(catalogueIndexes) {
        val existing = catalogueIndexes.firstOrNull { it.titles.get() === titles }
        if (existing != null) {
            catalogueIndexes.remove(existing)
            catalogueIndexes.addFirst(existing)
            existing
        } else {
            catalogueIndexes.removeAll { it.titles.get() == null }
            CachedCatalogueIndex(titles).also {
                catalogueIndexes.addFirst(it)
                while (catalogueIndexes.size > 8) catalogueIndexes.removeLast()
            }
        }
    }
    synchronized(entry) {
        return entry.index ?: CatalogueIndex(titles).also { entry.index = it }
    }
}

private fun pickEquivalent(
    expected: TitleFingerprint, relevant: List<PortalTitle>, requireVerifiedIdentity: Boolean
): PortalTitle? {
    val matches = relevant.map { it to fingerprint(it) }
        .filter { (_, candidate) -> expected.matches(candidate, !requireVerifiedIdentity) }
    if (matches.mapNotNull { it.second.year }.distinct().size > 1 ||
        matches.mapNotNull { it.second.imdb }.distinct().size > 1 ||
        matches.mapNotNull { it.second.tmdb }.distinct().size > 1
    ) return null
    return matches.firstOrNull()?.first
}

internal fun equivalentCatalogueTitle(
    title: PortalTitle, candidates: List<PortalTitle>, requireVerifiedIdentity: Boolean = false
): PortalTitle? {
    val expected = fingerprint(title)
    val relevant = if (candidates.size >= 256) catalogueIndex(candidates).candidates(expected) else candidates
    return pickEquivalent(expected, relevant, requireVerifiedIdentity)
}

/**
 * A prepared lookup for many searches in one catalogue, for example a Trakt import. It
 * returns the same result as [equivalentCatalogueTitle] and is released with its owner.
 */
internal class CatalogueMatcher(titles: List<PortalTitle>) {
    private val index = CatalogueIndex(titles)

    fun find(title: PortalTitle, requireVerifiedIdentity: Boolean = false): PortalTitle? {
        val expected = fingerprint(title)
        return pickEquivalent(expected, index.candidates(expected), requireVerifiedIdentity)
    }
}

internal fun <T> mergeSourceItems(
    items: Iterable<SourceItem<T>>,
    identity: (T) -> String?
): List<List<SourceItem<T>>> {
    val groups = mutableListOf<MutableList<SourceItem<T>>>()
    val byIdentity = mutableMapOf<String, MutableList<SourceItem<T>>>()
    for (item in items) {
        val key = identity(item.value)
        val group = key?.let(byIdentity::get)
        if (group != null) {
            group.add(item)
        } else {
            val added = mutableListOf(item)
            groups.add(added)
            if (key != null) byIdentity.putIfAbsent(key, added)
        }
    }
    return groups
}
