package com.iptvplayer.app

import android.app.Application

class StreamWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.install(this)
    }
}
