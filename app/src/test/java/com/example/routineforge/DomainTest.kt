package com.example.routineforge

import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.Goal
import com.example.routineforge.core.database.GoalPeriod
import com.example.routineforge.core.database.RepeatRule
import com.example.routineforge.core.database.SessionSlice
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.data.CheckItem
import com.example.routineforge.data.ChecklistBlock
import com.example.routineforge.data.DrawingBlock
import com.example.routineforge.data.DrawingStrokes
import com.example.routineforge.data.HtmlText
import com.example.routineforge.data.NoteContent
import com.example.routineforge.data.Stroke
import com.example.routineforge.data.TextBlock
import com.example.routineforge.domain.DateRange
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.InsightEngine
import com.example.routineforge.domain.InsightInput
import com.example.routineforge.domain.InsightKind
import com.example.routineforge.domain.Recurrence
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.domain.StopwatchState
import com.example.routineforge.domain.TimerMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

private const val MIN = 60_000L
private val UTC: ZoneId = ZoneOffset.UTC

private fun at(s: String) = LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli()
private fun slice(start: String, end: String, actualMin: Long, subject: Long? = 1) = SessionSlice(subject, at(start), at(end), actualMin * MIN)

class TimerMathTest {
    private fun running(planned: Long, banked: Long, segStart: Long) = FocusSession(
        id = "s", subjectId = 1, sessionType = SessionType.FOCUS, status = SessionStatus.RUNNING,
        plannedDurationMs = planned, actualDurationMs = banked, startTime = 0, segmentStartedAt = segStart, createdAt = 0, updatedAt = 0
    )

    @Test fun pausedTimeNeverCounts() {
        // 10:00 start, pause 10:20 (20 min banked), resume 10:35, stop 10:45 → 30 min focused.
        val s = running(45 * MIN, 20 * MIN, at("2026-09-30T10:35:00"))
        assertEquals(30 * MIN, TimerMath.elapsed(s, at("2026-09-30T10:45:00")))
        assertEquals(15 * MIN, TimerMath.remaining(s, at("2026-09-30T10:45:00")))
        // Ends when the remaining 25 minutes run out: 10:35 + 25 = 11:00, not 10:45.
        assertEquals(at("2026-09-30T11:00:00"), TimerMath.endsAt(s))
    }

    @Test fun pausedSessionDoesNotAdvance() {
        val s = running(45 * MIN, 20 * MIN, 0).copy(status = SessionStatus.PAUSED, segmentStartedAt = null)
        assertEquals(20 * MIN, TimerMath.elapsed(s, at("2026-09-30T23:00:00")))
        assertNull(TimerMath.endsAt(s))
        assertFalse(TimerMath.isDue(s, Long.MAX_VALUE / 2))
    }

    @Test fun dueOnlyWhenTimeIsUp() {
        val s = running(25 * MIN, 0, at("2026-09-30T10:00:00"))
        assertFalse(TimerMath.isDue(s, at("2026-09-30T10:24:59")))
        assertTrue(TimerMath.isDue(s, at("2026-09-30T10:25:00")))
        assertEquals(0L, TimerMath.remaining(s, at("2026-09-30T12:00:00")))
        assertEquals(1f, TimerMath.fraction(s, at("2026-09-30T12:00:00")))
    }

    @Test fun longBreakEveryFourth() {
        assertEquals(SessionType.SHORT_BREAK, TimerMath.nextType(SessionType.FOCUS, 3, 4))
        assertEquals(SessionType.LONG_BREAK, TimerMath.nextType(SessionType.FOCUS, 4, 4))
        assertEquals(SessionType.FOCUS, TimerMath.nextType(SessionType.LONG_BREAK, 4, 4))
    }

    @Test fun tickLandsWhereTheClockChanges() {
        assertEquals(400L, TimerMath.msToNextSecond(25 * MIN - 600))
        assertEquals("24:59", TimeFormat.clock(25 * MIN - 600 - TimerMath.msToNextSecond(25 * MIN - 600)))
        assertEquals(1000L, TimerMath.msToNextSecond(5_000))
    }

