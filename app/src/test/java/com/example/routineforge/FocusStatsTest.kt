package com.example.routineforge

import com.example.routineforge.data.FocusGoal
import com.example.routineforge.data.FocusStats
import com.example.routineforge.data.StatsRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FocusStatsTest {
    private val day = LocalDate.of(2026, 9, 30) // Wednesday
    private val math = FocusGoal(title = "Math", minutes = 120, colorHex = 0)
    private val physics = FocusGoal(title = "Physics", minutes = 60, colorHex = 0)

    @Test
    fun sessionIsSplitAcrossTheHoursItSpanned() {
        // 90 minutes ending at 10:30 → 60m in the 9 o'clock hour, 30m in the 10 o'clock hour.
        val log = FocusStats.add(emptyMap(), "Math", day.atTime(10, 30), 90 * 60)
        val base = day.toEpochDay() * 24
        assertEquals(30 * 60, log[base + 10]!!["Math"])
        assertEquals(60 * 60, log[base + 9]!!["Math"])
        // Ending exactly on the hour belongs to the previous hour.
        val onHour = FocusStats.add(emptyMap(), "Math", day.atTime(10, 0), 60)
        assertEquals(60, onHour[base + 9]!!["Math"])
    }

    @Test
    fun sessionAcrossMidnightLandsOnBothDays() {
        val log = FocusStats.add(emptyMap(), "Math", day.atTime(0, 10), 20 * 60)
        val days = FocusStats.days(log)
        assertEquals(10 * 60, days[day.toEpochDay()]!!["Math"])
        assertEquals(10 * 60, days[day.toEpochDay() - 1]!!["Math"])
    }

    @Test
    fun extraTimeOnOneGoalDoesNotCompleteAnother() {
        val goals = listOf(math, physics)
        val today = mapOf("Math" to 3 * 3600)
        assertEquals(2 * 3600, FocusStats.goalProgress(goals, today))
        assertFalse(FocusStats.goalMet(goals, today))
        assertTrue(FocusStats.goalMet(goals, mapOf("Math" to 7200, "Physics" to 3600)))
    }

    @Test
    fun streakCountsBackFromYesterdayWhileTodayIsInProgress() {
        val goals = listOf(physics)
        val done = mapOf("Physics" to 3600)
        val days = mapOf(
            day.toEpochDay() - 1 to done,
            day.toEpochDay() - 2 to done,
            day.toEpochDay() - 4 to done
        )
        assertEquals(2, FocusStats.streak(goals, days, day))
        assertEquals(3, FocusStats.streak(goals, days + (day.toEpochDay() to done), day))
    }

    @Test
    fun weekBucketsStartOnMondayAndCandlesChainOpens() {
        val log = FocusStats.add(emptyMap(), "Math", day.atTime(12, 0), 3600)
        val buckets = FocusStats.buckets(log, FocusStats.days(log), StatsRange.WEEK, day, 7200, day.atTime(12, 0))
        assertEquals(7, buckets.size)
        assertEquals("M", buckets[0].label)
        assertEquals(3600, buckets[2].seconds) // Wednesday
        assertFalse(buckets[2].future)
        assertTrue(buckets[3].future)
        val candles = FocusStats.candles(buckets, previous = 500)
        assertEquals(500, candles[0].open)
        assertEquals(3600, candles[2].close)
        assertEquals(3600, candles[3].open)
        assertEquals(12, FocusStats.buckets(log, FocusStats.days(log), StatsRange.YEAR, day, 60, day.atTime(12, 0)).size)
    }
}
