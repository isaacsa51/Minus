package com.serranoie.app.minus

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import com.serranoie.app.minus.wearsync.PhoneWearMessageListener
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MinusApplication: Application(), Configuration.Provider {

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
		phoneWearMessageListener.start()

		registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
			override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

			}

			override fun onActivityStarted(activity: Activity) {

			}

			override fun onActivityResumed(activity: Activity) {

			}

			override fun onActivityPaused(activity: Activity) {
//				ExtendWidgetReceiver.requestUpdateData(activity.applicationContext)
//				MinimalWidgetReceiver.requestUpdateData(activity.applicationContext)
			}

			override fun onActivityStopped(activity: Activity) {

			}

			override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

			}

			override fun onActivityDestroyed(activity: Activity) {

			}
		})
	}
}