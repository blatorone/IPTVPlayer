package com.iptvplayer.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import java.io.File
import java.time.Instant
import java.util.Locale

/** Small, local-only diagnostics store. It deliberately excludes exception messages,
 * request URLs, portal data, and user content from exported reports. */
internal object AppDiagnostics {
    private const val MAX_FILE_BYTES = 512 * 1024L
    private val lock = Any()
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        synchronized(lock) {
            if (installed) return
            val app = context.applicationContext
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                runCatching { writeCrash(app, thread, error) }
                if (previous != null) previous.uncaughtException(thread, error)
                else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
            }
            installed = true
            record(app, "app_started")
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
            ?: error("Die Diagnose-Datei konnte im gewählten Ordner nicht angelegt werden.")
        writeReport(context, target)
        return target
    }

    fun exportToDocument(context: Context, target: Uri) {
        writeReport(context, target)
    }

    private fun writeReport(context: Context, target: Uri) {
        val resolver = context.contentResolver
        val events = File(context.filesDir, "streamwatch-events.log")
        val crash = File(context.filesDir, "streamwatch-crash.log")
        resolver.openOutputStream(target, "w")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
            writer.appendLine("StreamWatch Diagnosebericht")
            writer.appendLine("Zeitpunkt: ${Instant.now()}")
            writer.appendLine("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            writer.appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
            writer.appendLine("\n--- Ereignisse ---")
            if (events.exists()) writer.append(events.readText(Charsets.UTF_8))
            writer.appendLine("\n--- Letzter Absturz ---")
            if (crash.exists()) writer.append(crash.readText(Charsets.UTF_8))
            else writer.appendLine("Kein Absturzbericht vorhanden.")
        } ?: error("Die Diagnose-Datei konnte nicht geschrieben werden.")
    }

    private fun writeCrash(context: Context, thread: Thread, error: Throwable) {
        val appInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
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
                        .append(frame.methodName.take(120)).append("(")
                        .append(frame.fileName?.take(160) ?: "Unknown Source")
                        .append(':').append(frame.lineNumber).appendLine(")")
                }
                cause = cause.cause
            }
        }
        replace(File(context.filesDir, "streamwatch-crash.log"), report)
        record(context, "uncaught_exception:${error.javaClass.simpleName.lowercase(Locale.ROOT)}")
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

    private fun replace(file: File, text: String) = synchronized(lock) {
        runCatching { file.writeText(text.takeLast(MAX_FILE_BYTES.toInt()), Charsets.UTF_8) }
    }
}
