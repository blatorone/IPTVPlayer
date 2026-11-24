package com.iptvplayer.app

import java.text.Normalizer

internal enum class LiveCategory(val label: Int, val terms: String) {
    GENERAL(R.string.live_category_general, "allgemein(?:es)?|general|entertainment|unterhaltung|hauptsender"),
    NEWS(R.string.live_category_news, "news|nachrichten|nachrichtensender|information|info"),
    DAZN(R.string.live_category_dazn, "dazn"),
    MAGENTA(R.string.live_category_magenta, "magenta[ -]?sport"),
    TENNIS(R.string.live_category_tennis, "tennis|wimbledon|atp|wta|roland garros"),
    RACING(R.string.live_category_racing, "motorsport|formula 1|formel 1|f1|motogp|nascar"),
    BASKETBALL(R.string.live_category_basketball, "basketball|nba|wnba|euroleague"),
    HOCKEY(R.string.live_category_hockey, "eishockey|ice hockey|nhl"),
    AMERICAN_FOOTBALL(R.string.live_category_american_football, "american football|nfl"),
    FOOTBALL(R.string.live_category_football, "fußball|fussball|football|soccer|bundesliga|champions league|premier league|la liga"),
    SPORTS(R.string.live_category_sports, "sport|sports|sportkanäle"),
    MOVIES(R.string.live_category_movies, "filme?|movies?|kino|cinema|serien|series|sky atlantic"),
    KIDS(R.string.live_category_kids, "kinder|kids|cartoons?|children|jugend"),
    DOCUMENTARY(R.string.live_category_documentary, "doku|dokus|dokumentation(?:en)?|documentar(?:y|ies)|wissen|sky nature"),
    MUSIC(R.string.live_category_music, "musik|music|radio"),
    REGIONAL(R.string.live_category_regional, "regional|lokal|local"),
    SHOPPING(R.string.live_category_shopping, "shopping|teleshopping"),
    RELIGION(R.string.live_category_religion, "religion|religious|kirche"),
    SKY(R.string.live_category_sky, "sky");

