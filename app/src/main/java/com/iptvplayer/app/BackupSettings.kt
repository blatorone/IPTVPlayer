package com.iptvplayer.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BackupStage { IDLE, EXPORT, IMPORT_SOURCE, IMPORT }

/**
 * "Backup" in Daten & Diagnose: export or import everything or chosen parts. Files go to the
 * Download folder (works on Fire TV, which has no folder picker) or to a chosen place where the
 * device offers one. [guard] asks for the settings PIN when portals are locked.
 */
@Composable
internal fun BackupSettings(storage: PortalStorage?, guard: (() -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stage by remember { mutableStateOf(BackupStage.IDLE) }
    var chosen by remember { mutableStateOf(BackupPart.entries.toSet()) }
    var loaded by remember { mutableStateOf<BackupDocument?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf(emptyList<StoredBackup>()) }

    // The two main rows stay; the steps open below them. Before a step changes, focus moves to a
    // row that stays, so it never falls back to the section bar (which would switch the section).
    val createRow = remember { FocusRequester() }
    val importRow = remember { FocusRequester() }
    val firstStepRow = remember { FocusRequester() }
    fun go(next: BackupStage) {
        runCatching { (if (stage == BackupStage.EXPORT) createRow else importRow).requestFocus() }
        stage = next
    }
    LaunchedEffect(stage) {
        if (stage == BackupStage.IDLE) return@LaunchedEffect
        withFrameNanos { }
        runCatching { firstStepRow.requestFocus() }
    }


    fun fail() { status = context.getString(R.string.backup_failed) }
    fun open(uri: android.net.Uri) {
        scope.launch {
            try {
                val document = withContext(Dispatchers.IO) { readBackup(context, uri) }
                loaded = document
                chosen = document.parts.keys
                go(BackupStage.IMPORT)
                status = null
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail()
            }
        }
    }
    suspend fun build(): ByteArray = createBackup(context, requireNotNull(storage), chosen)

    val saveElsewhere = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            working = true
            try {
                val bytes = build()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no stream")
                }
                status = context.getString(R.string.backup_saved, uri.lastPathSegment?.substringAfterLast('/') ?: "")
                go(BackupStage.IDLE)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail()
            } finally {
                working = false
            }
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) open(uri) }
    fun picker(launch: () -> Unit) {
        // Fire TV has no document picker: say so instead of doing nothing.
        try { launch() } catch (_: android.content.ActivityNotFoundException) {
            status = context.getString(R.string.backup_picker_missing)
        }
    }

    Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.backup_description))
    SettingRow(stringResource(R.string.backup_create), Modifier.focusRequester(createRow), enabled = storage != null) {
        if (stage == BackupStage.EXPORT) go(BackupStage.IDLE) else guard {
            chosen = BackupPart.entries.toSet()
            status = null
            go(BackupStage.EXPORT)
        }
    }
    if (stage == BackupStage.EXPORT) {
        Text(stringResource(R.string.backup_pick_export))
        PartChoice(BackupPart.entries, chosen, firstStepRow) { chosen = it }
        SettingRow(
            stringResource(R.string.backup_save_downloads),
            enabled = chosen.isNotEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        ) {
            // Rows stay enabled while saving: a disabled row would lose the focus.
            if (!working) scope.launch {
                working = true
                try {
                    val bytes = build()
                    val name = withContext(Dispatchers.IO) { saveBackupToDownloads(context, bytes) }
                    status = context.getString(R.string.backup_saved, name)
                    go(BackupStage.IDLE)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    fail()
                } finally {
                    working = false
                }
            }
        }
        SettingRow(stringResource(R.string.backup_save_choose), enabled = chosen.isNotEmpty()) {
            picker { saveElsewhere.launch(backupFileName()) }
        }
        SettingRow(stringResource(android.R.string.cancel)) { go(BackupStage.IDLE) }
    }
    SettingRow(stringResource(R.string.backup_import), Modifier.focusRequester(importRow), enabled = storage != null) {
        if (stage == BackupStage.IMPORT_SOURCE || stage == BackupStage.IMPORT) go(BackupStage.IDLE) else guard {
            status = null
            found = backupsInDownloads(context)
            go(BackupStage.IMPORT_SOURCE)
        }
    }
    if (stage == BackupStage.IMPORT_SOURCE) {
        if (found.isEmpty()) Text(stringResource(R.string.backup_none_found))
        found.forEachIndexed { index, backup ->
            SettingRow(backup.name, if (index == 0) Modifier.focusRequester(firstStepRow) else Modifier) { open(backup.uri) }
        }
        SettingRow(
            stringResource(R.string.backup_from_file), if (found.isEmpty()) Modifier.focusRequester(firstStepRow) else Modifier
        ) { picker { openFile.launch(arrayOf("*/*")) } }
        SettingRow(stringResource(android.R.string.cancel)) { go(BackupStage.IDLE) }
    }
    val document = loaded
    if (stage == BackupStage.IMPORT && document != null) {
        if (document.createdAt.isNotBlank()) Text(stringResource(
            R.string.backup_file_info, document.createdAt.replace('T', ' '), document.appVersion
        ))
        Text(stringResource(R.string.backup_pick_import))
        PartChoice(BackupPart.entries.filter { it in document.parts }, chosen, firstStepRow) { chosen = it }
        Text(stringResource(R.string.backup_restart_hint), style = MaterialTheme.typography.bodySmall)
        SettingRow(stringResource(R.string.backup_import_now), enabled = chosen.isNotEmpty()) {
            if (!working) scope.launch {
                working = true
                try {
                    restoreBackup(context, requireNotNull(storage), document, chosen)
                    restartApp(context)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    fail()
                    working = false
                }
            }
        }
        SettingRow(stringResource(android.R.string.cancel)) { go(BackupStage.IDLE); loaded = null }
    }
    if (working) androidx.compose.material3.LinearProgressIndicator()
    status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
}

/** "Alles" plus one switch per part. */
@Composable
private fun PartChoice(parts: List<BackupPart>, chosen: Set<BackupPart>, first: FocusRequester, onChange: (Set<BackupPart>) -> Unit) {
    val all = parts.isNotEmpty() && parts.all { it in chosen }
    SettingRow(stringResource(R.string.backup_all), Modifier.focusRequester(first), selected = all) { onChange(if (all) emptySet() else parts.toSet()) }
    parts.forEach { part ->
        SettingRow(stringResource(part.label), selected = part in chosen) {
            onChange(if (part in chosen) chosen - part else chosen + part)
        }
    }
}
