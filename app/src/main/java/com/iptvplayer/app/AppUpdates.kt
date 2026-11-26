package com.iptvplayer.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import java.io.File
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val repository = "blatorone/IPTVPlayer-Releases"
private const val maxApkBytes = 150L * 1024 * 1024
/** "v1.2.3" for releases, "v1.2.3-beta.4" for betas (published on GitHub as pre-releases). */
private val releaseTag = Regex("""v\d+\.\d+\.\d+(-beta\.\d+)?""")

internal data class AppRelease(
    val tag: String, val code: Long, val notes: String, val digest: String, val beta: Boolean = false,
    val assetName: String = "IPTVPlayer-$code.apk"
) {
    val downloadUrl get() = "https://github.com/$repository/releases/download/$tag/$assetName"
}

/**
 * APK names of a release: "StreamWatch-<code>.apk" since 1.0.93. During the change the same file is
 * also published as "IPTVPlayer-<code>.apk" so that older installs still find their update.
 */
private val apkPrefixes = listOf("StreamWatch-", "IPTVPlayer-")

/**
 * Whether a GitHub release is offered: release tags always, betas ("-beta.N", marked as
 * pre-release) only with the beta switch. A beta tag without the pre-release mark, or the other
 * way round, is never offered, so a mistake cannot hand a beta to everyone.
 */
internal fun offersRelease(tag: String, draft: Boolean, prerelease: Boolean, allowBeta: Boolean): Boolean =
    releaseTag.matches(tag) && !draft && tag.contains("-beta.") == prerelease && (!prerelease || allowBeta)

internal fun parseAppRelease(json: JSONObject, installedCode: Long, allowBeta: Boolean = false): AppRelease? {
    val tag = json.optString("tag_name")
    val beta = json.optBoolean("prerelease")
    if (!offersRelease(tag, json.optBoolean("draft"), beta, allowBeta)) return null
    val assets = json.getJSONArray("assets")
    // The StreamWatch name is preferred; the earlier name is still accepted.
    for (prefix in apkPrefixes) for (index in 0 until assets.length()) {
        val asset = assets.getJSONObject(index)
        val name = asset.optString("name")
        if (!name.startsWith(prefix) || !name.endsWith(".apk")) continue
        val code = name.removePrefix(prefix).removeSuffix(".apk").toLongOrNull() ?: continue
        if (code <= installedCode) continue
        val digest = asset.optString("digest")
        require(digest.matches(Regex("sha256:[0-9a-f]{64}")))
        return AppRelease(tag, code, json.optString("body").take(4000), digest.removePrefix("sha256:"), beta, name)
    }
    return null
}

/** From a list of releases (newest first, as GitHub returns it): the one with the highest version code. */
internal fun newestAppRelease(releases: org.json.JSONArray, installedCode: Long, allowBeta: Boolean): AppRelease? =
    (0 until releases.length()).mapNotNull { index ->
        runCatching { parseAppRelease(releases.getJSONObject(index), installedCode, allowBeta) }.getOrNull()
    }.maxByOrNull { it.code }

private fun URL.connection(): HttpsURLConnection = (openConnection() as HttpsURLConnection).apply {
    connectTimeout = 12_000
    readTimeout = 15_000
    setRequestProperty("User-Agent", "IPTVPlayer")
}

@Suppress("DEPRECATION")
private fun android.content.pm.PackageInfo.code(): Long =
    if (android.os.Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()

@Suppress("DEPRECATION")
private suspend fun checkRelease(context: Context, beta: Boolean): AppRelease? = withContext(Dispatchers.IO) {
    val installed = context.packageManager.getPackageInfo(context.packageName, 0).code()
    if (beta) {
        // "latest" never returns pre-releases; with betas switched on the newest releases are compared.
        val list = URL("https://api.github.com/repos/$repository/releases?per_page=20").connection()
        try {
            list.setRequestProperty("Accept", "application/vnd.github+json")
            if (list.responseCode == 404) return@withContext null
            val bytes = list.inputStream.use { it.readNBytesCompat(4_000_000) }
            return@withContext newestAppRelease(org.json.JSONArray(bytes.decodeToString()), installed, allowBeta = true)
        } finally {
            list.disconnect()
        }
    }
    val connection = URL("https://api.github.com/repos/$repository/releases/latest").connection()
    try {
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        if (connection.responseCode == 404) {
            val repositoryConnection = URL("https://api.github.com/repos/$repository").connection()
            try {
                check(repositoryConnection.responseCode == 200)
                return@withContext null
            } finally {
                repositoryConnection.disconnect()
            }
        }
        val bytes = connection.inputStream.use { it.readNBytesCompat(1_000_000) }
        parseAppRelease(JSONObject(bytes.decodeToString()), installed)
    } finally {
        connection.disconnect()
    }
}

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) return result.toByteArray()
        require(result.size() + count <= limit)
        result.write(buffer, 0, count)
    }
}

@Suppress("DEPRECATION")
private suspend fun downloadRelease(context: Context, release: AppRelease): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "updates").apply { mkdirs() }
    val apk = File(directory, "IPTVPlayer.apk")
    val temporary = File.createTempFile("download-", ".apk", directory)
    val connection = URL(release.downloadUrl).connection()
    try {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        connection.inputStream.use { input ->
            temporary.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= maxApkBytes)
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
            }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == release.digest)
        val manager = context.packageManager
        val installed = manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val candidate = requireNotNull(manager.getPackageArchiveInfo(temporary.path, PackageManager.GET_SIGNATURES))
        require(candidate.packageName == context.packageName && candidate.code() == release.code && candidate.code() > installed.code())
        val signers = installed.signatures.orEmpty().toSet()
        require(signers.isNotEmpty() && candidate.signatures.orEmpty().toSet() == signers)
        require(temporary.renameTo(apk))
        apk
    } finally {
        connection.disconnect()
        temporary.delete()
    }
}

