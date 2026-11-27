package com.iptvplayer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Settings page "Per Handy einstellen": starts a temporary, end-to-end encrypted session and
 * shows its QR code (and only the connection status: what is changed on the phone is not shown
 * here). The session ends with "Zugriff beenden" here or on the phone (then Live-TV opens), when
 * the page or the app is left and after 15 minutes without any change.
 */
@Composable
internal fun RemoteAccessPage(
    handler: RemoteHandler,
    storage: PortalStorage? = null,
    unlock: ((String) -> Boolean)? = null,
    unlockPassword: Boolean = false,
    onPinReset: () -> Unit, onFinished: () -> Unit, onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // The phone's backup page: create (download) and import (upload) a backup.
    val sessionHandler = remember(handler, storage) { backupRemoteHandler(handler, storage, context) }
    var session by remember { mutableStateOf<RemoteSession?>(null) }
    fun stop() {
        session?.end()
    }
    DisposableEffect(Unit) { onDispose { session?.end() } }
    val lifecycle = (LocalContext.current as? LifecycleOwner)?.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) stop() }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val current = session
    val endedMessage = stringResource(R.string.remote_msg_ended)
    val failedMessage = stringResource(R.string.remote_msg_change_failed)
    val wrongPin = stringResource(R.string.pin_wrong)
    val startFocus = remember { FocusRequester() }
    val endFocus = remember { FocusRequester() }
    val running = current != null && current.status.collectAsState().value != RemoteStatus.ENDED
    LaunchedEffect(running) {
        // The row that was pressed disappears; keep the focus on this page (not the top bar).
        androidx.compose.runtime.withFrameNanos { }
        runCatching { (if (running) endFocus else startFocus).requestFocus() }
    }
    LaunchedEffect(current) {
        val active = current ?: return@LaunchedEffect
        // Every change on the device (or from the phone) is sent to the phone right away.
        snapshotFlow { RemoteProtocol.encodeState(sessionHandler.remoteState(), "") }.collect { active.publish() }
    }
    SettingsDialog(stringResource(R.string.remote_access), onDismiss) {
        val status = current?.status?.collectAsState()?.value ?: RemoteStatus.ENDED
        if (current == null || status == RemoteStatus.ENDED) {
            if (current != null) Text(stringResource(R.string.remote_status_ended), color = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.remote_access_hint), style = MaterialTheme.typography.bodyMedium)
            SettingRow(stringResource(R.string.remote_access_start), Modifier.focusRequester(startFocus)) {
                onPinReset()
                session = RemoteSession(
                    scope, sessionHandler, endedMessage, failedMessage, onFinished,
                    unlock = unlock, unlockPassword = unlockPassword, wrongPinMessage = wrongPin
                ).also { it.start() }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                QrCode(current.pageUrl, Modifier.size(240.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(when (status) {
                            RemoteStatus.CONNECTING -> R.string.remote_status_connecting
                            RemoteStatus.WAITING -> R.string.remote_status_waiting
                            RemoteStatus.CONNECTED -> R.string.remote_status_connected
                            RemoteStatus.OFFLINE -> R.string.remote_status_offline
                            RemoteStatus.ENDED -> R.string.remote_status_ended
                        }),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SettingRow(stringResource(R.string.remote_access_end), Modifier.focusRequester(endFocus)) { stop() }
                }
            }
        }
    }
}

/** Largest backup the phone can download or upload (the relay forwards frames up to 512 KB). */
private const val PHONE_BACKUP_LIMIT = 270_000

/**
 * Adds the backup commands of the phone page to [base]: they run off the main thread, and the
 * state names the backup parts in the app language.
 */
internal fun backupRemoteHandler(base: RemoteHandler, storage: PortalStorage?, context: android.content.Context) =
    object : RemoteHandler {
        override fun remoteState(): RemoteState = base.remoteState().copy(
            backupParts = if (storage == null) emptyList()
            else BackupPart.entries.map { RemoteOption(it.name, context.getString(it.label)) },
            language = chosenLanguage(context) ?: java.util.Locale.getDefault().language
        )

        override fun applyRemote(command: RemoteCommand): RemoteReply = base.applyRemote(command)

        override suspend fun applyRemoteSuspending(command: RemoteCommand): RemoteReply? {
            val store = storage ?: return null
            fun parts(names: List<String>) = BackupPart.entries.filter { it.name in names }.toSet()
            fun decode(file: String): BackupDocument {
                val bytes = java.util.Base64.getDecoder().decode(file)
                return if (isBackupFile(bytes)) decodeBackup(bytes)
                else legacyPortalBackup(bytes.toString(Charsets.UTF_8)) ?: error("not a backup")
            }
            return when (command) {
                is RemoteCommand.BackupCreate -> {
                    val chosen = parts(command.parts).ifEmpty { return RemoteReply(false, context.getString(R.string.backup_failed)) }
                    val bytes = createBackup(context, store, chosen)
                    if (bytes.size > PHONE_BACKUP_LIMIT) RemoteReply(false, context.getString(R.string.backup_too_large_phone))
                    else RemoteReply(true, null, com.google.gson.JsonObject().apply {
                        addProperty("name", backupFileName())
                        addProperty("file", java.util.Base64.getEncoder().encodeToString(bytes))
                    })
                }
                is RemoteCommand.BackupInspect -> {
                    val document = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { decode(command.file) } }
                        .getOrElse { error -> return RemoteReply(false, context.getString(
                            if (error is BackupPasswordException) R.string.backup_password_phone else R.string.backup_failed
                        )) }
                    RemoteReply(true, null, com.google.gson.JsonObject().apply {
                        add("backupParts", com.google.gson.JsonArray().apply { document.parts.keys.forEach { add(it.name) } })
                        addProperty("createdAt", document.createdAt.replace('T', ' '))
                        addProperty("appVersion", document.appVersion)
                    })
                }
                is RemoteCommand.BackupRestore -> {
                    val document = runCatching { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { decode(command.file) } }
                        .getOrElse { error -> return RemoteReply(false, context.getString(
                            if (error is BackupPasswordException) R.string.backup_password_phone else R.string.backup_failed
                        )) }
                    restoreBackup(context, store, document, parts(command.parts))
                    // Answer first, then restart: the phone shows that the import worked.
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ restartApp(context) }, 1_500)
                    RemoteReply(true, context.getString(R.string.backup_imported_restart))
                }
                else -> null
            }
        }
    }