    @Test fun stopwatchCountsOnlyRunningTimeAndSplitsLaps() {
        val sw = StopwatchState().start(0).lap(10_000).pause(25_000).start(100_000).lap(105_000)
        assertEquals(30_000L, sw.elapsed(105_000))
        assertEquals(listOf(10_000L, 20_000L), sw.lapLengths)
        assertEquals(30_000L, sw.pause(105_000).elapsed(999_999))
        assertTrue(StopwatchState().isIdle)
        assertEquals("00:06.42", TimeFormat.stopwatch(6_429, centis = true))
        assertEquals("1:02:03", TimeFormat.stopwatch(3_723_999))
    }

    @Test fun clockRoundsUpSoTimeLeftNeverShowsZero() {
        assertEquals("45:00", TimeFormat.clock(45 * MIN))
        assertEquals("00:01", TimeFormat.clock(1))
        assertEquals("1:05:00", TimeFormat.clock(65 * MIN))
        assertEquals("2h 10m", TimeFormat.duration(130 * MIN))
        assertEquals("45m", TimeFormat.duration(45 * MIN))
    }
}

class FocusAnalyticsTest {
    @Test fun oneSessionFeedsEveryPeriod() {
        // A single 45-minute DSA session shows up in its day, week, month and year.
        val s = listOf(slice("2026-09-29T10:00:00", "2026-09-29T10:45:00", 45))
        val day = LocalDate.parse("2026-09-29")
        for (p in StatsPeriod.entries) {
            val st = FocusAnalytics.stats(s, p.range(day), UTC)
            assertEquals("$p total", 45 * MIN, st.totalMs)
            assertEquals("$p subject", 45 * MIN, st.bySubject[1L])
            assertEquals(1, st.sessionCount)
        }
        assertEquals(0L, FocusAnalytics.stats(s, StatsPeriod.DAY.range(day.plusDays(1)), UTC).totalMs)
    }

    @Test fun sessionAcrossMidnightIsSplitByDayAndHour() {
        val s = listOf(slice("2026-09-29T23:30:00", "2026-09-30T00:30:00", 60))
        val d1 = FocusAnalytics.stats(s, StatsPeriod.DAY.range(LocalDate.parse("2026-09-29")), UTC)
        val d2 = FocusAnalytics.stats(s, StatsPeriod.DAY.range(LocalDate.parse("2026-09-30")), UTC)
        assertEquals(30 * MIN, d1.totalMs)
        assertEquals(30 * MIN, d2.totalMs)
        assertEquals(30 * MIN, d1.byHour[23])
        assertEquals(30 * MIN, d2.byHour[0])
        // Nothing lost or double counted across the split.
        assertEquals(60 * MIN, FocusAnalytics.stats(s, StatsPeriod.WEEK.range(LocalDate.parse("2026-09-30")), UTC).totalMs)
    }

    @Test fun pausedSessionSpreadsOnlyFocusedTime() {
        // 60 wall-clock minutes but only 30 focused.
        val s = listOf(slice("2026-09-29T10:00:00", "2026-09-29T11:00:00", 30))
        assertEquals(30 * MIN, FocusAnalytics.stats(s, StatsPeriod.DAY.range(LocalDate.parse("2026-09-29")), UTC).totalMs)
    }

    @Test fun weekBucketsMondayToSunday() {
        val range = StatsPeriod.WEEK.range(LocalDate.parse("2026-09-30")) // Wednesday
        assertEquals(LocalDate.parse("2026-09-28"), range.start)
        assertEquals(7, range.dayCount)
        val s = listOf(slice("2026-09-28T09:00:00", "2026-09-28T10:00:00", 60), slice("2026-10-04T09:00:00", "2026-10-04T09:30:00", 30))
        val st = FocusAnalytics.stats(s, range, UTC)
        assertEquals(60 * MIN, st.byDay[LocalDate.parse("2026-09-28")])
        assertEquals(30 * MIN, st.byDay[LocalDate.parse("2026-10-04")])
        assertEquals(9 to 10, st.peakWindow(1))
        assertEquals(9 to 10, st.peakWindow(3))
    }

    @Test fun streakCountsOnlyRealFocusAboveThreshold() {
        val today = LocalDate.parse("2026-09-29")
        val totals = mapOf(
            today to 20 * MIN,
            today.minusDays(1) to 16 * MIN,
            today.minusDays(2) to 15 * MIN,
            today.minusDays(3) to 5 * MIN, // below 15 min: breaks the streak
            today.minusDays(4) to 90 * MIN
        )
        assertEquals(3, FocusAnalytics.streak(totals, today, 15 * MIN))
        // Today not active yet: the streak up to yesterday still stands.
        assertEquals(2, FocusAnalytics.streak(totals - today, today, 15 * MIN))
        assertEquals(0, FocusAnalytics.streak(emptyMap(), today, 15 * MIN))
        assertEquals(3, FocusAnalytics.longestStreak(totals, 15 * MIN))
    }

