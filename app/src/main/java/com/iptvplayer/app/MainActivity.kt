package com.iptvplayer.app

import androidx.core.content.edit
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState


import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.util.TypedValue
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.key
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.res.pluralStringResource
import java.time.format.DateTimeFormatter
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import android.view.SurfaceView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class MainActivity : ComponentActivity() {
    // The interface language (chosen in the settings, otherwise the device language or English).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localizedContext(newBase))
    }

    private lateinit var player: ExoPlayer
    private val storage by lazy { PortalStorage(applicationContext) }
    private var longBackAction: (() -> Boolean)? = null
    private var screenBackAction: ((Boolean) -> Boolean)? = null
    private var playbackInputAction: (() -> Unit)? = null
    private var promptInputAction: ((android.view.KeyEvent) -> Boolean)? = null
    private var consumedLongBack = false
    @Volatile private var keySoundsEnabled = false

    /** Plays the device navigation sounds; they follow the system sound setting of the device. */
    private fun playKeySound(event: android.view.KeyEvent) {
        if (!keySoundsEnabled || event.action != android.view.KeyEvent.ACTION_DOWN || event.repeatCount != 0) return
        val effect = when (event.keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP -> android.view.SoundEffectConstants.NAVIGATION_UP
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> android.view.SoundEffectConstants.NAVIGATION_DOWN
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> android.view.SoundEffectConstants.NAVIGATION_LEFT
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> android.view.SoundEffectConstants.NAVIGATION_RIGHT
            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER,
            android.view.KeyEvent.KEYCODE_NUMPAD_ENTER, android.view.KeyEvent.KEYCODE_BUTTON_A ->
                android.view.SoundEffectConstants.CLICK
            else -> return
        }
        window?.decorView?.playSoundEffect(effect)
    }

    // RestrictedApi: lint confuses this public Activity method with androidx.core's restricted
    // helper of the same name. GestureBackNavigation: the long press of Back (a remote key on TV)
    // only exists as key events; the back callbacks report no long press.
    @android.annotation.SuppressLint("RestrictedApi", "GestureBackNavigation")
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        playKeySound(event)
        // Modal playback prompts own the remote while visible, before PlayerView or
        // Compose can route directional keys to the guide/list behind the prompt.
        if (promptInputAction?.invoke(event) == true) return true
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) playbackInputAction?.invoke()
        if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount > 0) {
                if (consumedLongBack || longBackAction?.invoke() == true) {
                    consumedLongBack = true
                    return true
                }
            }
            if (event.action == android.view.KeyEvent.ACTION_UP && consumedLongBack) {
                consumedLongBack = false
                return true
            }
            if (screenBackAction?.invoke(
                    event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled
                ) == true
            ) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) playbackInputAction?.invoke()
        return super.dispatchTouchEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.setLogLevel(Log.LOG_LEVEL_OFF)
        // Optional: keep listening for the device waking from standby (also without a restart).
        AutoStart.sync(this)
        // The audio sink is wrapped so that series openings can also be recognised by their theme music.
        // Passthrough audio (Dolby to the TV or receiver) is never decoded by the app; users may
        // choose to decode it so that the theme music can be analysed as well.
        val decodeAudioForIntro = getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
            .getBoolean("decode_audio_for_intro", false)
        val renderers = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? =
                // The device-aware sink always stays in place; with the option the wrapper only
                // declines passthrough, so encoded audio is decoded and played as regular PCM.
                super.buildAudioSink(context, enableFloatOutput, enableAudioOutputPlaybackParams)
                    ?.let { EpisodeAudioTapSink(it, decodeEncoded = decodeAudioForIntro) }
        }
        SmoothSwitch.enabled = smoothSwitchEnabled(this)
        SmoothSwitch.init(this)
        // Timeshift recordings are only temporary: whatever an earlier run left behind is deleted.
        Thread {
            java.io.File(cacheDir, "timeshift").listFiles()?.forEach { it.delete() }
            // Stand-in recordings of the smooth switch left from the last run.
            cacheDir.listFiles { file -> file.isDirectory && file.name.startsWith("standby-") }?.forEach { it.deleteRecursively() }
        }.start()
        // Optional buffer (Wiedergabe → Puffer); off keeps the player's usual values.
        player = ExoPlayer.Builder(this, renderers).setLoadControl(bufferLoadControl(chosenBuffer(this))).build().also { activePlayer ->
            activePlayer.trackSelectionParameters = activePlayer.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(java.util.Locale.getDefault().language)
                .build()
        }
        enableEdgeToEdge()
        setContent {
            // Each account has its own accent colour.
            var kidsAccount by remember { mutableStateOf(false) }
            var accentVersion by remember { mutableIntStateOf(0) }
            val accentChoice = remember(kidsAccount, accentVersion) {
                AccentChoice.entries.firstOrNull {
                    it.name == getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
                        .getString(UserProfiles.accentKey(kidsAccount), null)
                } ?: AccentChoice.RED
            }
            // New installs accept the terms first; nothing else is shown before that.
            var termsAccepted by remember { mutableStateOf(disclaimerAccepted(this@MainActivity)) }
            val accentGradient = remember(kidsAccount, accentVersion) {
                AccentGradient.parse(getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
                    .getString(AccentGradient.key(kidsAccount), null))
            }
            MaterialTheme(colorScheme = accentGradient?.let(::playerColors) ?: playerColors(accentChoice)) {
            androidx.compose.runtime.CompositionLocalProvider(LocalAccentGradient provides accentGradient) {
                // New installs: language first, then the terms in that language.
                if (!termsAccepted && !firstLanguageChosen(this@MainActivity)) FirstLanguageScreen(
                    onChosen = { tag, mirror ->
                        saveLanguage(this@MainActivity, tag)
                        saveMirrorLayout(this@MainActivity, mirror)
                        markFirstLanguageChosen(this@MainActivity)
                        recreate()
                    },
                    onExit = ::finish
                ) else if (!termsAccepted) DisclaimerScreen(
                    onAccept = { acceptDisclaimer(this@MainActivity); termsAccepted = true },
                    onExit = ::finish
                ) else PlayerHome(
                    player = player,
                    storage = storage,
                    accentChoice = accentChoice,
                    accentGradient = accentGradient,
                    onAccentChoiceChange = { choice ->
                        // A plain colour replaces a gradient of this account.
                        getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit {
                            putString(UserProfiles.accentKey(kidsAccount), choice.name)
                            remove(AccentGradient.key(kidsAccount))
                        }
                        accentVersion++
                    },
                    onAccentGradientChange = { gradient ->
                        getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit {
                            putString(AccentGradient.key(kidsAccount), gradient.name)
                        }
                        accentVersion++
                    },
                    onAccountChange = { kids -> kidsAccount = kids; accentVersion++ },
                    registerLongBack = { longBackAction = it },
                    registerScreenBack = { screenBackAction = it },
                    registerPlaybackInput = { playbackInputAction = it },
                    registerPromptInput = { promptInputAction = it },
                    registerKeySounds = { keySoundsEnabled = it },
                    onExit = ::finish,
                    onFullscreen = { fullscreen ->
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        val bars = WindowInsetsCompat.Type.systemBars()
                        if (fullscreen) controller.hide(bars) else controller.show(bars)
                    },
                    canPlay = { lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
                )
            }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStop() {
        player.pause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onStop()
    }

    override fun onDestroy() {
        // Closing the app ends the timeshift recording and deletes it.
        if (isFinishing) {
            Timeshift.stop()
            SmoothSwitch.stopStandby()
        }
        player.release()
        super.onDestroy()
    }
}

/** Where "Zurück" leads after opening a title from an actor's filmography. */
/** A pending PIN prompt; [newPin] asks twice to set a PIN. */
internal class PinRequest(
    val title: String,
    val message: String?,
    val newPin: Boolean = false,
    val check: (String) -> Boolean,
    val length: Int? = null,
    /** Offers "Per Handy einstellen" (locked settings: everything is set up on the phone). */
    val remoteSetup: Boolean = false,
    val onSuccess: (String) -> Unit
)

/** "Same PIN as … or an own one?" when setting the Kindersicherung or settings PIN. */
private class SharedPinQuestion(val text: String, val onShared: () -> Unit, val onOwn: () -> Unit)

private class CastReturn(
    val destination: Destination?,
    val details: Pair<PortalSection, PortalTitle>?,
    val castOf: Triple<PortalSection, PortalTitle, Int?>?,
    val person: CastMember,
    val opened: Pair<PortalSection, PortalTitle>
)

private enum class Destination(val label: Int, val icon: Int? = null) {
    LIVE(R.string.destination_live, R.drawable.ic_menu_live),
    MOVIES(R.string.destination_movies, R.drawable.ic_menu_movies),
    SERIES(R.string.destination_series, R.drawable.ic_menu_series),
    SETTINGS(R.string.destination_settings, R.drawable.ic_menu_settings),
    SEARCH(R.string.search, R.drawable.ic_menu_search),
    /** "Abmelden": opens the account selection (only with a kids account). */
    PROFILES(R.string.logout, R.drawable.ic_menu_logout),
    CATEGORIES(R.string.category_settings),
    SOURCES(R.string.destination_sources)
}

private enum class MainMenuStyle(val label: Int) {
    BOTH(R.string.menu_style_both), TEXT(R.string.menu_style_text), ICONS(R.string.menu_style_icons)
}

private enum class ProviderCategoryStyle(val label: Int) {
    SYMBOLS(R.string.provider_style_symbols), BOTH(R.string.provider_style_both), LABELS(R.string.provider_style_labels)
}

private fun providerCategoryMark(name: String): String? = when (serviceCategory(name)) {
    VodCategory.NETFLIX -> "N"
    VodCategory.PRIME -> "prime video"
    VodCategory.APPLE -> "tv+"
    VodCategory.DISNEY -> "Disney+"
    VodCategory.MAX -> "MAX"
    VodCategory.PARAMOUNT -> "P+"
    VodCategory.SKY -> "sky"
    VodCategory.PEACOCK -> "peacock"
    else -> null
}

private fun providerCategoryIcon(name: String): Int? = when (serviceCategory(name)) {
    VodCategory.NETFLIX -> R.drawable.provider_netflix
    VodCategory.PRIME -> R.drawable.provider_prime_video
    VodCategory.APPLE -> R.drawable.provider_appletv
    VodCategory.DISNEY -> R.drawable.provider_disney_plus
    VodCategory.MAX -> R.drawable.provider_max
    VodCategory.PARAMOUNT -> R.drawable.provider_paramountplus
    VodCategory.SKY -> R.drawable.provider_sky
    VodCategory.PEACOCK -> R.drawable.provider_peacock
    else -> null
}

/** Symbol for a film or series genre; provider categories keep their own symbol. */
private fun genreCategoryIcon(group: PortalGroup): Int? =
    when (VodCategory.entries.firstOrNull { it.id == group.id } ?: vodCategory(group)) {
        VodCategory.ALL -> R.drawable.genre_all
        VodCategory.RECENT -> R.drawable.genre_new
        VodCategory.THIS_YEAR -> R.drawable.genre_year
        VodCategory.POPULAR -> R.drawable.genre_popular
        VodCategory.CINEMA -> R.drawable.genre_cinema
        VodCategory.CINEMA_THIS_YEAR, VodCategory.CINEMA_LAST_YEAR -> R.drawable.genre_ticket
        VodCategory.FAMILY -> R.drawable.genre_family
        VodCategory.ANIMATION -> R.drawable.genre_animation
        VodCategory.ACTION -> R.drawable.genre_action
        VodCategory.COMEDY -> R.drawable.genre_comedy
        VodCategory.HORROR -> R.drawable.genre_horror
        VodCategory.CRIME -> R.drawable.genre_crime
        VodCategory.FANTASY -> R.drawable.genre_fantasy
        VodCategory.DOCUMENTARY -> R.drawable.genre_documentary
        VodCategory.ROMANCE -> R.drawable.genre_romance
        VodCategory.DRAMA -> R.drawable.genre_drama
        VodCategory.OTHER -> R.drawable.genre_other
        else -> null
    }

private enum class VodLiveView(val label: Int) {
    BACKGROUND(R.string.vod_live_background),
    CORNER(R.string.vod_live_corner),
    HIDDEN(R.string.vod_live_hidden)
}

/** Accent colours, saved by name (new colours may be added in any order). */
internal enum class AccentChoice(val label: Int, val color: Color) {
    RED(R.string.accent_red, Color(0xFFE50914)),
    WINE(R.string.accent_wine, Color(0xFFB0123A)),
    CORAL(R.string.accent_coral, Color(0xFFFF5A4E)),
    PINK(R.string.accent_pink, Color(0xFFFF4D8D)),
    MAGENTA(R.string.accent_magenta, Color(0xFFD63AF9)),
    PURPLE(R.string.accent_purple, Color(0xFF9B59D0)),
    VIOLET(R.string.accent_violet, Color(0xFF7C4DFF)),
    INDIGO(R.string.accent_indigo, Color(0xFF5C6BC0)),
    BLUE(R.string.accent_blue, Color(0xFF1689D4)),
    SKY(R.string.accent_sky, Color(0xFF29B6F6)),
    CYAN(R.string.accent_cyan, Color(0xFF00BCD4)),
    TEAL(R.string.accent_teal, Color(0xFF14A89A)),
    GREEN(R.string.accent_green, Color(0xFF2EAD68)),
    LIME(R.string.accent_lime, Color(0xFF8BC34A)),
    YELLOW(R.string.accent_yellow, Color(0xFFF5C518)),
    AMBER(R.string.accent_amber, Color(0xFFFFB300)),
    ORANGE(R.string.accent_orange, Color(0xFFFF8A24)),
    BRONZE(R.string.accent_bronze, Color(0xFFC8783C)),
    BROWN(R.string.accent_brown, Color(0xFF8D6E63)),
    GREY(R.string.accent_grey, Color(0xFF90A4AE)),
    // Added in 1.0.93, below the first 20 so the palette keeps its width.
    CRIMSON(R.string.accent_crimson, Color(0xFFC62828)),
    RASPBERRY(R.string.accent_raspberry, Color(0xFFD81B60)),
    SALMON(R.string.accent_salmon, Color(0xFFFF8A80)),
    ROSE(R.string.accent_rose, Color(0xFFF48FB1)),
    ORCHID(R.string.accent_orchid, Color(0xFFBA68C8)),
    LAVENDER(R.string.accent_lavender, Color(0xFFB39DDB)),
    NAVY(R.string.accent_navy, Color(0xFF3949AB)),
    ROYAL(R.string.accent_royal, Color(0xFF2962FF)),
    ICE(R.string.accent_ice, Color(0xFF81D4FA)),
    TURQUOISE(R.string.accent_turquoise, Color(0xFF1DE9B6)),
    PETROL(R.string.accent_petrol, Color(0xFF00796B)),
    MINT(R.string.accent_mint, Color(0xFF69F0AE)),
    EMERALD(R.string.accent_emerald, Color(0xFF00C853)),
    OLIVE(R.string.accent_olive, Color(0xFF9E9D24)),
    CHARTREUSE(R.string.accent_chartreuse, Color(0xFFC6FF00)),
    LEMON(R.string.accent_lemon, Color(0xFFFFEB3B)),
    GOLD(R.string.accent_gold, Color(0xFFD4A017)),
    APRICOT(R.string.accent_apricot, Color(0xFFFFAB76)),
    RUST(R.string.accent_rust, Color(0xFFD84315)),
    SAND(R.string.accent_sand, Color(0xFFBCAAA4));

    /** Darker shade for filled areas such as the selected row. */
    val container: Color get() = androidx.compose.ui.graphics.lerp(color, Color.Black, 0.35f)
    /** Text colour on the accent: black on light colours, white otherwise. */
    val onColor: Color get() = if (color.luminance() > 0.45f) Color(0xFF111111) else Color.White
    /** "#RRGGBB" for the phone page. */
    val hex: String get() = "#%06X".format(color.toArgb() and 0xFFFFFF)
}

/** Pages the app can open with. */
private val START_DESTINATIONS = listOf(Destination.LIVE, Destination.MOVIES, Destination.SERIES)

private fun savedStartDestination(preferences: android.content.SharedPreferences?): Destination {
    val saved = preferences?.getString("start_destination", null)
    return START_DESTINATIONS.firstOrNull { it.name == saved } ?: Destination.LIVE
}

private val mainDestinations = Destination.entries.filter {
    it != Destination.SOURCES && it != Destination.CATEGORIES
}
private const val EXIT_CONFIRMATION_MS = 3_000L

/** How leaving the app with Back is confirmed. */
internal enum class ExitConfirmation(val label: Int) {
    DIALOG(R.string.exit_confirm_dialog),
    DOUBLE_BACK(R.string.exit_confirm_double_back),
    NONE(R.string.exit_confirm_none)
}
private const val SETTINGS_PAGE_FOCUS_GRACE_MS = 800L
private const val REMOTE_PIN_ATTEMPTS = 5
/** Live-TV watchdog: long buffering or jumps only count within this window. */
private const val STALL_WINDOW_MS = 3 * 60_000L
private const val LONG_JUMP_BACK_MS = 10_000L
/** The outer play buttons jump this far in films and timeshift. */
private const val BIG_JUMP_MS = 10 * 60_000L
/** Picture (or picture and sound) standing still this long while "playing" counts as a hang. */
private const val FROZEN_LIMIT_MS = 8_000L
/** A stream that played this long without trouble is remembered as a good one for the channel. */
private const val STEADY_PLAY_MS = 90_000L

/**
 * Position in the stream as a whole. For live HLS the plain position is measured from the start
 * of the moving live window and would jump back with every playlist update.
 */
internal fun liveStreamPosition(player: Player): Long {
    val timeline = player.currentTimeline
    if (timeline.isEmpty) return player.currentPosition
    val window = timeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
    return window.positionInFirstPeriodMs + player.currentPosition
}

internal fun playerColors(accent: AccentChoice) = playerColors(accent.color, accent.onColor, accent.container)

internal fun playerColors(color: Color, onColor: Color, container: Color) = darkColorScheme(
    primary = color,
    onPrimary = onColor,
    primaryContainer = container,
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFF5C451),
    onSecondary = Color(0xFF241900),
    secondaryContainer = Color(0xFF302818),
    onSecondaryContainer = Color(0xFFFFEFC4),
    tertiary = color,
    surface = Color(0xFF080808),
    surfaceContainerLowest = Color(0xFF050505),
    surfaceContainerLow = Color(0xFF0D0D0D),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF292929),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF646464),
    outlineVariant = Color(0xFF383838),
    tertiaryContainer = container,
    onTertiaryContainer = Color(0xFFFFFFFF)
)

private data class PlaybackSelection(
    val source: String,
    val section: PortalSection,
    val id: String,
    val episode: String? = null,
    val seriesId: String? = null
) {
    val progressKey: String get() = "$section:$source:${seriesId.orEmpty()}:$id"
}

private data class PlaybackVariant(
    val label: String,
    val timing: EpisodeTiming = EpisodeTiming(),
    /** Identity of the portal this variant plays from (null when unknown). */
    val sourceId: String? = null,
    /** This one stream of the portal (FHD and HD of the same portal are separate), for its history. */
    val streamKey: String? = null,
    val resolve: suspend () -> String
)

/** Key of one Live-TV stream in the stream history: portal plus the stream's id in that portal. */
internal fun liveStreamKey(sourceId: String, streamId: String?) =
    streamId?.takeIf(String::isNotBlank)?.let { "$sourceId#$it" } ?: sourceId

/**
 * Languages spoken in the chosen countries (plus the device language), as ISO 639-1 codes
 * like Media3 reports them. Empty when no country is chosen, which allows every language.
 */
internal fun allowedAudioLanguages(countries: Collection<String>): Set<String> {
    val chosen = countries.filter { it.length == 2 }.map { it.uppercase() }.toSet()
    if (chosen.isEmpty()) return emptySet()
    val spoken = java.util.Locale.getAvailableLocales().filter { it.country in chosen }.map { it.language.lowercase() }
    return (spoken + java.util.Locale.getDefault().language.lowercase()).filter { it.isNotBlank() }.toSet()
}

/** Orders items so that the chosen portal comes first, then the preferred one, then the rest unchanged. */
internal fun <T> List<T>.preferringSource(chosen: String?, preferred: String?, sourceOf: (T) -> String?): List<T> =
    sortedBy { item ->
        val source = sourceOf(item)
        when {
            source == null -> 2
            source == chosen -> 0
            source == preferred -> 1
            else -> 2
        }
    }
/** Forced subtitles: flagged as forced in the stream, or only named so by the provider. */
private fun isForcedSubtitle(format: androidx.media3.common.Format): Boolean =
    isForcedSubtitle(format.selectionFlags, format.label, format.id)

internal fun isForcedSubtitle(selectionFlags: Int, label: String?, id: String?): Boolean =
    selectionFlags and C.SELECTION_FLAG_FORCED != 0 ||
        listOfNotNull(label, id).any { text ->
            text.contains("forced", ignoreCase = true) || text.contains("erzwungen", ignoreCase = true)
        }

private data class PreparedLiveVariant(val variant: PlaybackVariant, val url: String, val preparedAt: Long)
private data class PlaybackAttempt(
    val selection: PlaybackSelection,
    val channel: StalkerChannel,
    val variants: List<PlaybackVariant>,
    val index: Int,
    val positionMs: Long,
    val progressKey: String?,
    val mediaId: String = "",
    val openFullscreenOnStart: Boolean = true,
    val liveRestoreKey: String? = null,
    /** Live-TV goes through its streams a second time before giving up (round 1). */
    val round: Int = 0
)

/** The stream tried after this one: the next in the list, for Live-TV once more from the start. */
private fun PlaybackAttempt.nextIndex(): Int? = when {
    index + 1 < variants.size -> index + 1
    selection.section == PortalSection.LIVE && round == 0 -> 0
    else -> null
}


/** One poster in a row of "Mein Bereich". */
private class MyAreaCard(
    val key: String,
    val title: PortalTitle,
    val refs: List<SeriesPortalRef>,
    val caption: String?,
    val unseen: Int,
    val open: () -> Unit
)
private data class PortalContentCounts(val tv: Int, val movies: Int, val series: Int)
private enum class SettingsSection(val label: Int, val icon: Int) {
    GENERAL(R.string.settings_general, R.drawable.settings_general),
    SOURCES(R.string.settings_library, R.drawable.settings_library),
    GUIDE(R.string.destination_guide, R.drawable.settings_guide),
    APPEARANCE(R.string.settings_appearance, R.drawable.settings_appearance),
    PLAYBACK(R.string.settings_playback, R.drawable.settings_playback),
    CONTROLS(R.string.settings_controls, R.drawable.settings_controls),
    SYNC(R.string.settings_sync, R.drawable.settings_sync),
    SMART_HOME(R.string.settings_smart_home, R.drawable.settings_smart_home),
    PARENTAL(R.string.settings_locks, R.drawable.settings_parental),
    UPDATES(R.string.settings_updates, R.drawable.settings_updates),
    DIAGNOSTICS(R.string.settings_diagnostics, R.drawable.settings_diagnostics),
    ABOUT(R.string.settings_about, R.drawable.settings_about)
}

private fun liveSelection(channel: StalkerChannel, group: LiveChannelGroup, combine: Boolean): PlaybackSelection {
    val source = group.variants.first().item.source.identity
    val key = group.identity ?: channel.id ?: channel.command
    return PlaybackSelection(if (combine && group.identity != null) "catalogue" else source, PortalSection.LIVE, key)
}

private fun playbackTime(positionMs: Long): String {
    val seconds = positionMs / 1_000
    val minutes = seconds / 60
    val remainder = (seconds % 60).toString().padStart(2, '0')
    return if (minutes < 60) "$minutes:$remainder"
    else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$remainder"
}

@Composable
private fun EpisodeTimingAdjustment(
    label: String, seconds: Int, minimum: Int, maximum: Int, onChange: (Int) -> Unit
) {
    Text(label)
    Row {
        MenuButton("−5 s", Modifier) { onChange((seconds - 5).coerceAtLeast(minimum)) }
        MenuButton("+5 s", Modifier) { onChange((seconds + 5).coerceAtMost(maximum)) }
    }
}

/**
 * Simple on/off states of PlayerHome in one object: the large content lambdas capture this one
 * reference instead of a MutableState each (ART on Android 11 rejects lambdas with >255 captures).
 */
private class PlayerHomeFlags {
    /** Live-TV without the automatic switch: the channel waits for the stream to be chosen. */
    var streamChoice by mutableStateOf<PlaybackAttempt?>(null)
    var streamChoiceFailed by mutableStateOf(false)
    // Moved out of PlayerHome: fewer values captured by its lambdas (ART limit 255).
    var sharedPinQuestion by mutableStateOf<SharedPinQuestion?>(null)
    var pinRequest by mutableStateOf<PinRequest?>(null)
    var portalCategoriesFor by mutableStateOf<SavedPortal?>(null)
    var traktDeviceCode by mutableStateOf<TraktDeviceCode?>(null)
    var traktStatus by mutableStateOf<String?>(null)
    var traktPlaybackNotice by mutableStateOf<String?>(null)
    var portalRefreshNotice by mutableStateOf<String?>(null)
    var castOf by mutableStateOf<Triple<PortalSection, PortalTitle, Int?>?>(null)
    var castPerson by mutableStateOf<CastMember?>(null)
    var castPreview by mutableStateOf<Pair<PortalTitle, List<CastMember>>?>(null)
    var guideAppearancePage by mutableStateOf<GuideAppearancePage?>(null)
    var playbackVideoSurface by mutableStateOf<SurfaceView?>(null)
    var nextEpisodeName by mutableStateOf("")
    var nextEpisodeSeconds by mutableStateOf<Int?>(null)
    var interruptedEpisode by mutableStateOf(false)
    var advancingEpisode by mutableStateOf(false)
    var watchingPrompt by mutableStateOf(false)
    var skipIntroTo by mutableStateOf<Long?>(null)
    var introOfferClosed by mutableStateOf<Pair<String, Long>?>(null)
    var outroOfferClosed by mutableStateOf<Pair<String, Long>?>(null)
    var skipIsOutro by mutableStateOf(false)
    var pendingSeriesEpisode by mutableStateOf<Pair<PortalTitle, EpisodeResume>?>(null)
    var pendingMovie by mutableStateOf<WatchedMovie?>(null)
    var currentTraktPlayback by mutableStateOf<TraktMedia?>(null)
    var portalPickerFor by mutableStateOf<Pair<PortalSection, PortalTitle>?>(null)
    var categoryMenu by mutableStateOf<Pair<PortalSection, String?>?>(null)
    var restoreLiveFocus by mutableStateOf(false)
    var attemptSerial by mutableLongStateOf(0L)
    var playbackSourceBadge by mutableStateOf<Pair<String, String>?>(null)
    var triedHls by mutableStateOf(false)
    var batchProgress by mutableStateOf<Pair<Int, Int>?>(null)
    var batchSummary by mutableStateOf<Pair<Int, Int>?>(null)
    var selectedRefreshSources by mutableStateOf<Set<String>>(emptySet())
    var sourceError by mutableStateOf<Int?>(null)
    var importStatuses by mutableStateOf(emptyMap<PortalSection, ImportStatus>())
    var metadataNextPages by mutableStateOf(emptyMap<String, Int>())
    var catalogLoading by mutableStateOf(emptySet<PortalSection>())
    var catalogErrors by mutableStateOf(emptySet<PortalSection>())
    var catalogueReturnTitle by mutableStateOf<PortalTitle?>(null)
    var episodeError by mutableStateOf(false)
    var sourceContentCounts by mutableStateOf<Map<String, PortalContentCounts>>(emptyMap())
    var combinedContentCounts by mutableStateOf(PortalContentCounts(0, 0, 0))
    var diagnosticsStatus by mutableStateOf<String?>(null)
    var startupPortalRefresh by mutableStateOf<Pair<Int, Int>?>(null)
    var startupPortalCheck by mutableStateOf<Pair<Int, Int>?>(null)
    var storageError by mutableStateOf<Int?>(null)
    var epgFetchedAt by mutableStateOf(emptyMap<String, Long>())
    var watchStatusAction by mutableStateOf<((Boolean) -> Unit)?>(null)
    var fullscreenOverlayContent by mutableStateOf(GuidePanel.CHANNELS)
    var guideProgramme by mutableStateOf<Pair<StalkerChannel, PortalProgramme>?>(null)
    var metadataSettingsOpen by mutableStateOf<Boolean>(false)
    var traktSettingsOpen by mutableStateOf<Boolean>(false)
    var kidsProtectionOpen by mutableStateOf<Boolean>(false)
    var termsOpen by mutableStateOf<Boolean>(false)
    var whatsNewOpen by mutableStateOf<Boolean>(false)
    var languageOpen by mutableStateOf<Boolean>(false)
    var profilePickerOpen by mutableStateOf<Boolean>(false)
    var profileStartChecked by mutableStateOf<Boolean>(false)
    var kidsNameDialog by mutableStateOf<Boolean>(false)
    var kidsColorOpen by mutableStateOf<Boolean>(false)
    var kidsAccentVersion by mutableIntStateOf(0)
    /** The Trakt page was opened for the kids account (by the admin, in the Kinderschutz). */
    var traktForKids by mutableStateOf<Boolean>(false)
    var kidsTraktName by mutableStateOf<String?>(null)
    var exitBlockedUntil by mutableLongStateOf(0L)
    var exitDialogOpen by mutableStateOf<Boolean>(false)
    var accentPaletteOpen by mutableStateOf<Boolean>(false)
    var settingsContentFocused by mutableStateOf<Boolean>(false)
    var episodeSettingsOpen by mutableStateOf<Boolean>(false)
    var epgSettingsOpen by mutableStateOf<Boolean>(false)
    var epgUpdating by mutableStateOf<Boolean>(false)
    var epgUpdateFailed by mutableStateOf<Boolean>(false)
    var epgSourceWriteFailed by mutableStateOf<Boolean>(false)
    var epgSourceSaving by mutableStateOf<Boolean>(false)
    var askForCountries by mutableStateOf<Boolean>(false)
    var choosingCountries by mutableStateOf<Boolean>(false)
    var showHiddenCategories by mutableStateOf<Boolean>(false)
    var sourceRefreshDialogOpen by mutableStateOf<Boolean>(false)
    var controlSettingsOpen by mutableStateOf<Boolean>(false)
    var settingsPageOpenedAt by mutableLongStateOf(0L)
    var remoteAccessOpen by mutableStateOf<Boolean>(false)
    /** "Per Handy einstellen" opened from the PIN prompt: a window over the current screen. */
    var remoteAccessStandalone by mutableStateOf<Boolean>(false)
    var expiryLeadPickerOpen by mutableStateOf<Boolean>(false)
    var metadataBusy by mutableStateOf<Boolean>(false)
    var metadataError by mutableStateOf<Boolean>(false)
    var traktBusy by mutableStateOf<Boolean>(false)
    var sendingWatchStatus by mutableStateOf<Boolean>(false)
    var traktSyncRequested by mutableStateOf<Boolean>(false)
    var startupPortalRefreshStarted by mutableStateOf<Boolean>(false)
    var groupUsesTmdb by mutableStateOf<Boolean>(false)
    var groupLoading by mutableStateOf<Boolean>(false)
    var groupError by mutableStateOf<Boolean>(false)
    var focusFirstTitle by mutableStateOf<Boolean>(false)
    var focusTitlePrimary by mutableStateOf<Boolean>(false)
    var resumeEpisodeMissing by mutableStateOf<Boolean>(false)
    var liveGroupsLoading by mutableStateOf<Boolean>(false)
    var liveGroupsReady by mutableStateOf<Boolean>(false)
    var cachedStartupAttempt by mutableStateOf<Boolean>(false)
    var restoredLastLiveChannel by mutableStateOf<Boolean>(false)
    var overlayScheduleFocused by mutableStateOf<Boolean>(false)
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun PlayerHome(
    player: ExoPlayer? = null,
    storage: PortalStorage? = null,
    accentChoice: AccentChoice = AccentChoice.RED,
    accentGradient: AccentGradient? = null,
    onAccentGradientChange: (AccentGradient) -> Unit = {},
    onAccentChoiceChange: (AccentChoice) -> Unit = {},
    registerLongBack: (((() -> Boolean)?) -> Unit) = {},
    registerScreenBack: ((((Boolean) -> Boolean)?) -> Unit) = {},
    registerPlaybackInput: (((() -> Unit)?) -> Unit) = {},
    registerPromptInput: ((((android.view.KeyEvent) -> Boolean)?) -> Unit) = {},
    registerKeySounds: (Boolean) -> Unit = {},
    onExit: () -> Unit = {},
    onAccountChange: (kids: Boolean) -> Unit = {},
    onFullscreen: (Boolean) -> Unit = {},
    canPlay: () -> Boolean = { false }
) {
    val ui = remember { PlayerHomeFlags() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val preview = LocalInspectionMode.current
    val preferences = remember(context, preview) {
        if (preview) null else context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    }
    /** The page the app opens with (Live-TV, Filme or Serien). */
    var startDestination by remember(preferences) { mutableStateOf(savedStartDestination(preferences)) }
    var selected by remember { mutableStateOf<Destination?>(startDestination) }
    // Films or series as the start page open their first category.
    var pendingVodCategory by rememberSaveable {
        mutableStateOf(startDestination.takeIf { it == Destination.MOVIES || it == Destination.SERIES })
    }
    var startupFirstCategory by rememberSaveable { mutableStateOf(pendingVodCategory != null) }
    var lastFocused by rememberSaveable { mutableStateOf(startDestination) }
    var settingsReturnDestination by rememberSaveable { mutableStateOf(startDestination) }
    var lastSettingsSection by rememberSaveable { mutableStateOf(SettingsSection.GENERAL) }
    val initialFocus = remember { FocusRequester() }
    val settingsFocusRequesters = remember {
        SettingsSection.entries.associateWith { FocusRequester() }
    }
    val isTelevision = remember(context) {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
    }
    var showSettingsBackButton by remember(preferences, isTelevision) {
        mutableStateOf(preferences?.getBoolean("show_settings_back_button", !isTelevision) ?: !isTelevision)
    }
    var keySounds by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("key_sounds", true) ?: true)
    }
    var vodLiveView by remember(preferences) {
        val saved = preferences?.getString("vod_live_view", null)
        mutableStateOf(VodLiveView.entries.firstOrNull { it.name == saved } ?: VodLiveView.BACKGROUND)
    }
    val parental = remember(preferences) { ParentalControls(preferences) }
    val profiles = remember(preferences) { UserProfiles(preferences) }
    val kidsAccountActive = parental.kidsMode && profiles.hasKids
    /** The kids account keeps its own progress, watchlist, favourites and Trakt account. */
    val separateKidsData = kidsAccountActive && !profiles.kidsShareData
    fun userDataKey(name: String) = if (separateKidsData) "kids:$name" else name
    val progressPreferences = remember(context, preview, separateKidsData) {
        if (preview) null else context.getSharedPreferences(
            if (separateKidsData) "vod_progress_kids" else "vod_progress", Context.MODE_PRIVATE
        )
    }
    // Which portal's stream ran best per Live-TV channel; that one is tried first next time.
    val liveStreamHistory = remember(context, preview) {
        LiveStreamHistory(if (preview) null else context.getSharedPreferences(LiveStreamHistory.FILE, Context.MODE_PRIVATE))
    }
    var seriesActivity by remember(progressPreferences) {
        mutableStateOf(decodeSeriesActivity(progressPreferences?.getString("series_activity", null)))
    }
    /** "Mein Bereich" of films or series: continue watching, watchlist and recently watched. */
    var myAreaSection by rememberSaveable { mutableStateOf<PortalSection?>(null) }
    var movieHistory by remember(progressPreferences) {
        mutableStateOf(decodeMovieHistory(progressPreferences?.getString("movie_history", null)))
    }
    var movieBookmarks by remember(progressPreferences) {
        mutableStateOf(decodeMovieHistory(progressPreferences?.getString("movie_bookmarks", null)))
    }
    var metadataClient by remember { mutableStateOf<TmdbClient?>(null) }
    var traktSession by remember { mutableStateOf<TraktSession?>(null) }
    /** The user's own Trakt API app; needed before a (new) account can be connected. */
    var traktApp by remember { mutableStateOf<TraktApp?>(null) }
    var exitConfirmation by remember(preferences) {
        val saved = preferences?.getString("exit_confirmation", null)
        mutableStateOf(ExitConfirmation.entries.firstOrNull { it.name == saved } ?: ExitConfirmation.DIALOG)
    }
    // Settings: whether focus is in the rows on the right, and a request to move it there.
    var settingsContentFocusRequest by remember { mutableIntStateOf(0) }
    val settingsContentFocus = remember { FocusRequester() }
    val settingsFocusMemory = remember { SettingsFocusMemory() }
    val ageRatings = remember { HashMap<String, Int?>() }
    val playbackNoticeHost = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(ui.traktPlaybackNotice) {
        val notice = ui.traktPlaybackNotice ?: return@LaunchedEffect
        playbackNoticeHost.showSnackbar(notice)
        ui.traktPlaybackNotice = null
    }
    LaunchedEffect(ui.portalRefreshNotice) {
        val notice = ui.portalRefreshNotice ?: return@LaunchedEffect
        playbackNoticeHost.showSnackbar(notice)
        ui.portalRefreshNotice = null
    }
    var newEpisodesNotice by remember { mutableStateOf<String?>(null) }
    val castReturns = remember { mutableStateListOf<CastReturn>() }
    /** Cast found for the open details title; the "Besetzung" button only appears when it is not empty. */
    /** Keys of all films watched according to Trakt (null until the first full import). */
    var traktWatchedMovieKeys by remember(progressPreferences) {
        mutableStateOf(progressPreferences?.getStringSet("trakt_watched_movie_keys", null)?.toSet())
    }
    LaunchedEffect(newEpisodesNotice) {
        val notice = newEpisodesNotice ?: return@LaunchedEffect
        // Episode checks run one series at a time; collect findings briefly before showing them.
        delay(3_000)
        scope.launch {
            playbackNoticeHost.showSnackbar(notice, duration = androidx.compose.material3.SnackbarDuration.Long)
        }
        newEpisodesNotice = null
    }
    var settingsSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var mainMenuStyle by remember(preferences) {
        mutableStateOf(MainMenuStyle.entries.firstOrNull {
            it.name == preferences?.getString("main_menu_style", null)
        } ?: MainMenuStyle.BOTH)
    }
    var providerCategoryStyle by remember(preferences) {
        mutableStateOf(ProviderCategoryStyle.entries.firstOrNull {
            it.name == preferences?.getString("provider_category_style", null)
        } ?: ProviderCategoryStyle.BOTH)
    }
    var guideAppearance by remember(preferences) { mutableStateOf(readGuideAppearance(preferences)) }
    var useXmlTv by remember(preferences) { mutableStateOf(preferences?.getBoolean("xmltv_enabled", false) ?: false) }
    var epgAtStartup by remember(preferences) { mutableStateOf(preferences?.getBoolean("epg_at_startup", true) ?: true) }
    val portalRefreshAtStartupDefault = remember(preferences) {
        val settings = preferences
        if (settings != null && settings.getInt("portal_refresh_startup_migration", 0) < 51) {
            settings.edit {
                putBoolean("refresh_portals_at_startup", true)
                putInt("portal_refresh_startup_migration", 51)
            }
            true
        } else settings?.getBoolean("refresh_portals_at_startup", true) ?: true
    }
    var refreshPortalsAtStartup by remember(preferences) {
        mutableStateOf(portalRefreshAtStartupDefault)
    }
    var playLastChannelAtStartup by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("play_last_channel_at_startup", true) ?: true)
    }
    /** The last channel starts straight in fullscreen instead of the guide. */
    var startInFullscreen by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("start_in_fullscreen", true) ?: true)
    }
    var epgRefreshHours by remember(preferences) { mutableIntStateOf(preferences?.getInt("epg_refresh_hours", 6) ?: 6) }
    var epgHorizonHours by remember(preferences) { mutableIntStateOf(preferences?.getInt("epg_horizon_hours", 24) ?: 24) }
    var subtitleDefaultEnabled by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("subtitle_default_enabled", false) ?: false)
    }
    var forcedSubtitlesEnabled by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("forced_subtitles_enabled", true) ?: true)
    }
    var xmlTvSources by remember { mutableStateOf(emptyList<String>()) }
    var xmlTvSourcesLoaded by remember { mutableStateOf(storage == null) }
    var epgRefreshRequest by remember { mutableIntStateOf(0) }
    var epgSourceDraft by remember { mutableStateOf("") }
    val activeEpisodeProvider = when {
        traktSession != null -> "Trakt"
        metadataClient != null -> "TMDB"
        else -> "TVmaze"
    }
    var autoNextEpisode by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_next_episode", true) ?: true)
    }
    var decodeAudioForIntro by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("decode_audio_for_intro", false) ?: false)
    }
    var autoSkipIntro by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_skip_intro", false) ?: false)
    }
    var episodeNoticeSeconds by remember(preferences) {
        mutableIntStateOf((preferences?.getInt("episode_notice_seconds", 15) ?: 15).coerceIn(5, 60))
    }
    var manualIntro by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("manual_intro", false) ?: false)
    }
    var manualCredits by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("manual_credits", false) ?: false)
    }
    var introStartSeconds by remember(preferences) {
        mutableIntStateOf(preferences?.getInt("intro_start_seconds", 0) ?: 0)
    }
    var introEndSeconds by remember(preferences) {
        mutableIntStateOf(preferences?.getInt("intro_end_seconds", 60) ?: 60)
    }
    var creditsBeforeEndSeconds by remember(preferences) {
        mutableIntStateOf(preferences?.getInt("credits_before_end_seconds", 60) ?: 60)
    }
    val seriesOpenings = remember { mutableMapOf<Pair<String, String>, Pair<String, List<EpisodeFrame>>>() }
    val seriesOpeningAudio = remember { mutableMapOf<Pair<String, String>, Pair<String, List<AudioPrint>>>() }
    val seriesClosings = remember { mutableMapOf<Pair<String, String>, Pair<String, List<EpisodeFrame>>>() }
    val introStore = remember(context, preview) { if (preview) null else IntroFingerprintStore.create(context) }
    LaunchedEffect(progressPreferences) {
        // Opening fingerprints of earlier versions used another hash and grew without limit;
        // they are rebuilt automatically in the bounded fingerprint store.
        val preferences = progressPreferences ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val obsolete = preferences.all.keys.filter { it.startsWith("opening:") || it.startsWith("opening-samples:") }
            if (obsolete.isNotEmpty()) preferences.edit { obsolete.forEach { remove(it) } }
        }
    }
    var fullscreenVideoSurface by remember { mutableStateOf<SurfaceView?>(null) }
    var detectedTiming by remember { mutableStateOf<Pair<String, EpisodeTiming>?>(null) }
    var nextEpisodeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var previousEpisodeAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var checkSeriesInactivity by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("check_series_inactivity", true) ?: true)
    }
    var inactiveEpisodeLimit by remember(preferences) {
        mutableIntStateOf((preferences?.getInt("inactive_episode_limit", 3) ?: 3).coerceIn(1, 10))
    }
    var silentEpisodes by remember { mutableIntStateOf(0) }
    /** Media item whose skip-intro offer was used or dismissed; it is not offered again for it. */
    /**
     * Media item and skip target up to which the skip-intro offer was used (or Long.MAX_VALUE
     * after Back). It returns only if the opening turns out to end clearly later.
     */
    /** Whether the current skip offer is for end credits rather than the opening. */
    var countries by remember(preferences) {
        mutableStateOf(
            preferences?.getString("countryOrder", null)?.split(',')?.filter { it.isNotBlank() }?.toSet()
                ?: preferences?.getStringSet("countries", emptySet()).orEmpty().toSet()
        )
    }
    var automaticCountries by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("autoCountries", true) ?: true)
    }
    var hiddenCategories by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("hidden", emptySet()).orEmpty().toSet())
    }
    var mergeSources by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("mergeSources", true) ?: true)
    }
    var mergedIndividualPortals by remember(preferences) {
        mutableStateOf(preferences?.getStringSet("merged_individual_portals", emptySet()).orEmpty().toSet())
    }
    // Portal tried first for live TV, movies and series; the others remain the automatic fallback.
    var preferredPortal by remember(preferences) { mutableStateOf(preferences?.getString("preferred_portal", null)) }
    fun setPreferredPortal(identity: String?) {
        preferredPortal = identity
        preferences?.edit { if (identity == null) remove("preferred_portal") else putString("preferred_portal", identity) }
    }
    // Portal chosen on a film's or series' page (title key -> portal), kept while the app runs.
    val titlePortalChoices = remember { mutableStateMapOf<String, String>() }
    // Media item whose sound was already checked at start (a stream without sound switches portal).
    var audioCheckedFor by remember { mutableStateOf<String?>(null) }
    val categoryOrders = remember { mutableStateMapOf<String, List<String>>() }
    var details by remember { mutableStateOf<Pair<PortalSection, PortalTitle>?>(null) }
    var favorites by remember(preferences, separateKidsData) {
        mutableStateOf(preferences?.getStringSet(userDataKey("favorites"), emptySet()).orEmpty().toSet())
    }
    var watchlist by remember(preferences, separateKidsData) {
        mutableStateOf(preferences?.getStringSet(userDataKey("watchlist"), emptySet()).orEmpty().toSet())
    }
    var searchQuery by remember { mutableStateOf("") }
    var liveSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var channels by remember { mutableStateOf(emptyList<StalkerChannel>()) }
    var genres by remember { mutableStateOf(emptyMap<String, String>()) }
    var category by remember(preferences) {
        mutableStateOf(preferences?.getString("last_live_category", null))
    }
    var lastLiveCategory by remember(preferences) {
        mutableStateOf(preferences?.getString("last_live_category", null))
    }
    val categoryScroll = rememberLazyListState()
    val guideScroll = rememberLazyListState()
    val guideState = rememberSaveableStateHolder()
    val channelScrollByCategory = remember { mutableMapOf<String, LazyListState>() }
    val channelScroll = remember(category) {
        category?.let { channelScrollByCategory.getOrPut(it) { LazyListState() } }
            ?: LazyListState()
    }
    val contentScroll = rememberScrollState()
    val menuScroll = rememberScrollState()
    var playing by remember { mutableStateOf<StalkerChannel?>(null) }
    var playbackSelection by remember { mutableStateOf<PlaybackSelection?>(null) }
    var playbackAttempt by remember { mutableStateOf<PlaybackAttempt?>(null) }
    var lastBadgedAttempt by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(false) }
    var programmeChannel by remember { mutableStateOf<StalkerChannel?>(null) }
    var epgChannelIds by remember { mutableStateOf(emptySet<String>()) }
    var starting by remember { mutableStateOf<StalkerChannel?>(null) }
    // Starting the last channel in fullscreen shows black (and the channel info) at once,
    // instead of the guide until the stream is ready.
    val fullscreenAtStart = remember {
        startDestination == Destination.LIVE && playLastChannelAtStartup && startInFullscreen &&
            preferences?.getString("last_live_channel", null) != null
    }
    var fullscreen by remember { mutableStateOf(fullscreenAtStart) }
    /** The channel info at the top left, shown once when the app starts in fullscreen. */
    var startupInfoVisible by remember { mutableStateOf(fullscreenAtStart) }
    val appUpdates = rememberAppUpdates()
    var lastBack by remember { mutableLongStateOf(0L) }
    /** When the position was last moved on purpose (seek, reload), so the loop check ignores it. */
    val playerPositionResetAt = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    /** When a running Live-TV stream was last reloaded after a short network error. */
    var liveRetryAt by remember { mutableLongStateOf(0L) }
    var connecting by remember { mutableStateOf(false) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var catalogSession by remember { mutableStateOf<Pair<StalkerPortal, String?>?>(null) }
    var catalogs by remember { mutableStateOf(emptyMap<PortalSection, PortalCatalog>()) }
    var selectedGroup by remember { mutableStateOf<Pair<PortalSection, PortalGroup>?>(null) }
    var groupCatalog by remember { mutableStateOf<PortalCatalog?>(null) }
    var metadataTitles by remember {
        mutableStateOf(emptyMap<Int, Pair<PortalTitle, List<SourceItem<PortalTitle>>>>())
    }
    var groupPages by remember { mutableStateOf(emptyMap<String, PortalCatalog>()) }
    var groupVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var searchedVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var groupJob by remember { mutableStateOf<Job?>(null) }
    var playbackError by remember { mutableStateOf<Int?>(null) }
    var vodSection by remember { mutableStateOf<PortalSection?>(null) }
    val vodPlaybackViews = remember(preferences) {
        mutableStateMapOf(*listOf(PortalSection.MOVIES, PortalSection.SERIES).map { section ->
            section to (VodLiveView.entries.firstOrNull {
                it.name == preferences?.getString("vod_view_${section.name}", null)
            } ?: VodLiveView.CORNER)
        }.toTypedArray())
    }
    val cataloguePlaybackView = vodPlaybackViews[vodSection] ?: vodLiveView
    val controlBindings = remember(preferences) {
        mutableStateMapOf(*GuideInput.entries.mapNotNull { input ->
            ControlAction.entries.firstOrNull {
                it.name == preferences?.getString("control_${input.name}", null)
            }?.let { input to it }
        }.toTypedArray())
    }
    val remotePinFailures = remember { java.util.concurrent.atomic.AtomicInteger() }
    var openedSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var seasons by remember { mutableStateOf<List<PortalSeason>?>(null) }
    var playbackSeasons by remember { mutableStateOf(emptyList<PortalSeason>()) }
    var playbackSeries by remember { mutableStateOf<PortalTitle?>(null) }
    var playbackMovieTitle by remember { mutableStateOf<PortalTitle?>(null) }
    var playbackSeason by remember { mutableStateOf<PortalSeason?>(null) }
    var playbackEpisode by remember { mutableStateOf<PortalTitle?>(null) }
    var overlaySeason by remember { mutableStateOf<PortalSeason?>(null) }
    var playbackEpisodeVariants by remember {
        mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>())
    }
    var openedSeason by remember { mutableStateOf<PortalSeason?>(null) }
    val seriesDetailScroll = rememberLazyListState()
    var episodeVariants by remember { mutableStateOf(emptyMap<PortalTitle, List<SourceItem<PortalTitle>>>()) }
    var episodeJob by remember { mutableStateOf<Job?>(null) }
    var address by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    var storedSource by remember { mutableStateOf<SavedPortal?>(null) }
    var savedSources by remember { mutableStateOf(emptyList<SavedPortal>()) }
    val sourceContentKeys = savedSources.map { source ->
        listOf(
            source.identity, source.enabled, System.identityHashCode(source.channels),
            System.identityHashCode(source.genres), System.identityHashCode(source.catalogs),
            source.epgChannelIds.hashCode()
        )
    }
    LaunchedEffect(sourceContentKeys, selected) {
        // These merged counts are only shown on the Sources page. Building them
        // during every cold start duplicated the much more important live-group
        // work and competed with restoring the cached guide.
        if (selected != Destination.SOURCES) return@LaunchedEffect
        val snapshot = savedSources
        val counts = withContext(Dispatchers.Default) {
            val perSource = snapshot.associate { source ->
                val live = buildLiveChannelGroups(listOf(source), combine = false, mergeWithinSource = true).size
                fun uniqueCount(section: PortalSection): Int = source.catalogs[section]?.titles.orEmpty()
                    .distinctBy { title ->
                        val year = vodReleaseYear(title)
                        "${normalizedLabel(displayVodTitle(title))}:${year ?: "?"}"
                    }.size
                source.identity to PortalContentCounts(live, uniqueCount(PortalSection.MOVIES), uniqueCount(PortalSection.SERIES))
            }
            val enabled = snapshot.filter { it.enabled }
            fun mergedCount(section: PortalSection): Int = mergeCatalogueTitles(
                enabled.flatMap { source -> source.catalogs[section]?.titles.orEmpty().map { SourceItem(source, it) } },
                requireVerifiedIdentity = false
            ).size
            perSource to PortalContentCounts(
                buildLiveChannelGroups(enabled, combine = true, mergeWithinSource = true).size,
                mergedCount(PortalSection.MOVIES), mergedCount(PortalSection.SERIES)
            )
        }
        ui.sourceContentCounts = counts.first
        ui.combinedContentCounts = counts.second
    }
    var diagnosticsTree by remember(context) {
        mutableStateOf(context.getSharedPreferences("diagnostics_settings", Context.MODE_PRIVATE)
            .getString("tree_uri", null)?.let(Uri::parse))
    }
    val chooseDiagnosticsFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val permission = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val persisted = runCatching { context.contentResolver.takePersistableUriPermission(uri, permission) }
            if (persisted.isSuccess) {
                context.getSharedPreferences("diagnostics_settings", Context.MODE_PRIVATE)
                    .edit { putString("tree_uri", uri.toString()) }
                diagnosticsTree = uri
                AppDiagnostics.record(context, "diagnostics_folder_selected")
                ui.diagnosticsStatus = context.getString(R.string.diagnostics_folder_selected)
            } else {
                ui.diagnosticsStatus = context.getString(R.string.diagnostics_export_failed)
            }
        }
    }
    val saveDiagnosticsFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching { AppDiagnostics.exportToDocument(context, uri) }
            .onSuccess { ui.diagnosticsStatus = context.getString(R.string.diagnostics_export_complete) }
            .onFailure { ui.diagnosticsStatus = context.getString(R.string.diagnostics_export_failed) }
    }
    val secondarySessions = remember { mutableMapOf<String, StalkerPortal>() }
    val sourcesForBrowsing = remember(sourceContentKeys, storedSource?.identity, mergeSources) {
        val enabledSources = savedSources.filter { it.enabled }
        val active = storedSource?.takeIf { it.enabled }
        if (mergeSources) enabledSources.ifEmpty { listOfNotNull(active) } else listOfNotNull(active)
    }
    // Live-TV, films and series only appear when an active source provides them. Without any
    // source everything stays visible so the setup remains reachable.
    val visibleDestinations = remember(sourcesForBrowsing, parental.hiddenAreas, kidsAccountActive, profiles.kidsAreas, profiles.kidsName) {
        fun offers(section: PortalSection) = sourcesForBrowsing.any { source ->
            source.catalogs[section]?.let { it.titles.isNotEmpty() || it.groups.isNotEmpty() } == true
        }
        val available = if (sourcesForBrowsing.isEmpty()) mainDestinations else mainDestinations.filter { destination ->
            when (destination) {
                Destination.LIVE -> sourcesForBrowsing.any { it.channels.isNotEmpty() }
                Destination.MOVIES -> offers(PortalSection.MOVIES)
                Destination.SERIES -> offers(PortalSection.SERIES)
                else -> true
            }
        }
        // Areas hidden in the Einstellungsschutz (or not allowed for the kids account); if that
        // would leave none, they all stay. The kids account has no settings; "Abmelden" only
        // exists with a kids account.
        val hidden = if (kidsAccountActive) MAIN_AREAS.toSet() - profiles.kidsAreas else parental.hiddenAreas
        val shown = available.filterNot { it.name in hidden }
        (if (shown.any { it.name in MAIN_AREAS }) shown else available).filter { destination ->
            when (destination) {
                Destination.PROFILES -> profiles.hasKids
                Destination.SETTINGS -> !kidsAccountActive
                else -> true
            }
        }
    }
    LaunchedEffect(visibleDestinations) {
        val content = setOf(Destination.LIVE, Destination.MOVIES, Destination.SERIES)
        val current = selected
        if (current in content && current !in visibleDestinations) {
            selected = visibleDestinations.firstOrNull { it in content } ?: Destination.SETTINGS
        }
    }
    val combine = mergeSources
    fun categoryOrderKey(section: PortalSection): String =
        "category_order_${if (combine) "combined" else storedSource?.identity.orEmpty()}_${section.name}"

    fun <T> withCategoryOrder(section: PortalSection, values: List<T>, id: (T) -> String): List<T> {
        val key = categoryOrderKey(section)
        val stored = categoryOrders[key] ?: runCatching {
            val data = org.json.JSONArray(preferences?.getString(key, null) ?: "[]")
            List(data.length()) { data.getString(it) }
        }.getOrDefault(emptyList())
        if (stored.isEmpty()) return values
        val positions = stored.withIndex().associate { it.value to it.index }
        return values.sortedBy { positions[id(it)] ?: Int.MAX_VALUE }
    }
    val mergePortalChannels = storedSource?.identity in mergedIndividualPortals
    var editing by remember { mutableStateOf(false) }
    var initialPortalPromptDismissed by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(storage != null) }
    var autoDisableUnavailablePortals by remember(preferences) {
        mutableStateOf(preferences?.getBoolean("auto_disable_unavailable_portals", true) ?: true)
    }
    // How long before a portal's end date the notice comes (default one week).
    var expiryLead by remember(preferences) { mutableStateOf(ExpiryLead.parse(preferences?.getString("expiry_lead", null))) }
    fun changeExpiryLead(lead: ExpiryLead) {
        expiryLead = lead
        preferences?.edit { putString("expiry_lead", lead.key) }
    }
    val detailMetadata by produceState<TmdbTitle?>(null, metadataClient, details, mergeSources, traktSession?.username) {
        value = null
        if (!mergeSources || traktSession != null) return@produceState
        val client = metadataClient ?: return@produceState
        val entry = details ?: return@produceState
        if (entry.first == PortalSection.SERIES && titleIdentity(entry.second) == null) return@produceState
        value = try {
            client.lookup(entry.first, entry.second)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
    val tvmazeDetailMetadata by produceState<TvmazeTitleMetadata?>(null, traktSession?.username, metadataClient, details) {
        value = null
        if (traktSession != null || metadataClient != null) return@produceState
        val entry = details?.takeIf { it.first == PortalSection.SERIES } ?: return@produceState
        value = try {
            TvmazeClient.lookupTitleMetadata(entry.second)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    // Smart home: the light rule and the Home Assistant token (kept encrypted).
    LaunchedEffect(storage) {
        val haToken = runCatching { storage?.readHomeAssistantToken() }.getOrNull()
        LightAutomation.init(context, storage, haToken)
    }

    LaunchedEffect(storage) {
        AppDiagnostics.record(context, "portal_library_restore_started")
        try {
            metadataClient = storage?.readMetadataToken()?.let(::TmdbClient)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.metadataError = true
        }
    }

    LaunchedEffect(storage) {
        traktApp = runCatching { storage?.readTraktApp() }.getOrNull()
    }
    fun saveTraktApp(clientId: String?, clientSecret: String?): String? {
        val id = clientId?.trim().orEmpty().ifBlank { traktApp?.clientId.orEmpty() }
        val secret = clientSecret?.trim()?.ifBlank { null } ?: traktApp?.clientSecret
        if (!TraktApp.isValidValue(id)) return context.getString(R.string.trakt_app_invalid_id)
        if (secret != null && !TraktApp.isValidValue(secret)) return context.getString(R.string.trakt_app_invalid_secret)
        val app = TraktApp(id, secret)
        traktApp = app
        scope.launch { runCatching { storage?.writeTraktApp(app) } }
        return null
    }

    LaunchedEffect(storage, profiles.kidsShareData, profiles.kidsName) {
        ui.kidsTraktName = runCatching { storage?.readTraktSession(true)?.username }.getOrNull()
    }
    // Loaded at start and again when the account changes (a kids account may have its own).
    LaunchedEffect(storage, separateKidsData) {
        try {
            val session = storage?.readTraktSession(separateKidsData)
            traktSession = session
            ui.traktDeviceCode = null
            if (session != null) ui.traktSyncRequested = true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.traktStatus = context.getString(R.string.trakt_session_read_failed)
        }
    }

    suspend fun <T> withTraktSession(action: suspend (TraktApi, TraktSession) -> T): T {
        var session = storage?.readTraktSession(separateKidsData) ?: traktSession
            ?: error(context.getString(R.string.trakt_not_connected))
        // A connected account keeps the API app it was connected with.
        val api = TraktApi(session.clientId, session.clientSecret) { id, args -> context.getString(id, *args) }
        if (session.expiresAtMillis <= System.currentTimeMillis() + 60_000L) {
            session = api.refresh(session)
            storage?.writeTraktSession(session, separateKidsData)
            traktSession = session
        }
        return action(api, session)
    }

    val traktDetailMetadata by produceState<TraktTitleMetadata?>(null, traktSession?.username, details) {
        value = null
        if (traktSession == null) return@produceState
        val entry = details ?: return@produceState
        value = try {
            withTraktSession { api, session -> api.lookupTitleMetadata(entry.first, entry.second, session.accessToken) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    fun connectTrakt() {
        if (ui.traktBusy) return
        // The kids account uses the Trakt API of the admin: the saved one, or the one the admin's
        // account was connected with (earlier versions had it built in).
        val app = traktApp ?: traktSession?.takeIf { ui.traktForKids }?.let { TraktApp(it.clientId, it.clientSecret) }
        if (app == null) {
            ui.traktStatus = context.getString(R.string.trakt_app_missing)
            return
        }
        ui.traktBusy = true
        ui.traktStatus = null
        scope.launch {
            try {
                val api = TraktApi(app.clientId, app.clientSecret) { id, args -> context.getString(id, *args) }
                val code = api.beginDeviceAuthorization()
                ui.traktDeviceCode = code
                var interval = code.intervalSeconds
                val deadline = SystemClock.elapsedRealtime() + code.expiresInSeconds * 1_000L
                while (SystemClock.elapsedRealtime() < deadline) {
                    delay(interval * 1_000L)
                    when (val result = api.pollDeviceAuthorization(code)) {
                        TraktDevicePoll.Pending -> Unit
                        TraktDevicePoll.SlowDown -> interval = (interval + 5).coerceAtMost(60)
                        is TraktDevicePoll.Authorized -> {
                            val username = runCatching { api.accountName(result.session.accessToken) }
                                .getOrDefault("Trakt")
                            val session = result.session.copy(username = username)
                            val forKids = ui.traktForKids
                            storage?.writeTraktSession(session, forKids)
                            // The session of the account that is signed in is used right away.
                            if (forKids == separateKidsData) traktSession = session
                            if (forKids) ui.kidsTraktName = username
                            ui.traktSyncRequested = true
                            ui.traktDeviceCode = null
                            ui.traktStatus = context.getString(R.string.trakt_connected_status, username)
                            return@launch
                        }
                    }
                }
                ui.traktDeviceCode = null
                ui.traktStatus = context.getString(R.string.trakt_code_expired)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                ui.traktDeviceCode = null
                ui.traktStatus = error.message ?: context.getString(R.string.trakt_connect_failed)
            } finally {
                ui.traktBusy = false
            }
        }
    }

    fun disconnectTrakt() {
        if (ui.traktBusy) return
        scope.launch {
            try {
                val forKids = ui.traktForKids
                storage?.clearTraktSession(forKids)
                if (forKids == separateKidsData) traktSession = null
                if (forKids) ui.kidsTraktName = null
                ui.traktDeviceCode = null
                ui.traktStatus = context.getString(R.string.trakt_disconnected_status)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ui.traktStatus = context.getString(R.string.trakt_disconnect_failed)
            }
        }
    }

    fun syncTrakt(forceSeriesHistory: Boolean = false) {
        if (ui.traktBusy || ui.sendingWatchStatus) return
        ui.traktBusy = true
        ui.traktStatus = context.getString(R.string.trakt_checking_changes)
        fun importedTitleKey(section: PortalSection, title: PortalTitle): String =
            "${catalogSession?.first?.sourceLabel.orEmpty()}:${section.name}:${title.id ?: title.name}"
        fun importedSeriesKey(title: PortalTitle): String =
            titleIdentity(title) ?: importedTitleKey(PortalSection.SERIES, title)
                fun traktLibraryKey(media: TraktMedia): String =
                    "trakt:${media.section.name}:${media.traktId ?: media.imdbId ?: media.tmdbId ?: normalizedLabel(media.name)}"
        scope.launch {
            try {
                val enabledSavedSources = savedSources.filter { it.enabled }
                val localSaved = enabledSavedSources.flatMap { source ->
                    listOf(PortalSection.MOVIES, PortalSection.SERIES).flatMap { section ->
                        source.catalogs[section]?.titles.orEmpty().filter { title ->
                            importedTitleKey(section, title) in watchlist
                        }.map { title ->
                            TraktMedia(section, displayVodTitle(title), title.year, title.imdbId, title.tmdbId, null)
                        }
                    }
                }.distinctBy { "${it.section}:${it.imdbId ?: it.tmdbId ?: normalizedLabel(it.name)}" }
                var pendingActivities: String? = null
                val library = withTraktSession { api, session ->
                    val activityKey = "trakt_last_activities:${session.username.lowercase()}"
                    val dirtyKey = "trakt_watchlist_dirty:${session.username.lowercase()}"
                    val activities = api.loadLastActivities(session.accessToken)
                    val cachedActivities = preferences?.getString(activityKey, null)
                    val watchlistDirty = preferences?.getBoolean(dirtyKey, true) ?: true
                    val historyReconcileKey = "trakt_series_history_reconciled:${session.username.lowercase()}"
                    val reconcileSeriesHistory = forceSeriesHistory ||
                        preferences?.getBoolean(historyReconcileKey, false) != true
                    if (cachedActivities == activities && !watchlistDirty && !reconcileSeriesHistory && traktWatchedMovieKeys != null) {
                        withContext(Dispatchers.Main) {
                            ui.traktStatus = context.getString(R.string.trakt_cache_current)
                        }
                        return@withTraktSession null
                    }
                    withContext(Dispatchers.Main) {
                        ui.traktStatus = context.getString(R.string.trakt_checking_changes)
                    }
                    val remote = api.loadLibrary(
                        session.accessToken,
                        onProgress = { progress -> withContext(Dispatchers.Main) { ui.traktStatus = progress } },
                        previousActivities = cachedActivities,
                        currentActivities = activities,
                        reconcileSeriesHistory = reconcileSeriesHistory,
                        // All watched films are loaded once, then again whenever they change.
                        loadAllWatchedMovies = traktWatchedMovieKeys == null
                    )
                    if (watchlistDirty) {
                        localSaved.forEachIndexed { index, item ->
                            if (item.imdbId != null || item.tmdbId != null) {
                                withContext(Dispatchers.Main) {
                                    ui.traktStatus = context.getString(
                                        R.string.trakt_uploading_watchlist, index + 1, localSaved.size
                                    )
                                }
                                api.setWatchlistItem(session.accessToken, item, saved = true)
                            }
                        }
                    }
                    pendingActivities = activities
                    remote
                } ?: return@launch
                val localMatchCache = mutableMapOf<String, List<SourceItem<PortalTitle>>>()
                val portalSearchLimiter = Semaphore(3)
                // One prepared lookup per portal and section for this import, built once even
                // when many entries are resolved concurrently; released when the sync ends.
                val syncScope = this
                val catalogueMatchers = mutableMapOf<String, Deferred<CatalogueMatcher>>()
                fun catalogueMatcher(source: SavedPortal, section: PortalSection): Deferred<CatalogueMatcher> =
                    catalogueMatchers.getOrPut("${source.identity}:${section.name}") {
                        syncScope.async(Dispatchers.Default) {
                            CatalogueMatcher(source.catalogs[section]?.titles.orEmpty())
                        }
                    }
                suspend fun localMatches(
                    media: TraktMedia,
                    searchPortalIfMissing: Boolean = false
                ): List<SourceItem<PortalTitle>> {
                    val cacheKey = "${media.section}:${media.imdbId}:${media.tmdbId}:${normalizedLabel(media.name)}:${media.year}"
                    val cached = localMatchCache[cacheKey]
                    if (cached != null && (cached.isNotEmpty() || !searchPortalIfMissing)) return cached
                    val expected = media.asTitle()
                    var matches = cached ?: run {
                        val matchers = enabledSavedSources.map { it to catalogueMatcher(it, media.section).await() }
                        withContext(Dispatchers.Default) {
                            matchers.mapNotNull { (source, matcher) ->
                                // Trakt IDs often exist while portal titles omit external IDs. Exact
                                // title plus release year is still a safe local match; conflicting IDs
                                // remain rejected.
                                matcher.find(expected, requireVerifiedIdentity = false)?.let { SourceItem(source, it) }
                            }
                        }
                    }
                    if (matches.isEmpty() && searchPortalIfMissing) {
                        for (source in enabledSavedSources) {
                            try {
                                val portal = if (source.identity == storedSource?.identity) {
                                    liveSession?.first ?: secondarySessions.getOrPut(source.identity) {
                                        StalkerPortal(source.address, source.mac)
                                    }
                                } else secondarySessions.getOrPut(source.identity) {
                                    StalkerPortal(source.address, source.mac)
                                }
                                val found = searchPortal(
                                    source, portal, media.section,
                                    displayVodTitle(expected), countries, portalSearchLimiter
                                ).titles
                                val candidate = equivalentCatalogueTitle(
                                    expected,
                                    found.filter { it.source.identity == source.identity }.map { it.value },
                                    requireVerifiedIdentity = false
                                )
                                if (candidate != null) matches = matches + SourceItem(source, candidate)
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                // Ein nicht erreichbares Portal darf die übrigen Treffer nicht verwerfen.
                            }
                        }
                    }
                    matches = matches.distinctBy { "${it.source.identity}:${it.value.id ?: it.value.name}" }
                    localMatchCache[cacheKey] = matches
                    return matches
                }
                var matched = 0
                val updatedSeries = seriesActivity.associateByTo(linkedMapOf()) { it.key }
                fun mergeSeriesRecords(records: List<TrackedSeries>): TrackedSeries? {
                    val first = records.firstOrNull() ?: return null
                    val watchedEpisodes = records.flatMapTo(linkedSetOf()) { it.watchedEpisodes }
                    return first.copy(
                        refs = records.flatMap { it.refs }.distinct(),
                        saved = records.any { it.saved },
                        viewed = records.any { it.viewed },
                        localViewed = records.any { it.localViewed },
                        known = records.flatMapTo(linkedSetOf()) { it.known },
                        unseen = records.flatMapTo(linkedSetOf()) { it.unseen } - watchedEpisodes,
                        checkedAt = records.maxOf { it.checkedAt },
                        usedTmdb = records.any { it.usedTmdb },
                        episodeProvider = records.maxByOrNull { it.checkedAt }?.episodeProvider,
                        watchedAt = records.maxOf { it.watchedAt },
                        lastEpisode = records.maxByOrNull { it.watchedAt }?.lastEpisode,
                        nextEpisodeName = records.firstNotNullOfOrNull { it.nextEpisodeName },
                        nextAirDate = records.firstNotNullOfOrNull { it.nextAirDate },
                        nextSeason = records.firstNotNullOfOrNull { it.nextSeason },
                        nextSeasonDate = records.firstNotNullOfOrNull { it.nextSeasonDate },
                        newAt = records.maxOf { it.newAt },
                        watchedEpisodes = watchedEpisodes,
                        isComplete = records.mapNotNull { it.isComplete }.takeIf { it.isNotEmpty() }?.all { it }
                    )
                }
                fun trackedRecordsFor(media: TraktMedia, title: PortalTitle): List<TrackedSeries> {
                    val imdb = title.imdbId ?: media.imdbId
                    val tmdb = title.tmdbId?.toLongOrNull()?.toString() ?: media.tmdbId
                    val name = normalizedLabel(displayVodTitle(title))
                    val year = title.year ?: media.year
                    return updatedSeries.values.filter { item ->
                        val conflicts = imdb != null && item.imdb != null && imdb != item.imdb ||
                            tmdb != null && item.tmdb != null && tmdb != item.tmdb
                        if (conflicts) return@filter false
                        val sameId = imdb != null && imdb == item.imdb || tmdb != null && tmdb == item.tmdb
                        val sameName = normalizedLabel(item.name) == name &&
                            (year != null && item.year == year || year == null && item.year == null)
                        sameId || sameName
                    }
                }
                fun absorbSeries(media: TraktMedia, title: PortalTitle, canonicalKey: String): TrackedSeries? {
                    val matches = trackedRecordsFor(media, title)
                    val merged = mergeSeriesRecords(matches) ?: return null
                    matches.forEach { updatedSeries.remove(it.key) }
                    return merged.copy(
                        key = canonicalKey,
                        name = displayVodTitle(title),
                        year = title.year ?: media.year ?: merged.year,
                        imdb = title.imdbId ?: media.imdbId ?: merged.imdb,
                        tmdb = title.tmdbId ?: media.tmdbId ?: merged.tmdb
                    )
                }
                val completedEpisodes = linkedSetOf<String>()
                val updatedWatchlist = watchlist.toMutableSet()
                val updatedMovieBookmarks = movieBookmarks.associateByTo(linkedMapOf()) { it.progressKey }
                val updatedMovieHistory = movieHistory.associateByTo(linkedMapOf()) { it.progressKey }
                val importedProgress = linkedMapOf<String, Float>()
                suspend fun reportImportProgress(label: String, index: Int, total: Int) {
                    if ((index + 1) % 100 == 0 || index + 1 == total) {
                        ui.traktStatus = "$label: ${index + 1} / $total"
                        yield()
                    }
                }
                // Resolve all watchlist entries concurrently; portal searches stay limited by
                // portalSearchLimiter, the loop below then reads the cached matches.
                coroutineScope {
                    library.watchlist.map { media -> async { localMatches(media, searchPortalIfMissing = true) } }.awaitAll()
                }
                for ((index, media) in library.watchlist.withIndex()) {
                    val entries = localMatches(media, searchPortalIfMissing = true)
                    matched++
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    val key = entries.firstOrNull()?.let { importedTitleKey(media.section, title) } ?: traktLibraryKey(media)
                    updatedWatchlist += key
                    if (media.section == PortalSection.MOVIES) {
                        val bookmark = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year, title.imdbId ?: media.imdbId,
                            title.tmdbId ?: media.tmdbId,
                            entries.mapNotNull { item -> item.value.id?.let { SeriesPortalRef(item.source.identity, it) } }.distinct(),
                            key, 0L
                        )
                        updatedMovieBookmarks[key] = bookmark
                    } else {
                        val keyForSeries = if (entries.isNotEmpty()) importedSeriesKey(title) else traktLibraryKey(media)
                        val existing = absorbSeries(media, title, keyForSeries)
                        val refs = entries.mapNotNull { item ->
                            item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                        }.distinct()
                        val tracked = (existing ?: TrackedSeries(
                            keyForSeries, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(saved = true, refs = (existing?.refs.orEmpty() + refs).distinct())
                        updatedSeries[keyForSeries] = tracked
                    }
                    reportImportProgress(context.getString(R.string.trakt_import_watchlist), index, library.watchlist.size)
                }
                for ((index, media) in library.watched.withIndex()) {
                    val entries = localMatches(media, searchPortalIfMissing = true)
                    matched++
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    if (media.section == PortalSection.MOVIES) {
                        val key = entries.firstOrNull()?.let { importedTitleKey(PortalSection.MOVIES, title) } ?: traktLibraryKey(media)
                        val watched = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year, title.imdbId ?: media.imdbId,
                            title.tmdbId ?: media.tmdbId,
                            entries.mapNotNull { it.value.id?.let { id -> SeriesPortalRef(it.source.identity, id) } }.distinct(),
                            key, media.watchedAtMillis ?: System.currentTimeMillis()
                        )
                        updatedMovieHistory[key] = watched
                    } else {
                        val key = if (entries.isNotEmpty()) importedSeriesKey(title) else traktLibraryKey(media)
                        val existing = absorbSeries(media, title, key)
                        val refs = entries.mapNotNull { it.value.id?.let { id -> SeriesPortalRef(it.source.identity, id) } }.distinct()
                        val tracked = (existing ?: TrackedSeries(
                            key, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(viewed = true, watchedAt = media.watchedAtMillis ?: System.currentTimeMillis(), refs = (existing?.refs.orEmpty() + refs).distinct())
                        updatedSeries[key] = tracked
                    }
                    reportImportProgress(context.getString(R.string.trakt_import_watched), index, library.watched.size)
                }
                val sortedWatchedEpisodes = library.watchedEpisodes.sortedBy { it.watchedAtMillis ?: 0L }
                if (sortedWatchedEpisodes.isNotEmpty()) {
                    ui.traktStatus = context.getString(R.string.trakt_matching_saved_episodes)
                }
                val watchedEpisodeGroups = sortedWatchedEpisodes.groupBy { media ->
                    "${media.traktId ?: media.imdbId ?: media.tmdbId ?: normalizedLabel(media.name)}:${media.year ?: 0}"
                }.values
                var processedWatchedEpisodes = 0
                for (episodes in watchedEpisodeGroups) {
                    val media = episodes.first()
                    // A full Trakt watched snapshot repeats all episodes when even one
                    // play changes. Reuse an already linked local series record first;
                    // otherwise this loop would issue a portal search for every known
                    // Trakt show on every sync.
                    val cachedRecords = trackedRecordsFor(media, media.asTitle())
                    val cachedSeries = mergeSeriesRecords(cachedRecords)
                    val episodesToApply = episodes.filter { episode ->
                        val key = "${episode.seasonNumber}:${episode.episodeNumber}"
                        key !in cachedSeries?.watchedEpisodes.orEmpty() ||
                            (episode.watchedAtMillis ?: 0L) > (cachedSeries?.watchedAt ?: 0L)
                    }
                    if (episodesToApply.isEmpty()) {
                        processedWatchedEpisodes += episodes.size
                        reportImportProgress(context.getString(R.string.trakt_import_check_episodes), processedWatchedEpisodes - 1, sortedWatchedEpisodes.size)
                        continue
                    }
                    val entries = if (cachedSeries == null) {
                        localMatches(media)
                    } else emptyList()
                    val title = entries.firstOrNull()?.value ?: cachedSeries?.let { record ->
                        PortalTitle(record.name, year = record.year, imdbId = record.imdb, tmdbId = record.tmdb)
                    } ?: media.asTitle()
                    val trackedKey = cachedSeries?.key ?: if (entries.isNotEmpty()) {
                        importedSeriesKey(title)
                    } else traktLibraryKey(media)
                    val old = if (cachedSeries == null) {
                        absorbSeries(media, title, trackedKey)
                    } else {
                        cachedRecords.forEach { updatedSeries.remove(it.key) }
                        cachedSeries.copy(key = trackedKey)
                    }
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    val watchedKeys = linkedSetOf<String>()
                    var latestResume: EpisodeResume? = null
                    var latestWatchedAt = old?.watchedAt ?: 0L
                    for (episode in episodesToApply) {
                        val seasonNumber = episode.seasonNumber ?: continue
                        val episodeNumber = episode.episodeNumber ?: continue
                        val episodeKeys = buildList {
                            val canonical = titleIdentity(title)
                            if (canonical != null) add("SERIES:catalogue:$canonical:$seasonNumber:$episodeNumber")
                            entries.forEach { item ->
                                val variantCanonical = titleIdentity(item.value)
                                val scope = variantCanonical?.let { "catalogue" } ?: item.source.identity
                                val seriesId = variantCanonical ?: item.value.id ?: item.value.name
                                add("SERIES:$scope:$seriesId:$seasonNumber:$episodeNumber")
                            }
                            if (isEmpty() && title.id != null) {
                                add("SERIES:trakt:${title.id}:$seasonNumber:$episodeNumber")
                            }
                        }
                        .distinct()
                        val watchedKey = "$seasonNumber:$episodeNumber"
                        watchedKeys += watchedKey
                        val watchedAt = episode.watchedAtMillis ?: 0L
                        completedEpisodes += episodeKeys.filter { key ->
                            val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                            if (progressPreferences?.contains("manual_watched:$key") == true) {
                                localUpdated > 0L && watchedAt > localUpdated
                            } else localUpdated == 0L || watchedAt >= localUpdated
                        }
                        if (watchedAt > latestWatchedAt || old?.lastEpisode == null && latestResume == null) {
                            latestWatchedAt = maxOf(latestWatchedAt, watchedAt)
                            latestResume = EpisodeResume(
                                "$seasonNumber:$episodeNumber", seasonNumber, episodeNumber.toString(), null,
                                episodeKeys.firstOrNull()
                                    ?: "SERIES:catalogue:${titleIdentity(title) ?: title.name}:$seasonNumber:$episodeNumber"
                            )
                        }
                    }
                    if (watchedKeys.isEmpty()) continue
                    val tracked = (old ?: TrackedSeries(
                        trackedKey, displayVodTitle(title), refs, title.year ?: media.year,
                        title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                    )).copy(
                        refs = (old?.refs.orEmpty() + refs).distinct(), viewed = true,
                        watchedAt = maxOf(old?.watchedAt ?: 0L, latestWatchedAt),
                        lastEpisode = latestResume ?: old?.lastEpisode,
                        known = old?.known.orEmpty() + watchedKeys,
                        unseen = old?.unseen.orEmpty() - watchedKeys,
                        watchedEpisodes = old?.watchedEpisodes.orEmpty() + watchedKeys
                    )
                    updatedSeries[trackedKey] = tracked
                    matched += watchedKeys.size
                    processedWatchedEpisodes += episodes.size
                    reportImportProgress(context.getString(R.string.trakt_import_mark_episodes), processedWatchedEpisodes - 1, sortedWatchedEpisodes.size)
                }
                for ((index, media) in library.playback.withIndex()) {
                    val percent = media.progressPercent?.takeIf { it > 0f } ?: continue
                    val entries = localMatches(media)
                    val match = entries.firstOrNull()
                    val title = match?.value ?: media.asTitle()
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    if (media.section == PortalSection.MOVIES) {
                        val identity = titleIdentity(title)
                        val scope = if (identity != null) "catalogue" else match?.source?.identity ?: "trakt"
                        val id = identity ?: title.id ?: media.traktId?.toString() ?: title.name
                        val key = "MOVIES:$scope::$id"
                        val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                        if ((progressPreferences?.getLong(key, 0L) ?: 0L) <= 1L ||
                            (media.watchedAtMillis ?: 0L) >= localUpdated
                        ) importedProgress[key] = percent
                        val entry = WatchedMovie(
                            displayVodTitle(title), title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId,
                            refs, key, media.watchedAtMillis ?: System.currentTimeMillis()
                        )
                        updatedMovieHistory[key] = entry
                    } else {
                        val canonical = titleIdentity(title)
                        val scope = if (canonical != null) "catalogue" else match?.source?.identity ?: "trakt"
                        val seriesId = canonical ?: title.id ?: media.traktId?.toString() ?: title.name
                        val seasonNumber = media.seasonNumber ?: continue
                        val episodeNumber = media.episodeNumber ?: continue
                        val key = "SERIES:$scope:$seriesId:$seasonNumber:$episodeNumber"
                        val resume = EpisodeResume(
                            "$seasonNumber:$episodeNumber", seasonNumber, episodeNumber.toString(), null, key
                        )
                        val localUpdated = progressPreferences?.getLong("progress_updated:$key", 0L) ?: 0L
                        if ((progressPreferences?.getLong(key, 0L) ?: 0L) <= 1L ||
                            (media.watchedAtMillis ?: 0L) >= localUpdated
                        ) importedProgress[key] = percent
                        val trackedKey = if (match != null) importedSeriesKey(title) else traktLibraryKey(media)
                        val old = absorbSeries(media, title, trackedKey)
                        val tracked = (old ?: TrackedSeries(
                            trackedKey, displayVodTitle(title), refs, title.year ?: media.year,
                            title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                        )).copy(
                            refs = (old?.refs.orEmpty() + refs).distinct(), viewed = true,
                            watchedAt = media.watchedAtMillis ?: System.currentTimeMillis(), lastEpisode = resume
                        )
                        updatedSeries[trackedKey] = tracked
                    }
                    matched++
                    reportImportProgress(context.getString(R.string.trakt_import_playback), index, library.playback.size)
                }
                for ((index, progress) in library.seriesProgress.withIndex()) {
                    val media = progress.show
                    val entries = localMatches(media)
                    val title = entries.firstOrNull()?.value ?: media.asTitle()
                    val key = entries.firstOrNull()?.let { importedSeriesKey(title) } ?: traktLibraryKey(media)
                    val old = absorbSeries(media, title, key)
                    val refs = entries.mapNotNull { item ->
                        item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
                    }.distinct()
                    updatedSeries[key] = (old ?: TrackedSeries(
                        key, displayVodTitle(title), refs, title.year ?: media.year,
                        title.imdbId ?: media.imdbId, title.tmdbId ?: media.tmdbId
                    )).copy(
                        refs = (old?.refs.orEmpty() + refs).distinct(),
                        viewed = true,
                        watchedAt = maxOf(old?.watchedAt ?: 0L, media.watchedAtMillis ?: 0L),
                        isComplete = progress.isComplete
                    )
                    reportImportProgress(context.getString(R.string.trakt_import_series_progress), index, library.seriesProgress.size)
                }
                watchlist = updatedWatchlist
                movieBookmarks = updatedMovieBookmarks.values.toList()
                movieHistory = updatedMovieHistory.values.sortedByDescending { it.watchedAt }.take(200)
                library.watchedMovieKeys?.let { keys ->
                    traktWatchedMovieKeys = keys
                    progressPreferences?.edit { putStringSet("trakt_watched_movie_keys", keys) }
                }
                val remoteSeriesKeys = library.seriesHistorySnapshot?.let { snapshot ->
                    ui.traktStatus = context.getString(R.string.trakt_reconcile_series_history)
                    withContext(Dispatchers.Default) {
                        snapshot.flatMapTo(hashSetOf()) { media ->
                            trackedRecordsFor(media, media.asTitle()).map { it.key }
                        }
                    }
                }
                seriesActivity = updatedSeries.values.map { record ->
                    if (remoteSeriesKeys == null || !record.viewed) record
                    else record.copy(hiddenFromHistory = !record.localViewed && record.key !in remoteSeriesKeys)
                }
                progressPreferences?.edit {
                    completedEpisodes.forEach { key ->
                        putBoolean("completed:$key", true).remove(key)
                            .remove("manual_watched:$key").remove("trakt_percent:$key")
                    }
                    importedProgress.forEach { (key, percent) ->
                        if (key !in completedEpisodes && progressPreferences.contains("manual_watched:$key") != true) {
                            putFloat("trakt_percent:$key", percent).putLong(key, 1L)
                                .remove("completed:$key")
                        }
                    }
                }
                preferences?.edit { putStringSet(userDataKey("watchlist"), watchlist) }
                progressPreferences?.edit {
                    putString("movie_bookmarks", encodeMovieHistory(movieBookmarks))
                    putString("movie_history", encodeMovieHistory(movieHistory))
                    putString("series_activity", encodeSeriesActivity(seriesActivity))
                }
                pendingActivities?.let { activities ->
                    traktSession?.username?.lowercase()?.let { username ->
                        preferences?.edit {
                            putString("trakt_last_activities:$username", activities)
                            putBoolean("trakt_watchlist_dirty:$username", false)
                            if (remoteSeriesKeys != null) putBoolean("trakt_series_history_reconciled:$username", true)
                        }
                    }
                }
                ui.traktStatus = context.getString(R.string.trakt_sync_complete, matched)
                ui.traktPlaybackNotice = context.getString(R.string.trakt_sync_complete, matched)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppDiagnostics.record(context, "trakt_library_sync_failed")
                ui.traktStatus = error.message ?: context.getString(R.string.trakt_sync_failed)
            } finally {
                ui.traktBusy = false
            }
        }
    }

    LaunchedEffect(ui.traktSyncRequested, ui.traktBusy, ui.sendingWatchStatus, traktSession?.username, restoring) {
        if (ui.traktSyncRequested && !ui.traktBusy && !ui.sendingWatchStatus && traktSession != null && !restoring) {
            // Let the cached guide and last channel render before remote account sync begins.
            delay(5_000)
            ui.traktSyncRequested = false
            syncTrakt()
        }
    }

    fun saveEpgSources(sources: List<String>) {
        if (ui.epgSourceSaving || storage == null) return
        ui.epgSourceSaving = true
        ui.epgSourceWriteFailed = false
        scope.launch {
            try {
                storage.writeEpgSources(sources)
                xmlTvSources = sources
                epgSourceDraft = ""
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ui.epgSourceWriteFailed = true
            } finally {
                ui.epgSourceSaving = false
            }
        }
    }

    LaunchedEffect(storage) {
        try {
            xmlTvSources = storage?.readEpgSources().orEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.epgSourceWriteFailed = true
        } finally {
            xmlTvSourcesLoaded = true
        }
    }

    fun updateMetadataConnection(token: String?) {
        if (ui.metadataBusy) return
        ui.metadataBusy = true
        ui.metadataError = false
        scope.launch {
            try {
                val client = token?.let(::TmdbClient)
                client?.validate()
                storage?.writeMetadataToken(token.orEmpty())
                metadataClient = client
                ui.metadataSettingsOpen = false
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ui.metadataError = true
            } finally {
                ui.metadataBusy = false
            }
        }
    }
    val fallbackGroup = stringResource(R.string.uncategorized_channels)
    val uncategorized = stringResource(R.string.uncategorized_channels)
    val categoryNames = LiveCategory.entries.associateWith { stringResource(it.label) }
    val displayChannels = remember(combine, mergePortalChannels) { mutableMapOf<StalkerChannel, StalkerChannel>() }
    var liveGroups: Map<StalkerChannel, LiveChannelGroup> by remember {
        mutableStateOf(emptyMap())
    }
    LaunchedEffect(sourcesForBrowsing, combine, mergePortalChannels, countries) {
        // Large portals can contain thousands of channels. Grouping and normalizing
        // them on Compose's main dispatcher made cold start and remote navigation
        // feel frozen even though the source data had already been restored locally.
        ui.liveGroupsLoading = true
        // Start and end are both logged: a start without an end means the work was restarted.
        AppDiagnostics.record(context, "live_groups_start ch:${sourcesForBrowsing.sumOf { it.channels.size }}")
        try {
            val started = SystemClock.elapsedRealtime()
            val descriptionFile = java.io.File(context.cacheDir, "channel-descriptions")
            // Only a change of how channel names are read drops the saved file, not every app update
            // (that cost ~45 s on the Fire TV Stick after each update).
            val descriptionVersion = CHANNEL_DESCRIPTION_RULES
            val groups = withContext(Dispatchers.Default) {
                loadChannelDescriptions(descriptionFile, descriptionVersion)
                buildLiveChannelGroups(sourcesForBrowsing, combine, mergePortalChannels, countries)
            }
            // Newly parsed channel names are kept for the next start.
            if (sourcesForBrowsing.isNotEmpty()) launch(Dispatchers.IO) {
                saveChannelDescriptions(descriptionFile, descriptionVersion)
            }
            AppDiagnostics.record(
                context,
                "live_groups ms:${SystemClock.elapsedRealtime() - started} " +
                    "ch:${sourcesForBrowsing.sumOf { it.channels.size }} g:${groups.size}"
            )
            val originals = groups.map { it.variants.first().item.value }.toSet()
            displayChannels.keys.retainAll(originals)
            liveGroups = groups.associateBy { group ->
                val original = group.variants.first().item.value
                if (!combine && !mergePortalChannels) original else displayChannels.getOrPut(original) {
                    StalkerChannel(
                        group.description.name, original.command, original.genreId, original.genreName,
                        original.id, original.logo, original.direct, original.epgIdentity, original.number
                    )
                }
            }
        } finally {
            ui.liveGroupsLoading = false
            ui.liveGroupsReady = true
        }
    }
    val liveVariants = remember(liveGroups) {
        liveGroups.mapValues { (_, group) -> group.variants.map { it.item } }
    }
    val allLiveCategories = remember(liveGroups, categoryNames, combine, uncategorized) {
        liveGroups.keys.groupBy { channel ->
            val group = liveGroups.getValue(channel)
            if (combine) categoryNames.getValue(group.description.category) else {
                val first = group.variants.first().item
                first.value.genreName?.takeIf(String::isNotBlank)
                    ?: first.source.genres[first.value.genreId] ?: uncategorized
            }
        }
    }
    val liveCategories = remember(allLiveCategories, liveGroups, countries) {
        allLiveCategories.mapValues { (_, items) ->
            if (countries.isEmpty()) items else items.filter {
                liveGroups.getValue(it).description.country in countries
            }
        }.filterValues { it.isNotEmpty() }
    }
    // Read through this holder where a callback may outlive the composition that created it
    // (the fullscreen player keeps its key handler): it always sees the current channel list.
    val currentLiveCategories = rememberUpdatedState(liveCategories)
    val currentYear = remember { java.time.Year.now().value }
    val vodCategoryNames = VodCategory.entries.associate {
        val label = when {
            metadataClient == null && it == VodCategory.POPULAR -> stringResource(R.string.portal_popular)
            metadataClient == null && it == VodCategory.CINEMA -> stringResource(R.string.portal_cinema)
            else -> when (it) {
            VodCategory.CINEMA_THIS_YEAR -> stringResource(it.label, currentYear)
            VodCategory.CINEMA_LAST_YEAR -> stringResource(it.label, currentYear - 1)
            VodCategory.THIS_YEAR -> stringResource(it.label, currentYear)
            else -> stringResource(it.label)
            }
        }
        it.id to label
    }
    val vodGroups by produceState(
        initialValue = emptyMap<PortalSection, Map<String, List<SourceItem<PortalGroup>>>>(),
        sourcesForBrowsing, combine, fallbackGroup, countries, metadataClient
    ) {
        val started = SystemClock.elapsedRealtime()
        value = withContext(Dispatchers.Default) {
            listOf(PortalSection.MOVIES, PortalSection.SERIES).associateWith { section ->
            val entries = sourcesForBrowsing.flatMap { source ->
                source.catalogs[section]?.availableGroups(fallbackGroup).orEmpty().map { SourceItem(source, it) }
            }.filter { item ->
                // The country selection applies to films and series as well. Merged genres
                // must not take in provider categories marked for other countries
                // (for example "NL | Serien"); unmarked categories stay available.
                !combine || countries.isEmpty() ||
                    categoryCountry(item.value.name, item.value.country).let { it == UNKNOWN_COUNTRY || it in countries }
            }
            if (combine) {
                val groups = entries.groupBy { vodCategory(it.value).id }.toMutableMap()
                val all = if (countries.isEmpty()) sourcesForBrowsing.filter { section in it.catalogs }.map {
                    SourceItem(it, PortalGroup(ALL_CATALOG_ITEMS, fallbackGroup))
                } else entries
                groups[VodCategory.ALL.id] = all
                groups[VodCategory.THIS_YEAR.id] = all
                val directPopular = groups[VodCategory.POPULAR.id].orEmpty()
                groups[VodCategory.POPULAR.id] = (directPopular + all).distinct()
                if (section == PortalSection.MOVIES) {
                    val cinema = groups[VodCategory.CINEMA.id].orEmpty()
                    groups[VodCategory.CINEMA_THIS_YEAR.id] = cinema
                    groups[VodCategory.CINEMA_LAST_YEAR.id] = cinema
                }
                for (service in VodCategory.entries.filter { it.service }) {
                    val taggedSources = sourcesForBrowsing.filter { source ->
                        source.catalogs[section]?.titles.orEmpty().any {
                            serviceCategory(it.service.orEmpty()) == service
                        }
                    }.map { it.identity }.toSet()
                    val targets = groups[service.id].orEmpty()
                    if (taggedSources.isNotEmpty()) {
                        groups[service.id] = (targets + all.filter { it.source.identity in taggedSources }).distinct()
                    }
                }
                if (sourcesForBrowsing.isNotEmpty()) {
                    for (category in VodCategory.entries.filter { it.usesTmdb(section) }) {
                        if (category.requiresTmdb() || metadataClient != null) {
                            if (groups[category.id].isNullOrEmpty()) groups[category.id] = all
                        }
                    }
                }
                groups
            } else entries.associate { it.value.id to listOf(it) }
            }
        }
        if (sourcesForBrowsing.isNotEmpty()) AppDiagnostics.record(context, "vod_groups ms:${SystemClock.elapsedRealtime() - started}")
    }
    fun sessionFor(source: SavedPortal): StalkerPortal =
        if (source.identity == storedSource?.identity) {
            liveSession?.first ?: secondarySessions.getOrPut(source.identity) {
                StalkerPortal(source.address, source.mac)
            }
        } else secondarySessions.getOrPut(source.identity) { StalkerPortal(source.address, source.mac) }

    fun portalName(source: SavedPortal): String {
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        val host = runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
        return resources.getString(R.string.saved_portal_label, index + 1, host)
    }

    fun playbackSourceLabel(source: SavedPortal, portal: StalkerPortal): String {
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        return if (index < 0) portal.sourceLabel else
            resources.getString(R.string.saved_portal_label, index + 1, portal.sourceLabel)
    }

    fun programmeKey(channel: StalkerChannel): String? {
        val item = liveVariants[channel]?.firstOrNull { it.value.id != null } ?: return null
        return item.value.id?.let { "${item.source.identity}:$it" }
    }

    val pinnedProgrammeKeys = remember(liveVariants) {
        liveVariants.keys.filter { channel ->
            val item = liveVariants.getValue(channel).first()
            item.value.id in item.source.epgChannelIds
        }.mapNotNull(::programmeKey).toSet()
    }

    fun groupOwners(section: PortalSection, id: String): List<SourceItem<PortalGroup>> =
        vodGroups[section]?.get(id).orEmpty().filter {
            !combine || countries.isEmpty() || categoryCountry(it.value.name, it.value.country) in countries
        }

    fun browsingGroups(section: PortalSection): List<PortalGroup> =
        vodGroups[section].orEmpty().mapNotNull { (id, entries) ->
            val category = VodCategory.entries.firstOrNull { it.id == id }
            if (groupOwners(section, id).isEmpty() && category?.requiresTmdb() != true) null
            else if (combine) PortalGroup(id, vodCategoryNames.getValue(id))
            else entries.first().value
        }

    // Country detection parses every channel name; with large portals this took seconds and
    // previously ran on the main thread during composition, which caused input ANRs right
    // after a fast start. It now runs in the background and the list fills in when ready.
    val knownLiveCountries by produceState(emptySet<String>(), sourcesForBrowsing) {
        value = withContext(Dispatchers.Default) {
            sourcesForBrowsing.flatMap { source ->
                source.channels.map { channel ->
                    categoryCountry(channel.name, channel.genreName ?: source.genres[channel.genreId])
                }
            }.toSet()
        }
    }
    val knownCountries by produceState(emptySet<String>(), knownLiveCountries, vodGroups) {
        value = withContext(Dispatchers.Default) {
            (knownLiveCountries +
                vodGroups.values.flatMap { groups ->
                    groups.filterKeys { id ->
                        VodCategory.entries.none { it.id == id && it in listOf(VodCategory.ALL, VodCategory.THIS_YEAR) }
                    }.values.flatten().map {
                        categoryCountry(it.value.name, it.value.country)
                    }
                }).toSet()
        }
    }

    fun saveCountries(selection: Set<String>) {
        countries = selection
        preferences?.edit { putString("countryOrder", countries.joinToString(",")) }
    }

    fun setCategoryVisibility(section: PortalSection, ids: Set<String>, hidden: Boolean) {
        val keys = ids.map { categoryKey(section, it) }.toSet()
        hiddenCategories = if (hidden) hiddenCategories + keys else hiddenCategories - keys
        preferences?.edit { putStringSet("hidden", hiddenCategories) }
    }

    fun countryPriority(name: String, country: String? = null): Int =
        countries.indexOf(categoryCountry(name, country)).takeIf { it >= 0 } ?: Int.MAX_VALUE

    fun defaultVodCategoryOrder(section: PortalSection): List<PortalGroup> =
        browsingGroups(section).sortedWith(compareBy(
            { if (combine) vodCategoryNames.keys.indexOf(it.id) else countryPriority(it.name, it.country) },
            { it.name }
        ))

    fun titleKey(section: PortalSection, title: PortalTitle): String =
        "${catalogSession?.first?.sourceLabel.orEmpty()}:${section.name}:${title.id ?: title.name}"

    fun seriesKey(title: PortalTitle): String =
        titleIdentity(title) ?: titleKey(PortalSection.SERIES, title)
    fun trackedSeries(title: PortalTitle): TrackedSeries? {
        val titleImdb = title.imdbId?.takeIf { it.startsWith("tt") }
        val titleTmdb = title.tmdbId?.toLongOrNull()?.takeIf { it > 0 }?.toString()
        val normalizedTitle = normalizedLabel(displayVodTitle(title))
        val compatible = seriesActivity.filter { entry ->
            val conflictingIds = titleImdb != null && entry.imdb != null && titleImdb != entry.imdb ||
                titleTmdb != null && entry.tmdb != null && titleTmdb != entry.tmdb
            if (conflictingIds) return@filter false
            val sameId = titleImdb != null && entry.imdb == titleImdb ||
                titleTmdb != null && entry.tmdb == titleTmdb
            val sameNameAndYear = normalizedLabel(entry.name) == normalizedTitle &&
                (title.year == null || entry.year == null || title.year == entry.year)
            sameId || sameNameAndYear
        }
        val exact = compatible.filter { it.key == seriesKey(title) }
        val identified = compatible.filter { entry ->
            titleImdb != null && entry.imdb == titleImdb || titleTmdb != null && entry.tmdb == titleTmdb
        }
        val named = compatible.filter { entry ->
            normalizedLabel(entry.name) == normalizedTitle &&
                (title.year != null && entry.year == title.year || title.year == null && entry.year == null)
        }
        val matches = when {
            exact.isNotEmpty() -> (exact + identified).distinctBy { it.key }
            identified.isNotEmpty() -> identified
            else -> {
                if (named.size != 1 || named.mapNotNull { it.imdb }.distinct().size > 1 ||
                    named.mapNotNull { it.tmdb }.distinct().size > 1
                ) return null
                named
            }
        }
        val first = matches.firstOrNull() ?: return null
        if (matches.size == 1) return first
        val watched = matches.flatMapTo(linkedSetOf()) { it.watchedEpisodes }
        return first.copy(
            refs = matches.flatMap { it.refs }.distinct(),
            saved = matches.any { it.saved },
            viewed = matches.any { it.viewed },
            known = matches.flatMapTo(linkedSetOf()) { it.known },
            unseen = matches.flatMapTo(linkedSetOf()) { it.unseen } - watched,
            watchedAt = matches.maxOf { it.watchedAt },
            lastEpisode = matches.maxByOrNull { it.watchedAt }?.lastEpisode,
            watchedEpisodes = watched
        )
    }
    fun isTitleSaved(section: PortalSection, title: PortalTitle): Boolean =
        titleKey(section, title) in watchlist ||
            (section == PortalSection.SERIES && trackedSeries(title)?.saved == true)


    fun saveSeriesActivity(entries: List<TrackedSeries>) {
        seriesActivity = entries
        progressPreferences?.edit { putString("series_activity", encodeSeriesActivity(entries)) }
    }

    fun updateTrackedSeries(title: PortalTitle, saved: Boolean? = null, watchedEpisode: EpisodeResume? = null) {
        val key = seriesKey(title)
        val existing = seriesActivity.firstOrNull { it.key == key }
        val variants = searchedVariants[title] ?: groupVariants[title] ?: listOfNotNull(
            storedSource?.let { SourceItem(it, title) }
        )
        val refs = variants.mapNotNull { item ->
            item.value.id?.let { SeriesPortalRef(item.source.identity, it) }
        }
        val initial = existing ?: TrackedSeries(
            key, displayVodTitle(title), refs, vodReleaseYear(title), title.imdbId, title.tmdbId
        )
        val updated = initial.copy(
            refs = (initial.refs + refs).distinct(),
            saved = saved ?: initial.saved,
            viewed = initial.viewed || watchedEpisode != null,
            localViewed = initial.localViewed || watchedEpisode != null,
            hiddenFromHistory = initial.hiddenFromHistory && watchedEpisode == null,
            known = if (watchedEpisode != null && initial.checkedAt > 0) {
                initial.known + watchedEpisode.episodeKey
            } else initial.known,
            unseen = if (watchedEpisode == null) initial.unseen else initial.unseen - watchedEpisode.episodeKey,
            watchedEpisodes = if (watchedEpisode == null) initial.watchedEpisodes
                else initial.watchedEpisodes + watchedEpisode.episodeKey,
            watchedAt = if (watchedEpisode == null) initial.watchedAt else System.currentTimeMillis(),
            lastEpisode = watchedEpisode ?: initial.lastEpisode,
            isComplete = if (watchedEpisode == null) initial.isComplete else false
        )
        val remaining = seriesActivity.filterNot { it.key == key }
        saveSeriesActivity(if (updated.saved || updated.viewed) remaining + updated else remaining)
    }

    LaunchedEffect(preferences, savedSources.isNotEmpty()) {
        // Favourites were merged into the watchlist. Earlier favourites move over once (series
        // also into the series watchlist) and are uploaded to Trakt with the next sync.
        val earlier = favorites
        if (earlier.isEmpty() || savedSources.isEmpty()) return@LaunchedEffect
        watchlist = watchlist + earlier
        val seriesIds = earlier.filter { ":${PortalSection.SERIES.name}:" in it }
            .map { it.substringAfter(":${PortalSection.SERIES.name}:") }.toSet()
        savedSources.flatMap { it.catalogs[PortalSection.SERIES]?.titles.orEmpty() }
            .filter { (it.id ?: it.name) in seriesIds }
            .distinctBy { it.id ?: it.name }
            .forEach { updateTrackedSeries(it, saved = true) }
        favorites = emptySet()
        preferences?.edit { putStringSet(userDataKey("watchlist"), watchlist); remove(userDataKey("favorites")) }
        preferences?.all?.keys?.filter { it.startsWith("trakt_watchlist_dirty:") }?.forEach { key ->
            preferences.edit { putBoolean(key, true) }
        }
    }

    fun observeEpisodeProvider(
        key: String,
        episodes: Set<String>,
        provider: String,
        upcoming: TraktUpcomingEpisode? = null,
        season: TraktUpcomingEpisode? = null
    ) {
        saveSeriesActivity(seriesActivity.map {
            if (it.key != key) it
            else if ((it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) == provider) {
                it.observe(episodes, System.currentTimeMillis()).copy(
                    nextEpisodeName = upcoming?.name,
                    nextAirDate = upcoming?.airDate,
                    nextSeason = season?.season,
                    nextSeasonDate = season?.airDate
                ).also { updated ->
                    val added = updated.unseen.size - it.unseen.size
                    if (added > 0 && (it.viewed || it.saved) && !it.hiddenFromHistory) {
                        val entry = context.getString(R.string.new_episodes_entry, it.name, added)
                        newEpisodesNotice = newEpisodesNotice?.let { pending -> "$pending, $entry" }
                            ?: context.getString(R.string.new_episodes_available, entry)
                    }
                }
            } else it.copy(
                episodeProvider = provider, known = episodes, checkedAt = System.currentTimeMillis(),
                nextEpisodeName = upcoming?.name, nextAirDate = upcoming?.airDate,
                nextSeason = season?.season, nextSeasonDate = season?.airDate
            )
        })
    }

    val trackedSources = savedSources.filter { it.enabled }.map { it.identity }
    val trackedTitles = seriesActivity.map { it.key to it.refs }
    /**
     * Episode keys ("season:episode") the portals linked to [entry] offer, or null when none could
     * be asked. Stops once every episode reported as new was found.
     */
    suspend fun portalEpisodeKeys(entry: TrackedSeries): Set<String>? {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val found = linkedSetOf<String>()
        var asked = false
        for (ref in entry.refs.take(3)) {
            val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: continue
            val title = source.catalogs[PortalSection.SERIES]?.titles?.firstOrNull { it.id == ref.portalTitleId }
                ?: reference.copy(id = ref.portalTitleId)
            try {
                val portal = sessionFor(source)
                portal.loadSeasons(title, portal.openSession()).forEach { season ->
                    season.episodes.forEach { found += seriesEpisodeKey(season, it) }
                }
                asked = true
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                continue
            }
            if (found.containsAll(entry.unseen)) break
        }
        return found.takeIf { asked }
    }

    val trackedEpisodeChecks = seriesActivity.map { it.key to it.checkedAt }
    LaunchedEffect(metadataClient, traktSession?.username, trackedSources, trackedTitles, trackedEpisodeChecks) {
        val client = metadataClient
        val provider = when {
            traktSession != null -> "trakt"
            client != null -> "tmdb"
            else -> "tvmaze"
        }
        var scanOffset = 0
        while (trackedSources.isNotEmpty() && trackedTitles.isNotEmpty()) {
            if (canPlay()) {
                val now = System.currentTimeMillis()
                val due = seriesActivity.filter {
                    (it.episodeProvider ?: if (it.usedTmdb) "tmdb" else null) != provider ||
                        now - it.checkedAt >= 30 * 60 * 1000L
                }
                // A Trakt library can contain many shows. Process one per minute
                // instead of issuing a burst of requests while the user browses.
                val tracked = due.getOrNull(scanOffset % due.size.coerceAtLeast(1))
                if (due.isNotEmpty()) scanOffset = (scanOffset + 1) % due.size
                if (tracked != null) {
                    try {
                        val title = PortalTitle(
                            tracked.name, year = tracked.year, imdbId = tracked.imdb, tmdbId = tracked.tmdb
                        )
                        // All three deliver the aired episodes, the next episode and the next season start.
                        val feed = when (provider) {
                            "trakt" -> withTraktSession { api, session -> api.episodeFeed(title, session.accessToken) }
                            "tmdb" -> client?.episodeFeed(title)
                            else -> TvmazeClient.episodeFeed(title)
                        }
                        val keys = feed?.airedKeys
                        if (keys != null && seriesActivity.any { it.key == tracked.key }) {
                            val before = seriesActivity.firstOrNull { it.key == tracked.key }?.unseen?.size ?: 0
                            observeEpisodeProvider(tracked.key, keys, provider, feed.upcoming, feed.upcomingSeason)
                            val after = seriesActivity.firstOrNull { it.key == tracked.key }?.unseen?.size ?: 0
                            // Counts only (no series names): shows whether checks run and find something.
                            if (after > before) AppDiagnostics.record(context, "episode_check p:$provider aired:${keys.size} new:${after - before}")
                            // Is a reported new episode already in the portal? ("Weiter schauen" marks it then.)
                            val current = seriesActivity.firstOrNull { it.key == tracked.key }
                            if (current != null && current.portalCheckDue(System.currentTimeMillis())) {
                                val offered = portalEpisodeKeys(current)
                                val checked = System.currentTimeMillis()
                                if (offered != null) saveSeriesActivity(seriesActivity.map {
                                    if (it.key == current.key) it.withPortalEpisodes(offered, checked) else it
                                })
                                AppDiagnostics.record(context, "episode_portal missing:${(current.unseen - current.portalNew).size} " +
                                    (offered?.let { "found:${(current.unseen intersect it).size}" } ?: "failed"))
                            }
                        } else if (keys == null) AppDiagnostics.record(context, "episode_check p:$provider no_list")
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        // Keep the last verified episode list when TMDB is unavailable.
                        AppDiagnostics.record(context, "episode_check p:$provider failed:${error.javaClass.simpleName}")
                    }
                }
            }
            delay(60_000)
        }
    }

    fun toggleSavedTitle(section: PortalSection, title: PortalTitle, favorite: Boolean) {
        val key = titleKey(section, title)
        if (favorite) {
            favorites = if (key in favorites) favorites - key else favorites + key
            preferences?.edit { putStringSet(userDataKey("favorites"), favorites) }
        } else {
            val saved = !isTitleSaved(section, title)
            watchlist = if (saved) watchlist + key else watchlist - key
            preferences?.edit { putStringSet(userDataKey("watchlist"), watchlist) }
            traktSession?.username?.lowercase()?.let { username ->
                preferences?.edit { putBoolean("trakt_watchlist_dirty:$username", true) }
            }
            if (section == PortalSection.SERIES) updateTrackedSeries(title, saved = saved)
            else if (section == PortalSection.MOVIES) {
                val others = movieBookmarks.filterNot { it.progressKey == key }
                val ownerEntries = searchedVariants[title] ?: groupVariants[title] ?: listOfNotNull(
                    storedSource?.let { SourceItem(it, title) }
                )
                movieBookmarks = if (saved) {
                    listOf(WatchedMovie(
                        displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                        ownerEntries.map { SeriesPortalRef(it.source.identity, it.value.id.orEmpty()) }.distinct(),
                        key, System.currentTimeMillis()
                    )) + others
                } else others
                progressPreferences?.edit { putString("movie_bookmarks", encodeMovieHistory(movieBookmarks)) }
            }
        }
    }
    fun visibleCategory(section: PortalSection, id: String, label: String, country: String? = null): Boolean {
        val hidden = categoryKey(section, id) in hiddenCategories
        val kidsFriendly = section != PortalSection.LIVE && (id == VodCategory.FAMILY.id || id == VodCategory.ANIMATION.id)
        if (!parental.allowsCategory(categoryKey(section, id), label, kidsFriendly)) return false
        return (ui.showHiddenCategories || !hidden) &&
            (section == PortalSection.LIVE || combine || countries.isEmpty() || categoryCountry(label, country) in countries ||
                (ui.showHiddenCategories && hidden))
    }

    fun closeGroup() {
        groupJob?.cancel()
        selectedGroup = null
        groupCatalog = null
        ui.metadataNextPages = emptyMap()
        metadataTitles = emptyMap()
        ui.groupUsesTmdb = false
        groupPages = emptyMap()
        groupVariants = emptyMap()
        ui.groupError = false
        ui.groupLoading = false
    }

    fun loadGroup(section: PortalSection, group: PortalGroup, next: Boolean = false) {
        val category = VodCategory.entries.firstOrNull { it.id == group.id }
        val client = metadataClient
        if (category?.usesTmdb(section) == true && client != null) {
            groupJob?.cancel()
            val pages = if (next) ui.metadataNextPages else tmdbShelfRegions(
                category, countries, java.util.Locale.getDefault().country
            ).associateWith { 1 }
            if (pages.isEmpty()) return
            // Opened before in this session: shown at once instead of searching all portals again.
            val shelfKey = listOf(
                section.name, category.id, pages.keys.sorted().joinToString(","), countries.sorted().joinToString(","),
                sourcesForBrowsing.map { it.identity }.sorted().joinToString(",")
            ).joinToString("|")
            if (!next) TmdbShelfCache.get(shelfKey)?.let { cached ->
                groupJob?.cancel()
                selectedGroup = section to group
                metadataTitles = cached.titles
                ui.metadataNextPages = cached.nextPages
                groupVariants = cached.titles.values.toMap()
                groupCatalog = PortalCatalog(groupVariants.keys.toList(), null, 1.takeIf { cached.nextPages.isNotEmpty() })
                ui.groupUsesTmdb = true
                ui.groupError = cached.incomplete
                ui.groupLoading = false
                AppDiagnostics.record(context, "vod_group_tmdb_cached ${section.name} titles:${cached.titles.size}")
                return
            }
            val previous = if (next) metadataTitles else emptyMap()
            ui.metadataNextPages = pages
            metadataTitles = previous
            selectedGroup = section to group
            groupCatalog = null
            groupVariants = previous.values.toMap()
            ui.groupUsesTmdb = true
            ui.groupError = false
            ui.groupLoading = true
            groupJob = scope.launch {
                try {
                    // Only titles found in the portals are shown, so one TMDB page may yield just a
                    // few. Further pages are loaded until enough titles were found or TMDB has no more.
                    var requested = pages
                    var rounds = 0
                    var incomplete = false
                    val started = SystemClock.elapsedRealtime()
                    do {
                        val result = loadTmdbPortalShelf(
                            client, section, category, requested, sourcesForBrowsing, countries, ::sessionFor
                        )
                        metadataTitles = metadataTitles + result.titles
                        groupVariants = metadataTitles.values.toMap()
                        ui.metadataNextPages = result.nextPages
                        incomplete = incomplete || result.incomplete
                        groupCatalog = PortalCatalog(groupVariants.keys.toList(), null, 1.takeIf { ui.metadataNextPages.isNotEmpty() })
                        requested = result.nextPages
                        rounds++
                    } while (requested.isNotEmpty() && metadataTitles.size - previous.size < 24 && rounds < 8)
                    AppDiagnostics.record(context,
                        "vod_group_tmdb ${section.name} ms:${SystemClock.elapsedRealtime() - started} rounds:$rounds titles:${metadataTitles.size}")
                    if (!next) TmdbShelfCache.put(shelfKey, TmdbShelfResult(metadataTitles, ui.metadataNextPages, incomplete))
                    ui.groupError = incomplete
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    ui.groupError = true
                    groupCatalog = PortalCatalog(groupVariants.keys.toList(), null, 1)
                } finally {
                    if (kotlin.coroutines.coroutineContext.isActive) ui.groupLoading = false
                }
            }
            return
        }
        val owners = groupOwners(section, group.id)
        if (owners.isEmpty()) return
        val previous = if (next) groupPages else emptyMap()
        groupJob?.cancel()
        selectedGroup = section to group
        ui.groupUsesTmdb = false
        if (!next) {
            groupCatalog = null
            groupVariants = emptyMap()
        }
        ui.groupError = false
        ui.groupLoading = true
        groupJob = scope.launch {
            val started = SystemClock.elapsedRealtime()
            try {
                val requests = Semaphore(4)
                fun pageKey(item: SourceItem<PortalGroup>) = "${item.source.identity}:${item.value.id}"
                val pages = coroutineScope {
                    owners.map { item ->
                        async {
                            val key = pageKey(item)
                            val before = previous[key]
                            val page = if (next) before?.nextPage else 1
                            if (page == null) return@async key to before
                            try {
                                key to requests.withPermit {
                                    val portal = sessionFor(item.source)
                                    portal.loadCatalog(section, portal.openSession(), page, before, item.value.id)
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                ui.groupError = true
                                key to before
                            }
                        }
                    }.map { it.await() }.mapNotNull { (id, page) -> page?.let { id to it } }.toMap()
                }
                val loadedAt = SystemClock.elapsedRealtime()
                val target = VodCategory.entries.firstOrNull { it.id == group.id }
                // Filtering and merging thousands of titles off the main thread (it made the UI stall).
                val variants = withContext(Dispatchers.Default) {
                val entries = owners.flatMap { item ->
                    val providerCategory = vodCategory(item.value)
                    pages[pageKey(item)]?.titles.orEmpty().filter { title ->
                        when (target) {
                            VodCategory.THIS_YEAR, VodCategory.CINEMA_THIS_YEAR -> vodReleaseYear(title) == currentYear
                            VodCategory.CINEMA_LAST_YEAR -> vodReleaseYear(title) == currentYear - 1
                            VodCategory.POPULAR -> providerCategory == VodCategory.POPULAR || title.popularity != null
                            else -> target?.service != true || providerCategory == target ||
                                serviceCategory(title.service.orEmpty()) == target
                        }
                    }.map { SourceItem(item.source, it) }
                }
                if (combine) {
                    mergeCatalogueTitles(entries.distinct(), requireVerifiedIdentity = section == PortalSection.SERIES)
                } else entries.map(::listOf)
                }
                AppDiagnostics.record(context,
                    "vod_group ${section.name} portals:${owners.size} load_ms:${loadedAt - started} " +
                        "merge_ms:${SystemClock.elapsedRealtime() - loadedAt} titles:${variants.size} next:$next")
                groupPages = pages
                groupVariants = variants.associateBy { it.first().value }
                groupCatalog = PortalCatalog(
                    groupVariants.keys.toList(),
                    if (combine) null else pages.values.singleOrNull()?.total,
                    if (pages.values.any { it.nextPage != null }) 1 else null
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ui.groupError = true
            } finally {
                if (kotlin.coroutines.coroutineContext.isActive) ui.groupLoading = false
            }
        }
    }
    LaunchedEffect(countries) {
        val group = selectedGroup
        if (ui.groupUsesTmdb && group != null) loadGroup(group.first, group.second)
    }
    var epgCache by remember {
        mutableStateOf(emptyMap<String, List<PortalProgramme>>())
    }
    var visibleEpgChannels by remember { mutableStateOf(emptyList<StalkerChannel>()) }
    val epgInFlight = remember { mutableSetOf<String>() }
    val epgRequestLimit = remember { Semaphore(6) }
    val xmlTvRequestLimit = remember { Semaphore(3) }
    var guideCacheLoaded by remember(storage) { mutableStateOf(storage == null) }
    var externalGuides by remember { mutableStateOf(emptyList<XmlTvGuide>()) }
    LaunchedEffect(storage) {
        if (storage == null) return@LaunchedEffect
        val saved = try {
            storage.readGuideCache()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            emptyMap()
        }
        val now = System.currentTimeMillis()
        val recent = saved.filterValues { now - it.fetchedAt in 0..(7 * 86_400_000L) }
        epgCache = recent.mapValues { it.value.programmes } + epgCache
        ui.epgFetchedAt = recent.mapValues { it.value.fetchedAt } + ui.epgFetchedAt
        guideCacheLoaded = true
    }
    LaunchedEffect(storage, guideCacheLoaded) {
        if (storage == null || !guideCacheLoaded) return@LaunchedEffect
        snapshotFlow { ui.epgFetchedAt to epgCache }.collectLatest { (times, entries) ->
            if (times.isEmpty()) return@collectLatest
            delay(900)
            try {
                storage.writeGuideCache(times.mapNotNull { (key, time) ->
                    entries[key]?.let { key to CachedProgramme(time, it) }
                }.toMap())
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A cache write is optional. The in-memory guide remains available.
            }
        }
    }

    fun rawProgrammes(channel: StalkerChannel): List<PortalProgramme>? =
        if (useXmlTv) {
            externalGuides.firstNotNullOfOrNull { guide ->
                liveVariants[channel].orEmpty().firstNotNullOfOrNull { item ->
                    guide.programmes(item.value, channel.name).takeIf { it.isNotEmpty() }
                }
            } ?: emptyList<PortalProgramme>().takeUnless { ui.epgUpdating }
        } else {
            val key = programmeKey(channel)
            if (key == null) emptyList() else epgCache[key]
        }
    val cleanSchedules = remember { HashMap<Any, Pair<List<PortalProgramme>, List<PortalProgramme>>>() }
    // Grid and preview share one cleaned schedule per channel, recomputed only when the raw list changes.
    fun effectiveProgrammes(channel: StalkerChannel): List<PortalProgramme>? {
        val raw = rawProgrammes(channel) ?: return null
        val key: Any = programmeKey(channel) ?: channel
        cleanSchedules[key]?.takeIf { it.first === raw }?.let { return it.second }
        return normalizeSchedule(raw).also { cleanSchedules[key] = raw to it }
    }

    suspend fun loadProviderSchedule(channel: StalkerChannel, force: Boolean = false): Boolean {
        val key = programmeKey(channel) ?: return true
        val variants = liveVariants[channel].orEmpty().filter { it.value.id != null }.distinctBy { it.source.identity to it.value.id }
        if (variants.isEmpty()) return true
        val last = ui.epgFetchedAt[key] ?: -1
        val maxAge = if (epgRefreshHours > 0) epgRefreshHours * 3_600_000L else Long.MAX_VALUE
        if (!guideCacheLoaded) return true
        if (!force && last >= 0 && System.currentTimeMillis() - last in 0 until maxAge) return true
        if (!epgInFlight.add(key)) return true
        return try {
            var entries = emptyList<PortalProgramme>()
            var answered = false
            for (item in variants) {
                val portal = sessionFor(item.source)
                try {
                    entries = epgRequestLimit.withPermit {
                        portal.loadProgramme(
                            item.value, portal.openSession(),
                            periodHours = epgHorizonHours, refreshHours = epgRefreshHours
                        )
                            .guideWindow(System.currentTimeMillis() / 1_000, epgHorizonHours)
                    }
                    answered = true
                    if (entries.isNotEmpty()) break
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    continue
                }
            }
            check(answered)
            // The portal answers from now on: shows that already ran today stay from the last
            // fetch (up to one day back), the rest is replaced. So the guide holds about 48 hours
            // and does not grow.
            val nowSeconds = System.currentTimeMillis() / 1_000
            val firstNew = entries.mapNotNull { it.startSeconds }.minOrNull()
            val earlier = epgCache[key].orEmpty().filter { show ->
                val start = show.startSeconds ?: return@filter false
                start < (firstNew ?: nowSeconds) && (show.endSeconds ?: start + 3_600L) > nowSeconds - GUIDE_PAST_SECONDS
            }
            entries = (earlier + entries).guideWindow(nowSeconds, epgHorizonHours)
            epgCache = epgCache + (key to entries)
            // Nothing came back: asked again after a few minutes, not only after the whole interval.
            val now = System.currentTimeMillis()
            val fetchedAt = if (entries.isEmpty() && maxAge != Long.MAX_VALUE) now - maxAge + INCOMPLETE_EPG_RETRY_MS else now
            if (entries.isNotEmpty() || maxAge != Long.MAX_VALUE) ui.epgFetchedAt = ui.epgFetchedAt + (key to fetchedAt)
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (key !in epgCache) epgCache = epgCache + (key to emptyList())
            false
        } finally {
            epgInFlight.remove(key)
        }
    }

    suspend fun loadProviderSchedules(channels: List<StalkerChannel>, force: Boolean = false): Boolean {
        if (force) {
            channels.asSequence().flatMap { liveVariants[it].orEmpty().asSequence() }
                .distinctBy { it.source.identity }
                .forEach { item -> sessionFor(item.source).invalidateProgrammeCache() }
        }
        val results = coroutineScope {
            channels.distinct().map { channel -> async { loadProviderSchedule(channel, force) } }
                .map { it.await() }
        }
        return results.all { it }
    }

    val pinnedProgramme = programmeChannel?.let(::programmeKey) in pinnedProgrammeKeys
    val programmes = programmeChannel?.let(::effectiveProgrammes)
    LaunchedEffect(programmeChannel, sourcesForBrowsing, useXmlTv, epgHorizonHours, epgRefreshRequest, guideCacheLoaded) {
        val channel = programmeChannel ?: return@LaunchedEffect
        if (useXmlTv) return@LaunchedEffect
        delay(250)
        loadProviderSchedule(channel)
    }
    val hasPortal = storedSource != null
    // Every 15 minutes: what has grown older than the refresh interval (or came back empty) is
    // loaded again. Before, this happened only once the whole interval had passed since the app
    // started, and then for everything at once.
    var epgStaleCheck by remember { mutableIntStateOf(0) }
    val lastEpgReport = remember { arrayOf("") }
    LaunchedEffect(epgRefreshHours, hasPortal) {
        if (epgRefreshHours <= 0 || !hasPortal) return@LaunchedEffect
        while (true) {
            delay(15 * 60_000L)
            if (canPlay()) epgStaleCheck++
        }
    }
    LaunchedEffect(restoring, hasPortal, liveSession, useXmlTv, xmlTvSources, xmlTvSourcesLoaded, guideCacheLoaded,
        epgHorizonHours, epgRefreshRequest, epgRefreshHours, epgStaleCheck) {
        if (restoring || !hasPortal || !xmlTvSourcesLoaded || !guideCacheLoaded) return@LaunchedEffect
        val refresh = epgAtStartup || epgRefreshRequest > 0 || epgStaleCheck > 0
        ui.epgUpdateFailed = false
        ui.epgUpdating = true
        try {
            if (useXmlTv) {
                val sources = xmlTvSources.mapNotNull(::guideSourceUri)
                val maxAge = if (epgRefreshHours > 0) epgRefreshHours * 3_600_000L else Long.MAX_VALUE
                val cached = coroutineScope {
                    sources.map { uri -> async {
                        xmlTvRequestLimit.withPermit {
                            loadXmlTvGuide(context.cacheDir, uri, epgHorizonHours, false, false, maxAge)
                        }
                    } }.awaitAll()
                }
                externalGuides = cached.mapNotNull { it.guide }
                val results = coroutineScope {
                    sources.zip(cached).map { (uri, entry) -> async {
                        if (entry.guide == null || refresh && (epgRefreshRequest > 0 || entry.needsRefresh)) {
                            xmlTvRequestLimit.withPermit {
                                loadXmlTvGuide(context.cacheDir, uri, epgHorizonHours, refresh = true)
                            }
                        } else entry
                    } }.awaitAll()
                }
                externalGuides = results.mapNotNull { it.guide }
                ui.epgUpdateFailed = results.any { it.failed }
            } else if (refresh) {
                val available = liveCategories.entries
                    .filter { visibleCategory(PortalSection.LIVE, it.key, it.key) }.flatMap { it.value }
                val selected = sequenceOf(playing, programmeChannel).filterNotNull()
                val pinned = available.filter { programmeKey(it) in pinnedProgrammeKeys }
                // Refresh only the EPG rows the user is currently looking at, plus the
                // playing/selected and explicitly pinned channels. A cache entry alone
                // must not make thousands of old channels refresh every cycle.
                val refreshTargets = (selected + visibleEpgChannels.asSequence() + pinned.asSequence())
                    .filter { it in available }.distinct().toList()
                val complete = loadProviderSchedules(refreshTargets, force = epgRefreshRequest > 0)
                // Counts only: whether the guide comes back and how much of it is empty.
                val empty = refreshTargets.count { effectiveProgrammes(it).isNullOrEmpty() }
                val report = "epg_refresh rows:${refreshTargets.size} empty:$empty complete:$complete"
                // Only when something changed (rows without a guide would otherwise be noted every 15 minutes).
                if ((!complete || empty > 0) && report != lastEpgReport[0]) AppDiagnostics.record(context, report)
                lastEpgReport[0] = report
                ui.epgUpdateFailed = !complete
            }
        } finally {
            ui.epgUpdating = false
        }
    }

    LaunchedEffect(storage) {
        try {
            storage?.read()?.let { library ->
                savedSources = library.sources
                val saved = library.sources.firstOrNull { it.identity == library.activeId }
                    ?.takeIf { it.enabled } ?: library.sources.firstOrNull { it.enabled }
                if (saved != null) {
                    preferences?.edit { putString("active_portal_cache", saved.identity) }
                    val session = StalkerPortal(saved.address, saved.mac) to null
                    storedSource = saved
                    liveSession = session
                    catalogSession = session
                    address = saved.address
                    mac = saved.mac
                    channels = saved.channels
                    genres = saved.genres
                    catalogs = saved.catalogs
                    epgChannelIds = saved.epgChannelIds
                } else {
                    storedSource = null
                    liveSession = null
                    catalogSession = null
                    channels = emptyList()
                    genres = emptyMap()
                    catalogs = emptyMap()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_library_restore_failed:${error.javaClass.simpleName}")
            ui.storageError = R.string.portal_restore_failed
        } finally {
            restoring = false
            AppDiagnostics.record(context, "portal_library_restore_completed")
        }
    }

    suspend fun save(source: SavedPortal, activate: Boolean = true) {
        if (activate || storedSource?.identity == source.identity) storedSource = source
        preferences?.edit { putString("active_portal_cache", storedSource?.identity) }
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        savedSources = if (index < 0) savedSources + source else {
            savedSources.toMutableList().apply { this[index] = source }
        }
        try {
            storage?.write(SavedPortals(
                savedSources, storedSource?.identity ?: savedSources.firstOrNull()?.identity ?: source.identity
            ))
            ui.storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.storageError = R.string.portal_save_failed
        }
    }

    suspend fun persistPortalSnapshot(source: SavedPortal) {
        val index = savedSources.indexOfFirst { it.identity == source.identity }
        val persistedSources = if (index < 0) savedSources + source else {
            savedSources.toMutableList().apply { this[index] = source }
        }
        val activeId = storedSource?.identity
            ?: persistedSources.firstOrNull { it.enabled }?.identity
            ?: source.identity
        try {
            storage?.write(SavedPortals(persistedSources, activeId))
            ui.storageError = null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.storageError = R.string.portal_save_failed
        }
    }

    suspend fun updatePortalStatus(identity: String, reachable: Boolean? = null, enabled: Boolean? = null) {
        val current = savedSources.firstOrNull { source ->
            runCatching { source.identity == identity }.getOrDefault(false)
        } ?: return
        val nextReachable = reachable ?: current.reachable
        val userChangedEnabled = enabled != null
        val autoDisabled = when {
            userChangedEnabled -> false
            reachable == false && autoDisableUnavailablePortals && current.enabled -> true
            reachable == true && current.autoDisabled && autoDisableUnavailablePortals -> false
            else -> current.autoDisabled
        }
        val nextEnabled = when {
            userChangedEnabled -> enabled == true
            reachable == false && autoDisableUnavailablePortals && current.enabled -> false
            reachable == true && current.autoDisabled && autoDisableUnavailablePortals -> true
            else -> current.enabled
        }
        val updated = SavedPortal(
            current.address, current.mac, current.channels, current.genres, current.catalogs,
            current.epgChannelIds, current.subscriptionExpiry, nextEnabled, nextReachable, autoDisabled
        )
        if (current.enabled == nextEnabled && current.reachable == nextReachable &&
            current.autoDisabled == autoDisabled
        ) return
        if (storedSource?.identity == identity && !nextEnabled) {
            val fallback = savedSources.firstOrNull { source ->
                source.enabled && runCatching { source.identity != identity }.getOrDefault(false)
            }
            storedSource = fallback
            preferences?.edit { putString("active_portal_cache", fallback?.identity) }
            liveSession = fallback?.let { StalkerPortal(it.address, it.mac) to null }
            catalogSession = liveSession
            channels = fallback?.channels.orEmpty()
            genres = fallback?.genres.orEmpty()
            catalogs = fallback?.catalogs.orEmpty()
            epgChannelIds = fallback?.epgChannelIds.orEmpty()
            category = null
            programmeChannel = null
        }
        save(updated, activate = false)
    }

    /** Removes a saved portal; the last portal stays (it can be turned off instead). */
    suspend fun removePortal(identity: String): Boolean {
        val remaining = savedSources.filter { source ->
            runCatching { source.identity != identity }.getOrDefault(true)
        }
        if (remaining.size == savedSources.size || remaining.isEmpty()) return false
        if (storedSource?.identity == identity) {
            val fallback = remaining.firstOrNull { it.enabled }
            storedSource = fallback
            preferences?.edit { putString("active_portal_cache", fallback?.identity) }
            liveSession = fallback?.let { StalkerPortal(it.address, it.mac) to null }
            catalogSession = liveSession
            channels = fallback?.channels.orEmpty()
            genres = fallback?.genres.orEmpty()
            catalogs = fallback?.catalogs.orEmpty()
            epgChannelIds = fallback?.epgChannelIds.orEmpty()
            category = null
            programmeChannel = null
        }
        secondarySessions.remove(identity)
        savedSources = remaining
        return try {
            storage?.write(SavedPortals(remaining, storedSource?.identity ?: remaining.first().identity))
            ui.storageError = null
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            ui.storageError = R.string.portal_save_failed
            false
        }
    }

    fun toggleProgrammeChannel(channel: StalkerChannel) {
        val item = liveVariants[channel]?.firstOrNull() ?: return
        val id = item.value.id ?: return
        val source = item.source
        val ids = if (id in source.epgChannelIds) source.epgChannelIds - id else source.epgChannelIds + id
        if (storedSource?.identity == source.identity) epgChannelIds = ids
        val snapshot = SavedPortal(
            source.address, source.mac, source.channels, source.genres, source.catalogs, ids,
            source.subscriptionExpiry, source.enabled, source.reachable, source.autoDisabled
        )
        scope.launch { save(snapshot, activate = false) }
    }

    fun savePlaybackPosition(onFailure: Boolean = false) {
        val attempt = playbackAttempt ?: return
        val key = attempt.progressKey ?: return
        val activePlayer = player ?: return
        if (activePlayer.currentMediaItem?.mediaId != attempt.mediaId || starting != null ||
            activePlayer.playbackState == Player.STATE_ENDED
        ) return
        val position = activePlayer.currentPosition
        if (position >= 0 && (!onFailure || position > 0)) {
            progressPreferences?.saveInterruptedPlayback(key, position, activePlayer.duration)
        }
    }

    fun sendTraktScrobble(action: String, notifyUser: Boolean = false) {
        if (traktSession == null) return
        val media = ui.currentTraktPlayback ?: return
        val attempt = playbackAttempt ?: return
        val activePlayer = player ?: return
        if (attempt.selection.section == PortalSection.LIVE || activePlayer.currentMediaItem?.mediaId != attempt.mediaId) return
        val duration = activePlayer.duration
        val percent = if (duration > 0L) activePlayer.currentPosition.toFloat() * 100f / duration else 0f
        scope.launch {
            try {
                withTraktSession { api, session ->
                    api.scrobble(action, media, session.accessToken, percent)
                }
                if (notifyUser) ui.traktPlaybackNotice = context.getString(
                    if (percent >= 80f) R.string.trakt_title_watched else R.string.trakt_playback_saved
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (notifyUser) ui.traktPlaybackNotice = context.getString(R.string.trakt_playback_sync_failed)
            }
        }
    }

    fun resumeFor(entry: TrackedSeries): EpisodeResume? =
        recoverEpisodeResume(entry, progressPreferences?.all.orEmpty())

    var preparedLiveVariant by remember { mutableStateOf<PreparedLiveVariant?>(null) }

    fun stopPlayback() {
        preparedLiveVariant = null
        LightAutomation.onStopped()
        SmoothSwitch.stopStandby()
        savePlaybackPosition()
        if (ui.currentTraktPlayback != null && playbackAttempt != null && (player?.currentPosition ?: 0L) > 0L) {
            sendTraktScrobble("stop", notifyUser = true)
        }
        ui.currentTraktPlayback = null
        playbackJob?.cancel()
        playbackAttempt = null
        ui.playbackSourceBadge = null
        ui.pendingSeriesEpisode = null
        ui.pendingMovie = null
        playbackMovieTitle = null
        nextEpisodeAction = null
        previousEpisodeAction = null
        ui.nextEpisodeSeconds = null
        ui.skipIntroTo = null
        ui.watchingPrompt = false
        ui.interruptedEpisode = false
        if (!ui.advancingEpisode) silentEpisodes = 0
        player?.stop()
        player?.clearMediaItems()
        Timeshift.stop()
        playing = null
        playbackSelection = null
        starting = null
        playbackError = null
        vodSection = null
        fullscreen = false
        lastBack = 0
    }

    fun advanceEpisode(confirmed: Boolean = false) {
        val next = nextEpisodeAction ?: return
        if (confirmed) silentEpisodes = 0
        ui.watchingPrompt = false
        ui.advancingEpisode = true
        try {
            next()
        } finally {
            ui.advancingEpisode = false
        }
    }

    fun reopenPlayback(
        selection: PlaybackSelection, matchSeries: Boolean = false, openFullscreen: Boolean = true
    ): Boolean {
        val current = playbackSelection ?: return false
        val matches = if (matchSeries) {
            current.source == selection.source && current.section == PortalSection.SERIES &&
                current.seriesId == selection.id
        } else current == selection
        val activePlayer = player ?: return false
        if (!matches || playbackError != null) return false
        if (playing == null) {
            if (starting == null) return false
            if (openFullscreen) {
                lastBack = 0
                fullscreen = true
            }
            return true
        }
        if (activePlayer.playbackState != Player.STATE_READY &&
            activePlayer.playbackState != Player.STATE_BUFFERING
        ) return false
        if (openFullscreen) {
            lastBack = 0
            fullscreen = true
        }
        if (canPlay()) activePlayer.play()
        return true
    }

    fun runPlayback(plan: PlaybackAttempt) {
        val activePlayer = player ?: return
        playbackJob?.cancel()
        playbackAttempt = null
        activePlayer.stop()
        activePlayer.clearMediaItems()
        playing = null
        playbackError = null
        playbackJob = scope.launch {
            var message = R.string.playback_link_error
            // Chosen by hand (automatic switch off): only that one stream is tried.
            val lastIndex = if (plan.selection.section == PortalSection.LIVE && !StreamSwitch.auto(context)) plan.index
                else plan.variants.size - 1
            for (index in plan.index..lastIndex) {
                val attempt = plan.copy(index = index, mediaId = "playback-${++ui.attemptSerial}")
                playbackAttempt = attempt
                ui.triedHls = false
                starting = attempt.channel
                try {
                    val variant = attempt.variants[index]
                    // Smooth switch: the stand-in already recorded in the background plays from its
                    // recording, at the place where the old stream was last seen.
                    val standby = if (attempt.selection.section == PortalSection.LIVE) SmoothSwitch.takeStandby(variant)
                        else null.also { SmoothSwitch.stopStandby() }
                    if (standby != null) {
                        playing = attempt.channel
                        ui.triedHls = true
                        val media = MediaItem.Builder().setUri("standby:${attempt.mediaId}").setMediaId(attempt.mediaId).build()
                        val started = Timeshift.adopt(
                            activePlayer, standby, media, SmoothSwitch.standbyTarget ?: timeshiftNow(), timeshiftMinutes(context)
                        )
                        SmoothSwitch.standbyTarget = null
                        SmoothSwitch.markSource(started)
                        AppDiagnostics.record(context, "smooth_standby_used behind_live_ms:${timeshiftNow() - started}")
                        activePlayer.prepare()
                        activePlayer.playWhenReady = canPlay()
                        if (!canPlay()) starting = null
                        return@launch
                    }
                    val prepared = preparedLiveVariant?.takeIf {
                        it.variant === variant && SystemClock.elapsedRealtime() - it.preparedAt < 15_000
                    }
                    preparedLiveVariant = null
                    val stream = prepared?.url ?: variant.resolve()
                    if (playbackAttempt !== attempt) return@launch
                    playing = attempt.channel
                    val media = MediaItem.Builder().setUri(stream).setMediaId(attempt.mediaId).build()
                    // Timeshift (optional): Live-TV plays from a recording, so pausing and seeking work.
                    val shiftMinutes = timeshiftMinutes(context)
                        ?.takeIf { attempt.selection.section == PortalSection.LIVE && !stream.substringBefore('?').endsWith(".m3u8", true) }
                    if (shiftMinutes != null) Timeshift.play(context, activePlayer, stream, media, shiftMinutes)
                    else {
                        Timeshift.stop()
                        if (attempt.progressKey == null) activePlayer.setMediaItem(media)
                        else activePlayer.setMediaItem(media, attempt.positionMs)
                    }
                    activePlayer.prepare()
                    activePlayer.playWhenReady = canPlay()
                    if (!canPlay()) starting = null
                    return@launch
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    PlaybackReason.text = (error as? PortalRequestException)?.let { "HTTP ${it.status}" } ?: error.javaClass.simpleName
                    message = if (error is PortalRequestException && error.status in listOf(401, 403)) {
                        R.string.playback_portal_denied
                    } else R.string.playback_link_error
                }
            }
            starting = null
            playbackError = if (plan.variants.size > 1) R.string.playback_all_variants_failed else message
            if (plan.selection.section == PortalSection.LIVE && plan.variants.size > 1 &&
                !StreamSwitch.auto(context) && StreamSwitch.listOnError(context)
            ) {
                ui.streamChoiceFailed = true
                ui.streamChoice = plan
            }
        }
    }

    fun startPlayback(
        channel: StalkerChannel,
        selection: PlaybackSelection,
        variants: List<PlaybackVariant>,
        progressKey: String? = null,
        openFullscreenOnStart: Boolean = true,
        liveRestoreKey: String? = null,
        stayInGuide: Boolean = false
    ) {
        if (player == null || variants.isEmpty() || reopenPlayback(selection, openFullscreen = openFullscreenOnStart)) return
        // VOD stays on its detail page while the stream resolves and buffers.
        // The player listener opens fullscreen only once playback actually starts.
        // Live TV still opens immediately; automatic episode transitions stay fullscreen.
        val remainFullscreen = selection.section == PortalSection.LIVE && !stayInGuide ||
            fullscreen && selection.section == PortalSection.SERIES && ui.advancingEpisode
        // A new channel or title while one runs (also the next episode): not a stop for the light.
        LightAutomation.beginSwitch()
        PlaybackReason.text = null
        stopPlayback()
        fullscreen = remainFullscreen
        playbackSelection = selection
        vodSection = selection.section.takeUnless { it == PortalSection.LIVE }
        val resume = progressKey?.let { progressPreferences?.getLong(it, 0L) } ?: 0L
        val attempt = PlaybackAttempt(
            selection, channel, variants, 0, resume, progressKey,
            openFullscreenOnStart = openFullscreenOnStart, liveRestoreKey = liveRestoreKey
        )
        // Without the automatic switch a channel with several streams asks which one to play.
        if (selection.section == PortalSection.LIVE && variants.size > 1 && !StreamSwitch.auto(context)) {
            ui.streamChoiceFailed = false
            ui.streamChoice = attempt
            return
        }
        runPlayback(attempt)
    }

    /**
     * [block]: the stream looped or froze while running; this one stream is then tried last for a
     * while. [staleMs]: how long the viewer has not seen the stream move on (smooth switch).
     */
    fun failPlayback(attempt: PlaybackAttempt, message: Int, block: Boolean = false, staleMs: Long = 0L) {
        if (playbackAttempt !== attempt || SmoothSwitch.holding == attempt.mediaId) return
        if (attempt.selection.section == PortalSection.LIVE) {
            val key = attempt.liveRestoreKey
            val variant = attempt.variants[attempt.index]
            val stream = variant.streamKey ?: variant.sourceId
            if (key != null && stream != null) liveStreamHistory.recordFailure(key, stream, block)
        }
        val nextIndex = 
            // Chosen by hand (automatic switch off): never another stream by itself.
            if (attempt.selection.section == PortalSection.LIVE && !StreamSwitch.auto(context)) null else attempt.nextIndex()
        if (nextIndex == null) {
            savePlaybackPosition(onFailure = true)
            SmoothSwitch.stopStandby()
            ui.currentTraktPlayback = null
            playbackJob?.cancel()
            playbackAttempt = null
            player?.stop()
            starting = null
            playbackError = if (attempt.variants.size > 1) R.string.playback_all_variants_failed else message
            if (attempt.selection.section == PortalSection.LIVE && attempt.variants.size > 1 &&
                !StreamSwitch.auto(context) && StreamSwitch.listOnError(context)
            ) {
                ui.streamChoiceFailed = true
                ui.streamChoice = attempt
            }
            // Nothing playable (e.g. no sound): playback ends and fullscreen closes, so the message
            // is shown on the page below instead of a stream that cannot even be stopped.
            fullscreen = false
            return
        }
        savePlaybackPosition(onFailure = true)
        val position = attempt.progressKey?.let { progressPreferences?.getLong(it, attempt.positionMs) }
            ?: attempt.positionMs
        // `starting` is cleared once the stream played. A failover after that keeps the
        // current view: switching to another variant must not pull the guide into fullscreen.
        val stillStarting = starting != null
        // Live-TV: after the last stream it goes through all of them once more before giving up.
        val newRound = nextIndex <= attempt.index
        if (newRound) AppDiagnostics.record(context, "playback_second_round")
        val next = attempt.copy(
            index = nextIndex, positionMs = position, round = if (newRound) attempt.round + 1 else attempt.round,
            openFullscreenOnStart = attempt.openFullscreenOnStart && stillStarting
        )
        // Smooth switch (experimental, off: a hard switch as always). When the stand-in already
        // ran in the background, it starts in its recording where the old stream was last seen
        // (also when its portal is ahead). Otherwise, when the new portal is known to run behind,
        // the last picture is held that long. Every switch is measured to learn the distance.
        if (attempt.selection.section == PortalSection.LIVE && SmoothSwitch.enabled) {
            val from = attempt.variants[attempt.index].sourceId
            val to = next.variants[next.index].sourceId
            val channelKey = attempt.liveRestoreKey
            val lag = if (channelKey == null || from == null || to == null || from == to) null
                else SmoothSwitch.knownLag(channelKey, from, to)
            val standby = SmoothSwitch.standby?.takeIf { it.variant === next.variants[next.index] && it.recorder.healthy }
            val now = timeshiftNow()
            val hold = if (standby != null && !stillStarting) {
                val target = standbyStart(now, staleMs, lag)
                SmoothSwitch.standbyTarget = target
                AppDiagnostics.record(context, "smooth_standby recorded_ms:${now - standby.recorder.startedAt} start_ms:${target - now}" +
                    if (lag == null) " lag:unknown" else " lag_ms:$lag")
                (target - now).takeIf { it >= 300L }?.coerceAtMost(MAX_HOLD_MS)
            } else {
                SmoothSwitch.standbyTarget = null
                if (stillStarting) null else smoothHoldMs(lag, staleMs)
            }
            SmoothSwitch.markSwitch(from, to, hold ?: 0L, channelKey)
            scope.launch {
                delay(14_000 + (hold ?: 0L))
                val result = SmoothSwitch.measure()
                AppDiagnostics.record(context, result?.let {
                    "smooth_measure source:${it.source} lag_ms:${it.lagMs} score:${"%.2f".format(java.util.Locale.ROOT, it.score)}" +
                        " portal_lag_ms:${it.portalLagMs}" + if (it.saved) " saved" else ""
                } ?: "smooth_measure none")
            }
            if (hold != null) {
                AppDiagnostics.record(context, "smooth_hold ms:$hold")
                SmoothSwitch.holding = attempt.mediaId
                player?.pause() // the last picture stays, the sound stops
                scope.launch {
                    delay(hold)
                    if (SmoothSwitch.holding == attempt.mediaId) SmoothSwitch.holding = null
                    if (playbackAttempt === attempt) runPlayback(next)
                }
                return
            }
        }
        runPlayback(next)
    }

    /** Plays the current title or channel from another of its portals (chosen in the player settings). */
    fun switchPlaybackVariant(index: Int) {
        val attempt = playbackAttempt ?: return
        if (index == attempt.index || index !in attempt.variants.indices) return
        savePlaybackPosition()
        val position = attempt.progressKey?.let { progressPreferences?.getLong(it, attempt.positionMs) } ?: attempt.positionMs
        val ordered = listOf(attempt.variants[index]) + attempt.variants.filterIndexed { i, _ -> i != index }
        runPlayback(attempt.copy(variants = ordered, index = 0, positionMs = position, openFullscreenOnStart = false))
    }

    fun playChannel(channel: StalkerChannel, openFullscreenOnStart: Boolean = true, stayInGuide: Boolean = false) {
        val group = liveGroups[channel] ?: return
        programmeChannel = channel
        val selection = liveSelection(channel, group, combine)
        if (!openFullscreenOnStart && playing != null && reopenPlayback(selection)) return
        val channelKey = programmeKey(channel) ?: group.identity
        startPlayback(
            channel,
            selection,
            group.variants.preferringSource(null, preferredPortal) { it.item.source.identity }
                .rankedByHistory(
                    channelKey?.let(liveStreamHistory::records).orEmpty(),
                    random = if (preferredPortal == null) kotlin.random.Random.Default else null,
                    portalOf = { it.item.source.identity }
                ) { liveStreamKey(it.item.source.identity, it.item.value.id) }
                .map { variant ->
                val item = variant.item
                val portal = sessionFor(item.source)
                val quality = variant.description.qualities.joinToString(" / ")
                val sourceLabel = playbackSourceLabel(item.source, portal)
                val label = if (quality.isBlank()) sourceLabel else "$quality · $sourceLabel"
                PlaybackVariant(label, sourceId = item.source.identity, streamKey = liveStreamKey(item.source.identity, item.value.id)) {
                    portal.resolve(item.value, portal.openSession())
                }
            },
            openFullscreenOnStart = openFullscreenOnStart,
            liveRestoreKey = channelKey,
            stayInGuide = stayInGuide
        )
    }

    fun startupChannelScope(): String {
        val activeId = storedSource?.identity ?: preferences?.getString("active_portal_cache", null)
        val enabledScope = savedSources.filter { it.enabled }.map { it.identity }.sorted().joinToString(",")
        val sourceScope = if (mergeSources) "combined:$enabledScope" else
            "${activeId.orEmpty()}:${activeId in mergedIndividualPortals}:${activeId in enabledScope.split(',')}"
        return listOf(sourceScope, countries.joinToString(","), hiddenCategories.sorted().joinToString(",")).joinToString("|")
    }

    var startupSnapshotChecked by remember { mutableStateOf(storage == null) }
    LaunchedEffect(storage, restoring, playLastChannelAtStartup, selected) {
        if (restoring || startupSnapshotChecked) return@LaunchedEffect
        if (storage == null || !playLastChannelAtStartup || selected != Destination.LIVE) {
            startupSnapshotChecked = true
            return@LaunchedEffect
        }
        val cached = try {
            storage.readStartupChannel()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val lastLiveKey = preferences?.getString("last_live_channel", null)
        if (cached != null && selected == Destination.LIVE && playbackSelection == null &&
            cached.scope == startupChannelScope() && cached.key == lastLiveKey
        ) {
            val enabledSources = savedSources.filter { it.enabled }.map { it.identity }.toSet()
            val validStreams = cached.streams.filter { stream ->
                SavedPortal(stream.address, stream.mac, emptyList(), emptyMap(), emptyMap()).identity in enabledSources
            }
            if (validStreams.isNotEmpty()) {
                val channel = StalkerChannel(cached.name, "", null, null)
                programmeChannel = channel
                ui.cachedStartupAttempt = true
                startPlayback(
                    channel, PlaybackSelection(cached.selectionSource, PortalSection.LIVE, cached.selectionId),
                    validStreams.map { saved ->
                        saved to SavedPortal(saved.address, saved.mac, emptyList(), emptyMap(), emptyMap()).identity
                    }.preferringSource(null, preferredPortal) { it.second }
                        .rankedByHistory(
                            liveStreamHistory.records(cached.key),
                            random = if (preferredPortal == null) kotlin.random.Random.Default else null,
                            portalOf = { it.second }
                        ) { liveStreamKey(it.second, it.first.id) }
                        .map { (saved, identity) ->
                        val portal = StalkerPortal(saved.address, saved.mac)
                        val stream = StalkerChannel(cached.name, saved.command, null, null, id = saved.id, direct = saved.direct)
                        PlaybackVariant(portal.sourceLabel, sourceId = identity, streamKey = liveStreamKey(identity, saved.id)) {
                            portal.resolve(stream, if (saved.direct) "" else portal.openSession())
                        }
                    },
                    openFullscreenOnStart = startInFullscreen,
                    liveRestoreKey = cached.key,
                    stayInGuide = !startInFullscreen
                )
            }
        }
        startupSnapshotChecked = true
    }

    /**
     * The quick start plays a stand-in channel (only name and streams, from the last session)
     * before the channel list exists. This finds its entry in the list: by its saved key, else
     * by name. Without it the stand-in stayed: no programme info and switching channels did
     * nothing until fullscreen was left and the channel opened again.
     */
    fun listedLiveChannel(channel: StalkerChannel?): StalkerChannel? {
        if (channel == null || channel in liveGroups) return channel
        val key = playbackAttempt?.liveRestoreKey
        return liveGroups.keys.firstOrNull { key != null && (programmeKey(it) ?: liveGroups[it]?.identity) == key }
            ?: liveGroups.keys.firstOrNull { key != null && liveGroups[it]?.identity == key }
            ?: liveGroups.keys.firstOrNull { it.name.trim().equals(channel.name.trim(), ignoreCase = true) }
    }
    LaunchedEffect(restoring, playing, playbackAttempt, liveGroups) {
        if (restoring || playing == null || playing in liveGroups || vodSection != null) return@LaunchedEffect
        val restored = listedLiveChannel(playing) ?: return@LaunchedEffect
        playing = restored
        programmeChannel = restored
    }

    LaunchedEffect(category) {
        if (category != null) {
            lastLiveCategory = category
            preferences?.edit { putString("last_live_category", category) }
        }
    }
    LaunchedEffect(restoring, liveCategories) {
        if (restoring || liveCategories.isEmpty()) return@LaunchedEffect
        val available = liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }
        if (category !in available) {
            val savedKey = preferences?.getString("last_live_channel", null)
            val inferred = savedKey?.let { key -> liveCategories.entries.firstOrNull { (_, items) ->
                items.any { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
            }?.key }
            category = inferred ?: lastLiveCategory?.takeIf { it in available } ?: available.firstOrNull()
        }
    }
    LaunchedEffect(restoring, startupSnapshotChecked, liveCategories, playbackError, playLastChannelAtStartup) {
        if (restoring || !startupSnapshotChecked || liveCategories.isEmpty() || ui.restoredLastLiveChannel) return@LaunchedEffect
        val retryFromCatalogue = ui.cachedStartupAttempt && playbackError != null
        if (ui.cachedStartupAttempt && !retryFromCatalogue && playbackSelection != null) return@LaunchedEffect
        ui.restoredLastLiveChannel = true
        // With films or series as the start page the guide is still prepared (category and
        // channel of the last session), only nothing is played.
        if (playbackSelection != null && !retryFromCatalogue) return@LaunchedEffect
        val orderedCategories = liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }
        val savedKey = preferences?.getString("last_live_channel", null)
        val categoryWithLastChannel = savedKey?.let { key ->
            liveCategories.entries.firstOrNull { (_, items) -> items.any { channel ->
                (programmeKey(channel) ?: liveGroups[channel]?.identity) == key
            } }?.key
        }
        val restoredCategory = categoryWithLastChannel
            ?: category?.takeIf { it in orderedCategories }
            ?: lastLiveCategory?.takeIf { it in orderedCategories }
            ?: orderedCategories.firstOrNull()
        category = restoredCategory
        lastLiveCategory = restoredCategory
        if (restoredCategory != null) {
            val visibleChannels = liveCategories[restoredCategory].orEmpty()
            val index = visibleChannels.indexOfFirst { channel ->
                (programmeKey(channel) ?: liveGroups[channel]?.identity) == savedKey
            }
            if (index >= 0) {
                guideScroll.requestScrollToItem(index)
                programmeChannel = visibleChannels[index]
                ui.restoreLiveFocus = true
                if (playLastChannelAtStartup && selected == Destination.LIVE) playChannel(
                    visibleChannels[index], openFullscreenOnStart = startInFullscreen, stayInGuide = !startInFullscreen
                )
            } else {
                guideScroll.requestScrollToItem(0)
                programmeChannel = visibleChannels.firstOrNull()
            }
        }
    }

    // Started in fullscreen but no channel could be started (none saved any more, no portal):
    // back to the guide instead of a black screen.
    LaunchedEffect(Unit) {
        if (!fullscreenAtStart) return@LaunchedEffect
        withTimeoutOrNull(30_000L) {
            snapshotFlow {
                !restoring && startupSnapshotChecked && (playbackSelection != null || ui.restoredLastLiveChannel)
            }.first { it }
        }
        if (playbackSelection == null && fullscreen) {
            fullscreen = false
            startupInfoVisible = false
        }
    }
    LaunchedEffect(startupInfoVisible, fullscreen, starting, playing, playbackError, vodSection) {
        if (!startupInfoVisible) return@LaunchedEffect
        if (!fullscreen || playbackError != null || vodSection != null) {
            startupInfoVisible = false
            return@LaunchedEffect
        }
        // Visible while the stream starts, then a few more seconds.
        if (starting != null || playbackSelection == null) return@LaunchedEffect
        delay(6_000L)
        startupInfoVisible = false
    }

    fun closeSeries() {
        episodeJob?.cancel()
        openedSeries = null
        seasons = null
        openedSeason = null
        episodeVariants = emptyMap()
        ui.episodeError = false
        ui.resumeEpisodeMissing = false
    }

    fun switchSource(source: SavedPortal) {
        if (!source.enabled || connecting || source.identity == storedSource?.identity) return
        closeGroup()
        closeSeries()
        details = null
        searchedVariants = emptyMap()
        category = null
        lastLiveCategory = null
        channelScrollByCategory.clear()
        categoryScroll.requestScrollToItem(0)
        guideScroll.requestScrollToItem(0)
        programmeChannel = null
        ui.importStatuses = emptyMap()
        ui.sourceError = null
        ui.catalogErrors = emptySet()
        ui.catalogLoading = emptySet()
        val session = StalkerPortal(source.address, source.mac) to null
        liveSession = session
        catalogSession = session
        channels = source.channels
        genres = source.genres
        catalogs = source.catalogs
        epgChannelIds = source.epgChannelIds
        address = source.address
        mac = source.mac
        scope.launch { save(source) }
    }

    fun vodVariantCountry(section: PortalSection, item: SourceItem<PortalTitle>): String {
        val group = item.value.categoryId?.let { id ->
            item.source.catalogs[section]?.groups?.firstOrNull { it.id == id }
        }
        return categoryCountry(group?.name ?: item.value.categoryName.orEmpty(), group?.country)
    }

    /** Whether a title's provider category belongs to a chosen country (or names none). */
    fun countryAllowed(section: PortalSection, item: SourceItem<PortalTitle>): Boolean {
        if (countries.isEmpty()) return true
        val country = vodVariantCountry(section, item)
        return country == UNKNOWN_COUNTRY || country in countries
    }

    /**
     * Drops variants from provider categories of countries outside the selection (for
     * example an English or Dutch series version) and orders the rest by country priority.
     */
    fun countryVariants(section: PortalSection, variants: List<SourceItem<PortalTitle>>): List<SourceItem<PortalTitle>> {
        if (!combine || countries.isEmpty()) return variants
        return variants.map { it to vodVariantCountry(section, it) }
            .filter { (_, country) -> country == UNKNOWN_COUNTRY || country in countries }
            .sortedBy { (_, country) -> countries.indexOf(country).takeIf { it >= 0 } ?: countries.size }
            .map { it.first }
    }

    // Matches across portals are remembered while sources and country selection stay the
    // same; detail views call this during composition.
    val titleEntryCache = remember(sourcesForBrowsing, combine, countries, storedSource?.identity) {
        HashMap<Pair<PortalSection, PortalTitle>, List<SourceItem<PortalTitle>>>()
    }
    LaunchedEffect(sourcesForBrowsing) {
        // Index large catalogues in the background after start-up work has settled, so the
        // first detail view does not build them on the main thread.
        delay(5_000)
        withContext(Dispatchers.Default) {
            for (source in sourcesForBrowsing) for (section in listOf(PortalSection.MOVIES, PortalSection.SERIES)) {
                val titles = source.catalogs[section]?.titles.orEmpty()
                if (titles.size >= 256) equivalentCatalogueTitle(PortalTitle(""), titles)
            }
        }
    }

    fun titleEntries(section: PortalSection, title: PortalTitle): List<SourceItem<PortalTitle>> {
        val known = searchedVariants[title] ?: groupVariants[title]
        if (known != null) return countryVariants(section, known).ifEmpty { known.take(1) }
        val source = storedSource ?: return emptyList()
        if (!combine) return listOf(SourceItem(source, title))
        return titleEntryCache.getOrPut(section to title) {
            val entries = listOf(SourceItem(source, title)) + sourcesForBrowsing
                .filter { it.identity != source.identity }.mapNotNull { other ->
                    equivalentCatalogueTitle(
                        title, other.catalogs[section]?.titles.orEmpty(), requireVerifiedIdentity = section == PortalSection.SERIES
                    )
                        ?.let { SourceItem(other, it) }
                }
            countryVariants(section, entries).ifEmpty { entries.take(1) }
        }
    }

    /** Portals offering a film or series, preferred portal first. */
    fun titlePortals(section: PortalSection, title: PortalTitle): List<SavedPortal> =
        titleEntries(section, title).map { it.source }.distinctBy { it.identity }
            .preferringSource(null, preferredPortal) { it.identity }

    suspend fun findEquivalent(
        source: SavedPortal, section: PortalSection, title: PortalTitle, sameSource: Boolean = false
    ): PortalTitle? {
        val portal = sessionFor(source)
        val search = displayVodTitle(title)
        if (search.isBlank()) return null
        val candidates = portal.loadCatalog(
            section, portal.openSession(), search = search
        ).titles
        if (sameSource && title.id != null) candidates.firstOrNull { it.id == title.id }?.let { return it }
        return equivalentCatalogueTitle(title, candidates, requireVerifiedIdentity = section == PortalSection.SERIES)
    }

    fun hasPortalReference(
        section: PortalSection, reference: PortalTitle, refs: List<SeriesPortalRef>
    ): Boolean = refs.any { ref ->
        if (ref.portalTitleId.isBlank()) return@any false
        val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@any false
        val cached = source.catalogs[section]?.titles?.firstOrNull { it.id == ref.portalTitleId }
        if (cached == null) true else equivalentCatalogueTitle(
            reference,
            listOf(cached), requireVerifiedIdentity = section == PortalSection.SERIES
        ) != null
    }

    val historyArtworkRequests = remember { Semaphore(3) }
    // Keep multi-portal season requests bounded; a catalog can contain thousands
    // of episodes and must not open one network request per source at once.
    val episodeCatalogRequests = remember { Semaphore(2) }
    /** A history entry stores no poster: it comes from the portal (looked up once). */
    @Composable
    fun historyArtwork(section: PortalSection, title: PortalTitle, refs: List<SeriesPortalRef>): PortalTitle {
        val artwork by produceState<PortalTitle?>(null, section, title, refs, savedSources) {
            value = null
            for (ref in refs) {
                val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: continue
                val cached = source.catalogs[section]?.titles?.firstOrNull {
                    ref.portalTitleId.isNotBlank() && it.id == ref.portalTitleId
                }
                val candidate = cached?.takeIf { it.poster != null } ?: try {
                    historyArtworkRequests.withPermit {
                        findEquivalent(source, section, cached ?: title.copy(id = ref.portalTitleId), sameSource = true)
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
                if (candidate?.poster != null) {
                    value = candidate
                    break
                }
            }
        }
        return artwork ?: title
    }

    @Composable
    fun HistoryArtwork(section: PortalSection, title: PortalTitle, refs: List<SeriesPortalRef>) {
        Poster(historyArtwork(section, title, refs), Modifier.width(104.dp).padding(end = 10.dp))
    }

    fun titleSelection(
        section: PortalSection, title: PortalTitle, season: PortalSeason? = openedSeason
    ): PlaybackSelection {
        val root = openedSeries.takeIf { section == PortalSection.SERIES && season != null }
        val canonical = titleIdentity(root ?: title)
        val source = titleEntries(section, root ?: title).firstOrNull()?.source?.identity.orEmpty()
        val seriesId = root?.let { titleIdentity(it) ?: it.id ?: it.name }
        val id = if (root != null) {
            val seasonNumber = season?.number ?: Regex("\\d+").find(season?.title?.name.orEmpty())?.value
            val rawEpisode = title.episode.orEmpty()
            val episode = Regex("\\d+").findAll(rawEpisode).lastOrNull()?.value?.toIntOrNull()?.toString()
                ?: title.id ?: title.name
            "${seasonNumber ?: season?.title?.name}:$episode"
        } else canonical ?: title.id ?: title.name
        return PlaybackSelection(
            if (canonical != null) "catalogue" else source, section, id, title.episode, seriesId
        )
    }

    fun isPlaybackCompleted(key: String, fallback: Boolean = false): Boolean {
        val prefs = progressPreferences ?: return fallback
        return prefs.isPlaybackCompleted(key, fallback)
    }
    fun setWatched(section: PortalSection, title: PortalTitle, episodes: List<Pair<PortalSeason, PortalTitle>>, watched: Boolean) {
        if (ui.sendingWatchStatus || ui.traktBusy) return
        val prefs = progressPreferences ?: return
        val keys = if (section == PortalSection.MOVIES) {
            setOf(titleSelection(section, title).progressKey) + movieHistory.filter { movie ->
                movie.refs.any { ref -> titleEntries(section, title).any { it.source.identity == ref.sourceId && it.value.id == ref.portalTitleId } }
            }.map { it.progressKey }
        } else episodes.mapTo(linkedSetOf()) { (season, episode) -> titleSelection(section, episode, season).progressKey }
        val previous = if (section == PortalSection.SERIES) trackedSeries(title) else null
        val marked = keys + listOfNotNull(previous?.lastEpisode?.progressKey?.takeIf {
            episodes.any { (season, episode) -> seriesEpisodeKey(season, episode) == previous.lastEpisode.episodeKey }
        })
        prefs.edit {
            marked.forEach { key ->
                putBoolean("manual_watched:$key", watched).putBoolean("completed:$key", watched)
                    .remove(key).remove("trakt_percent:$key").putLong("progress_updated:$key", System.currentTimeMillis())
            }
        }
        if (section == PortalSection.MOVIES) {
            val remaining = movieHistory.filterNot { it.progressKey in keys }
            val added = if (watched) listOf(WatchedMovie(
                displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                titleEntries(section, title).mapNotNull { item -> item.value.id?.let { SeriesPortalRef(item.source.identity, it) } },
                titleSelection(section, title).progressKey, System.currentTimeMillis()
            )) else emptyList()
            movieHistory = (added + remaining).take(200)
            prefs.edit { putString("movie_history", encodeMovieHistory(movieHistory)) }
        } else if (episodes.isNotEmpty()) {
            val episodeKeys = episodes.mapTo(linkedSetOf()) { (season, episode) -> seriesEpisodeKey(season, episode) }
            val last = episodes.last()
            val lastResume = EpisodeResume(
                seriesEpisodeKey(last.first, last.second), last.first.number, last.second.episode,
                last.second.name, titleSelection(section, last.second, last.first).progressKey
            )
            if (watched && previous == null) updateTrackedSeries(title, watchedEpisode = lastResume)
            val record = previous ?: trackedSeries(title)
            if (record != null) {
                val completed = if (watched) record.watchedEpisodes + episodeKeys else record.watchedEpisodes - episodeKeys
                val allEpisodes = seasons.orEmpty().flatMap { season -> season.episodes.map { seriesEpisodeKey(season, it) } }.toSet()
                val lastEpisode = if (watched) lastResume else record.lastEpisode?.takeUnless { it.episodeKey in episodeKeys }
                saveSeriesActivity(seriesActivity.map {
                    if (it.key != record.key) it else it.copy(
                        viewed = watched || completed.isNotEmpty() || lastEpisode != null,
                        localViewed = watched || it.localViewed && (completed.isNotEmpty() || lastEpisode != null),
                        hiddenFromHistory = it.hiddenFromHistory && !watched,
                        watchedEpisodes = completed,
                        unseen = it.unseen - completed,
                        watchedAt = if (watched) System.currentTimeMillis() else it.watchedAt,
                        lastEpisode = lastEpisode,
                        isComplete = allEpisodes.isNotEmpty() && allEpisodes.all { it in completed }
                    )
                })
            }
        }
        val account = traktSession?.username ?: return
        val remoteTitle = title.copy(imdbId = title.imdbId ?: previous?.imdb, tmdbId = title.tmdbId ?: previous?.tmdb)
        val numberedEpisodes = episodes.mapNotNull { (season, episode) ->
            val (seasonKey, episodeKey) = seriesEpisodeKey(season, episode).split(':', limit = 2)
            val seasonNumber = seasonKey.toIntOrNull()?.takeIf { it >= 0 } ?: return@mapNotNull null
            val episodeNumber = episodeKey.toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            seasonNumber to episodeNumber
        }
        ui.sendingWatchStatus = true
        scope.launch {
            try {
                val accepted = numberedEpisodes.size == episodes.size && withTraktSession { api, session ->
                    session.username == account && api.setHistoryStatus(
                        session.accessToken, section, remoteTitle, numberedEpisodes.toSet(), watched
                    )
                }
                ui.traktPlaybackNotice = context.getString(when {
                    !accepted -> R.string.trakt_manual_history_failed
                    watched -> R.string.trakt_manual_history_added
                    else -> R.string.trakt_manual_history_removed
                })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ui.traktPlaybackNotice = context.getString(R.string.trakt_manual_history_failed)
            } finally {
                ui.sendingWatchStatus = false
            }
        }
    }

    ui.watchStatusAction?.let { update ->
        AlertDialog(
            onDismissRequest = { ui.watchStatusAction = null },
            title = { Text(stringResource(R.string.watched_status)) },
            text = if (traktSession != null) {
                { Text(stringResource(R.string.trakt_manual_history_description)) }
            } else null,
            confirmButton = {
                MaterialTextButton(enabled = !ui.sendingWatchStatus && !ui.traktBusy, onClick = { update(true); ui.watchStatusAction = null }) {
                    Text(stringResource(R.string.mark_watched))
                }
            },
            dismissButton = {
                MaterialTextButton(enabled = !ui.sendingWatchStatus && !ui.traktBusy, onClick = { update(false); ui.watchStatusAction = null }) {
                    Text(stringResource(R.string.mark_unwatched))
                }
            }
        )
    }

    /** Age rating from TMDB (FSK) or Trakt; cached per title for this session. */
    suspend fun titleAge(section: PortalSection, title: PortalTitle): Int? {
        val key = "${section.name}:${title.tmdbId ?: title.imdbId ?: displayVodTitle(title)}"
        if (ageRatings.containsKey(key)) return ageRatings[key]
        val client = metadataClient
        val age = try {
            val fromTmdb = client?.let { tmdb ->
                val id = title.tmdbId?.toIntOrNull() ?: tmdb.lookup(section, title)?.id
                id?.let { tmdb.ageRating(section, it) }
            }
            fromTmdb ?: if (traktSession != null) withTraktSession { api, session ->
                ageFromRating(api.lookupTitleMetadata(section, title, session.accessToken)?.certification)
            } else null
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        ageRatings[key] = age
        return age
    }
    val detailAge by produceState<Int?>(null, details, metadataClient, traktSession?.username) {
        value = null
        val (section, title) = details?.takeIf { it.first != PortalSection.LIVE } ?: return@produceState
        if (metadataClient == null && traktSession == null) return@produceState
        value = titleAge(section, title)
    }
    fun showTitle(section: PortalSection, title: PortalTitle) {
        if (parental.kidsMode && section != PortalSection.LIVE) {
            val key = "${section.name}:${title.tmdbId ?: title.imdbId ?: displayVodTitle(title)}"
            if (key !in parental.allowedTitles) {
                scope.launch {
                    val age = titleAge(section, title)
                    if (parental.allowsAge(age)) {
                        parental.allowedTitles += key
                        showTitle(section, title)
                    } else ui.pinRequest = PinRequest(
                        context.getString(R.string.kids_title_blocked),
                        context.getString(
                            R.string.kids_title_blocked_message, displayVodTitle(title),
                            age?.let { context.getString(R.string.kids_age_value, it) }
                                ?: context.getString(R.string.kids_rating_unknown)
                        ),
                        check = parental::checkKidsPin, length = parental.kidsExitPinLength
                    ) {
                        parental.allowedTitles += key
                        showTitle(section, title)
                    }
                }
                return
            }
        }
        if (reopenPlayback(
                titleSelection(section, title),
                matchSeries = section == PortalSection.SERIES
            )
        ) return
        closeSeries()
        ui.focusTitlePrimary = true
        if (section == PortalSection.SERIES) seriesDetailScroll.requestScrollToItem(0)
        details = section to title
        if (section == PortalSection.SERIES && isTitleSaved(section, title) && trackedSeries(title) == null) {
            updateTrackedSeries(title, saved = true)
        }
    }

    fun openTrackedSeries(entry: TrackedSeries, onOpen: ((PortalTitle) -> Unit)? = null) {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val linkedVariants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.SERIES]?.titles?.firstOrNull { it.id == ref.portalTitleId }
                ?: reference.copy(id = ref.portalTitleId)
            SourceItem(source, title)
        }
        val discoveredVariants = if (linkedVariants.isEmpty()) sourcesForBrowsing.flatMap { source ->
            source.catalogs[PortalSection.SERIES]?.titles.orEmpty().mapNotNull { candidate ->
                equivalentCatalogueTitle(reference, listOf(candidate), requireVerifiedIdentity = false)
                    ?.let { SourceItem(source, it) }
            }
        } else emptyList()
        val allVariants = (linkedVariants + discoveredVariants).distinctBy { "${it.source.identity}:${it.value.id}" }
        // Prefer versions from the selected countries; a history entry may also link to
        // a foreign-language category of the same portal.
        val variants = countryVariants(PortalSection.SERIES, allVariants).ifEmpty { allVariants }
        val primary = variants.firstOrNull {
            equivalentCatalogueTitle(reference, listOf(it.value), requireVerifiedIdentity = true) != null
        } ?: variants.firstOrNull() ?: run {
            ui.traktPlaybackNotice = context.getString(R.string.trakt_source_missing)
            return
        }
        val title = primary.value
        searchedVariants = mapOf(title to (listOf(primary) + variants.filter {
            it != primary && equivalentCatalogueTitle(title, listOf(it.value), requireVerifiedIdentity = true) != null
        }))
        if (onOpen == null) showTitle(PortalSection.SERIES, title) else onOpen(title)
    }

    fun selectTitle(
        section: PortalSection,
        title: PortalTitle,
        resume: EpisodeResume? = null,
        progressOverride: String? = null,
        loadOnly: Boolean = false
    ) {
        if (storedSource == null) return
        val selection = titleSelection(section, title)
        if (!loadOnly && reopenPlayback(selection, matchSeries = section == PortalSection.SERIES && openedSeries == null)) {
            return
        }
        if (section == PortalSection.SERIES && openedSeries == title) {
            if (resume != null) {
                val item = resumeEpisode(seasons.orEmpty(), resume)
                if (item == null) ui.resumeEpisodeMissing = true
                else {
                    openedSeason = item.first
                    selectTitle(section, item.second, progressOverride = resume.progressKey)
                }
            }
            return
        }
        if (section == PortalSection.SERIES && openedSeries == null) {
            openedSeries = title
            ui.episodeError = false
            ui.resumeEpisodeMissing = false
            seasons = null
            openedSeason = null
            episodeVariants = emptyMap()
            episodeJob = scope.launch {
                try {
                    val known = titleEntries(section, title).distinctBy { it.source.identity }
                        .associateBy { it.source.identity }
                    val owners = if (combine) {
                        known.values.map { it.source } + sourcesForBrowsing.filter { it.identity !in known }
                    } else known.values.map { it.source }
                    val results = coroutineScope {
                        owners.map { source ->
                            async {
                                try {
                                    episodeCatalogRequests.withPermit {
                                        val item = known[source.identity]?.value
                                            ?: findEquivalent(source, section, title) ?: return@withPermit emptyList()
                                        val portal = sessionFor(source)
                                        portal.loadSeasons(item, portal.openSession()).map { SourceItem(source, it) }
                                    }
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    emptyList()
                                }
                            }
                        }.flatMap { it.await() }
                    }
                    val variants = mutableMapOf<PortalTitle, List<SourceItem<PortalTitle>>>()
                    val grouped = if (combine) {
                        mergeSourceItems(results) { it.number?.let { number -> "season:$number" } }
                    } else results.map(::listOf)
                    seasons = grouped.map { season ->
                        val items = season.flatMap { entry -> entry.value.episodes.map { SourceItem(entry.source, it) } }
                        val episodes = if (season.size > 1) {
                            mergeSourceItems(items) { it.episode?.toIntOrNull()?.let { number -> "episode:$number" } }
                        } else items.map(::listOf)
                        for (episode in episodes) variants[episode.first().value] = episode
                        season.first().value.copy(episodes = episodes.map { it.first().value })
                    }
                    episodeVariants = variants
                    if (resume != null) {
                        val item = resumeEpisode(seasons.orEmpty(), resume)
                        if (item != null) {
                            val (season, episode) = item
                            openedSeason = season
                            selectTitle(section, episode, progressOverride = resume.progressKey)
                        } else ui.resumeEpisodeMissing = true
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    AppDiagnostics.record(context, "series_catalog_load_failed")
                    ui.episodeError = true
                }
            }
        } else {
            val known = if (section == PortalSection.SERIES) episodeVariants[title].orEmpty()
            else titleEntries(section, title)
            val knownBySource = known.distinctBy { it.source.identity }.associateBy { it.source.identity }
            val chosenPortal = (if (section == PortalSection.SERIES) openedSeries else title)
                ?.let { titlePortalChoices[titleKey(section, it)] }
            val owners = (if (combine && section == PortalSection.MOVIES) {
                knownBySource.values.map { it.source } + sourcesForBrowsing.filter { it.identity !in knownBySource }
            } else knownBySource.values.map { it.source }).preferringSource(chosenPortal, preferredPortal) { it.identity }
            val variants = owners.map { source ->
                val portal = sessionFor(source)
                PlaybackVariant(
                    playbackSourceLabel(source, portal), knownBySource[source.identity]?.value?.timing ?: EpisodeTiming(),
                    sourceId = source.identity
                ) {
                    val knownTitle = knownBySource[source.identity]?.value
                    val item = knownTitle?.takeIf { it.command != null } ?: run {
                        val found = portal.loadCatalog(section, portal.openSession(), search = displayVodTitle(title))
                            .titles.filter { it.command != null && countryAllowed(section, SourceItem(source, it)) }
                        found.firstOrNull { knownTitle?.id != null && it.id == knownTitle.id }
                            ?: equivalentCatalogueTitle(title, found) ?: error("No equivalent title")
                    }
                    portal.resolveVod(item, portal.openSession())
                }
            }
            if (variants.isNotEmpty()) {
                val key = progressOverride ?: selection.progressKey
                startPlayback(
                    StalkerChannel(title.name, title.command.orEmpty(), null, null),
                    selection, variants, progressKey = key
                )
                ui.currentTraktPlayback = when (section) {
                    PortalSection.MOVIES -> TraktMedia(
                        PortalSection.MOVIES, displayVodTitle(title), title.year, title.imdbId, title.tmdbId, null
                    )
                    PortalSection.SERIES -> openedSeries?.let { show ->
                        TraktMedia(
                            PortalSection.SERIES, displayVodTitle(show), show.year, show.imdbId, show.tmdbId, null,
                            seasonNumber = openedSeason?.number,
                            episodeNumber = title.episode?.toIntOrNull()
                        )
                    }
                    PortalSection.LIVE -> null
                }
                if (section == PortalSection.SERIES) {
                    playbackMovieTitle = null
                    previousEpisodeAction = null
                    nextEpisodeAction = null
                    ui.nextEpisodeSeconds = null
                    val series = openedSeries
                    val season = openedSeason
                    if (series != null && season != null) {
                        ui.pendingSeriesEpisode = series to EpisodeResume(
                            seriesEpisodeKey(season, title), season.number, title.episode, title.name, key
                        )
                        val seasonList = seasons.orEmpty()
                        val availableVariants = episodeVariants
                        playbackSeries = series
                        playbackSeasons = seasonList
                        playbackSeason = season
                        playbackEpisode = title
                        overlaySeason = season
                        playbackEpisodeVariants = availableVariants
                        precedingEpisode(seasonList, season, title)?.let { (previousSeason, previous) ->
                            previousEpisodeAction = {
                                openedSeries = series
                                openedSeason = previousSeason
                                seasons = seasonList
                                episodeVariants = availableVariants
                                selectTitle(PortalSection.SERIES, previous)
                            }
                        } ?: run { previousEpisodeAction = null }
                        followingEpisode(seasonList, season, title)?.let { (followingSeason, following) ->
                            ui.nextEpisodeName = following.episode?.let {
                                resources.getString(R.string.episode_number, it)
                            } ?: following.name
                            nextEpisodeAction = {
                                openedSeries = series
                                openedSeason = followingSeason
                                seasons = seasonList
                                episodeVariants = availableVariants
                                selectTitle(PortalSection.SERIES, following)
                                progressPreferences?.edit { remove(key); putBoolean("completed:$key", true) }
                            }
                        }
                    }
                } else if (section == PortalSection.MOVIES) {
                    playbackMovieTitle = title
                    ui.pendingMovie = WatchedMovie(
                        displayVodTitle(title), vodReleaseYear(title), title.imdbId, title.tmdbId,
                        known.map { SeriesPortalRef(it.source.identity, it.value.id.orEmpty()) }.distinct(),
                        key, System.currentTimeMillis()
                    )
                }
            } else {
                vodSection = section
                playbackError = R.string.vod_missing_link
            }
        }
    }

    // Choice of the portal on a film's or series' page. Films start right away; for a series
    // the choice applies to the episodes started from its page.
    ui.portalPickerFor?.let { (section, title) ->
        val key = titleKey(section, title)
        val portals = titlePortals(section, title)
        fun choose(identity: String?) {
            if (identity == null) titlePortalChoices.remove(key) else titlePortalChoices[key] = identity
            ui.portalPickerFor = null
            if (section == PortalSection.MOVIES && identity != null) selectTitle(section, title)
        }
        AlertDialog(
            onDismissRequest = { ui.portalPickerFor = null },
            title = { Text(stringResource(R.string.title_portal_choose)) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(
                        if (section == PortalSection.MOVIES) R.string.title_portal_movie_hint else R.string.title_portal_series_hint
                    ), style = MaterialTheme.typography.bodySmall)
                    MenuButton(stringResource(R.string.title_portal_auto), Modifier.fillMaxWidth(), selected = key !in titlePortalChoices) {
                        choose(null)
                    }
                    portals.forEach { source ->
                        Row(Modifier.fillMaxWidth().focusGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MenuButton(
                                portalName(source), Modifier.weight(1f), selected = titlePortalChoices[key] == source.identity
                            ) { choose(source.identity) }
                            val preferred = preferredPortal == source.identity
                            MenuButton(
                                stringResource(if (preferred) R.string.portal_preferred else R.string.portal_prefer),
                                Modifier, selected = preferred
                            ) { setPreferredPortal(if (preferred) null else source.identity) }
                        }
                    }
                }
            },
            confirmButton = { MaterialTextButton(onClick = { ui.portalPickerFor = null }) { Text(stringResource(R.string.close)) } }
        )
    }

    LaunchedEffect(details) {
        val (section, title) = details ?: return@LaunchedEffect
        if (section == PortalSection.SERIES && openedSeries == null) selectTitle(section, title, loadOnly = true)
    }
    val portalDetail by produceState<PortalTitle?>(null, details) {
        value = null
        val (section, title) = details ?: return@produceState
        if (title.poster != null && germanDescription(title.description) != null) return@produceState
        for (item in titleEntries(section, title)) {
            val found = try {
                findEquivalent(item.source, section, item.value, sameSource = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            value = title.copy(
                poster = value?.poster ?: title.poster ?: item.value.poster ?: found?.poster,
                description = value?.description ?: germanDescription(title.description)
                    ?: germanDescription(item.value.description) ?: germanDescription(found?.description)
            )
            if (value?.poster != null && !value?.description.isNullOrBlank()) break
        }
    }
    val episodeMetadata by produceState(
        emptyMap<Pair<Int, Int>, TmdbEpisodeDetails>(), detailMetadata, seasons, metadataClient,
        details, traktSession?.username
    ) {
        value = emptyMap()
        val seriesTitle = details?.takeIf { it.first == PortalSection.SERIES }?.second
            ?: return@produceState
        if (traktSession != null) {
            // Series from the history already carry the IDs Trakt reported; they identify the
            // show reliably even when the portal uses a localized title.
            val tracked = trackedSeries(seriesTitle)
            val lookupTitle = seriesTitle.copy(
                imdbId = seriesTitle.imdbId ?: tracked?.imdb,
                tmdbId = seriesTitle.tmdbId ?: tracked?.tmdb,
                year = seriesTitle.year ?: tracked?.year
            )
            val summaries = try {
                withTraktSession { api, session ->
                    api.episodeDescriptions(lookupTitle, session.accessToken)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyMap()
            }
            value = summaries
        }
        val client = metadataClient ?: return@produceState
        val series = detailMetadata?.takeIf { details?.first == PortalSection.SERIES } ?: return@produceState
        // Fetch TMDB stills and localized overviews even when the portal supplied
        // a generic poster or a non-German episode description.
        val seasonNumbers = seasons.orEmpty().mapNotNull { it.number }.distinct()
        for (batch in seasonNumbers.chunked(3)) {
            val loaded = coroutineScope {
                batch.map { number ->
                    async {
                        try {
                            client.episodeDetails(series.id, number).mapKeys { number to it.key }
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            emptyMap()
                        }
                    }
                }.flatMap { it.await().entries }
            }
            value = value + loaded.map { (key, episode) ->
                key to episode.copy(
                    description = germanDescription(episode.description) ?: value[key]?.description,
                    picture = episode.picture ?: value[key]?.picture
                )
            }
        }
    }

    fun openWatchedMovie(entry: WatchedMovie, resume: Boolean) {
        val reference = PortalTitle(entry.name, year = entry.year, imdbId = entry.imdb, tmdbId = entry.tmdb)
        val linkedVariants = entry.refs.mapNotNull { ref ->
            val source = savedSources.firstOrNull { it.identity == ref.sourceId && it.enabled } ?: return@mapNotNull null
            val title = source.catalogs[PortalSection.MOVIES]?.titles?.firstOrNull {
                ref.portalTitleId.isNotBlank() && it.id == ref.portalTitleId
            } ?: PortalTitle(
                entry.name, id = ref.portalTitleId.ifBlank { null }, year = entry.year,
                imdbId = entry.imdb, tmdbId = entry.tmdb
            )
            SourceItem(source, title)
        }
        val discoveredVariants = if (linkedVariants.isEmpty()) sourcesForBrowsing.flatMap { source ->
            source.catalogs[PortalSection.MOVIES]?.titles.orEmpty().mapNotNull { candidate ->
                equivalentCatalogueTitle(reference, listOf(candidate), requireVerifiedIdentity = false)
                    ?.let { SourceItem(source, it) }
            }
        } else emptyList()
        val allVariants = (linkedVariants + discoveredVariants).distinctBy { "${it.source.identity}:${it.value.id}" }
        val variants = countryVariants(PortalSection.MOVIES, allVariants).ifEmpty { allVariants }
        val title = variants.firstOrNull()?.value ?: run {
            ui.traktPlaybackNotice = context.getString(R.string.trakt_source_missing)
            return
        }
        searchedVariants = mapOf(title to variants)
        if (resume) {
            details = PortalSection.MOVIES to title
            selectTitle(PortalSection.MOVIES, title, progressOverride = entry.progressKey)
        } else showTitle(PortalSection.MOVIES, title)
    }

    fun portalRefreshKey(identity: String) = "portal_full_refresh_at_$identity"

    fun markPortalFullRefresh(identity: String) {
        preferences?.edit { putLong(portalRefreshKey(identity), System.currentTimeMillis()) }
    }

    fun portalFullRefreshDue(source: SavedPortal): Boolean {
        val key = portalRefreshKey(source.identity)
        var lastRefresh = preferences?.getLong(key, 0L) ?: 0L
        if (lastRefresh == 0L && (source.channels.isNotEmpty() || source.catalogs.isNotEmpty())) {
            // Existing installs did not persist a refresh timestamp. Treat their saved
            // catalogue as a fresh snapshot once so upgrading does not trigger a cold scan.
            markPortalFullRefresh(source.identity)
            lastRefresh = System.currentTimeMillis()
        }
        return lastRefresh == 0L || System.currentTimeMillis() - lastRefresh >= 6 * 60 * 60 * 1_000L
    }

    suspend fun refreshPortal(enteredAddress: String, enteredMac: String, activate: Boolean): Boolean {
        AppDiagnostics.record(context, "portal_refresh_started")
        ui.sourceError = null
        ui.importStatuses = emptyMap()
        try {
            val source = StalkerPortal(enteredAddress, enteredMac)
            val token = source.openSession()
            val previous = savedSources.firstOrNull { candidate ->
                candidate.address == enteredAddress && runCatching {
                    normalizePortalMac(candidate.mac) == normalizePortalMac(enteredMac)
                }.getOrDefault(false)
            }
            val sourceIdentity = SavedPortal(
                enteredAddress, enteredMac, emptyList(), emptyMap(), emptyMap()
            ).identity
            var refreshedChannels = previous?.channels.orEmpty()
            var refreshedGenres = previous?.genres.orEmpty()
            var refreshedCatalogs = previous?.catalogs.orEmpty()
            // LIVE, MOVIES and SERIES load concurrently. Their results and progress
            // must be committed as one snapshot at a time so one section cannot
            // overwrite another section's newer in-memory catalogue.
            val snapshotMutex = Mutex()
            val keepEnabled = activate || previous?.enabled != false || previous.autoDisabled
            val initialSnapshot = SavedPortal(
                enteredAddress, enteredMac, refreshedChannels, refreshedGenres, refreshedCatalogs,
                previous?.epgChannelIds.orEmpty(), previous?.subscriptionExpiry,
                enabled = keepEnabled, reachable = true, autoDisabled = false
            )
            // Persist the source as soon as its portal session is verified. A process exit
            // during a slow catalogue load must not make a newly added source disappear.
            persistPortalSnapshot(initialSnapshot)
            suspend fun persistProgress() {
                snapshotMutex.withLock {
                    persistPortalSnapshot(SavedPortal(
                        enteredAddress, enteredMac, refreshedChannels, refreshedGenres, refreshedCatalogs,
                        previous?.epgChannelIds.orEmpty(), previous?.subscriptionExpiry,
                        enabled = keepEnabled, reachable = true, autoDisabled = false
                    ))
                }
            }
            ui.importStatuses = PortalSection.entries.associateWith { ImportStatus.Loading }
            coroutineScope {
                for (section in PortalSection.entries) launch {
                    val status = try {
                        if (section == PortalSection.LIVE) {
                            val (imported, labels) = coroutineScope {
                                val labels = async {
                                    try {
                                        source.loadGenres(token)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        emptyMap()
                                    }
                                }
                                val imported = source.loadChannels(token) { loaded, total ->
                                    withContext(Dispatchers.Main) {
                                        snapshotMutex.withLock {
                                            ui.importStatuses = ui.importStatuses +
                                                (section to ImportStatus.Progress(loaded, total))
                                        }
                                    }
                                }
                                imported to labels.await()
                            }
                            snapshotMutex.withLock {
                                refreshedChannels = if (imported.partial) {
                                    (imported.channels + refreshedChannels).distinctBy { it.id ?: it.command }
                                } else imported.channels
                                refreshedGenres = refreshedGenres + labels
                            }
                            if (imported.partial) ImportStatus.Partial(imported.channels.size)
                            else ImportStatus.Count(imported.channels.size)
                        } else {
                            val catalog = coroutineScope {
                                val groups = async {
                                    try {
                                        source.loadCatalogGroups(section, token)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                }
                                val firstPage = source.loadCatalog(section, token)
                                firstPage.copy(groups = groups.await().ifEmpty {
                                    previous?.catalogs?.get(section)?.groups.orEmpty()
                                })
                            }
                            val retainedCatalog = previous?.catalogs?.get(section)
                            val mergedCatalog = if (retainedCatalog == null) catalog else catalog.copy(
                                titles = (catalog.titles + retainedCatalog.titles).distinctBy {
                                    it.id ?: it.command ?: "${normalizedLabel(it.name)}:${it.year ?: 0}"
                                },
                                groups = catalog.groups.ifEmpty { retainedCatalog.groups }
                            )
                            snapshotMutex.withLock {
                                refreshedCatalogs = refreshedCatalogs + (section to mergedCatalog)
                            }
                            catalog.total?.let { ImportStatus.Count(it) }
                                ?: ImportStatus.Loaded(catalog.titles.size)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        when {
                            section != PortalSection.LIVE -> ImportStatus.Failed
                            error is PortalRequestException && error.status in listOf(401, 403) ->
                                ImportStatus.Error(R.string.portal_live_access_denied)
                            else -> ImportStatus.Error(R.string.portal_live_list_invalid)
                        }
                    }
                    snapshotMutex.withLock {
                        ui.importStatuses = ui.importStatuses + (section to status)
                    }
                    if (status is ImportStatus.Count || status is ImportStatus.Loaded || status is ImportStatus.Partial) {
                        persistProgress()
                    }
                }
            }
            val changed = ui.importStatuses.values.any {
                it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
            }
            if (!changed) {
                updatePortalStatus(sourceIdentity, reachable = true, enabled = if (activate) true else null)
                return false
            }
            val snapshot = SavedPortal(
                enteredAddress, enteredMac, refreshedChannels, refreshedGenres,
                refreshedCatalogs,
                savedSources.firstOrNull { it.identity == previous?.identity }?.epgChannelIds.orEmpty(),
                previous?.subscriptionExpiry ?: source.loadSubscriptionExpiry(token),
                enabled = keepEnabled,
                reachable = true,
                autoDisabled = false
            )
            val sameSource = snapshot.identity == storedSource?.identity
            val unchanged = previous != null &&
                previous.channels.map { channel ->
                    listOf(
                        channel.name, channel.command, channel.genreId, channel.genreName, channel.id,
                        channel.logo, channel.direct, channel.epgIdentity, channel.number
                    )
                } == snapshot.channels.map { channel ->
                    listOf(
                        channel.name, channel.command, channel.genreId, channel.genreName, channel.id,
                        channel.logo, channel.direct, channel.epgIdentity, channel.number
                    )
                } && previous.genres == snapshot.genres && previous.catalogs == snapshot.catalogs &&
                previous.subscriptionExpiry == snapshot.subscriptionExpiry
            if (unchanged) {
                val old = requireNotNull(previous)
                val unchangedSnapshot = SavedPortal(
                    old.address, old.mac, old.channels, old.genres, old.catalogs,
                    old.epgChannelIds, old.subscriptionExpiry,
                    enabled = activate || old.enabled || old.autoDisabled,
                    reachable = true,
                    autoDisabled = false
                )
                if (activate || sameSource) {
                    if (!sameSource) {
                        closeSeries()
                        closeGroup()
                        details = null
                        searchedVariants = emptyMap()
                        category = null
                        lastLiveCategory = null
                        channelScrollByCategory.clear()
                        categoryScroll.requestScrollToItem(0)
                        guideScroll.requestScrollToItem(0)
                        programmeChannel = null
                    }
                    liveSession = source to token
                    catalogSession = source to token
                    ui.catalogLoading = emptySet()
                    ui.catalogErrors = emptySet()
                    channels = unchangedSnapshot.channels
                    genres = unchangedSnapshot.genres
                    catalogs = unchangedSnapshot.catalogs
                    epgChannelIds = unchangedSnapshot.epgChannelIds
                    save(unchangedSnapshot, activate)
                } else {
                    updatePortalStatus(sourceIdentity, reachable = true)
                }
                AppDiagnostics.record(context, "portal_refresh_unchanged")
                val complete = ui.importStatuses.values.all { it is ImportStatus.Count || it is ImportStatus.Loaded }
                if (complete) markPortalFullRefresh(sourceIdentity)
                return complete
            }
            if (activate || sameSource) {
                if (!sameSource) {
                    closeSeries()
                    closeGroup()
                    details = null
                    searchedVariants = emptyMap()
                    category = null
                    lastLiveCategory = null
                    channelScrollByCategory.clear()
                    categoryScroll.requestScrollToItem(0)
                    guideScroll.requestScrollToItem(0)
                    programmeChannel = null
                }
                val session = source to token
                liveSession = session
                catalogSession = session
                ui.catalogLoading = emptySet()
                ui.catalogErrors = emptySet()
                channels = refreshedChannels
                genres = refreshedGenres
                catalogs = refreshedCatalogs
                epgChannelIds = snapshot.epgChannelIds
            } else {
                secondarySessions[snapshot.identity] = source
            }
            save(snapshot, activate)
            AppDiagnostics.record(context, "portal_refresh_completed")
            val complete = ui.importStatuses.values.all { it is ImportStatus.Count || it is ImportStatus.Loaded } &&
                ui.storageError == null
            if (complete) markPortalFullRefresh(sourceIdentity)
            return complete
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_refresh_failed:${error.javaClass.simpleName}")
            val identity = runCatching {
                SavedPortal(enteredAddress, enteredMac, emptyList(), emptyMap(), emptyMap()).identity
            }.getOrNull()
            if (identity != null) {
                try {
                    updatePortalStatus(identity, reachable = false)
                } catch (statusError: CancellationException) {
                    throw statusError
                } catch (_: Exception) {
                    AppDiagnostics.record(context, "portal_status_update_failed")
                }
            }
            ui.sourceError = when (error) {
                is InvalidPortalAddressException -> R.string.portal_invalid_address
                is PortalAuthenticationException -> R.string.portal_auth_failed
                is PortalHandshakeException -> R.string.portal_handshake_failed
                is IllegalArgumentException -> R.string.portal_invalid_mac
                else -> R.string.portal_error
            }
            return false
        }
    }

    /** Turns a portal on or off; turning it on refreshes it (TV switch and phone). */
    fun setPortalEnabled(source: SavedPortal, enabled: Boolean) {
        scope.launch {
            try {
                updatePortalStatus(source.identity, enabled = enabled)
                if (enabled) refreshPortal(source.address, source.mac, activate = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AppDiagnostics.record(context, "portal_toggle_failed")
                ui.sourceError = R.string.portal_error
            }
        }
    }

    /**
     * Changes address or MAC of a saved portal: the portal is loaded with the new details and the
     * old entry is removed once the new one was saved (from the phone).
     */
    fun editPortal(old: SavedPortal, newAddress: String, newMac: String) {
        if (connecting) return
        val wasActive = storedSource?.identity == old.identity
        connecting = true
        ui.batchSummary = null
        ui.sourceError = null
        scope.launch {
            try {
                refreshPortal(newAddress, newMac, activate = wasActive)
                val newIdentity = runCatching {
                    SavedPortal(newAddress, newMac, emptyList(), emptyMap(), emptyMap()).identity
                }.getOrNull()
                if (newIdentity != null && newIdentity != old.identity &&
                    savedSources.any { it.identity == newIdentity }
                ) removePortal(old.identity)
            } finally {
                connecting = false
            }
        }
    }

    fun importPortal() {
        editing = false
        val firstPortal = storedSource == null
        val enteredAddress = address.trim()
        val enteredMac = mac.trim()
        connecting = true
        ui.batchSummary = null
        scope.launch {
            try {
                val imported = refreshPortal(enteredAddress, enteredMac, activate = true)
                if (automaticCountries && ui.importStatuses.values.any {
                    it is ImportStatus.Count || it is ImportStatus.Loaded || it is ImportStatus.Partial
                }) ui.askForCountries = true
                if (firstPortal && imported && selected == Destination.SOURCES) selected = Destination.LIVE
            } finally {
                connecting = false
            }
        }
    }

    fun openSourceRefreshDialog() {
        ui.selectedRefreshSources = emptySet()
        ui.sourceRefreshDialogOpen = true
    }

    fun refreshSources(sources: List<SavedPortal>) {
        if (connecting || sources.isEmpty()) return
        connecting = true
        ui.batchSummary = null
        scope.launch {
            var completed = 0
            val unavailable = mutableListOf<String>()
            try {
                // Let the initial background check finish first. The UI stays usable and
                // the manual request is queued instead of silently discarded.
                while (ui.startupPortalRefresh != null || ui.startupPortalCheck != null) delay(200)
                for ((index, source) in sources.withIndex()) {
                    ui.batchProgress = (index + 1) to sources.size
                    if (refreshPortal(source.address, source.mac, activate = false)) completed++
                    if (savedSources.firstOrNull { it.identity == source.identity }?.reachable == false) {
                        unavailable += runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                            .ifBlank { "Portal" }
                    }
                }
                ui.batchSummary = completed to (sources.size - completed)
                val reachableCount = sources.count { source ->
                    savedSources.firstOrNull { it.identity == source.identity }?.reachable == true
                }
                ui.portalRefreshNotice = if (unavailable.isEmpty()) {
                    context.getString(R.string.portals_refresh_success, reachableCount)
                } else context.getString(R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", "))
                if (completed > 0 && automaticCountries) ui.askForCountries = true
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
            AppDiagnostics.record(context, "portal_batch_refresh_failed:${error.javaClass.simpleName}")
                ui.portalRefreshNotice = context.getString(R.string.portals_refresh_failed)
            } finally {
                ui.batchProgress = null
                connecting = false
            }
        }
    }

    LaunchedEffect(restoring, refreshPortalsAtStartup, ui.liveGroupsReady) {
        if (restoring || ui.startupPortalRefreshStarted || savedSources.isEmpty()) return@LaunchedEffect
        ui.startupPortalRefreshStarted = true
        val sources = savedSources.filter { it.enabled || it.autoDisabled }
        val unavailable = mutableListOf<String>()
        var fullRefreshPerformed = false
        try {
            for ((index, source) in sources.withIndex()) {
                if (refreshPortalsAtStartup) {
                    if (portalFullRefreshDue(source)) {
                        fullRefreshPerformed = true
                        ui.startupPortalRefresh = (index + 1) to sources.size
                        refreshPortal(source.address, source.mac, activate = false)
                    } else {
                        ui.startupPortalCheck = (index + 1) to sources.size
                        try {
                            sessionFor(source).openSession()
                            updatePortalStatus(source.identity, reachable = true)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            updatePortalStatus(source.identity, reachable = false)
                        }
                    }
                } else {
                    ui.startupPortalCheck = (index + 1) to sources.size
                    try {
                        sessionFor(source).openSession()
                        updatePortalStatus(source.identity, reachable = true)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        updatePortalStatus(source.identity, reachable = false)
                    }
                }
                if (savedSources.firstOrNull { it.identity == source.identity }?.reachable == false) {
                    unavailable += runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                        .ifBlank { "Portal" }
                }
                ui.startupPortalRefresh = null
                ui.startupPortalCheck = null
            }
            if (sources.isNotEmpty()) {
                val reachableCount = sources.count { source ->
                    savedSources.firstOrNull { it.identity == source.identity }?.reachable == true
                }
                if (fullRefreshPerformed) {
                    ui.portalRefreshNotice = if (unavailable.isEmpty()) {
                        context.getString(R.string.portals_refresh_success, reachableCount)
                    } else context.getString(R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", "))
                } else if (unavailable.isNotEmpty()) {
                    ui.portalRefreshNotice = context.getString(
                        R.string.portals_refresh_with_unavailable, reachableCount, unavailable.joinToString(", ")
                    )
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppDiagnostics.record(context, "startup_portal_refresh_failed:${error.javaClass.simpleName}")
            ui.portalRefreshNotice = context.getString(R.string.portals_refresh_failed)
        } finally {
            ui.startupPortalCheck = null
            ui.startupPortalRefresh = null
        }
    }

    fun loadMore(section: PortalSection) {
        val session = catalogSession ?: return
        val previous = catalogs[section] ?: return
        val page = previous.nextPage ?: return
        if (connecting || section in ui.catalogLoading) return
        ui.catalogLoading = ui.catalogLoading + section
        ui.catalogErrors = ui.catalogErrors - section
        scope.launch {
            try {
                val token = session.second ?: session.first.openSession()
                val catalog = session.first.loadCatalog(section, token, page, previous)
                if (catalogSession === session) {
                    catalogs = catalogs + (section to catalog)
                    storedSource?.let { saved ->
                        save(SavedPortal(
                            saved.address, saved.mac, channels, genres, catalogs, epgChannelIds,
                            saved.subscriptionExpiry, saved.enabled, saved.reachable, saved.autoDisabled
                        ))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (catalogSession === session) ui.catalogErrors = ui.catalogErrors + section
            } finally {
                if (catalogSession === session) ui.catalogLoading = ui.catalogLoading - section
            }
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                    playerPositionResetAt.set(SystemClock.elapsedRealtime())
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Smart home: play dims, pause switches the light on again (buffering does not count).
                LightAutomation.onPlayer(isPlaying, player?.playWhenReady == true, playbackAttempt?.selection?.section)
                if (isPlaying) {
                    sendTraktScrobble("start")
                    ui.pendingSeriesEpisode?.let { (series, episode) ->
                        ui.pendingSeriesEpisode = null
                        updateTrackedSeries(series, watchedEpisode = episode)
                    }
                    ui.pendingMovie?.let { movie ->
                        ui.pendingMovie = null
                        movieHistory = (listOf(movie) + movieHistory.filterNot { it.progressKey == movie.progressKey }).take(200)
                        progressPreferences?.edit { putString("movie_history", encodeMovieHistory(movieHistory)) }
                    }
                    playbackAttempt?.let { attempt ->
                        if (lastBadgedAttempt != attempt.mediaId) {
                            if (attempt.selection.section == PortalSection.LIVE) {
                                preferences?.edit { putString("last_live_channel", attempt.liveRestoreKey) }
                                val key = attempt.liveRestoreKey
                                val variants = liveGroups[attempt.channel]?.variants
                                if (key != null && !variants.isNullOrEmpty() && storage != null) {
                                    val snapshot = StartupChannel(
                                        key, attempt.selection.source, attempt.selection.id, startupChannelScope(),
                                        attempt.channel.name, variants.take(8).map { (item, _) ->
                                            val stream = item.value
                                            StartupStream(item.source.address, item.source.mac, stream.id, stream.command, stream.direct)
                                        }
                                    )
                                    scope.launch {
                                        try {
                                            storage.writeStartupChannel(snapshot)
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            // The normal catalogue restore remains available.
                                        }
                                    }
                                }
                            }
                            lastBadgedAttempt = attempt.mediaId
                            ui.playbackSourceBadge = attempt.mediaId to attempt.variants[attempt.index].label
                            if (attempt.index > 0) {
                                SmoothSwitch.markPlaying()
                                AppDiagnostics.record(context, "playback_source_badge index:${attempt.index}")
                            }
                        }
                    }
                    val openFullscreen = when (vodSection) {
                        null -> selected == null || selected == Destination.LIVE
                        PortalSection.MOVIES -> selected == Destination.MOVIES
                        PortalSection.SERIES -> selected == Destination.SERIES
                        PortalSection.LIVE -> false
                    }
                    if (starting != null && openFullscreen && playbackAttempt?.openFullscreenOnStart == true) fullscreen = true
                    starting = null
                } else {
                    savePlaybackPosition(onFailure = true)
                    if (player?.playWhenReady == false) sendTraktScrobble("pause")
                }
            }

            // A stream without any playable sound (none in the stream or none the device can decode)
            // counts as a failed start: the next portal is tried.
            fun checkAudio(tracks: Tracks) {
                val attempt = playbackAttempt ?: return
                val active = player ?: return
                if (active.currentMediaItem?.mediaId != attempt.mediaId || active.playbackState != Player.STATE_READY) return
                if (tracks.groups.isEmpty() || audioCheckedFor == attempt.mediaId) return
                audioCheckedFor = attempt.mediaId
                val audio = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
                if (audio.isNotEmpty()) {
                    val languages = audio.flatMap { group ->
                        (0 until group.length).mapNotNull { group.getTrackFormat(it).language?.substringBefore('-')?.lowercase() }
                    }.filter { it.isNotBlank() && it != "und" && it != "mul" && it != "zxx" }
                    val allowed = allowedAudioLanguages(countries)
                    // Only decided when the stream names its languages; unnamed tracks are kept.
                    if (attempt.selection.section == PortalSection.LIVE || languages.isEmpty() || allowed.isEmpty() ||
                        languages.any { it in allowed }
                    ) return
                    // No other portal left: it keeps playing, but says that only a foreign language is there.
                    if (attempt.index + 1 >= attempt.variants.size) {
                        android.widget.Toast.makeText(context, R.string.playback_foreign_audio_only, android.widget.Toast.LENGTH_LONG).show()
                        return
                    }
                    AppDiagnostics.record(context, "playback_foreign_audio")
                    scope.launch { failPlayback(attempt, R.string.playback_foreign_audio) }
                    return
                }
                AppDiagnostics.record(context, "playback_without_audio")
                // The next portal is tried; without one, playback ends and fullscreen closes.
                scope.launch { failPlayback(attempt, R.string.playback_no_audio) }
            }

            override fun onTracksChanged(tracks: Tracks) = checkAudio(tracks)

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) player?.currentTracks?.let(::checkAudio)
                val attempt = playbackAttempt ?: return
                if (playbackState == Player.STATE_READY && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    val key = attempt.progressKey
                    val active = player
                    val percent = key?.let { progressPreferences?.getFloat("trakt_percent:$it", -1f) } ?: -1f
                    if (key != null && active.duration > 0 && percent in 0.1f..100f) {
                        val target = (active.duration * percent / 100f).toLong()
                            .coerceIn(0L, (active.duration - 1_000L).coerceAtLeast(0L))
                        progressPreferences?.edit {
                            putLong(key, target)
                            putLong("duration:$key", active.duration)
                            putLong("progress_updated:$key", System.currentTimeMillis())
                            remove("trakt_percent:$key")
                        }
                        active.seekTo(target)
                    }
                }
                if (playbackState == Player.STATE_ENDED && player?.currentMediaItem?.mediaId == attempt.mediaId) {
                    // The end of a film counts as stop. An episode only when no next one follows (then
                    // stopPlayback below does it); a live stream that ends is replaced by another portal.
                    if (attempt.selection.section == PortalSection.MOVIES) LightAutomation.onStopped()
                    if (attempt.progressKey == null) {
                        if (attempt.selection.section == PortalSection.LIVE) AppDiagnostics.record(context, "playback_live_ended")
                        scope.launch { failPlayback(attempt, R.string.playback_error) }
                    } else {
                        progressPreferences?.edit {
                            remove(attempt.progressKey)
                            putBoolean("completed:${attempt.progressKey}", true)
                        }
                        if (attempt.selection.section == PortalSection.SERIES && nextEpisodeAction == null) {
                            stopPlayback()
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val item = player?.currentMediaItem
                val attempt = playbackAttempt ?: return
                if (item?.mediaId != attempt.mediaId) return
                val startingChannel = starting
                val uri = item.localConfiguration?.uri
                val formatError = error.errorCode in setOf(
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
                )
                if (uri != null && startingChannel != null && formatError &&
                    !ui.triedHls && !uri.path.orEmpty().endsWith(".m3u8", ignoreCase = true)
                ) {
                    ui.triedHls = true
                    scope.launch {
                        if (starting === startingChannel && player.currentMediaItem == item) {
                            Timeshift.stop()
                            val hls = item.buildUpon().setMimeType(MimeTypes.APPLICATION_M3U8).build()
                            if (attempt.progressKey == null) player.setMediaItem(hls)
                            else player.setMediaItem(hls, attempt.positionMs)
                            player.prepare()
                            player.playWhenReady = canPlay()
                        }
                    }
                    return
                }
                // A running Live-TV stream that fell behind its live window or lost the connection
                // for a moment is reloaded once instead of switching away from a good source.
                val now = SystemClock.elapsedRealtime()
                // With timeshift the player reads the local recording: when that recording stopped
                // (portal ended or refused the stream) a reload cannot help; while it still runs, a
                // decoder that choked (seen 30–60 s after a switch) restarts at its live edge.
                val recording = Timeshift.recorder
                val recordingFailed = recording != null && recording.problem != null
                val transient = !recordingFailed && (
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                        error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
                        recording != null && error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                    )
                if (attempt.selection.section == PortalSection.LIVE && startingChannel == null && transient &&
                    now - liveRetryAt > 60_000
                ) {
                    liveRetryAt = now
                    playerPositionResetAt.set(now)
                    AppDiagnostics.record(context, "playback_live_reload ${error.errorCodeName}" +
                        (Timeshift.describe()?.let { " $it" } ?: ""))
                    if (recording != null) Timeshift.goLive(player)
                    else {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    return
                }
                val httpStatus = (error.cause as? HttpDataSource.InvalidResponseCodeException)
                    ?.responseCode
                val message = when {
                    httpStatus == 401 || httpStatus == 403 -> R.string.playback_stream_denied
                    formatError -> R.string.playback_format_error
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        R.string.playback_stream_http_error
                    else -> R.string.playback_error
                }
                PlaybackReason.text = httpStatus?.let { "HTTP $it" } ?: error.errorCodeName.removePrefix("ERROR_CODE_")
                AppDiagnostics.record(context, "playback_error ${error.errorCodeName}" + (httpStatus?.let { " http:$it" } ?: "") +
                    (Timeshift.describe()?.let { " $it" } ?: ""))
                scope.launch { failPlayback(attempt, message) }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(playbackAttempt, starting, buffering) {
        val attempt = playbackAttempt
        if (attempt != null && (starting != null || buffering)) {
            delay(if (attempt.selection.section == PortalSection.LIVE) 12_000 else 25_000)
            if (canPlay() && (starting != null || player?.playWhenReady == true)) {
                PlaybackReason.text = "Timeout"
                AppDiagnostics.record(context, if (starting != null) "playback_start_timeout" else "playback_buffer_timeout")
                failPlayback(attempt, R.string.playback_timeout)
            }
        }
    }
    /**
     * Resolves the next Live-TV stream ahead of time, so a failover starts right away. Portal links
     * expire, so this is repeated as soon as the running stream shows trouble.
     */
    /** [trouble]: the running stream shows signs of trouble (not just the early link fetch). */
    fun prepareNextLiveVariant(attempt: PlaybackAttempt, trouble: Boolean = false) {
        if (attempt.selection.section != PortalSection.LIVE) return
        // Chosen by hand: no stand-in is prepared (it would never be used).
        if (!StreamSwitch.auto(context)) return
        val next = attempt.nextIndex()?.let(attempt.variants::get) ?: return
        if (trouble) SmoothSwitch.troubleAt = timeshiftNow()
        // Smooth switch: a stand-in from another portal (another account) is recorded in the
        // background while the running stream has trouble, so the switch can be seamless.
        val standbySource = next.sourceId?.takeIf {
            trouble && SmoothSwitch.enabled && it != attempt.variants[attempt.index].sourceId
        }
        if (standbySource != null) {
            if (SmoothSwitch.standby?.variant === next || SmoothSwitch.standbyPending === next) return
            SmoothSwitch.standbyPending = next
        } else {
            val ready = preparedLiveVariant
            if (ready != null && ready.variant === next && SystemClock.elapsedRealtime() - ready.preparedAt < 8_000) return
        }
        scope.launch {
            try {
                val stream = next.resolve()
                if (playbackAttempt === attempt) {
                    if (standbySource != null && !stream.substringBefore('?').endsWith(".m3u8", true)) {
                        SmoothSwitch.startStandby(context, next, standbySource, stream)
                        AppDiagnostics.record(context, "smooth_standby_started")
                    } else {
                        preparedLiveVariant = PreparedLiveVariant(next, stream, SystemClock.elapsedRealtime())
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Resolved again on demand if the running stream fails.
            } finally {
                if (SmoothSwitch.standbyPending === next) SmoothSwitch.standbyPending = null
            }
        }
    }

    LaunchedEffect(playbackAttempt, player) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val activePlayer = player ?: return@LaunchedEffect
        val live = attempt.selection.section == PortalSection.LIVE
        var hasPlayed = activePlayer.playbackState == Player.STATE_READY
        var bufferingSince: Long? = null
        var countedThisBuffer = false
        // Long buffering only counts within a few minutes: a stream that stalls once an hour is fine.
        val bufferEpisodes = ArrayDeque<Long>()
        val jumps = ArrayDeque<Long>()
        var lastPosition = -1L
        var positionMovedAt = SystemClock.elapsedRealtime()
        var lastFrames = -1
        var framesMovedAt = SystemClock.elapsedRealtime()
        var frameSampleAt = 0L
        var frameSample = 0
        var fluentVideo = false
        var playedMs = 0L
        var successRecorded = false
        var rewound = false
        fun fail(event: String, staleMs: Long = 0L) {
            AppDiagnostics.record(context, event)
            scope.launch { failPlayback(attempt, R.string.playback_timeout, block = true, staleMs = staleMs) }
        }
        while (playbackAttempt === attempt) {
            val now = SystemClock.elapsedRealtime()
            if (activePlayer.currentMediaItem?.mediaId != attempt.mediaId) { delay(250); continue }
            // The running stream recovered: the stand-in recorded in the background is dropped.
            if (SmoothSwitch.standby != null && timeshiftNow() - SmoothSwitch.troubleAt > 30_000) {
                SmoothSwitch.stopStandby()
                AppDiagnostics.record(context, "smooth_standby_dropped")
            }
            if (activePlayer.playbackState == Player.STATE_READY) hasPlayed = true
            while (bufferEpisodes.isNotEmpty() && now - bufferEpisodes.first() > STALL_WINDOW_MS) bufferEpisodes.removeFirst()
            while (jumps.isNotEmpty() && now - jumps.first() > STALL_WINDOW_MS) jumps.removeFirst()
            if (hasPlayed && canPlay() && activePlayer.playWhenReady &&
                activePlayer.playbackState == Player.STATE_BUFFERING
            ) {
                if (bufferingSince == null) {
                    bufferingSince = now
                    countedThisBuffer = false
                }
                if (now - bufferingSince >= 1_500) prepareNextLiveVariant(attempt, trouble = true)
                if (!countedThisBuffer && now - bufferingSince >= 4_000) {
                    countedThisBuffer = true
                    bufferEpisodes.addLast(now)
                    if (bufferEpisodes.size >= 3) return@LaunchedEffect fail("playback_repeated_buffering", now - bufferingSince)
                }
            } else if (activePlayer.playbackState != Player.STATE_BUFFERING) {
                bufferingSince = null
                countedThisBuffer = false
            }
            // Rewound with timeshift: the picture comes from the recording, and a switch would throw
            // the recording (and the spot in it) away, so the stream is only watched at live. Only
            // the viewer's own seeks and pauses count: a portal jumping back by itself also ends up behind live.
            val behind = Timeshift.behindLive(activePlayer) ?: 0L
            if (now - playerPositionResetAt.get() < 5_000 || !activePlayer.playWhenReady) rewound = behind > 6_000
            else if (behind < 3_000) rewound = false
            if (live && activePlayer.isPlaying && !rewound) {
                playedMs += 250
                val position = liveStreamPosition(activePlayer)
                val reset = now - playerPositionResetAt.get() < 5_000
                if (lastPosition >= 0 && !reset && position < lastPosition - 1_500) {
                    // The stream jumped back by itself: a source that keeps repeating the last seconds.
                    AppDiagnostics.record(context, "playback_jump_back ms:${lastPosition - position}")
                    jumps.addLast(now)
                    prepareNextLiveVariant(attempt, trouble = true)
                    // A jump of many seconds is a source replaying a whole stretch (seen: 55 s every
                    // minute): it switches at once instead of showing the same minute again.
                    if (jumps.size >= 2 || lastPosition - position >= LONG_JUMP_BACK_MS) {
                        return@LaunchedEffect fail("playback_stream_loop")
                    }
                }
                if (position != lastPosition) positionMovedAt = now
                lastPosition = position
                // Picture and sound stand still although the player reports playback.
                if (now - positionMovedAt >= 3_000) prepareNextLiveVariant(attempt, trouble = true)
                if (now - positionMovedAt >= FROZEN_LIMIT_MS) return@LaunchedEffect fail("playback_frozen", now - positionMovedAt)
                // Only the picture stands still (sound goes on).
                val videoSelected = activePlayer.currentTracks.isTypeSelected(C.TRACK_TYPE_VIDEO)
                val frames = activePlayer.videoDecoderCounters?.let { it.ensureUpdated(); it.renderedOutputBufferCount }
                if (videoSelected && frames != null && frameSampleAt == 0L) { frameSampleAt = now; frameSample = frames }
                if (videoSelected && frames != null && now - frameSampleAt >= 4_000) {
                    // Radio channels often show a still image with hardly any frames: only fluent
                    // video (10+ frames per second at some point) is watched for a frozen picture.
                    if (frames - frameSample >= 40) fluentVideo = true
                    frameSampleAt = now
                    frameSample = frames
                }
                if (videoSelected && frames != null && fluentVideo) {
                    if (frames != lastFrames) framesMovedAt = now
                    lastFrames = frames
                    if (now - framesMovedAt >= 3_000) prepareNextLiveVariant(attempt, trouble = true)
                    if (now - framesMovedAt >= FROZEN_LIMIT_MS) return@LaunchedEffect fail("playback_picture_frozen")
                } else framesMovedAt = now
                // A steady run is remembered: this portal is tried first for the channel next time.
                if (!successRecorded && playedMs >= STEADY_PLAY_MS && bufferEpisodes.isEmpty() && jumps.isEmpty()) {
                    successRecorded = true
                    val key = attempt.liveRestoreKey
                    val source = attempt.variants[attempt.index].let { it.streamKey ?: it.sourceId }
                    if (key != null && source != null) liveStreamHistory.recordSuccess(key, source)
                }
            } else {
                positionMovedAt = now
                framesMovedAt = now
                lastPosition = -1L
            }
            delay(250)
        }
    }
    LaunchedEffect(playbackAttempt, starting) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        if (starting != null || attempt.selection.section != PortalSection.LIVE) return@LaunchedEffect
        delay(750)
        if (player?.isPlaying != true) return@LaunchedEffect
        prepareNextLiveVariant(attempt)
    }
    LaunchedEffect(playbackSelection) {
        if (playbackSelection?.section == PortalSection.LIVE || playbackSelection == null) return@LaunchedEffect
        while (true) {
            delay(3_000)
            if (player?.isPlaying == true) savePlaybackPosition()
        }
    }
    LaunchedEffect(ui.playbackSourceBadge) {
        if (ui.playbackSourceBadge != null) {
            delay(4_000)
            ui.playbackSourceBadge = null
        }
    }

    val inGuide = selected == Destination.LIVE
    var guidePanel by remember { mutableStateOf(GuidePanel.GRID) }
    var fullscreenGuidePanel by remember { mutableStateOf(GuidePanel.GRID) }
    var playbackControlsVisible by remember { mutableStateOf(false) }
    var dismissPlaybackControls by remember { mutableIntStateOf(0) }
    var overlayInteraction by remember { mutableIntStateOf(0) }
    val guideNow by produceState(System.currentTimeMillis() / 1_000) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis() / 1_000
        }
    }
    fun guideSelection(channel: StalkerChannel): PortalProgramme? = resolveGuideProgramme(
        effectiveProgrammes(channel).orEmpty(),
        ui.guideProgramme?.takeIf { it.first === channel }?.second,
        guideNow
    )
    val guidePanelFocus = remember { FocusRequester() }
    val guideChannelFocus = remember { FocusRequester() }
    val guideWindow = rememberGuideWindow()
    val destinationFocusRequesters = remember {
        mainDestinations.associateWith { FocusRequester() }
    }
    fun openMainMenu() {
        if (selected == Destination.MOVIES || selected == Destination.SERIES) {
            ui.catalogueReturnTitle = null
            closeGroup()
            closeSeries()
            details = null
            myAreaSection = null
        }
        pendingVodCategory = null
        lastBack = 0L
        guidePanel = GuidePanel.MENU
        selected = Destination.LIVE
    }
    fun guideChannelKey(channel: StalkerChannel): String {
        val description = liveGroups[channel]?.description ?: describeChannel(channel, channel.genreName)
        return channelIdentity(channel) ?: if (description.country != UNKNOWN_COUNTRY) {
            "station:${description.country}:${description.normalizedName}"
        } else {
            "entry:${channel.id ?: channel.command}:${description.normalizedName}"
        }
    }
    fun restoreGuideChannel(channel: StalkerChannel) {
        val key = guideChannelKey(channel)
        val entry = liveCategories.entries.firstNotNullOfOrNull { (group, items) ->
            items.firstOrNull { guideChannelKey(it) == key }?.let { group to it }
        }
        if (entry == null) {
            programmeChannel = channel
        } else {
            category = entry.first
            lastLiveCategory = entry.first
            programmeChannel = entry.second
            val index = liveCategories.getValue(entry.first).indexOf(entry.second)
            if (index >= 0) guideScroll.requestScrollToItem(index)
        }
        ui.restoreLiveFocus = true
    }
    fun restoreGuideEntryFocus() {
        val savedKey = preferences?.getString("last_live_channel", null)
        val channel = playing.takeIf { vodSection == null } ?: savedKey?.let { key ->
            liveGroups.keys.firstOrNull { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
        }
        if (channel != null) restoreGuideChannel(channel) else ui.restoreLiveFocus = true
    }
    fun navigateGuide(input: GuideInput) {
        if (input in setOf(GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT) &&
            guidePanel == GuidePanel.CATEGORIES
        ) {
            destinationFocusRequesters.getValue(Destination.LIVE).requestFocus()
            return
        }
        guidePanel = guidePanel.afterProgrammeGuide(input)
    }
    fun showFullscreenPanel(panel: GuidePanel) {
        if (panel != fullscreenGuidePanel) ui.overlayScheduleFocused = false
        if (panel != GuidePanel.GRID) ui.fullscreenOverlayContent = panel
        fullscreenGuidePanel = panel
    }
    LaunchedEffect(
        fullscreen, fullscreenGuidePanel, guideAppearance.closeSeconds, overlayInteraction,
        ui.categoryMenu, vodSection, ui.watchingPrompt, ui.nextEpisodeSeconds, ui.skipIntroTo
    ) {
        if (!fullscreen) showFullscreenPanel(GuidePanel.GRID)
        else if ((vodSection == null || vodSection == PortalSection.SERIES && playbackSeries != null) &&
            fullscreenGuidePanel != GuidePanel.GRID && guideAppearance.closeSeconds > 0 &&
            ui.categoryMenu == null && !ui.watchingPrompt && ui.nextEpisodeSeconds == null && ui.skipIntroTo == null
        ) {
            delay(guideAppearance.closeSeconds * 1_000L)
            if (fullscreenGuidePanel != GuidePanel.GRID && !ui.watchingPrompt &&
                ui.nextEpisodeSeconds == null && ui.skipIntroTo == null
            ) showFullscreenPanel(GuidePanel.GRID)
        }
    }
    fun isMainMenuOpen() = selected == Destination.LIVE && guidePanel == GuidePanel.MENU && !fullscreen
    val mainMenuOpen = isMainMenuOpen()
    LaunchedEffect(mainMenuOpen) { if (!mainMenuOpen) lastBack = 0L }
    fun settingsPageOpen() = SmartHomePages.open || ui.epgSettingsOpen || ui.episodeSettingsOpen || ui.controlSettingsOpen ||
        ui.metadataSettingsOpen || ui.traktSettingsOpen || ui.kidsProtectionOpen || ui.termsOpen || ui.whatsNewOpen || ui.languageOpen || ui.guideAppearancePage != null || ui.remoteAccessOpen || ui.accentPaletteOpen
    fun openSettingsPage(open: () -> Unit) {
        ui.settingsPageOpenedAt = SystemClock.elapsedRealtime()
        open()
    }
    fun closeSettingsPages(): Boolean {
        // Smart home: from a light back to its state, from a state back to the section.
        if (SmartHomePages.back()) return true
        if (ui.kidsColorOpen) {
            ui.kidsColorOpen = false
            return true
        }
        if (ui.traktForKids && ui.traktSettingsOpen) {
            ui.traktSettingsOpen = false
            ui.traktForKids = false
            return true
        }
        val open = settingsPageOpen()
        ui.epgSettingsOpen = false
        ui.episodeSettingsOpen = false
        ui.controlSettingsOpen = false
        ui.metadataSettingsOpen = false
        ui.traktSettingsOpen = false
        ui.kidsProtectionOpen = false
        ui.termsOpen = false
        ui.whatsNewOpen = false
        ui.languageOpen = false
        ui.guideAppearancePage = null
        ui.remoteAccessOpen = false
        ui.accentPaletteOpen = false
        return open
    }
    fun openSettingsNow() {
        closeSettingsPages()
        val current = if (isMainMenuOpen()) lastFocused else selected ?: lastFocused
        settingsReturnDestination = current.takeUnless {
            it == Destination.SETTINGS || it == Destination.SOURCES || it == Destination.CATEGORIES
        } ?: Destination.LIVE
        // Settings open on their first section, focused in the section bar at the top; every
        // section starts again at its top row.
        settingsFocusMemory.reset()
        settingsSection = SettingsSection.entries.first()
        lastSettingsSection = SettingsSection.entries.first()
        guidePanel = GuidePanel.GRID
        lastBack = 0L
        selected = Destination.SETTINGS
        // A new update opens its window once (per version); later the settings open as usual.
        val updateTag = appUpdates.tag
        if (appUpdates.available && updateTag != null && preferences?.getString("update_window_shown", null) != updateTag) {
            preferences?.edit { putString("update_window_shown", updateTag) }
            settingsSection = SettingsSection.UPDATES
            lastSettingsSection = SettingsSection.UPDATES
            appUpdates.showUpdate()
        }
    }
    /** Runs [action] right away, or after the settings PIN when [flag] protects it (Einstellungsschutz). */
    fun withSettingsPin(flag: Boolean, unlock: Boolean = true, action: () -> Unit) {
        if (!parental.needsSettingsPin(flag)) return action()
        if (ui.pinRequest == null) ui.pinRequest = PinRequest(
            context.getString(R.string.pin_enter), null, check = parental::checkSettingsPin, length = parental.settingsPinLength
        ) {
            if (unlock) parental.settingsUnlocked = true
            action()
        }
    }
    fun openSettings(after: () -> Unit = {}) {
        if (parental.kidsMode || parental.needsSettingsPin(parental.protectSettings)) {
            val kids = parental.kidsMode
            if (ui.pinRequest == null) ui.pinRequest = PinRequest(
                context.getString(if (kids) R.string.kids_mode_exit else R.string.pin_enter), null,
                check = if (kids) parental::checkKidsPin else parental::checkSettingsPin,
                length = if (kids) parental.kidsExitPinLength else parental.settingsPinLength,
                remoteSetup = !kids
            ) {
                if (kids) parental.updateKidsMode(false) else parental.settingsUnlocked = true
                openSettingsNow()
                after()
            }
            return
        }
        openSettingsNow()
        after()
    }
    /** Opens "Per Handy einstellen" (e.g. from setting up a portal). */
    fun openRemoteAccess() {
        editing = false
        openSettings {
            withSettingsPin(parental.lockPortals || parental.lockCategories) { openSettingsPage { ui.remoteAccessOpen = true } }
        }
    }
    fun returnToCategories() {
        lastLiveCategory = category
        category = null
        ui.restoreLiveFocus = true
    }
    DisposableEffect(fullscreen) {
        onFullscreen(fullscreen)
        onDispose { if (fullscreen) onFullscreen(false) }
    }
    LaunchedEffect(lastBack) {
        if (lastBack != 0L) {
            delay(EXIT_CONFIRMATION_MS)
            lastBack = 0
        }
    }
    fun leaveFullscreen() {
        savePlaybackPosition()
        // Back from fullscreen returns to where the title was opened (guide, film, series),
        // never to the settings: those were only visited while it played.
        if (selected == null || selected == Destination.SETTINGS || selected == Destination.SOURCES ||
            selected == Destination.CATEGORIES
        ) {
            closeSettingsPages()
            when (vodSection) {
                PortalSection.MOVIES -> {
                    selected = Destination.MOVIES
                    if (details == null) playbackMovieTitle?.let { details = PortalSection.MOVIES to it }
                }
                PortalSection.SERIES -> selected = Destination.SERIES
                else -> selected = Destination.LIVE
            }
        }
        if (vodSection == PortalSection.SERIES && playbackSeries != null && selected == Destination.SERIES) {
            openedSeries = playbackSeries
            openedSeason = playbackSeason
            seasons = playbackSeasons
            episodeVariants = playbackEpisodeVariants
        }
        ui.restoreLiveFocus = vodSection == null && (selected == null || selected == Destination.LIVE)
        if (ui.restoreLiveFocus && selected == Destination.LIVE) {
            playing?.let(::restoreGuideChannel)
        }
        if (vodSection != null && cataloguePlaybackView == VodLiveView.HIDDEN) player?.pause()
        fullscreen = false
        lastBack = 0L
    }
    fun openCurrentFullscreen(): Boolean {
        if (playing == null || playbackError != null) return false
        guidePanel = GuidePanel.GRID
        lastBack = 0L
        fullscreen = true
        return true
    }
    fun requestAppExit() {
        val now = SystemClock.elapsedRealtime()
        if (now < ui.exitBlockedUntil) return
        when (exitConfirmation) {
            ExitConfirmation.NONE -> {
                savePlaybackPosition()
                onExit()
            }
            ExitConfirmation.DIALOG -> ui.exitDialogOpen = true
            ExitConfirmation.DOUBLE_BACK -> if (lastBack != 0L && now - lastBack <= EXIT_CONFIRMATION_MS) {
                savePlaybackPosition()
                onExit()
            } else {
                lastBack = now
            }
        }
    }
    /** Back from the top level of a page: to the start page, and from there out of the app. */
    fun leaveToStartPage() {
        val start = startDestination.takeIf { it in visibleDestinations }
            ?: visibleDestinations.firstOrNull { it in START_DESTINATIONS } ?: Destination.LIVE
        if (selected == start) {
            requestAppExit()
            return
        }
        lastFocused = start
        guidePanel = GuidePanel.GRID
        lastBack = 0L
        if (start == Destination.LIVE) restoreGuideEntryFocus()
        pendingVodCategory = start.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
        selected = start
    }
    fun navigateBack() {
        if (fullscreen) {
            if (ui.watchingPrompt) {
                stopPlayback()
                return
            }
            if (playbackControlsVisible) {
                dismissPlaybackControls++
                return
            }
            if (ui.skipIntroTo != null) {
                // Back only closes the skip-intro offer, for the rest of this episode; the
                // remote's left/right then seek again instead of moving between offer buttons.
                ui.skipIntroTo = null
                val closed = playbackAttempt?.mediaId?.let { it to Long.MAX_VALUE }
                if (ui.skipIsOutro) ui.outroOfferClosed = closed else ui.introOfferClosed = closed
                return
            }
            if (fullscreenGuidePanel != GuidePanel.GRID) {
                showFullscreenPanel(
                    if (vodSection == PortalSection.SERIES) fullscreenGuidePanel.afterSeriesOverlay(GuideInput.BACK)
                    else GuidePanel.GRID
                )
                return
            }
            leaveFullscreen()
        } else if (isMainMenuOpen()) {
            requestAppExit()
        } else if (selected == Destination.LIVE) {
            // Read the current destination here: a value captured earlier can be outdated and
            // made Back from the settings close the app.
            if (guidePanel == GuidePanel.GRID) leaveToStartPage()
            else guidePanel = GuidePanel.GRID
        } else if (selected == Destination.SETTINGS && closeSettingsPages()) {
            // Back from a settings page returns to the rows of its section (one step at a time).
            settingsContentFocusRequest++
        } else if (selected == Destination.SETTINGS && ui.settingsContentFocused) {
            // Back from the rows goes to the section bar on the left.
            settingsFocusRequesters[settingsSection]?.let { requester -> runCatching { requester.requestFocus() } }
        } else if (selected == Destination.SETTINGS) {
            ui.exitBlockedUntil = SystemClock.elapsedRealtime() + EXIT_CONFIRMATION_MS
            selected = settingsReturnDestination
            guidePanel = GuidePanel.GRID
            lastBack = 0L
            if (selected == Destination.LIVE) restoreGuideEntryFocus()
        } else if (selected == null && category != null) returnToCategories()
        else if (selected == Destination.SERIES && openedSeason != null) openedSeason = null
        else if (details != null && castReturns.lastOrNull()?.opened == details) {
            // One step back: from a title opened via the cast to the actor's filmography,
            // from there to the cast list and finally to the original title.
            val back = castReturns.removeAt(castReturns.lastIndex)
            closeSeries()
            selected = back.destination
            back.destination?.let { lastFocused = it }
            details = back.details
            ui.castOf = back.castOf
            ui.castPerson = back.person
        }
        else if (selected == Destination.SERIES && openedSeries != null) {
            closeSeries()
            details = null
        }
        else if (details != null && selected in listOf(Destination.SERIES, Destination.MOVIES)) {
            details = null
        }
        else if (selected == Destination.SOURCES || selected == Destination.CATEGORIES) selected = Destination.SETTINGS
        // Films and series always keep a category (or "Mein Bereich") open: closing it only
        // left an empty, black area. Back goes to the start page or leaves the app instead.
        else if (selected == Destination.MOVIES || selected == Destination.SERIES) leaveToStartPage()
        else openMainMenu()
    }
    ui.portalCategoriesFor?.let { source ->
        PortalCategoriesDialog(
            stringResource(R.string.portal_categories_title, portalName(source)), source
        ) { ui.portalCategoriesFor = null }
    }
    if (ui.expiryLeadPickerOpen) AlertDialog(
        onDismissRequest = { ui.expiryLeadPickerOpen = false },
        title = { Text(stringResource(R.string.expiry_lead_title)) },
        text = {
            val current = remember { FocusRequester() }
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(ExpiryLead.ALL) { lead ->
                    MenuButton(
                        expiryLeadLabel(context, lead),
                        if (lead == expiryLead) Modifier.fillMaxWidth().focusRequester(current) else Modifier.fillMaxWidth(),
                        selected = lead == expiryLead
                    ) {
                        changeExpiryLead(lead)
                        ui.expiryLeadPickerOpen = false
                    }
                }
            }
            LaunchedEffect(Unit) { withFrameNanos { }; runCatching { current.requestFocus() } }
        },
        confirmButton = {}
    )
    if (ui.exitDialogOpen) AlertDialog(
        onDismissRequest = { ui.exitDialogOpen = false },
        title = { Text(stringResource(R.string.exit_dialog_title)) },
        text = { Text(stringResource(R.string.exit_dialog_message)) },
        confirmButton = {
            MenuButton(stringResource(R.string.exit_dialog_confirm), Modifier) {
                ui.exitDialogOpen = false
                savePlaybackPosition()
                onExit()
            }
        },
        dismissButton = {
            val cancel = remember { FocusRequester() }
            MenuButton(stringResource(android.R.string.cancel), Modifier.focusRequester(cancel)) { ui.exitDialogOpen = false }
            // Cancel has focus first, so a further Back or OK press does not close the app by accident.
            LaunchedEffect(Unit) { runCatching { cancel.requestFocus() } }
        }
    )
    // Portals that run out soon (or did): a notice that has to be confirmed with OK or Back.
    val expiryConfirmations = remember(preferences) { ExpiryConfirmations(preferences) }
    var expiryConfirmedVersion by remember { mutableIntStateOf(0) }
    val expiryNotices = remember(savedSources, restoring, expiryConfirmedVersion) {
        if (restoring) emptyList() else pendingExpiryNotices(
            savedSources.filter { it.enabled || it.autoDisabled }, java.time.LocalDate.now(), expiryLead, ::portalName,
            expiryConfirmations::state
        )
    }
    if (expiryNotices.isNotEmpty() && !ui.exitDialogOpen && !ui.profilePickerOpen && ui.pinRequest == null && !kidsAccountActive) {
        fun confirmExpiry() {
            expiryConfirmations.confirm(expiryNotices)
            expiryConfirmedVersion++
        }
        AlertDialog(
            onDismissRequest = ::confirmExpiry,
            title = {
                Text(stringResource(
                    if (expiryNotices.all { it.state == ExpiryState.EXPIRED }) R.string.portal_expiry_notice_title_expired
                    else R.string.portal_expiry_notice_title
                ))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val format = DateTimeFormatter.ofPattern("dd.MM.uuuu")
                    expiryNotices.forEach { notice ->
                        val date = notice.expiry.format(format)
                        Text(when {
                            notice.daysLeft < 0 -> stringResource(R.string.portal_expiry_notice_expired, notice.name, date)
                            notice.daysLeft == 0L -> stringResource(R.string.portal_expiry_notice_today, notice.name)
                            else -> pluralStringResource(
                                R.plurals.portal_expiry_notice_days, notice.daysLeft.toInt(), notice.name, date, notice.daysLeft.toInt()
                            )
                        })
                    }
                    Text(
                        stringResource(R.string.portal_expiry_notice_hint),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                val ok = remember { FocusRequester() }
                MenuButton(stringResource(android.R.string.ok), Modifier.focusRequester(ok)) { confirmExpiry() }
                LaunchedEffect(Unit) { runCatching { ok.requestFocus() } }
            }
        )
    }
    var playerControlsRequest by remember { mutableIntStateOf(0) }
    fun changeLiveChannel(step: Int): Boolean {
        // Current list, not the one from when fullscreen was opened (empty after a fullscreen start).
        val liveCategories = currentLiveCategories.value
        val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
            .filter { visibleCategory(PortalSection.LIVE, it, it) }
            .sortedBy { categoryNames.values.indexOf(it) }) { it }
        val currentChannel = if (vodSection == null) listedLiveChannel(starting ?: playing) else null
        val group = guideChannelCategory(category, groups, liveCategories, currentChannel)
        val channels = liveCategories[group].orEmpty()
        val index = channels.indexOf(currentChannel)
        if (channels.isEmpty()) return false
        val next = if (index < 0) {
            if (step > 0) channels.first() else channels.last()
        } else channels[Math.floorMod(index + step, channels.size)]
        category = group
        selected = Destination.LIVE
        playChannel(next, openFullscreenOnStart = fullscreen)
        // Every switch in fullscreen shows the channel info at the top left (it takes no focus,
        // so up/down keep switching while it is visible).
        if (fullscreen) startupInfoVisible = true
        return true
    }
    fun runMappedControl(input: GuideInput): Boolean {
        val action = controlBindings[input] ?: ControlAction.DEFAULT
        val panel = when (action) {
            ControlAction.CHANNELS -> GuidePanel.CHANNELS
            ControlAction.CATEGORIES -> GuidePanel.CATEGORIES
            ControlAction.MENU -> GuidePanel.MENU
            ControlAction.DETAILS -> GuidePanel.DETAILS
            else -> null
        }
        if (panel != null) {
            if (fullscreen && (vodSection == null || vodSection == PortalSection.SERIES && panel != GuidePanel.DETAILS)) {
                if (panel == GuidePanel.DETAILS) {
                    programmeChannel = playing
                    ui.guideProgramme = null
                }
                showFullscreenPanel(panel)
                return true
            }
            if (selected == Destination.LIVE && !fullscreen && panel != GuidePanel.CHANNELS && panel != GuidePanel.DETAILS) {
                guidePanel = panel
                return true
            }
            return false
        }
        return when (action) {
            ControlAction.BACK -> { navigateBack(); true }
            ControlAction.FULLSCREEN -> if (fullscreen) { leaveFullscreen(); true } else openCurrentFullscreen()
            ControlAction.PLAY_PAUSE -> player?.takeIf { playing != null }?.let {
                if (it.playWhenReady) it.pause() else it.play()
                true
            } ?: false
            ControlAction.STOP -> if (playing != null || starting != null) { stopPlayback(); true } else false
            ControlAction.CONTROLS -> if (fullscreen) { playerControlsRequest++; true } else false
            ControlAction.NEXT_CHANNEL -> changeLiveChannel(1)
            ControlAction.PREVIOUS_CHANNEL -> changeLiveChannel(-1)
            else -> false
        }
    }
    val longBack by rememberUpdatedState<() -> Boolean>({
        !ui.watchingPrompt && !playbackControlsVisible && !ui.controlSettingsOpen && ui.guideAppearancePage == null &&
            (runMappedControl(GuideInput.LONG_BACK) ||
                if (fullscreen && fullscreenGuidePanel != GuidePanel.GRID) {
                    showFullscreenPanel(GuidePanel.GRID)
                    true
                } else !fullscreen && openCurrentFullscreen())
    })
    SideEffect {
        // Navigation sounds while browsing; plain fullscreen video stays silent so seeking
        // with left and right does not click.
        registerKeySounds(
            keySounds && (!fullscreen || playbackControlsVisible || ui.watchingPrompt || ui.skipIntroTo != null ||
                fullscreenGuidePanel != GuidePanel.GRID)
        )
    }
    val screenBack by rememberUpdatedState<(Boolean) -> Boolean>({ released ->
        val handled = fullscreen && (
            playbackControlsVisible || ui.watchingPrompt || ui.skipIntroTo != null || fullscreenGuidePanel != GuidePanel.GRID
        ) || !fullscreen && (
            isMainMenuOpen() || selected == Destination.MOVIES || selected == Destination.SERIES
        )
        if (handled && released) navigateBack()
        handled
    })
    val playbackInput by rememberUpdatedState<() -> Unit>({
        if (vodSection == PortalSection.SERIES && playing != null && !ui.watchingPrompt) {
            ui.interruptedEpisode = true
            silentEpisodes = 0
        }
    })
    DisposableEffect(Unit) {
        registerLongBack { longBack() }
        registerScreenBack { released -> screenBack(released) }
        registerPlaybackInput { playbackInput() }
        onDispose {
            registerLongBack(null)
            registerScreenBack(null)
            registerPlaybackInput(null)
            registerPromptInput(null)
        }
    }
    BackHandler(onBack = ::navigateBack)
    // Hiding, ordering and locking categories needs the settings PIN when they are protected.
    val lockedCategoryMenu = ui.categoryMenu?.takeIf { parental.needsSettingsPin(parental.lockCategories) }
    LaunchedEffect(lockedCategoryMenu) {
        val pending = lockedCategoryMenu ?: return@LaunchedEffect
        ui.categoryMenu = null
        withSettingsPin(true, unlock = false) { ui.categoryMenu = pending }
    }
    if (lockedCategoryMenu == null) ui.categoryMenu?.let { (section, first) ->
        val defaultGroups = if (section == PortalSection.LIVE) {
            allLiveCategories.keys.sortedBy { categoryNames.values.indexOf(it) }.map { PortalGroup(it, it) }
        } else defaultVodCategoryOrder(section)
        val groups = withCategoryOrder(section, defaultGroups) { it.id }
        CategoryDialog(
            groups, first,
            isHidden = { categoryKey(section, it.id) in hiddenCategories },
            onApply = { ids, hidden ->
                setCategoryVisibility(section, ids, hidden)
                ui.categoryMenu = null
            },
            onMove = { id, direction ->
                val order = groups.map { it.id }.moveCategory(id, direction)
                val key = categoryOrderKey(section)
                categoryOrders[key] = order
                preferences?.edit { putString(key, org.json.JSONArray(order).toString()) }
            },
            onDismiss = { ui.categoryMenu = null },
            isLocked = { categoryKey(section, it.id) in parental.lockedCategories },
            onToggleLock = if (parental.hasPin) { group ->
                val key = categoryKey(section, group.id)
                if (key in parental.lockedCategories) {
                    ui.pinRequest = PinRequest(context.getString(R.string.pin_enter), null, check = parental::checkPin, length = parental.pinLength) {
                        parental.toggleLocked(key)
                    }
                } else parental.toggleLocked(key)
            } else null
        )
    }
    // Leaving the settings locks the protected settings again.
    LaunchedEffect(selected) {
        if (selected != Destination.SETTINGS && selected != Destination.SOURCES && selected != Destination.CATEGORIES) {
            parental.settingsUnlocked = false
        }
    }
    // ---- Accounts ----
    LaunchedEffect(kidsAccountActive) { onAccountChange(kidsAccountActive) }
    // Without a PIN the kids account cannot be protected: it goes with the PIN.
    LaunchedEffect(parental.hasPin) { if (!parental.hasPin && profiles.hasKids) profiles.removeKids() }
    LaunchedEffect(Unit) {
        if (ui.profileStartChecked) return@LaunchedEffect
        ui.profileStartChecked = true
        if (!profiles.hasKids) return@LaunchedEffect
        when (profiles.startProfile) {
            StartProfile.KIDS -> parental.updateKidsMode(true)
            // Admin (with PIN) or choose: nothing is usable before the account is confirmed.
            StartProfile.ADMIN, StartProfile.ASK -> {
                parental.updateKidsMode(true)
                ui.profilePickerOpen = true
                if (profiles.startProfile == StartProfile.ADMIN) ui.pinRequest = PinRequest(
                    context.getString(R.string.profile_admin_pin), null,
                    check = parental::checkKidsPin, length = parental.kidsExitPinLength
                ) {
                    parental.updateKidsMode(false)
                    ui.profilePickerOpen = false
                }
            }
        }
    }
    fun switchAccount(kids: Boolean) {
        if (kids != parental.kidsMode) stopPlayback()
        parental.updateKidsMode(kids)
        parental.settingsUnlocked = false
        ui.profilePickerOpen = false
        closeSettingsPages()
        if (selected == Destination.SETTINGS || selected == Destination.SOURCES || selected == Destination.CATEGORIES) {
            selected = Destination.LIVE
        }
    }
    if (ui.profilePickerOpen && profiles.hasKids) ProfilePicker(
        profiles.kidsName.orEmpty(),
        onAdmin = {
            if (ui.pinRequest == null) ui.pinRequest = PinRequest(
                context.getString(R.string.profile_admin_pin), null,
                check = parental::checkKidsPin, length = parental.kidsExitPinLength
            ) { switchAccount(false) }
        },
        onKids = { switchAccount(true) },
        onExit = onExit
    )
    // Closing the dialog must not hand the focus to a section of the bar (that would switch the
    // section): the Kinderschutz page keeps it.
    fun closeKidsNameDialog() {
        ui.kidsNameDialog = false
        ui.settingsPageOpenedAt = SystemClock.elapsedRealtime()
        settingsContentFocusRequest++
    }
    if (ui.kidsNameDialog) ProfileNameDialog(
        profiles.kidsName.orEmpty(),
        onSave = { name ->
            profiles.createKids(name)
            closeKidsNameDialog()
        },
        onDismiss = ::closeKidsNameDialog
    )
    ui.sharedPinQuestion?.let { question ->
        AlertDialog(
            onDismissRequest = { ui.sharedPinQuestion = null },
            title = { Text(stringResource(R.string.pin_same_title)) },
            text = { Text(question.text) },
            confirmButton = {
                val same = remember { FocusRequester() }
                MenuButton(stringResource(R.string.pin_same), Modifier.focusRequester(same)) {
                    ui.sharedPinQuestion = null
                    question.onShared()
                }
                LaunchedEffect(Unit) { runCatching { same.requestFocus() } }
            },
            dismissButton = {
                MenuButton(stringResource(R.string.pin_own), Modifier) {
                    ui.sharedPinQuestion = null
                    question.onOwn()
                }
            }
        )
    }
    // Live-TV without the automatic switch: which stream to play (at the start, or after one failed).
    ui.streamChoice?.let { attempt ->
        StreamChoiceDialog(
            attempt.variants.map { it.label }, attempt.index, ui.streamChoiceFailed,
            onPick = { index ->
                ui.streamChoice = null
                playbackSelection = attempt.selection
                runPlayback(attempt.copy(index = index, round = 0))
            },
            onDismiss = {
                ui.streamChoice = null
                if (!ui.streamChoiceFailed) stopPlayback()
            }
        )
    }
    ui.pinRequest?.let { request ->
        // One phone connection for all steps of a PIN change (current PIN, then the new one); each
        // step is a dialog of its own (key), so nothing of the previous step is kept.
        val pinScope = rememberCoroutineScope()
        val phoneLink = remember { PhonePinLink(pinScope) }
        DisposableEffect(phoneLink) { onDispose { phoneLink.end() } }
        key(request) { PinDialog(
            request.title, request.message, confirmTwice = request.newPin, length = request.length,
            phoneLink = phoneLink,
            onRemoteSetup = if (request.remoteSetup) { { ui.pinRequest = null; ui.remoteAccessStandalone = true } } else null,
            onPin = { pin ->
                val ok = request.check(pin)
                if (ok) {
                    ui.pinRequest = null
                    request.onSuccess(pin)
                }
                ok
            },
            onDismiss = { ui.pinRequest = null }
        ) }
    }
    LaunchedEffect(ui.watchingPrompt) {
        if (ui.watchingPrompt) {
            delay(30_000)
            if (ui.watchingPrompt) stopPlayback()
        }
    }
    if (ui.watchingPrompt && !fullscreen) AlertDialog(
        onDismissRequest = ::stopPlayback,
        title = { Text(stringResource(R.string.series_still_watching)) },
        text = { Text(stringResource(R.string.series_still_watching_timeout)) },
        confirmButton = { MenuButton(stringResource(R.string.play_next_episode), Modifier) { advanceEpisode(confirmed = true) } },
        dismissButton = { MenuButton(stringResource(R.string.stop_playback), Modifier, onClick = ::stopPlayback) }
    )
    val guideAppearanceSettingsPage: @Composable (GuideAppearancePage) -> Unit = { page ->
        GuideAppearanceDialog(page, guideAppearance, onChange = {
            guideAppearance = it
            it.save(preferences)
        }, onDismiss = { ui.guideAppearancePage = null })
    }
    val controlSettingsPage: @Composable () -> Unit = { ControlMappingDialog(
        bindings = controlBindings,
        onChange = { input, action ->
            controlBindings[input] = action
            preferences?.edit { putString("control_${input.name}", action.name) }
        },
        onReset = {
            controlBindings.clear()
            preferences?.edit { GuideInput.entries.forEach { remove("control_${it.name}") } }
        },
        onDismiss = { ui.controlSettingsOpen = false }
    ) }
    // Remote settings from a phone: reads and changes the same states as the settings on the device.
    val remoteHandlerNow = object : RemoteHandler {
        fun text(id: Int) = context.getString(id)
        /** Label of a "Name: value" string without its value. */
        fun name(id: Int) = context.getString(id, "").trimEnd(' ', ':')
        fun <T : Enum<T>> options(values: List<T>, label: (T) -> Int) =
            values.map { RemoteOption(it.name, text(label(it))) }

        fun entries(): List<RemoteEntry> {
            val general = text(R.string.settings_general)
            val library = text(R.string.settings_library)
            val guide = text(R.string.destination_guide)
            val appearance = text(R.string.settings_appearance)
            val playback = text(R.string.settings_playback)
            val controls = text(R.string.settings_controls)
            val parentalSection = text(R.string.settings_parental)
            fun toggle(id: String, section: String, label: String, get: () -> Boolean, key: String, set: (Boolean) -> Unit) =
                RemoteEntry.Toggle(id, section, label, get) { value ->
                    set(value)
                    preferences?.edit { putBoolean(key, value) }
                }
            fun number(id: String, section: String, label: String, min: Int, max: Int, step: Int, unit: String,
                       get: () -> Int, key: String, set: (Int) -> Unit) =
                RemoteEntry.Number(id, section, label, min, max, step, unit, get) { value ->
                    set(value)
                    preferences?.edit { putInt(key, value) }
                }
            val vodViews = VodLiveView.entries
            return buildList {
                add(toggle("showBack", general, text(R.string.show_touch_back_button), { showSettingsBackButton },
                    "show_settings_back_button") { showSettingsBackButton = it })
                add(toggle("keySounds", general, text(R.string.key_sounds), { keySounds }, "key_sounds") { keySounds = it })
                add(RemoteEntry.Choice("exitConfirmation", general, name(R.string.exit_confirmation_setting),
                    options(ExitConfirmation.entries) { it.label }, { exitConfirmation.name }
                ) { value ->
                    exitConfirmation = ExitConfirmation.valueOf(value)
                    preferences?.edit { putString("exit_confirmation", value) }
                })
                add(RemoteEntry.Choice("startPage", general, name(R.string.start_destination_setting),
                    options(START_DESTINATIONS) { it.label }, { startDestination.name }
                ) { value ->
                    startDestination = START_DESTINATIONS.firstOrNull { it.name == value } ?: Destination.LIVE
                    preferences?.edit { putString("start_destination", startDestination.name) }
                })
                // App language: the app is built again in the new language (the phone reconnects).
                add(RemoteEntry.Choice("language", general, text(R.string.app_language),
                    listOf(RemoteOption("system", text(R.string.app_language_system_short))) +
                        APP_LANGUAGES.map { RemoteOption(it.tag, "${it.flag} ${it.name}") },
                    { chosenLanguage(context) ?: "system" }
                ) { value ->
                    saveLanguage(context, value.takeUnless { it == "system" })
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        (context as? android.app.Activity)?.recreate()
                    }, 800)
                })
                add(toggle("inputRemote", general, text(R.string.input_remote_only),
                    { preferences?.getBoolean("input_remote_only", false) == true }, "input_remote_only") { })
                add(toggle("searchHistory", general, text(R.string.search_history_setting),
                    { preferences?.getBoolean("search_history_enabled", true) != false }, "search_history_enabled") { on ->
                    if (!on) preferences?.let(::clearSearchHistories)
                })
                add(RemoteEntry.Choice("searchHistorySize", general,
                    context.getString(R.string.search_history_size, 0).substringBefore(':').trim(),
                    SEARCH_HISTORY_SIZES.map { RemoteOption(it.toString(), it.toString()) },
                    { (preferences?.getInt("search_history_size", SEARCH_HISTORY_DEFAULT_SIZE) ?: SEARCH_HISTORY_DEFAULT_SIZE).toString() }
                ) { value -> preferences?.edit { putInt("search_history_size", value.toInt()) } }
                    .shownIf("searchHistory", true))
                add(RemoteEntry.Action("searchHistoryClear", general, text(R.string.clear_search_history),
                    text(R.string.clear_search_history)) {
                    preferences?.let(::clearSearchHistories)
                    text(R.string.search_history_cleared)
                }.shownIf("searchHistory", true))
                // Start with the device and on waking.
                addAll(autoStartRemoteEntries(context, general))
                // Playing the last channel (and fullscreen) only exists with Live-TV as the start page.
                if (startDestination == Destination.LIVE) {
                    add(toggle("lastChannel", general, text(R.string.start_last_channel), { playLastChannelAtStartup },
                        "play_last_channel_at_startup") { playLastChannelAtStartup = it })
                    if (playLastChannelAtStartup) add(toggle("startFullscreen", general, text(R.string.start_in_fullscreen),
                        { startInFullscreen }, "start_in_fullscreen") { startInFullscreen = it })
                }
                add(toggle("portalRefresh", library, text(R.string.portal_refresh_at_start), { refreshPortalsAtStartup },
                    "refresh_portals_at_startup") { refreshPortalsAtStartup = it })
                add(RemoteEntry.Choice("expiryLead", library, text(R.string.expiry_lead_title),
                    ExpiryLead.ALL.map { RemoteOption(it.key, expiryLeadLabel(context, it)) }, { expiryLead.key }
                ) { value -> changeExpiryLead(ExpiryLead.parse(value)) })
                add(toggle("autoDisable", library, text(R.string.auto_disable_unavailable_portals),
                    { autoDisableUnavailablePortals }, "auto_disable_unavailable_portals") { autoDisableUnavailablePortals = it })
                add(toggle("merge", library, text(R.string.merge_sources), { mergeSources }, "mergeSources") {
                    if (it != mergeSources) {
                        mergeSources = it
                        closeGroup()
                        closeSeries()
                        details = null
                        searchedVariants = emptyMap()
                        category = null
                    }
                })
                add(toggle("autoCountries", library, text(R.string.auto_country_prompt), { automaticCountries },
                    "autoCountries") { automaticCountries = it })
                storedSource?.identity?.takeIf { !mergeSources }?.let { activeId ->
                    add(RemoteEntry.Toggle("mergePortalChannels", library, text(R.string.merge_portal_channels),
                        { activeId in mergedIndividualPortals }) { value ->
                        mergedIndividualPortals = if (value) mergedIndividualPortals + activeId else mergedIndividualPortals - activeId
                        preferences?.edit { putStringSet("merged_individual_portals", mergedIndividualPortals) }
                        category = null
                    })
                }
                add(RemoteEntry.Action("refreshAll", library, text(R.string.refresh_all_sources), text(R.string.remote_action_refresh)) {
                    if (connecting || restoring) text(R.string.remote_msg_portals_loading_now) else if (savedSources.isEmpty()) text(R.string.remote_msg_no_portal)
                    else { refreshSources(savedSources); text(R.string.remote_msg_all_refreshing) }
                })
                add(RemoteEntry.Choice("epgSource", guide, text(R.string.remote_epg_source), listOf(
                    RemoteOption("PROVIDER", text(R.string.epg_provider)), RemoteOption("CUSTOM", text(R.string.epg_custom))
                ), { if (useXmlTv) "CUSTOM" else "PROVIDER" }) {
                    useXmlTv = it == "CUSTOM"
                    preferences?.edit { putBoolean("xmltv_enabled", useXmlTv) }
                })
                xmlTvSources.forEachIndexed { index, url ->
                    add(RemoteEntry.Action("epgRemove$index", guide, "${index + 1}. " + (guideSourceUri(url)?.host ?: url),
                        text(R.string.epg_remove_source)) {
                        if (ui.epgSourceSaving) text(R.string.remote_msg_wait) else { saveEpgSources(xmlTvSources.filterNot { it == url }); text(R.string.remote_msg_source_removed) }
                    }.shownIf("epgSource", "CUSTOM"))
                }
                add(RemoteEntry.Input("epgAdd", guide, text(R.string.epg_add_source), text(R.string.epg_source_url), false,
                    text(R.string.remote_action_add)) { value ->
                    val source = guideSourceUri(value.trim())?.toASCIIString()
                    when {
                        source == null -> text(R.string.remote_msg_address_invalid)
                        source in xmlTvSources -> text(R.string.remote_msg_source_exists)
                        ui.epgSourceSaving || !xmlTvSourcesLoaded -> text(R.string.remote_msg_wait)
                        else -> { saveEpgSources(xmlTvSources + source); text(R.string.remote_msg_source_added) }
                    }
                }.shownIf("epgSource", "CUSTOM"))
                add(toggle("epgAtStart", guide, "EPG: " + text(R.string.epg_refresh_at_start), { epgAtStartup },
                    "epg_at_startup") { epgAtStartup = it })
                add(RemoteEntry.Choice(
                    "epgRefresh", guide, text(R.string.remote_epg_refresh_auto),
                    listOf(0, 1, 3, 6, 12, 24).map {
                        RemoteOption(it.toString(), if (it == 0) text(R.string.subtitle_off) else context.getString(R.string.remote_epg_every_hours, it))
                    }, { epgRefreshHours.toString() }
                ) {
                    epgRefreshHours = it.toInt()
                    preferences?.edit { putInt("epg_refresh_hours", epgRefreshHours) }
                })
                add(RemoteEntry.Choice(
                    "epgHorizon", guide, text(R.string.remote_epg_horizon),
                    listOf(6, 12, 24, 48, 72, 168).map {
                        RemoteOption(it.toString(), if (it < 24) context.getString(R.string.remote_epg_hours, it) else if (it == 24) text(R.string.remote_epg_one_day) else context.getString(R.string.remote_epg_days, it / 24))
                    }, { epgHorizonHours.toString() }
                ) {
                    epgHorizonHours = it.toInt()
                    preferences?.edit { putInt("epg_horizon_hours", epgHorizonHours) }
                    epgRefreshRequest++
                })
                add(RemoteEntry.Action("epgRefreshNow", guide,
                    when {
                        ui.epgUpdating -> text(R.string.remote_epg_updating)
                        ui.epgUpdateFailed || ui.epgSourceWriteFailed -> text(R.string.epg_refresh_failed)
                        else -> text(R.string.remote_epg_update)
                    }, text(R.string.epg_refresh_now)) {
                    if (ui.epgUpdating) text(R.string.remote_msg_already_running) else if (useXmlTv && xmlTvSources.isEmpty()) text(R.string.remote_msg_add_xmltv_first)
                    else { epgRefreshRequest++; text(R.string.remote_msg_epg_updating) }
                })
                // Appearance of the guide and of the fullscreen channel list.
                fun appearance(change: (GuideAppearance) -> GuideAppearance) {
                    guideAppearance = change(guideAppearance)
                    guideAppearance.save(preferences)
                }
                val sizes = options(GuideSize.entries) { it.label }
                val grid = text(R.string.guide_grid_appearance)
                val list = text(R.string.guide_channel_list_appearance)
                fun size(id: String, section: String, label: Int, get: (GuideAppearance) -> GuideSize,
                         set: (GuideAppearance, GuideSize) -> GuideAppearance) =
                    RemoteEntry.Choice(id, section, text(label), sizes, { get(guideAppearance).name }) { value ->
                        appearance { set(it, GuideSize.valueOf(value)) }
                    }
                fun shown(id: String, section: String, label: Int, get: (GuideAppearance) -> Boolean,
                          set: (GuideAppearance, Boolean) -> GuideAppearance) =
                    RemoteEntry.Toggle(id, section, text(label), { get(guideAppearance) }) { value -> appearance { set(it, value) } }
                add(size("guideRows", grid, R.string.guide_row_size, { it.rowSize }) { a, v -> a.copy(rowSize = v) })
                add(size("guideText", grid, R.string.guide_text_size, { it.textSize }) { a, v -> a.copy(textSize = v) })
                add(size("guidePreviewSize", grid, R.string.guide_preview_size, { it.previewSize }) { a, v -> a.copy(previewSize = v) })
                add(shown("guidePreview", grid, R.string.guide_preview_visible, { it.showPreview }) { a, v -> a.copy(showPreview = v) })
                add(shown("guideNumbers", grid, R.string.guide_numbers_visible, { it.showNumbers }) { a, v -> a.copy(showNumbers = v) })
                add(shown("guideLogos", grid, R.string.guide_logos_visible, { it.showLogos }) { a, v -> a.copy(showLogos = v) })
                add(RemoteEntry.Choice("guideNowColor", grid, text(R.string.guide_now_line_color),
                    options(GuideNowColor.entries) { it.label }, { guideAppearance.nowColor.name }
                ) { value -> appearance { it.copy(nowColor = GuideNowColor.valueOf(value)) } })
                add(RemoteEntry.Choice("guideVeil", grid, text(R.string.guide_veil),
                    options(GuideVeil.entries) { it.label }, { guideAppearance.veil.name }
                ) { value -> appearance { it.copy(veil = GuideVeil.valueOf(value)) } })
                add(RemoteEntry.Choice("guideVeilStrength", grid, text(R.string.guide_veil_strength),
                    GUIDE_VEIL_STRENGTHS.map { RemoteOption(it.toString(), "$it %") }, { guideAppearance.veilStrength.toString() }
                ) { value -> appearance { it.copy(veilStrength = value.toInt()) } })
                add(size("listWidth", list, R.string.guide_list_width, { it.listWidth }) { a, v -> a.copy(listWidth = v) })
                add(shown("listSchedule", list, R.string.guide_show_schedule, { it.showSchedule }) { a, v -> a.copy(showSchedule = v) })
                add(shown("listDescription", list, R.string.guide_show_description, { it.showDescription }) { a, v ->
                    a.copy(showDescription = v, showSchedule = true)
                })
                add(shown("listProgress", list, R.string.guide_show_progress, { it.showProgress }) { a, v -> a.copy(showProgress = v) })
                add(shown("listKeep", list, R.string.guide_keep_list, { it.keepChannelList }) { a, v -> a.copy(keepChannelList = v) })
                add(RemoteEntry.Choice("listOpacity", list, text(R.string.remote_list_opacity),
                    listOf(60, 70, 80, 90, 100).map { RemoteOption(it.toString(), "$it %") }, { guideAppearance.opacity.toString() }
                ) { value -> appearance { it.copy(opacity = value.toInt()) } })
                add(RemoteEntry.Choice("listClose", list, text(R.string.remote_list_close),
                    listOf(0, 5, 10, 15, 30, 60).map {
                        RemoteOption(it.toString(), if (it == 0) text(R.string.guide_manual_close) else context.getString(R.string.guide_seconds, it))
                    }, { guideAppearance.closeSeconds.toString() }
                ) { value -> appearance { it.copy(closeSeconds = value.toInt()) } })
                add(RemoteEntry.Choice("accent", appearance, name(R.string.accent_color_setting),
                    AccentChoice.entries.map { RemoteOption(it.name, text(it.label), it.hex) }, { accentChoice.name }
                ) { value -> AccentChoice.entries.firstOrNull { it.name == value }?.let(onAccentChoiceChange) })
                // Farbverläufe: "none" goes back to the plain accent colour.
                add(RemoteEntry.Choice("gradient", appearance, text(R.string.accent_palette_gradients),
                    listOf(RemoteOption("none", text(R.string.remote_gradient_none))) + AccentGradient.entries.map {
                        RemoteOption(it.name, text(it.label), "#%06X".format(it.start.toArgb() and 0xFFFFFF))
                    }, { accentGradient?.name ?: "none" }
                ) { value ->
                    val gradient = AccentGradient.entries.firstOrNull { it.name == value }
                    if (gradient == null) onAccentChoiceChange(accentChoice) else onAccentGradientChange(gradient)
                })
                add(RemoteEntry.Choice("menuStyle", appearance, name(R.string.menu_style_setting),
                    options(MainMenuStyle.entries) { it.label }, { mainMenuStyle.name }
                ) { value ->
                    mainMenuStyle = MainMenuStyle.valueOf(value)
                    preferences?.edit { putString("main_menu_style", value) }
                })
                add(RemoteEntry.Choice("providerStyle", appearance, name(R.string.provider_category_style_setting),
                    options(ProviderCategoryStyle.entries) { it.label }, { providerCategoryStyle.name }
                ) { value ->
                    providerCategoryStyle = ProviderCategoryStyle.valueOf(value)
                    preferences?.edit { putString("provider_category_style", value) }
                })
                add(toggle("subtitles", playback, text(R.string.subtitles_default_on), { subtitleDefaultEnabled },
                    "subtitle_default_enabled") { subtitleDefaultEnabled = it })
                add(toggle("forcedSubtitles", playback, text(R.string.subtitles_forced_auto), { forcedSubtitlesEnabled },
                    "forced_subtitles_enabled") { forcedSubtitlesEnabled = it })
                // Buffer, timeshift and smooth switch, as under Wiedergabe on the TV.
                add(RemoteEntry.Choice("buffer", playback, text(R.string.buffer_setting),
                    listOf(RemoteOption("OFF", text(R.string.subtitle_off))) + BufferLevel.entries.map { RemoteOption(it.name, text(it.label)) },
                    { chosenBuffer(context)?.name ?: "OFF" }
                ) { value -> setChosenBuffer(context, BufferLevel.entries.firstOrNull { it.name == value }) })
                add(RemoteEntry.Info("bufferHint", playback, text(R.string.buffer_setting), text(R.string.buffer_restart_hint)))
                add(RemoteEntry.Choice("timeshift", playback, text(R.string.timeshift_setting),
                    listOf(RemoteOption("OFF", text(R.string.subtitle_off))) +
                        TIMESHIFT_DURATIONS.map { RemoteOption(it.toString(), context.getString(R.string.timeshift_duration_setting, it)) },
                    { timeshiftMinutes(context)?.toString() ?: "OFF" }
                ) { value -> setTimeshiftMinutes(context, value.toIntOrNull()?.takeIf { it in TIMESHIFT_DURATIONS }) })
                // Automatic switch on errors (its settings while on) or the stream chosen by hand.
                addAll(streamSwitchRemoteEntries(context, playback))
                add(RemoteEntry.Choice("vodLive", playback, name(R.string.live_preview_in_vod),
                    options(vodViews) { it.label }, { vodLiveView.name }
                ) { value ->
                    vodLiveView = VodLiveView.valueOf(value)
                    preferences?.edit { putString("vod_live_view", value) }
                })
                for ((section, label) in listOf(
                    PortalSection.MOVIES to R.string.vod_movies_playback,
                    PortalSection.SERIES to R.string.vod_series_playback
                )) add(RemoteEntry.Choice("vodView${section.name}", playback, name(label),
                    vodViews.map {
                        RemoteOption(it.name, text(if (it == VodLiveView.HIDDEN) R.string.vod_playback_off else it.label))
                    }, { vodPlaybackViews.getValue(section).name }
                ) { value ->
                    val next = VodLiveView.valueOf(value)
                    vodPlaybackViews[section] = next
                    preferences?.edit { putString("vod_view_${section.name}", value) }
                    if (next == VodLiveView.HIDDEN && vodSection == section) player?.pause()
                })
                val episodes = text(R.string.episode_settings)
                val detection = listOf(RemoteOption("AUTO", text(R.string.remote_detection_auto)), RemoteOption("TIME", text(R.string.remote_detection_time)))
                add(toggle("autoNext", episodes, text(R.string.auto_next_episode), { autoNextEpisode },
                    "auto_next_episode") { autoNextEpisode = it })
                add(toggle("inactivity", episodes, text(R.string.series_inactivity_check), { checkSeriesInactivity },
                    "check_series_inactivity") { checkSeriesInactivity = it })
                add(number("inactiveLimit", episodes, text(R.string.remote_inactive_limit), 1, 10, 1, text(R.string.series_overlay_episodes),
                    { inactiveEpisodeLimit }, "inactive_episode_limit") { inactiveEpisodeLimit = it }.shownIf("inactivity", true))
                add(toggle("skipIntro", episodes, text(R.string.skip_intro_automatically), { autoSkipIntro },
                    "auto_skip_intro") { autoSkipIntro = it })
                add(toggle("decodeAudio", episodes, text(R.string.decode_audio_for_intro), { decodeAudioForIntro },
                    "decode_audio_for_intro") { decodeAudioForIntro = it })
                add(RemoteEntry.Choice("introMode", episodes, text(R.string.remote_intro), detection, { if (manualIntro) "TIME" else "AUTO" }) {
                    manualIntro = it == "TIME"
                    preferences?.edit { putBoolean("manual_intro", manualIntro) }
                })
                add(number("introStart", episodes, text(R.string.remote_intro_start), 0, 595, 5, text(R.string.remote_unit_seconds), { introStartSeconds },
                    "intro_start_seconds") { introStartSeconds = it.coerceAtMost(introEndSeconds - 5).coerceAtLeast(0) }
                    .shownIf("introMode", "TIME"))
                add(number("introEnd", episodes, text(R.string.remote_intro_end), 5, 600, 5, text(R.string.remote_unit_seconds), { introEndSeconds },
                    "intro_end_seconds") { introEndSeconds = it.coerceAtLeast(introStartSeconds + 5) }
                    .shownIf("introMode", "TIME"))
                add(RemoteEntry.Choice("creditsMode", episodes, text(R.string.remote_credits), detection, { if (manualCredits) "TIME" else "AUTO" }) {
                    manualCredits = it == "TIME"
                    preferences?.edit { putBoolean("manual_credits", manualCredits) }
                })
                add(number("creditsBefore", episodes, text(R.string.remote_credits_before), 0, 600, 5, text(R.string.remote_unit_seconds), { creditsBeforeEndSeconds },
                    "credits_before_end_seconds") { creditsBeforeEndSeconds = it }.shownIf("creditsMode", "TIME"))
                add(number("episodeNotice", episodes, text(R.string.remote_episode_notice), 5, 60, 5, text(R.string.remote_unit_seconds),
                    { episodeNoticeSeconds }, "episode_notice_seconds") { episodeNoticeSeconds = it })
                for (input in GuideInput.entries) add(RemoteEntry.Choice(
                    "control${input.name}", controls, text(input.label),
                    options(ControlAction.entries) { it.label },
                    { (controlBindings[input] ?: ControlAction.DEFAULT).name }
                ) { value ->
                    val action = ControlAction.valueOf(value)
                    controlBindings[input] = action
                    preferences?.edit { putString("control_${input.name}", value) }
                })
                val updates = text(R.string.settings_updates)
                add(RemoteEntry.Toggle("updateAuto", updates, text(R.string.update_auto), { appUpdates.automatic }) {
                    appUpdates.setAutomatic(it)
                })
                add(RemoteEntry.Toggle("updateBeta", updates, text(R.string.update_beta), { appUpdates.beta }) {
                    appUpdates.setBeta(it)
                })
                add(RemoteEntry.Action("updateCheck", updates, appUpdates.status, text(R.string.update_check)) {
                    appUpdates.check()
                    text(R.string.remote_msg_searching_updates)
                })
                val metadata = text(R.string.settings_metadata)
                add(RemoteEntry.Info("tmdbStatus", metadata, "TMDB", when {
                    ui.metadataBusy -> text(R.string.remote_tmdb_checking)
                    ui.metadataError -> text(R.string.tmdb_connection_failed)
                    metadataClient != null -> text(R.string.tmdb_connected)
                    else -> text(R.string.remote_tmdb_not_connected)
                }, "https://www.themoviedb.org/settings/api"))
                add(RemoteEntry.Input("tmdbToken", metadata, text(R.string.tmdb_token), text(R.string.remote_tmdb_token_hint), true,
                    text(R.string.tmdb_connect)) { value ->
                    if (value.isBlank()) text(R.string.remote_msg_enter_token) else if (ui.metadataBusy) text(R.string.remote_msg_wait)
                    else { updateMetadataConnection(value.trim()); text(R.string.remote_tmdb_checking) }
                })
                if (metadataClient != null) add(RemoteEntry.Action("tmdbDisconnect", metadata, text(R.string.remote_tmdb_connection),
                    text(R.string.tmdb_disconnect)) { updateMetadataConnection(null); text(R.string.remote_msg_tmdb_disconnected) })
                val sync = text(R.string.settings_sync)
                val trakt = traktSession
                val code = ui.traktDeviceCode
                add(RemoteEntry.Info("traktStatus", sync, "Trakt", when {
                    trakt != null -> context.getString(R.string.trakt_connected_as, trakt.username)
                    code != null -> context.getString(R.string.trakt_enter_code, code.verificationUrl) + " " + code.userCode
                    ui.traktBusy -> text(R.string.trakt_connecting)
                    else -> ui.traktStatus ?: text(R.string.remote_trakt_not_connected)
                }, code?.verificationUrl))
                if (trakt == null) {
                    add(RemoteEntry.Info("traktAppInfo", sync, text(R.string.trakt_app_title),
                        text(if (traktApp == null) R.string.trakt_app_help else R.string.trakt_app_saved),
                        "https://trakt.tv/oauth/applications"))
                    add(RemoteEntry.Input("traktClientId", sync, "Client ID", text(R.string.remote_trakt_client_id_hint), true,
                        text(R.string.trakt_app_save)) { value -> saveTraktApp(value, null) ?: text(R.string.remote_msg_client_id_saved) })
                    add(RemoteEntry.Input("traktClientSecret", sync, "Client Secret", text(R.string.remote_trakt_client_secret_hint), true,
                        text(R.string.trakt_app_save)) { value ->
                        if (traktApp == null) text(R.string.remote_msg_client_id_first) else saveTraktApp(null, value) ?: text(R.string.remote_msg_client_secret_saved)
                    })
                }
                if (trakt == null && code == null && traktApp != null) add(RemoteEntry.Action("traktConnect", sync, text(R.string.remote_trakt_account),
                    text(R.string.trakt_connect)) { connectTrakt(); text(R.string.remote_msg_code_requested) })
                if (trakt != null) {
                    add(RemoteEntry.Action("traktSync", sync, ui.traktStatus ?: text(R.string.remote_trakt_history_watchlist), text(R.string.remote_action_sync)) {
                        if (ui.traktBusy) text(R.string.remote_msg_already_running) else { syncTrakt(forceSeriesHistory = true); text(R.string.trakt_syncing) }
                    })
                    add(RemoteEntry.Action("traktDisconnect", sync, text(R.string.remote_trakt_account), text(R.string.trakt_disconnect)) {
                        disconnectTrakt(); null
                    })
                }
                // Smart home (Home Assistant): its own section, built in SmartHomeSettings.kt.
                addAll(smartHomeRemoteEntries(context))
                // Parental settings are changed with their own command (some need the PIN).
                add(RemoteEntry.Toggle("parental.protectSettings", parentalSection, text(R.string.parental_protect_settings),
                    { parental.protectSettings }) {})
                add(RemoteEntry.Toggle("parental.lockAdult", parentalSection, text(R.string.parental_lock_adult),
                    { parental.lockAdult }) {})
                add(RemoteEntry.Toggle("parental.kidsMode", parentalSection, text(R.string.kids_mode),
                    { parental.kidsMode }) {})
                add(RemoteEntry.Choice("parental.kidsAge", parentalSection, name(R.string.kids_age),
                    KIDS_AGES.map { RemoteOption(it.toString(), context.getString(R.string.kids_age_value, it)) },
                    { parental.kidsAge.toString() }) {})
                // Einstellungsschutz (the PIN itself is set on the device) and areas of the admin.
                if (parental.hasSettingsPin) {
                    add(RemoteEntry.Toggle("parental.lockPortals", parentalSection, text(R.string.settings_lock_portals),
                        { parental.lockPortals }) {})
                    add(RemoteEntry.Toggle("parental.lockCategories", parentalSection, text(R.string.settings_lock_categories),
                        { parental.lockCategories }) {})
                }
                listOf("LIVE" to R.string.destination_live, "MOVIES" to R.string.destination_movies, "SERIES" to R.string.destination_series)
                    .forEach { (area, label) ->
                        add(RemoteEntry.Toggle("parental.area.$area", parentalSection,
                            context.getString(R.string.settings_area_shown, text(label)), { area !in parental.hiddenAreas }) {})
                    }
                profiles.kidsName?.let { kids ->
                    listOf("LIVE" to R.string.destination_live, "MOVIES" to R.string.destination_movies, "SERIES" to R.string.destination_series)
                        .forEach { (area, label) ->
                            add(RemoteEntry.Toggle("parental.kidsArea.$area", parentalSection,
                                context.getString(R.string.kids_profile_area, kids, text(label)), { area in profiles.kidsAreas }) {})
                        }
                    add(RemoteEntry.Toggle("parental.kidsShare", parentalSection,
                        context.getString(R.string.kids_profile_share, kids), { profiles.kidsShareData }) {})
                    add(RemoteEntry.Choice("parental.startProfile", parentalSection, text(R.string.remote_start_profile),
                        StartProfile.entries.map { RemoteOption(it.name, if (it == StartProfile.KIDS) kids else text(it.label)) },
                        { profiles.startProfile.name }) {})
                }
            }
        }

        fun categories(section: PortalSection): List<RemoteCategory> {
            val groups = if (section == PortalSection.LIVE) {
                allLiveCategories.keys.sortedBy { categoryNames.values.indexOf(it) }.map { PortalGroup(it, it) }
            } else defaultVodCategoryOrder(section)
            return withCategoryOrder(section, groups) { it.id }.map { group ->
                val key = categoryKey(section, group.id)
                RemoteCategory(group.id, group.name, key in hiddenCategories, key in parental.lockedCategories)
            }
        }

        fun countryName(code: String) =
            if (code == UNKNOWN_COUNTRY) text(R.string.remote_country_unspecified) else runCatching { countryLabel(code) }.getOrDefault(code)

        override fun remoteState(): RemoteState {
            val available = (countries + knownCountries).toList()
            return RemoteState(
                device = if (isTelevision) "StreamWatch (TV)" else "StreamWatch",
                accent = accentGradient?.hex ?: accentChoice.hex,
                settings = entries(),
                portals = savedSources.mapIndexed { index, source ->
                    val host = runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                    RemotePortal(
                        source.identity, context.getString(R.string.remote_portal_label, index + 1, host), source.address, source.mac,
                        source.enabled, source.reachable,
                        source.subscriptionExpiry?.format(DateTimeFormatter.ofPattern("dd.MM.uuuu")),
                        source.identity == storedSource?.identity,
                        preferred = source.identity == preferredPortal
                    )
                },
                categories = PortalSection.entries.associate { it.name to categories(it) },
                countries = available.sortedWith(compareBy({ it !in countries }, {
                    if (it in countries) countries.indexOf(it).toString().padStart(4, '0') else countryName(it)
                })).map { RemoteCountry(it, countryName(it), it in countries) },
                parental = RemoteParental(
                    parental.hasPin, parental.separateKidsPin, parental.kidsMode,
                    hasSettingsPin = parental.hasSettingsPin, kidsAccount = profiles.kidsName,
                    pinPassword = (if (parental.hasSettingsPin) parental.settingsPinLength else parental.pinLength) == PIN_PASSWORD,
                    ownSettingsPin = parental.ownSettingsPin,
                    parentalPinPassword = parental.pinLength == PIN_PASSWORD,
                    settingsPinPassword = parental.settingsPinLength == PIN_PASSWORD
                ),
                busy = if (connecting) text(R.string.remote_busy_portals) else null,
                askCountries = ui.askForCountries || ui.choosingCountries,
                error = ui.sourceError?.let(::text)
            )
        }

        fun pinOk(pin: String?, check: (String) -> Boolean): Boolean {
            if (remotePinFailures.get() >= REMOTE_PIN_ATTEMPTS) return false
            val ok = pin != null && check(pin)
            if (!ok) remotePinFailures.incrementAndGet() else remotePinFailures.set(0)
            return ok
        }
        val wrongPin get() = RemoteReply(false,
            if (remotePinFailures.get() >= REMOTE_PIN_ATTEMPTS) text(R.string.remote_msg_pin_too_many)
            else text(R.string.remote_msg_pin_wrong))

        override fun applyRemote(command: RemoteCommand): RemoteReply = when (command) {
            RemoteCommand.Hello -> RemoteReply(true)
            // Only the PIN-only phone session (from a PIN window) answers this.
            is RemoteCommand.PinEntry -> RemoteReply(false)
            // Backup commands run in the remote access page (off the main thread).
            is RemoteCommand.BackupCreate, is RemoteCommand.BackupInspect, is RemoteCommand.BackupRestore -> RemoteReply(false)
            is RemoteCommand.Set ->
                if (command.id.startsWith("parental.")) RemoteReply(false)
                else if (entries().applyRemoteValue(command.id, command.value)) RemoteReply(true)
                else RemoteReply(false, text(R.string.remote_msg_value_invalid))
            is RemoteCommand.Categories -> {
                setCategoryVisibility(command.section, command.ids, command.hidden)
                RemoteReply(true)
            }
            is RemoteCommand.CategoryLock -> {
                val key = categoryKey(command.section, command.id)
                when {
                    !parental.hasPin -> RemoteReply(false, text(R.string.remote_msg_pin_first))
                    command.locked == (key in parental.lockedCategories) -> RemoteReply(true)
                    !command.locked && !pinOk(command.pin, parental::checkPin) -> wrongPin
                    else -> { parental.toggleLocked(key); RemoteReply(true) }
                }
            }
            is RemoteCommand.Countries -> {
                val allowed = countries + knownCountries
                saveCountries(command.codes.filter { it in allowed }.toCollection(LinkedHashSet()))
                if (command.confirm) {
                    // Confirmed on the phone: the question on the device is answered too.
                    ui.askForCountries = false
                    ui.choosingCountries = false
                    RemoteReply(true, text(R.string.remote_msg_countries_saved))
                } else RemoteReply(true)
            }
            RemoteCommand.Finish -> RemoteReply(true)
            RemoteCommand.CountriesLater -> {
                ui.askForCountries = false
                RemoteReply(true)
            }
            is RemoteCommand.PortalEdit -> savedSources.firstOrNull { it.identity == command.id }?.let { source ->
                when {
                    connecting -> RemoteReply(false, text(R.string.remote_msg_portal_loading_wait))
                    else -> { editPortal(source, command.address, command.mac); RemoteReply(true, text(R.string.remote_msg_portal_loading)) }
                }
            } ?: RemoteReply(false, text(R.string.remote_msg_portal_not_found))
            is RemoteCommand.PortalAdd ->
                if (connecting) RemoteReply(false, text(R.string.remote_msg_portal_loading_wait))
                else {
                    address = command.address
                    mac = command.mac
                    ui.sourceError = null
                    importPortal()
                    RemoteReply(true, text(R.string.remote_msg_portal_loading))
                }
            is RemoteCommand.PortalEnabled -> savedSources.firstOrNull { it.identity == command.id }?.let { source ->
                if (source.enabled != command.enabled) setPortalEnabled(source, command.enabled)
                RemoteReply(true)
            } ?: RemoteReply(false, text(R.string.remote_msg_portal_not_found))
            is RemoteCommand.PortalPreferred -> savedSources.firstOrNull { it.identity == command.id }?.let { source ->
                setPreferredPortal(if (command.preferred) source.identity else preferredPortal.takeUnless { it == source.identity })
                RemoteReply(true, if (command.preferred) text(R.string.remote_msg_portal_preferred) else null)
            } ?: RemoteReply(false, text(R.string.remote_msg_portal_not_found))
            is RemoteCommand.PortalCategories -> savedSources.firstOrNull { it.identity == command.id }?.let { source ->
                RemoteReply(true, data = encodeProviderCategories(source.identity, portalName(source), providerCategories(source), text(R.string.country_none)))
            } ?: RemoteReply(false, text(R.string.remote_msg_portal_not_found))
            is RemoteCommand.Action -> (entries().firstOrNull { it.id == command.id } as? RemoteEntry.Action)
                ?.let { RemoteReply(true, it.run()) } ?: RemoteReply(false, text(R.string.remote_msg_action_unavailable))
            is RemoteCommand.Input -> (entries().firstOrNull { it.id == command.id } as? RemoteEntry.Input)
                ?.let { RemoteReply(true, it.submit(command.value)) } ?: RemoteReply(false, text(R.string.remote_msg_input_unavailable))
            is RemoteCommand.PortalRemove ->
                if (savedSources.size <= 1) RemoteReply(false, text(R.string.remote_msg_last_portal))
                else {
                    scope.launch { removePortal(command.id) }
                    RemoteReply(true, text(R.string.remote_msg_portal_removed))
                }
            is RemoteCommand.Pin ->
                if (parental.hasPin && !pinOk(command.current, parental::checkPin)) wrongPin
                else { parental.setPin(command.pin); RemoteReply(true, text(R.string.remote_msg_pin_saved)) }
            // Like the device: the Kindersicherung can take over the own settings PIN (then shared).
            is RemoteCommand.PinFromSettings -> when {
                parental.hasPin || !parental.ownSettingsPin -> RemoteReply(false)
                !pinOk(command.current, parental::checkSettingsPin) -> wrongPin
                else -> { parental.setPin(command.current!!); parental.setSettingsPin(null); RemoteReply(true, text(R.string.remote_msg_pin_saved)) }
            }
            // Einstellungsschutz: an own PIN, or the Kindersicherung PIN (confirmed with it).
            is RemoteCommand.SettingsPin -> when {
                parental.hasSettingsPin && !pinOk(command.current, parental::checkSettingsPin) -> wrongPin
                command.pin != null -> { parental.setSettingsPin(command.pin); RemoteReply(true, text(R.string.remote_msg_pin_saved)) }
                !parental.hasPin -> RemoteReply(false, text(R.string.remote_msg_pin_first))
                !pinOk(command.confirm, parental::checkPin) -> wrongPin
                else -> { parental.setSettingsPin(null); RemoteReply(true, text(R.string.remote_msg_pin_saved)) }
            }
            is RemoteCommand.SettingsPinRemove ->
                if (!parental.hasSettingsPin) RemoteReply(true)
                else if (!pinOk(command.current, parental::checkSettingsPin)) wrongPin
                else { parental.removeSettingsPin(); RemoteReply(true, text(R.string.remote_msg_pin_removed)) }
            is RemoteCommand.PinRemove ->
                if (!parental.hasPin) RemoteReply(true)
                else if (!pinOk(command.current, parental::checkPin)) wrongPin
                else { parental.removePin(); RemoteReply(true, text(R.string.remote_msg_pin_removed)) }
            is RemoteCommand.KidsPin ->
                if (!parental.hasPin) RemoteReply(false, text(R.string.remote_msg_pin_first))
                else if (!pinOk(command.current, parental::checkPin)) wrongPin
                else { parental.setKidsPin(command.pin); RemoteReply(true) }
            is RemoteCommand.Parental -> {
                val value = command.value?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
                when (command.field) {
                    "protectSettings" -> when {
                        value?.isBoolean != true -> RemoteReply(false)
                        !parental.hasSettingsPin -> RemoteReply(false, text(R.string.remote_msg_settings_pin_first))
                        !value.asBoolean && !pinOk(command.pin, parental::checkSettingsPin) -> wrongPin
                        else -> { parental.updateProtectSettings(value.asBoolean); RemoteReply(true) }
                    }
                    "lockAdult" -> when {
                        value?.isBoolean != true -> RemoteReply(false)
                        value.asBoolean -> { parental.updateLockAdult(true); RemoteReply(true) }
                        parental.hasPin && !pinOk(command.pin, parental::checkPin) -> wrongPin
                        else -> { parental.updateLockAdult(false); RemoteReply(true) }
                    }
                    "kidsMode" -> when {
                        value?.isBoolean != true -> RemoteReply(false)
                        !parental.hasPin -> RemoteReply(false, text(R.string.remote_msg_pin_first))
                        value.asBoolean -> { parental.updateKidsMode(true); RemoteReply(true) }
                        !pinOk(command.pin, parental::checkKidsPin) -> wrongPin
                        else -> { parental.updateKidsMode(false); RemoteReply(true) }
                    }
                    "kidsAge" -> value?.takeIf { it.isString }?.asString?.toIntOrNull()?.takeIf { it in KIDS_AGES }?.let {
                        parental.updateKidsAge(it); RemoteReply(true)
                    } ?: RemoteReply(false)
                    "lockPortals", "lockCategories", "area.LIVE", "area.MOVIES", "area.SERIES" -> when {
                        value?.isBoolean != true -> RemoteReply(false)
                        // Like on the device: with a settings PIN every change here needs it.
                        parental.hasSettingsPin && !pinOk(command.pin, parental::checkSettingsPin) -> wrongPin
                        command.field == "lockPortals" -> { parental.updateLockPortals(value.asBoolean); RemoteReply(true) }
                        command.field == "lockCategories" -> { parental.updateLockCategories(value.asBoolean); RemoteReply(true) }
                        parental.setAreaHidden(command.field.removePrefix("area."), !value.asBoolean) -> RemoteReply(true)
                        else -> RemoteReply(false, context.getString(R.string.settings_area_last))
                    }
                    "kidsName" -> when {
                        !parental.hasPin -> RemoteReply(false, text(R.string.remote_msg_pin_first))
                        value?.isString != true || value.asString.isBlank() -> RemoteReply(false, text(R.string.remote_msg_name_required))
                        else -> { profiles.createKids(value.asString.trim().take(UserProfiles.MAX_NAME)); RemoteReply(true) }
                    }
                    "kidsRemove" -> when {
                        !profiles.hasKids -> RemoteReply(false)
                        !pinOk(command.pin, parental::checkPin) -> wrongPin
                        else -> { profiles.removeKids(); RemoteReply(true, text(R.string.remote_msg_kids_removed)) }
                    }
                    "kidsShare" -> if (value?.isBoolean == true && profiles.hasKids) {
                        profiles.updateKidsShareData(value.asBoolean); RemoteReply(true)
                    } else RemoteReply(false)
                    "startProfile" -> StartProfile.entries.firstOrNull { value?.isString == true && it.name == value.asString }
                        ?.takeIf { profiles.hasKids }?.let { profiles.updateStartProfile(it); RemoteReply(true) } ?: RemoteReply(false)
                    "kidsArea.LIVE", "kidsArea.MOVIES", "kidsArea.SERIES" -> when {
                        value?.isBoolean != true || !profiles.hasKids -> RemoteReply(false)
                        profiles.setKidsArea(command.field.removePrefix("kidsArea."), value.asBoolean) -> RemoteReply(true)
                        else -> RemoteReply(false, context.getString(R.string.settings_area_last))
                    }
                    else -> RemoteReply(false)
                }
            }
        }
    }
    val remoteHandlerCurrent by rememberUpdatedState(remoteHandlerNow)
    val remoteHandler = remember {
        object : RemoteHandler {
            override fun remoteState() = remoteHandlerCurrent.remoteState()
            override fun applyRemote(command: RemoteCommand) = remoteHandlerCurrent.applyRemote(command)
        }
    }
    val remoteAccessPage: @Composable () -> Unit = {
        RemoteAccessPage(
            remoteHandler,
            storage = storage,
            // With a PIN or password set, the phone has to enter it before it sees any setting.
            unlock = when {
                parental.hasSettingsPin -> parental::checkSettingsPin
                parental.hasPin -> parental::checkPin
                else -> null
            },
            unlockPassword = (if (parental.hasSettingsPin) parental.settingsPinLength else parental.pinLength) == PIN_PASSWORD,
            onPinReset = { remotePinFailures.set(0) },
            onFinished = {
                // Finished on the phone: back to Live-TV (or the first available area).
                ui.remoteAccessOpen = false
                ui.remoteAccessStandalone = false
                selected = Destination.LIVE.takeIf { it in visibleDestinations } ?: settingsReturnDestination
                lastFocused = selected ?: Destination.LIVE
                guidePanel = GuidePanel.GRID
                lastBack = 0L
            }
        ) { ui.remoteAccessOpen = false; ui.remoteAccessStandalone = false }
    }
    // Opened from the PIN prompt of locked settings: a window over the current screen.
    if (ui.remoteAccessStandalone) remoteAccessPage()
    LaunchedEffect(playbackAttempt, manualIntro, manualCredits) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val active = player ?: return@LaunchedEffect
        val selection = attempt.selection
        if (selection.section != PortalSection.SERIES || (manualIntro && manualCredits)) return@LaunchedEffect
        while (active.currentMediaItem?.mediaId != attempt.mediaId || active.duration <= 0 ||
            active.playbackState != Player.STATE_READY) delay(250)
        val item = active.currentMediaItem ?: return@LaunchedEffect
        if (!active.isCurrentMediaItemSeekable) return@LaunchedEffect
        val duration = active.duration
        val portal = attempt.variants[attempt.index].timing
        val seriesKey = selection.seriesId?.let { selection.source to it }
        val markerKey = "credits_marker:" + java.security.MessageDigest.getInstance("SHA-256")
            .digest(attempt.mediaId.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
        val savedCreditsMarker = progressPreferences?.getLong(markerKey, 0L)?.takeIf { it > 0L }
        val scanOpening = !manualIntro && seriesKey != null && active.currentPosition < OPENING_SCAN_LIMIT_MS
        val scanCredits = !manualCredits && savedCreditsMarker == null &&
            nextEpisodeAction != null && portal.nextAt(duration) == duration
        var timing = EpisodeTiming(creditsStartMs = savedCreditsMarker)
        detectedTiming = attempt.mediaId to timing
        if (!scanOpening && !scanCredits) return@LaunchedEffect
        val uri = item.localConfiguration?.uri ?: return@LaunchedEffect
        val sampleFromSurface = uri.scheme in setOf("http", "https")
        val stepMs = if (sampleFromSurface) HLS_INTRO_FRAME_INTERVAL_MS else FRAME_INTERVAL_MS
        val store = introStore
        // References: the opening of another episode of this series (for a first
        // comparison) and a learned opening (to offer the skip while it is running).
        val previous = seriesOpenings[seriesKey]?.takeIf { it.first != selection.id }
            ?: seriesKey?.let { key ->
                withContext(Dispatchers.IO) {
                    decodeOpeningReference(store?.read(key, IntroFingerprintStore.VIDEO_REFERENCE)?.decodeToString())
                }
            }?.takeIf { it.mediaId != selection.id }?.let { it.mediaId to it.frames }
        val previousAudio = seriesOpeningAudio[seriesKey]?.takeIf { it.first != selection.id }
            ?: seriesKey?.let { key ->
                withContext(Dispatchers.IO) {
                    decodeAudioReference(store?.read(key, IntroFingerprintStore.AUDIO_REFERENCE))
                }
            }?.takeIf { it.first != selection.id }
        var variants = seriesKey?.let { key ->
            withContext(Dispatchers.IO) {
                decodeIntroVariants(store?.read(key, IntroFingerprintStore.INTRO_VARIANTS)).ifEmpty {
                    // An opening learned by an earlier version becomes the first variant.
                    val video = decodeLearnedOpening(store?.read(key, IntroFingerprintStore.VIDEO_LEARNED)?.decodeToString())
                    val audio = decodeLearnedAudio(store?.read(key, IntroFingerprintStore.AUDIO_LEARNED))
                    listOfNotNull(
                        video?.let { IntroVariant(it.range, it.frames, audio?.takeIf { a -> a.first == it.range }?.second.orEmpty(), 0L) },
                        audio?.takeIf { video == null || it.first != video.range }?.let { IntroVariant(it.first, emptyList(), it.second, 0L) }
                    )
                }
            }
        }.orEmpty()
        // End credits are learned like openings, from the closing window of two episodes.
        val closingStart = (duration - CLOSING_WINDOW_MS).coerceAtLeast(0L)
        val previousClosing = seriesClosings[seriesKey]?.takeIf { it.first != selection.id }
            ?: seriesKey?.let { key ->
                withContext(Dispatchers.IO) {
                    decodeOpeningReference(store?.read(key, IntroFingerprintStore.OUTRO_REFERENCE)?.decodeToString())
                }
            }?.takeIf { it.mediaId != selection.id }?.let { it.mediaId to it.frames }
        var outroVariants = seriesKey?.let { key ->
            withContext(Dispatchers.IO) { decodeIntroVariants(store?.read(key, IntroFingerprintStore.OUTRO_VARIANTS)) }
        }.orEmpty()
        var outroLearned: LongRange? = null
        var savedClosing = 0
        var processedClosing = 0
        var processedOpening = -1
        if (active.currentMediaItem != item) return@LaunchedEffect
        val audioToken = if (scanOpening && sampleFromSurface) EpisodeAudioTap.start() else null
        // Intro diagnostics contain counts and times only, never titles or addresses.
        AppDiagnostics.record(
            context,
            "intro_start s:${if (sampleFromSurface) 1 else 0} " +
                "rf:${previous?.second?.size ?: 0} ra:${previousAudio?.second?.size ?: 0} " +
                "iv:" + variants.joinToString(",") { "${it.range.first / 1000}-${it.range.last / 1000}" }.ifEmpty { "-" }
        )
        var lastFrames = 0
        var lastAudio = 0
        var lastLoud = 0
        var lastVideoRange: LongRange? = null
        var lastAudioRange: LongRange? = null
        var learnedRange: LongRange? = null
        var usageRecorded = false
        try {
            val updates = if (sampleFromSurface) flow {
                val opening = mutableListOf<EpisodeFrame>()
                val audio = mutableListOf<AudioPrint>()
                val credits = mutableListOf<EpisodeFrame>()
                var openingComplete = !scanOpening
                var creditsComplete = !scanCredits
                var lastOpeningBucket = -HLS_INTRO_FRAME_INTERVAL_MS
                var lastCreditsBucket = -5_000L
                var lastClosingBucket = -1_000L
                val closing = mutableListOf<EpisodeFrame>()
                var emittedClosing = 0
                var emittedFrames = 0
                var emittedAudio = 0
                var emittedCredits = 0
                while (active.currentMediaItem == item && (!openingComplete || !creditsComplete)) {
                    val position = active.currentPosition
                    val inOpening = !openingComplete && position < OPENING_SCAN_LIMIT_MS
                    if (!openingComplete && position >= OPENING_SCAN_LIMIT_MS) openingComplete = true
                    if (audioToken != null) {
                        audio += EpisodeAudioTap.drain().filter { it.timeMs in 0 until OPENING_SCAN_LIMIT_MS }
                        if (openingComplete) EpisodeAudioTap.stop(audioToken)
                    }
                    // Scan the closing eight minutes so longer credit rolls and
                    // post-credit scenes are not automatically reduced to a 15s fallback.
                    val inCredits = !creditsComplete && active.duration - position <= 480_000L
                    val surface = fullscreenVideoSurface ?: ui.playbackVideoSurface
                    if (surface != null && active.isPlaying) {
                        if (inOpening) {
                            val bucket = position / HLS_INTRO_FRAME_INTERVAL_MS * HLS_INTRO_FRAME_INTERVAL_MS
                            if (bucket > lastOpeningBucket) {
                                sampleEpisodeSurface(surface, bucket)?.let { frame ->
                                    opening += frame
                                    lastOpeningBucket = bucket
                                }
                            }
                        }
                        if (inCredits) {
                            // One sample per second; the closing window (last five minutes) is kept
                            // for comparing end credits between episodes, every fifth second also
                            // feeds the credit-text heuristic.
                            val bucket = position / 1_000L * 1_000L
                            if (bucket > lastClosingBucket) {
                                sampleEpisodeSurface(surface, bucket)?.let { frame ->
                                    lastClosingBucket = bucket
                                    if (bucket >= closingStart) closing += frame.copy(timeMs = bucket - closingStart)
                                    if (bucket - lastCreditsBucket >= 5_000L) {
                                        credits += frame
                                        lastCreditsBucket = bucket
                                    }
                                }
                            }
                        }
                    }
                    // Compare only when new evidence arrived instead of on every poll.
                    val audioReady = audio.size - emittedAudio >= 20 || openingComplete && audio.size > emittedAudio
                    if (opening.size != emittedFrames || audioReady || credits.size != emittedCredits ||
                        closing.size != emittedClosing
                    ) {
                        emittedFrames = opening.size
                        emittedAudio = audio.size
                        emittedCredits = credits.size
                        emittedClosing = closing.size
                        emit(EpisodeVideoAnalysis(opening.toList(), findCreditsStart(credits), audio.toList(), closing.toList()))
                    }
                    if (active.playbackState == Player.STATE_ENDED ||
                        active.duration > 0 && position >= active.duration - 1_000L
                    ) {
                        openingComplete = true
                        creditsComplete = true
                    }
                    delay(if (inOpening || inCredits) 150 else 500)
                }
            } else analyzeEpisodeVideo(uri, duration, scanOpening, scanCredits)
            var savedFrames = 0
            var savedAudio = 0
            updates.collect { analysis ->
                if (active.currentMediaItem != item) return@collect
                // Opening work only runs when new opening frames or prints arrived, not for
                // every closing sample near the end of the episode.
                val openingEvidence = analysis.opening.size + analysis.audio.size
                if (seriesKey != null && openingEvidence > 0 && openingEvidence != processedOpening) {
                    processedOpening = openingEvidence
                    // Learned openings (variants) are recognised early so the skip is offered while
                    // they run, and are re-checked as playback continues. The comparison with the
                    // previous episode keeps running until the scan ends and corrects or adds variants.
                    val (observations, videoRange, audioRange) = withContext(Dispatchers.Default) {
                        val seen = variants.mapNotNull { variant ->
                            val observation = observeKnownOpening(analysis.opening, variant.frames, variant.range, stepMs)
                                ?: confirmKnownAudioOpening(analysis.audio, variant.prints, variant.range)
                                    ?.let { OpeningObservation(it, diverged = false, lastMatchMs = null) }
                            observation?.let { variant to it }
                        }
                        Triple(
                            seen,
                            previous?.let { findRepeatedOpening(analysis.opening, it.second, stepMs) },
                            previousAudio?.let { findRepeatedAudioOpening(analysis.audio, it.second) }
                        )
                    }
                    if (active.currentMediaItem != item) return@collect
                    val compared = mergeOpeningRanges(videoRange, audioRange)
                    lastFrames = analysis.opening.size
                    lastAudio = analysis.audio.size
                    lastLoud = analysis.audio.count(AudioPrint::loud)
                    if (videoRange != lastVideoRange || audioRange != lastAudioRange) {
                        lastVideoRange = videoRange
                        lastAudioRange = audioRange
                        AppDiagnostics.record(
                            context,
                            "intro_match at:${active.currentPosition / 1000} v:${videoRange?.let { "${it.first / 1000}-${it.last / 1000}" } ?: "-"} " +
                                "a:${audioRange?.let { "${it.first / 1000}-${it.last / 1000}" } ?: "-"} " +
                                "f:${analysis.opening.size} p:${analysis.audio.size}"
                        )
                    }
                    val known = chooseIntro(observations.map { it.second })
                    val intro = known?.let { recognised ->
                        compared?.takeIf { it.first <= recognised.last && recognised.first <= it.last && it.last > recognised.last }
                            ?.let { recognised.first..it.last } ?: recognised
                    } ?: compared
                    val currentIntro = timing.introStartMs?.let { start -> timing.introEndMs?.let { start until it } }
                    if (intro != currentIntro) {
                        // A recognised opening the episode then left is withdrawn rather than kept.
                        timing = timing.copy(introStartMs = intro?.first, introEndMs = intro?.last?.plus(1))
                        AppDiagnostics.record(
                            context,
                            "intro_range at:${active.currentPosition / 1000} " +
                                "${intro?.let { "${it.first / 1000}-${it.last / 1000}" } ?: "-"} " +
                                "seen:" + observations.joinToString(",") { (_, o) ->
                                    "${o.range.first / 1000}-${o.range.last / 1000}${if (o.diverged) "x" else ""}"
                                }
                        )
                    }
                    if (store != null) {
                        val now = System.currentTimeMillis()
                        var changed = false
                        if (compared != null && compared != learnedRange) {
                            learnedRange = compared
                            variants = learnIntroVariant(variants, observations, compared, analysis.opening, analysis.audio, now)
                            changed = true
                        } else if (!usageRecorded && observations.any { !it.second.diverged }) {
                            // Recently seen variants are kept when the limit is reached.
                            val used = observations.filterNot { it.second.diverged }.map { it.first }
                            variants = variants.map { variant -> if (used.any { it === variant }) variant.copy(lastUsed = now) else variant }
                            changed = true
                        }
                        if (changed) {
                            usageRecorded = true
                            val encoded = encodeIntroVariants(variants)
                            withContext(Dispatchers.IO) { store.write(seriesKey, IntroFingerprintStore.INTRO_VARIANTS, encoded) }
                        }
                    }
                    // Keep this episode as reference for the next one, but do not replace a
                    // longer reference with a briefly watched episode.
                    val frameCoverage = analysis.opening.lastOrNull()?.timeMs ?: 0L
                    val audioCoverage = analysis.audio.lastOrNull()?.timeMs ?: 0L
                    val previousFrameCoverage = previous?.second?.lastOrNull()?.timeMs ?: 0L
                    val previousAudioCoverage = previousAudio?.second?.lastOrNull()?.timeMs ?: 0L
                    val finished = active.currentPosition >= OPENING_SCAN_LIMIT_MS - 5_000L ||
                        active.playbackState == Player.STATE_ENDED
                    if (seriesOpenings.size >= 16 && seriesKey !in seriesOpenings) {
                        seriesOpenings.remove(seriesOpenings.keys.first())
                    }
                    if (seriesOpeningAudio.size >= 16 && seriesKey !in seriesOpeningAudio) {
                        seriesOpeningAudio.remove(seriesOpeningAudio.keys.first())
                    }
                    val replaceFrames = analysis.opening.size >= 12 &&
                        frameCoverage >= minOf(previousFrameCoverage, 150_000L)
                    val replaceAudio = analysis.audio.size >= 200 &&
                        audioCoverage >= minOf(previousAudioCoverage, 150_000L)
                    if (replaceFrames) seriesOpenings[seriesKey] = selection.id to analysis.opening
                    if (replaceAudio) seriesOpeningAudio[seriesKey] = selection.id to analysis.audio
                    if (store != null) {
                        val writeFrames = replaceFrames && (analysis.opening.size - savedFrames >= 15 ||
                            finished && analysis.opening.size > savedFrames)
                        val writeAudio = replaceAudio && (analysis.audio.size - savedAudio >= 600 ||
                            finished && analysis.audio.size > savedAudio)
                        if (writeFrames || writeAudio) withContext(Dispatchers.IO) {
                            if (writeFrames) store.write(
                                seriesKey, IntroFingerprintStore.VIDEO_REFERENCE,
                                OpeningReference(selection.id, analysis.opening).encode().encodeToByteArray()
                            )
                            if (writeAudio) store.write(
                                seriesKey, IntroFingerprintStore.AUDIO_REFERENCE,
                                encodeAudioReference(selection.id, analysis.audio)
                            )
                        }
                        if (writeFrames) savedFrames = analysis.opening.size
                        if (writeAudio) savedAudio = analysis.audio.size
                    }
                }
                if (seriesKey != null && analysis.closing.size != processedClosing) {
                    processedClosing = analysis.closing.size
                    val closing = analysis.closing
                    val (outroSeen, outroCompared) = withContext(Dispatchers.Default) {
                        outroVariants.mapNotNull { variant ->
                            observeKnownOpening(closing, variant.frames, variant.range)?.let { variant to it }
                        } to previousClosing?.let { findRepeatedOpening(closing, it.second, HLS_INTRO_FRAME_INTERVAL_MS) }
                    }
                    if (active.currentMediaItem != item) return@collect
                    val recognised = chooseIntro(outroSeen.map { it.second })
                    val outro = recognised?.let { known ->
                        outroCompared?.takeIf { it.first <= known.last && known.first <= it.last && it.last > known.last }
                            ?.let { known.first..it.last } ?: known
                    } ?: outroCompared
                    val decision = outro?.let { closingDecision(it, closingStart, duration) }
                    val skip = decision?.skip
                    val previousOutro = timing.outro(duration)
                    timing = timing.copy(
                        outroStartMs = skip?.first, outroEndMs = skip?.last?.plus(1),
                        creditsStartMs = if (skip != null) null else decision?.creditsStart ?: timing.creditsStartMs
                    )
                    if (timing.outro(duration) != previousOutro || decision?.creditsStart != null && outroCompared != outroLearned) {
                        AppDiagnostics.record(
                            context,
                            "outro at:${active.currentPosition / 1000} skip:${skip?.let { "${it.first / 1000}-${it.last / 1000}" } ?: "-"} " +
                                "credits:${decision?.creditsStart?.div(1000) ?: "-"} f:${closing.size}"
                        )
                    }
                    if (store != null) {
                        if (outroCompared != null && outroCompared != outroLearned) {
                            outroLearned = outroCompared
                            outroVariants = learnIntroVariant(
                                outroVariants, outroSeen, outroCompared, closing, emptyList(), System.currentTimeMillis()
                            )
                            val encoded = encodeIntroVariants(outroVariants)
                            withContext(Dispatchers.IO) { store.write(seriesKey, IntroFingerprintStore.OUTRO_VARIANTS, encoded) }
                        }
                        // Keep this episode's closing as reference once most of the window was seen.
                        val covered = closing.size >= 120 && closing.size - savedClosing >= 30 ||
                            active.playbackState == Player.STATE_ENDED && closing.size > savedClosing
                        if (covered && closing.size >= (previousClosing?.second?.size ?: 0) / 2) {
                            if (seriesClosings.size >= 16 && seriesKey !in seriesClosings) {
                                seriesClosings.remove(seriesClosings.keys.first())
                            }
                            seriesClosings[seriesKey] = selection.id to closing
                            savedClosing = closing.size
                            val encoded = OpeningReference(selection.id, closing).encode().encodeToByteArray()
                            withContext(Dispatchers.IO) { store.write(seriesKey, IntroFingerprintStore.OUTRO_REFERENCE, encoded) }
                        }
                    }
                }
                // Credits followed by more footage are skipped instead of ending the episode early.
                analysis.creditsStartMs?.takeIf { timing.outroStartMs == null }?.let { marker ->
                    timing = timing.copy(creditsStartMs = marker)
                    progressPreferences?.edit { putLong(markerKey, marker) }
                }
                detectedTiming = attempt.mediaId to timing
            }
        } finally {
            audioToken?.let(EpisodeAudioTap::stop)
            if (scanOpening) AppDiagnostics.record(
                context,
                "intro_end f:$lastFrames p:$lastAudio loud:$lastLoud " +
                    "intro:${timing.introStartMs?.div(1000) ?: "-"}-${timing.introEndMs?.div(1000) ?: "-"}"
            )
            if (audioToken != null) AppDiagnostics.record(context, "intro_audio " + EpisodeAudioTap.summary())
            val appContext = context.applicationContext
            kotlin.concurrent.thread(name = "diagnostics-copy") { AppDiagnostics.writeLocalCopy(appContext) }
        }
    }
    LaunchedEffect(
        playbackAttempt, autoNextEpisode, autoSkipIntro, episodeNoticeSeconds,
        manualIntro, manualCredits, introStartSeconds, introEndSeconds, creditsBeforeEndSeconds
    ) {
        val attempt = playbackAttempt ?: return@LaunchedEffect
        val active = player ?: return@LaunchedEffect
        if (attempt.selection.section != PortalSection.SERIES) return@LaunchedEffect
        while (playbackAttempt === attempt) {
            val duration = active.duration
            if (active.currentMediaItem?.mediaId == attempt.mediaId && duration > 0) {
                val portal = attempt.variants[attempt.index].timing
                val detected = detectedTiming?.takeIf { it.first == attempt.mediaId }?.second
                val position = if (active.playbackState == Player.STATE_ENDED) duration else active.currentPosition
                val intro = if (manualIntro) {
                    EpisodeTiming(introStartSeconds * 1_000L, introEndSeconds * 1_000L).intro(duration)
                } else portal.intro(duration) ?: detected?.intro(duration)
                // Never offer a stale skip target: that would seek backwards after
                // frame matching completes near the end of an intro.
                val introGrace = intro?.let { position > it.last && position - it.last <= 15_000L } == true
                val introTarget = futureIntroSkipTarget(intro, position)
                val closedUntil = ui.introOfferClosed?.takeIf { it.first == attempt.mediaId }?.second
                val introClosed = closedUntil != null && (
                    closedUntil == Long.MAX_VALUE || introTarget == null || introTarget <= closedUntil + 2_000L
                )
                val introOffer = if (!introClosed && introTarget != null && position < introTarget &&
                    (intro?.contains(position) == true || introGrace) && active.isCurrentMediaItemSeekable
                ) introTarget else null
                // End credits followed by more footage: offer to skip to the post-credits scene.
                val outro = if (manualCredits) null else detected?.outro(duration)
                val outroTarget = futureIntroSkipTarget(outro, position)
                val outroClosedUntil = ui.outroOfferClosed?.takeIf { it.first == attempt.mediaId }?.second
                val outroClosed = outroClosedUntil != null && (
                    outroClosedUntil == Long.MAX_VALUE || outroTarget == null || outroTarget <= outroClosedUntil + 2_000L
                )
                val outroOffer = if (!outroClosed && outroTarget != null && outro?.contains(position) == true &&
                    active.isCurrentMediaItemSeekable
                ) outroTarget else null
                ui.skipIntroTo = introOffer ?: outroOffer
                ui.skipIsOutro = introOffer == null && outroOffer != null
                if (autoSkipIntro && active.isPlaying && !ui.skipIsOutro) ui.skipIntroTo?.let {
                    active.seekTo(it)
                    ui.skipIntroTo = null
                    ui.introOfferClosed = attempt.mediaId to it
                }
                val nextAt = if (manualCredits) {
                    (duration - creditsBeforeEndSeconds * 1_000L).takeIf { it > 0 } ?: duration
                } else minOf(portal.nextAt(duration), detected?.nextAt(duration) ?: duration)
                val remaining = (nextAt - position).coerceAtLeast(0)
                ui.nextEpisodeSeconds = if (nextEpisodeAction != null && remaining <= episodeNoticeSeconds * 1_000L) {
                    ((remaining + 999) / 1_000).toInt()
                } else null
                if (!ui.watchingPrompt && autoNextEpisode && (active.isPlaying || active.playbackState == Player.STATE_ENDED) &&
                    remaining == 0L && canPlay()
                ) {
                    if (nextEpisodeAction != null) {
                        silentEpisodes = if (ui.interruptedEpisode) 0 else silentEpisodes + 1
                        if (checkSeriesInactivity && silentEpisodes >= inactiveEpisodeLimit) {
                            active.pause()
                            ui.watchingPrompt = true
                            ui.nextEpisodeSeconds = null
                        } else advanceEpisode()
                        return@LaunchedEffect
                    }
                }
            }
            delay(250)
        }
    }
    // Kept in its own composable method: the JVM limits a single method to 64 KB.
    // Not skippable: it reads values of the surrounding screen that are not observable state.
    @Composable
    @androidx.compose.runtime.NonSkippableComposable
    fun FullscreenLayer() {
    if (fullscreen) {
        val episodeActionFocus = remember { FocusRequester() }
        val episodeActionSecondaryFocus = remember { FocusRequester() }
        val episodeActionTertiaryFocus = remember { FocusRequester() }
        var episodeActionSelection by remember { mutableIntStateOf(0) }
        val episodeAction = when {
            ui.watchingPrompt -> 3
            ui.nextEpisodeSeconds != null -> 2
            ui.skipIntroTo != null -> 1
            else -> 0
        }
        val episodeActionCount = when {
            ui.watchingPrompt -> 2
            else -> (if (ui.skipIntroTo != null) 1 else 0) + (if (ui.nextEpisodeSeconds != null) 2 else 0)
        }
        fun requestEpisodeActionFocus(index: Int) {
            episodeActionSelection = index.coerceIn(0, (episodeActionCount - 1).coerceAtLeast(0))
            when (episodeActionSelection) {
                0 -> episodeActionFocus
                1 -> episodeActionSecondaryFocus
                else -> episodeActionTertiaryFocus
            }.requestFocus()
        }
        fun activateEpisodeAction() {
            if (ui.watchingPrompt) {
                if (episodeActionSelection == 0) advanceEpisode(confirmed = true) else stopPlayback()
                return
            }
            val introOffset = if (ui.skipIntroTo != null) 1 else 0
            when {
                ui.skipIntroTo != null && episodeActionSelection == 0 -> {
                    val target = requireNotNull(ui.skipIntroTo)
                    player?.seekTo(target)
                    ui.skipIntroTo = null
                    val closed = playbackAttempt?.mediaId?.let { it to target }
                    if (ui.skipIsOutro) ui.outroOfferClosed = closed else ui.introOfferClosed = closed
                }
                ui.nextEpisodeSeconds != null && episodeActionSelection == introOffset ->
                    advanceEpisode(confirmed = true)
                ui.nextEpisodeSeconds != null && episodeActionSelection == introOffset + 1 -> stopPlayback()
            }
        }
        val episodeActionKeyModifier = Modifier.onPreviewKeyEvent { event ->
            if (episodeActionCount == 0) false
            else {
                val activate = event.key == Key.DirectionCenter || event.key == Key.Enter ||
                    event.key == Key.NumPadEnter || event.key == Key.ButtonA
                if (activate) {
                    if (event.type == KeyEventType.KeyUp) activateEpisodeAction()
                    true
                } else false
            }
        }
        LaunchedEffect(episodeAction, playbackControlsVisible, fullscreenGuidePanel, ui.skipIntroTo != null, ui.nextEpisodeSeconds != null) {
            if (episodeAction != 0 && !playbackControlsVisible &&
                (fullscreenGuidePanel == GuidePanel.GRID || ui.watchingPrompt || ui.skipIntroTo != null)
            ) {
                withFrameNanos { }
                requestEpisodeActionFocus(0)
            }
        }
        val screenSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
        val screenWidth = with(LocalDensity.current) { screenSize.width.toDp() }
        val screenHeight = with(LocalDensity.current) { screenSize.height.toDp() }
        val scheduleBesideList = guideAppearance.showSchedule && screenWidth >= 640.dp
        val descriptionBesideSchedule = scheduleBesideList && guideAppearance.showDescription && screenWidth >= 840.dp
        val live = vodSection == null
        val seriesOverlay = vodSection == PortalSection.SERIES && playbackSeries != null
        fun navigateFullscreen(input: GuideInput) {
            if (episodeAction != 0) {
                val selected = when (input) {
                    GuideInput.REMOTE_LEFT, GuideInput.SWIPE_LEFT -> (episodeActionSelection - 1).coerceAtLeast(0)
                    GuideInput.REMOTE_RIGHT, GuideInput.SWIPE_RIGHT ->
                        (episodeActionSelection + 1).coerceAtMost(episodeActionCount - 1)
                    else -> episodeActionSelection
                }
                if (selected != episodeActionSelection) requestEpisodeActionFocus(selected)
                return
            }
            if (runMappedControl(input)) return
            if (input.vertical) {
                if (live && fullscreenGuidePanel == GuidePanel.GRID) {
                    changeLiveChannel(if (input == GuideInput.REMOTE_UP || input == GuideInput.SWIPE_UP) 1 else -1)
                }
                return
            }
            if (seriesOverlay) {
                showFullscreenPanel(fullscreenGuidePanel.afterSeriesOverlay(input))
                return
            }
            if (!live) return
            if (fullscreenGuidePanel == GuidePanel.GRID && (input == GuideInput.SWIPE_RIGHT || input == GuideInput.REMOTE_RIGHT)) {
                programmeChannel = playing
                ui.guideProgramme = null
            }
            showFullscreenPanel(fullscreenGuidePanel.after(input))
        }
        SideEffect {
            registerPromptInput(if (episodeAction != 0) { event ->
                val input = when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                    else -> null
                }
                if (input != null) {
                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) navigateFullscreen(input)
                    true
                } else if (event.keyCode in setOf(
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                        android.view.KeyEvent.KEYCODE_BUTTON_A
                    )
                ) {
                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) activateEpisodeAction()
                    true
                } else false
            } else null)
        }
        DisposableEffect(Unit) {
            onDispose { registerPromptInput(null) }
        }
        val overlayFocus = remember { FocusRequester() }
        // Experimental smooth switch: picture brightness 5 times per second (for Dolby passthrough,
        // where the app cannot hear the sound).
        LaunchedEffect(fullscreenVideoSurface, vodSection) {
            val surface = fullscreenVideoSurface ?: return@LaunchedEffect
            if (vodSection != null) return@LaunchedEffect
            while (true) {
                if (SmoothSwitch.enabled) pictureBrightness(surface)?.let(SmoothSwitch::acceptVideo)
                delay(200)
            }
        }
        // Started straight in fullscreen, the player did not get the focus: the remote's keys
        // (switching channels) only worked after leaving and reopening fullscreen. Whenever the
        // stream starts or changes and nothing else is open, the player takes the focus.
        LaunchedEffect(starting, playing, fullscreenVideoSurface) {
            if (fullscreenGuidePanel != GuidePanel.GRID || playbackControlsVisible) return@LaunchedEffect
            withFrameNanos { }
            var view: android.view.View? = fullscreenVideoSurface
            while (view != null && view !is PlayerView) view = view.parent as? android.view.View
            if (view != null && !view.hasFocus()) view.requestFocus()
        }
        fun interceptDirection(input: GuideInput): Boolean {
            if (episodeAction != 0) return true
            if (playbackControlsVisible) return false
            if (!live && fullscreenGuidePanel == GuidePanel.GRID &&
                input in setOf(GuideInput.REMOTE_LEFT, GuideInput.REMOTE_RIGHT) &&
                player?.isCurrentMediaItemSeekable == true
            ) return false
            val custom = controlBindings[input]?.let { it != ControlAction.DEFAULT } == true
            if (input.vertical) return fullscreenGuidePanel == GuidePanel.GRID && (custom || live)
            return custom || seriesOverlay || live && (
                fullscreenGuidePanel != GuidePanel.CHANNELS || !scheduleBesideList ||
                    input == GuideInput.REMOTE_LEFT && !ui.overlayScheduleFocused
            )
        }
        val inputModifier = Modifier.guideNavigationInput(
            interceptRemote = ::interceptDirection,
            interceptGesture = { input -> interceptDirection(input) || live && !input.vertical && !playbackControlsVisible },
            onInput = ::navigateFullscreen
        )
        Box(Modifier.fillMaxSize().background(Color.Black).then(episodeActionKeyModifier).then(inputModifier)) {
            PlayerFrame(
                player, active = true, fullscreen = true, isLivePlayback = vodSection == null, onStop = ::stopPlayback,
                onSurface = { fullscreenVideoSurface = it },
                onGuideInput = ::navigateFullscreen,
                interceptGuideKey = ::interceptDirection,
                onPreviousEpisode = { previousEpisodeAction?.invoke() },
                onNextEpisode = { advanceEpisode(confirmed = true) },
                hasPreviousEpisode = previousEpisodeAction != null,
                hasNextEpisode = nextEpisodeAction != null,
                onTouchAction = { runMappedControl(GuideInput.TAP) },
                onMediaKey = { runMappedControl(GuideInput.PLAY_PAUSE) },
                controlsRequest = playerControlsRequest,
                controlsDismissRequest = dismissPlaybackControls,
                controlsAllowed = fullscreenGuidePanel == GuidePanel.GRID && !ui.watchingPrompt,
                actionPromptActive = episodeAction != 0 &&
                    (fullscreenGuidePanel == GuidePanel.GRID || ui.watchingPrompt || ui.skipIntroTo != null),
                onEpisodeActionConfirm = ::activateEpisodeAction,
                episodeActionVisible = episodeAction != 0 || fullscreenGuidePanel != GuidePanel.GRID || ui.skipIntroTo != null,
                subtitleDefaultEnabled = subtitleDefaultEnabled,
                forcedSubtitlesEnabled = forcedSubtitlesEnabled,
                subtitleSeriesKey = if (vodSection == PortalSection.SERIES) playbackSeries?.let {
                    titleIdentity(it) ?: normalizedLabel(displayVodTitle(it))
                } else null,
                subtitlePlaybackKey = playbackAttempt?.mediaId,
                subtitlePreferences = progressPreferences,
                onControlsVisible = { playbackControlsVisible = it },
                portalChoices = playbackAttempt?.variants?.map { it.label }.orEmpty(),
                portalIndex = playbackAttempt?.index ?: -1,
                onPortalChoice = ::switchPlaybackVariant
            )
            if (playbackControlsVisible && vodSection != null) {
                val episode = playbackEpisode
                val movie = playbackMovieTitle
                val show = playbackSeries
                val title = if (vodSection == PortalSection.SERIES) show else movie
                if (title != null) {
                    val episodeNumber = episode?.episode?.let { stringResource(R.string.episode_number, it) }
                    val seasonNumber = playbackSeason?.let { season ->
                        season.number?.let { stringResource(R.string.season_number, it) } ?: season.title.name
                    }
                    val detailLine = listOfNotNull(seasonNumber, episodeNumber).joinToString(" · ")
                    val episodeName = episode?.name?.takeIf { it.isNotBlank() && it != show?.name }
                    val description = if (vodSection == PortalSection.SERIES) {
                        germanDescription(episode?.description) ?: germanDescription(show?.description)
                    } else germanDescription(movie?.description)
                    Surface(
                        modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
                            .fillMaxWidth(0.46f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(displayVodTitle(title), style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (detailLine.isNotEmpty()) Text(detailLine,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall)
                            if (episodeName != null && episodeName != title.name) Text(episodeName,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            // When no stream plays at all (not while one is switched): the message and its reason.
            if (playbackError != null) Surface(
                modifier = Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 460.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.82f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlaybackFeedback(null, playbackError)
                }
            }
            if (live && (playbackControlsVisible ||
                    startupInfoVisible && fullscreenGuidePanel == GuidePanel.GRID && episodeAction == 0)
            ) {
                val channel = playing ?: starting
                val nowSeconds = System.currentTimeMillis() / 1_000L
                val programme = channel?.let(::effectiveProgrammes).orEmpty().firstOrNull { item ->
                    val from = item.startSeconds ?: return@firstOrNull false
                    from <= nowSeconds && (item.endSeconds == null || nowSeconds < item.endSeconds)
                } ?: channel?.let(::effectiveProgrammes).orEmpty().firstOrNull()
                if (channel != null) Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(20.dp).fillMaxWidth(0.46f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.82f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(channel.name, style = MaterialTheme.typography.titleLarge, color = Color.White,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        programme?.let { item ->
                            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            listOfNotNull(item.startSeconds, item.endSeconds).takeIf { it.isNotEmpty() }?.let { times ->
                                Text(times.joinToString(" – ", transform = ::guideTime),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                            germanDescription(item.description)?.takeIf(String::isNotBlank)?.let { description ->
                                Text(description, color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (controlBindings[GuideInput.TAP] == ControlAction.PLAY_PAUSE &&
                fullscreenGuidePanel == GuidePanel.GRID && !playbackControlsVisible
            ) {
                MenuButton(
                    stringResource(R.string.control_show_controls),
                    Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) { playerControlsRequest++ }
            }
            if ((live || seriesOverlay) && fullscreenGuidePanel != GuidePanel.GRID) {
                Box(Modifier.matchParentSize().clickable { showFullscreenPanel(GuidePanel.GRID) })
            }
            if (live || seriesOverlay) {
                AnimatedVisibility(
                    visible = fullscreenGuidePanel != GuidePanel.GRID,
                    modifier = Modifier.align(
                        if (ui.fullscreenOverlayContent == GuidePanel.DETAILS) Alignment.TopEnd else Alignment.CenterStart
                    ).padding(12.dp),
                    enter = fadeIn(), exit = fadeOut()
                ) {
                    val panel = ui.fullscreenOverlayContent
                    val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
                        .filter { visibleCategory(PortalSection.LIVE, it, it) }
                        .sortedBy { categoryNames.values.indexOf(it) }) { it }
                    val channelCategory = guideChannelCategory(category, groups, liveCategories, playing)
                    val panelWidth = if (seriesOverlay) minOf(360.dp, screenWidth * 0.8f) else when (panel) {
                        GuidePanel.CHANNELS -> {
                            val fraction = when {
                                descriptionBesideSchedule -> 0.82f
                                scheduleBesideList -> 0.62f
                                else -> 0.38f
                            }
                            (screenWidth * fraction * guideAppearance.listWidth.factor)
                                .coerceIn(minOf(205.dp, screenWidth * 0.8f), screenWidth * 0.88f)
                        }
                        GuidePanel.DETAILS -> (screenWidth * 0.55f).coerceIn(minOf(300.dp, screenWidth * 0.85f), minOf(580.dp, screenWidth * 0.85f))
                        else -> minOf(235.dp, screenWidth * 0.85f)
                    }
                    Surface(
                        modifier = Modifier.width(panelWidth)
                            .then(if (panel == GuidePanel.DETAILS) Modifier.heightIn(max = screenHeight * 0.83f) else Modifier.fillMaxHeight()),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = guideAppearance.opacity / 100f)
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            // The info panel's "Zurück" follows the setting for on-screen back buttons.
                            if (panel == GuidePanel.DETAILS && !seriesOverlay && !showSettingsBackButton) {
                                Box(Modifier.size(1.dp).focusRequester(overlayFocus).focusable())
                            } else MenuButton(
                                if (seriesOverlay && panel == GuidePanel.CHANNELS) overlaySeason?.title?.name
                                    ?.let { seasonName -> stringResource(R.string.series_overlay_season_episodes, seasonName) }
                                    ?: stringResource(R.string.series_overlay_episodes)
                                else if (seriesOverlay && panel == GuidePanel.CATEGORIES) stringResource(R.string.series_overlay_seasons)
                                else if (panel == GuidePanel.CHANNELS) channelCategory ?: stringResource(R.string.channels)
                                else stringResource(R.string.navigation_back),
                                Modifier.focusRequester(overlayFocus)
                            ) {
                                if (seriesOverlay) navigateBack()
                                else showFullscreenPanel(if (panel == GuidePanel.CHANNELS) GuidePanel.CATEGORIES else panel.after(GuideInput.BACK))
                            }
                            LaunchedEffect(panel) { if (seriesOverlay || panel != GuidePanel.CHANNELS) overlayFocus.requestFocus() }
                            when (panel) {
                                GuidePanel.CHANNELS -> {
                                    if (seriesOverlay) {
                                        val episodes = overlaySeason?.episodes.orEmpty()
                                        val current = episodes.indexOf(playbackEpisode).coerceAtLeast(0)
                                        key(overlaySeason) {
                                            LazyColumn(
                                                Modifier.weight(1f), state = rememberLazyListState(current)
                                            ) {
                                                items(episodes) { episode ->
                                                    val seasonName = overlaySeason?.title?.name
                                                    val episodeTitle = episode.name.takeUnless { name ->
                                                        name.equals(seasonName, ignoreCase = true) ||
                                                            Regex("(?i)^(season|staffel)\\s*\\d+$").matches(name.trim())
                                                    }
                                                    val episodeLabel = episode.episode?.let { number ->
                                                        if (episodeTitle.isNullOrBlank()) stringResource(R.string.episode_number, number)
                                                        else stringResource(R.string.episode_title, episodeTitle, number)
                                                    } ?: episodeTitle ?: episode.name
                                                    MenuButton(episodeLabel, Modifier.fillMaxWidth(), selected = episode == playbackEpisode) {
                                                        val series = playbackSeries ?: return@MenuButton
                                                        val season = overlaySeason ?: return@MenuButton
                                                        openedSeries = series
                                                        openedSeason = season
                                                        seasons = playbackSeasons
                                                        episodeVariants = playbackEpisodeVariants
                                                        selected = Destination.SERIES
                                                        showFullscreenPanel(GuidePanel.GRID)
                                                        selectTitle(PortalSection.SERIES, episode)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                    val channelsInPanel = liveCategories[channelCategory].orEmpty()
                                    if (channelsInPanel.isEmpty()) LaunchedEffect(Unit) { overlayFocus.requestFocus() }
                                    FullscreenChannelList(
                                        Modifier.weight(1f), channelsInPanel, playing, guideAppearance, scheduleBesideList,
                                        descriptionBesideSchedule = descriptionBesideSchedule,
                                        refreshKey = epgRefreshRequest + if (guideCacheLoaded) 1 else 0,
                                        programmes = ::effectiveProgrammes,
                                        onVisible = { visible ->
                                            visibleEpgChannels = visible
                                            if (!useXmlTv) loadProviderSchedules(visible)
                                        },
                                        onInteraction = { overlayInteraction++ },
                                        onScheduleFocused = { ui.overlayScheduleFocused = it },
                                        onSelect = { channel ->
                                            overlayInteraction++
                                            if (!guideAppearance.keepChannelList) showFullscreenPanel(GuidePanel.GRID)
                                            selected = Destination.LIVE
                                            playChannel(channel)
                                        }
                                    )
                                }
                                    }
                                GuidePanel.CATEGORIES -> LazyColumn(Modifier.weight(1f)) {
                                    if (seriesOverlay) {
                                        items(playbackSeasons) { season ->
                                            MenuButton(season.title.name, Modifier.fillMaxWidth(), selected = season == playbackSeason) {
                                                overlaySeason = season
                                                showFullscreenPanel(GuidePanel.CHANNELS)
                                            }
                                        }
                                    } else {
                                    items(groups) { group ->
                                        MenuButton(group, Modifier.fillMaxWidth(), selected = channelCategory == group,
                                            onLongPress = { ui.categoryMenu = PortalSection.LIVE to group }) {
                                            category = group
                                            showFullscreenPanel(GuidePanel.CHANNELS)
                                        }
                                    }
                                }
                                    }
                                GuidePanel.MENU -> LazyColumn(Modifier.weight(1f)) {
                                    items(visibleDestinations) { destination ->
                                        MenuButton(
                                            stringResource(destination.label), Modifier.fillMaxWidth(),
                                            leadingIcon = destination.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                                            badge = 1.takeIf { destination == Destination.SETTINGS && appUpdates.available },
                                            iconOnly = mainMenuStyle == MainMenuStyle.ICONS
                                        ) {
                                            fullscreen = false
                                            if (destination == Destination.PROFILES) ui.profilePickerOpen = true
                                            else if (destination == Destination.SETTINGS) openSettings() else {
                                                showFullscreenPanel(GuidePanel.GRID)
                                                guidePanel = GuidePanel.GRID
                                                lastFocused = destination
                                                pendingVodCategory = destination.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                                                selected = destination
                                                if (destination == Destination.LIVE) restoreGuideEntryFocus()
                                            }
                                        }
                                    }
                                }
                                GuidePanel.DETAILS -> {
                                    val schedule = playing?.let(::effectiveProgrammes).orEmpty()
                                    ProgrammeInfoCard(
                                        playing, currentGuideProgramme(schedule, guideNow),
                                        Modifier.fillMaxWidth().weight(1f, fill = false)
                                    )
                                }
                                GuidePanel.GRID -> Unit
                            }
                        }
                    }
                }
            }
            if (episodeAction != 0 && (fullscreenGuidePanel == GuidePanel.GRID || ui.watchingPrompt || ui.skipIntroTo != null)) {
                Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (ui.watchingPrompt) {
                            Text(stringResource(R.string.series_still_watching), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.series_still_watching_timeout))
                            Row(Modifier.focusGroup()) {
                                MenuButton(
                                    stringResource(R.string.play_next_episode),
                                    Modifier.focusRequester(episodeActionFocus),
                                    selected = episodeActionSelection == 0,
                                    onFocused = { episodeActionSelection = 0 }
                                ) { advanceEpisode(confirmed = true) }
                                MenuButton(
                                    stringResource(R.string.stop_playback),
                                    Modifier.focusRequester(episodeActionSecondaryFocus),
                                    selected = episodeActionSelection == 1,
                                    onFocused = { episodeActionSelection = 1 },
                                    onClick = ::stopPlayback
                                )
                            }
                        } else {
                        ui.skipIntroTo?.let { position ->
                            MenuButton(
                                stringResource(if (ui.skipIsOutro) R.string.skip_credits else R.string.skip_intro),
                                Modifier.focusRequester(episodeActionFocus),
                                selected = episodeActionSelection == 0,
                                onFocused = { episodeActionSelection = 0 }
                            ) {
                                player?.seekTo(position)
                                ui.skipIntroTo = null
                                val closed = playbackAttempt?.mediaId?.let { it to position }
                                if (ui.skipIsOutro) ui.outroOfferClosed = closed else ui.introOfferClosed = closed
                            }
                        }
                        ui.nextEpisodeSeconds?.let { seconds ->
                            Text(
                                if (autoNextEpisode) stringResource(R.string.episode_countdown, ui.nextEpisodeName, seconds)
                                else stringResource(R.string.next_episode_ready, ui.nextEpisodeName)
                            )
                            Row(Modifier.focusGroup()) {
                                val introOffset = if (ui.skipIntroTo != null) 1 else 0
                                MenuButton(
                                    stringResource(R.string.play_next_episode),
                                    Modifier.focusRequester(
                                        if (introOffset == 1) episodeActionSecondaryFocus else episodeActionFocus
                                    ),
                                    selected = episodeActionSelection == introOffset,
                                    onFocused = { episodeActionSelection = introOffset }
                                ) {
                                    advanceEpisode(confirmed = true)
                                }
                                MenuButton(
                                    stringResource(R.string.stop_playback),
                                    Modifier.focusRequester(
                                        if (introOffset == 1) episodeActionTertiaryFocus else episodeActionSecondaryFocus
                                    ),
                                    selected = episodeActionSelection == introOffset + 1,
                                    onFocused = { episodeActionSelection = introOffset + 1 },
                                    onClick = ::stopPlayback
                                )
                            }
                        }
                        }
                    }
                }
            }
            ui.playbackSourceBadge?.takeUnless { fullscreenGuidePanel == GuidePanel.DETAILS }?.let { (_, label) ->
                Text(
                    stringResource(R.string.playback_source_badge, label),
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            playbackAttempt?.takeIf { it.index > 0 && starting != null }?.let { attempt ->
                val portal = attempt.variants[attempt.index].label
                Text(
                    if (attempt.positionMs > 0) {
                        stringResource(R.string.playback_switching_source_at, portal, playbackTime(attempt.positionMs))
                    } else stringResource(R.string.playback_switching_source, portal),
                    Modifier.align(Alignment.TopCenter).background(MaterialTheme.colorScheme.surface).padding(12.dp)
                )
            }
            if (lastBack != 0L || playbackError != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        stringResource(playbackError ?: R.string.fullscreen_back_hint),
                        Modifier.padding(12.dp)
                    )
                }
            }
        }
        return
    }
    }
    FullscreenLayer()
    // The fullscreen layer replaces the whole screen; its own return only leaves the layer.
    if (fullscreen) return
    suspend fun loadCast(section: PortalSection, title: PortalTitle, tmdbId: Int?): List<CastMember> {
        val client = metadataClient
        if (client != null && tmdbId != null) {
            val fromTmdb = try {
                client.cast(section, tmdbId)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
            if (fromTmdb.isNotEmpty()) return fromTmdb
        }
        if (traktSession == null) return emptyList()
        // The TMDB ID lets Trakt find a localized portal title reliably.
        val lookup = title.copy(tmdbId = title.tmdbId ?: tmdbId?.toString())
        return withTraktSession { api, session -> api.cast(section, lookup, session.accessToken) }
    }
    // Cast and filmography dialogs, in their own composable method (JVM method size limit).
    @Composable
    @androidx.compose.runtime.NonSkippableComposable
    fun CastLayer() {
        // The cast of the open title is looked up in the background; "Besetzung" is only
        // offered when TMDB or Trakt know actors for it.
        val openTitle = details?.takeIf { it.first != PortalSection.LIVE }
        LaunchedEffect(openTitle, metadataClient, traktSession?.username) {
            ui.castPreview = null
            val (section, title) = openTitle ?: return@LaunchedEffect
            if (metadataClient == null && traktSession == null) return@LaunchedEffect
            // Without Trakt the TMDB match of the details arrives shortly after opening; wait for
            // it briefly. With Trakt the details skip TMDB, so the cast asks TMDB itself.
            val client = metadataClient
            val tmdbForDetails = when {
                client == null -> null
                traktSession == null -> withTimeoutOrNull(6_000) {
                    snapshotFlow { detailMetadata?.id }.filterNotNull().first()
                }
                else -> title.tmdbId?.toIntOrNull() ?: try {
                    client.lookup(section, title)?.id
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    null
                }
            }
            val members = try {
                loadCast(section, title, tmdbForDetails)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                emptyList()
            }
            ui.castPreview = title to members
        }
        val (castSection, castTitle, tmdbId) = ui.castOf ?: return
        CastDialog(
            displayVodTitle(castTitle),
            load = {
                ui.castPreview?.takeIf { it.first == castTitle && it.second.isNotEmpty() }?.second
                    ?: loadCast(castSection, castTitle, tmdbId)
            },
            onPerson = { ui.castPerson = it },
            onDismiss = { ui.castOf = null }
        )
        ui.castPerson?.let { person ->
            PersonDialog(
                person,
                load = {
                    val client = metadataClient
                    val tmdbPerson = person.tmdbId
                    val traktPerson = person.traktId
                    val credits = when {
                        client != null && tmdbPerson != null -> client.personCredits(tmdbPerson)
                        traktSession != null && traktPerson != null -> withTraktSession { api, session ->
                            api.personCredits(traktPerson, session.accessToken)
                        }
                        else -> emptyList()
                    }
                    // TMDB or Trakt list the work; the portals are only asked whether they have it.
                    resolvePersonCredits(
                        credits, sourcesForBrowsing,
                        allowed = { kind, items -> countryVariants(kind, items) },
                        lookup = { source, kind, reference -> findEquivalent(source, kind, reference) }
                    )
                },
                isSaved = ::isTitleSaved,
                onOpen = { kind, title ->
                    castReturns += CastReturn(selected, details, ui.castOf, person, kind to title)
                    ui.castPerson = null
                    ui.castOf = null
                    // A film is shown under "Filme", a series under "Serien".
                    val target = if (kind == PortalSection.MOVIES) Destination.MOVIES else Destination.SERIES
                    if (selected != target) {
                        selected = target
                        lastFocused = target
                    }
                    showTitle(kind, title)
                },
                onDismiss = { ui.castPerson = null }
            )
        }
    }
    CastLayer()
    // While the phone is used, the question appears only there; the device shows nothing of it.
    if (ui.askForCountries && !ui.remoteAccessOpen && !ui.profilePickerOpen && ui.pinRequest == null && !kidsAccountActive) {
        AlertDialog(
            onDismissRequest = { ui.askForCountries = false },
            title = { Text(stringResource(R.string.select_countries)) },
            text = { Text(stringResource(R.string.ask_countries)) },
            confirmButton = {
                TextButton(onClick = {
                    ui.askForCountries = false
                    ui.choosingCountries = true
                }) { Text(stringResource(R.string.select_countries)) }
            },
            dismissButton = {
                TextButton(onClick = { ui.askForCountries = false }) {
                    Text(stringResource(R.string.later))
                }
            }
        )
    }
    if (ui.sourceRefreshDialogOpen) {
        AlertDialog(
            onDismissRequest = { ui.sourceRefreshDialogOpen = false },
            title = { Text(stringResource(R.string.select_sources_to_refresh_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.select_sources_to_refresh))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MenuButton(stringResource(R.string.select_all_sources), Modifier.weight(1f)) {
                            ui.selectedRefreshSources = savedSources.map { it.identity }.toSet()
                        }
                        MenuButton(stringResource(R.string.clear_source_selection), Modifier.weight(1f)) {
                            ui.selectedRefreshSources = emptySet()
                        }
                    }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(savedSources, key = { it.identity }) { source ->
                            val selectedForRefresh = source.identity in ui.selectedRefreshSources
                            val host = runCatching { java.net.URI(source.address).host }.getOrNull()
                                .orEmpty().ifBlank { source.address }
                            MenuButton(
                                (if (selectedForRefresh) "✓  " else "○  ") + host,
                                Modifier.fillMaxWidth(),
                                selected = selectedForRefresh
                            ) {
                                ui.selectedRefreshSources = if (selectedForRefresh) {
                                    ui.selectedRefreshSources - source.identity
                                } else ui.selectedRefreshSources + source.identity
                            }
                        }
                    }
                }
            },
            confirmButton = {
                MenuButton(
                    stringResource(R.string.refresh_selected_sources),
                    Modifier,
                    enabled = ui.selectedRefreshSources.isNotEmpty() && !connecting
                ) {
                    val sources = savedSources.filter { it.identity in ui.selectedRefreshSources }
                    ui.sourceRefreshDialogOpen = false
                    refreshSources(sources)
                }
            },
            dismissButton = {
                MenuButton(stringResource(android.R.string.cancel), Modifier) {
                    ui.sourceRefreshDialogOpen = false
                }
            }
        )
    }
    if (ui.choosingCountries && !ui.remoteAccessOpen) {
        CountryDialog(
            knownCountries, countries,
            onApply = { saveCountries(it); ui.choosingCountries = false },
            onDismiss = { ui.choosingCountries = false }
        )
    }
    val metadataSettingsPage: @Composable () -> Unit = {
        MetadataSettings(
            connected = metadataClient != null,
            busy = ui.metadataBusy,
            error = ui.metadataError,
            onConnect = { updateMetadataConnection(it) },
            onDisconnect = { updateMetadataConnection(null) },
            onDismiss = { ui.metadataSettingsOpen = false }
        )
    }
    if (storage != null && !restoring && ui.storageError == null && savedSources.isEmpty() &&
        !hasPortal && !initialPortalPromptDismissed && !editing && !connecting &&
        // Not before the account is chosen, and never in the kids account.
        !ui.profilePickerOpen && ui.pinRequest == null && !kidsAccountActive
    ) AlertDialog(
        onDismissRequest = { initialPortalPromptDismissed = true },
        title = { Text(stringResource(R.string.first_portal_title)) },
        text = { Text(stringResource(R.string.first_portal_message)) },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    initialPortalPromptDismissed = true
                    openRemoteAccess()
                }) { Text(stringResource(R.string.remote_setup_portal)) }
                if (!remoteInputOnly(context)) TextButton(onClick = {
                    initialPortalPromptDismissed = true
                    withSettingsPin(parental.lockPortals) {
                        selected = Destination.SOURCES
                        address = ""
                        mac = ""
                        editing = true
                    }
                }) { Text(stringResource(R.string.add_portal)) }
            }
        },
        dismissButton = {
            TextButton(onClick = { initialPortalPromptDismissed = true }) { Text(stringResource(R.string.later)) }
        }
    )
    val epgSettingsPage: @Composable () -> Unit = {
        SettingsDialog(stringResource(R.string.epg_settings), { ui.epgSettingsOpen = false }) {
                SettingRow(stringResource(R.string.epg_provider), Modifier, selected = !useXmlTv) {
                    useXmlTv = false
                    preferences?.edit { putBoolean("xmltv_enabled", false) }
                }
                SettingRow(stringResource(R.string.epg_custom), Modifier, selected = useXmlTv) {
                    useXmlTv = true
                    preferences?.edit { putBoolean("xmltv_enabled", true) }
                }
                if (useXmlTv) {
                    xmlTvSources.forEachIndexed { index, url ->
                        SettingRow(
                            "${index + 1}. ${guideSourceUri(url)?.host.orEmpty()}: " + stringResource(R.string.epg_remove_source),
                            enabled = !ui.epgSourceSaving
                        ) { saveEpgSources(xmlTvSources.filterNot { it == url }) }
                    }
                    OutlinedTextField(
                        value = epgSourceDraft, onValueChange = { if (it.length <= 2_048) epgSourceDraft = it },
                        label = { Text(stringResource(R.string.epg_source_url)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    val source = guideSourceUri(epgSourceDraft)?.toASCIIString()
                    SettingRow(stringResource(R.string.epg_add_source), enabled = source != null && source !in xmlTvSources && !ui.epgSourceSaving && xmlTvSourcesLoaded) { source?.let { saveEpgSources(xmlTvSources + it) } }
                }
                SettingRow(stringResource(R.string.epg_refresh_at_start), Modifier, selected = epgAtStartup) {
                    epgAtStartup = !epgAtStartup
                    preferences?.edit { putBoolean("epg_at_startup", epgAtStartup) }
                }
                val refreshLabel = if (epgRefreshHours == 0) stringResource(R.string.epg_refresh_off)
                    else stringResource(R.string.epg_refresh_hours, epgRefreshHours)
                SettingRow(refreshLabel, Modifier) {
                    val intervals = listOf(0, 1, 3, 6, 12, 24)
                    epgRefreshHours = intervals[(intervals.indexOf(epgRefreshHours) + 1) % intervals.size]
                    preferences?.edit { putInt("epg_refresh_hours", epgRefreshHours) }
                }
                val horizon = if (epgHorizonHours < 24) stringResource(R.string.epg_schedule_hours, epgHorizonHours)
                    else if (epgHorizonHours == 24) stringResource(R.string.epg_schedule_day)
                    else stringResource(R.string.epg_schedule_days, epgHorizonHours / 24)
                SettingRow(horizon, Modifier) {
                    val windows = listOf(6, 12, 24, 48, 72, 168)
                    epgHorizonHours = windows[(windows.indexOf(epgHorizonHours) + 1) % windows.size]
                    preferences?.edit { putInt("epg_horizon_hours", epgHorizonHours) }
                    epgRefreshRequest++
                }
                SettingRow(stringResource(R.string.epg_refresh_now), enabled = !ui.epgUpdating && (!useXmlTv || xmlTvSources.isNotEmpty())) { epgRefreshRequest++ }
                if (ui.epgUpdating) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (ui.epgUpdateFailed || ui.epgSourceWriteFailed) {
                    Text(stringResource(R.string.epg_refresh_failed), color = MaterialTheme.colorScheme.error)
                }
        }
    }
    val episodeSettingsPage: @Composable () -> Unit = {
        SettingsDialog(stringResource(R.string.episode_settings), { ui.episodeSettingsOpen = false }) {
                SettingRow(stringResource(R.string.auto_next_episode), Modifier, selected = autoNextEpisode) {
                    autoNextEpisode = !autoNextEpisode
                    preferences?.edit { putBoolean("auto_next_episode", autoNextEpisode) }
                }
                SettingRow(
                    stringResource(R.string.series_inactivity_check), Modifier, selected = checkSeriesInactivity
                ) {
                    checkSeriesInactivity = !checkSeriesInactivity
                    preferences?.edit { putBoolean("check_series_inactivity", checkSeriesInactivity) }
                }
                if (checkSeriesInactivity) {
                    Text(stringResource(R.string.series_inactive_episode_limit, inactiveEpisodeLimit))
                    Row {
                        MenuButton("−1", Modifier) {
                            inactiveEpisodeLimit = (inactiveEpisodeLimit - 1).coerceAtLeast(1)
                            preferences?.edit { putInt("inactive_episode_limit", inactiveEpisodeLimit) }
                        }
                        MenuButton("+1", Modifier) {
                            inactiveEpisodeLimit = (inactiveEpisodeLimit + 1).coerceAtMost(10)
                            preferences?.edit { putInt("inactive_episode_limit", inactiveEpisodeLimit) }
                        }
                    }
                }
                SettingRow(stringResource(R.string.skip_intro_automatically), Modifier, selected = autoSkipIntro) {
                    autoSkipIntro = !autoSkipIntro
                    preferences?.edit { putBoolean("auto_skip_intro", autoSkipIntro) }
                }
                SettingRow(stringResource(R.string.decode_audio_for_intro), Modifier, selected = decodeAudioForIntro) {
                    decodeAudioForIntro = !decodeAudioForIntro
                    preferences?.edit { putBoolean("decode_audio_for_intro", decodeAudioForIntro) }
                }
                Text(stringResource(R.string.decode_audio_for_intro_hint), style = MaterialTheme.typography.bodySmall)
                SettingRow(
                    stringResource(if (manualIntro) R.string.intro_manual else R.string.intro_automatic),
                    Modifier
                ) {
                    manualIntro = !manualIntro
                    preferences?.edit { putBoolean("manual_intro", manualIntro) }
                }
                if (manualIntro) {
                    EpisodeTimingAdjustment(
                        stringResource(R.string.intro_start_time, playbackTime(introStartSeconds * 1_000L)),
                        introStartSeconds, 0, introEndSeconds - 5
                    ) {
                        introStartSeconds = it
                        preferences?.edit { putInt("intro_start_seconds", it) }
                    }
                    EpisodeTimingAdjustment(
                        stringResource(R.string.intro_end_time, playbackTime(introEndSeconds * 1_000L)),
                        introEndSeconds, introStartSeconds + 5, 600
                    ) {
                        introEndSeconds = it
                        preferences?.edit { putInt("intro_end_seconds", it) }
                    }
                }
                SettingRow(
                    stringResource(if (manualCredits) R.string.credits_manual else R.string.credits_automatic),
                    Modifier
                ) {
                    manualCredits = !manualCredits
                    preferences?.edit { putBoolean("manual_credits", manualCredits) }
                }
                if (manualCredits) {
                    EpisodeTimingAdjustment(
                        stringResource(R.string.credits_before_end, playbackTime(creditsBeforeEndSeconds * 1_000L)),
                        creditsBeforeEndSeconds, 0, 600
                    ) {
                        creditsBeforeEndSeconds = it
                        preferences?.edit { putInt("credits_before_end_seconds", it) }
                    }
                }
                Text(stringResource(R.string.episode_notice_seconds, episodeNoticeSeconds))
                Row {
                    MenuButton("−5", Modifier) {
                        episodeNoticeSeconds = (episodeNoticeSeconds - 5).coerceAtLeast(5)
                        preferences?.edit { putInt("episode_notice_seconds", episodeNoticeSeconds) }
                    }
                    MenuButton("+5", Modifier) {
                        episodeNoticeSeconds = (episodeNoticeSeconds + 5).coerceAtMost(60)
                        preferences?.edit { putInt("episode_notice_seconds", episodeNoticeSeconds) }
                    }
                }
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().then(if (selected == Destination.SEARCH) Modifier.imePadding() else Modifier),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { androidx.compose.material3.SnackbarHost(playbackNoticeHost) }
    ) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
        val vodSectionForScreen = when (selected) {
            Destination.MOVIES -> PortalSection.MOVIES
            Destination.SERIES -> PortalSection.SERIES
            else -> null
        }
        if (vodSectionForScreen != null && cataloguePlaybackView == VodLiveView.BACKGROUND &&
            playing != null && player != null
        ) {
            Box(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                AndroidView(
                    factory = { viewContext ->
                        (android.view.LayoutInflater.from(viewContext)
                            .inflate(R.layout.vod_live_backdrop, android.widget.FrameLayout(viewContext), false) as PlayerView).apply { this.player = player }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onReset = null,
                    onRelease = { it.player = null },
                    update = {}
                )
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)))
            }
        }
        val compactGuideWindow = inGuide && with(LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.height.toDp() } < 440.dp
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = if (compactGuideWindow) 10.dp else 24.dp,
                    vertical = if (compactGuideWindow) 4.dp else 16.dp
                )
        ) {
            val destination = selected
            val compactMedia = maxHeight < 400.dp
            val cornerPreviewWidth = (maxWidth * 0.18f).coerceIn(90.dp, 164.dp)
            val visibleVodGroups = vodSectionForScreen?.let { section ->
                withCategoryOrder(
                    section,
                    defaultVodCategoryOrder(section).filter { visibleCategory(section, it.id, it.name, it.country) }
                ) { it.id }
            }.orEmpty()
            val recentCategoryKey = "last_vod_category_${vodSectionForScreen?.name}"
            fun destinationFocusRequester(item: Destination): FocusRequester = when {
                item == lastFocused && !inGuide && destination != Destination.SEARCH &&
                    destination != Destination.SETTINGS && destination != Destination.SOURCES &&
                    destination != Destination.CATEGORIES -> initialFocus
                inGuide && item == Destination.LIVE -> guidePanelFocus
                else -> destinationFocusRequesters.getValue(item)
            }
            fun openVodCategory(group: PortalGroup) {
                val section = vodSectionForScreen ?: return
                ui.catalogueReturnTitle = null
                ui.focusFirstTitle = true
                preferences?.edit { putString(recentCategoryKey, group.id) }
                closeSeries()
                details = null
                myAreaSection = null
                if (!combine && group.id == ALL_CATALOG_ITEMS) {
                    closeGroup()
                    selectedGroup = section to group
                } else loadGroup(section, group)
            }
            val compactGuide = inGuide && (maxHeight < 400.dp || maxWidth < 680.dp)
            val basePreviewWidth = if (compactGuide) {
                minOf(maxWidth * 0.24f, maxHeight * 0.43f).coerceAtLeast(76.dp)
            } else (maxWidth * 0.25f).coerceIn(128.dp, 250.dp)
            val guidePreviewWidth = (basePreviewWidth * guideAppearance.previewSize.factor)
                .coerceAtMost(minOf(maxWidth * 0.45f, maxHeight * 0.7f))
            val menu: @Composable () -> Unit = {
                StreamWatchLogo(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), height = 26.dp)
                visibleDestinations.forEach { item ->
                    val position = visibleDestinations.indexOf(item)
                    val previousItem = visibleDestinations.getOrNull(position - 1) ?: item
                    val nextItem = visibleDestinations.getOrNull(position + 1) ?: item
                    MenuButton(
                        label = stringResource(item.label),
                        leadingIcon = item.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                        badge = 1.takeIf { item == Destination.SETTINGS && appUpdates.available },
                        iconOnly = mainMenuStyle == MainMenuStyle.ICONS,
                        tintSelection = true,
                        modifier = Modifier.fillMaxWidth()
                            .focusRequester(destinationFocusRequester(item))
                            .focusProperties {
                                up = destinationFocusRequester(previousItem)
                                down = destinationFocusRequester(nextItem)
                            },
                        selected = item == (destination ?: Destination.LIVE) ||
                            (item == Destination.SETTINGS &&
                                destination in listOf(Destination.SOURCES, Destination.CATEGORIES)),
                        onClick = {
                            if (item == Destination.PROFILES) ui.profilePickerOpen = true
                            else if (item == Destination.SETTINGS) openSettings() else {
                                lastFocused = item
                                guidePanel = GuidePanel.GRID
                                if (item == Destination.LIVE) restoreGuideEntryFocus()
                                pendingVodCategory = item.takeIf { it == Destination.MOVIES || it == Destination.SERIES }
                                selected = item
                            }
                        }
                    )
                }
                if (mainMenuOpen && lastBack != 0L) {
                    Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            val topNavigation: @Composable () -> Unit = {
                Row(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                        .focusGroup().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreamWatchLogo(height = 28.dp)
                    Row(
                        Modifier.weight(1f).focusGroup().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        visibleDestinations.forEachIndexed { index, item ->
                            val previous = visibleDestinations.getOrNull(index - 1) ?: item
                            val next = visibleDestinations.getOrNull(index + 1) ?: item
                            MenuButton(
                                stringResource(item.label),
                                Modifier.focusRequester(destinationFocusRequesters.getValue(item))
                                    .focusProperties {
                                        left = destinationFocusRequesters.getValue(previous)
                                        right = destinationFocusRequesters.getValue(next)
                                    },
                                leadingIcon = item.icon.takeUnless { mainMenuStyle == MainMenuStyle.TEXT },
                                badge = 1.takeIf { item == Destination.SETTINGS && appUpdates.available },
                                iconOnly = mainMenuStyle == MainMenuStyle.ICONS,
                                tintSelection = true,
                                selected = item == (destination ?: Destination.LIVE) ||
                                    item == Destination.SETTINGS && destination in setOf(
                                        Destination.SOURCES, Destination.CATEGORIES
                                    ),
                                onClick = {
                                    if (item == Destination.PROFILES) ui.profilePickerOpen = true
                                    else if (item == Destination.SETTINGS) openSettings() else {
                                        lastFocused = item
                                        guidePanel = GuidePanel.GRID
                                        if (item == Destination.LIVE) restoreGuideEntryFocus()
                                        pendingVodCategory = item.takeIf {
                                            it == Destination.MOVIES || it == Destination.SERIES
                                        }
                                        selected = item
                                    }
                                }
                            )
                        }
                    }
                }
            }
            val header: @Composable () -> Unit = {
                val portalRefreshText = ui.startupPortalRefresh?.let { (number, total) ->
                    val source = savedSources.filter { it.enabled || it.autoDisabled }.getOrNull(number - 1)
                    val host = source?.address?.let { runCatching { java.net.URI(it).host }.getOrNull() }
                        ?.takeIf(String::isNotBlank) ?: "Portal"
                    stringResource(R.string.portal_refresh_progress, number, total, host)
                }
                val portalCheckText = ui.startupPortalCheck?.let { (number, total) ->
                    stringResource(R.string.portal_check_progress, number, total)
                }
                val busy = restoring || connecting || ui.liveGroupsLoading || ui.traktBusy || portalRefreshText != null ||
                    portalCheckText != null || ui.importStatuses.values.any { it == ImportStatus.Loading || it is ImportStatus.Progress }
                // All background activity in one line next to the category; together with the fixed
                // progress strip above, the screen below never moves while something loads.
                val statusLine = if (!busy) "" else buildList {
                    if (restoring) add(stringResource(R.string.startup_loading))
                    if (ui.liveGroupsLoading) add(stringResource(R.string.live_groups_loading))
                    portalRefreshText?.let(::add)
                    portalCheckText?.let(::add)
                    ui.batchProgress?.let { (number, total) -> add(stringResource(R.string.portal_import_progress, number, total)) }
                    if (ui.traktBusy) ui.traktStatus?.let(::add)
                    ui.importStatuses.forEach { (section, status) ->
                        when (status) {
                            ImportStatus.Loading -> add(stringResource(R.string.section_loading, stringResource(section.label)))
                            is ImportStatus.Progress -> add(
                                "${stringResource(section.label)}: ${status.loaded}" + (status.total?.let { " / $it" } ?: "")
                            )
                            else -> Unit
                        }
                    }
                }.joinToString("  ·  ")
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The top menu already marks Live-TV, so the guide shows its category only.
                    if (vodSectionForScreen == null && destination != Destination.SEARCH && !inGuide) Text(
                        text = stringResource(destination?.label ?: R.string.destination_live),
                        style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall
                    )
                    if (inGuide) {
                        MenuButton(category ?: stringResource(R.string.guide_all_channels),
                            Modifier.focusRequester(initialFocus).focusProperties {
                                canFocus = guidePanel == GuidePanel.GRID
                                down = guideChannelFocus
                            }
                        ) {
                            guidePanel = GuidePanel.CATEGORIES
                        }
                    }
                    Text(
                        statusLine, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (inGuide) GuideWindowControls(
                        guideWindow, epgHorizonHours,
                        Modifier.focusProperties {
                            canFocus = guidePanel == GuidePanel.GRID
                            down = guideChannelFocus
                        }
                    )
                    if (vodSectionForScreen != null && cataloguePlaybackView == VodLiveView.CORNER) {
                        playing?.let { channel ->
                            Box(Modifier.width(cornerPreviewWidth)) {
                                PlayerFrame(
                                    player, active = true, onStop = ::stopPlayback,
                                    onSurface = { ui.playbackVideoSurface = it },
                                    onOpenFullscreen = { if (vodSection == null) playChannel(channel) else fullscreen = true }
                                )
                            }
                        }
                    }
                }
            }
            val content: @Composable ColumnScope.() -> Unit = {
                when (destination) {
                    null -> LiveOverview(
                        player = player,
                        showBackButton = showSettingsBackButton,
                        groups = liveCategories,
                        loading = liveGroups.isEmpty() && sourcesForBrowsing.any { it.channels.isNotEmpty() },
                        visibleGroups = liveCategories.keys.filter {
                            visibleCategory(PortalSection.LIVE, it, it)
                        }.sortedBy { categoryNames.values.indexOf(it) },
                        hiddenGroups = hiddenCategories,
                        onManageGroups = { ui.categoryMenu = PortalSection.LIVE to it },
                        category = category,
                        onCategory = {
                            if (it == null) returnToCategories()
                            else {
                                lastLiveCategory = it
                                category = it
                            }
                        },
                        categoryScroll = categoryScroll,
                        channelScroll = channelScroll,
                        lastCategory = lastLiveCategory,
                        restoreFocus = ui.restoreLiveFocus,
                        onFocusRestored = { ui.restoreLiveFocus = false },
                        playing = playing.takeIf { vodSection == null },
                        starting = starting.takeIf { vodSection == null },
                        programmeChannel = programmeChannel,
                        programmes = programmes,
                        variants = programmeChannel?.let { channel ->
                            liveGroups[channel]?.variants.orEmpty().map { variant ->
                                val host = sessionFor(variant.item.source).sourceLabel
                                val quality = variant.description.qualities.joinToString(" / ")
                                    .ifBlank { stringResource(R.string.live_quality_unknown) }
                                "$quality · $host"
                            }
                        }.orEmpty(),
                        epgPinned = pinnedProgramme,
                        onToggleEpg = ::toggleProgrammeChannel,
                        onHighlight = { programmeChannel = it },
                        playbackError = playbackError.takeIf { vodSection == null },
                        onStop = ::stopPlayback,
                        onSelect = ::playChannel
                    )
                    Destination.SETTINGS -> {
                        val section = settingsSection
                        // The sections are chosen in the bar above; there is no separate section list.
                        if (section != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.padding(end = 12.dp).width(4.dp).height(34.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                stringResource(R.string.destination_settings).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            SettingsHeading(section.label)
                                        }
                                        if (showSettingsBackButton) MenuButton(
                                            stringResource(R.string.navigation_back),
                                            Modifier.focusRequester(initialFocus)
                                        ) { navigateBack() }
                                    }
                                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    // A page opened from a row ("›") replaces the rows on the right;
                                    // "Zurück" returns to the section.
                                    val subpage: (@Composable () -> Unit)? = when {
                                        ui.epgSettingsOpen -> epgSettingsPage
                                        ui.episodeSettingsOpen -> episodeSettingsPage
                                        ui.controlSettingsOpen -> controlSettingsPage
                                        ui.remoteAccessOpen -> remoteAccessPage
                                        ui.metadataSettingsOpen -> metadataSettingsPage
                                        ui.languageOpen -> { {
                                            SettingsDialog(stringResource(R.string.app_language), { ui.languageOpen = false }) {
                                                AppLanguageSettings(remember { chosenLanguage(context) }, remember { mirrorLayout(context) }) { tag, mirror ->
                                                    // The new language applies at once: the screen is built again.
                                                    saveLanguage(context, tag)
                                                    saveMirrorLayout(context, mirror)
                                                    (context as? android.app.Activity)?.recreate()
                                                }
                                            }
                                        } }
                                        ui.termsOpen -> { {
                                            SettingsDialog(stringResource(R.string.disclaimer_title), { ui.termsOpen = false }) { DisclaimerText() }
                                        } }
                                        ui.whatsNewOpen -> { {
                                            SettingsDialog(stringResource(R.string.whats_new), { ui.whatsNewOpen = false }) { WhatsNewText() }
                                        } }
                                        ui.kidsColorOpen -> { {
                                            // The kids account's own accent colour, chosen by the admin.
                                            val kidsAccent = remember(ui.kidsAccentVersion) {
                                                AccentChoice.entries.firstOrNull {
                                                    it.name == preferences?.getString(UserProfiles.accentKey(true), null)
                                                } ?: AccentChoice.RED
                                            }
                                            SettingsDialog(
                                                stringResource(R.string.kids_profile_color, profiles.kidsName.orEmpty()),
                                                { ui.kidsColorOpen = false }
                                            ) {
                                                val kidsGradient = remember(ui.kidsAccentVersion) {
                                                    AccentGradient.parse(preferences?.getString(AccentGradient.key(true), null))
                                                }
                                                AccentPalette(kidsAccent, kidsGradient, { choice ->
                                                    preferences?.edit {
                                                        putString(UserProfiles.accentKey(true), choice.name)
                                                        remove(AccentGradient.key(true))
                                                    }
                                                    ui.kidsAccentVersion++
                                                }) { gradient ->
                                                    preferences?.edit { putString(AccentGradient.key(true), gradient.name) }
                                                    ui.kidsAccentVersion++
                                                }
                                            }
                                        } }
                                        // Opened from the Kinderschutz page (Trakt of the kids account): shown above it.
                                        ui.traktSettingsOpen -> { {
                                            SettingsDialog(
                                                if (ui.traktForKids) stringResource(R.string.kids_profile_trakt, profiles.kidsName.orEmpty())
                                                else stringResource(R.string.trakt_settings),
                                                { ui.traktSettingsOpen = false; ui.traktForKids = false }
                                            ) {
                                                TraktSettings(
                                                    accountName = if (ui.traktForKids) ui.kidsTraktName else traktSession?.username,
                                                    app = traktApp ?: traktSession?.takeIf { ui.traktForKids }?.let { TraktApp(it.clientId, it.clientSecret) },
                                                    onSaveApp = ::saveTraktApp,
                                                    deviceCode = ui.traktDeviceCode,
                                                    busy = ui.traktBusy,
                                                    status = ui.traktStatus,
                                                    onConnect = ::connectTrakt,
                                                    onSync = {
                                                        // The kids account syncs when it signs in with its own data.
                                                        if (ui.traktForKids) ui.traktStatus = context.getString(R.string.kids_profile_trakt_sync, profiles.kidsName.orEmpty())
                                                        else syncTrakt(forceSeriesHistory = true)
                                                    },
                                                    onDisconnect = ::disconnectTrakt,
                                                    remoteOnly = remoteInputOnly(context),
                                                    onRemote = {
                                                        ui.traktSettingsOpen = false
                                                        ui.traktForKids = false
                                                        openSettingsPage { ui.remoteAccessOpen = true }
                                                    }
                                                )
                                            }
                                        } }
                                        ui.kidsProtectionOpen -> { {
                                            SettingsDialog(stringResource(R.string.kids_protection), { ui.kidsProtectionOpen = false }) {
                                                ParentalSettings(parental, { ui.pinRequest = it }, profiles.hasKids) { onShared, onOwn ->
                                                    ui.sharedPinQuestion = SharedPinQuestion(context.getString(R.string.parental_pin_same_question), onShared, onOwn)
                                                }
                                                KidsProfileSettings(
                                                    profiles, parental,
                                                    onCreate = { ui.kidsNameDialog = true },
                                                    onRename = { ui.kidsNameDialog = true },
                                                    onColor = { openSettingsPage { ui.kidsColorOpen = true } },
                                                    onTrakt = { openSettingsPage { ui.traktForKids = true; ui.traktSettingsOpen = true } },
                                                    kidsTraktName = ui.kidsTraktName,
                                                    adminTraktConnected = traktSession != null,
                                                    onRemove = {
                                                        ui.pinRequest = PinRequest(context.getString(R.string.pin_enter), null,
                                                            check = parental::checkPin, length = parental.pinLength) { profiles.removeKids() }
                                                    }
                                                )
                                            }
                                        } }
                                        ui.accentPaletteOpen -> { {
                                            AccentPalette(accentChoice, accentGradient, onAccentChoiceChange, onAccentGradientChange)
                                        } }
                                        else -> ui.guideAppearancePage?.let { page -> { guideAppearanceSettingsPage(page) } }
                                    }
                                    if (subpage != null) {
                                        androidx.compose.runtime.CompositionLocalProvider(LocalSettingsInline provides true) { subpage() }
                                    } else when (section) {
                                SettingsSection.GENERAL -> {
                                    SettingRow(
                                        stringResource(R.string.app_language_setting,
                                            chosenLanguage(context)?.let { tag -> APP_LANGUAGES.firstOrNull { it.tag == tag }?.name }
                                                ?: stringResource(R.string.app_language_system_short)),
                                        Modifier
                                    ) { openSettingsPage { ui.languageOpen = true } }
                                    SettingRow(stringResource(R.string.remote_access), Modifier) {
                                        // The phone can change portals and categories: it needs the PIN when they are protected.
                                        withSettingsPin(parental.lockPortals || parental.lockCategories) {
                                            openSettingsPage { ui.remoteAccessOpen = true }
                                        }
                                    }
                                    AutoStartSettings()
                                    SettingRow(
                                        stringResource(R.string.show_touch_back_button), Modifier,
                                        selected = showSettingsBackButton
                                    ) {
                                        showSettingsBackButton = !showSettingsBackButton
                                        preferences?.edit { putBoolean("show_settings_back_button", showSettingsBackButton) }
                                    }
                                    SettingRow(
                                        stringResource(R.string.exit_confirmation_setting, stringResource(exitConfirmation.label)), Modifier
                                    ) {
                                        exitConfirmation = ExitConfirmation.entries[(exitConfirmation.ordinal + 1) % ExitConfirmation.entries.size]
                                        preferences?.edit { putString("exit_confirmation", exitConfirmation.name) }
                                    }
                                    SearchHistorySettings()
                                    InputModeSetting()
                                    SettingRow(stringResource(R.string.key_sounds), Modifier, selected = keySounds) {
                                        keySounds = !keySounds
                                        preferences?.edit { putBoolean("key_sounds", keySounds) }
                                    }
                                    SettingRow(
                                        stringResource(R.string.start_destination_setting, stringResource(startDestination.label)),
                                        Modifier
                                    ) {
                                        startDestination = START_DESTINATIONS[
                                            (START_DESTINATIONS.indexOf(startDestination) + 1) % START_DESTINATIONS.size
                                        ]
                                        preferences?.edit { putString("start_destination", startDestination.name) }
                                    }
                                    // Playing the last channel (and fullscreen) only exists with Live-TV as the start page.
                                    if (startDestination == Destination.LIVE) {
                                        SettingRow(
                                            stringResource(R.string.start_last_channel),
                                            Modifier,
                                            selected = playLastChannelAtStartup
                                        ) {
                                            playLastChannelAtStartup = !playLastChannelAtStartup
                                            preferences?.edit { putBoolean("play_last_channel_at_startup", playLastChannelAtStartup) }
                                        }
                                        if (playLastChannelAtStartup) SettingRow(
                                            stringResource(R.string.start_in_fullscreen), Modifier, selected = startInFullscreen
                                        ) {
                                            startInFullscreen = !startInFullscreen
                                            preferences?.edit { putBoolean("start_in_fullscreen", startInFullscreen) }
                                        }
                                    }
                                }
                                SettingsSection.SOURCES -> {
                                    SettingRow(
                                        stringResource(R.string.portal_refresh_at_start), Modifier,
                                        selected = refreshPortalsAtStartup
                                    ) {
                                        refreshPortalsAtStartup = !refreshPortalsAtStartup
                                        preferences?.edit { putBoolean("refresh_portals_at_startup", refreshPortalsAtStartup) }
                                    }
                                    SettingRow(
                                        label = stringResource(R.string.destination_sources),
                                        modifier = Modifier,
                                        onClick = { withSettingsPin(parental.lockPortals) { selected = Destination.SOURCES } }
                                    )
                                    SettingRow(stringResource(R.string.category_settings), Modifier) {
                                        withSettingsPin(parental.lockCategories) { selected = Destination.CATEGORIES }
                                    }
                                    SettingRow(
                                        stringResource(R.string.expiry_lead_setting, expiryLeadLabel(context, expiryLead)), Modifier
                                    ) { ui.expiryLeadPickerOpen = true }
                                    SettingRow(
                                        stringResource(R.string.auto_disable_unavailable_portals), Modifier,
                                        selected = autoDisableUnavailablePortals
                                    ) {
                                        autoDisableUnavailablePortals = !autoDisableUnavailablePortals
                                        preferences?.edit { putBoolean("auto_disable_unavailable_portals", autoDisableUnavailablePortals) }
                                    }
                                }
                                SettingsSection.GUIDE ->
                                    SettingRow(stringResource(R.string.epg_settings), Modifier) { openSettingsPage { ui.epgSettingsOpen = true } }
                                SettingsSection.APPEARANCE -> {
                                    SettingRow(
                                stringResource(R.string.accent_color_setting, stringResource(accentGradient?.label ?: accentChoice.label)),
                                Modifier
                            ) { openSettingsPage { ui.accentPaletteOpen = true } }
                            SettingRow(
                                        stringResource(R.string.menu_style_setting, stringResource(mainMenuStyle.label)),
                                        Modifier
                                    ) {
                                        val styles = MainMenuStyle.entries
                                        mainMenuStyle = styles[(mainMenuStyle.ordinal + 1) % styles.size]
                                        preferences?.edit { putString("main_menu_style", mainMenuStyle.name) }
                                    }
                                    SettingRow(
                                        stringResource(R.string.provider_category_style_setting, stringResource(providerCategoryStyle.label)),
                                        Modifier
                                    ) {
                                        providerCategoryStyle = ProviderCategoryStyle.entries[
                                            (providerCategoryStyle.ordinal + 1) % ProviderCategoryStyle.entries.size
                                        ]
                                        preferences?.edit { putString("provider_category_style", providerCategoryStyle.name) }
                                    }
                                    SettingRow(stringResource(R.string.guide_grid_appearance), Modifier) {
                                        openSettingsPage { ui.guideAppearancePage = GuideAppearancePage.GRID }
                                    }
                                    SettingRow(stringResource(R.string.guide_channel_list_appearance), Modifier) {
                                        openSettingsPage { ui.guideAppearancePage = GuideAppearancePage.CHANNELS }
                                    }
                                }
                                SettingsSection.PLAYBACK -> {
                                    BufferSettings()
                                    TimeshiftSettings()
                                    StreamSwitchSettings()
                                    SettingRow(stringResource(R.string.episode_settings), Modifier) { openSettingsPage { ui.episodeSettingsOpen = true } }
                                    SettingRow(
                                        stringResource(
                                            if (subtitleDefaultEnabled) R.string.subtitles_default_on
                                            else R.string.subtitles_default_off
                                        ), Modifier, selected = subtitleDefaultEnabled
                                    ) {
                                        subtitleDefaultEnabled = !subtitleDefaultEnabled
                                        preferences?.edit { putBoolean("subtitle_default_enabled", subtitleDefaultEnabled) }
                                    }
                                    SettingRow(
                                        stringResource(R.string.subtitles_forced_auto), Modifier,
                                        selected = forcedSubtitlesEnabled
                                    ) {
                                        forcedSubtitlesEnabled = !forcedSubtitlesEnabled
                                        preferences?.edit { putBoolean("forced_subtitles_enabled", forcedSubtitlesEnabled) }
                                    }
                                    SettingRow(stringResource(R.string.live_preview_in_vod, stringResource(vodLiveView.label)), Modifier) {
                                        vodLiveView = VodLiveView.entries[(vodLiveView.ordinal + 1) % VodLiveView.entries.size]
                                        preferences?.edit { putString("vod_live_view", vodLiveView.name) }
                                    }
                                    for ((section, label) in listOf(
                                        PortalSection.MOVIES to R.string.vod_movies_playback,
                                        PortalSection.SERIES to R.string.vod_series_playback
                                    )) {
                                        val view = vodPlaybackViews.getValue(section)
                                        SettingRow(stringResource(label, stringResource(
                                            if (view == VodLiveView.HIDDEN) R.string.vod_playback_off else view.label
                                        )), Modifier) {
                                            val next = VodLiveView.entries[(view.ordinal + 1) % VodLiveView.entries.size]
                                            vodPlaybackViews[section] = next
                                            preferences?.edit { putString("vod_view_${section.name}", next.name) }
                                            if (next == VodLiveView.HIDDEN && vodSection == section) player?.pause()
                                        }
                                    }
                                }
                                SettingsSection.CONTROLS ->
                                    SettingRow(stringResource(R.string.control_settings), Modifier) { openSettingsPage { ui.controlSettingsOpen = true } }
                                SettingsSection.UPDATES -> appUpdates.settings()
                                // "Sperren": the Kinderschutz opens as its own page; below it the settings protection.
                                SettingsSection.PARENTAL -> {
                                    SettingRow(
                                        listOf(
                                            stringResource(R.string.kids_protection),
                                            stringResource(if (parental.hasPin) R.string.kids_protection_on else R.string.kids_protection_off)
                                        ).joinToString(" · "), Modifier
                                    ) { openSettingsPage { ui.kidsProtectionOpen = true } }
                                    Text(stringResource(R.string.settings_protection), style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    SettingsProtection(parental, { ui.pinRequest = it }) { onShared, onOwn ->
                                        ui.sharedPinQuestion = SharedPinQuestion(context.getString(R.string.settings_pin_same_question), onShared, onOwn)
                                    }
                                }
                                SettingsSection.ABOUT -> {
                                    AboutSettings()
                                    SettingRow(stringResource(R.string.whats_new), Modifier) { openSettingsPage { ui.whatsNewOpen = true } }
                                    SettingRow(stringResource(R.string.disclaimer_title), Modifier) { openSettingsPage { ui.termsOpen = true } }
                                }
                                SettingsSection.SMART_HOME -> SmartHomeSettings { openSettingsPage { ui.remoteAccessOpen = true } }
                                SettingsSection.DIAGNOSTICS -> {
                                    // The backup holds portals and accounts, so it follows the portal lock.
                                    BackupSettings(storage, guard = { action -> withSettingsPin(parental.lockPortals, action = action) }) {
                                        openSettingsPage { ui.remoteAccessOpen = true }
                                    }
                                    Text(stringResource(R.string.diagnostics_title), style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.diagnostics_description))
                                    Text(stringResource(
                                        if (diagnosticsTree == null) R.string.diagnostics_folder_missing
                                        else R.string.diagnostics_folder_selected
                                    ))
                                    // Works without a folder picker (Fire TV has none) and without a permission.
                                    SettingRow(
                                        stringResource(R.string.diagnostics_save_downloads), Modifier,
                                        enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
                                    ) {
                                        scope.launch {
                                            ui.diagnosticsStatus = try {
                                                val name = withContext(Dispatchers.IO) { AppDiagnostics.saveToDownloads(context) }
                                                context.getString(R.string.diagnostics_saved_downloads, name)
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (_: Exception) {
                                                context.getString(R.string.diagnostics_export_failed)
                                            }
                                        }
                                    }
                                    SettingRow(stringResource(R.string.diagnostics_choose_folder), Modifier) {
                                        runCatching { chooseDiagnosticsFolder.launch(null) }
                                            .onFailure { ui.diagnosticsStatus = context.getString(R.string.diagnostics_folder_open_failed) }
                                    }
                                    SettingRow(stringResource(R.string.diagnostics_save_file), Modifier) {
                                        runCatching { saveDiagnosticsFile.launch("StreamWatch-Diagnose.txt") }
                                            .onFailure { ui.diagnosticsStatus = context.getString(R.string.diagnostics_folder_open_failed) }
                                    }
                                    SettingRow(stringResource(R.string.diagnostics_export), Modifier, enabled = diagnosticsTree != null) {
                                        diagnosticsTree?.let { tree ->
                                            runCatching { AppDiagnostics.export(context, tree) }
                                                .onSuccess { ui.diagnosticsStatus = context.getString(R.string.diagnostics_export_complete) }
                                                .onFailure { ui.diagnosticsStatus = context.getString(R.string.diagnostics_export_failed) }
                                        }
                                    }
                                    ui.diagnosticsStatus?.let { Text(it) }
                                }
                                // Accounts and metadata in one place: each row opens its page on the right.
                                SettingsSection.SYNC -> {
                                    SettingRow(
                                        listOf(
                                            stringResource(R.string.trakt_settings),
                                            traktSession?.username?.let { stringResource(R.string.trakt_row_connected, it) }
                                                ?: stringResource(R.string.trakt_row_not_connected)
                                        ).joinToString(" · "), Modifier
                                    ) { openSettingsPage { ui.traktForKids = false; ui.traktSettingsOpen = true } }
                                    SettingRow(stringResource(R.string.tmdb_settings), Modifier) {
                                        openSettingsPage { ui.metadataSettingsOpen = true }
                                    }
                                    Text(stringResource(R.string.episode_provider_active, activeEpisodeProvider))
                                    if (activeEpisodeProvider == "TVmaze") {
                                        SettingRow(stringResource(R.string.tvmaze_credit), Modifier) {
                                            uriHandler.openUri("https://www.tvmaze.com/")
                                        }
                                    }
                                }
                                    }
                                }
                            }
                        }
                    }
                    Destination.CATEGORIES -> {
                        Text(stringResource(R.string.category_settings), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.category_settings_hint))
                        SettingRow(
                            stringResource(R.string.select_countries),
                            Modifier.focusRequester(initialFocus)
                        ) { ui.choosingCountries = true }
                        SettingRow(
                            stringResource(R.string.auto_country_prompt),
                            Modifier, selected = automaticCountries
                        ) {
                            automaticCountries = !automaticCountries
                            preferences?.edit { putBoolean("autoCountries", automaticCountries) }
                        }
                        SettingRow(
                            stringResource(R.string.show_hidden_categories),
                            Modifier,
                            selected = ui.showHiddenCategories
                        ) { ui.showHiddenCategories = !ui.showHiddenCategories }
                    }
                    // "Abmelden" never becomes the selected page: it only opens the account selection.
                    Destination.PROFILES -> Unit
                    Destination.SOURCES -> {
                        Text(stringResource(R.string.destination_sources), style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        SettingRow(
                            label = stringResource(R.string.add_portal),
                            modifier = Modifier.focusRequester(initialFocus),
                            onClick = {
                                if (!restoring && !connecting) {
                                    address = ""
                                    mac = ""
                                    editing = true
                                }
                            }
                        )
                        SettingRow(
                            stringResource(R.string.refresh_all_sources),
                            Modifier.fillMaxWidth(),
                            enabled = savedSources.isNotEmpty() && !connecting && !restoring,
                            onClick = { refreshSources(savedSources) }
                        )
                        SettingRow(
                            stringResource(R.string.refresh_sources),
                            Modifier.fillMaxWidth(),
                            enabled = savedSources.isNotEmpty() && !connecting && !restoring,
                            onClick = ::openSourceRefreshDialog
                        )
                        SettingRow(
                            stringResource(R.string.merge_sources), Modifier,
                            selected = mergeSources
                        ) {
                            mergeSources = !mergeSources
                            preferences?.edit { putBoolean("mergeSources", mergeSources) }
                            closeGroup()
                            closeSeries()
                            details = null
                            searchedVariants = emptyMap()
                            category = null
                        }
                        Text(stringResource(
                            if (mergeSources) R.string.sources_combined else R.string.sources_separate
                        ))
                        val activePortalId = storedSource?.identity
                        if (!mergeSources && activePortalId != null) {
                            SettingRow(stringResource(R.string.merge_portal_channels), Modifier, selected = mergePortalChannels) {
                                mergedIndividualPortals = if (mergePortalChannels) mergedIndividualPortals - activePortalId
                                    else mergedIndividualPortals + activePortalId
                                preferences?.edit { putStringSet("merged_individual_portals", mergedIndividualPortals) }
                                category = null
                            }
                        }
                        for ((index, source) in savedSources.withIndex()) {
                            val host = remember(source.address) {
                                runCatching { java.net.URI(source.address).host }.getOrNull().orEmpty()
                            }
                            val isActive = source.identity == storedSource?.identity
                            // Portals that run out soon (amber) or did (red) are marked, with a note.
                            val expiry = expiryState(source.subscriptionExpiry, java.time.LocalDate.now(), expiryLead)
                            val expiryColor = when (expiry) {
                                ExpiryState.SOON -> Color(0xFFFFB300)
                                ExpiryState.EXPIRED -> Color(0xFFE53935)
                                null -> null
                            }
                            // Status and the counts of Live-TV, films and series side by side, above the frame:
                            // inside it they took the room of the portal address.
                            val counts = ui.sourceContentCounts[source.identity] ?: PortalContentCounts(0, 0, 0)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(stringResource(when (source.reachable) {
                                    true -> R.string.portal_status_reachable
                                    false -> R.string.portal_status_unreachable
                                    null -> R.string.portal_status_unknown
                                }))
                                Text(
                                    stringResource(R.string.portal_content_counts, counts.tv, counts.movies, counts.series),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                            // The portal's frame: name and its on/off switch belong together.
                            Surface(
                                Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                                color = expiryColor?.copy(alpha = 0.12f)?.compositeOver(MaterialTheme.colorScheme.surfaceContainer)
                                    ?: MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(if (expiryColor != null) 2.dp else 1.dp, expiryColor ?: MaterialTheme.colorScheme.outlineVariant)
                            ) {
                            Column {
                            if (expiry != null && expiryColor != null) {
                                val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), source.subscriptionExpiry)
                                Text(
                                    when {
                                        expiry == ExpiryState.EXPIRED -> stringResource(R.string.portal_expired_badge)
                                        days == 0L -> stringResource(R.string.portal_expires_today_badge)
                                        else -> pluralStringResource(R.plurals.portal_expires_soon_badge, days.toInt(), days.toInt())
                                    },
                                    Modifier.padding(start = 14.dp, top = 8.dp),
                                    color = expiryColor, style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                SettingRow(
                                listOf(
                                    stringResource(
                                        if (!mergeSources && isActive) R.string.active_portal_label else R.string.saved_portal_label,
                                        index + 1, host
                                    ),
                                    source.subscriptionExpiry?.let { date ->
                                        stringResource(R.string.portal_expiry, date.format(
                                            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                                        ))
                                    } ?: stringResource(R.string.portal_expiry_unknown)
                                ).joinToString(" · "),
                                Modifier.weight(1f),
                                onClick = {
                                    if (mergeSources && !connecting) {
                                        address = source.address
                                        mac = source.mac
                                        editing = true
                                    } else switchSource(source)
                                }
                                )
                                // The switch on the portal turns it on or off (and refreshes it when turned on).
                                SettingToggleButton(
                                    source.enabled,
                                    stringResource(if (source.enabled) R.string.disable_portal else R.string.enable_portal)
                                ) { setPortalEnabled(source, !source.enabled) }
                            }
                            }
                            }
                            // The provider's own categories (before grouping): which countries are in the portal.
                            MenuButton(stringResource(R.string.portal_categories), Modifier) { ui.portalCategoriesFor = source }
                            // "Bevorzugen" has its own frame next to the portal, so it is not taken for the switch.
                            val preferred = preferredPortal == source.identity
                            MenuButton(
                                stringResource(if (preferred) R.string.portal_preferred else R.string.portal_prefer),
                                Modifier, selected = preferred
                            ) { setPreferredPortal(if (preferred) null else source.identity) }
                            }
                        }
                        Text(
                            stringResource(
                                R.string.portal_content_counts_merged,
                                ui.combinedContentCounts.tv, ui.combinedContentCounts.movies, ui.combinedContentCounts.series
                            ),
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        storedSource?.takeUnless { mergeSources }?.let { saved ->
                            SettingRow(stringResource(R.string.edit_portal), Modifier) {
                                if (!connecting) {
                                    address = saved.address
                                    mac = saved.mac
                                    editing = true
                                }
                            }
                        }
                        // "Eingaben nur per Handy": portals are entered on the phone, not here.
                        if (editing && remoteInputOnly(context)) LaunchedEffect(Unit) { openRemoteAccess() }
                        else if (editing) PortalEditor(
                            address = address,
                            mac = mac,
                            existingSources = savedSources,
                            onAddressChange = { address = it },
                            onMacChange = { mac = it },
                            onSelectExisting = { address = it.address; mac = it.mac },
                            onDismiss = { editing = false },
                            onSubmit = ::importPortal,
                            onRemote = ::openRemoteAccess
                        )
                        if (connecting) {
                            ui.batchProgress?.let { (number, total) ->
                                Text(stringResource(R.string.refresh_all_progress, number, total))
                            }
                            if (ui.importStatuses.isEmpty()) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            } else {
                                val completed = ui.importStatuses.values.count {
                                    it != ImportStatus.Loading && it !is ImportStatus.Progress
                                }
                                LinearProgressIndicator(
                                    progress = { (completed + 1f) / (PortalSection.entries.size + 1) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(stringResource(R.string.import_steps, completed + 1, PortalSection.entries.size + 1))
                            }
                            if (ui.importStatuses.isEmpty()) Text(stringResource(R.string.connecting))
                        }
                        ui.sourceError?.let { message ->
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                        ui.storageError?.let { message ->
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                        PortalImportStatus(ui.importStatuses)
                        ui.batchSummary?.let { (updated, failed) ->
                            Text(stringResource(R.string.refresh_all_summary, updated, failed))
                        }
                        if (liveVariants.isNotEmpty()) {
                            SettingRow(
                                label = stringResource(R.string.open_live_tv),
                                modifier = Modifier,
                                onClick = {
                                    selected = Destination.LIVE
                                    lastFocused = Destination.LIVE
                                }
                            )
                        }
                        if (showSettingsBackButton) SettingRow(
                            label = stringResource(R.string.back_to_settings),
                            modifier = Modifier,
                            onClick = { selected = Destination.SETTINGS }
                        )
                    }
                    Destination.LIVE -> {
                        val groups = withCategoryOrder(PortalSection.LIVE, liveCategories.keys
                            .filter { visibleCategory(PortalSection.LIVE, it, it) }
                            .sortedBy { categoryNames.values.indexOf(it) }) { it }
                        val guideChannels = remember(liveCategories, groups, category) {
                            if (category in groups) liveCategories[category].orEmpty()
                            else groups.flatMap { liveCategories[it].orEmpty() }
                        }
                        val livePlayback = playing.takeIf { vodSection == null }
                        val focusTarget = if (ui.restoreLiveFocus) {
                            livePlayback ?: preferences?.getString("last_live_channel", null)?.let { key ->
                                guideChannels.firstOrNull { (programmeKey(it) ?: liveGroups[it]?.identity) == key }
                            }
                        } else null
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(if (compactGuide) 3.dp else 6.dp)
                            ) {
                                val livePreview = playing.takeIf { vodSection == null }
                                val infoChannel = programmeChannel ?: livePreview
                                Row(
                                    Modifier.fillMaxWidth().focusGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(if (compactGuide) 8.dp else 16.dp)
                                ) {
                                    if (guideAppearance.showPreview) Box(Modifier.width(guidePreviewWidth)) {
                                        PlayerFrame(
                                            player, active = livePreview != null, onStop = ::stopPlayback,
                                            onSurface = { ui.playbackVideoSurface = it },
                                            onOpenFullscreen = (livePreview ?: infoChannel)?.let { channel ->
                                                { playChannel(channel) }
                                            }
                                        )
                                    }
                                    Column(
                                        Modifier.weight(1f).heightIn(max = guidePreviewWidth * (9f / 16f)),
                                        verticalArrangement = Arrangement.spacedBy(if (compactGuide) 1.dp else 5.dp)
                                    ) {
                                        infoChannel?.let { channel ->
                                            val current = guideSelection(channel)
                                            val time = listOfNotNull(current?.startSeconds, current?.endSeconds).joinToString(" – ") {
                                                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                                                    .format(Instant.ofEpochSecond(it))
                                            }
                                            Text(
                                                if (time.isEmpty()) channel.name else "${channel.name} · $time",
                                                style = if (compactGuide) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                current?.title ?: stringResource(R.string.no_guide),
                                                style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            val from = current?.startSeconds
                                            val to = current?.endSeconds
                                            if (from != null && to != null && to > from) {
                                                val nowSeconds = Instant.now().epochSecond
                                                if (nowSeconds in from until to) Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    LinearProgressIndicator(
                                                        progress = { (nowSeconds - from).toFloat() / (to - from) },
                                                        modifier = Modifier.width(72.dp)
                                                    )
                                                    Text(
                                                        "noch ${(to - nowSeconds + 59) / 60} Min",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            current?.description?.let {
                                                Text(
                                                    it, style = MaterialTheme.typography.bodySmall,
                                                    maxLines = if (compactGuide) 1 else 5, overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            guideState.SaveableStateProvider("programme-guide") {
                                ProgrammeTimeline(
                                channels = guideChannels,
                                pinnedIds = pinnedProgrammeKeys,
                                highlighted = programmeChannel,
                                playing = playing.takeIf { vodSection == null },
                                focusTarget = focusTarget,
                                programmes = ::effectiveProgrammes,
                                programmeKey = ::programmeKey,
                                scroll = guideScroll,
                                compact = compactGuide,
                                appearance = guideAppearance,
                                refreshKey = epgRefreshRequest + if (guideCacheLoaded) 1 else 0,
                                horizonHours = epgHorizonHours,
                                window = guideWindow,
                                active = guidePanel == GuidePanel.GRID,
                                channelColumnFocus = guideChannelFocus,
                                restorePlayingFocus = ui.restoreLiveFocus,
                                onPlayingFocusRestored = { ui.restoreLiveFocus = false },
                                onChannelLeft = { guidePanel = GuidePanel.CATEGORIES },
                                onVisibleChannels = { visible ->
                                    visibleEpgChannels = visible
                                    if (!useXmlTv) loadProviderSchedules(visible)
                                },
                                onHighlight = {
                                    programmeChannel = it
                                    ui.guideProgramme = null
                                },
                                onProgrammeFocus = { channel, programme ->
                                    programmeChannel = channel
                                    ui.guideProgramme = channel to programme
                                },
                                onPlay = { channel -> playChannel(channel, openFullscreenOnStart = false) },
                                onTogglePin = ::toggleProgrammeChannel
                                )
                            }
                            }
                            if (guidePanel == GuidePanel.DETAILS) {
                                val channel = programmeChannel ?: playing.takeIf { vodSection == null }
                                val programme = channel?.let(::guideSelection)
                                Column(
                                    Modifier.align(Alignment.TopEnd).fillMaxHeight()
                                        .width((guidePreviewWidth * 1.65f).coerceIn(250.dp, 420.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                        .verticalScroll(rememberScrollState()).padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (showSettingsBackButton) {
                                        MenuButton(stringResource(R.string.navigation_back), Modifier.focusRequester(guidePanelFocus)) {
                                            navigateGuide(GuideInput.BACK)
                                        }
                                    } else Box(Modifier.size(1.dp).focusRequester(guidePanelFocus).focusable())
                                    channel?.let { Text(it.name, style = MaterialTheme.typography.labelMedium) }
                                    Text(
                                        programme?.title ?: stringResource(R.string.no_guide),
                                        style = if (compactGuide) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        programme?.description ?: stringResource(R.string.no_title_description),
                                        style = if (compactGuide) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                                    )
                                }
                            } else if (guidePanel != GuidePanel.GRID) {
                                Column(
                                    Modifier.width(240.dp).fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                                        .verticalScroll(rememberScrollState()).padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (guidePanel == GuidePanel.MENU) menu()
                                    else {
                                        MenuButton(
                                            stringResource(R.string.guide_all_channels),
                                            Modifier.fillMaxWidth().focusRequester(guidePanelFocus), selected = category == null
                                        ) {
                                            category = null
                                            ui.restoreLiveFocus = false
                                            guideScroll.requestScrollToItem(0)
                                            guidePanel = GuidePanel.GRID
                                        }
                                        groups.forEach { group ->
                                            MenuButton(group, Modifier.fillMaxWidth(), selected = category == group,
                                                onLongPress = { ui.categoryMenu = PortalSection.LIVE to group }) {
                                                category = group
                                                ui.restoreLiveFocus = false
                                                guideScroll.requestScrollToItem(0)
                                                guidePanel = GuidePanel.GRID
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Destination.SEARCH -> PortalSearch(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        focusRequester = initialFocus,
                        sources = sourcesForBrowsing,
                        channels = liveCategories.values.flatten(),
                        liveSources = { channel -> liveVariants[channel].orEmpty().map { it.source } },
                        liveProgramme = { channel -> effectiveProgrammes(channel)?.firstOrNull()?.title },
                        onHighlightChannel = { programmeChannel = it },
                        metadataClient = metadataClient.takeIf { combine },
                        portalFor = ::sessionFor,
                        countries = countries,
                        combine = combine,
                        historyKey = userDataKey("search_history"),
                        onChannel = { channel ->
                            programmeChannel = channel
                            category = liveCategories.entries.firstOrNull { channel in it.value }?.key
                            selected = Destination.LIVE
                            lastFocused = Destination.LIVE
                            liveGroups[channel]?.let { group ->
                                reopenPlayback(liveSelection(channel, group, combine))
                            }
                        },
                        onTitle = { section, items ->
                            val title = items.first().value
                            searchedVariants = mapOf(title to items)
                            selected = if (section == PortalSection.MOVIES) Destination.MOVIES else Destination.SERIES
                            lastFocused = requireNotNull(selected)
                            showTitle(section, title)
                        }
                    )
                    Destination.MOVIES, Destination.SERIES -> {
                        val section = if (destination == Destination.MOVIES) {
                            PortalSection.MOVIES
                        } else {
                            PortalSection.SERIES
                        }
                        LaunchedEffect(pendingVodCategory, destination, visibleVodGroups, restoring) {
                            if (restoring || pendingVodCategory != destination || visibleVodGroups.isEmpty()) return@LaunchedEffect
                            // The start page begins with the first category; later visits reopen the last one.
                            val previous = preferences?.getString(recentCategoryKey, null).takeUnless { startupFirstCategory }
                            pendingVodCategory = null
                            startupFirstCategory = false
                            val group = visibleVodGroups.firstOrNull { it.id == previous } ?: visibleVodGroups.first()
                            if (myAreaSection != section &&
                                (selectedGroup?.first != section || selectedGroup?.second?.id != group.id)
                            ) openVodCategory(group)
                        }
                        if (vodSection == section) {
                            PlaybackFeedback(starting, playbackError)
                        }
                        val series = openedSeries.takeIf { section == PortalSection.SERIES }
                        if (details?.first == section) {
                            val title = requireNotNull(details).second
                            val refs = titleEntries(section, title).mapNotNull { item ->
                                item.value.id?.takeIf(String::isNotBlank)?.let { SeriesPortalRef(item.source.identity, it) }
                            }.toSet()
                            val trackedEntry = if (section == PortalSection.SERIES) {
                                trackedSeries(title) ?: seriesActivity.firstOrNull { entry -> entry.refs.any { it in refs } }
                            } else null
                            val trackedResume = trackedEntry?.let(::resumeFor)
                            val seriesResume = if (section == PortalSection.SERIES && trackedEntry?.isComplete != true) {
                                val savedResume = trackedResume
                                val savedEpisode = savedResume?.let { resumeEpisode(seasons.orEmpty(), it) }
                                if (savedResume == null || savedEpisode == null) null
                                else if (!isPlaybackCompleted(savedResume.progressKey, savedResume.episodeKey in trackedEntry.watchedEpisodes)) {
                                    // An interrupted local/Trakt playback resumes at its exact episode,
                                    // including a start at position zero.
                                    savedResume
                                } else {
                                    // Trakt's watched history identifies the last completed episode.
                                    // Continue with the next episode that is not already completed.
                                    nextEpisodeAfter(seasons.orEmpty(), savedResume) { season, episode ->
                                        val key = titleSelection(section, episode, season).progressKey
                                        isPlaybackCompleted(key, seriesEpisodeKey(season, episode) in trackedEntry.watchedEpisodes)
                                    }?.let { (season, episode) ->
                                        EpisodeResume(
                                            seriesEpisodeKey(season, episode), season.number, episode.episode,
                                            episode.name, titleSelection(section, episode, season).progressKey
                                        )
                                    }
                                }
                            } else null
                            val firstEpisode = if (section == PortalSection.SERIES && seriesResume == null) {
                                firstPlayableEpisode(seasons.orEmpty())
                            } else null
                            val movieResumeKey = if (section == PortalSection.MOVIES) {
                                val currentKey = titleSelection(section, title).progressKey
                                sequenceOf(currentKey).plus(movieHistory.asSequence()
                                    .filter { entry -> entry.refs.any { it in refs } }.map { it.progressKey })
                                    .firstOrNull { (progressPreferences?.getLong(it, 0L) ?: 0L) > 0L }
                            } else null
                            val resumeKey = seriesResume?.progressKey ?: movieResumeKey
                            val movieProgress = if (section == PortalSection.MOVIES) {
                                val key = movieResumeKey ?: titleSelection(section, title).progressKey
                                val position = progressPreferences?.getLong(key, 0L) ?: 0L
                                val duration = progressPreferences?.getLong("duration:$key", 0L) ?: 0L
                                // Films watched according to Trakt count as watched unless marked otherwise here.
                                val watchedOnTrakt = traktWatchedMovieKeys?.let { keys ->
                                    movieIdentityKeys(
                                        displayVodTitle(title), vodReleaseYear(title), title.imdbId,
                                        title.tmdbId ?: detailMetadata?.id?.toString()
                                    ).any { it in keys }
                                } == true
                                EpisodeCardProgress(isPlaybackCompleted(key, fallback = watchedOnTrakt), position, duration)
                            } else null
                            val resumePosition = resumeKey?.let { progressPreferences?.getLong(it, 0L) ?: 0L }
                            val resumePercent = resumeKey?.let {
                                progressPreferences?.getFloat("trakt_percent:$it", -1f)?.takeIf { value -> value > 0f }
                            }
                            val resumeLabel = resumePercent?.let {
                                stringResource(R.string.trakt_resume_progress, it.toInt())
                            } ?: seriesResume?.let { episodeResumeLabel(it, resumePosition ?: 0L) }
                                ?: resumePosition?.takeIf { it > 0 }?.let(::playbackTime)
                            val baseTitle = (portalDetail ?: title).let { detail -> detailMetadata?.applyTo(detail) ?: detail }
                            val displayTitle = traktDetailMetadata?.let { metadata ->
                                baseTitle.copy(
                                    name = metadata.title,
                                    year = metadata.year ?: baseTitle.year,
                                    description = germanDescription(metadata.overview) ?: germanDescription(baseTitle.description)
                                )
                            } ?: tvmazeDetailMetadata?.let { metadata ->
                                baseTitle.copy(
                                    name = metadata.title,
                                    year = metadata.year ?: baseTitle.year,
                                    description = germanDescription(metadata.overview) ?: germanDescription(baseTitle.description)
                                )
                            } ?: baseTitle
                            val metadataProvider = when {
                                traktDetailMetadata != null -> "Trakt"
                                detailMetadata != null -> "TMDB"
                                tvmazeDetailMetadata != null -> "TVmaze"
                                else -> null
                            }
                            val titleOverview: @Composable () -> Unit = {
                            TitleDetails(
                                displayTitle, section,
                                metadataRating = traktDetailMetadata?.rating ?: detailMetadata?.rating ?: tvmazeDetailMetadata?.rating,
                                ageRating = detailAge,
                                metadataProvider = metadataProvider,
                                favorite = titleKey(section, title) in favorites,
                                saved = isTitleSaved(section, title),
                                newEpisodes = if (section == PortalSection.SERIES) {
                                    trackedSeries(title)?.unseen?.size ?: 0
                                } else 0,
                                upcomingEpisode = trackedSeries(title)?.nextAirDate?.let { rawDate ->
                                    val date = runCatching {
                                        java.time.LocalDate.parse(rawDate).format(
                                            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                        )
                                    }.getOrNull() ?: return@let null
                                    context.getString(
                                        R.string.next_episode_airdate,
                                        trackedSeries(title)?.nextEpisodeName ?: "",
                                        date
                                    )
                                },
                                resumeLabel = resumeLabel,
                                onResume = if (resumeKey != null && (section == PortalSection.MOVIES || seasons != null)) {
                                    { selectTitle(section, title, seriesResume, movieResumeKey) }
                                } else null,
                                firstEpisodeAction = firstEpisode?.let { (season, episode) ->
                                    {
                                        openedSeason = season
                                        selectTitle(section, episode)
                                    }
                                },
                                firstEpisodeLabel = firstEpisode?.let { (season, episode) ->
                                    val number = episode.episode
                                    if (season.number == null || number.isNullOrBlank()) stringResource(R.string.play_first_episode)
                                    else stringResource(R.string.play_numbered_episode, season.number, number)
                                },
                                focusPrimary = ui.focusTitlePrimary,
                                onPrimaryFocused = { ui.focusTitlePrimary = false },
                                compact = compactMedia,
                                onBack = ::navigateBack,
                                onPlay = { selectTitle(section, title) },
                                watchProgress = movieProgress,
                                onWatchStatus = if (section == PortalSection.MOVIES || seasons.orEmpty().any { it.episodes.isNotEmpty() }) {
                                    {
                                        val items = if (section == PortalSection.SERIES) {
                                            seasons.orEmpty().flatMap { season -> season.episodes.map { season to it } }
                                        } else emptyList()
                                        ui.watchStatusAction = { watched -> setWatched(section, title, items, watched) }
                                    }
                                } else null,
                                onFavorite = { toggleSavedTitle(section, title, true) },
                                onWatch = { toggleSavedTitle(section, title, false) },
                                onCast = ui.castPreview?.takeIf { it.first == title && it.second.isNotEmpty() }?.let {
                                    { ui.castOf = Triple(section, title, detailMetadata?.id) }
                                },
                                portalLabel = titlePortalChoices[titleKey(section, title)]
                                    ?.let { id -> savedSources.firstOrNull { it.identity == id } }
                                    ?.let { context.getString(R.string.title_portal_chosen, portalName(it)) },
                                onPortal = { ui.portalPickerFor = section to title }.takeIf { titlePortals(section, title).size > 1 }
                            )
                            }
                            if (section == PortalSection.SERIES) {
                                SeriesSeasons(
                                    modifier = Modifier.weight(1f),
                                    scroll = seriesDetailScroll,
                                    series = displayTitle, seasons = seasons, selected = openedSeason,
                                    failed = ui.episodeError, unseen = trackedSeries(title)?.unseen.orEmpty(),
                                    metadata = episodeMetadata,
                                    onSeason = { openedSeason = it.takeUnless { season -> season == openedSeason } },
                                    onSeasonWatchStatus = { season ->
                                        ui.watchStatusAction = { watched ->
                                            setWatched(section, title, season.episodes.map { season to it }, watched)
                                        }
                                    },
                                    onEpisodeWatchStatus = { season, episode ->
                                        ui.watchStatusAction = { watched -> setWatched(section, title, listOf(season to episode), watched) }
                                    },
                                    episodeProgress = { season, episode ->
                                        val key = titleSelection(section, episode, season).progressKey
                                        val savedKey = trackedResume?.takeIf { it.episodeKey == seriesEpisodeKey(season, episode) }
                                            ?.progressKey ?: key
                                        val prefs = progressPreferences
                                        val episodeKey = seriesEpisodeKey(season, episode)
                                        val completed = isPlaybackCompleted(savedKey, episodeKey in trackedEntry?.watchedEpisodes.orEmpty())
                                        val localPosition = prefs?.getLong(savedKey, 0L) ?: 0L
                                        val localDuration = prefs?.getLong("duration:$savedKey", 0L) ?: 0L
                                        val traktPercent = prefs?.getFloat("trakt_percent:$savedKey", 0f) ?: 0f
                                        val position = localPosition.takeIf { it > 0L } ?:
                                            (traktPercent.coerceIn(0f, 100f) * 10L).toLong()
                                        val duration = localDuration.takeIf { it > 0L } ?:
                                            if (traktPercent > 0f) 1000L else 0L
                                        EpisodeCardProgress(completed, position, duration)
                                    },
                                    onEpisode = { season, episode ->
                                        openedSeason = season
                                        selectTitle(section, episode)
                                    },
                                    onRetry = {
                                        closeSeries()
                                        selectTitle(section, title, loadOnly = true)
                                    },
                                    onBack = ::navigateBack,
                                    header = {
                                        titleOverview()
                                        if (ui.resumeEpisodeMissing) Text(
                                            stringResource(R.string.resume_episode_missing),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                )
                            } else titleOverview()
                        } else if (myAreaSection == section) {
                            // Like the "My Netflix" page: one row each for continue watching,
                            // the watchlist, (for series) announced episodes and recently watched.
                            val movieSection = section == PortalSection.MOVIES
                            fun reference(name: String, year: Int?, imdb: String?, tmdb: String?) =
                                PortalTitle(name, year = year, imdbId = imdb, tmdbId = tmdb)
                            val continueLabel = stringResource(R.string.continue_watching)
                            val watchlistLabel = stringResource(R.string.watchlist)
                            val upcomingLabel = stringResource(R.string.upcoming_series)
                            val recentLabel = stringResource(R.string.recently_watched)
                            val shelves: List<Pair<String, List<MyAreaCard>>> = if (movieSection) {
                                fun unfinished(key: String): Boolean {
                                    val position = progressPreferences?.getLong(key, 0L) ?: 0L
                                    return progressPreferences?.getBoolean("completed:$key", false) != true && position > 0L &&
                                        (progressPreferences?.getFloat("trakt_percent:$key", 0f) ?: 0f) < 90f
                                }
                                fun movieReference(entry: WatchedMovie) = reference(entry.name, entry.year, entry.imdb, entry.tmdb)
                                val watched = movieHistory.filter {
                                    hasPortalReference(PortalSection.MOVIES, movieReference(it), it.refs)
                                }.sortedByDescending { it.watchedAt }
                                val continuing = watched.filter { unfinished(it.progressKey) }.take(30).map { entry ->
                                    val position = progressPreferences?.getLong(entry.progressKey, 0L) ?: 0L
                                    MyAreaCard(
                                        "c:${entry.progressKey}", movieReference(entry), entry.refs,
                                        resources.getString(R.string.saved_playback_position, playbackTime(position)), 0
                                    ) { openWatchedMovie(entry, resume = true) }
                                }
                                val bookmarks = movieBookmarks.filter { entry ->
                                    entry.progressKey in watchlist &&
                                        hasPortalReference(PortalSection.MOVIES, movieReference(entry), entry.refs)
                                }
                                val bookmarkKeys = movieBookmarks.map { it.progressKey }.toSet()
                                // Older watchlist entries are only known as catalogue titles.
                                val legacy = (catalogs[section]?.titles.orEmpty() + groupCatalog?.titles.orEmpty())
                                    .distinctBy { titleKey(section, it) }
                                    .filter { titleKey(section, it) in watchlist && titleKey(section, it) !in bookmarkKeys }
                                listOf(
                                    continueLabel to continuing,
                                    watchlistLabel to bookmarks.map { entry ->
                                        MyAreaCard("w:${entry.progressKey}", movieReference(entry), entry.refs, null, 0) {
                                            openWatchedMovie(entry, resume = false)
                                        }
                                    } + legacy.map { title ->
                                        MyAreaCard("l:${titleKey(section, title)}", title, emptyList(), null, 0) {
                                            showTitle(section, title)
                                        }
                                    },
                                    recentLabel to watched.take(20).map { entry ->
                                        MyAreaCard("r:${entry.progressKey}", movieReference(entry), entry.refs, null, 0) {
                                            openWatchedMovie(entry, resume = false)
                                        }
                                    }
                                )
                            } else {
                                fun seriesReference(entry: TrackedSeries) = reference(entry.name, entry.year, entry.imdb, entry.tmdb)
                                fun card(prefix: String, entry: TrackedSeries, caption: String?, newEpisodes: Int = entry.unseen.size) = MyAreaCard(
                                    "$prefix:${entry.key}", seriesReference(entry), entry.refs, caption, newEpisodes
                                ) { openTrackedSeries(entry) }
                                // "Weiter schauen" marks only episodes that are in the portal and came up this week.
                                val now = System.currentTimeMillis()
                                val playable = seriesActivity.filter {
                                    hasPortalReference(PortalSection.SERIES, seriesReference(it), it.refs)
                                }
                                val dateFormat = java.time.format.DateTimeFormatter.ofPattern(
                                    "EE, dd.MM.", resources.configuration.locales[0]
                                )
                                listOf(
                                    continueLabel to playable.filter { it.continuable() }.continueOrder(now)
                                        .map { entry ->
                                            card("c", entry, newEpisodes = entry.newInPortal(now).size, caption = resumeFor(entry)?.let { episode ->
                                                episodeResumeLabel(episode, progressPreferences?.getLong(episode.progressKey, 0L) ?: 0L)
                                            })
                                        },
                                    watchlistLabel to playable.filter { it.saved }.sortedBy { it.name }.map { card("w", it, null) },
                                    upcomingLabel to upcomingSeries(playable, java.time.LocalDate.now()).map { (entry, date) ->
                                        card("u", entry, (entry.nextSeason?.let { "S$it · " } ?: "") + date.format(dateFormat), newEpisodes = 0)
                                    },
                                    recentLabel to playable.filter { it.viewed && !it.hiddenFromHistory }
                                        .sortedByDescending { it.watchedAt }.take(20).map { card("r", it, null) }
                                )
                            }
                            Text(stringResource(R.string.my_area), style = MaterialTheme.typography.titleMedium)
                            val filled = shelves.filter { it.second.isNotEmpty() }
                            if (filled.isEmpty()) Text(stringResource(R.string.my_area_empty))
                            val cardWidth = 118.dp
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                items(filled, key = { it.first }) { (label, cards) ->
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text(label, style = MaterialTheme.typography.titleSmall)
                                        LazyRow(
                                            Modifier.fillMaxWidth().height(cardWidth * 1.5f + 72.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(cards, key = { it.key }) { card ->
                                                PosterShelfCard(
                                                    historyArtwork(section, card.title, card.refs), cardWidth, card.unseen,
                                                    caption = card.caption
                                                ) { card.open() }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val group = selectedGroup?.takeIf { it.first == section }?.second
                            val baseCatalog = catalogs[section]
                            val uncategorizedSelected = !combine && group?.id == ALL_CATALOG_ITEMS
                            if (group != null) {
                                Text(group.name, style = MaterialTheme.typography.titleSmall)

                                guideState.SaveableStateProvider("vod-${section.name}-${group.id}") {
                                    CatalogOverview(
                                    modifier = Modifier.weight(1f),
                                        categoryTitle = group.name,
                                    catalog = if (uncategorizedSelected) baseCatalog else groupCatalog,
                                    loading = if (uncategorizedSelected) section in ui.catalogLoading else ui.groupLoading,
                                    failed = if (uncategorizedSelected) section in ui.catalogErrors else ui.groupError,
                                    canLoadMore = !connecting,
                                    emptyMessage = if (ui.groupUsesTmdb) R.string.tmdb_no_portal_matches
                                    else if (section == PortalSection.MOVIES) {
                                        R.string.no_movies
                                    } else R.string.no_series,
                                    onLoadMore = {
                                        if (uncategorizedSelected) loadMore(section)
                                        else loadGroup(section, group, next = true)
                                    },
                                    isFavorite = { titleKey(section, it) in favorites },
                                    isSaved = { isTitleSaved(section, it) },
                                    preferPopularity = group.id == VodCategory.POPULAR.id,
                                    returnTitle = ui.catalogueReturnTitle,
                                    focusFirstTitle = ui.focusFirstTitle,
                                    onTitleFocusRestored = {
                                        ui.catalogueReturnTitle = null
                                        ui.focusFirstTitle = false
                                    },
                                    newEpisodes = { title ->
                                        if (section == PortalSection.SERIES) trackedSeries(title)?.unseen?.size ?: 0 else 0
                                    },
                                    onSelect = {
                                        ui.catalogueReturnTitle = it
                                        searchedVariants = emptyMap()
                                        showTitle(section, it)
                                    }
                                )
                                }
                            } else {
                                if (visibleVodGroups.isEmpty()) CatalogCategories(
                                groups = visibleVodGroups,
                                loading = section in ui.catalogLoading ||
                                    ui.importStatuses[section] == ImportStatus.Loading,
                                failed = section in ui.catalogErrors ||
                                    ui.importStatuses[section] == ImportStatus.Failed,
                                isHidden = { categoryKey(section, it.id) in hiddenCategories },
                                onSelect = ::openVodCategory,
                                onManage = { ui.categoryMenu = section to it }
                                )
                            }
                        }
                    }
                }
            }
            if (inGuide) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    topNavigation()
                    header()
                    if (lastBack != 0L && !mainMenuOpen) Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Column(
                        Modifier.weight(1f).fillMaxWidth().guideNavigationInput(interceptRemote = { input ->
                            !input.vertical && (guidePanel != GuidePanel.GRID ||
                                input != GuideInput.REMOTE_RIGHT && controlBindings[input]?.let {
                                    it != ControlAction.DEFAULT && it != ControlAction.CHANNELS
                                } == true)
                        }) {
                            if (!runMappedControl(it)) navigateGuide(it)
                        }
                    ) { content() }
                }
            } else if (vodSectionForScreen != null) {
                val section = vodSectionForScreen
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topNavigation()
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Continue watching, watchlist, upcoming episodes and history in one place.
                        MenuButton(
                            stringResource(R.string.my_area), Modifier,
                            leadingIcon = R.drawable.ic_menu_my_area,
                            selected = myAreaSection == section,
                            // New episodes of followed series: a number in a circle, like at "Einstellungen".
                            badge = if (section == PortalSection.SERIES) {
                                seriesActivity.filter { it.saved || it.viewed }.sumOf { it.unseen.size }.takeIf { it > 0 }
                            } else null
                        ) {
                            closeSeries()
                            details = null
                            myAreaSection = section
                        }
                            visibleVodGroups.forEach { group ->
                                MenuButton(
                                    group.name,
                                    Modifier,
                                    providerMark = providerCategoryMark(group.name).takeIf {
                                        providerCategoryStyle != ProviderCategoryStyle.LABELS && providerCategoryIcon(group.name) == null
                                    },
                                    providerIcon = providerCategoryIcon(group.name)?.takeIf {
                                        providerCategoryStyle != ProviderCategoryStyle.LABELS
                                    } ?: genreCategoryIcon(group),
                                    // "Symbol only" applies to provider categories; genres keep their name.
                                    providerMarkOnly = providerCategoryStyle == ProviderCategoryStyle.SYMBOLS &&
                                        serviceCategory(group.name) != null,
                                    selected = selectedGroup?.first == section && selectedGroup?.second?.id == group.id &&
                                        myAreaSection != section,
                                    hiddenIndicator = categoryKey(section, group.id) in hiddenCategories,
                                    onLongPress = { ui.categoryMenu = section to group.id }
                                ) { openVodCategory(group) }
                            }
                        MenuButton(
                            stringResource(R.string.manage_categories),
                            Modifier
                        ) {
                            ui.categoryMenu = section to null
                        }
                    }
                    if (restoring || connecting || ui.liveGroupsLoading || ui.traktBusy || ui.startupPortalRefresh != null || ui.startupPortalCheck != null ||
                        cataloguePlaybackView == VodLiveView.CORNER && playing != null
                    ) header()
                    content()
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topNavigation()
                    if (destination != Destination.SEARCH) header()
                    if (lastBack != 0L && !mainMenuOpen) Text(
                        stringResource(R.string.press_back_again_to_exit),
                        Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (destination == Destination.SETTINGS || destination == Destination.SOURCES ||
                        destination == Destination.CATEGORIES
                    ) Row(
                        Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Settings: sections on the left, the rows of the chosen section on the right.
                        Column(
                            Modifier.weight(0.27f).fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                                .verticalScroll(rememberScrollState()).padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            SettingsSection.entries.forEach { item ->
                                SettingsNavItem(
                                    stringResource(item.label), item.icon,
                                    selected = settingsSection == item,
                                    badge = 1.takeIf { item == SettingsSection.UPDATES && appUpdates.available },
                                    modifier = Modifier.focusRequester(settingsFocusRequesters.getValue(item))
                                        .onFocusChanged {
                                            // Opening a page removes the row that had focus, and Android
                                            // briefly hands focus to the first section ("Allgemein").
                                            // That must not close the page that has just opened.
                                            val justOpened = settingsPageOpen() &&
                                                SystemClock.elapsedRealtime() - ui.settingsPageOpenedAt <
                                                SETTINGS_PAGE_FOCUS_GRACE_MS
                                            // The update window opening or closing also hands focus over
                                            // here: the section stays, focus goes back to its row.
                                            val dialogMoved = SystemClock.elapsedRealtime() - updateDialogChangedAt < 1_500
                                            if (it.isFocused && settingsSection != item && dialogMoved) {
                                                scope.launch {
                                                    withFrameNanos { }
                                                    runCatching { settingsContentFocus.requestFocus() }
                                                }
                                            } else if (it.isFocused && settingsSection != item && !justOpened) {
                                                closeSettingsPages()
                                                // Sources and categories are pages of the settings too.
                                                if (selected != Destination.SETTINGS) selected = Destination.SETTINGS
                                                lastSettingsSection = item
                                                settingsSection = item
                                            }
                                        }
                                ) {
                                    // A tap on another section closes an open page (D-pad focus does this already).
                                    if (settingsSection != item) closeSettingsPages()
                                    if (selected != Destination.SETTINGS) selected = Destination.SETTINGS
                                    lastSettingsSection = item
                                    settingsSection = item
                                }
                            }
                        }
                        Column(
                            Modifier.weight(0.73f)
                                .onFocusChanged { ui.settingsContentFocused = it.hasFocus }
                                .focusRequester(settingsContentFocus)
                                .focusProperties {
                                    // Into the rows: the row used last in this section, the first
                                    // time the top row (not the row nearest to the section bar item,
                                    // which made "Sperren" land in the middle).
                                    // (A requester whose row is gone fails quietly: then the usual search runs.)
                                    onEnter = {
                                        val remembered = settingsFocusMemory.target()
                                        val redirected = remembered != null && runCatching { remembered.requestFocus() }.getOrDefault(false)
                                        if (!redirected && requestedFocusDirection == FocusDirection.Right) {
                                            settingsContentFocusRequest++
                                            cancelFocusChange()
                                        }
                                    }
                                    // Left out of the rows works like Back: one step at a time, and
                                    // to the section that is open (not the one nearest on screen).
                                    this.onExit = {
                                        if (requestedFocusDirection == FocusDirection.Left) {
                                            if (closeSettingsPages()) {
                                                settingsContentFocusRequest++
                                                cancelFocusChange()
                                            } else settingsFocusRequesters[settingsSection]?.let { section ->
                                                runCatching { section.requestFocus() }
                                            }
                                        }
                                    }
                                }
                                .focusGroup()
                                .verticalScroll(contentScroll),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            settingsFocusMemory.section = if (destination == Destination.SETTINGS) settingsSection else destination
                            settingsFocusMemory.pageOpen = settingsPageOpen()
                            androidx.compose.runtime.CompositionLocalProvider(LocalSettingsFocusMemory provides settingsFocusMemory) {
                                content()
                            }
                        }
                        LaunchedEffect(settingsContentFocusRequest) {
                            if (settingsContentFocusRequest == 0) return@LaunchedEffect
                            withFrameNanos { }
                            runCatching { settingsContentFocus.requestFocus() }
                        }
                    } else Column(
                        Modifier.weight(1f).then(
                            if (destination == Destination.SEARCH) Modifier else Modifier.verticalScroll(contentScroll)
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) { content() }
                }
            }
            LaunchedEffect(destination, settingsSection) {
                if (!inGuide && !ui.restoreLiveFocus) {
                    if (destination == Destination.SETTINGS) {
                        contentScroll.scrollTo(0)
                        withFrameNanos { }
                        if (settingsSection == null) {
                            settingsFocusRequesters[SettingsSection.entries.first()]?.requestFocus() ?: initialFocus.requestFocus()
                        } else settingsFocusRequesters[settingsSection]?.requestFocus() ?: initialFocus.requestFocus()
                    } else initialFocus.requestFocus()
                }
            }
            LaunchedEffect(inGuide, guidePanel) {
                if (inGuide) {
                    if (guidePanel != GuidePanel.GRID) guidePanelFocus.requestFocus()
                    else initialFocus.requestFocus()
                }
            }
        }
        }
    }
}

@Composable
private fun PortalEditor(
    address: String,
    mac: String,
    existingSources: List<SavedPortal>,
    onAddressChange: (String) -> Unit,
    onMacChange: (String) -> Unit,
    onSelectExisting: (SavedPortal) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onRemote: (() -> Unit)? = null
) {
    var addressMode by remember { mutableStateOf(portalAddressMode(address)) }
    var selectedSavedSource by remember { mutableStateOf<SavedPortal?>(null) }
    val initialMacHex = remember {
        runCatching { normalizePortalMac(mac).filter(Char::isLetterOrDigit) }
            .getOrDefault("001A79000000")
    }
    var prefix by remember { mutableStateOf(initialMacHex.take(6)) }
    var customPrefixDigits by remember { mutableStateOf(initialMacHex.take(6).toList()) }
    var customPrefixInput by remember { mutableStateOf(initialMacHex.take(6)) }
    var customPrefix by remember { mutableStateOf(initialMacHex.take(6) !in portalMacPrefixes) }
    var deviceDigits by remember { mutableStateOf(initialMacHex.drop(6).take(6).padEnd(6, '0').toList()) }
    var prefixMenu by remember { mutableStateOf(false) }
    var savedPortalMenu by remember { mutableStateOf(false) }
    var addressModeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (mac.isBlank()) onMacChange(initialMacHex.chunked(2).joinToString(":"))
    }

    fun publishMac() {
        val prefixValue = if (customPrefix) customPrefixDigits.joinToString("") else prefix
        val hex = (prefixValue + deviceDigits.joinToString(""))
            .uppercase(java.util.Locale.ROOT).take(12).padEnd(12, '0')
        onMacChange(hex.chunked(2).joinToString(":"))
    }

    fun setMac(value: String) {
        val hex = runCatching { normalizePortalMac(value).filter(Char::isLetterOrDigit) }
            .getOrDefault("001A79000000")
        prefix = hex.take(6)
        customPrefixDigits = hex.take(6).toList()
        customPrefixInput = hex.take(6)
        customPrefix = prefix !in portalMacPrefixes
        deviceDigits = hex.drop(6).take(6).toList()
        onMacChange(hex.chunked(2).joinToString(":"))
    }

    fun chooseAddressMode(next: PortalAddressMode) {
        selectedSavedSource = null
        if (next == PortalAddressMode.SAVED) {
            addressMode = next
            onAddressChange("")
            onMacChange("")
            return
        }
        val host = portalAddressHost(address, addressMode)
        addressMode = next
        onAddressChange(if (next == PortalAddressMode.CUSTOM || host.isBlank()) "" else portalComposeAddress(next, host))
        if (customPrefix) customPrefixInput = customPrefixDigits.joinToString("")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.destination_sources),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(stringResource(R.string.stalker_hint))
                    Box {
                        MenuButton(
                            stringResource(addressMode.label), Modifier.fillMaxWidth()
                        ) { addressModeMenu = true }
                        DropdownMenu(addressModeMenu, onDismissRequest = { addressModeMenu = false }) {
                            PortalAddressMode.entries.filter {
                                it != PortalAddressMode.SAVED || existingSources.isNotEmpty()
                            }.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.label)) },
                                    onClick = { chooseAddressMode(mode); addressModeMenu = false }
                                )
                            }
                        }
                    }
                    if (addressMode == PortalAddressMode.SAVED) {
                        Box {
                            MenuButton(
                                selectedSavedSource?.let { selected ->
                                    val index = existingSources.indexOfFirst { it.identity == selected.identity }
                                    val host = runCatching { java.net.URI(selected.address).host }.getOrNull()
                                        .orEmpty().ifBlank { "Portal ${index + 1}" }
                                    stringResource(R.string.saved_portal_label, index + 1, host)
                                } ?: stringResource(R.string.portal_pick_saved),
                                Modifier.fillMaxWidth(),
                                onClick = { savedPortalMenu = true }
                            )
                            DropdownMenu(savedPortalMenu, onDismissRequest = { savedPortalMenu = false }) {
                                existingSources.forEachIndexed { index, source ->
                                    val host = runCatching { java.net.URI(source.address).host }.getOrNull()
                                        .orEmpty().ifBlank { "Portal ${index + 1}" }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.saved_portal_label, index + 1, host)) },
                                        onClick = {
                                            selectedSavedSource = source
                                            addressMode = PortalAddressMode.SAVED
                                            onSelectExisting(source)
                                            setMac(source.mac)
                                            savedPortalMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    } else if (addressMode == PortalAddressMode.CUSTOM) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = onAddressChange,
                            label = { Text(stringResource(R.string.portal_address)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(addressMode.prefix)
                            OutlinedTextField(
                                value = portalAddressHost(address, addressMode),
                                onValueChange = { host ->
                                    onAddressChange(if (host.isBlank()) "" else portalComposeAddress(addressMode, host))
                                },
                                label = { Text(stringResource(R.string.portal_address_host)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text("/c")
                        }
                    }
                    if (addressMode != PortalAddressMode.SAVED) {
                        Text(stringResource(R.string.portal_mac_prefix))
                        Box {
                            MenuButton(
                                if (customPrefix) stringResource(R.string.portal_mac_prefix_custom)
                                else "${prefix.chunked(2).joinToString(":")}: ",
                                Modifier.fillMaxWidth()
                            ) { prefixMenu = true }
                            DropdownMenu(prefixMenu, onDismissRequest = { prefixMenu = false }) {
                                portalMacPrefixes.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text("${value.chunked(2).joinToString(":")}: ") },
                                        onClick = {
                                            prefix = value
                                            customPrefix = false
                                            publishMac()
                                            prefixMenu = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.portal_mac_prefix_custom)) },
                                    onClick = {
                                        customPrefix = true
                                        customPrefixInput = customPrefixDigits.joinToString("")
                                        publishMac()
                                        prefixMenu = false
                                    }
                                )
                            }
                        }
                        if (customPrefix) {
                            Text(stringResource(R.string.portal_mac_custom_prefix_digits))
                            OutlinedTextField(
                                value = customPrefixInput,
                                onValueChange = { raw ->
                                    val clean = raw.uppercase(java.util.Locale.ROOT)
                                        .filter { it in "0123456789ABCDEF" }.take(6)
                                    customPrefixInput = clean
                                    customPrefixDigits = clean.padEnd(6, '0').toList()
                                    if (clean.length == 6) publishMac()
                                },
                                label = { Text(stringResource(R.string.portal_mac_prefix_custom)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii,
                                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters
                                )
                            )
                            MacDigitRow(customPrefixDigits) { index, value ->
                                customPrefixDigits = customPrefixDigits.toMutableList().also { it[index] = value }
                                customPrefixInput = customPrefixDigits.joinToString("")
                                publishMac()
                            }
                        }
                        Text(stringResource(R.string.portal_mac_device_digits))
                        MacDigitRow(deviceDigits) { index, value ->
                            deviceDigits = deviceDigits.toMutableList().also { it[index] = value }
                            publishMac()
                        }
                    }
                    Text(stringResource(R.string.portal_mac_preview, mac), style = MaterialTheme.typography.bodySmall)
                    val addressReady = when (addressMode) {
                        PortalAddressMode.SAVED -> selectedSavedSource != null
                        PortalAddressMode.CUSTOM -> address.isNotBlank()
                        else -> portalAddressHost(address, addressMode).isNotBlank()
                    }
                    val macReady = addressMode == PortalAddressMode.SAVED ||
                        (!customPrefix || customPrefixInput.length == 6) &&
                        runCatching { normalizePortalMac(mac) }.isSuccess
                    MenuButton(
                        stringResource(R.string.connect_portal),
                        Modifier.fillMaxWidth(),
                        enabled = addressReady && macReady,
                        onClick = onSubmit
                    )
                    onRemote?.let { MenuButton(stringResource(R.string.remote_setup_portal), Modifier.fillMaxWidth(), onClick = it) }
                    MenuButton(stringResource(android.R.string.cancel), Modifier, onClick = onDismiss)
                }
            }
        }
    }
}

private enum class PortalAddressMode(val prefix: String, val label: Int) {
    HTTP("http://", R.string.portal_address_http),
    HTTPS("https://", R.string.portal_address_https),
    CUSTOM("", R.string.portal_address_manual),
    SAVED("", R.string.portal_address_saved)
}

private val portalMacPrefixes = listOf(
    "001A78", "001A79", "001B78", "001B79", "001C78", "001C79",
    "002A01", "002A78", "002A79"
)

private fun portalAddressMode(address: String): PortalAddressMode = when {
    address.isBlank() -> PortalAddressMode.HTTP
    address.startsWith("https://", ignoreCase = true) && address.endsWith("/c", ignoreCase = true) -> PortalAddressMode.HTTPS
    address.startsWith("http://", ignoreCase = true) && address.endsWith("/c", ignoreCase = true) -> PortalAddressMode.HTTP
    else -> PortalAddressMode.CUSTOM
}

private fun portalAddressHost(address: String, mode: PortalAddressMode): String {
    var value = address.trim()
    if (value.startsWith("https://", ignoreCase = true)) value = value.drop(8)
    else if (value.startsWith("http://", ignoreCase = true)) value = value.drop(7)
    value = value.trimEnd('/')
    if (value.endsWith("/c", ignoreCase = true)) value = value.dropLast(2).trimEnd('/')
    return value
}

private fun portalComposeAddress(mode: PortalAddressMode, host: String): String {
    var normalized = host.trim()
    if (normalized.startsWith("https://", ignoreCase = true)) normalized = normalized.drop(8)
    else if (normalized.startsWith("http://", ignoreCase = true)) normalized = normalized.drop(7)
    normalized = normalized.removePrefix("/").trimEnd('/')
        .let { if (it.endsWith("/c", ignoreCase = true)) it.dropLast(2).trimEnd('/') else it }
    return if (normalized.isBlank()) "" else "${mode.prefix}$normalized/c"
}

@Composable
private fun MacDigitRow(values: List<Char>, onChange: (Int, Char) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { index, value ->
            if (index > 0 && index % 2 == 0) Text(":", style = MaterialTheme.typography.titleMedium)
            var expanded by remember(index) { mutableStateOf(false) }
            Box {
                MenuButton(value.toString(), Modifier.width(36.dp)) { expanded = true }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    "0123456789ABCDEF".forEach { digit ->
                        DropdownMenuItem(
                            text = { Text(digit.toString()) },
                            onClick = { onChange(index, digit); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalImportStatus(statuses: Map<PortalSection, ImportStatus>) {
    PortalSection.entries.forEach { section ->
        val label = stringResource(section.label)
        when (val status = statuses[section]) {
            ImportStatus.Loading -> Text(stringResource(R.string.portal_section_loading, label))
            is ImportStatus.Progress -> Text(
                if (status.total != null) {
                    stringResource(R.string.live_progress_total, status.loaded, status.total)
                } else {
                    stringResource(R.string.live_progress_loaded, status.loaded)
                }
            )
            ImportStatus.Failed -> Text(
                text = stringResource(R.string.portal_section_failed, label),
                color = MaterialTheme.colorScheme.error
            )
            is ImportStatus.Count -> Text(
                stringResource(R.string.portal_section_count, label, status.value)
            )
            is ImportStatus.Partial -> Text(
                stringResource(R.string.portal_section_partial, label, status.value),
                color = MaterialTheme.colorScheme.error
            )
            is ImportStatus.Loaded -> Text(
                stringResource(R.string.portal_section_loaded, label, status.value)
            )
            is ImportStatus.Error -> Text(
                stringResource(status.message),
                color = MaterialTheme.colorScheme.error
            )
            null -> Unit
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun LiveOverview(
    player: ExoPlayer?,
    groups: Map<String, List<StalkerChannel>>,
    loading: Boolean,
    visibleGroups: List<String>,
    hiddenGroups: Set<String>,
    onManageGroups: (String?) -> Unit,
    category: String?,
    onCategory: (String?) -> Unit,
    categoryScroll: LazyListState,
    channelScroll: LazyListState,
    lastCategory: String?,
    restoreFocus: Boolean,
    onFocusRestored: () -> Unit,
    playing: StalkerChannel?,
    starting: StalkerChannel?,
    programmeChannel: StalkerChannel?,
    programmes: List<PortalProgramme>?,
    variants: List<String>,
    epgPinned: Boolean,
    onToggleEpg: (StalkerChannel) -> Unit,
    onHighlight: (StalkerChannel) -> Unit,
    playbackError: Int?,
    onStop: () -> Unit,
    showBackButton: Boolean = true,
    onSelect: (StalkerChannel) -> Unit
) {
    val visibleChannels = groups[category]
    val targetIndex = if (visibleChannels == null) {
        visibleGroups.indexOfFirst { it == lastCategory }
    } else {
        visibleChannels.indexOfFirst { it === playing }.takeIf { it >= 0 }
            ?: visibleChannels.indexOfFirst { it === programmeChannel }
    }
    val activeScroll = if (visibleChannels == null) categoryScroll else channelScroll
    val restoreRequester = remember { FocusRequester() }
    LaunchedEffect(restoreFocus, targetIndex, activeScroll) {
        if (!restoreFocus) return@LaunchedEffect
        if (targetIndex < 0) {
            onFocusRestored()
            return@LaunchedEffect
        }
        val layout = snapshotFlow { activeScroll.layoutInfo }.first { it.totalItemsCount > targetIndex }
        if (layout.visibleItemsInfo.none { it.index == targetIndex }) {
            activeScroll.scrollToItem(targetIndex)
        }
    }
    val rowModifier: @Composable (Int) -> Modifier = { index ->
        if (restoreFocus && index == targetIndex) {
            LaunchedEffect(activeScroll, targetIndex) {
                withFrameNanos { }
                if (restoreRequester.requestFocus()) onFocusRestored()
            }
            Modifier.fillMaxWidth().focusRequester(restoreRequester)
        } else {
            Modifier.fillMaxWidth()
        }
    }
    val channels: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.padding(end = 9.dp).width(3.dp).height(22.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
            Column {
                Text(
                    category ?: stringResource(R.string.channel_categories),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (visibleChannels == null) "KATEGORIEN" else "SENDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (groups.isEmpty()) {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.startup_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.no_channels), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (visibleChannels == null) {
            MenuButton(stringResource(R.string.manage_categories), Modifier.fillMaxWidth()) {
                onManageGroups(null)
            }
            if (visibleGroups.isEmpty()) Text(stringResource(R.string.no_visible_categories))
            LazyColumn(Modifier.heightIn(max = 320.dp), state = categoryScroll) {
                itemsIndexed(visibleGroups, key = { _, group -> group }) { index, group ->
                    MenuButton(
                        categoryEntryLabel(group, categoryKey(PortalSection.LIVE, group) in hiddenGroups),
                        rowModifier(index),
                        count = groups.getValue(group).size,
                        hiddenIndicator = categoryKey(PortalSection.LIVE, group) in hiddenGroups,
                        onLongPress = { onManageGroups(group) }
                    ) { onCategory(group) }
                }
            }
        } else {
            if (showBackButton) MenuButton(stringResource(R.string.back_to_categories), Modifier.fillMaxWidth()) {
                onCategory(null)
            }
            LazyColumn(Modifier.heightIn(max = 320.dp), state = channelScroll) {
                itemsIndexed(visibleChannels) { index, channel ->
                    MenuButton(
                        channel.name, rowModifier(index), channel == playing, channel.logo,
                        onFocused = { onHighlight(channel) }
                    ) {
                        if (programmeChannel === channel || playing === channel) onSelect(channel)
                        else onHighlight(channel)
                    }
                }
            }
        }
    }
    val preview: @Composable (Modifier) -> Unit = { modifier ->
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.padding(end = 9.dp).size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                    )
                    Text(
                        stringResource(if (playing != null) R.string.live_now_caps else R.string.live_tv_caps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    playing?.let { channel ->
                        Text(
                            "  ·  ${channel.name}",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                PlaybackFeedback(starting, playbackError)
                PlayerFrame(player, active = playing != null, showEmpty = starting == null, onStop = onStop)
                if (programmeChannel != null) {
                    ProgrammeDetails(programmeChannel, programmes?.take(2))
                    if (variants.isNotEmpty()) {
                        Text(stringResource(R.string.live_stream_variants), style = MaterialTheme.typography.titleSmall)
                        variants.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    if (programmeChannel !== playing && programmeChannel !== starting) {
                        MenuButton(
                            stringResource(R.string.play_selected_channel), Modifier,
                            onClick = { onSelect(programmeChannel) }
                        )
                    }
                    if (programmeChannel.id != null) {
                        MenuButton(
                            stringResource(if (epgPinned) R.string.epg_unpin else R.string.epg_pin),
                            Modifier,
                            onClick = { onToggleEpg(programmeChannel) }
                        )
                    }
                }
            }
        }
    }
    BoxWithConstraints {
        if (maxWidth >= 380.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    Modifier.weight(1.05f).background(
                        MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium
                    ).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { channels() }
                preview(Modifier.weight(2f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                preview(Modifier.fillMaxWidth())
                channels()
            }
        }
    }
}

@Composable
private fun categoryEntryLabel(name: String, hidden: Boolean): String =
    if (hidden) stringResource(R.string.hidden_category_label, name) else name

@Composable
private fun CatalogCategories(
    groups: List<PortalGroup>,
    loading: Boolean,
    failed: Boolean,
    isHidden: (PortalGroup) -> Boolean,
    onSelect: (PortalGroup) -> Unit,
    onManage: (String?) -> Unit
) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    if (failed) Text(stringResource(R.string.catalog_load_failed), color = MaterialTheme.colorScheme.error)
    MenuButton(stringResource(R.string.manage_categories), Modifier) { onManage(null) }
    if (groups.isEmpty() && !loading && !failed) Text(stringResource(R.string.no_visible_categories))
    if (groups.isNotEmpty()) {
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(groups, key = { it.id }) { group ->
                MenuButton(
                    categoryEntryLabel(group.name, isHidden(group)),
                    Modifier.fillMaxWidth(),
                    hiddenIndicator = isHidden(group),
                    onLongPress = { onManage(group.id) }
                ) { onSelect(group) }
            }
        }
    }
}

@Composable
private fun PlaybackFeedback(starting: StalkerChannel?, error: Int?) {
    if (starting != null) {
        Text(stringResource(R.string.playback_starting, starting.name))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (error != null) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
    // Why: the HTTP code of the portal or the player's error type.
    if (error != null) PlaybackReason.text?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun PlayerFrame(
    player: ExoPlayer?,
    active: Boolean,
    showEmpty: Boolean = true,
    fullscreen: Boolean = false,
    isLivePlayback: Boolean = false,
    onStop: () -> Unit,
    onSurface: ((SurfaceView?) -> Unit)? = null,
    onOpenFullscreen: (() -> Unit)? = null,
    onGuideInput: ((GuideInput) -> Unit)? = null,
    interceptGuideKey: (GuideInput) -> Boolean = { true },
    onTouchAction: (() -> Boolean)? = null,
    onMediaKey: (() -> Boolean)? = null,
    controlsRequest: Int = 0,
    controlsDismissRequest: Int = 0,
    controlsAllowed: Boolean = true,
    actionPromptActive: Boolean = false,
    subtitleDefaultEnabled: Boolean = false,
    forcedSubtitlesEnabled: Boolean = true,
    subtitleSeriesKey: String? = null,
    subtitlePlaybackKey: String? = null,
    subtitlePreferences: android.content.SharedPreferences? = null,
    onEpisodeActionConfirm: (() -> Unit)? = null,
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    hasPreviousEpisode: Boolean = false,
    hasNextEpisode: Boolean = false,
    episodeActionVisible: Boolean = false,
    onControlsVisible: ((Boolean) -> Unit)? = null,
    /** Portals the current stream can play from; the settings menu offers them. */
    portalChoices: List<String> = emptyList(),
    portalIndex: Int = -1,
    onPortalChoice: ((Int) -> Unit)? = null
) {
    val accentArgb = MaterialTheme.colorScheme.primary.toArgb()
    var subtitleDialogOpen by remember(player) { mutableStateOf(false) }
    var playbackSettingsOpen by remember(player) { mutableStateOf(false) }
    var saveSubtitleChoiceForSeries by remember(player, subtitleSeriesKey) { mutableStateOf(false) }
    var oneTimeSubtitleChoice by remember(player, subtitlePlaybackKey) { mutableStateOf(false) }
    val currentOneTimeSubtitleChoice by rememberUpdatedState(oneTimeSubtitleChoice)
    val buffering by produceState(
        initialValue = false,
        player, active, fullscreen
    ) {
        if (player == null || !active || !fullscreen) {
            value = false
        } else {
            var playbackStarted = player.isPlaying || player.playbackState == Player.STATE_READY
            var bufferingStartedAt: Long? = null
            while (true) {
                val now = SystemClock.elapsedRealtime()
                when {
                    player.isPlaying || player.playbackState == Player.STATE_READY -> {
                        playbackStarted = true
                        bufferingStartedAt = null
                        value = false
                    }
                    playbackStarted && player.playWhenReady && player.playbackState == Player.STATE_BUFFERING -> {
                        if (bufferingStartedAt == null) bufferingStartedAt = now
                        value = now - bufferingStartedAt >= 1_500L
                    }
                    else -> {
                        bufferingStartedAt = null
                        value = false
                    }
                }
                delay(200)
            }
        }
    }
    DisposableEffect(player, subtitleSeriesKey, subtitlePlaybackKey, subtitleDefaultEnabled, forcedSubtitlesEnabled) {
        val activePlayer = player
        if (activePlayer == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                if (currentOneTimeSubtitleChoice) return
                val choices = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMap { group ->
                        (0 until group.length).filter(group::isTrackSupported).map { index ->
                            group to index
                        }
                    }
                val preferredLanguage = java.util.Locale.getDefault().language
                val forced = choices.firstOrNull { (group, index) ->
                    val format = group.getTrackFormat(index)
                    isForcedSubtitle(format) && format.language?.substringBefore('-') == preferredLanguage
                } ?: choices.firstOrNull { (group, index) ->
                    val format = group.getTrackFormat(index)
                    isForcedSubtitle(format) && (format.language.isNullOrBlank() || format.language == "und")
                }
                val languageChoice = choices.firstOrNull { (group, index) ->
                    group.getTrackFormat(index).language?.substringBefore('-') == preferredLanguage
                }
                val savedMode = subtitleSeriesKey?.let { key ->
                    subtitlePreferences?.getString("subtitle_series:$key", null)
                }
                val enabled = when (savedMode) {
                    "OFF" -> false
                    "FORCED" -> forced != null
                    "ON" -> choices.isNotEmpty()
                    else -> subtitleDefaultEnabled || (forcedSubtitlesEnabled && forced != null)
                }
                val chosen = when {
                    !enabled -> null
                    forced != null && (savedMode == "FORCED" || savedMode == null && forcedSubtitlesEnabled) -> forced
                    else -> languageChoice ?: choices.firstOrNull()
                }
                val current = activePlayer.trackSelectionParameters
                val updated = current.buildUpon()
                if (chosen == null) {
                    updated.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                } else {
                    updated.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setOverrideForType(TrackSelectionOverride(chosen.first.mediaTrackGroup, listOf(chosen.second)))
                }
                val next = updated.build()
                if (next != current) activePlayer.trackSelectionParameters = next
            }
        }
        activePlayer.addListener(listener)
        // Tracks known before this listener existed (e.g. a film that was already prepared)
        // never reported a change: choose the forced subtitles for them right away too.
        if (!activePlayer.currentTracks.isEmpty) listener.onTracksChanged(activePlayer.currentTracks)
        onDispose { activePlayer.removeListener(listener) }
    }
    val stop by rememberUpdatedState(onStop)
    val openFullscreen by rememberUpdatedState(onOpenFullscreen)
    val guideInput by rememberUpdatedState(onGuideInput)
    val interceptDirection by rememberUpdatedState(interceptGuideKey)
    val touchAction by rememberUpdatedState(onTouchAction)
    val mediaKey by rememberUpdatedState(onMediaKey)
    val allowControls by rememberUpdatedState(controlsAllowed)
    val livePlayback by rememberUpdatedState(isLivePlayback)
    val controlsVisibility by rememberUpdatedState(onControlsVisible)
    val previousEpisode by rememberUpdatedState(onPreviousEpisode)
    val nextEpisode by rememberUpdatedState(onNextEpisode)
    val canPreviousEpisode by rememberUpdatedState(hasPreviousEpisode)
    val canNextEpisode by rememberUpdatedState(hasNextEpisode)
    val captureActionKeys by rememberUpdatedState(actionPromptActive)
    val confirmEpisodeAction by rememberUpdatedState(onEpisodeActionConfirm)
    fun enableRemoteFocus(view: android.view.View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        // Like the settings rows: a lighter surface and an accent border when focused.
        val focused = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.argb(70, 255, 255, 255))
            setStroke((2.5f * view.resources.displayMetrics.density).toInt().coerceAtLeast(2), accentArgb)
        }
        // Round, softly dark buttons on the picture (the time bar keeps its own look).
        if (view is android.widget.ImageView) {
            view.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.argb(120, 0, 0, 0))
            }
            view.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }
        val idle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.TRANSPARENT)
        }
        view.foreground = android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), focused)
            addState(intArrayOf(), idle)
        }
        view.nextFocusUpId = view.id
        view.nextFocusDownId = view.id
    }
    /**
     * The row of play buttons: unusable ones are hidden (not greyed out), all look the same, and
     * left/right reach every visible one in on-screen order; down leads to the time bar.
     */
    fun arrangePlayerButtons(view: PlayerView) {
        // Live-TV can be seeked only with a running timeshift recording.
        val seekable = (!livePlayback || Timeshift.recorder != null) && view.player?.isCurrentMediaItemSeekable == true
        // Media3's playlist buttons are not used (episodes have their own buttons).
        view.setShowPreviousButton(false)
        view.setShowNextButton(false)
        view.setShowRewindButton(seekable)
        view.setShowFastForwardButton(seekable)
        // Outer buttons: the previous/next episode (arrows with a bar) when there is one, otherwise a
        // 10-minute jump (three arrows; the seek buttons have two)
        // (films, Live-TV with timeshift); hidden when neither works.
        fun outer(name: String, episode: Boolean, episodeIcon: Int, episodeLabel: Int, jumpIcon: Int, jumpLabel: Int) {
            val button = view.findViewWithTag<android.widget.ImageButton>(name) ?: return
            val mode = when {
                !livePlayback && episode -> 1
                seekable -> 2
                else -> 0
            }
            button.isVisible = mode != 0
            if (mode != 0 && button.getTag(R.string.player_jump_back) != mode) {
                button.setTag(R.string.player_jump_back, mode)
                button.setImageResource(if (mode == 1) episodeIcon else jumpIcon)
                button.contentDescription = view.context.getString(if (mode == 1) episodeLabel else jumpLabel)
                button.tooltipText = button.contentDescription
            }
        }
        outer("previous_episode_action", canPreviousEpisode, R.drawable.ic_player_skip_previous, R.string.previous_episode,
            R.drawable.ic_player_big_jump_back, R.string.player_jump_back)
        outer("next_episode_action", canNextEpisode, R.drawable.ic_player_skip_next, R.string.next_episode,
            R.drawable.ic_player_big_jump_forward, R.string.player_jump_forward)
        // Media3 draws its seek icon (circle arrow with the seconds) as the button's foreground:
        // replaced by the double arrows in the same round look as the other buttons.
        fun styleSeek(id: Int, icon: Int) {
            val button = view.findViewById<android.widget.TextView>(id) ?: return
            if (button.getTag(R.string.player_jump_forward) == true) return
            button.setTag(R.string.player_jump_forward, true)
            val density = button.resources.displayMetrics.density
            val drawable = androidx.core.content.ContextCompat.getDrawable(button.context, icon) ?: return
            if (button.id == android.view.View.NO_ID) button.id = android.view.View.generateViewId()
            enableRemoteFocus(button)
            // The focus ring first: a layer list insets every layer by the padding of the ones
            // before it, so with the (inset) icon first the ring came out smaller than the button.
            button.foreground = android.graphics.drawable.LayerDrawable(arrayOf(
                button.foreground, android.graphics.drawable.InsetDrawable(drawable, (13 * density).toInt())
            )).apply { paddingMode = android.graphics.drawable.LayerDrawable.PADDING_MODE_STACK }
            button.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.argb(120, 0, 0, 0))
            }
            button.setTextColor(android.graphics.Color.TRANSPARENT)
            button.minWidth = 0
            button.minHeight = 0
            // Media3 puts the button into a frame of its own size; both get the size of the other
            // buttons, so the focus ring is just as large.
            val size = (52 * density).toInt()
            (button.parent as? android.view.ViewGroup)?.takeIf { it.id != androidx.media3.ui.R.id.exo_center_controls }?.let { frame ->
                frame.layoutParams = frame.layoutParams?.apply { width = size; height = size }
            }
            button.layoutParams = button.layoutParams?.apply { width = size; height = size }
            button.setPadding(0, 0, 0, 0)
            button.requestLayout()
        }
        // Play/pause: the same size and inner spacing as the other round buttons.
        view.findViewById<android.widget.ImageView>(androidx.media3.ui.R.id.exo_play_pause)?.let { pause ->
            if (pause.getTag(R.string.player_jump_back) != true) {
                pause.setTag(R.string.player_jump_back, true)
                val density = pause.resources.displayMetrics.density
                val size = (52 * density).toInt()
                val padding = (13 * density).toInt()
                pause.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                pause.setPadding(padding, padding, padding, padding)
                pause.layoutParams = pause.layoutParams?.apply { width = size; height = size }
            }
        }
        styleSeek(androidx.media3.ui.R.id.exo_rew_with_amount, R.drawable.ic_player_jump_back)
        styleSeek(androidx.media3.ui.R.id.exo_ffwd_with_amount, R.drawable.ic_player_jump_forward)
        val row = view.findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_center_controls) ?: return
        val progress = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
        val pause = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
        // Media3 puts the seek buttons into a frame together with their seconds label: the
        // button inside is the one that takes the focus (and gets the round look).
        fun target(child: android.view.View): android.view.View? = when {
            child.isFocusable || child !is android.view.ViewGroup -> child
            else -> (0 until child.childCount).map(child::getChildAt).firstOrNull { it.isFocusable && it.isVisible }
        }
        val buttons = (0 until row.childCount).map(row::getChildAt).filter { child ->
            child.isVisible && child !is LinearLayout
        }.mapNotNull(::target).filter { it.isEnabled }
        buttons.forEach { button ->
            if (button.id == android.view.View.NO_ID) button.id = android.view.View.generateViewId()
            if (button.foreground == null) enableRemoteFocus(button)
        }
        buttons.forEachIndexed { index, button ->
            button.nextFocusLeftId = buttons.getOrNull(index - 1)?.id ?: button.id
            button.nextFocusRightId = buttons.getOrNull(index + 1)?.id ?: button.id
            button.nextFocusUpId = button.id
            button.nextFocusDownId = progress?.id ?: button.id
        }
        if (progress != null && pause != null) progress.nextFocusUpId = pause.id
    }
    fun configureBottomFocus(view: PlayerView) {
        val progress = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress) ?: return
        val settingsButton = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)
        // Media3 disables this button when it cannot currently find selectable tracks.
        // Keep it focusable on TV so the settings entry remains in the D-pad route; its
        // own click handler still decides which settings are actually available.
        if (settingsButton?.isVisible == true) settingsButton.isEnabled = true
        val bottomButtons = listOf(
            view.findViewById(androidx.media3.ui.R.id.exo_settings),
            view.findViewWithTag<android.view.View>("player_subtitles_action"),
            // Smoking break (smart home), only shown when it is set up.
            view.findViewWithTag<android.view.View>("player_smoke_action"),
            view.findViewWithTag<android.view.View>("player_live_action"),
            view.findViewById(androidx.media3.ui.R.id.exo_fullscreen),
            view.findViewById(androidx.media3.ui.R.id.exo_overflow_show)
        ).filterNotNull()
            .filter { it.isVisible && it.isEnabled }
        if (bottomButtons.isEmpty()) {
            progress.nextFocusDownId = progress.id
            return
        }
        progress.nextFocusDownId = bottomButtons.first().id
        bottomButtons.forEachIndexed { index, button ->
            button.nextFocusUpId = progress.id
            button.nextFocusLeftId = bottomButtons.getOrNull(index - 1)?.id ?: button.id
            button.nextFocusRightId = bottomButtons.getOrNull(index + 1)?.id ?: button.id
        }
    }
    var shownControlsRequest by remember { mutableIntStateOf(controlsRequest) }
    var previousDismissRequest by remember { mutableIntStateOf(controlsDismissRequest) }
    var previousEpisodeAction by remember { mutableStateOf(episodeActionVisible) }
    Box(
        modifier = (if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF171717), Color(0xFF080808))),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (player != null && active) {
            AndroidView(
                factory = { context ->
                    object : PlayerView(context) {
                        private var controlsVisible = false
                        private val hideControls = Runnable { hideController() }

                        init {
                            if (fullscreen) {
                                isFocusable = true
                                controllerShowTimeoutMs = 0
                                setControllerAnimationEnabled(false)
                                setControllerVisibilityListener(ControllerVisibilityListener { visibility ->
                                    controlsVisible = visibility == android.view.View.VISIBLE
                                    controlsVisibility?.invoke(controlsVisible)
                                    removeCallbacks(hideControls)
                                    if (controlsVisible) {
                                        postDelayed(hideControls, 6_000L)
                                        post {
                                            if (controlsVisible) focusInitialControllerTarget()
                                        }
                                    } else if (allowControls) {
                                        post { if (!controlsVisible && allowControls) requestFocus() }
                                    }
                                })
                            }
                        }

                        private fun extendControlsTimeout() {
                            removeCallbacks(hideControls)
                            postDelayed(hideControls, 6_000L)
                        }

                        private fun focusInitialControllerTarget() {
                            findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)?.requestFocus()
                        }

                        override fun onDetachedFromWindow() {
                            removeCallbacks(hideControls)
                            super.onDetachedFromWindow()
                        }

                        override fun performClick(): Boolean =
                            touchAction?.invoke() == true || super.performClick()

                        override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
                            if (fullscreen && controlsVisible) extendControlsTimeout()
                            return super.dispatchTouchEvent(event)
                        }

                        override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                            if (fullscreen && captureActionKeys) {
                                val direction = when (event.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                                    else -> null
                                }
                                if (direction != null) {
                                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) {
                                        guideInput?.invoke(direction)
                                    }
                                    return true
                                }
                                if (event.keyCode in setOf(
                                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                        android.view.KeyEvent.KEYCODE_ENTER,
                                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                        android.view.KeyEvent.KEYCODE_BUTTON_A
                                    )
                                ) {
                                    if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) {
                                        confirmEpisodeAction?.invoke()
                                    }
                                    return true
                                }
                            }
                            if (event.keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE &&
                                event.action == android.view.KeyEvent.ACTION_DOWN &&
                                (event.repeatCount == 0 && mediaKey?.invoke() == true)
                            ) return true
                            if (fullscreen && allowControls) {
                                if (!livePlayback && !controlsVisible && event.keyCode in setOf(
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT,
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                                    ) && player.isCurrentMediaItemSeekable
                                ) {
                                    val delta = if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) -10_000L else 10_000L
                                    if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                                        val duration = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                                        player.seekTo((player.currentPosition + delta).coerceIn(0L, duration))
                                        if (!controlsVisible) {
                                            showController()
                                            post {
                                                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
                                                    ?.requestFocus()
                                            }
                                        }
                                    }
                                    return true
                                }
                                if (controlsVisible) {
                                    extendControlsTimeout()
                                    if (event.action == android.view.KeyEvent.ACTION_DOWN &&
                                        event.repeatCount == 0 && event.keyCode in setOf(
                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                                            android.view.KeyEvent.KEYCODE_DPAD_UP
                                        )
                                    ) {
                                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.let { settings ->
                                            if (settings.isVisible) settings.isEnabled = true
                                        }
                                        configureBottomFocus(this)
                                        val focused = findFocus()
                                        val progress = findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)
                                        val bottomTargets = listOfNotNull(
                                            findViewWithTag<android.view.View>("player_subtitles_action"),
                                            findViewWithTag<android.view.View>("player_smoke_action"),
                                            findViewById(androidx.media3.ui.R.id.exo_settings),
                                            findViewById(androidx.media3.ui.R.id.exo_fullscreen),
                                            findViewById(androidx.media3.ui.R.id.exo_overflow_show)
                                        )
                                        val bottom = bottomTargets.firstOrNull {
                                            it.isVisible && it.isEnabled && it.isFocusable
                                        }
                                        // Live streams cannot seek: Media3 disables the time bar, so the
                                        // route between the play controls and the bottom row skips it.
                                        val progressUsable = progress != null && progress.isShown && progress.isEnabled
                                        val moved = when {
                                            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && focused === progress ->
                                                bottom?.requestFocus() == true
                                            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && !progressUsable &&
                                                focused !in bottomTargets -> bottom?.requestFocus() == true
                                            event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP && bottom != null &&
                                            focused in bottomTargets -> if (progressUsable) progress.requestFocus() else
                                                (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                                                    ?.takeIf { it.isShown } ?: findViewWithTag<android.view.View>("stop_playback_action"))
                                                    ?.requestFocus() == true
                                            else -> false
                                        }
                                        if (moved) return true
                                    }
                                } else {
                                    val direction = when (event.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> GuideInput.REMOTE_LEFT
                                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> GuideInput.REMOTE_RIGHT
                                        android.view.KeyEvent.KEYCODE_DPAD_UP -> GuideInput.REMOTE_UP
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> GuideInput.REMOTE_DOWN
                                        else -> null
                                    }
                                    val onDirection = guideInput
                                    if (direction != null && onDirection != null && interceptDirection(direction)) {
                                        if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) onDirection(direction)
                                        return true
                                    }
                                    if (direction?.vertical == true && onDirection != null) return true
                                    if (event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                                        event.keyCode == android.view.KeyEvent.KEYCODE_ENTER
                                    ) {
                                        if (event.action == android.view.KeyEvent.ACTION_UP && !event.isCanceled) showController()
                                        return true
                                    }
                                }
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
                        useController = true
                        controllerAutoShow = false
                        // Play row, like Netflix or Prime: previous episode (or 10 min back), seek back,
                        // play/pause, stop, seek forward, next episode (or 10 min forward). Buttons that
                        // do nothing for the running title are hidden in arrangePlayerButtons.
                        val playerView = this
                        findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_center_controls)?.let { row ->
                            val density = context.resources.displayMetrics.density
                            val size = (52 * density).toInt()
                            val padding = (13 * density).toInt()
                            fun actionButton(icon: Int, label: Int, name: String, action: () -> Unit) = ImageButton(context).apply {
                                id = android.view.View.generateViewId()
                                tag = name
                                setImageResource(icon)
                                contentDescription = context.getString(label)
                                tooltipText = contentDescription
                                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                setPadding(padding, padding, padding, padding)
                                setOnClickListener { action() }
                                enableRemoteFocus(this)
                            }
                            fun jump(byMs: Long) {
                                val active = playerView.player ?: return
                                val target = (active.currentPosition + byMs).coerceAtLeast(0L)
                                val duration = active.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                                active.seekTo(duration?.let { target.coerceAtMost((it - 1_000).coerceAtLeast(0L)) } ?: target)
                            }
                            val previous = actionButton(R.drawable.ic_player_skip_previous, R.string.previous_episode, "previous_episode_action") {
                                if (!livePlayback && canPreviousEpisode) previousEpisode?.invoke() else jump(-BIG_JUMP_MS)
                            }
                            val stopButton = actionButton(R.drawable.ic_player_stop, R.string.stop_playback, "stop_playback_action") { stop() }
                            val next = actionButton(R.drawable.ic_player_skip_next, R.string.next_episode, "next_episode_action") {
                                if (!livePlayback && canNextEpisode) nextEpisode?.invoke() else jump(BIG_JUMP_MS)
                            }
                            val pause = row.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                            row.addView(previous, 0, LinearLayout.LayoutParams(size, size))
                            val afterPause = row.indexOfChild(pause).takeIf { it >= 0 }?.plus(1) ?: row.childCount
                            row.addView(stopButton, afterPause, LinearLayout.LayoutParams(size, size))
                            row.addView(next, LinearLayout.LayoutParams(size, size))
                        }
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_subtitle)?.visibility =
                            android.view.View.GONE
                        (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_basic_controls) as? LinearLayout)?.let { bar ->
                            if (bar.findViewWithTag<android.view.View>("player_subtitles_action") == null) {
                                val cc = ImageButton(context).apply {
                                    id = android.view.View.generateViewId()
                                    tag = "player_subtitles_action"
                                    setImageResource(R.drawable.ic_subtitles)
                                    imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                                    contentDescription = context.getString(R.string.subtitle_tracks)
                                    tooltipText = contentDescription
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    defaultFocusHighlightEnabled = false
                                    val selectable = TypedValue()
                                    if (context.theme.resolveAttribute(
                                            android.R.attr.selectableItemBackgroundBorderless, selectable, true
                                        )
                                    ) setBackgroundResource(selectable.resourceId)
                                    setOnClickListener {
                                        saveSubtitleChoiceForSeries = false
                                        subtitleDialogOpen = true
                                    }
                                }
                                val gear = bar.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)
                                gear?.setOnClickListener { playbackSettingsOpen = true }
                                val insertAt = gear?.let { bar.indexOfChild(it) + 1 }?.takeIf { it > 0 } ?: bar.childCount
                                val size = (48 * context.resources.displayMetrics.density).toInt()
                                bar.addView(cc, insertAt, LinearLayout.LayoutParams(size, size))
                                enableRemoteFocus(cc)
                            }
                            // Smoking break (smart home): pauses and starts its own scene; the next play
                            // brings everything back. Only shown when that scene has devices set up.
                            if (bar.findViewWithTag<android.view.View>("player_smoke_action") == null) {
                                val smoke = ImageButton(context).apply {
                                    id = android.view.View.generateViewId()
                                    tag = "player_smoke_action"
                                    setImageResource(R.drawable.ic_smoking)
                                    imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                                    contentDescription = context.getString(R.string.smart_home_smoke)
                                    tooltipText = contentDescription
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    defaultFocusHighlightEnabled = false
                                    val selectable = TypedValue()
                                    if (context.theme.resolveAttribute(
                                            android.R.attr.selectableItemBackgroundBorderless, selectable, true
                                        )
                                    ) setBackgroundResource(selectable.resourceId)
                                    visibility = if (LightAutomation.smokeReady) android.view.View.VISIBLE else android.view.View.GONE
                                    setOnClickListener {
                                        LightAutomation.startSmoke()
                                        playerView.player?.pause()
                                    }
                                }
                                val size = (48 * context.resources.displayMetrics.density).toInt()
                                val after = bar.findViewWithTag<android.view.View>("player_subtitles_action")
                                val insertAt = after?.let { bar.indexOfChild(it) + 1 }?.takeIf { it > 0 } ?: bar.childCount
                                bar.addView(smoke, insertAt, LinearLayout.LayoutParams(size, size))
                                enableRemoteFocus(smoke)
                                val check = object : Runnable {
                                    override fun run() {
                                        val visibility = if (LightAutomation.smokeReady) android.view.View.VISIBLE else android.view.View.GONE
                                        if (smoke.visibility != visibility) {
                                            smoke.visibility = visibility
                                            configureBottomFocus(playerView)
                                        }
                                        smoke.postDelayed(this, 2_000)
                                    }
                                }
                                smoke.post(check)
                            }
                        }
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                            progress.isFocusable = true
                            progress.isFocusableInTouchMode = true
                            progress.defaultFocusHighlightEnabled = false
                            (progress as? DefaultTimeBar)?.let { timeBar ->
                                // The point on the bar is always in the accent colour: with timeshift it
                                // shows that something is recorded and can be moved with left/right.
                                timeBar.setScrubberColor(accentArgb)
                                timeBar.setPlayedColor(accentArgb)
                                progress.setOnFocusChangeListener { _, focused ->
                                    timeBar.setScrubberColor(if (focused) android.graphics.Color.WHITE else accentArgb)
                                    timeBar.setPlayedColor(accentArgb)
                                }
                            }
                        }
                        listOf(
                            androidx.media3.ui.R.id.exo_settings,
                            androidx.media3.ui.R.id.exo_fullscreen,
                            androidx.media3.ui.R.id.exo_overflow_show
                        ).forEach { id ->
                            findViewById<android.view.View>(id)?.let(::enableRemoteFocus)
                        }
                        findViewWithTag<android.view.View>("player_subtitles_action")?.let(::enableRemoteFocus)
                        // Buttons that cannot be used are hidden (not greyed out).
                        listOf(
                            androidx.media3.ui.R.id.exo_vr,
                            androidx.media3.ui.R.id.exo_shuffle,
                            androidx.media3.ui.R.id.exo_repeat_toggle,
                            androidx.media3.ui.R.id.exo_fullscreen
                        ).forEach { id -> findViewById<android.view.View>(id)?.visibility = android.view.View.GONE }
                        // A soft dark fade at the bottom instead of dimming the whole picture.
                        findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controls_background)?.background =
                            android.graphics.drawable.GradientDrawable(
                                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.argb(40, 0, 0, 0),
                                    android.graphics.Color.argb(210, 0, 0, 0))
                            )
                        // "● Live" at the far right: only with timeshift and when behind live.
                        (findViewById<android.view.View>(androidx.media3.ui.R.id.exo_basic_controls) as? LinearLayout)?.let { bar ->
                            if (bar.findViewWithTag<android.view.View>("player_live_action") == null) {
                                val live = android.widget.TextView(context).apply {
                                    id = android.view.View.generateViewId()
                                    tag = "player_live_action"
                                    text = android.text.SpannableString("●  " + context.getString(R.string.timeshift_live)).apply {
                                        setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.rgb(229, 9, 20)), 0, 1, 0)
                                    }
                                    setTextColor(android.graphics.Color.WHITE)
                                    textSize = 15f
                                    gravity = android.view.Gravity.CENTER
                                    val padding = (14 * context.resources.displayMetrics.density).toInt()
                                    setPadding(padding, 0, padding, 0)
                                    background = android.graphics.drawable.GradientDrawable().apply {
                                        cornerRadius = 100f
                                        setColor(android.graphics.Color.argb(120, 0, 0, 0))
                                    }
                                    visibility = android.view.View.GONE
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    setOnClickListener { Timeshift.goLive(playerView.player) }
                                }
                                val height = (44 * context.resources.displayMetrics.density).toInt()
                                bar.addView(live, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, height))
                                live.foreground = android.graphics.drawable.StateListDrawable().apply {
                                    addState(intArrayOf(android.R.attr.state_focused), android.graphics.drawable.GradientDrawable().apply {
                                        cornerRadius = 100f
                                        setStroke((2 * context.resources.displayMetrics.density).toInt(), accentArgb)
                                    })
                                }
                                // Shows itself once playback is more than a few seconds behind live.
                                val tick = object : Runnable {
                                    override fun run() {
                                        val behind = Timeshift.behindLive(playerView.player)
                                        val show = behind != null && behind > 6_000
                                        val visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                                        if (live.visibility != visibility) {
                                            live.visibility = visibility
                                            configureBottomFocus(playerView)
                                        }
                                        live.postDelayed(this, 1_000)
                                    }
                                }
                                live.post(tick)
                            }
                        }
                        arrangePlayerButtons(this)
                        // Subtitles only when the stream has some.
                        val view = this
                        fun showSubtitleButton(tracks: Tracks) {
                            view.findViewWithTag<android.view.View>("player_subtitles_action")?.visibility =
                                if (tracks.groups.any { it.type == C.TRACK_TYPE_TEXT }) android.view.View.VISIBLE
                                else android.view.View.GONE
                            configureBottomFocus(view)
                        }
                        player.let { activePlayer ->
                            showSubtitleButton(activePlayer.currentTracks)
                            val listener = object : Player.Listener {
                                override fun onTracksChanged(tracks: Tracks) = showSubtitleButton(tracks)
                                // Seekable or not is only known once the stream runs.
                                override fun onEvents(player: Player, events: Player.Events) {
                                    view.post { arrangePlayerButtons(view) }
                                }
                            }
                            activePlayer.addListener(listener)
                            setTag(R.string.subtitle_tracks, listener)
                        }
                        configureBottomFocus(this)
                        // The controls see a timeline over the timeshift recording (when it runs).
                        this.player = Timeshift.controls(player)
                        if (fullscreen) post { if (allowControls) requestFocus() }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = {
                    if (fullscreen) controlsVisibility?.invoke(false)
                    (it.getTag(R.string.subtitle_tracks) as? Player.Listener)?.let { listener -> it.player?.removeListener(listener) }
                    it.player = null
                    onSurface?.invoke(null)
                },
                update = { view ->
                    onSurface?.invoke(view.videoSurfaceView as? SurfaceView)
                    // Live TV keeps the last picture while another stream or channel starts (no black cut).
                    view.setKeepContentOnPlayerReset(livePlayback)
                    arrangePlayerButtons(view)
                    val pause = view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_play_pause)
                    val subtitlesButton = view.findViewWithTag<android.view.View>("player_subtitles_action")
                    view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_progress)?.let { progress ->
                        progress.nextFocusUpId = pause?.id ?: progress.id
                        (progress as? DefaultTimeBar)?.let { timeBar ->
                            timeBar.setPlayedColor(accentArgb)
                            timeBar.setScrubberColor(
                                if (progress.isFocused) accentArgb else android.graphics.Color.WHITE
                            )
                        }
                    }
                    listOf(
                        androidx.media3.ui.R.id.exo_settings,
                        androidx.media3.ui.R.id.exo_fullscreen,
                        androidx.media3.ui.R.id.exo_overflow_show
                    ).forEach { id -> view.findViewById<android.view.View>(id)?.let(::enableRemoteFocus) }
                    subtitlesButton?.let(::enableRemoteFocus)
                    configureBottomFocus(view)
                    view.useController = openFullscreen == null && controlsAllowed
                    if (previousEpisodeAction != episodeActionVisible) {
                        previousEpisodeAction = episodeActionVisible
                        if (!episodeActionVisible && controlsAllowed && !view.isControllerFullyVisible) {
                            view.post { if (allowControls) view.requestFocus() }
                        }
                    }
                    if (previousDismissRequest != controlsDismissRequest) {
                        previousDismissRequest = controlsDismissRequest
                        view.hideController()
                    }
                    if (shownControlsRequest != controlsRequest) {
                        shownControlsRequest = controlsRequest
                        if (controlsAllowed) view.showController()
                    }
                    if (openFullscreen != null) {
                        view.isFocusable = true
                        view.setOnClickListener { openFullscreen?.invoke() }
                    }
                }
            )
            if (playbackSettingsOpen) PlaybackSettingsDialog(
                player, livePlayback, portalChoices, portalIndex,
                onPortalChoice = { index -> playbackSettingsOpen = false; onPortalChoice?.invoke(index) },
                onDismiss = { playbackSettingsOpen = false }
            )
            if (subtitleDialogOpen) {
                val subtitleChoices = player.currentTracks.groups
                    .filter { it.type == C.TRACK_TYPE_TEXT }
                    .flatMap { group ->
                        (0 until group.length).filter(group::isTrackSupported).map { index ->
                            val format = group.getTrackFormat(index)
                            val language = format.language?.takeUnless { it == "und" }
                                ?.let { java.util.Locale.forLanguageTag(it).getDisplayName(java.util.Locale.GERMAN) }
                            Triple(
                                group,
                                index,
                                format.label?.takeIf(String::isNotBlank) ?: language
                                    ?: format.id?.takeIf(String::isNotBlank) ?: "Spur ${index + 1}"
                            )
                        }
                    }
                val savedSeriesMode = subtitleSeriesKey?.let { key ->
                    subtitlePreferences?.getString("subtitle_series:$key", null)
                }
                val subtitlesDisabled = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
                fun applySubtitleChoice(selection: TrackSelectionOverride?, enabled: Boolean, savedMode: String) {
                    val persistForSeries = saveSubtitleChoiceForSeries && subtitleSeriesKey != null
                    if (persistForSeries) {
                        subtitlePreferences?.edit { putString("subtitle_series:$subtitleSeriesKey", savedMode) }
                        oneTimeSubtitleChoice = false
                    } else {
                        oneTimeSubtitleChoice = true
                    }
                    val parameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    if (enabled && selection != null) parameters.setOverrideForType(selection)
                    player.trackSelectionParameters = parameters.build()
                    subtitleDialogOpen = false
                }
                fun useSeriesDefault() {
                    subtitleSeriesKey?.let { key -> subtitlePreferences?.edit { remove("subtitle_series:$key") } }
                    oneTimeSubtitleChoice = false
                    val forced = subtitleChoices.firstOrNull { (group, index, _) ->
                        isForcedSubtitle(group.getTrackFormat(index))
                    }
                    val language = java.util.Locale.getDefault().language
                    val preferred = subtitleChoices.firstOrNull { (group, index, _) ->
                        group.getTrackFormat(index).language?.substringBefore('-') == language
                    } ?: subtitleChoices.firstOrNull()
                    val choice = when {
                        forcedSubtitlesEnabled && forced != null -> forced
                        subtitleDefaultEnabled -> preferred
                        else -> null
                    }
                    val parameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, choice == null)
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    choice?.let { (group, index, _) ->
                        parameters.setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                    }
                    player.trackSelectionParameters = parameters.build()
                    subtitleDialogOpen = false
                }
                AlertDialog(
                    onDismissRequest = { subtitleDialogOpen = false },
                    title = { Text(stringResource(R.string.subtitle_tracks)) },
                    text = {
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            Row(Modifier.fillMaxWidth().focusGroup()) {
                                MenuButton(
                                    stringResource(R.string.subtitle_current_episode), Modifier.weight(1f),
                                    selected = !saveSubtitleChoiceForSeries
                                ) { saveSubtitleChoiceForSeries = false }
                                if (subtitleSeriesKey != null) {
                                    MenuButton(
                                        stringResource(R.string.subtitle_for_series), Modifier.weight(1f),
                                        selected = saveSubtitleChoiceForSeries
                                    ) { saveSubtitleChoiceForSeries = true }
                                }
                            }
                            MenuButton(
                                stringResource(R.string.subtitle_off), Modifier.fillMaxWidth(),
                                selected = subtitlesDisabled || savedSeriesMode == "OFF"
                            ) { applySubtitleChoice(null, false, "OFF") }
                            if (subtitleSeriesKey != null) {
                                MenuButton(
                                    stringResource(R.string.subtitles_forced_auto), Modifier.fillMaxWidth(),
                                    selected = savedSeriesMode == "FORCED"
                                ) {
                                    val forced = subtitleChoices.firstOrNull { (group, index, _) ->
                                        isForcedSubtitle(group.getTrackFormat(index))
                                    }
                                    applySubtitleChoice(
                                        forced?.let { (group, index, _) -> TrackSelectionOverride(group.mediaTrackGroup, listOf(index)) },
                                        forced != null, "FORCED"
                                    )
                                }
                                MenuButton(stringResource(R.string.subtitle_series_default), Modifier.fillMaxWidth()) {
                                    useSeriesDefault()
                                }
                            }
                            if (subtitleChoices.isEmpty()) {
                                Text(
                                    stringResource(R.string.subtitle_none_available),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            } else subtitleChoices.forEach { (group, index, label) ->
                                MenuButton(
                                    stringResource(
                                        if (saveSubtitleChoiceForSeries && subtitleSeriesKey != null) R.string.subtitle_choice_series
                                        else R.string.subtitle_choice_episode,
                                        label
                                    ),
                                    Modifier.fillMaxWidth(),
                                    selected = group.isTrackSelected(index) && !subtitlesDisabled
                                ) {
                                    applySubtitleChoice(
                                        TrackSelectionOverride(group.mediaTrackGroup, listOf(index)), true, "ON"
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        MaterialTextButton(onClick = { subtitleDialogOpen = false }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }
            if (fullscreen && buffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(42.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        } else if (showEmpty) {
            Text(stringResource(R.string.no_playback))
        }
    }
}

/** Player settings: speed (not for live TV), sound track and the portal the stream plays from. */
@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlaybackSettingsDialog(
    player: ExoPlayer,
    live: Boolean,
    portals: List<String>,
    portalIndex: Int,
    onPortalChoice: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val audio = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }.flatMap { group ->
        (0 until group.length).filter(group::isTrackSupported).map { index ->
            val format = group.getTrackFormat(index)
            val language = format.language?.takeUnless { it == "und" }
                ?.let { java.util.Locale.forLanguageTag(it).getDisplayName(java.util.Locale.GERMAN) }
            val name = format.label?.takeIf(String::isNotBlank) ?: language ?: "Spur ${index + 1}"
            val channels = when {
                format.channelCount > 2 -> " · ${format.channelCount - 1}.1"
                format.channelCount == 2 -> " · Stereo"
                else -> ""
            }
            Triple(group, index, name + channels)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_settings)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!live) {
                    Text(stringResource(R.string.player_speed), style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.horizontalScroll(rememberScrollState()).focusGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val current = player.playbackParameters.speed
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                            MenuButton(
                                if (speed == 1f) stringResource(R.string.player_speed_normal) else "${speed}x".replace('.', ','),
                                Modifier, selected = kotlin.math.abs(current - speed) < 0.01f
                            ) {
                                player.setPlaybackSpeed(speed)
                                onDismiss()
                            }
                        }
                    }
                }
                Text(stringResource(R.string.player_audio), style = MaterialTheme.typography.titleSmall)
                if (audio.isEmpty()) Text(stringResource(R.string.playback_no_audio))
                audio.forEach { (group, index, label) ->
                    MenuButton(label, Modifier.fillMaxWidth(), selected = group.isTrackSelected(index)) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(index)))
                            .build()
                        onDismiss()
                    }
                }
                if (portals.isNotEmpty()) {
                    Text(stringResource(R.string.player_portal), style = MaterialTheme.typography.titleSmall)
                    portals.forEachIndexed { index, label ->
                        MenuButton(label, Modifier.fillMaxWidth(), selected = index == portalIndex) {
                            if (index == portalIndex) onDismiss() else onPortalChoice(index)
                        }
                    }
                }
            }
        },
        confirmButton = { MaterialTextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
@Suppress("DEPRECATION")
private fun AboutSettings() {
    val context = LocalContext.current
    val app = remember(context) { context.packageManager.getPackageInfo(context.packageName, 0) }
    val code = if (android.os.Build.VERSION.SDK_INT >= 28) app.longVersionCode else app.versionCode.toLong()
    StreamWatchLogo(height = 30.dp)
    Text(stringResource(R.string.about_version, app.versionName.orEmpty()))
    Text(stringResource(R.string.about_version_code, code))
    Text(stringResource(R.string.about_version_date, stringResource(R.string.version_date)))
}

@Composable
private fun ProgrammeDetails(channel: StalkerChannel, programmes: List<PortalProgramme>?) {
    Text(channel.name, style = MaterialTheme.typography.titleMedium)
    when {
        programmes == null -> Text(stringResource(R.string.guide_loading))
        programmes.isEmpty() -> Text(stringResource(R.string.no_guide))
        else -> programmes.forEachIndexed { index, programme ->
            val label = when (index) {
                0 -> stringResource(R.string.guide_now)
                1 -> stringResource(R.string.guide_next)
                else -> null
            }
            val time = programme.startSeconds?.let {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(it))
            }
            Text(listOfNotNull(label, time, programme.title).joinToString(" · "))
        }
    }
}

@Composable
private fun episodeResumeLabel(episode: EpisodeResume, position: Long): String = listOfNotNull(
    episode.season?.let { stringResource(R.string.season_number, it) },
    episode.episode?.let { stringResource(R.string.episode_number, it) }
        ?: episode.name,
    position.takeIf { it > 0 }?.let { stringResource(R.string.saved_playback_position, playbackTime(it)) }
).joinToString(" · ")

@Composable
private fun CatalogOverview(
    modifier: Modifier,
    categoryTitle: String,
    catalog: PortalCatalog?,
    loading: Boolean,
    failed: Boolean,
    canLoadMore: Boolean,
    emptyMessage: Int,
    onLoadMore: () -> Unit,
    isFavorite: (PortalTitle) -> Boolean,
    isSaved: (PortalTitle) -> Boolean,
    preferPopularity: Boolean,
    returnTitle: PortalTitle?,
    focusFirstTitle: Boolean,
    onTitleFocusRestored: () -> Unit,
    newEpisodes: (PortalTitle) -> Int,
    onSelect: (PortalTitle) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (failed) Text(
            stringResource(R.string.catalog_load_failed),
            color = MaterialTheme.colorScheme.error
        )
        if (catalog == null || catalog.titles.isEmpty()) {
            if (!loading && !failed) Text(stringResource(emptyMessage))
            if (catalog?.nextPage != null && !loading && canLoadMore) {
                MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
            }
            return@Column
        }
        catalog.total?.let { total ->
            Text(stringResource(R.string.catalog_count, catalog.titles.size, total))
        }
        TitleGrid(
            catalog.titles, categoryTitle, isFavorite, isSaved, preferPopularity, newEpisodes,
            returnTitle, focusFirstTitle, onTitleFocusRestored, onSelect
        )
        if (catalog.nextPage != null && !loading && canLoadMore) {
            MenuButton(stringResource(R.string.catalog_load_more), Modifier, onClick = onLoadMore)
        }
    }
}

@Composable
private fun SettingsHeading(label: Int) {
    Text(
        stringResource(label),
        Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ControlMappingDialog(
    bindings: Map<GuideInput, ControlAction>,
    onChange: (GuideInput, ControlAction) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    SettingsDialog(
        stringResource(R.string.control_settings), onDismiss,
        buttons = { SettingRow(stringResource(R.string.control_reset), onClick = onReset) }
    ) {
        for (input in GuideInput.entries.filter { it != GuideInput.BACK }) {
            var expanded by remember(input) { mutableStateOf(false) }
            Box {
                SettingRow(
                    stringResource(input.label) + ": " + stringResource((bindings[input] ?: ControlAction.DEFAULT).label)
                ) { expanded = true }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    for (action in ControlAction.entries) {
                        DropdownMenuItem(text = { Text(stringResource(action.label)) }, onClick = {
                            onChange(input, action)
                            expanded = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
internal fun MenuButton(
    label: String,
    modifier: Modifier,
    selected: Boolean = false,
    logo: String? = null,
    count: Int? = null,
    /** Small marker at the top right of the label (e.g. an update at "Einstellungen"). */
    badge: Int? = null,
    hiddenIndicator: Boolean = false,
    leadingIcon: Int? = null,
    providerMark: String? = null,
    providerIcon: Int? = null,
    providerMarkOnly: Boolean = false,
    iconOnly: Boolean = false,
    /** Selection shown in the accent colour (symbol, or the text without symbol) instead of a stripe. */
    tintSelection: Boolean = false,
    enabled: Boolean = true,
    onFocused: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val highlighted = focused || hovered
    val colors = MaterialTheme.colorScheme
    // Same look as the settings rows: the focused entry gets a brighter surface and an accent
    // border; the selected one is written in the accent colour (symbol and text).
    val container = when {
        enabled && highlighted -> colors.surfaceContainerHighest
        // The selected entry (e.g. the open category): a light accent tint, the text stays white.
        selected && !tintSelection -> colors.primary.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    Surface(
        modifier = modifier
            .then(if (enabled) Modifier.hoverable(interactions) else Modifier)
            .onFocusChanged {
                if (it.isFocused && !focused) onFocused?.invoke()
                focused = it.isFocused
            }
            .focusProperties { canFocus = enabled || focused }
            // The press and focus effect stays inside the rounded shape (no square corners).
            .clip(RoundedCornerShape(12.dp))
            // Disabled entries ignore clicks but keep their focus (see SettingRow).
            .combinedClickable(
                onClick = { if (enabled) onClick() },
                onLongClick = onLongPress?.let { longPress -> { if (enabled) longPress() } }
            ),
        color = container,
        contentColor = when {
            !enabled -> colors.onSurface.copy(alpha = 0.38f)
            else -> colors.onSurface
        },
        shape = RoundedCornerShape(12.dp),
        border = when {
            enabled && highlighted -> BorderStroke(2.dp, accentBrush())
            else -> null
        }
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(logo, Modifier.padding(end = 8.dp))
            providerIcon?.let { icon ->
                Icon(
                    painterResource(icon), contentDescription = null,
                    modifier = Modifier.padding(end = if (providerMarkOnly) 0.dp else 8.dp).size(24.dp),
                    tint = Color.Unspecified
                )
            }
            providerMark?.let { mark ->
                Surface(
                    color = when (mark) {
                        "N" -> Color(0xFFE50914)
                        "prime video" -> Color(0xFF1689D4)
                        "tv+" -> Color(0xFF30343B)
                        "D+" -> Color(0xFF132A75)
                        "MAX" -> Color(0xFF4A248A)
                        "P+" -> Color(0xFF0756A8)
                        "sky" -> Color(0xFF0072C6)
                        else -> colors.primaryContainer
                    },
                    contentColor = Color.White,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = if (providerMarkOnly) 0.dp else 8.dp)
                ) {
                    Text(
                        mark,
                        Modifier.padding(horizontal = if (mark.length > 1) 6.dp else 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            leadingIcon?.let { icon ->
                Icon(
                    painterResource(icon),
                    contentDescription = label.takeIf { iconOnly },
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) colors.primary else LocalContentColor.current
                )
                if (!iconOnly) Box(Modifier.width(9.dp))
            }
            if (hiddenIndicator) {
                HiddenCategoryIcon()
                Box(Modifier.width(8.dp))
            }
            if ((!iconOnly || leadingIcon == null) && !providerMarkOnly) Text(
                label,
                modifier = if (count == null) Modifier else Modifier.weight(1f),
                // The top bar without symbols marks the text; elsewhere the tint and symbol do it.
                color = if (selected && tintSelection && leadingIcon == null) colors.primary else Color.Unspecified,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count != null) {
                Text(count.toString(), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall)
            }
            if (badge != null) Box(
                Modifier.align(Alignment.Top).padding(start = 4.dp)
                    .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape).padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
internal fun TextButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val highlighted = enabled && (focused || hovered)
    MaterialTextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = 48.dp).hoverable(interactions, enabled).onFocusChanged { focused = it.isFocused },
        interactionSource = interactions,
        shape = RoundedCornerShape(12.dp),
        border = if (highlighted) BorderStroke(2.dp, accentBrush()) else null,
        // Small cards like the settings rows: brighter with an accent border when focused.
        colors = ButtonDefaults.textButtonColors(
            // One step lighter than the dialog (which has surfaceContainerHigh), lighter still when focused.
            containerColor = if (highlighted) colors.onSurface.copy(alpha = 0.14f).compositeOver(colors.surfaceContainerHighest)
                else colors.surfaceContainerHighest,
            contentColor = colors.onSurface
        ),
        content = content
    )
}

@Preview(name = "Fernseher", widthDp = 960, heightDp = 540)
@Preview(name = "Handy im Querformat", widthDp = 852, heightDp = 393)
@Composable
private fun PlayerHomePreview() {
    MaterialTheme(colorScheme = playerColors(AccentChoice.RED)) {
        PlayerHome()
    }
}

/** "1 Tag", "5 Tage", "2 Wochen", "1 Monat" for the expiry notice lead time. */
internal fun expiryLeadLabel(context: Context, lead: ExpiryLead): String = when (lead.unit) {
    'w' -> context.resources.getQuantityString(R.plurals.expiry_lead_weeks, lead.amount, lead.amount)
    'm' -> context.getString(R.string.expiry_lead_month)
    else -> context.resources.getQuantityString(R.plurals.expiry_lead_days, lead.amount, lead.amount)
} + " " + context.getString(R.string.expiry_lead_before)

/** Why the last stream failed (HTTP code or error type), shown with the message when none plays. */
internal object PlaybackReason {
    var text by mutableStateOf<String?>(null)
}
