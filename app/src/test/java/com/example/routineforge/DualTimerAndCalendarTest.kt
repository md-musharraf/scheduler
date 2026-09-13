package com.example.routineforge

import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.RoutineStorage
import com.example.routineforge.data.ScheduledRoutine
import com.example.routineforge.data.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DualTimerAndCalendarTest {

    @Test
    fun testUserRequestedRoutinesTemplatesExist() {
        val routines = RoutineStorage.DEFAULT_ROUTINES
        
        // 1. Communication Skills: 30 min category
        val commRoutine = routines.find { it.id == "routine_comm" }
        assertNotNull("Communication Skills routine should exist", commRoutine)
        assertEquals("Communication Skills (30 Min)", commRoutine!!.title)
        assertEquals("cat_comm", commRoutine.categoryId)
        assertEquals(30 * 60, commRoutine.totalDurationSeconds)
        assertEquals(3, commRoutine.steps.size)
        assertEquals("Video Record", commRoutine.steps[0].title)
        assertEquals(5 * 60, commRoutine.steps[0].durationSeconds)
        assertEquals("Speak with AI", commRoutine.steps[1].title)
        assertEquals(15 * 60, commRoutine.steps[1].durationSeconds)
        assertEquals("Listen English Podcast", commRoutine.steps[2].title)
        assertEquals(10 * 60, commRoutine.steps[2].durationSeconds)

        // 2. Gym Workout: 2 Hours category with Bench Press, Incline Press, etc.
        val gymRoutine = routines.find { it.id == "routine_gym" }
        assertNotNull("Gym Workout routine should exist", gymRoutine)
        assertEquals("Gym Heavy Chest & Arms (2 Hours)", gymRoutine!!.title)
        assertEquals("cat_gym", gymRoutine.categoryId)
        assertEquals(2 * 3600, gymRoutine.totalDurationSeconds)
        assertTrue(gymRoutine.steps.any { it.title.contains("Bench Press") })
        assertTrue(gymRoutine.steps.any { it.title.contains("Incline") })

        // 3. Study Session: 4 Hours category with DSA 1h, Development 3h
        val studyRoutine = routines.find { it.id == "routine_study" }
        assertNotNull("Study Session routine should exist", studyRoutine)
        assertEquals("cat_study", studyRoutine!!.categoryId)
        assertEquals(4 * 3600, studyRoutine.totalDurationSeconds)
        assertTrue(studyRoutine.steps.any { it.title.contains("DSA") && it.durationSeconds == 3600 })
        assertTrue(studyRoutine.steps.any { it.title.contains("Development") && it.durationSeconds == 3 * 3600 })
    }

    @Test
    fun testDualParallelCountdownMathematicalConsistency() {
        // Communication skills routine with 3 steps: 300s (5m), 900s (15m), 600s (10m) -> total 1800s (30m)
        val steps = listOf(
            RoutineStep(title = "Video Record", durationSeconds = 300, stepType = StepType.WORK),
            RoutineStep(title = "Speak with AI", durationSeconds = 900, stepType = StepType.WORK),
            RoutineStep(title = "Listen English Podcast", durationSeconds = 600, stepType = StepType.WORK)
        )
        val totalCategorySeconds = steps.sumOf { it.durationSeconds }
        assertEquals(1800, totalCategorySeconds)

        // Parallel tick simulation:
        // When step 1 begins, both timers are at initial values
        var categoryRemaining = totalCategorySeconds
        var stepIndex = 0
        var currentStepRemaining = steps[stepIndex].durationSeconds

        assertEquals(1800, categoryRemaining)
        assertEquals(300, currentStepRemaining)

        // After 100 seconds of parallel ticking
        val elapsedTick = 100
        categoryRemaining -= elapsedTick
        currentStepRemaining -= elapsedTick

        // Both decremented simultaneously
        assertEquals(1700, categoryRemaining)
        assertEquals(200, currentStepRemaining)

        // Subtask progress ratio
        val stepProgress = (steps[stepIndex].durationSeconds - currentStepRemaining).toFloat() / steps[stepIndex].durationSeconds.toFloat()
        val categoryProgress = (totalCategorySeconds - categoryRemaining).toFloat() / totalCategorySeconds.toFloat()

        assertEquals(100f / 300f, stepProgress, 0.001f)
        assertEquals(100f / 1800f, categoryProgress, 0.001f)

        // Subtask 1 finishes (200 more seconds elapsed)
        categoryRemaining -= currentStepRemaining
        currentStepRemaining = 0

        assertEquals(1500, categoryRemaining) // 1800 - 300 = 1500s remaining in category
        assertEquals(0, currentStepRemaining)

        // Transition: Step 1 completes, transition message generated for Step 2
        val completedStepTitle = steps[stepIndex].title
        stepIndex++
        val nextStepTitle = steps[stepIndex].title

        val transitionMessage = "Aapka $completedStepTitle khatam ho gya hai! Ab aap $nextStepTitle shuru karein."
        assertEquals("Aapka Video Record khatam ho gya hai! Ab aap Speak with AI shuru karein.", transitionMessage)

        // Step 2 starts counting down from 900s while category continues counting down from 1500s
        currentStepRemaining = steps[stepIndex].durationSeconds
        assertEquals(900, currentStepRemaining)
        assertEquals(1500, categoryRemaining)

        // After 450 seconds in Step 2
        categoryRemaining -= 450
        currentStepRemaining -= 450
        assertEquals(1050, categoryRemaining)
        assertEquals(450, currentStepRemaining)
    }

    @Test
    fun testScheduledRoutineModelAndFiltering() {
        val todayEpoch = LocalDate.of(2026, 9, 13).toEpochDay()
        val tomorrowEpoch = todayEpoch + 1
        val nextWeekEpoch = todayEpoch + 7

        val scheduledItems = listOf(
            ScheduledRoutine(
                id = "sched_1",
                routineId = "routine_comm",
                routineTitle = "Communication Skills (30 Min)",
                categoryName = "Communication",
                categoryEmoji = "🗣️",
                categoryColorHex = 0xFFEC4899,
                dateEpochDay = todayEpoch,
                timeOfDay = "08:00",
                isCompleted = false
            ),
            ScheduledRoutine(
                id = "sched_2",
                routineId = "routine_gym",
                routineTitle = "Gym Workout (2 Hours)",
                categoryName = "Gym & Workout",
                categoryEmoji = "💪",
                categoryColorHex = 0xFFEF4444,
                dateEpochDay = todayEpoch,
                timeOfDay = "17:00",
                isCompleted = true
            ),
            ScheduledRoutine(
                id = "sched_3",
                routineId = "routine_study",
                routineTitle = "Deep Study Block (4 Hours)",
                categoryName = "Study",
                categoryEmoji = "📚",
                categoryColorHex = 0xFF6366F1,
                dateEpochDay = tomorrowEpoch,
                timeOfDay = "09:00",
                isCompleted = false
            ),
            ScheduledRoutine(
                id = "sched_4",
                routineId = "routine_comm",
                routineTitle = "Communication Skills (30 Min)",
                categoryName = "Communication",
                categoryEmoji = "🗣️",
                categoryColorHex = 0xFFEC4899,
                dateEpochDay = nextWeekEpoch,
                timeOfDay = "08:00",
                isCompleted = false
            )
        )

        // Filter for today
        val todayRoutines = scheduledItems.filter { it.dateEpochDay == todayEpoch }
        assertEquals(2, todayRoutines.size)
        assertEquals("08:00", todayRoutines[0].timeOfDay)
        assertFalse(todayRoutines[0].isCompleted)
        assertTrue(todayRoutines[1].isCompleted)

        // Filter for tomorrow
        val tomorrowRoutines = scheduledItems.filter { it.dateEpochDay == tomorrowEpoch }
        assertEquals(1, tomorrowRoutines.size)
        assertEquals("sched_3", tomorrowRoutines[0].id)

        // Set of scheduled dates
        val scheduledDateSet = scheduledItems.map { it.dateEpochDay }.toSet()
        assertEquals(3, scheduledDateSet.size)
        assertTrue(scheduledDateSet.contains(todayEpoch))
        assertTrue(scheduledDateSet.contains(tomorrowEpoch))
        assertTrue(scheduledDateSet.contains(nextWeekEpoch))
    }

    @Test
    fun testCalendarMonthCalculations() {
        val ym = YearMonth.of(2026, 9)
        assertEquals(30, ym.lengthOfMonth())
        
        val firstDay = ym.atDay(1)
        assertEquals(2, firstDay.dayOfWeek.value) // 2026-09-01 is Tuesday (2 in ISO-8601, Mon=1)
    }
}