    @Test fun progressIsCappedAt100Percent() {
        assertEquals(1f, FocusAnalytics.progress(135 * MIN, 100))
        assertEquals(0.5f, FocusAnalytics.progress(30 * MIN, 60))
        assertEquals(0f, FocusAnalytics.progress(30 * MIN, 0))
    }

    @Test fun subjectTargetsFallBackToDailyTimesDays() {
        val s = Subject(name = "DSA", dailyTargetMinutes = 120, weeklyTargetMinutes = 0, monthlyTargetMinutes = 3000)
        val sept = StatsPeriod.MONTH.range(LocalDate.parse("2026-09-10"))
        assertEquals(840, FocusAnalytics.subjectTarget(s, StatsPeriod.WEEK, StatsPeriod.WEEK.range(LocalDate.now())))
        assertEquals(3000, FocusAnalytics.subjectTarget(s, StatsPeriod.MONTH, sept))
        assertEquals(120 * 365, FocusAnalytics.subjectTarget(s, StatsPeriod.YEAR, StatsPeriod.YEAR.range(LocalDate.parse("2026-01-01"))))
        // An explicit goal wins; archived subjects don't count.
        val daily = mapOf(GoalPeriod.DAILY to Goal(period = GoalPeriod.DAILY, targetMinutes = 360))
        val week = StatsPeriod.WEEK.range(LocalDate.now())
        assertEquals(360, FocusAnalytics.overallTarget(daily, listOf(s), StatsPeriod.DAY, StatsPeriod.DAY.range(LocalDate.now())))
        // No weekly goal: the daily goal × 7 wins over the subjects' sum.
        assertEquals(360 * 7, FocusAnalytics.overallTarget(daily, listOf(s), StatsPeriod.WEEK, week))
        val weekly = daily + (GoalPeriod.WEEKLY to Goal(period = GoalPeriod.WEEKLY, targetMinutes = 600))
        assertEquals(600, FocusAnalytics.overallTarget(weekly, listOf(s), StatsPeriod.WEEK, week))
        assertEquals(840, FocusAnalytics.overallTarget(emptyMap(), listOf(s), StatsPeriod.WEEK, week))
        assertEquals(0, FocusAnalytics.overallTarget(emptyMap(), listOf(s.copy(isArchived = true)), StatsPeriod.DAY, StatsPeriod.DAY.range(LocalDate.now())))
    }

    @Test fun rangeElapsedDays() {
        val week = StatsPeriod.WEEK.range(LocalDate.parse("2026-09-30"))
        assertEquals(3, week.elapsedDays(LocalDate.parse("2026-09-30")))
        assertEquals(7, week.elapsedDays(LocalDate.parse("2026-12-01")))
        assertEquals(0, week.elapsedDays(LocalDate.parse("2026-01-01")))
    }
}

class RecurrenceTest {
    private val start = LocalDateTime.parse("2026-01-31T09:00:00")

    @Test fun oneOffFiresOnce() {
        assertEquals(start, Recurrence.next(RepeatRule.NONE, start, start.minusMinutes(1)))
        assertNull(Recurrence.next(RepeatRule.NONE, start, start))
    }

    @Test fun repeatsStepFromTheAnchor() {
        assertEquals(LocalDateTime.parse("2026-02-01T09:00:00"), Recurrence.next(RepeatRule.DAILY, start, start))
        assertEquals(LocalDateTime.parse("2026-02-07T09:00:00"), Recurrence.next(RepeatRule.WEEKLY, start, start))
        // Monthly on the 31st: Feb clamps to the 28th, March returns to the 31st.
        assertEquals(LocalDateTime.parse("2026-02-28T09:00:00"), Recurrence.next(RepeatRule.MONTHLY, start, start))
        assertEquals(LocalDateTime.parse("2026-03-31T09:00:00"), Recurrence.next(RepeatRule.MONTHLY, start, LocalDateTime.parse("2026-02-28T09:00:00")))
        // Far in the future without looping from the anchor forever.
        assertEquals(LocalDateTime.parse("2030-06-02T09:00:00"), Recurrence.next(RepeatRule.DAILY, start, LocalDateTime.parse("2030-06-01T12:00:00")))
    }