    val words = Regex("(?:^|[^\\p{L}\\p{N}])(?:$terms)(?=$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
}

internal data class ChannelDescription(
    val name: String,
    val country: String,
    val category: LiveCategory,
    val qualities: List<String>
) {
    val normalizedName: String get() = normalizedLabel(name)
}

internal data class LiveStreamVariant(
    val item: SourceItem<StalkerChannel>,
    val description: ChannelDescription
)

internal data class LiveChannelGroup(val variants: List<LiveStreamVariant>) {
    val description: ChannelDescription
        get() {
            val first = variants.first().description
            val category = if (variants.any { generalStation.matches(it.description.name) }) {
                LiveCategory.GENERAL
            } else {
                variants.firstOrNull { it.description.category != LiveCategory.GENERAL }
                    ?.description?.category ?: LiveCategory.GENERAL
            }
            return first.copy(category = category)
        }
    val identity: String? get() = comparisonName(description) ?: channelIdentity(variants.first().item.value)
}

private val separatorSegments = Regex("\\s*[|¦┃│]\\s*")
private val leftMarker = Regex("^\\s*(?:[\\[({<【]([^\\])}>】]+)[\\])}>】]|[|¦┃│]\\s*([^|¦┃│]+?)\\s*[|¦┃│])\\s*")
private val rightMarker = Regex("\\s*[\\[({<【]([^\\])}>】]+)[\\])}>】]\\s*$")
private val leadingCode = Regex("^([A-Za-z]{2,3})(?:\\s*[-_:·/]+\\s*|\\s+)(?=\\S)")
private val leadingFlag = Regex("^\\s*[\\x{1F1E6}-\\x{1F1FF}]{2}\\s*")
private val qualityOptions =
    "FULL[ ._-]*HD\\+?|ULTRA[ ._-]*HD\\+?|FHD\\+?|HD[ ._-]*PLUS|UHD\\+?|" +
        "(?:(?:REAL|TRUE)[ ._-]+)?[48][ ._-]*K\\+?|(?:2160|1080|720|576|480)[PI](?:[256][0450])?|" +
        "RAW\\+?|HD\\+?|SD\\+?|HEVC|[HX][ ._-]*26[45]|AV1|HDR(?:10\\+?)?|DOLBY[ ._-]*VISION"
private val qualitySuffix = Regex(
    "(?:^|[\\s|_:·/-]+|[\\[(])([\\[(]?\\s*(?:$qualityOptions)\\s*[\\])]?)\\s*$",
    RegexOption.IGNORE_CASE
)
private val qualityPrefix = Regex(
    "^([\\[(]?\\s*(?:$qualityOptions)\\s*[\\])]?)\\s*(?:[|_:·/-]+\\s*|\\s+)(?=\\S)",
    RegexOption.IGNORE_CASE
)
private val trimSeparators = " \t|¦┃│:·_-"
private val generalStation = Regex(
    "pro[ ._-]?(?:sieben|7)(?:[ ._-]*(?:fun|maxx))?|" +
        "das erste|ard|zdf(?:neo)?|rtl(?:[ ._-]*(?:ii|2|up|plus))?|super[ ._-]*rtl|" +
        "sat[ ._-]?1(?:[ ._-]*gold)?|vox(?:[ ._-]*up)?|kabel[ ._-]?(?:eins|1)|" +
        "nitro|tele[ ._-]?5|sixx|3sat|arte",
    RegexOption.IGNORE_CASE
)

// The description depends only on the channel name and provider category; the same names repeat
// across portals and every regrouping (country selection, refresh) would parse them again.
private val descriptionCache = java.util.concurrent.ConcurrentHashMap<String, ChannelDescription>()

internal fun describeChannel(channel: StalkerChannel, providerCategory: String?): ChannelDescription {
    val key = channel.name + '\u0000' + providerCategory.orEmpty()
    descriptionCache[key]?.let { return it }
    val description = parseChannelDescription(channel, providerCategory)
    if (descriptionCache.size > 60_000) descriptionCache.clear()
    descriptionCache[key] = description
    return description
}

private fun parseChannelDescription(channel: StalkerChannel, providerCategory: String?): ChannelDescription {
    val providerLabel = providerCategory.orEmpty()
    val countryInName = categoryCountry(channel.name)
    val country = if (countryInName == UNKNOWN_COUNTRY) categoryCountry(providerLabel) else countryInName
    val fullName = Normalizer.normalize(channel.name, Normalizer.Form.NFKC)
    val normalizedProvider = Normalizer.normalize(providerLabel, Normalizer.Form.NFKC).trim()
    val input = if (normalizedProvider.isNotBlank() &&
        fullName.startsWith(normalizedProvider, ignoreCase = true) &&
        fullName.getOrNull(normalizedProvider.length)?.let { it.isWhitespace() || it in trimSeparators } == true
    ) fullName.drop(normalizedProvider.length).trimStart() else fullName
    val qualities = linkedSetOf<String>()
    val pieces = separatorSegments.split(input).map { it.trim() }.filter { it.isNotEmpty() }
    var name = pieces.dropWhile { part ->
        pieces.size > 1 && (isCountryMarker(part, country) ||
            part.equals(providerLabel, ignoreCase = true) ||
            LiveCategory.entries.any { it.words.matchEntire(part) != null })
    }.joinToString(" ").ifBlank { input }
    while (true) {
        val trimmed = name.trim { it in trimSeparators }
        val left = leftMarker.find(trimmed)?.takeIf {
            isCountryMarker(it.groupValues[1].ifBlank { it.groupValues[2] }, country)
        }
        val right = rightMarker.find(trimmed)?.takeIf { isCountryMarker(it.groupValues[1], country) }
        val code = leadingCode.find(trimmed)?.takeIf {
            val marker = it.groupValues[1]
            (marker == marker.uppercase(java.util.Locale.ROOT) || it.value.any { character ->
                character in "-_:·/"
            }) && isCountryMarker(marker, country)
        }
        val flag = leadingFlag.find(trimmed)?.takeIf { isCountryMarker(it.value, country) }
        val suffix = qualitySuffix.find(trimmed)?.takeIf { it.range.first > 0 }
        val prefix = qualityPrefix.find(trimmed)
        name = when {
            left != null -> trimmed.drop(left.range.last + 1)
            right != null -> trimmed.take(right.range.first)
            flag != null -> trimmed.drop(flag.range.last + 1)
            code != null -> trimmed.drop(code.range.last + 1)
            suffix != null -> {
                qualities.add(qualityName(suffix.groupValues[1]))
                trimmed.take(suffix.range.first)
            }
            prefix != null -> {
                qualities.add(qualityName(prefix.groupValues[1]))
                trimmed.drop(prefix.range.last + 1)
            }
            else -> {
                name = trimmed
                break
            }
        }
    }
    name = name.replace(Regex("\\s+"), " ").trim().ifBlank { channel.name.trim() }
    val category = if (generalStation.matches(name)) LiveCategory.GENERAL
    else LiveCategory.entries.drop(1).firstOrNull { it.words.containsMatchIn(name) }
        ?: LiveCategory.entries.firstOrNull { it.words.containsMatchIn(providerLabel) }
        ?: LiveCategory.GENERAL
    return ChannelDescription(name, country, category, qualities.toList())
}

private fun isCountryMarker(value: String, country: String): Boolean =
    country != UNKNOWN_COUNTRY && categoryCountry(value) == country

private fun qualityName(value: String): String {
    val token = value.uppercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() || it == '+' }
    return when (token) {
        "FULLHD", "FHD+", "1080P", "1080I" -> "FHD"
        "HDPLUS" -> "HD+"
        "720P", "720I" -> "HD"
        "ULTRAHD", "2160P", "4K" -> "UHD"
        "576P", "576I", "480P", "480I" -> "SD"
        else -> token
    }
}

