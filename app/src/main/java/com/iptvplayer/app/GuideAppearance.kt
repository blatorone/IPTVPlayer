package com.iptvplayer.app

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource

internal enum class GuideSize(val label: Int, val factor: Float) {
    SMALL(R.string.guide_size_small, 0.83f),
    MEDIUM(R.string.guide_size_medium, 1f),
    LARGE(R.string.guide_size_large, 1.2f),
    EXTRA_LARGE(R.string.guide_size_extra_large, 1.4f);

    fun next() = entries[(ordinal + 1) % entries.size]
}

/** The veil over the time already gone in the guide. */
internal enum class GuideVeil(val label: Int) {
    LINE_COLOR(R.string.guide_veil_line_color),
    DARK(R.string.guide_veil_dark),
    OFF(R.string.subtitle_off);

    fun next() = entries[(ordinal + 1) % entries.size]
}

/** Colour of the "now" line in the guide; cyan stands out best next to a red accent. */
internal enum class GuideNowColor(val label: Int, val color: Color) {
    CYAN(R.string.accent_cyan, Color(0xFF00E5FF)),
    TURQUOISE(R.string.accent_turquoise, Color(0xFF1DE9B6)),
    YELLOW(R.string.accent_yellow, Color(0xFFF5C451)),
    LIME(R.string.accent_lime, Color(0xFFC6FF00)),
    ICE(R.string.accent_ice, Color(0xFF81D4FA));

    fun next() = entries[(ordinal + 1) % entries.size]
}

internal data class GuideAppearance(
    val rowSize: GuideSize = GuideSize.MEDIUM,
    val textSize: GuideSize = GuideSize.MEDIUM,
    val previewSize: GuideSize = GuideSize.MEDIUM,
    val listWidth: GuideSize = GuideSize.MEDIUM,
    val showPreview: Boolean = true,
    val showNumbers: Boolean = true,
    val showLogos: Boolean = true,
    val showProgress: Boolean = true,
    val showSchedule: Boolean = true,
    val showDescription: Boolean = true,
    val keepChannelList: Boolean = false,
    val opacity: Int = 90,
    val closeSeconds: Int = 15,
    val nowColor: GuideNowColor = GuideNowColor.CYAN,
    val veil: GuideVeil = GuideVeil.LINE_COLOR
) {
    fun save(preferences: SharedPreferences?) {
        preferences?.edit()
            ?.putString("guide_row_size", rowSize.name)?.putString("guide_text_size", textSize.name)
            ?.putString("guide_preview_size", previewSize.name)?.putString("guide_list_width", listWidth.name)
            ?.putBoolean("guide_preview_visible", showPreview)?.putBoolean("guide_numbers", showNumbers)
            ?.putBoolean("guide_logos", showLogos)?.putBoolean("guide_progress", showProgress)
            ?.putBoolean("guide_schedule", showSchedule)?.putBoolean("guide_description", showDescription)
            ?.putBoolean("guide_keep_list", keepChannelList)?.putInt("guide_opacity", opacity)
            ?.putInt("guide_close_seconds", closeSeconds)?.putString("guide_now_color", nowColor.name)
            ?.putString("guide_veil", veil.name)?.apply()
    }
}

internal fun readGuideAppearance(preferences: SharedPreferences?): GuideAppearance {
    fun size(key: String): GuideSize =
        GuideSize.entries.firstOrNull { it.name == preferences?.getString(key, null) } ?: GuideSize.MEDIUM
    fun enabled(key: String, default: Boolean = true) = preferences?.getBoolean(key, default) ?: default
    return GuideAppearance(
        size("guide_row_size"), size("guide_text_size"), size("guide_preview_size"), size("guide_list_width"),
        enabled("guide_preview_visible"), enabled("guide_numbers"), enabled("guide_logos"),
        enabled("guide_progress"), enabled("guide_schedule"), enabled("guide_description"),
        enabled("guide_keep_list", false),
        preferences?.getInt("guide_opacity", 90)?.coerceIn(60, 100) ?: 90,
        preferences?.getInt("guide_close_seconds", 15)?.coerceIn(0, 60) ?: 15,
        GuideNowColor.entries.firstOrNull { it.name == preferences?.getString("guide_now_color", null) } ?: GuideNowColor.CYAN,
        GuideVeil.entries.firstOrNull { it.name == preferences?.getString("guide_veil", null) } ?: GuideVeil.LINE_COLOR
    )
}

