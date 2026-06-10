package com.serranoie.app.minus.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.core.net.toUri
import logcat.asLog
import logcat.logcat

object Utils {
	/**
	 * Open the web link in the browser.
	 *
	 * @param context The context
	 * @param url The URL to open
	 */
	fun openWebLink(context: Context, url: String) {
		val uri: Uri = url.toUri()
		val intent = Intent(Intent.ACTION_VIEW, uri)
		try {
			context.startActivity(intent)
		} catch (exc: ActivityNotFoundException) {
			logcat("Utils") { exc.asLog() }
		}
	}


	fun View.toggleFeedback() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			this.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
		}
	}

	fun View.weakHapticFeedback() {
		this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
	}

	fun View.strongHapticFeedback() {
		this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
	}

	fun View.confirmFeedback() {
		this.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
	}

	fun View.swipedVibration() {
		try {
			val vibrator = this.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
			vibrator?.let {
				// A smoother 10-pulse Ease-In ramp for a "mechanical lock" feel
				val pulses = 10
				val timings = LongArray(pulses * 2)
				val amplitudes = IntArray(pulses * 2)

				for (i in 0 until pulses) {
					val t = (i + 1).toFloat() / pulses
					// Quadratic ease-in for amplitude
					val amplitude = (255 * t * t).toInt().coerceIn(1, 255)
					// Gaps shrink as we reach the "peak"
					val gap = (10 * (1 - t)).toLong().coerceAtLeast(1L)
					// Pulse duration grows for a heavier finish
					val duration = (8 + 12 * t).toLong()

					timings[i * 2] = gap
					timings[i * 2 + 1] = duration
					amplitudes[i * 2] = 0
					amplitudes[i * 2 + 1] = amplitude
				}

				val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
				it.vibrate(effect)
			}
		} catch (e: Exception) {
			this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
			logcat("Utils") { e.asLog() }
		}
	}

	fun View.abortFeedback() {
		this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
	}

	fun View.errorFeedback() {
		try {
			val vibrator = this.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
			vibrator?.let {
				// Custom haptic pattern with increasing intensity (extracted from CustomHapticView)
				val numberOfPulses = 2 // Number of increasing haptic pulses
				val pulseDuration = 75L // Duration of each pulse in milliseconds
				val spaceBetweenPulses = 24L // Duration of space between pulses in milliseconds
				val maxAmplitude = 255 // Maximum amplitude for the last pulse

				val timings = LongArray(numberOfPulses * 2) // Double the size for on/off
				val amplitudes = IntArray(numberOfPulses * 2)

				for (i in 0 until numberOfPulses) {
					val amplitude =
						(maxAmplitude * (i + 1) / numberOfPulses) // Calculate increasing amplitude
					timings[i * 2] = spaceBetweenPulses // Space before the pulse
					timings[i * 2 + 1] = pulseDuration // Duration of the pulse
					amplitudes[i * 2] = 0 // Amplitude of the space
					amplitudes[i * 2 + 1] = amplitude // Amplitude of the pulse
				}

				val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
				it.vibrate(effect)
			}
		} catch (e: Exception) {
			// Fallback to basic haptic feedback if custom vibration fails
			this.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
			logcat("Utils") { e.asLog() }
		}
	}

	fun String.toToast(context: Context, length: Int = Toast.LENGTH_SHORT) {
		Toast.makeText(context, this, length).show()
	}
}
