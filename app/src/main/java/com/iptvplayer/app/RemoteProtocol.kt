package com.iptvplayer.app

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

/*
 * Messages of the remote settings session (inside the encrypted frames of RemoteSessionCrypto).
 *
 * Device → phone:
 *   {"t":"state","epoch":"…","device":"…","settings":[…],"portals":[…],"categories":{…},
 *    "countries":[…],"parental":{…},"busy":"…"}
 *   {"t":"result","ok":true|false,"message":"…"}
 * Phone → device (every command carries the epoch of the device's latest "state"):
 *   {"t":"hello"}                                     ask for the state
 *   {"t":"set","id":"…","value":…}                    change one setting
 *   {"t":"categories","section":"LIVE","ids":[…],"hidden":true}
 *   {"t":"categoryLock","section":"LIVE","id":"…","locked":true,"pin":"…"}
 *   {"t":"countries","codes":["DE","AT"]}
 *   {"t":"portalAdd","address":"…","mac":"…"}
 *   {"t":"portalEnabled","id":"…","enabled":true}
 *   {"t":"portalRemove","id":"…"}
 *   {"t":"pin","pin":"1234","current":"…"}            set or change the parental PIN
 *   {"t":"pinRemove","current":"…"}
 *   {"t":"kidsPin","pin":"…"|null,"current":"…"}      separate PIN for leaving kids mode
 *   {"t":"parental","field":"lockAdult","value":false,"pin":"…"}
 *   {"t":"portalPreferred","id":"…","preferred":true}
 *   {"t":"portalCategories","id":"…"}                   the provider's own categories of a portal
 *                                                       (answered in "result" as "data")
 *   {"t":"action","id":"…"}                             run an action entry (button)
 *   {"t":"input","id":"…","value":"…"}                  submit an input entry (text field)
 * A setting may carry "showIf":{"id":"…","value":"…"}: the phone shows it only while that
 * other setting has that value (for example intro times only with "Feste Zeit").
 * The format is plain JSON so that the web page and later iOS/Linux builds share it.
 */

internal data class RemoteOption(val value: String, val label: String, val color: String? = null)

/** One setting shown on the phone, with the code that reads and changes it on the device. */
internal sealed class RemoteEntry(val id: String, val section: String, val label: String) {
    /** Shown on the phone only while the setting [showIf].first has the value [showIf].second. */
    var showIf: Pair<String, String>? = null
        private set

    fun shownIf(id: String, value: Any): RemoteEntry = apply { showIf = id to value.toString() }

    class Toggle(id: String, section: String, label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) :
        RemoteEntry(id, section, label)

    class Choice(
        id: String, section: String, label: String, val options: List<RemoteOption>,
        val get: () -> String, val set: (String) -> Unit
    ) : RemoteEntry(id, section, label)

    class Number(
        id: String, section: String, label: String, val min: Int, val max: Int, val step: Int,
        val unit: String, val get: () -> Int, val set: (Int) -> Unit
    ) : RemoteEntry(id, section, label)

    /** A button; [run] returns an optional message for the phone. */
    class Action(id: String, section: String, label: String, val button: String, val run: () -> String?) :
        RemoteEntry(id, section, label)

    /** A text field with a button. The entered value is never sent back to the phone. */
    class Input(
        id: String, section: String, label: String, val hint: String, val secret: Boolean, val button: String,
        val submit: (String) -> String?
    ) : RemoteEntry(id, section, label)

    /** Read-only text, optionally with a link the phone can open. */
    class Info(id: String, section: String, label: String, val text: String, val link: String? = null) :
        RemoteEntry(id, section, label)
}