internal enum class GuideAppearancePage(val label: Int) {
    GRID(R.string.destination_guide),
    CHANNELS(R.string.guide_channel_list_appearance)
}

@Composable
internal fun GuideAppearanceDialog(
    page: GuideAppearancePage,
    appearance: GuideAppearance,
    onChange: (GuideAppearance) -> Unit,
    onDismiss: () -> Unit
) {
    SettingsDialog(stringResource(page.label), onDismiss) {
                if (page == GuideAppearancePage.GRID) {
                    AppearanceSize(R.string.guide_row_size, appearance.rowSize) { onChange(appearance.copy(rowSize = it)) }
                    AppearanceSize(R.string.guide_text_size, appearance.textSize) { onChange(appearance.copy(textSize = it)) }
                    AppearanceSize(R.string.guide_preview_size, appearance.previewSize) { onChange(appearance.copy(previewSize = it)) }
                    AppearanceToggle(R.string.guide_preview_visible, appearance.showPreview) {
                        onChange(appearance.copy(showPreview = !appearance.showPreview))
                    }
                    AppearanceToggle(R.string.guide_numbers_visible, appearance.showNumbers) {
                        onChange(appearance.copy(showNumbers = !appearance.showNumbers))
                    }
                    AppearanceToggle(R.string.guide_logos_visible, appearance.showLogos) {
                        onChange(appearance.copy(showLogos = !appearance.showLogos))
                    }
                    SettingRow(stringResource(R.string.guide_setting_value, stringResource(R.string.guide_now_line_color),
                        stringResource(appearance.nowColor.label))) {
                        onChange(appearance.copy(nowColor = appearance.nowColor.next()))
                    }
                    SettingRow(stringResource(R.string.guide_setting_value, stringResource(R.string.guide_veil),
                        stringResource(appearance.veil.label))) {
                        onChange(appearance.copy(veil = appearance.veil.next()))
                    }
                } else {
                    AppearanceSize(R.string.guide_list_width, appearance.listWidth) { onChange(appearance.copy(listWidth = it)) }
                    AppearanceToggle(R.string.guide_show_schedule, appearance.showSchedule) {
                        onChange(appearance.copy(showSchedule = !appearance.showSchedule))
                    }
                    AppearanceToggle(R.string.guide_show_description, appearance.showDescription) {
                        onChange(appearance.copy(showDescription = !appearance.showDescription, showSchedule = true))
                    }
                    AppearanceToggle(R.string.guide_show_progress, appearance.showProgress) {
                        onChange(appearance.copy(showProgress = !appearance.showProgress))
                    }
                    AppearanceToggle(R.string.guide_keep_list, appearance.keepChannelList) {
                        onChange(appearance.copy(keepChannelList = !appearance.keepChannelList))
                    }
                    SettingRow(stringResource(R.string.guide_opacity, appearance.opacity)) {
                        val steps = listOf(60, 70, 80, 90, 100)
                        onChange(appearance.copy(opacity = steps[(steps.indexOf(appearance.opacity) + 1) % steps.size]))
                    }
                    val timeout = if (appearance.closeSeconds == 0) stringResource(R.string.guide_manual_close)
                        else stringResource(R.string.guide_seconds, appearance.closeSeconds)
                    SettingRow(stringResource(R.string.guide_auto_close, timeout)) {
                        val steps = listOf(0, 5, 10, 15, 30, 60)
                        onChange(appearance.copy(closeSeconds = steps[(steps.indexOf(appearance.closeSeconds) + 1) % steps.size]))
                    }
                }
    }
}

@Composable
private fun AppearanceSize(label: Int, size: GuideSize, onChange: (GuideSize) -> Unit) {
    SettingRow(stringResource(R.string.guide_setting_value, stringResource(label), stringResource(size.label))) {
        onChange(size.next())
    }
}

@Composable
private fun AppearanceToggle(label: Int, enabled: Boolean, onClick: () -> Unit) {
    SettingRow(stringResource(label), selected = enabled, onClick = onClick)
}
