package com.iptvplayer.app

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * A language of the app interface. [tag] is the BCP-47 tag of the translation (values-… folder),
 * [name] how the language calls itself, [flag] a flag shown in front of it.
 */
internal data class AppLanguage(val tag: String, val name: String, val flag: String)

/** All interface languages, in the order of the list (as TiviMate, plus Albanian, Bosnian, Portuguese). */
internal val APP_LANGUAGES = listOf(
    AppLanguage("cs", "Čeština", "🇨🇿"),
    AppLanguage("da", "Dansk", "🇩🇰"),
    AppLanguage("de", "Deutsch", "🇩🇪"),
    AppLanguage("en", "English", "🇬🇧"),
    AppLanguage("es", "Español", "🇪🇸"),
    AppLanguage("fr", "Français", "🇫🇷"),
    AppLanguage("hr", "Hrvatski", "🇭🇷"),
    AppLanguage("bs", "Bosanski", "🇧🇦"),
    AppLanguage("it", "Italiano", "🇮🇹"),
    AppLanguage("lv", "Latviešu", "🇱🇻"),
    AppLanguage("lt", "Lietuvių", "🇱🇹"),
    AppLanguage("hu", "Magyar", "🇭🇺"),
    AppLanguage("nl", "Nederlands", "🇳🇱"),
    AppLanguage("nb", "Norsk", "🇳🇴"),
    AppLanguage("pl", "Polski", "🇵🇱"),
    AppLanguage("pt-BR", "Português (Brasil)", "🇧🇷"),
    AppLanguage("pt-PT", "Português (Portugal)", "🇵🇹"),
    AppLanguage("ro", "Română", "🇷🇴"),
    AppLanguage("sq", "Shqip", "🇦🇱"),
    AppLanguage("sk", "Slovenský", "🇸🇰"),
    AppLanguage("sl", "Slovenščina", "🇸🇮"),
    AppLanguage("sr-Latn", "Srpski", "🇷🇸"),
    AppLanguage("fi", "Suomi", "🇫🇮"),
    AppLanguage("sv", "Svenska", "🇸🇪"),
    AppLanguage("vi", "Tiếng Việt", "🇻🇳"),
    AppLanguage("tr", "Türkçe", "🇹🇷"),
    AppLanguage("el", "Ελληνικά", "🇬🇷"),
    AppLanguage("bg", "Български", "🇧🇬"),
    AppLanguage("ru", "Русский", "🇷🇺"),
    AppLanguage("sr", "Српски", "🇷🇸"),
    AppLanguage("uk", "Українська", "🇺🇦"),
    AppLanguage("ka", "ქართული", "🇬🇪"),
    AppLanguage("he", "עברית", "🇮🇱"),
    AppLanguage("ar", "عربي", "🇸🇦"),
    AppLanguage("zh", "中文", "🇨🇳"),
    AppLanguage("ja", "日本語", "🇯🇵"),
)

/**
 * Languages whose translation is complete; only these are offered and used. German is the
 * built-in text (values/), the others live in values-<tag>. Grows with every translation.
 */
internal val TRANSLATED_LANGUAGES = setOf("de", "en", "tr", "pl", "ru", "ar", "ro", "hr", "bs", "sr", "sr-Latn", "es", "fr", "it", "pt-BR", "sq", "uk", "cs", "da", "lv", "lt", "hu", "nl", "nb", "sv", "sk", "sl")

private const val LANGUAGE_KEY = "app_language"
private const val MIRROR_KEY = "mirror_rtl"
/** Languages written right to left: the user decides whether the whole layout is mirrored. */
internal val RTL_LANGUAGES = setOf("ar", "he")

internal fun mirrorLayout(context: Context): Boolean =
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).getBoolean(MIRROR_KEY, false)

internal fun saveMirrorLayout(context: Context, mirror: Boolean) {
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit().putBoolean(MIRROR_KEY, mirror).apply()
}

/** The chosen language tag, or null for the device language. */
internal fun chosenLanguage(context: Context): String? =
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).getString(LANGUAGE_KEY, null)
        ?.takeIf { it in TRANSLATED_LANGUAGES }

