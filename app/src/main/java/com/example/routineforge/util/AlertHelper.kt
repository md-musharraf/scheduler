package com.example.routineforge.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Helper for audio beeps and tactile haptic feedback alerts during timer and routine playback.
 * Delegates haptic waveform vibration to HapticFeedbackManager for strict DRY compliance.
 */
class AlertHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playWarningTick(soundEnabled: Boolean = true, vibeEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (vibeEnabled) {
            HapticFeedbackManager.vibrateOneShot(context, 60L)
        }
    }

    fun playStepTransition(soundEnabled: Boolean = true, vibeEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (vibeEnabled) {
            HapticFeedbackManager.vibrateOneShot(context, 1000L)
        }
    }

    fun playRoutineComplete(soundEnabled: Boolean = true, vibeEnabled: Boolean = true) {
        if (soundEnabled) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (vibeEnabled) {
            HapticFeedbackManager.vibrateRoutineComplete(context)
        }
    }

    fun testVibrationPattern(patternName: String) {
        HapticFeedbackManager.vibratePattern(context, patternName, isPreReminder = false)
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
