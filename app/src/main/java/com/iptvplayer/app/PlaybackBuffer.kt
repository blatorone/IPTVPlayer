package com.iptvplayer.app

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.media3.exoplayer.DefaultLoadControl

/**
 * Fixed buffer sizes, as in other IPTV players. [startMs]: buffered before playback starts;
 * [rebufferMs]: before it continues after a stall; [minMs]/[maxMs]: how much is kept ahead.
 * A larger buffer starts a bit later but stutters less on unsteady streams.
 */
internal enum class BufferLevel(val label: Int, val minMs: Int, val maxMs: Int, val startMs: Int, val rebufferMs: Int) {
    SMALL(R.string.buffer_small, 15_000, 30_000, 2_000, 4_000),
    MEDIUM(R.string.buffer_medium, 30_000, 60_000, 4_000, 8_000),
    LARGE(R.string.buffer_large, 60_000, 120_000, 8_000, 15_000),
    VERY_LARGE(R.string.buffer_very_large, 90_000, 180_000, 15_000, 25_000)
}

private const val BUFFER_ENABLED = "buffer_enabled"
private const val BUFFER_LEVEL = "buffer_level"

/** The chosen buffer, or null when it is off (the player keeps its usual values). */
internal fun chosenBuffer(context: Context): BufferLevel? {
    val preferences = context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)
    if (!preferences.getBoolean(BUFFER_ENABLED, false)) return null
    return BufferLevel.entries.firstOrNull { it.name == preferences.getString(BUFFER_LEVEL, null) } ?: BufferLevel.MEDIUM
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun bufferLoadControl(level: BufferLevel?): DefaultLoadControl =
    if (level == null) DefaultLoadControl()
    else DefaultLoadControl.Builder()
        .setBufferDurationsMs(level.minMs, level.maxMs, level.startMs, level.rebufferMs)
        .build()

/** Settings rows under Wiedergabe: buffer on/off and its size (applies after a restart of the app). */
@Composable
internal fun BufferSettings() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(preferences.getBoolean(BUFFER_ENABLED, false)) }
    var level by remember { mutableStateOf(chosenBuffer(context) ?: BufferLevel.MEDIUM) }
    var changed by remember { mutableStateOf(false) }
    SettingRow(stringResource(R.string.buffer_setting), selected = enabled) {
        enabled = !enabled
        changed = true
        preferences.edit().putBoolean(BUFFER_ENABLED, enabled).apply()
    }
    if (enabled) SettingRow(stringResource(R.string.buffer_size_setting, stringResource(level.label))) {
        level = BufferLevel.entries[(level.ordinal + 1) % BufferLevel.entries.size]
        changed = true
        preferences.edit().putString(BUFFER_LEVEL, level.name).apply()
    }
    Text(
        stringResource(if (changed) R.string.buffer_restart_hint else R.string.buffer_hint),
        style = MaterialTheme.typography.bodySmall,
        color = if (changed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
