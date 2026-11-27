package com.iptvplayer.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/*
 * Smart home (Home Assistant): the light follows the playback. Play dims or switches the chosen
 * lights off, pause and stop switch them on again (only when StreamWatch switched them off), and
 * only while the rule is active (always, between two times, or while it is dark according to
 * Home Assistant's sun). Everything stays in the home network; address and token are never logged.
 */

/**
 * A light, switch or scene of Home Assistant and what it can do: dim, colour temperature (with
 * its range in Kelvin) and colour.
 */
internal data class HaEntity(
    val id: String,
    val name: String,
    val dimmable: Boolean = false,
    val temperature: Boolean = false,
    val color: Boolean = false,
    val minKelvin: Int = 2_000,
    val maxKelvin: Int = 6_500
) {
    val domain: String get() = id.substringBefore('.')
}

internal class HomeAssistantException(val status: Int) : Exception("HTTP $status")

/** What one light does in one state: off, or on with brightness and a colour temperature or colour. */
internal data class LightSetting(
    val on: Boolean,
    /** Percent (1-100); null: as it is. */
    val brightness: Int? = null,
    val kelvin: Int? = null,
    /** Colour as hue (0-359) and saturation (0-100); used instead of [kelvin] when set. */
    val hue: Int? = null,
    val saturation: Int? = null
) {
    fun toJson(): JSONObject = JSONObject().put("on", on).apply {
        brightness?.let { put("b", it) }
        kelvin?.let { put("k", it) }
        hue?.let { put("h", it) }
        saturation?.let { put("s", it) }
    }

    companion object {
        fun fromJson(json: JSONObject) = LightSetting(
            json.optBoolean("on"),
            json.optInt("b", -1).takeIf { it in 1..100 },
            json.optInt("k", -1).takeIf { it in 1_000..10_000 },
            json.optInt("h", -1).takeIf { it in 0..359 },
            json.optInt("s", -1).takeIf { it in 0..100 }
        )
    }
}

/** The moments the light follows; SMOKE is the smoking break started with its button in the player. */
internal enum class LightState { PLAY, PAUSE, STOP, SMOKE }

/** The Home Assistant REST API (long-lived access token). */
internal class HomeAssistantClient(baseUrl: String, private val token: String) {
    private val base = baseUrl.trim().trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