    @Test fun occurrencesInAWindow() {
        val weekOf = LocalDate.parse("2026-02-02").atStartOfDay()
        val daily = Recurrence.between(RepeatRule.DAILY, start, 60, weekOf, weekOf.plusDays(7))
        assertEquals(7, daily.size)
        assertEquals(LocalDateTime.parse("2026-02-02T09:00:00"), daily.first())
        assertTrue(Recurrence.between(RepeatRule.NONE, start, 60, weekOf, weekOf.plusDays(7)).isEmpty())
        // An event overlapping the window start is included.
        val late = LocalDateTime.parse("2026-02-01T23:30:00")
        assertEquals(listOf(late), Recurrence.between(RepeatRule.NONE, late, 60, weekOf, weekOf.plusDays(1)))
    }
}

class InsightsTest {
    private fun input(sessions: List<SessionSlice>, today: LocalDate = LocalDate.parse("2026-09-30")): InsightInput {
        val week = StatsPeriod.WEEK.range(today)
        return InsightInput(
            today = today,
            dayTotals = FocusAnalytics.dayTotals(sessions, UTC),
            last30 = FocusAnalytics.stats(sessions, DateRange(today.minusDays(29), today.plusDays(1)), UTC),
            thisWeek = FocusAnalytics.stats(sessions, week, UTC),
            lastWeek = FocusAnalytics.stats(sessions, StatsPeriod.WEEK.range(today.minusWeeks(1)), UTC),
            lifetimeSessions = sessions.size,
            lifetimeMs = sessions.sumOf { it.actualDurationMs },
            subjectNames = mapOf(1L to "DSA"),
            weeklyTargetMinutes = 600,
            minActiveMs = 15 * MIN
        )
    }

    @Test fun noDataNoInsights() {
        assertTrue(InsightEngine.build(input(emptyList())).isEmpty())
    }

    @Test fun patternsNeedEnoughSessions() {
        val few = listOf(slice("2026-09-29T09:00:00", "2026-09-29T10:00:00", 60))
        assertTrue(InsightEngine.build(input(few)).none { it.kind == InsightKind.PEAK_TIME })
        val many = (4..9).map { d -> slice("2026-09-2${d}T09:00:00", "2026-09-2${d}T10:00:00", 60) }
        val insights = InsightEngine.build(input(many))
        val peak = insights.first { it.kind == InsightKind.PEAK_TIME }
        assertEquals("You focus best between 9 AM and 10 AM.", peak.title)
        assertTrue(insights.any { it.kind == InsightKind.TOP_SUBJECT && it.title.startsWith("DSA") })
        assertTrue(insights.any { it.kind == InsightKind.STREAK })
    }
}

class NoteContentTest {
    @Test fun blocksRoundTrip() {
        val blocks = listOf(
            TextBlock(html = "<p><b>Binary</b> Tree</p>"),
            ChecklistBlock(items = listOf(CheckItem("Inorder", true), CheckItem("Preorder"))),
            DrawingBlock(drawingId = 7)
        )
        assertEquals(blocks, NoteContent.decode(NoteContent.encode(blocks)))
        assertEquals("Binary Tree\n☑ Inorder\n☐ Preorder", NoteContent.plainText(blocks))
        assertEquals(1 to 2, NoteContent.checklistProgress(blocks))
    }

    @Test fun corruptContentIsKeptAsText() {
        val blocks = NoteContent.decode("just some old text")
        assertEquals(1, blocks.size)
        assertEquals("just some old text", NoteContent.plainText(blocks))
    }

    @Test fun htmlHelpers() {
        assertEquals("a < b & c\nnext", HtmlText.toPlain("<p>a &lt; b &amp; c</p><p>next</p>"))
        assertEquals("<p>x &lt;y&gt;</p>", HtmlText.fromPlain("x <y>"))
    }

    @Test fun strokesRoundTrip() {
        val strokes = listOf(Stroke(0xFF000000, 0.01f, listOf(0.1f, 0.2f, 0.3f, 0.4f)))
        assertEquals(strokes, DrawingStrokes.decode(DrawingStrokes.encode(strokes)))
        assertTrue(DrawingStrokes.decode("not json").isEmpty())
    }
}
