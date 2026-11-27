package com.iptvplayer.app

import androidx.core.content.edit
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch

/**
 * Version of the terms that must be accepted. Only raised when a changed text has to be
 * accepted again (the user decides that for every change).
 */
internal const val DISCLAIMER_VERSION = 1
private const val DISCLAIMER_KEY = "disclaimer_accepted_version"

/**
 * Whether the terms were accepted. Installs from before the notice (they already have settings
 * or portals) count as accepted without asking: only new installs see it.
 */
internal fun disclaimerAccepted(context: Context): Boolean {
    val preferences = context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    if (preferences.contains(DISCLAIMER_KEY)) return preferences.getInt(DISCLAIMER_KEY, 0) >= DISCLAIMER_VERSION
    // The first-start language choice is not a sign of an earlier install.
    val firstStartKeys = setOf("app_language", "mirror_rtl", "first_language_chosen")
    val existingInstall = preferences.all.keys.any { it !in firstStartKeys } ||
        listOf("portal-library.v2", "stalker-source.v1", "stalker-source.v1.migrated")
            .any { File(context.noBackupFilesDir, it).exists() }
    if (existingInstall) {
        preferences.edit {
            putInt(DISCLAIMER_KEY, DISCLAIMER_VERSION)
            putLong("disclaimer_accepted_at", System.currentTimeMillis())
        }
    }
    return existingInstall
}

internal fun acceptDisclaimer(context: Context) {
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit {
        putInt(DISCLAIMER_KEY, DISCLAIMER_VERSION)
        putLong("disclaimer_accepted_at", System.currentTimeMillis())
    }
}

/** First start: the terms fill the screen; nothing else can be used before "Akzeptieren". */
@Composable
internal fun DisclaimerScreen(onAccept: () -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    // Focus starts on the text: Down scrolls through it first, then reaches the buttons.
    val text = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { text.requestFocus() } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.widthIn(max = 900.dp).padding(32.dp),
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                StreamWatchLogo(height = 28.dp)
                Text(stringResource(R.string.disclaimer_title), style = MaterialTheme.typography.headlineSmall)
                DisclaimerText(Modifier.weight(1f, fill = false), text)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    TextButton(onClick = onExit) { Text(stringResource(R.string.disclaimer_exit)) }
                    TextButton(onClick = onAccept) { Text(stringResource(R.string.disclaimer_accept)) }
                }
            }
        }
    }
}

/**
 * The terms as text. With the remote, Down/Up scroll it while there is more to read; at the end
 * Down moves on to the buttons.
 */
@Composable
internal fun DisclaimerText(modifier: Modifier = Modifier, focus: FocusRequester = remember { FocusRequester() }) =
    RemoteScrollText(stringResource(R.string.disclaimer_text), modifier, focus)

/** Longer text that the remote can scroll: Down/Up scroll while there is more, then move on. */
@Composable
internal fun RemoteScrollText(text: String, modifier: Modifier = Modifier, focus: FocusRequester = remember { FocusRequester() }) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    Text(
        text,
        modifier
            // A scrolling text needs a height limit: inside the (scrolling) settings panel it had
            // none, and opening the terms there crashed the app.
            .heightIn(max = 520.dp)
            .border(2.dp, if (focused) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
            .padding(8.dp)
            .verticalScroll(scroll)
            .focusRequester(focus)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = 220f
                when {
                    event.key == Key.DirectionDown && scroll.canScrollForward -> { scope.launch { scroll.animateScrollBy(step) }; true }
                    event.key == Key.DirectionUp && scroll.canScrollBackward -> { scope.launch { scroll.animateScrollBy(-step) }; true }
                    else -> false
                }
            }
            .focusable(),
        style = MaterialTheme.typography.bodyMedium
    )
}

/** Whether the language was chosen on the first start (installs before this count as done). */
internal fun firstLanguageChosen(context: Context): Boolean =
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).getBoolean("first_language_chosen", false)

internal fun markFirstLanguageChosen(context: Context) {
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit { putBoolean("first_language_chosen", true) }
}

/**
 * First start, before the terms: the interface language (System first). Arabic and Hebrew ask
 * whether the layout is mirrored. The terms then appear in the chosen language.
 */
@Composable
internal fun FirstLanguageScreen(onChosen: (String?, Boolean) -> Unit, onExit: () -> Unit) {
    BackHandler(onBack = onExit)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Surface(
            Modifier.widthIn(max = 900.dp).padding(32.dp),
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(Modifier.padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StreamWatchLogo(height = 28.dp)
                Text("🌐  " + stringResource(R.string.app_language), style = MaterialTheme.typography.headlineSmall)
                // First only the question (in the device language); the list opens on "Sprache ändern".
                var choosing by remember { mutableStateOf(false) }
                if (choosing) AppLanguageSettings(null, false, onChosen) else {
                    val deviceLanguage = APP_LANGUAGES.firstOrNull {
                        it.tag == effectiveLanguage(null, android.content.res.Resources.getSystem().configuration.locales[0] ?: java.util.Locale.ENGLISH)
                    }
                    val current = deviceLanguage?.name ?: "English"
                    // Keeping a right-to-left device language also asks whether to mirror the layout.
                    var askMirror by remember { mutableStateOf(false) }
                    if (askMirror && deviceLanguage != null) MirrorQuestion(deviceLanguage, { askMirror = false }) { onChosen(null, it) }
                    Text(stringResource(R.string.first_language_question, current), style = MaterialTheme.typography.bodyLarge)
                    val keep = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { keep.requestFocus() } }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuButton(stringResource(R.string.first_language_keep, current), Modifier.focusRequester(keep)) {
                            if (deviceLanguage?.tag in RTL_LANGUAGES) askMirror = true else onChosen(null, false)
                        }
                        MenuButton(stringResource(R.string.first_language_change), Modifier) { choosing = true }
                    }
                }
            }
        }
    }
}

