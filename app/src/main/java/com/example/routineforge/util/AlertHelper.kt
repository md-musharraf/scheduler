package com.example.routineforge.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlertHelper(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
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
            vibrate(60)
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Full 1-second (1000ms) authoritative haptic vibration for task transitions
                    val effect = VibrationEffect.createOneShot(1000L, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(1000L)
                }
            } catch (e: Exception) {
                vibrate(1000L)
            }
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 1200ms celebratory haptic pattern (two 500ms pulses with 200ms rest)
                    val timings = longArrayOf(0, 500, 200, 500)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
                }
            } catch (e: Exception) {
                vibrate(1000L)
            }
        }
    }


    fun testVibrationPattern(patternName: String) {
        try {
            val (timings, amplitudes) = when (patternName) {
                "STEADY_BUZZ" -> Pair(longArrayOf(0, 600), intArrayOf(0, 255))
                "TRIPLE_TAP" -> Pair(
                    longArrayOf(0, 180, 80, 180, 80, 180),
                    intArrayOf(0, 255, 0, 255, 0, 255)
                )
                else -> Pair(
                    longArrayOf(0, 300, 120, 300),
                    intArrayOf(0, 255, 0, 255)
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            vibrate(400L)
        }
    }

    private fun vibrate(milliseconds: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
