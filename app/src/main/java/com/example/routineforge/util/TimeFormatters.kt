package com.example.routineforge.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

/**
 * Unified, allocation-efficient time and date formatting utilities across RoutineForge.
 * Replaces redundant inline string builders and formatters.
 */
object TimeFormatters {

    private val TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$")
    private val DAY_MONTH_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
    private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    /**
     * Validates 24-hour time strings such as "09:30", "18:00".
     */
    fun isValidTimeOfDay(timeStr: String): Boolean {
        if (timeStr.length != 5) return false
        return TIME_PATTERN.matcher(timeStr).matches()
    }

    /**
     * Formats total seconds into "MM:SS" (or "HH:MM:SS" if >= 1 hour).
     */
    fun formatSeconds(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val seconds = safeSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats minutes into concise readable badge string (e.g. "30m", "1h", "2h 30m").
     */
    fun formatDurationMinutes(minutes: Int): String {
        val safeMinutes = minutes.coerceAtLeast(1)
        val hours = safeMinutes / 60
        val remainingMin = safeMinutes % 60
        return when {
            hours > 0 && remainingMin > 0 -> "${hours}h ${remainingMin}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMin}m"
        }
    }

    /**
     * Formats duration in seconds to concise readable string (e.g. "45s", "2m", "25m 30s", "1h 15m").
     */
    fun formatDurationSeconds(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val remainingSec = safeSeconds % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 && remainingSec > 0 -> "${minutes}m ${remainingSec}s"
            minutes > 0 -> "${minutes}m"
            else -> "${remainingSec}s"
        }
    }

    /**
     * Formats LocalDate epochDay to friendly display like "Mon, Sep 14".
     */
    fun formatEpochDay(epochDay: Long): String {
        return try {
            val date = LocalDate.ofEpochDay(epochDay)
            date.format(DAY_MONTH_FORMATTER)
        } catch (_: Exception) {
            "Day $epochDay"
        }
    }

    /**
     * Formats LocalDate to Month Year header like "September 2026".
     */
    fun formatMonthYear(date: LocalDate): String {
        return try {
            date.format(MONTH_YEAR_FORMATTER)
        } catch (_: Exception) {
            date.toString()
        }
    }
}