/** Applies a value from the phone; false when the id is unknown or the value does not fit. */
internal fun List<RemoteEntry>.applyRemoteValue(id: String, value: JsonElement?): Boolean {
    val entry = firstOrNull { it.id == id } ?: return false
    val primitive = value?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive ?: return false
    when (entry) {
        is RemoteEntry.Toggle -> {
            if (!primitive.isBoolean) return false
            entry.set(primitive.asBoolean)
        }
        is RemoteEntry.Choice -> {
            if (!primitive.isString) return false
            val choice = primitive.asString
            if (entry.options.none { it.value == choice }) return false
            entry.set(choice)
        }
        is RemoteEntry.Action, is RemoteEntry.Input, is RemoteEntry.Info -> return false
        is RemoteEntry.Number -> {
            if (!primitive.isNumber) return false
            val number = primitive.asDouble
            if (number % 1.0 != 0.0) return false
            val whole = number.toInt()
            if (whole < entry.min || whole > entry.max || (whole - entry.min) % entry.step != 0) return false
            entry.set(whole)
        }
    }
    return true
}

internal data class RemotePortal(
    val id: String, val label: String, val address: String, val mac: String, val enabled: Boolean,
    val reachable: Boolean?, val expiry: String?, val active: Boolean, val preferred: Boolean = false
)
internal data class RemoteCategory(val id: String, val name: String, val hidden: Boolean, val locked: Boolean)
internal data class RemoteCountry(val code: String, val name: String, val selected: Boolean)
internal data class RemoteParental(val hasPin: Boolean, val separateKidsPin: Boolean, val kidsMode: Boolean)

internal data class RemoteState(
    val device: String,
    val settings: List<RemoteEntry>,
    val portals: List<RemotePortal>,
    /** Keyed by PortalSection name (LIVE, MOVIES, SERIES). */
    val categories: Map<String, List<RemoteCategory>>,
    val countries: List<RemoteCountry>,
    val parental: RemoteParental,
    val busy: String?,
    /** The device asks which countries to show (after a portal was loaded). */
    val askCountries: Boolean = false,
    val error: String?,
    /** The app's accent colour ("#RRGGBB"), used by the page. */
    val accent: String? = null
)

internal sealed interface RemoteCommand {
    data object Hello : RemoteCommand
    data class Set(val id: String, val value: JsonElement?) : RemoteCommand
    data class Categories(val section: PortalSection, val ids: kotlin.collections.Set<String>, val hidden: Boolean) : RemoteCommand
    data class CategoryLock(val section: PortalSection, val id: String, val locked: Boolean, val pin: String?) : RemoteCommand
    data class Countries(val codes: List<String>, val confirm: Boolean = false) : RemoteCommand
    data object CountriesLater : RemoteCommand
    /** The phone is done: the session ends and the device returns to Live-TV. */
    data object Finish : RemoteCommand
    data class PortalEdit(val id: String, val address: String, val mac: String) : RemoteCommand
    data class PortalAdd(val address: String, val mac: String) : RemoteCommand
    data class PortalEnabled(val id: String, val enabled: Boolean) : RemoteCommand
    data class PortalRemove(val id: String) : RemoteCommand
    data class Pin(val pin: String, val current: String?) : RemoteCommand
    data class PinRemove(val current: String?) : RemoteCommand
    data class KidsPin(val pin: String?, val current: String?) : RemoteCommand
    data class Parental(val field: String, val value: JsonElement?, val pin: String?) : RemoteCommand
    data class PortalPreferred(val id: String, val preferred: Boolean) : RemoteCommand
    data class PortalCategories(val id: String) : RemoteCommand
    data class Action(val id: String) : RemoteCommand
    data class Input(val id: String, val value: String) : RemoteCommand
}

internal object RemoteProtocol {
    private const val MAX_TEXT = 2_048
    private const val MAX_IDS = 5_000
    private val PIN = Regex("^[0-9]{4,12}$")
    private val COUNTRY = Regex("^([A-Z]{2}|$UNKNOWN_COUNTRY)$")
    val PARENTAL_FIELDS = setOf("protectSettings", "lockAdult", "kidsMode", "kidsAge")

