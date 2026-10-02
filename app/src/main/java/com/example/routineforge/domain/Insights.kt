package com.example.routineforge.domain

import androidx.compose.runtime.Immutable
import com.example.routineforge.core.time.TimeFormat
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

enum class InsightKind { PEAK_TIME, AVERAGE_SESSION, WEEK_CHANGE, TOP_SUBJECT, WEEKEND, STREAK, ACTIVE_DAYS, GOAL_GAP, TOTAL }

@Immutable
data class Insight(val kind: InsightKind, val title: String, val detail: String, val positive: Boolean = true)

/** Everything the insights need, all derived from completed FocusSessions. */
data class InsightInput(
    val today: LocalDate,
    val dayTotals: Map<LocalDate, Long>,
    val last30: FocusStats,
    val thisWeek: FocusStats,
    val lastWeek: FocusStats,
    val lifetimeSessions: Int,
    val lifetimeMs: Long,
    val subjectNames: Map<Long, String>,
    val weeklyTargetMinutes: Int,
    val minActiveMs: Long
)

/** Only states what the data supports; returns nothing when there's no data at all. */
object InsightEngine {
    const val MIN_SESSIONS_FOR_PATTERNS = 5

    fun build(i: InsightInput): List<Insight> {
        if (i.lifetimeSessions == 0) return emptyList()
        val out = ArrayList<Insight>()

        if (i.last30.sessionCount >= MIN_SESSIONS_FOR_PATTERNS) {
            i.last30.peakWindow(3)?.let { (from, to) ->
                out += Insight(InsightKind.PEAK_TIME, "You focus best between ${TimeFormat.hour(from)} and ${TimeFormat.hour(to)}.", "Based on your last 30 days")
            }
        }

        if (i.lifetimeSessions >= 3) {
            val avg = i.lifetimeMs / i.lifetimeSessions
            out += Insight(InsightKind.AVERAGE_SESSION, "Your average focus session is ${TimeFormat.duration(avg)}.", "Across ${i.lifetimeSessions} sessions")
        }

        // Compare this week so far with the same days of last week.
        val daysSoFar = i.thisWeek.range.elapsedDays(i.today)
        val lastWeekSoFar = i.lastWeek.range.days.take(daysSoFar).sumOf { i.lastWeek.byDay[it] ?: 0L }
        if (lastWeekSoFar > 0 && daysSoFar > 0) {
            val pct = ((i.thisWeek.totalMs - lastWeekSoFar) * 100.0 / lastWeekSoFar).roundToInt()
            if (pct != 0) {
                out += Insight(
                    InsightKind.WEEK_CHANGE,
                    "You focused ${abs(pct)}% ${if (pct > 0) "more" else "less"} than last week.",
                    "This week so far vs. the same days last week",
                    positive = pct > 0
                )
            }
        }

        i.thisWeek.bySubject.filterKeys { it != null }.maxByOrNull { it.value }?.let { (id, ms) ->
            val name = i.subjectNames[id]
            if (name != null && ms > 0) out += Insight(InsightKind.TOP_SUBJECT, "$name is your most studied subject this week.", "${TimeFormat.duration(ms)} so far")
        }

        val last28 = (1..28).map { i.today.minusDays(it.toLong()) }
        val (weekend, weekday) = last28.partition { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
        val weekendAvg = weekend.sumOf { i.dayTotals[it] ?: 0L } / weekend.size.toDouble()
        val weekdayAvg = weekday.sumOf { i.dayTotals[it] ?: 0L } / weekday.size.toDouble()
        if (i.last30.sessionCount >= MIN_SESSIONS_FOR_PATTERNS && weekdayAvg > 0) {
            when {
                weekendAvg < weekdayAvg * 0.6 -> out += Insight(InsightKind.WEEKEND, "Your productivity drops on weekends.", "Weekend average ${TimeFormat.duration(weekendAvg.toLong())} vs. weekday ${TimeFormat.duration(weekdayAvg.toLong())}", positive = false)
                weekendAvg > weekdayAvg * 1.2 -> out += Insight(InsightKind.WEEKEND, "You focus more on weekends.", "Weekend average ${TimeFormat.duration(weekendAvg.toLong())} vs. weekday ${TimeFormat.duration(weekdayAvg.toLong())}")
            }
        }

        val streak = FocusAnalytics.streak(i.dayTotals, i.today, i.minActiveMs)
        if (streak >= 2) {
            out += Insight(InsightKind.STREAK, "You're on a $streak-day streak.", "Longest streak: ${FocusAnalytics.longestStreak(i.dayTotals, i.minActiveMs)} days")
        }

        val month = StatsPeriod.MONTH.range(i.today)
        val activeThisMonth = month.days.count { (i.dayTotals[it] ?: 0L) >= i.minActiveMs }
        if (activeThisMonth > 0) {
            out += Insight(InsightKind.ACTIVE_DAYS, "Active on $activeThisMonth of ${month.elapsedDays(i.today)} days this month.", "A day counts after ${TimeFormat.duration(i.minActiveMs)} of focus")
        }

        if (i.weeklyTargetMinutes > 0) {
            val left = i.weeklyTargetMinutes * 60_000L - i.thisWeek.totalMs
            out += if (left > 0) Insight(InsightKind.GOAL_GAP, "Keep going! You're ${TimeFormat.duration(left)} away from your weekly goal.", "Weekly goal: ${TimeFormat.minutes(i.weeklyTargetMinutes)}")
            else Insight(InsightKind.GOAL_GAP, "Weekly goal reached. Great work!", "Weekly goal: ${TimeFormat.minutes(i.weeklyTargetMinutes)}")
        }

        out += Insight(InsightKind.TOTAL, "${TimeFormat.duration(i.lifetimeMs)} of total focus time.", "${i.lifetimeSessions} completed sessions")
        return out
    }
}
