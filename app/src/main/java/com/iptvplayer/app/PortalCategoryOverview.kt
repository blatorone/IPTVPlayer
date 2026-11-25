package com.iptvplayer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/** A category as the provider lists it, with the country the app recognises in it. */
internal data class ProviderCategory(val name: String, val country: String, val count: Int?)

private const val MAX_CATEGORIES = 3_000

/**
 * The provider's own categories of one portal (before any grouping by the app), so one can see
 * which countries a portal offers. Live TV counts the stored channels per category.
 */
internal fun providerCategories(source: SavedPortal): Map<PortalSection, List<ProviderCategory>> {
    val channelsPerGenre = source.channels.groupingBy { it.genreId }.eachCount()
    val live = source.genres.entries.take(MAX_CATEGORIES).map { (id, name) ->
        ProviderCategory(name, categoryCountry(name), channelsPerGenre[id])
    }
    fun vod(section: PortalSection): List<ProviderCategory> {
        val catalog = source.catalogs[section] ?: return emptyList()
        val titlesPerGroup = catalog.titles.groupingBy { it.categoryId }.eachCount()
        return catalog.groups.take(MAX_CATEGORIES).map { group ->
            ProviderCategory(group.name, categoryCountry(group.name, group.country), titlesPerGroup[group.id])
        }
    }
    return mapOf(
        PortalSection.LIVE to live,
        PortalSection.MOVIES to vod(PortalSection.MOVIES),
        PortalSection.SERIES to vod(PortalSection.SERIES)
    )
}

/** "Deutschland 120 · Türkei 40 · …": categories per recognised country, most first. */
internal fun countrySummary(categories: List<ProviderCategory>, label: (String) -> String): String =
    categories.groupingBy { it.country }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { label(it.key) })
        .joinToString(" · ") { (country, count) -> "${label(country)} $count" }

internal fun countryName(code: String): String =
    if (code == UNKNOWN_COUNTRY) "Ohne Land" else runCatching { countryLabel(code) }.getOrDefault(code)

/** For the phone: {"id":…,"sections":{"LIVE":[{"name":…,"country":…,"count":…}],…}}. */
internal fun encodeProviderCategories(id: String, label: String, categories: Map<PortalSection, List<ProviderCategory>>) =
    JsonObject().apply {
        addProperty("id", id)
        addProperty("label", label)
        add("sections", JsonObject().apply {
            categories.forEach { (section, list) ->
                add(section.name, JsonArray().apply {
                    list.forEach { category ->
                        add(JsonObject().apply {
                            addProperty("name", category.name)
                            addProperty("country", countryName(category.country))
                            category.count?.let { addProperty("count", it) }
                        })
                    }
                })
            }
        })
    }

/** Read-only list of a portal's provider categories, by Live-TV, Filme and Serien. */
@Composable
internal fun PortalCategoriesDialog(title: String, source: SavedPortal, onDismiss: () -> Unit) {
    val categories = remember(source) { providerCategories(source) }
    var section by remember { mutableStateOf(PortalSection.LIVE) }
    val list = categories[section].orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        PortalSection.LIVE to R.string.destination_live,
                        PortalSection.MOVIES to R.string.destination_movies,
                        PortalSection.SERIES to R.string.destination_series
                    ).forEach { (item, label) ->
                        MenuButton(
                            stringResource(label) + " (${categories[item].orEmpty().size})",
                            Modifier, selected = section == item
                        ) { section = item }
                    }
                }
                if (list.isEmpty()) Text(stringResource(R.string.portal_categories_empty))
                else {
                    Text(
                        countrySummary(list, ::countryName),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(list) { category ->
                            // Each line can take focus, so the list scrolls with the remote.
                            Row(
                                Modifier.fillMaxWidth().focusable().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(category.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    listOfNotNull(countryName(category.country), category.count?.toString()).joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { MaterialTextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
