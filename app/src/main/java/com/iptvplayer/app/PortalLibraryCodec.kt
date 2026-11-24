package com.iptvplayer.app

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/*
 * Streaming codec for the saved portal library. It writes the same JSON structure
 * as the earlier org.json snapshot, but never holds the whole document as a string
 * or object tree, which matters for large film and series catalogues on TV devices.
 */

internal fun writePortalLibrary(writer: JsonWriter, library: SavedPortals) {
    writer.beginObject()
    writer.name("activeId").value(library.activeId)
    writer.name("sources").beginArray()
    library.sources.forEach { writePortal(writer, it) }
    writer.endArray()
    writer.endObject()
}

private fun JsonWriter.optional(name: String, value: String?) {
    if (value != null) name(name).value(value)
}

private fun JsonWriter.optional(name: String, value: Number?) {
    if (value != null && (value !is Double || value.isFinite())) name(name).value(value)
}

private fun writePortal(writer: JsonWriter, source: SavedPortal) = with(writer) {
    beginObject()
    name("address").value(source.address)
    name("mac").value(source.mac)
    name("genres").beginObject()
    source.genres.forEach { (id, label) -> name(id).value(label) }
    endObject()
    name("epgChannelIds").beginArray()
    source.epgChannelIds.forEach { value(it) }
    endArray()
    optional("subscriptionExpiry", source.subscriptionExpiry?.toString())
    name("enabled").value(source.enabled)
    name("autoDisabled").value(source.autoDisabled)
    source.reachable?.let { name("reachable").value(it) }
    name("channels").beginArray()
    for (channel in source.channels) {
        beginObject()
        name("name").value(channel.name)
        name("command").value(channel.command)
        optional("genreId", channel.genreId)
        optional("genreName", channel.genreName)
        optional("id", channel.id)
        optional("logo", channel.logo)
        name("direct").value(channel.direct)
        optional("epgIdentity", channel.epgIdentity)
        optional("number", channel.number)
        endObject()
    }
    endArray()
    name("catalogs").beginObject()
    for ((section, catalog) in source.catalogs) {
        name(section.name).beginObject()
        name("titles").beginArray()
        for (title in catalog.titles) {
            beginObject()
            name("name").value(title.name)
            optional("id", title.id)
            optional("command", title.command)
            optional("categoryId", title.categoryId)
            optional("categoryName", title.categoryName)
            optional("poster", title.poster)
            optional("year", title.year)
            optional("addedAt", title.addedAt)
            optional("description", title.description)
            optional("imdbId", title.imdbId)
            optional("tmdbId", title.tmdbId)
            optional("popularity", title.popularity)
            optional("service", title.service)
            endObject()
        }
        endArray()
        optional("total", catalog.total)
        optional("nextPage", catalog.nextPage)
        name("groups").beginArray()
        for (group in catalog.groups) {
            beginObject()
            name("id").value(group.id)
            name("name").value(group.name)
            optional("country", group.country)
            endObject()
        }
        endArray()
        endObject()
    }
    endObject()
    endObject()
}

/** Reads a library; a legacy snapshot that is a single portal object becomes a one-source library. */
internal fun readPortalLibrary(reader: JsonReader): SavedPortals {
    val strings = HashMap<String, String>()
    var activeId: String? = null
    var sources: List<SavedPortal>? = null
    val legacy = PortalFields(strings)
    reader.beginObject()
    while (reader.hasNext()) {
        when (val name = reader.nextName()) {
            "activeId" -> activeId = reader.text()
            "sources" -> sources = reader.list { PortalFields(strings).also { fields -> fields.readObject(this) }.build() }
            else -> legacy.read(name, reader)
        }
    }
    reader.endObject()
    sources?.let { return SavedPortals(it, activeId ?: it.firstOrNull()?.identity.orEmpty()) }
    val source = legacy.build()
    return SavedPortals(listOf(source), source.identity)
}

private class PortalFields(private val strings: HashMap<String, String>) {
    var address: String? = null
    var mac: String? = null
    var channels: List<StalkerChannel> = emptyList()
    var genres: Map<String, String> = emptyMap()
    var catalogs: Map<PortalSection, PortalCatalog> = emptyMap()
    var epgIds: Set<String> = emptySet()
    var expiry: String? = null
    var enabled = true
    var reachable: Boolean? = null
    var autoDisabled = false

    /** Repeated category and genre labels share one string instance. */
    fun shared(value: String?): String? = value?.let { strings.getOrPut(it) { it } }

    fun readObject(reader: JsonReader) {
        reader.beginObject()
        while (reader.hasNext()) read(reader.nextName(), reader)
        reader.endObject()
    }

    fun read(name: String, reader: JsonReader) {
        when (name) {
            "address" -> address = reader.text()
            "mac" -> mac = reader.text()
            "genres" -> genres = reader.stringMap()
            "epgChannelIds" -> epgIds = reader.list { text() }.filterNotNull().toSet()
            "subscriptionExpiry" -> expiry = reader.text()
            "enabled" -> enabled = reader.flag() ?: true
            "reachable" -> reachable = reader.flag()
            "autoDisabled" -> autoDisabled = reader.flag() ?: false
            "channels" -> channels = reader.list { channel(this) }.filterNotNull()
            "catalogs" -> catalogs = catalogs(reader)
            else -> reader.skipValue()
        }
    }