    private suspend fun call(path: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$base$path")
            .header("Authorization", "Bearer $token")
            .apply { if (body != null) post(body.toString().toRequestBody("application/json".toMediaType())) }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw HomeAssistantException(response.code)
            response.body.string()
        }
    }

    /** True when address and token work. */
    suspend fun check(): Boolean = JSONObject(call("/api/")).has("message")

    /** Lights, switches and scenes with what they can do, sorted by name. */
    suspend fun entities(): List<HaEntity> {
        val states = JSONArray(call("/api/states"))
        return (0 until states.length()).mapNotNull { index ->
            val row = states.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("entity_id")
            if (id.substringBefore('.') !in CONTROLLED_DOMAINS) return@mapNotNull null
            val attributes = row.optJSONObject("attributes")
            val name = attributes?.optString("friendly_name")?.takeIf { it.isNotBlank() } ?: id
            val modes = attributes?.optJSONArray("supported_color_modes")?.let { list ->
                (0 until list.length()).map { list.optString(it) }.toSet()
            }.orEmpty()
            HaEntity(
                id, name,
                dimmable = id.startsWith("light.") && modes.any { it != "onoff" },
                temperature = "color_temp" in modes,
                color = modes.any { it in COLOR_MODES },
                minKelvin = attributes?.optInt("min_color_temp_kelvin", 2_000)?.takeIf { it > 0 } ?: 2_000,
                maxKelvin = attributes?.optInt("max_color_temp_kelvin", 6_500)?.takeIf { it > 0 } ?: 6_500
            )
        }.sortedBy { it.name.lowercase() }
    }

    /** Whether it is dark: the sun is below the horizon, or sets within [leadMinutes]. Null: unknown. */
    suspend fun dark(leadMinutes: Int): Boolean? {
        val sun = JSONObject(call("/api/states/sun.sun"))
        if (sun.optString("state") == "below_horizon") return true
        val setting = sun.optJSONObject("attributes")?.optString("next_setting")
            ?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return false
        return Instant.now().plusSeconds(leadMinutes * 60L) >= setting
    }

    /** Sets every light of [scene] as configured (each light its own call: they differ). */
    suspend fun apply(scene: Map<String, LightSetting>) {
        for ((id, setting) in scene) {
            val domain = id.substringBefore('.')
            val body = JSONObject().put("entity_id", id)
            // Blinds: open or closed, or a position (kept in [LightSetting.brightness]) when restoring.
            if (domain == "cover") {
                val position = setting.brightness
                if (position != null) call("/api/services/cover/set_cover_position", body.put("position", position))
                else call("/api/services/cover/" + (if (setting.on) "open_cover" else "close_cover"), body)
                continue
            }
            val service = when {
                domain == "scene" -> if (setting.on) "turn_on" else continue
                setting.on -> "turn_on"
                else -> "turn_off"
            }
            if (domain == "light") {
                body.put("transition", 1)
                if (setting.on) {
                    setting.brightness?.let { body.put("brightness_pct", it) }
                    if (setting.hue != null) body.put("hs_color", JSONArray().put(setting.hue).put(setting.saturation ?: 100))
                    else setting.kelvin?.let { body.put("color_temp_kelvin", it) }
                }
            }
            call("/api/services/$domain/$service", body)
        }
    }

    /**
     * How [ids] are right now, as settings that bring them back exactly: on/off, brightness,
     * colour temperature or colour, blind position. Scenes cannot be undone and are left out.
     */
    suspend fun snapshot(ids: Collection<String>): Map<String, LightSetting> = buildMap {
        for (id in ids) {
            val domain = id.substringBefore('.')
            if (domain == "scene") continue
            val row = runCatching { JSONObject(call("/api/states/$id")) }.getOrNull() ?: continue
            val state = row.optString("state")
            val attributes = row.optJSONObject("attributes") ?: JSONObject()
            put(id, when (domain) {
                "cover" -> LightSetting(
                    on = state != "closed",
                    brightness = attributes.optInt("current_position", -1).takeIf { it in 0..100 }
                )
                "light" -> if (state != "on") LightSetting(false) else {
                    val brightness = attributes.optInt("brightness", -1).takeIf { it >= 0 }?.let { ((it * 100 + 127) / 255).coerceIn(1, 100) }
                    val hs = attributes.optJSONArray("hs_color")
                    val mode = attributes.optString("color_mode")
                    if (mode != "color_temp" && hs != null && hs.length() == 2) {
                        LightSetting(true, brightness, hue = hs.optDouble(0).toInt().coerceIn(0, 359), saturation = hs.optDouble(1).toInt().coerceIn(0, 100))
                    } else LightSetting(true, brightness, kelvin = attributes.optInt("color_temp_kelvin", -1).takeIf { it > 0 })
                }
                else -> LightSetting(state == "on")
            })
        }
    }

    companion object {
        val CONTROLLED_DOMAINS = setOf("light", "switch", "scene", "cover")
        private val COLOR_MODES = setOf("hs", "xy", "rgb", "rgbw", "rgbww")
    }
}

/** When the rule applies. */
internal enum class LightWindow { ALWAYS, DARK, HOURS }

/**
 * The light rule, stored in the "smart_home" preferences (the token is stored encrypted elsewhere).
 * Each state has its own scene: per light off, or on with brightness and colour; lights not in a
 * scene are left alone in that state.
 */
