package com.serranoie.app.wear.minus.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import logcat.logcat

class WearWatchListenerService : WearableListenerService() {

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onMessageReceived(messageEvent: MessageEvent) {
		logcat { "onMessageReceived: path=${messageEvent.path}, sourceNode=${messageEvent.sourceNodeId}" }
		if (!WearInboundMessageHandler.handles(messageEvent.path)) {
			super.onMessageReceived(messageEvent)
			return
		}
		val path = messageEvent.path
		val data = messageEvent.data
		scope.launch { WearInboundMessageHandler.handle(applicationContext, path, data) }
	}
}
