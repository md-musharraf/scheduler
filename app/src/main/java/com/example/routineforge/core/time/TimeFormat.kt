package com.example.routineforge.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object TimeFormat {
    private val TIME = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val DAY = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val DAY_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    /** "2h 10m", "45m", "0m". */
    fun duration(ms: Long): String {
        val totalMin = (ms.coerceAtLeast(0) / 60_000)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun minutes(min: Int): String = duration(min * 60_000L)

    /** "45:00" or "1:05:00", rounded up so a timer never shows 00:00 while time is left. */
    fun clock(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0) + 999) / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    }

    /** Count-up time, rounded down like a stopwatch: "00:06", "1:02:03", or "00:06.42" with [centis]. */
    fun stopwatch(ms: Long, centis: Boolean = false): String {
        val t = ms.coerceAtLeast(0)
        val totalSec = t / 1000
        val h = totalSec / 3600
        val base = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, (totalSec % 3600) / 60, totalSec % 60)
        else String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60)
        return if (centis) base + String.format(Locale.US, ".%02d", (t % 1000) / 10) else base
    }

    /** "9 AM", "12 PM". */
    fun hour(h: Int): String {
        val hh = ((h % 24) + 24) % 24
        val suffix = if (hh < 12) "AM" else "PM"
        val twelve = if (hh % 12 == 0) 12 else hh % 12
        return "$twelve $suffix"
    }

    fun time(t: LocalDateTime): String = t.format(TIME)
    fun time(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String = time(toLocal(millis, zone))
    fun day(d: LocalDate): String = d.format(DAY)
    fun dayYear(d: LocalDate): String = d.format(DAY_YEAR)
    fun monthYear(d: LocalDate): String = d.format(MONTH_YEAR)

    fun toLocal(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    fun toMillis(t: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long = t.atZone(zone).toInstant().toEpochMilli()

    /** "Today", "Yesterday", "3 days ago", or a date. */
    fun relative(millis: Long, today: LocalDate = LocalDate.now()): String {
        val d = toLocal(millis).toLocalDate()
        val days = today.toEpochDay() - d.toEpochDay()
        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days in 2..6 -> "$days days ago"
            days in 7..13 -> "1 week ago"
            days in 14..30 -> "${days / 7} weeks ago"
            else -> dayYear(d)
        }
    }
}