internal data class LightRule(
    val enabled: Boolean = false,
    val url: String = "",
    val scenes: Map<LightState, Map<String, LightSetting>> = emptyMap(),
    val movies: Boolean = true,
    val series: Boolean = true,
    val live: Boolean = false,
    /** Seconds after pausing until the pause scene is set. */
    val pauseDelay: Int = 3,
    /** Seconds after stop or end until the stop scene is set (the next episode cancels it). */
    val stopDelay: Int = 3,
    /** Weekdays on which the rule applies (1 = Monday ... 7 = Sunday). */
    val days: Set<Int> = (1..7).toSet(),
    val window: LightWindow = LightWindow.DARK,
    val fromMinutes: Int = 18 * 60,
    val toMinutes: Int = 6 * 60,
    val sunLead: Int = 30
) {
    fun scene(state: LightState): Map<String, LightSetting> = scenes[state].orEmpty()

    fun withSetting(state: LightState, id: String, setting: LightSetting?): LightRule {
        val scene = scene(state).let { if (setting == null) it - id else it + (id to setting) }
        return copy(scenes = scenes + (state to scene))
    }

    fun applies(section: PortalSection?): Boolean = when (section) {
        PortalSection.MOVIES -> movies
        PortalSection.SERIES -> series
        PortalSection.LIVE -> live
        null -> false
    }

    fun save(preferences: SharedPreferences) = preferences.edit {
        putBoolean("enabled", enabled)
        putString("url", url)
        putString("scenes", JSONObject().apply {
            scenes.forEach { (state, scene) ->
                put(state.name, JSONObject().apply { scene.forEach { (id, setting) -> put(id, setting.toJson()) } })
            }
        }.toString())
        putBoolean("movies", movies)
        putBoolean("series", series)
        putBoolean("live", live)
        putInt("pause_delay", pauseDelay)
        putInt("stop_delay", stopDelay)
        putStringSet("days", days.map { it.toString() }.toSet())
        putString("window", window.name)
        putInt("from", fromMinutes)
        putInt("to", toMinutes)
        putInt("sun_lead", sunLead)
    }

    companion object {
        fun load(preferences: SharedPreferences): LightRule {
            val stored = preferences.getString("scenes", null)?.let { raw ->
                runCatching {
                    val json = JSONObject(raw)
                    LightState.entries.associateWith { state ->
                        val scene = json.optJSONObject(state.name) ?: JSONObject()
                        scene.keys().asSequence().associateWith { LightSetting.fromJson(scene.getJSONObject(it)) }
                    }
                }.getOrNull()
            }
            return LightRule(
                enabled = preferences.getBoolean("enabled", false),
                url = preferences.getString("url", "").orEmpty(),
                scenes = stored ?: earlierScenes(preferences),
                movies = preferences.getBoolean("movies", true),
                series = preferences.getBoolean("series", true),
                live = preferences.getBoolean("live", false),
                pauseDelay = preferences.getInt("pause_delay", 3).coerceIn(0, 60),
                stopDelay = preferences.getInt("stop_delay", 3).coerceIn(0, 60),
                days = preferences.getStringSet("days", null)?.mapNotNull { it.toIntOrNull()?.takeIf { day -> day in 1..7 } }?.toSet() ?: (1..7).toSet(),
                window = LightWindow.entries.firstOrNull { it.name == preferences.getString("window", null) } ?: LightWindow.DARK,
                fromMinutes = preferences.getInt("from", 18 * 60),
                toMinutes = preferences.getInt("to", 6 * 60),
                sunLead = preferences.getInt("sun_lead", 30)
            )
        }

        /** The first version had one list of lights for all states: it becomes the three scenes. */
        private fun earlierScenes(preferences: SharedPreferences): Map<LightState, Map<String, LightSetting>> {
            val entities = preferences.getStringSet("entities", null).orEmpty()
            if (entities.isEmpty()) return emptyMap()
            val playLevel = preferences.getInt("play_level", 0)
            val onLevel = preferences.getInt("on_level", 100)
            val play = entities.associateWith {
                if (playLevel > 0 && it.startsWith("light.")) LightSetting(true, playLevel) else LightSetting(false)
            }
            val on = entities.associateWith { LightSetting(true, onLevel.takeIf { _ -> it.startsWith("light.") }) }
            return mapOf(
                LightState.PLAY to play,
                LightState.PAUSE to if (preferences.getInt("pause_delay", 3) < 0) emptyMap() else on,
                LightState.STOP to if (preferences.getBoolean("on_stop", true)) on else emptyMap()
            )
        }
    }
}

/** Whether [now] lies between [from] and [to] (minutes of the day; across midnight when from > to). */
internal fun inHours(now: Int, from: Int, to: Int): Boolean =
    if (from == to) true else if (from < to) now in from until to else now >= from || now < to

