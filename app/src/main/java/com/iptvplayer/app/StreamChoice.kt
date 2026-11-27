package com.iptvplayer.app

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit

/*
 * Automatic channel switch on errors (on by default, as before) or the stream chosen by hand:
 * without the automatic a Live-TV channel with several streams asks which one to play, and the
 * app never changes it by itself. On an error it shows the list again, or does nothing (the
 * viewer switches or stops, and the list comes with the next start).
 */

internal object StreamSwitch {
    private fun prefs(context: Context) = context.getSharedPreferences("catalog_visibility", Context.MODE_PRIVATE)

    fun auto(context: Context): Boolean = prefs(context).getBoolean("auto_stream_switch", true)
    fun setAuto(context: Context, on: Boolean) = prefs(context).edit { putBoolean("auto_stream_switch", on) }

    /** Without the automatic: show the list of streams when one fails (false: do nothing). */
    fun listOnError(context: Context): Boolean = prefs(context).getBoolean("manual_stream_error_list", true)
    fun setListOnError(context: Context, on: Boolean) = prefs(context).edit { putBoolean("manual_stream_error_list", on) }
}

/** Settings rows under Wiedergabe: the switch, then the settings that belong to its state. */
@Composable
internal fun StreamSwitchSettings() {
    val context = LocalContext.current
    var auto by remember { mutableStateOf(StreamSwitch.auto(context)) }
    var list by remember { mutableStateOf(StreamSwitch.listOnError(context)) }
    SettingRow(stringResource(R.string.auto_switch_setting), selected = auto) {
        auto = !auto
        StreamSwitch.setAuto(context, auto)
    }
    if (auto) {
        // The settings of the automatic switch only matter while it is on.
        SmoothSwitchSettings()
    } else {
        Text(stringResource(R.string.auto_switch_off_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingRow(stringResource(R.string.manual_error_setting) + ": " +
            stringResource(if (list) R.string.manual_error_list else R.string.manual_error_none)) {
            list = !list
            StreamSwitch.setListOnError(context, list)
        }
    }
}

/** The same for the phone page (section [section]). */
internal fun streamSwitchRemoteEntries(context: Context, section: String): List<RemoteEntry> = listOf(
    RemoteEntry.Toggle("autoSwitch", section, context.getString(R.string.auto_switch_setting), { StreamSwitch.auto(context) }) {
        StreamSwitch.setAuto(context, it)
    },
    RemoteEntry.Toggle("smoothSwitch", section, context.getString(R.string.smooth_switch_setting), { smoothSwitchEnabled(context) }) {
        setSmoothSwitchEnabled(context, it)
    }.shownIf("autoSwitch", true),
    RemoteEntry.Choice("manualError", section, context.getString(R.string.manual_error_setting),
        listOf(
            RemoteOption("list", context.getString(R.string.manual_error_list)),
            RemoteOption("none", context.getString(R.string.manual_error_none))
        ),
        { if (StreamSwitch.listOnError(context)) "list" else "none" }
    ) { value -> StreamSwitch.setListOnError(context, value == "list") }.shownIf("autoSwitch", false)
)

/** The streams of a channel to choose from; [failed]: the chosen one did not play. */
@Composable
internal fun StreamChoiceDialog(labels: List<String>, current: Int?, failed: Boolean, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    SettingsDialog(stringResource(R.string.stream_choice_title), onDismiss) {
        if (failed) Text(stringResource(R.string.stream_choice_failed), color = MaterialTheme.colorScheme.error)
        labels.forEachIndexed { index, label ->
            SettingRow(label, if (index == 0) Modifier.focusRequester(first) else Modifier, selected = if (failed) index == current else null) {
                onPick(index)
            }
        }
        SettingRow(stringResource(android.R.string.cancel)) { onDismiss() }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        runCatching { first.requestFocus() }
    }
}
