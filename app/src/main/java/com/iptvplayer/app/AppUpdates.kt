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
private val releaseTag = Regex("""v\d+\.\d+\.\d+""")

internal data class AppRelease(val tag: String, val code: Long, val notes: String, val digest: String) {
    val downloadUrl get() = "https://github.com/$repository/releases/download/$tag/IPTVPlayer-$code.apk"
}

internal fun parseAppRelease(json: JSONObject, installedCode: Long): AppRelease? {
    val tag = json.optString("tag_name")
    if (!releaseTag.matches(tag) || json.optBoolean("draft") || json.optBoolean("prerelease")) return null
    val assets = json.getJSONArray("assets")
    for (index in 0 until assets.length()) {
        val asset = assets.getJSONObject(index)
        val name = asset.optString("name")
        if (!name.startsWith("IPTVPlayer-") || !name.endsWith(".apk")) continue
        val code = name.removePrefix("IPTVPlayer-").removeSuffix(".apk").toLongOrNull() ?: continue
        if (code <= installedCode) continue
        val digest = asset.optString("digest")
        require(digest.matches(Regex("sha256:[0-9a-f]{64}")))
        return AppRelease(tag, code, json.optString("body").take(4000), digest.removePrefix("sha256:"))
    }
    return null
}

private fun URL.connection(): HttpsURLConnection = (openConnection() as HttpsURLConnection).apply {
    connectTimeout = 12_000
    readTimeout = 15_000
    setRequestProperty("User-Agent", "IPTVPlayer")
}

@Suppress("DEPRECATION")
private fun android.content.pm.PackageInfo.code(): Long =
    if (android.os.Build.VERSION.SDK_INT >= 28) longVersionCode else versionCode.toLong()

@Suppress("DEPRECATION")
private suspend fun checkRelease(context: Context): AppRelease? = withContext(Dispatchers.IO) {
    val installed = context.packageManager.getPackageInfo(context.packageName, 0).code()
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

@Composable
internal fun rememberAppUpdateSettings(showDialog: Boolean): @Composable () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { context.getSharedPreferences("app_updates", Context.MODE_PRIVATE) }
    var auto by remember { mutableStateOf(preferences.getBoolean("automatic", false)) }
    var result by remember { mutableStateOf<Int?>(null) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun check() {
        if (busy) return
        busy = true
        result = null
        scope.launch {
            try {
                release = checkRelease(context)
                preferences.edit().putLong("last_check", System.currentTimeMillis()).apply()
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
        if (auto && now - preferences.getLong("last_check", 0L) >= 86_400_000L) {
            check()
        }
    }
    release?.takeIf { showDialog }?.let { available ->
        AlertDialog(
            onDismissRequest = { if (!busy) release = null },
            title = { Text(stringResource(R.string.update_available, available.tag)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(available.notes.ifBlank { stringResource(R.string.update_confirm_hint) })
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    result?.let { Text(stringResource(it)) }
                }
            },
            confirmButton = {
                if (!busy) MenuButton(stringResource(R.string.update_install), Modifier) {
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
            },
            dismissButton = {
                if (!busy) MenuButton(stringResource(android.R.string.cancel), Modifier) { release = null }
            }
        )
    }
    return {
        MenuButton(stringResource(R.string.update_check), Modifier.fillMaxWidth()) { check() }
        MenuButton(stringResource(R.string.update_auto), Modifier.fillMaxWidth(), selected = auto) {
            auto = !auto
            preferences.edit().putBoolean("automatic", auto).apply()
        }
        release?.let { Text(stringResource(R.string.update_available, it.tag)) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        result?.let { Text(stringResource(it)) }
    }
}
