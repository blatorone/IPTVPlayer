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

/** A light, switch or scene of Home Assistant. */
internal data class HaEntity(val id: String, val name: String) {
    val domain: String get() = id.substringBefore('.')
}

internal class HomeAssistantException(val status: Int) : Exception("HTTP $status")

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

    /** Lights, switches and scenes, sorted by name. */
    suspend fun entities(): List<HaEntity> {
        val states = JSONArray(call("/api/states"))
        return (0 until states.length()).mapNotNull { index ->
            val row = states.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("entity_id")
            if (id.substringBefore('.') !in CONTROLLED_DOMAINS) return@mapNotNull null
            val name = row.optJSONObject("attributes")?.optString("friendly_name")?.takeIf { it.isNotBlank() } ?: id
            HaEntity(id, name)
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

    /** Switches [entities] on (with [brightness] in percent for lights) or off. */
    suspend fun switch(entities: Collection<String>, on: Boolean, brightness: Int? = null) {
        for ((domain, ids) in entities.groupBy { it.substringBefore('.') }) {
            val body = JSONObject().put("entity_id", JSONArray(ids))
            val service = when {
                domain == "scene" -> if (on) "turn_on" else continue
                on -> "turn_on"
                else -> "turn_off"
            }
            if (domain == "light" && on && brightness != null) body.put("brightness_pct", brightness)
            if (domain == "light") body.put("transition", 1)
            call("/api/services/$domain/$service", body)
        }
    }

    /** Dims lights to [percent]; switches and scenes have no brightness and are switched off. */
    suspend fun dim(entities: Collection<String>, percent: Int) {
        val lights = entities.filter { it.startsWith("light.") }
        if (lights.isNotEmpty()) switch(lights, on = true, brightness = percent)
        val others = entities.filter { it.startsWith("switch.") }
        if (others.isNotEmpty()) switch(others, on = false)
    }

    companion object {
        val CONTROLLED_DOMAINS = setOf("light", "switch", "scene")
    }
}

/** When the rule applies. */
internal enum class LightWindow { ALWAYS, DARK, HOURS }

/** The light rule, stored in the "smart_home" preferences (the token is stored encrypted elsewhere). */
internal data class LightRule(
    val enabled: Boolean = false,
    val url: String = "",
    val entities: Set<String> = emptySet(),
    val movies: Boolean = true,
    val series: Boolean = true,
    val live: Boolean = false,
    /** 0: off; otherwise dim to this percent while playing. */
    val playLevel: Int = 0,
    /** Seconds after pausing until the light goes on; -1: leave it as it is. */
    val pauseDelay: Int = 3,
    val onStop: Boolean = true,
    /** Brightness when the light goes on again (percent). */
    val onLevel: Int = 100,
    val window: LightWindow = LightWindow.DARK,
    val fromMinutes: Int = 18 * 60,
    val toMinutes: Int = 6 * 60,
    val sunLead: Int = 30
) {
    fun applies(section: PortalSection?): Boolean = when (section) {
        PortalSection.MOVIES -> movies
        PortalSection.SERIES -> series
        PortalSection.LIVE -> live
        null -> false
    }

    fun save(preferences: SharedPreferences) = preferences.edit {
        putBoolean("enabled", enabled)
        putString("url", url)
        putStringSet("entities", entities)
        putBoolean("movies", movies)
        putBoolean("series", series)
        putBoolean("live", live)
        putInt("play_level", playLevel)
        putInt("pause_delay", pauseDelay)
        putBoolean("on_stop", onStop)
        putInt("on_level", onLevel)
        putString("window", window.name)
        putInt("from", fromMinutes)
        putInt("to", toMinutes)
        putInt("sun_lead", sunLead)
    }

    companion object {
        fun load(preferences: SharedPreferences) = LightRule(
            enabled = preferences.getBoolean("enabled", false),
            url = preferences.getString("url", "").orEmpty(),
            entities = preferences.getStringSet("entities", null)?.toSet().orEmpty(),
            movies = preferences.getBoolean("movies", true),
            series = preferences.getBoolean("series", true),
            live = preferences.getBoolean("live", false),
            playLevel = preferences.getInt("play_level", 0),
            pauseDelay = preferences.getInt("pause_delay", 3),
            onStop = preferences.getBoolean("on_stop", true),
            onLevel = preferences.getInt("on_level", 100),
            window = LightWindow.entries.firstOrNull { it.name == preferences.getString("window", null) } ?: LightWindow.DARK,
            fromMinutes = preferences.getInt("from", 18 * 60),
            toMinutes = preferences.getInt("to", 6 * 60),
            sunLead = preferences.getInt("sun_lead", 30)
        )
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
    /** Last switch: on (true), dimmed (null) or off (false), and when (epoch ms). */
    var last by mutableStateOf<Pair<Boolean?, Long>?>(null)
        private set
    private var storage: PortalStorage? = null
    /** StreamWatch dimmed or switched the light off and has not switched it on again. */
    @Volatile private var dimmed = false
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

    fun testInBackground() {
        scope.launch { runCatching { test() } }
    }

    fun disconnectInBackground() {
        scope.launch { runCatching { disconnect() } }
    }

    suspend fun disconnect() {
        storage?.writeHomeAssistantToken(null)
        token = null
        known = null
        update { it.copy(enabled = false, entities = emptySet()) }
    }

    /** Switches the chosen devices off and on again after 3 s, to see that it works. */
    suspend fun test() {
        val current = rule
        val client = client() ?: return
        client.switch(current.entities, on = false)
        delay(3_000)
        client.switch(current.entities, on = true, brightness = current.onLevel)
        last = true to System.currentTimeMillis()
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

    private fun ready(): Pair<LightRule, HomeAssistantClient>? {
        val current = rule
        if (!current.enabled || current.entities.isEmpty()) return null
        return client()?.let { current to it }
    }

    private suspend fun active(current: LightRule, client: HomeAssistantClient): Boolean = when (current.window) {
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

    /** The player started or stopped playing; [wanted]: it should play (false: paused by the user). */
    fun onPlayer(isPlaying: Boolean, wanted: Boolean, section: PortalSection?) {
        val (current, client) = ready() ?: return
        if (isPlaying) {
            playing = true
            if (!current.applies(section)) return
            run {
                if (!active(current, client)) return@run
                if (current.playLevel > 0) client.dim(current.entities, current.playLevel)
                else client.switch(current.entities, on = false)
                dimmed = true
                last = (if (current.playLevel > 0) null else false) to System.currentTimeMillis()
            }
        } else if (!wanted && playing) {
            // Paused (buffering keeps "wanted" and is ignored).
            playing = false
            if (!dimmed || current.pauseDelay < 0) return
            run {
                delay(current.pauseDelay * 1_000L)
                client.switch(current.entities, on = true, brightness = current.onLevel)
                dimmed = false
                last = true to System.currentTimeMillis()
            }
        }
    }

    /** Playback stopped or ended. Waits a moment: the next episode may start right away. */
    fun onStopped() {
        playing = false
        val (current, client) = ready() ?: return
        if (!dimmed || !current.onStop) return
        run {
            delay(STOP_DELAY_MS)
            client.switch(current.entities, on = true, brightness = current.onLevel)
            dimmed = false
            last = true to System.currentTimeMillis()
        }
    }

    private const val PREFERENCES = "smart_home"
    private const val STOP_DELAY_MS = 4_000L
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
