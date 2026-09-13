package com.example.routineforge

import com.example.routineforge.data.CompletedSession
import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.RoutineStorage
import com.example.routineforge.data.StepType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineModelTest {

    @Test
    fun testRoutineStepFormattedDuration() {
        val step1 = RoutineStep(title = "Pushups", durationSeconds = 45, stepType = StepType.WORK)
        assertEquals("45s", step1.formattedDuration())

        val step2 = RoutineStep(title = "Rest", durationSeconds = 120, stepType = StepType.REST)
        assertEquals("2m", step2.formattedDuration())

        val step3 = RoutineStep(title = "Deep Study", durationSeconds = 1530, stepType = StepType.WORK)
        assertEquals("25m 30s", step3.formattedDuration())
    }

    @Test
    fun testRoutineTotalDurationCalculation() {
        val steps = listOf(
            RoutineStep(title = "Warmup", durationSeconds = 60, stepType = StepType.PREPARE),
            RoutineStep(title = "Pushups", durationSeconds = 45, stepType = StepType.WORK),
            RoutineStep(title = "Rest", durationSeconds = 15, stepType = StepType.REST),
            RoutineStep(title = "Squats", durationSeconds = 60, stepType = StepType.WORK)
        )

        val routine = Routine(
            title = "Quick HIIT",
            categoryId = "cat_gym",
            steps = steps
        )

        assertEquals(180, routine.totalDurationSeconds)
        assertEquals("3m", routine.formattedDuration)
    }

    @Test
    fun testRoutineFormattedDurationWithHours() {
        val steps = listOf(
            RoutineStep(title = "Study Session 1", durationSeconds = 3600, stepType = StepType.WORK),
            RoutineStep(title = "Break", durationSeconds = 900, stepType = StepType.REST)
        )

        val routine = Routine(
            title = "Long Study Block",
            categoryId = "cat_study",
            steps = steps
        )

        assertEquals(4500, routine.totalDurationSeconds)
        assertEquals("1h 15m", routine.formattedDuration)
    }

    @Test
    fun testDefaultCategoriesExist() {
        val categories = RoutineStorage.DEFAULT_CATEGORIES
        assertTrue(categories.isNotEmpty())

        val studyCat = categories.find { it.id == "cat_study" }
        assertNotNull(studyCat)
        assertEquals("Study", studyCat?.name)
        assertEquals("📚", studyCat?.emoji)

        val gymCat = categories.find { it.id == "cat_gym" }
        assertNotNull(gymCat)
        assertEquals("Gym & Workout", gymCat?.name)
        assertEquals("💪", gymCat?.emoji)
    }

    @Test
    fun testCompletedSessionFormatting() {
        val session = CompletedSession(
            routineId = "routine_pomodoro",
            routineTitle = "Pomodoro Study Sprint",
            categoryName = "Study",
            categoryEmoji = "📚",
            categoryColorHex = 0xFF6366F1,
            durationSeconds = 1530,
            stepsCompleted = 4,
            totalSteps = 4
        )

        assertEquals("25m 30s", session.formattedDuration())
        assertEquals(4, session.stepsCompleted)
        assertEquals(4, session.totalSteps)
    }

    @Test
    fun testStepTypeDisplayNames() {
        assertEquals("Work / Focus", StepType.WORK.displayName())
        assertEquals("Rest / Break", StepType.REST.displayName())
        assertEquals("Prepare / Warm Up", StepType.PREPARE.displayName())
    }
}
