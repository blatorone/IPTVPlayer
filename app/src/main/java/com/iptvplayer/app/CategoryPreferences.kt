package com.iptvplayer.app

import java.util.Locale

internal const val UNKNOWN_COUNTRY = "unknown"
internal const val ALL_CATALOG_ITEMS = "*"

private val countryCodes = Locale.getISOCountries().toSet()
private val countryNames = countryCodes.flatMap { code ->
    val locale = Locale.Builder().setRegion(code).build()
    listOf(Locale.GERMAN, Locale.ENGLISH).map {
        locale.getDisplayCountry(it).lowercase(Locale.ROOT) to code
    }
}.toMap()

private val countryPrefix = Regex(
    "^\\s*(?:\\[([A-Za-z]{2})\\]|([A-Za-z]{2})\\s*[|:·–—-])"
)

internal fun categoryCountry(name: String, explicit: String? = null): String {
    val value = explicit?.trim().orEmpty()
    val code = value.uppercase(Locale.ROOT)
    if (code in countryCodes) return code
    countryNames[value.lowercase(Locale.ROOT)]?.let { return it }
    countryNames[name.trim().lowercase(Locale.ROOT)]?.let { return it }
    val prefix = countryPrefix.find(name)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
        ?.uppercase(Locale.ROOT)
    return if (prefix == "UK") "GB" else prefix?.takeIf { it in countryCodes } ?: UNKNOWN_COUNTRY
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
