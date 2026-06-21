package com.serranoie.app.minus

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.serranoie.app.minus.wearsync.PhoneWearMessageListener
import dagger.hilt.android.HiltAndroidApp
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import javax.inject.Inject

@HiltAndroidApp
class MinusApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var phoneWearMessageListener: PhoneWearMessageListener

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        phoneWearMessageListener.start()

//        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
//            override fun onActivityPaused(activity: Activity) {
// 				ExtendWidgetReceiver.requestUpdateData(activity.applicationContext)
// 				MinimalWidgetReceiver.requestUpdateData(activity.applicationContext)
//            }
//        })
    }
}
