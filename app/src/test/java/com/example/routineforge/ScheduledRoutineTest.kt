package com.example.routineforge

import com.example.routineforge.data.ScheduledRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduledRoutineTest {

    private fun reminder(date: LocalDate, time: String = "09:00", repeat: String = ScheduledRoutine.REPEAT_NONE) =
        ScheduledRoutine(dateEpochDay = date.toEpochDay(), timeOfDay = time, repeat = repeat)

    private val jan31 = LocalDate.of(2026, 1, 31)

    @Test
    fun oneOffFiresOnceThenIsDone() {
        val r = reminder(jan31)
        assertEquals(jan31.atTime(9, 0), r.nextOccurrence(jan31.atTime(8, 0)))
        assertNull(r.nextOccurrence(jan31.atTime(9, 0))) // strictly after
    }

    @Test
    fun dailyRollsToTomorrowAfterFiring() {
        val r = reminder(jan31, repeat = ScheduledRoutine.REPEAT_DAILY)
        assertEquals(jan31.plusDays(1).atTime(9, 0), r.nextOccurrence(jan31.atTime(9, 1)))
        // Long-past start still lands on the next upcoming day.
        val later = LocalDateTime.of(2027, 6, 10, 12, 0)
        assertEquals(LocalDateTime.of(2027, 6, 11, 9, 0), r.nextOccurrence(later))
    }

    @Test
    fun weeklyKeepsTheWeekday() {
        val r = reminder(jan31, repeat = ScheduledRoutine.REPEAT_WEEKLY)
        val next = r.nextOccurrence(jan31.atTime(10, 0))!!
        assertEquals(jan31.plusWeeks(1).atTime(9, 0), next)
        assertEquals(jan31.dayOfWeek, next.dayOfWeek)
    }

    @Test
    fun monthlyClampsShortMonthsButStaysAnchored() {
        val r = reminder(jan31, repeat = ScheduledRoutine.REPEAT_MONTHLY)
        assertEquals(LocalDate.of(2026, 2, 28).atTime(9, 0), r.nextOccurrence(jan31.atTime(10, 0)))
        assertEquals(LocalDate.of(2026, 3, 31).atTime(9, 0), r.nextOccurrence(LocalDate.of(2026, 2, 28).atTime(10, 0)))
    }

    @Test
    fun sanitizeFixesBadInput() {
        val s = reminder(jan31, time = "25:00", repeat = "HOURLY")
            .copy(soundEnabled = false, vibrationEnabled = false, durationMinutes = -5)
            .sanitized()
        assertEquals("09:00", s.timeOfDay)
        assertEquals(ScheduledRoutine.REPEAT_NONE, s.repeat)
        assertTrue("never fully silent", s.vibrationEnabled)
        assertFalse(s.soundEnabled)
        assertEquals(0, s.durationMinutes)
    }
}