private fun comparisonName(description: ChannelDescription): String? =
    description.normalizedName.takeIf { it.isNotEmpty() && description.country != UNKNOWN_COUNTRY }
        ?.let { "station:${description.country}:$it" }

internal fun buildLiveChannelGroups(
    sources: List<SavedPortal>,
    combine: Boolean,
    mergeWithinSource: Boolean = true,
    countries: Set<String> = emptySet()
): List<LiveChannelGroup> {
    val variants = sources.flatMap { source ->
        val channels = if (!combine && !mergeWithinSource) source.channels
        else source.channels.sortedBy { it.number ?: Long.MAX_VALUE }
        channels.map { channel ->
            val category = channel.genreName ?: source.genres[channel.genreId]
            LiveStreamVariant(SourceItem(source, channel), describeChannel(channel, category))
        }
    }
    if (!combine && !mergeWithinSource) return variants
        .asSequence()
        .filter { countries.isEmpty() || it.description.country in countries }
        .map { LiveChannelGroup(listOf(it)) }
        .toList()
    val known = variants.filter { it.description.country != UNKNOWN_COUNTRY }
        .groupBy({ it.description.normalizedName }, { it.description.country })
        .mapValues { (_, countries) -> countries.distinct().singleOrNull() }
    val normalized = variants.map { item ->
        val description = item.description
        val inferred = if (description.country == UNKNOWN_COUNTRY) known[description.normalizedName] else null
        if (inferred == null) item else item.copy(description = description.copy(country = inferred))
    }
    val selected = if (countries.isEmpty()) normalized else
        normalized.filter { it.description.country in countries }
    if (selected.isEmpty()) return emptyList()
    val parent = IntArray(selected.size) { it }
    fun root(index: Int): Int {
        var next = index
        while (parent[next] != next) {
            parent[next] = parent[parent[next]]
            next = parent[next]
        }
        return next
    }
    val knownKeys = mutableMapOf<String, Int>()
    selected.forEachIndexed { index, item ->
        val description = item.description
        val name = comparisonName(description)
        val epg = channelIdentity(item.item.value)?.let {
            val edition = Regex("\\+\\s*\\d*$").find(description.normalizedName)?.value.orEmpty()
            "$it:${description.country}:${description.normalizedName}:$edition"
        }
        for (key in listOfNotNull(name, epg)) {
            val scoped = if (combine) key else "${item.item.source.identity}:$key"
            val older = knownKeys.putIfAbsent(scoped, index) ?: continue
            val priorRoot = root(older)
            val itemRoot = root(index)
            parent[maxOf(priorRoot, itemRoot)] = minOf(priorRoot, itemRoot)
        }
    }
    val groups = selected.indices.groupBy({ root(it) }, { selected[it] })
    return groups.values.map { entries ->
        LiveChannelGroup(entries.distinctBy { it.item.source.identity to it.item.value.command })
    }
}
