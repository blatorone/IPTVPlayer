package com.iptvplayer.app

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream
import java.time.Instant

/** Local diagnostics. No URLs, credentials, exception messages, or user content are recorded. */
internal object AppDiagnostics {
    private const val MAX_FILE_BYTES = 512 * 1024L
    private const val SETTINGS = "diagnostics_settings"
    private const val TREE_KEY = "tree_uri"
    private val lock = Any()
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            val app = context.applicationContext
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                // This path deliberately avoids the event-log lock: the crashing thread may have
                // died while another thread held it. Flush the internal copy before SAF export.
                runCatching { writeCrash(app, thread, error) }
                runCatching { exportCrashToSelectedFolder(app) }
                if (previous != null) previous.uncaughtException(thread, error)
                else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
            }
            installed = true
            record(app, "app_started")
            // Refresh the readable copy, including a crash report from the previous run.
            kotlin.concurrent.thread(name = "diagnostics-copy") { writeLocalCopy(app) }
        }
    }

    fun record(context: Context, event: String) {
        val safeEvent = event.filter { it.isLetterOrDigit() || it in " _-.:" }.take(100)
        append(File(context.applicationContext.filesDir, "streamwatch-events.log"),
            "${Instant.now()} $safeEvent\n")
    }

    fun export(context: Context, tree: Uri): Uri {
        val resolver = context.contentResolver
        val directory = DocumentsContract.getTreeDocumentId(tree)
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, directory)
        val name = "StreamWatch-Diagnose-${System.currentTimeMillis()}.txt"
        val target = DocumentsContract.createDocument(resolver, document, "text/plain", name)
            ?: error(context.getString(R.string.diagnostics_file_create_failed))
        writeReport(context, target)
        return target
    }

    fun exportToDocument(context: Context, target: Uri) = writeReport(context, target)

    /**
     * Saves the report in the public "Download" folder (Android 10+). Needs no permission and no
     * folder selection, which Fire TV does not offer. Returns the file name.
     */
    fun saveToDownloads(context: Context): String {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val name = "StreamWatch-Diagnose-${System.currentTimeMillis()}.txt"
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, name)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val target = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error(context.getString(R.string.diagnostics_file_create_failed))
        try {
            writeReport(context, target)
            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(target, values, null, null)
        } catch (error: Exception) {
            runCatching { resolver.delete(target, null, null) }
            throw error
        }
        return name
    }

    private fun exportCrashToSelectedFolder(context: Context) {
        val value = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            .getString(TREE_KEY, null) ?: return
        val tree = Uri.parse(value)
        val permission = context.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == tree && it.isWritePermission } ?: return
        @Suppress("UNUSED_VARIABLE") val retainedPermission = permission
        export(context, tree)
    }

    private fun writeReport(context: Context, target: Uri) {
        context.contentResolver.openOutputStream(target, "w")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writeReportText(context, writer)
        } ?: error(context.getString(R.string.diagnostics_file_write_failed))
    }

    /**
     * Keeps a current copy of the report in the app's own external folder
     * (Android/data/<package>/files). It needs no permission or folder selection, which
     * TV devices often cannot offer, and contains the same restricted data as an export.
     */
    fun writeLocalCopy(context: Context) {
        runCatching {
            val directory = context.getExternalFilesDir(null) ?: return
            val target = File(directory, "StreamWatch-Diagnose.txt")
            val temporary = File(directory, "StreamWatch-Diagnose.txt.tmp")
            synchronized(lock) {
                temporary.bufferedWriter(Charsets.UTF_8).use { writeReportText(context, it) }
                if (!temporary.renameTo(target)) {
                    target.delete()
                    temporary.renameTo(target)
                }
            }
        }
    }

    private fun writeReportText(context: Context, writer: java.io.Writer) {
        val events = File(context.filesDir, "streamwatch-events.log")
        val crash = File(context.filesDir, "streamwatch-crash.log")
        val crashEvent = File(context.filesDir, "streamwatch-crash-event.log")
        val version = runCatching {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        writer.appendLine("StreamWatch Diagnosebericht")
        writer.appendLine("Zeitpunkt: ${Instant.now()}")
        writer.appendLine("Version: ${version.ifBlank { "unbekannt" }}")
        writer.appendLine("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        writer.appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
        writer.appendLine("\n--- Ereignisse ---")
        if (events.exists()) writer.append(events.readText(Charsets.UTF_8))
        if (crashEvent.exists()) writer.append(crashEvent.readText(Charsets.UTF_8))
        writer.appendLine("\n--- Letzter Absturz ---")
        if (crash.exists()) writer.append(crash.readText(Charsets.UTF_8))
        else writer.appendLine("Kein Absturzbericht vorhanden.")
    }

    private fun writeCrash(context: Context, thread: Thread, error: Throwable) {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= 33) context.packageManager.getPackageInfo(
                context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)
            ).versionName else @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
        val report = buildString {
            appendLine("Zeitpunkt: ${Instant.now()}")
            appendLine("Version: ${appInfo.ifBlank { "unbekannt" }}")
            appendLine("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name.take(80)}")
            var cause: Throwable? = error
            var depth = 0
            while (cause != null && depth++ < 8) {
                appendLine("\nFehlerklasse: ${cause.javaClass.name.take(200)}")
                cause.stackTrace.take(100).forEach { frame ->
                    append("  at ").append(frame.className.take(220)).append('.')
                        .append(frame.methodName.take(120)).append('(')
                        .append(frame.fileName?.take(160) ?: "Unknown Source")
                        .append(':').append(frame.lineNumber).appendLine(")")
                }
                cause = cause.cause
            }
        }.takeLast(MAX_FILE_BYTES.toInt())
        writeDurably(File(context.filesDir, "streamwatch-crash.log"), report)
        // Use a separate lock-free append for the crash event for the same deadlock reason.
        writeDurably(File(context.filesDir, "streamwatch-crash-event.log"),
            "${Instant.now()} uncaught_exception:${error.javaClass.simpleName.take(80)}\n", append = true)
    }

    private fun append(file: File, text: String) = synchronized(lock) {
        runCatching {
            if (file.exists() && file.length() + text.length > MAX_FILE_BYTES) {
                val backup = File(file.parentFile, "${file.name}.1")
                backup.delete()
                file.renameTo(backup)
            }
            file.appendText(text, Charsets.UTF_8)
        }
    }

    private fun writeDurably(file: File, text: String, append: Boolean = false) {
        runCatching {
            FileOutputStream(file, append).use { stream ->
                stream.write(text.toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.fd.sync()
            }
        }
    }
}
