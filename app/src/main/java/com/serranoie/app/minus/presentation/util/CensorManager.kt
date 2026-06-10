package com.serranoie.app.minus.presentation.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CensorManager @Inject constructor(
	@ApplicationContext private val context: Context
) : SensorEventListener {

	private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
	private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

	private val _isCensored = MutableStateFlow(false)
	val isCensored: StateFlow<Boolean> = _isCensored

	private var wasNear = false
	private var censorToggleJob: Job? = null
	private val scope = CoroutineScope(Dispatchers.Main)

	fun start() {
		proximitySensor?.let {
			sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
		}
	}

	fun stop() {
		sensorManager.unregisterListener(this)
		censorToggleJob?.cancel()
		censorToggleJob = null
	}

	override fun onSensorChanged(event: SensorEvent?) {
		if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
			val distance = event.values[0]
			val isNear = distance < (proximitySensor?.maximumRange ?: 5f)

			if (isNear && !wasNear) {
				// Transition to "Near" - start the timer
				startCensorTimer()
			} else if (!isNear && wasNear) {
				// Transition to "Far" - cancel the timer
				cancelCensorTimer()
			}
			wasNear = isNear
		}
	}

	private fun startCensorTimer() {
		censorToggleJob?.cancel()
		censorToggleJob = scope.launch {
			delay(800) // Wait for 0.8 seconds
			toggleCensor()
		}
	}

	private fun cancelCensorTimer() {
		censorToggleJob?.cancel()
		censorToggleJob = null
	}

	private fun toggleCensor() {
		val newState = !_isCensored.value
		_isCensored.value = newState
		
		val message = if (newState) "Censor Mode Enabled" else "Censor Mode Disabled"
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		
		logcat { "Censor mode toggled: $newState (after 0.8s hold)" }
	}

	override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