/** Follows the player and switches the light; one per app. */
// Holds only the application context (for the diagnostic report), which lives as long as the app.
@android.annotation.SuppressLint("StaticFieldLeak")
internal object LightAutomation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preferences: SharedPreferences? = null
    private var diagnostics: Context? = null
    var rule by mutableStateOf(LightRule())
        private set
    var token by mutableStateOf<String?>(null)
        private set
    /** Lights, switches and scenes of the connected Home Assistant (null: not loaded yet). */
    var known by mutableStateOf<List<HaEntity>?>(null)
        private set
    /** Last scene set, and when (epoch ms). */
    var last by mutableStateOf<Pair<LightState, Long>?>(null)
        private set
    private var storage: PortalStorage? = null
    /** Area of the running playback (for pause and stop). */
    @Volatile private var section0: PortalSection? = null
    private var pending: Job? = null
    private var playing = false

    fun init(context: Context, store: PortalStorage?, storedToken: String?) {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences = prefs
        diagnostics = context.applicationContext
        storage = store
        rule = LightRule.load(prefs)
        token = storedToken
        if (connected) scope.launch { runCatching { known = client()?.entities() } }
    }

    /** Checks [url] and [key]; on success both are kept (the key encrypted). Returns the number of devices. */
    suspend fun connect(url: String, key: String): Int {
        val clean = url.trim().trimEnd('/').let { if ("://" in it) it else "http://$it" }
        val test = HomeAssistantClient(clean, key.trim())
        check(test.check())
        val devices = test.entities()
        storage?.writeHomeAssistantToken(key.trim())
        token = key.trim()
        update { it.copy(url = clean) }
        known = devices
        return devices.size
    }

    /** For the phone page: connecting runs in the background, the page shows the outcome. */
    var connecting by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    fun connectInBackground(url: String, key: String) {
        connecting = true
        failed = false
        scope.launch {
            failed = runCatching { connect(url, key) }.isFailure
            connecting = false
        }
    }

    /** Searches the home network; the first Home Assistant found becomes the address. */
    fun searchInBackground(context: Context) {
        connecting = true
        scope.launch {
            val found = runCatching { discoverHomeAssistant(context) }.getOrDefault(emptyList())
            found.firstOrNull()?.let { address -> update { it.copy(url = address) } }
            failed = found.isEmpty()
            connecting = false
        }
    }

    fun testInBackground(state: LightState) {
        scope.launch { runCatching { test(state) } }
    }

    fun disconnectInBackground() {
        scope.launch { runCatching { disconnect() } }
    }

    suspend fun disconnect() {
        storage?.writeHomeAssistantToken(null)
        token = null
        known = null
        update { it.copy(enabled = false) }
    }

    /** Sets the scene of [state] now, to see how it looks. */
    suspend fun test(state: LightState) {
        val client = client() ?: return
        client.apply(rule.scene(state))
        last = state to System.currentTimeMillis()
    }

    fun update(change: (LightRule) -> LightRule) {
        rule = change(rule)
        preferences?.let(rule::save)
    }

    fun client(): HomeAssistantClient? {
        val current = rule
        val key = token ?: return null
        return current.url.takeIf { it.isNotBlank() }?.let { HomeAssistantClient(it, key) }
    }

    val connected: Boolean get() = client() != null

    private suspend fun active(current: LightRule, client: HomeAssistantClient): Boolean =
        java.time.LocalDate.now().dayOfWeek.value in current.days && when (current.window) {
        LightWindow.ALWAYS -> true
        LightWindow.HOURS -> LocalTime.now().let { inHours(it.hour * 60 + it.minute, current.fromMinutes, current.toMinutes) }
        LightWindow.DARK -> client.dark(current.sunLead) ?: true
    }

    private fun run(action: suspend () -> Unit) {
        pending?.cancel()
        pending = scope.launch {
            try {
                action()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                // Counts only, no address: whether the light could be switched.
                diagnostics?.let { AppDiagnostics.record(it, "light_failed ${(error as? HomeAssistantException)?.status ?: error.javaClass.simpleName}") }
            }
        }
    }

    /** Sets the scene of [state] (lights not in it stay as they are). */
    private suspend fun set(client: HomeAssistantClient, current: LightRule, state: LightState) {
        val scene = current.scene(state)
        if (scene.isEmpty()) return
        client.apply(scene)
        last = state to System.currentTimeMillis()
    }

    // ---- Smoking break: its own scene from the player button, undone with the next play ----

    /** How the devices of the smoking break were before it (set back exactly with the next play). */
    @Volatile private var smokeBefore: Map<String, LightSetting>? = null
    private var smokeJob: Job? = null
    /** The smoking break runs: its pause does not set the pause scene. */
    @Volatile private var smoking = false

    /** The button in the player is shown only when the smoking break has devices set up. */
    val smokeReady: Boolean get() = connected && rule.scene(LightState.SMOKE).isNotEmpty()

    /** Starts the smoking break (the caller pauses the player right after). Regardless of time and day. */
    fun startSmoke() {
        val client = client() ?: return
        val scene = rule.scene(LightState.SMOKE)
        if (scene.isEmpty() || smoking) return
        smoking = true
        pending?.cancel()
        smokeJob = scope.launch {
            try {
                smokeBefore = client.snapshot(scene.keys)
                client.apply(scene)
                last = LightState.SMOKE to System.currentTimeMillis()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                diagnostics?.let { AppDiagnostics.record(it, "light_failed ${(error as? HomeAssistantException)?.status ?: error.javaClass.simpleName}") }
            }
        }
    }

    /** Ends a running smoking break: everything it changed goes back to how it was. */
    private suspend fun endSmoke(client: HomeAssistantClient) {
        if (!smoking) return
        smoking = false
        smokeJob?.join()
        smokeJob = null
        smokeBefore?.let { client.apply(it) }
        smokeBefore = null
    }

    /** The player started or stopped playing; [wanted]: it should play (false: paused by the user). */
    fun onPlayer(isPlaying: Boolean, wanted: Boolean, section: PortalSection?) {
        val client = client() ?: return
        val current = rule
        if (isPlaying) {
            playing = true
            section0 = section
            run {
                endSmoke(client)
                if (current.enabled && current.applies(section) && active(current, client)) set(client, current, LightState.PLAY)
            }
        } else if (!wanted && playing) {
            // Paused (buffering keeps "wanted" and is ignored). Always sent, even when the light
            // was switched by hand in between; not during the smoking break.
            playing = false
            if (smoking || !current.enabled || !current.applies(section ?: section0)) return
            run {
                delay(current.pauseDelay * 1_000L)
                if (active(current, client)) set(client, current, LightState.PAUSE)
            }
        }
    }

    /** Playback stopped or ended. Waits as set: the next episode may start right away. */
    fun onStopped() {
        val wasPlaying = playing || section0 != null
        playing = false
        val client = client() ?: return
        val current = rule
        val section = section0
        section0 = null
        if (smoking) run { endSmoke(client) }
        if (!wasPlaying || !current.enabled || !current.applies(section)) return
        run {
            endSmoke(client)
            delay(current.stopDelay * 1_000L)
            if (active(current, client)) set(client, current, LightState.STOP)
        }
    }

    private const val PREFERENCES = "smart_home"
}

