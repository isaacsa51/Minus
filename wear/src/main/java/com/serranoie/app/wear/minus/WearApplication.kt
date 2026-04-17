package com.serranoie.app.wear.minus

import android.app.Application
import logcat.AndroidLogcatLogger
import logcat.LogPriority

class WearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)
    }
}
