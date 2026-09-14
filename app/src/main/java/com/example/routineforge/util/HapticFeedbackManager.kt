package com.example.routineforge.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Single source of truth for all haptic vibration patterns across RoutineForge.
 * Ensures DRY, safe, and backwards-compatible vibration execution.
 */
object HapticFeedbackManager {

    const val PATTERN_NOTHING_PULSE = "NOTHING_PULSE"
    const val PATTERN_STEADY_BUZZ = "STEADY_BUZZ"
    const val PATTERN_TRIPLE_TAP = "TRIPLE_TAP"
    const val PATTERN_GENTLE_NUDGE = "GENTLE_NUDGE"

    val AVAILABLE_PATTERNS = listOf(
        PATTERN_NOTHING_PULSE,
        PATTERN_STEADY_BUZZ,
        PATTERN_TRIPLE_TAP,
        PATTERN_GENTLE_NUDGE
    )

    fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retrieves timings (ms) and amplitudes (0-255) for a given pattern.
     */
    fun getWaveform(patternName: String, isPreReminder: Boolean = false): Pair<LongArray, IntArray> {
        return when (patternName) {
            PATTERN_STEADY_BUZZ -> {
                if (isPreReminder) {
                    Pair(longArrayOf(0, 700), intArrayOf(0, 255))
                } else {
                    Pair(longArrayOf(0, 1200), intArrayOf(0, 255))
                }
            }
            PATTERN_TRIPLE_TAP -> {
                if (isPreReminder) {
                    Pair(
                        longArrayOf(0, 180, 80, 180, 80, 180),
                        intArrayOf(0, 255, 0, 255, 0, 255)
                    )
                } else {
                    Pair(
                        longArrayOf(0, 300, 100, 300, 100, 400),
                        intArrayOf(0, 255, 0, 255, 0, 255)
                    )
                }
            }
            PATTERN_GENTLE_NUDGE -> {
                if (isPreReminder) {
                    Pair(longArrayOf(0, 120, 80, 120), intArrayOf(0, 160, 0, 160))
                } else {
                    Pair(longArrayOf(0, 200, 100, 200), intArrayOf(0, 200, 0, 200))
                }
            }
            else -> { // PATTERN_NOTHING_PULSE
                if (isPreReminder) {
                    // Double pulse for pre-alarm
                    Pair(
                        longArrayOf(0, 300, 120, 300),
                        intArrayOf(0, 255, 0, 255)
                    )
                } else {
                    // Authoritative triple pulse for on-time alarm
                    Pair(
                        longArrayOf(0, 450, 150, 450, 150, 600),
                        intArrayOf(0, 255, 0, 255, 0, 255)
                    )
                }
            }
        }
    }

    /**
     * Executes a vibration pattern safely with hardware capability checks.
     */
    fun vibratePattern(context: Context, patternName: String, isPreReminder: Boolean = false) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val (timings, amplitudes) = getWaveform(patternName, isPreReminder)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            fallbackOneShot(vibrator, if (isPreReminder) 400L else 800L)
        }
    }

    /**
     * Single one-shot pulse for quick tactile feedback.
     */
    fun vibrateOneShot(context: Context, durationMs: Long = 60L) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        fallbackOneShot(vibrator, durationMs)
    }

    /**
     * Distinct celebratory haptic burst upon completing a full routine.
     */
    fun vibrateRoutineComplete(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        val timings = longArrayOf(0, 450, 150, 450, 150, 600)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            fallbackOneShot(vibrator, 1000L)
        }
    }

    private fun fallbackOneShot(vibrator: Vibrator, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }
}
