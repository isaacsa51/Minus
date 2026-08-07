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
import com.serranoie.app.minus.R
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
                startCensorTimer()
            } else if (!isNear && wasNear) {
                cancelCensorTimer()
            }
            wasNear = isNear
        }
    }

    private fun startCensorTimer() {
        censorToggleJob?.cancel()
        censorToggleJob = scope.launch {
            delay(800)
            toggleCensor()
        }
    }

    private fun cancelCensorTimer() {
        censorToggleJob?.cancel()
        censorToggleJob = null
    }

    fun toggleCensor() {
        val newState = !_isCensored.value
        _isCensored.value = newState

        val messageRes = if (newState) R.string.censor_mode_toast_enabled
        else R.string.censor_mode_toast_disabled
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()

        logcat { "Censor mode toggled: $newState (after 0.8s hold)" }
    }

    fun setCensored(enabled: Boolean) {
        if (_isCensored.value == enabled) return
        _isCensored.value = enabled
        val messageRes = if (enabled) R.string.censor_mode_toast_enabled
        else R.string.censor_mode_toast_disabled
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        logcat { "Censor mode manually set: $enabled" }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