    fun encodeState(state: RemoteState, epoch: String): String = JsonObject().apply {
        addProperty("t", "state")
        addProperty("epoch", epoch)
        addProperty("device", state.device)
        state.busy?.let { addProperty("busy", it) }
        state.error?.let { addProperty("error", it) }
        addProperty("askCountries", state.askCountries)
        state.accent?.let { addProperty("accent", it) }
        add("settings", JsonArray().apply {
            state.settings.forEach { entry ->
                add(JsonObject().apply {
                    addProperty("id", entry.id)
                    addProperty("section", entry.section)
                    addProperty("label", entry.label)
                    entry.showIf?.let { (id, value) ->
                        add("showIf", JsonObject().apply {
                            addProperty("id", id)
                            addProperty("value", value)
                        })
                    }
                    when (entry) {
                        is RemoteEntry.Toggle -> {
                            addProperty("type", "toggle")
                            addProperty("value", entry.get())
                        }
                        is RemoteEntry.Choice -> {
                            addProperty("type", "choice")
                            addProperty("value", entry.get())
                            add("options", JsonArray().apply {
                                entry.options.forEach { option ->
                                    add(JsonObject().apply {
                                        addProperty("value", option.value)
                                        addProperty("label", option.label)
                                        option.color?.let { addProperty("color", it) }
                                    })
                                }
                            })
                        }
                        is RemoteEntry.Number -> {
                            addProperty("type", "number")
                            addProperty("value", entry.get())
                            addProperty("min", entry.min)
                            addProperty("max", entry.max)
                            addProperty("step", entry.step)
                            addProperty("unit", entry.unit)
                        }
                        is RemoteEntry.Action -> {
                            addProperty("type", "action")
                            addProperty("button", entry.button)
                        }
                        is RemoteEntry.Input -> {
                            addProperty("type", "input")
                            addProperty("hint", entry.hint)
                            addProperty("secret", entry.secret)
                            addProperty("button", entry.button)
                        }
                        is RemoteEntry.Info -> {
                            addProperty("type", "info")
                            addProperty("text", entry.text)
                            entry.link?.let { addProperty("link", it) }
                        }
                    }
                })
            }
        })
        add("portals", JsonArray().apply {
            state.portals.forEach { portal ->
                add(JsonObject().apply {
                    addProperty("id", portal.id)
                    addProperty("label", portal.label)
                    addProperty("address", portal.address)
                    addProperty("mac", portal.mac)
                    addProperty("enabled", portal.enabled)
                    portal.reachable?.let { addProperty("reachable", it) }
                    portal.expiry?.let { addProperty("expiry", it) }
                    addProperty("active", portal.active)
                    addProperty("preferred", portal.preferred)
                })
            }
        })
        add("categories", JsonObject().apply {
            state.categories.forEach { (section, categories) ->
                add(section, JsonArray().apply {
                    categories.forEach { category ->
                        add(JsonObject().apply {
                            addProperty("id", category.id)
                            addProperty("name", category.name)
                            addProperty("hidden", category.hidden)
                            addProperty("locked", category.locked)
                        })
                    }
                })
            }
        })
        add("countries", JsonArray().apply {
            state.countries.forEach { country ->
                add(JsonObject().apply {
                    addProperty("code", country.code)
                    addProperty("name", country.name)
                    addProperty("selected", country.selected)
                })
            }
        })
        add("parental", JsonObject().apply {
            addProperty("hasPin", state.parental.hasPin)
            addProperty("separateKidsPin", state.parental.separateKidsPin)
            addProperty("kidsMode", state.parental.kidsMode)
        })
    }.toString()

    fun encodeResult(ok: Boolean, message: String?, data: JsonObject? = null): String = JsonObject().apply {
        addProperty("t", "result")
        addProperty("ok", ok)
        message?.let { addProperty("message", it) }
        data?.let { add("data", it) }
    }.toString()

