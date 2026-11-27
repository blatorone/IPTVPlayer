package com.iptvplayer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.core.net.toUri
import kotlinx.coroutines.delay

/*
 * Start with the device (like TiviMate): when it is switched on and, optionally, when it wakes
 * from standby. Since Android 10 an app may only open itself from the background with the
 * permission "Display over other apps"; waking is only reported to a running app, so a small
 * foreground service listens for it while that option is on.
 */

private const val PREFERENCES = "autostart"

internal object AutoStart {
    fun onBoot(context: Context) = prefs(context).getBoolean("boot", false)
    fun onWake(context: Context) = prefs(context).getBoolean("wake", false)

    fun set(context: Context, boot: Boolean? = null, wake: Boolean? = null) {
        prefs(context).edit {
            boot?.let { putBoolean("boot", it) }
            wake?.let { putBoolean("wake", it) }
        }
        sync(context)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    /** May the app open itself from the background? */
    fun allowed(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Fire TV has no settings page for that permission (only a placeholder). */
    fun fireTv(context: Context): Boolean = context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")

    /** The system page where the permission is granted (Android TV, phones). */
    fun permissionIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Starts or stops the wake listener to match the setting. */
    fun sync(context: Context) {
        val service = Intent(context, WakeService::class.java)
        if (onWake(context)) runCatching { androidx.core.content.ContextCompat.startForegroundService(context, service) }
        else context.stopService(service)
    }

    /** Opens StreamWatch (from the background: needs the permission). */
    fun open(context: Context) {
        if (!allowed(context)) return
        val start = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        runCatching {
            context.startActivity(start.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }
}

/** Device switched on: open StreamWatch and/or start the wake listener. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return
        AutoStart.sync(context)
        if (AutoStart.onBoot(context)) AutoStart.open(context)
    }

    private companion object {
        val BOOT_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, "android.intent.action.QUICKBOOT_POWERON")
    }
}

/** Listens for the device waking from standby and then opens StreamWatch. */
class WakeService : Service() {
    private val screenOn = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON && AutoStart.onWake(context)) AutoStart.open(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.autostart_channel), NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.autostart_notification))
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(ID, notification)
        registerReceiver(screenOn, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AutoStart.onWake(this)) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOn) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL = "autostart"
        const val ID = 4_201
    }
}

/** "Starten" in Allgemein: on switch-on, on waking, and the permission it needs. */
@Composable
internal fun AutoStartSettings() {
    val context = LocalContext.current
    var boot by remember { mutableStateOf(AutoStart.onBoot(context)) }
    var wake by remember { mutableStateOf(AutoStart.onWake(context)) }
    var allowed by remember { mutableStateOf(AutoStart.allowed(context)) }
    // The permission is granted in the system settings: look again now and then.
    LaunchedEffect(Unit) {
        while (true) {
            allowed = AutoStart.allowed(context)
            delay(1_500)
        }
    }
    SettingRow(stringResource(R.string.autostart_boot), selected = boot) {
        boot = !boot
        AutoStart.set(context, boot = boot)
    }
    SettingRow(stringResource(R.string.autostart_wake), selected = wake) {
        wake = !wake
        AutoStart.set(context, wake = wake)
    }
    if ((boot || wake) && !allowed) {
        Text(stringResource(R.string.autostart_permission_missing), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
        if (AutoStart.fireTv(context)) Text(stringResource(R.string.autostart_fire_tv), style = MaterialTheme.typography.bodySmall)
        else SettingRow(stringResource(R.string.autostart_permission)) {
            runCatching { context.startActivity(AutoStart.permissionIntent(context)) }
        }
    }
}

/** The same for the phone page (section [section]). */
internal fun autoStartRemoteEntries(context: Context, section: String): List<RemoteEntry> = buildList {
    add(RemoteEntry.Toggle("autostart.boot", section, context.getString(R.string.autostart_boot),
        { AutoStart.onBoot(context) }) { on -> AutoStart.set(context, boot = on) })
    add(RemoteEntry.Toggle("autostart.wake", section, context.getString(R.string.autostart_wake),
        { AutoStart.onWake(context) }) { on -> AutoStart.set(context, wake = on) })
    if ((AutoStart.onBoot(context) || AutoStart.onWake(context)) && !AutoStart.allowed(context)) {
        add(RemoteEntry.Info("autostart.permission", section, context.getString(R.string.autostart_permission_missing),
            if (AutoStart.fireTv(context)) context.getString(R.string.autostart_fire_tv) else context.getString(R.string.autostart_permission_device)))
    }
}