internal fun saveLanguage(context: Context, tag: String?) {
    context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE).edit()
        .apply { if (tag == null) remove(LANGUAGE_KEY) else putString(LANGUAGE_KEY, tag) }.apply()
}

/**
 * The language the interface uses: the chosen one, otherwise the device language if it is
 * translated, otherwise English.
 */
internal fun effectiveLanguage(chosen: String?, device: Locale): String {
    if (chosen != null && chosen in TRANSLATED_LANGUAGES) return chosen
    val candidates = listOf(
        device.toLanguageTag(),
        "${device.language}-${device.country}",
        if (device.script.isNotEmpty()) "${device.language}-${device.script}" else "",
        device.language
    )
    // Norwegian may come as "no"; Hebrew as "iw" on older systems.
    val aliases = mapOf("no" to "nb", "iw" to "he", "in" to "id")
    return candidates.map { aliases[it] ?: it }.firstOrNull { it in TRANSLATED_LANGUAGES }
        ?: if (device.language == "pt" && "pt-PT" in TRANSLATED_LANGUAGES && device.country != "BR") "pt-PT"
        else "en"
}

private fun deviceLocale(): Locale =
    android.content.res.Resources.getSystem().configuration.locales[0] ?: Locale.ENGLISH

/** Context in the interface language; used for the activity's base context. */
internal fun localizedContext(base: Context): Context {
    val tag = effectiveLanguage(chosenLanguage(base), deviceLocale())
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val configuration = Configuration(base.resources.configuration)
    configuration.setLocales(LocaleList(locale))
    // Right-to-left text is always shown in its reading direction; the layout is only mirrored
    // when the user chose that for the language.
    configuration.setLayoutDirection(if (tag in RTL_LANGUAGES && mirrorLayout(base)) locale else Locale.ENGLISH)
    return base.createConfigurationContext(configuration)
}

/** "App-Sprache" page: System first, then the translated languages with their flags. */
@Composable
internal fun AppLanguageSettings(current: String?, mirror: Boolean, onChoose: (String?, Boolean) -> Unit) {
    var askMirror by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<AppLanguage?>(null) }
    askMirror?.let { language ->
        MirrorQuestion(language, onDismiss = { askMirror = null }) { mirror ->
            askMirror = null
            onChoose(language.tag, mirror)
        }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    val device = remember { deviceLocale() }
    val deviceName = APP_LANGUAGES.firstOrNull { it.tag == effectiveLanguage(null, device) }?.name ?: "English"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.app_language_hint), style = MaterialTheme.typography.bodySmall)
        SettingRow(
            stringResource(R.string.app_language_system, deviceName),
            if (current == null) Modifier.focusRequester(first) else Modifier,
            selected = current == null
        ) { onChoose(null, mirror) }
        APP_LANGUAGES.filter { it.tag in TRANSLATED_LANGUAGES }.forEach { language ->
            SettingRow(
                "${language.flag}  ${language.name}",
                if (current == language.tag) Modifier.focusRequester(first) else Modifier,
                selected = current == language.tag
            ) { if (language.tag in RTL_LANGUAGES) askMirror = language else onChoose(language.tag, mirror) }
        }
        if (current in RTL_LANGUAGES) SettingRow(stringResource(R.string.mirror_setting), selected = mirror) {
            onChoose(current, !mirror)
        }
    }
}

/**
 * "Oberfläche spiegeln?" for a right-to-left language, asked in that language (the reader of
 * the chosen language must understand it, whatever the interface shows right now).
 */
@Composable
internal fun MirrorQuestion(language: AppLanguage, onDismiss: () -> Unit, onAnswer: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val texts = remember(language) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(language.tag))
        context.createConfigurationContext(configuration).resources
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(texts.getString(R.string.mirror_title)) },
        text = { Text(texts.getString(R.string.mirror_text, language.name)) },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(texts.getString(R.string.mirror_yes)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(texts.getString(R.string.mirror_no)) } }
    )
}