    private fun channel(reader: JsonReader): StalkerChannel? {
        var name: String? = null
        var command: String? = null
        var genreId: String? = null
        var genreName: String? = null
        var id: String? = null
        var logo: String? = null
        var direct = false
        var epgIdentity: String? = null
        var number: Long? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> name = reader.text()
                "command" -> command = reader.text()
                "genreId" -> genreId = shared(reader.text().nonEmpty())
                "genreName" -> genreName = shared(reader.text().nonEmpty())
                "id" -> id = reader.text().nonEmpty()
                "logo" -> logo = reader.text().nonEmpty()
                "direct" -> direct = reader.flag() ?: false
                "epgIdentity" -> epgIdentity = reader.text().nonEmpty()
                "number" -> number = reader.number()?.toLong()?.takeIf { it > 0 }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return StalkerChannel(
            name ?: return null, command ?: return null, genreId, genreName, id, logo, direct, epgIdentity, number
        )
    }

    private fun catalogs(reader: JsonReader): Map<PortalSection, PortalCatalog> = buildMap {
        reader.beginObject()
        while (reader.hasNext()) {
            val sectionName = reader.nextName()
            val section = PortalSection.entries.firstOrNull { it.name == sectionName }
            if (section == null) {
                reader.skipValue()
                continue
            }
            var titles: List<PortalTitle> = emptyList()
            var total: Int? = null
            var nextPage: Int? = null
            var groups: List<PortalGroup> = emptyList()
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "titles" -> titles = reader.list { title(this) }.filterNotNull()
                    "total" -> total = reader.number()?.toInt()?.takeIf { it >= 0 }
                    "nextPage" -> nextPage = reader.number()?.toInt()?.takeIf { it > 0 }
                    "groups" -> groups = reader.list { group(this) }.filterNotNull()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            put(section, PortalCatalog(titles, total, nextPage, groups))
        }
        reader.endObject()
    }

    private fun title(reader: JsonReader): PortalTitle? {
        // The earliest snapshots stored plain title names.
        if (reader.peek() == JsonToken.STRING) return PortalTitle(reader.nextString())
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        var name: String? = null
        var id: String? = null
        var command: String? = null
        var categoryId: String? = null
        var categoryName: String? = null
        var poster: String? = null
        var year: Int? = null
        var addedAt: Long? = null
        var description: String? = null
        var imdbId: String? = null
        var tmdbId: String? = null
        var popularity: Double? = null
        var service: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> name = reader.text()
                "id" -> id = reader.text().nonEmpty()
                "command" -> command = reader.text().nonEmpty()
                "categoryId" -> categoryId = shared(reader.text().nonEmpty())
                "categoryName" -> categoryName = shared(reader.text().nonEmpty())
                "poster" -> poster = reader.text().nonEmpty()
                "year" -> year = reader.number()?.toInt()?.takeIf { it > 0 }
                "addedAt" -> addedAt = reader.number()?.toLong()?.takeIf { it > 0 }
                "description" -> description = reader.text().nonEmpty()
                "imdbId" -> imdbId = reader.text().nonEmpty()
                "tmdbId" -> tmdbId = reader.text().nonEmpty()
                "popularity" -> popularity = reader.number()?.takeIf { it.isFinite() && it > 0 }
                "service" -> service = shared(reader.text().nonEmpty())
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return PortalTitle(
            name ?: return null, id, command, categoryId = categoryId, categoryName = categoryName, poster = poster,
            year = year, addedAt = addedAt, description = description, imdbId = imdbId, tmdbId = tmdbId,
            popularity = popularity, service = service
        )
    }

    private fun group(reader: JsonReader): PortalGroup? {
        var id: String? = null
        var name: String? = null
        var country: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = shared(reader.text())
                "name" -> name = shared(reader.text())
                "country" -> country = reader.text().nonEmpty()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return PortalGroup(id ?: return null, name ?: return null, country)
    }

    fun build(): SavedPortal = SavedPortal(
        requireNotNull(address), requireNotNull(mac), channels, genres, catalogs, epgIds,
        parsePortalExpiry(expiry), enabled, reachable, autoDisabled
    )
}

private fun String?.nonEmpty(): String? = this?.takeIf { it.isNotEmpty() }

/** A string, or the literal of a number or boolean, like org.json's optString; null for JSON null. */
private fun JsonReader.text(): String? = when (peek()) {
    JsonToken.NULL -> nextNull().let { null }
    JsonToken.STRING, JsonToken.NUMBER -> nextString()
    JsonToken.BOOLEAN -> nextBoolean().toString()
    else -> skipValue().let { null }
}

private fun JsonReader.number(): Double? = when (peek()) {
    JsonToken.NUMBER -> nextDouble()
    JsonToken.STRING -> nextString().toDoubleOrNull()
    else -> skipValue().let { null }
}

private fun JsonReader.flag(): Boolean? = when (peek()) {
    JsonToken.BOOLEAN -> nextBoolean()
    JsonToken.STRING -> nextString().toBooleanStrictOrNull()
    else -> skipValue().let { null }
}

private fun JsonReader.stringMap(): Map<String, String> {
    if (peek() != JsonToken.BEGIN_OBJECT) return skipValue().let { emptyMap() }
    val result = LinkedHashMap<String, String>()
    beginObject()
    while (hasNext()) {
        val key = nextName()
        text()?.let { result[key] = it }
    }
    endObject()
    return result
}

private fun <T> JsonReader.list(item: JsonReader.() -> T): List<T> {
    if (peek() != JsonToken.BEGIN_ARRAY) return skipValue().let { emptyList() }
    val result = ArrayList<T>()
    beginArray()
    while (hasNext()) result += item()
    endArray()
    return result
}
