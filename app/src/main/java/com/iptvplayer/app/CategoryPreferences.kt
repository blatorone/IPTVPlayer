package com.iptvplayer.app

import java.text.Normalizer
import java.util.Locale

internal const val UNKNOWN_COUNTRY = "unknown"
internal const val ALL_CATALOG_ITEMS = "*"

private val countryCodes = Locale.getISOCountries().toSet() + "XK"
private val regionLocales = countryCodes.associateWith { Locale.Builder().setRegion(it).build() }
private val alternateCodes = mapOf(
    "UK" to "GB", "GBR" to "GB", "GER" to "DE", "SUI" to "CH",
    "NED" to "NL", "GRE" to "GR", "EL" to "GR", "UAE" to "AE",
    "KSA" to "SA", "POR" to "PT", "RSA" to "ZA", "KOS" to "XK",
    "XKX" to "XK", "CRO" to "HR", "SLO" to "SI", "BIH" to "BA"
)
private val alpha3Codes = regionLocales.mapNotNull { (code, locale) ->
    runCatching { locale.getISO3Country() }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { it to code }
}.toMap()
private val countryNames by lazy {
    val languages = listOf(
        "de", "en", "fr", "es", "it", "pt", "nl", "tr", "pl", "ro", "ru", "uk", "ar"
    ).map(Locale::forLanguageTag)
    regionLocales.flatMap { (code, region) ->
        languages.flatMap { nameLocale ->
            val name = normalizeCountryText(region.getDisplayCountry(nameLocale))
            listOf(name to code, withoutAccents(name) to code)
        }
    }.filter { (name, _) -> name.length >= 4 }.groupBy({ it.first }, { it.second })
        .mapNotNull { (name, codes) -> codes.distinct().singleOrNull()?.let { name to it } }.toMap() +
        mapOf("SUEDKOREA" to "KR", "TSCHECHIEN" to "CZ", "KOSOVO" to "XK")
}

private val bracketedLabel = Regex("[\\[({<【「『]([^\\])}>】」』]{1,80})[\\])}>】」』]")
private val categoryDivider = Regex("[|¦｜┃│:/\\\\_·•,;–—-]+")
private val barePrefix = Regex("^([A-Z]{2,3})\\s+")
private val bareSuffix = Regex("\\s+([A-Z]{2,3})$")
private val formatCharacters = Regex("\\p{Cf}")
private val whitespace = Regex("[\\p{Z}\\s]+")
private val accents = Regex("\\p{M}+")

private fun normalizeCountryText(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFKC).replace(formatCharacters, "")
        .replace(whitespace, " ").trim().uppercase(Locale.ROOT)

private fun withoutAccents(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD).replace(accents, "")

private fun countryToken(value: String, explicit: Boolean = false): String? {
    val token = normalizeCountryText(value)
    if (!explicit && token in setOf("TV", "SD")) return null
    if (token in countryCodes) return token
    alternateCodes[token]?.let { return it }
    alpha3Codes[token]?.let { return it }
    return countryNames[token] ?: countryNames[withoutAccents(token)]
}

private fun flagCountries(text: String): Set<String> = buildSet {
    val letters = text.codePoints().toArray()
    var index = 0
    while (index + 1 < letters.size) {
        val first = letters[index] - 0x1F1E6
        val second = letters[index + 1] - 0x1F1E6
        if (first in 0..25 && second in 0..25) {
            val code = "${'A' + first}${'A' + second}"
            if (code in countryCodes) add(code)
            index += 2
        } else index++
    }
}

// Country detection depends only on the text; category labels repeat for thousands of channels.
private val countryCache = java.util.concurrent.ConcurrentHashMap<String, String>()

internal fun categoryCountry(name: String, explicit: String? = null): String {
    val key = if (explicit == null) name else name + '\u0000' + explicit
    countryCache[key]?.let { return it }
    val country = detectCategoryCountry(name, explicit)
    if (countryCache.size > 50_000) countryCache.clear()
    countryCache[key] = country
    return country
}

private fun detectCategoryCountry(name: String, explicit: String?): String {
    val field = explicit.orEmpty()
    countryToken(field, explicit = true)?.let { return it }
    val explicitMarkers = markedCountries(field)
    if (explicitMarkers.isNotEmpty()) return explicitMarkers.singleOrNull() ?: UNKNOWN_COUNTRY

    val label = Normalizer.normalize(name, Normalizer.Form.NFKC).replace(formatCharacters, "").trim()
    countryToken(label)?.let { return it }
    val marked = markedCountries(label)
    if (marked.isNotEmpty()) return marked.singleOrNull() ?: UNKNOWN_COUNTRY
    val bare = listOfNotNull(barePrefix.find(label), bareSuffix.find(label))
        .mapNotNull { countryToken(it.groupValues[1]) }.toSet()
    return bare.singleOrNull() ?: UNKNOWN_COUNTRY
}

private fun markedCountries(label: String): Set<String> = buildSet {
    addAll(flagCountries(label))
    bracketedLabel.findAll(label).forEach { match ->
        countryToken(match.groupValues[1])?.let(::add)
    }
    if (categoryDivider.containsMatchIn(label)) {
        categoryDivider.split(label).forEach { part ->
            val cleaned = part.trim().trim('[', ']', '(', ')', '{', '}', '<', '>', '【', '】')
            countryToken(cleaned)?.let(::add)
        }
    }
}

internal fun countryLabel(code: String): String =
    Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.GERMAN)

internal fun categoryKey(section: PortalSection, id: String): String = "${section.name}:$id"

internal fun PortalCatalog.availableGroups(fallback: String): List<PortalGroup> {
    if (groups.isNotEmpty()) return groups
    val detected = titles.mapNotNull { title ->
        val id = title.categoryId ?: return@mapNotNull null
        PortalGroup(id, title.categoryName ?: id)
    }.distinctBy { it.id }
    return detected.ifEmpty {
        if (titles.isEmpty()) emptyList() else listOf(PortalGroup(ALL_CATALOG_ITEMS, fallback))
    }
}