    /** Parses a command; null for anything malformed or sent for an older epoch. */
    fun decodeCommand(text: String, epoch: String): RemoteCommand? = runCatching {
        val root = JsonParser.parseString(text).asJsonObject
        if (root.text("epoch") != epoch) return null
        when (root.text("t")) {
            "hello" -> RemoteCommand.Hello
            "set" -> RemoteCommand.Set(root.text("id") ?: return null, root.get("value"))
            "categories" -> RemoteCommand.Categories(
                root.section() ?: return null,
                root.ids() ?: return null,
                root.flag("hidden") ?: return null
            )
            "categoryLock" -> RemoteCommand.CategoryLock(
                root.section() ?: return null, root.text("id") ?: return null,
                root.flag("locked") ?: return null, root.pin("pin")
            )
            "countries" -> RemoteCommand.Countries(
                root.ids()?.takeIf { codes -> codes.all(COUNTRY::matches) }?.toList() ?: return null,
                root.flag("confirm") == true
            )
            "portalAdd" -> {
                val address = root.text("address")?.trim() ?: return null
                val mac = root.text("mac")?.trim() ?: return null
                if (!address.startsWith("http://") && !address.startsWith("https://")) return null
                if (mac.isEmpty()) return null
                RemoteCommand.PortalAdd(address, mac)
            }
            "countriesLater" -> RemoteCommand.CountriesLater
            "finish" -> RemoteCommand.Finish
            "portalEdit" -> {
                val address = root.text("address")?.trim() ?: return null
                val mac = root.text("mac")?.trim() ?: return null
                if (!address.startsWith("http://") && !address.startsWith("https://") || mac.isEmpty()) return null
                RemoteCommand.PortalEdit(root.text("id") ?: return null, address, mac)
            }
            "portalEnabled" -> RemoteCommand.PortalEnabled(root.text("id") ?: return null, root.flag("enabled") ?: return null)
            "portalRemove" -> RemoteCommand.PortalRemove(root.text("id") ?: return null)
            "portalPreferred" -> RemoteCommand.PortalPreferred(root.text("id") ?: return null, root.flag("preferred") ?: return null)
            "portalCategories" -> RemoteCommand.PortalCategories(root.text("id") ?: return null)
            "action" -> RemoteCommand.Action(root.text("id") ?: return null)
            "input" -> RemoteCommand.Input(root.text("id") ?: return null, root.text("value") ?: return null)
            "pin" -> RemoteCommand.Pin(root.pin("pin") ?: return null, root.pin("current"))
            "pinRemove" -> RemoteCommand.PinRemove(root.pin("current"))
            "kidsPin" -> {
                val requested = root.get("pin")
                val pin = if (requested == null || requested.isJsonNull) null else root.pin("pin") ?: return null
                RemoteCommand.KidsPin(pin, root.pin("current"))
            }
            "parental" -> RemoteCommand.Parental(
                root.text("field")?.takeIf { it in PARENTAL_FIELDS } ?: return null,
                root.get("value"), root.pin("pin")
            )
            else -> null
        }
    }.getOrNull()

    private fun JsonObject.text(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
            ?.takeIf { it.length <= MAX_TEXT }

    private fun JsonObject.flag(name: String): Boolean? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean

    private fun JsonObject.pin(name: String): String? = text(name)?.takeIf(PIN::matches)

    private fun JsonObject.section(): PortalSection? =
        PortalSection.entries.firstOrNull { it.name == text("section") }

    private fun JsonObject.ids(): kotlin.collections.Set<String>? {
        val values = get("ids")?.takeIf { it.isJsonArray }?.asJsonArray ?: get("codes")?.takeIf { it.isJsonArray }?.asJsonArray
            ?: return null
        if (values.size() > MAX_IDS) return null
        return values.map { element ->
            (element as? JsonPrimitive)?.takeIf { it.isString }?.asString?.takeIf { it.length <= MAX_TEXT } ?: return null
        }.toSet()
    }
}