/** Update state for the menu badge and the content of the "App-Updates" settings section. */
internal class AppUpdateUi(
    val available: Boolean,
    val settings: @Composable () -> Unit,
    /** For the phone remote: automatic search, manual check and a short status line. */
    val automatic: Boolean = true,
    val setAutomatic: (Boolean) -> Unit = {},
    val check: () -> Unit = {},
    val status: String = "",
    val beta: Boolean = false,
    val setBeta: (Boolean) -> Unit = {}
)

@Composable
internal fun rememberAppUpdates(): AppUpdateUi {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { context.getSharedPreferences("app_updates", Context.MODE_PRIVATE) }
    // Only searches; installing always needs the user's confirmation. Users can switch it off.
    var auto by remember { mutableStateOf(preferences.getBoolean("automatic", true)) }
    // Betas (GitHub pre-releases) only for those who switch them on; off by default.
    var beta by remember { mutableStateOf(preferences.getBoolean("beta", false)) }
    var lastCheck by remember { mutableStateOf(preferences.getLong("last_check", 0L)) }
    var result by remember { mutableStateOf<Int?>(null) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var busy by remember { mutableStateOf(false) }
    // Automatic checks only mark the update; the dialog opens on request.
    var dialogOpen by remember { mutableStateOf(false) }
    fun check(manual: Boolean) {
        if (busy) return
        busy = true
        result = null
        scope.launch {
            try {
                release = checkRelease(context, beta)
                if (manual && release != null) dialogOpen = true
                lastCheck = System.currentTimeMillis()
                preferences.edit().putLong("last_check", lastCheck).apply()
                if (release == null) result = R.string.update_current
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                result = R.string.update_check_failed
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(auto) {
        val now = System.currentTimeMillis()
        // Every start, at most once an hour.
        if (auto && now - preferences.getLong("last_check", 0L) >= 3_600_000L) {
            check(manual = false)
        }
    }
    fun install(available: AppRelease) {
        scope.launch {
            try {
                result = null
                if (!context.packageManager.canRequestPackageInstalls()) {
                    result = R.string.update_permission
                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                    return@launch
                }
                busy = true
                val apk = downloadRelease(context, available)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                result = R.string.update_install_failed
            } finally {
                busy = false
            }
        }
    }
    release?.takeIf { dialogOpen }?.let { available ->
        AlertDialog(
            onDismissRequest = { if (!busy) dialogOpen = false },
            title = { Text(stringResource(R.string.update_available, available.tag) + if (available.beta) " (Beta)" else "") },
            text = {
                // The notes scroll with the remote: Up from the button leads into them.
                Column {
                    RemoteScrollText(
                        available.notes.ifBlank { stringResource(R.string.update_confirm_hint) },
                        Modifier.weight(1f, fill = false).heightIn(max = 340.dp)
                    )
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    result?.let { Text(stringResource(it)) }
                }
            },
            confirmButton = {
                val installFocus = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
                if (!busy) {
                    MenuButton(stringResource(R.string.update_install), Modifier.focusRequester(installFocus)) { install(available) }
                    LaunchedEffect(Unit) { runCatching { installFocus.requestFocus() } }
                }
            },
            dismissButton = {
                if (!busy) MenuButton(stringResource(android.R.string.cancel), Modifier) { dialogOpen = false }
            }
        )
    }
    val lastCheckText = lastCheck.takeIf { it > 0 }?.let {
        java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.ofEpochMilli(it))
    }
    val status = listOfNotNull(
        release?.let {
            context.getString(R.string.update_available, it.tag) + (if (it.beta) " (Beta)" else "") +
                " – installieren am Gerät unter Einstellungen → App-Updates"
        },
        if (busy) "Suche läuft …" else null,
        result?.let { context.getString(it) },
        lastCheckText?.let { context.getString(R.string.update_last_check, it) } ?: context.getString(R.string.update_never_checked)
    ).joinToString("\n")
    return AppUpdateUi(
        release != null,
        automatic = auto,
        setAutomatic = { value ->
            auto = value
            preferences.edit().putBoolean("automatic", value).apply()
        },
        check = { check(manual = false) },
        beta = beta,
        setBeta = { value ->
            beta = value
            preferences.edit().putBoolean("beta", value).apply()
            release = null
            check(manual = false)
        },
        status = status,
        settings = {
        // The changes of a new version stay in their popup ("Update anzeigen").
        release?.let { available ->
            Text(stringResource(R.string.update_available, available.tag) + if (available.beta) " (Beta)" else "")
            SettingRow(stringResource(R.string.update_show)) { dialogOpen = true }
        }
        val checked = lastCheck.takeIf { it > 0 }?.let {
            java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(java.time.ZoneId.systemDefault())
                .format(java.time.Instant.ofEpochMilli(it))
        }
        SettingRow(
            stringResource(R.string.update_check) + ": " + (checked?.let { stringResource(R.string.update_last_check, it) }
                ?: stringResource(R.string.update_never_checked)),
            enabled = !busy
        ) { check(manual = true) }
        SettingRow(stringResource(R.string.update_auto), selected = auto) {
            auto = !auto
            preferences.edit().putBoolean("automatic", auto).apply()
        }
        SettingRow(stringResource(R.string.update_beta), selected = beta) {
            beta = !beta
            preferences.edit().putBoolean("beta", beta).apply()
            release = null
            check(manual = false)
        }
        Text(stringResource(R.string.update_beta_hint), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        result?.let { Text(stringResource(it)) }
    })
}
