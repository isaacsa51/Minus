package com.serranoie.app.minus.data.wearable

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class WearableService @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	suspend fun getReachableSenderNodeIds(): List<String> {
		val capability = Wearable.getCapabilityClient(context)
			.getCapability("minus_wear_sender", CapabilityClient.FILTER_REACHABLE)
			.await()
		return capability.nodes.map { it.id }
	}
}