/**
 * Looks for Home Assistant in the home network (it announces itself as _home-assistant._tcp).
 * Returns base addresses like http://192.168.1.10:8123; empty when nothing answered in time.
 */
internal suspend fun discoverHomeAssistant(context: Context, timeoutMs: Long = 4_000): List<String> {
    val manager = context.getSystemService(Context.NSD_SERVICE) as? android.net.nsd.NsdManager ?: return emptyList()
    val found = java.util.Collections.synchronizedSet(linkedSetOf<String>())
    val listener = object : android.net.nsd.NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onDiscoveryStopped(serviceType: String) = Unit
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        override fun onServiceLost(serviceInfo: android.net.nsd.NsdServiceInfo) = Unit
        override fun onServiceFound(serviceInfo: android.net.nsd.NsdServiceInfo) {
            @Suppress("DEPRECATION")
            manager.resolveService(serviceInfo, object : android.net.nsd.NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: android.net.nsd.NsdServiceInfo, errorCode: Int) = Unit
                override fun onServiceResolved(serviceInfo: android.net.nsd.NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    val host = serviceInfo.host?.hostAddress?.takeIf { '.' in it } ?: return
                    found += "http://$host:${serviceInfo.port}"
                }
            })
        }
    }
    return try {
        manager.discoverServices("_home-assistant._tcp", android.net.nsd.NsdManager.PROTOCOL_DNS_SD, listener)
        delay(timeoutMs)
        found.toList()
    } catch (_: Exception) {
        found.toList()
    } finally {
        runCatching { manager.stopServiceDiscovery(listener) }
    }
}
