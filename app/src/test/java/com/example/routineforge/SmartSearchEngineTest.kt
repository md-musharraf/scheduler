package com.example.routineforge

import com.example.routineforge.data.Routine
import com.example.routineforge.data.RoutineCategory
import com.example.routineforge.data.RoutineStep
import com.example.routineforge.data.RoutineStorage
import com.example.routineforge.data.StepType
import com.example.routineforge.util.SmartSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSearchEngineTest {

    private val categories = RoutineStorage.DEFAULT_CATEGORIES
    private val routines = RoutineStorage.DEFAULT_ROUTINES

    @Test
    fun testExactTitleSearch() {
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "Communication Skills (30 Min)",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        assertEquals("routine_comm", results.first().id)
    }

    @Test
    fun testSemanticSynonymWorkoutMatchesGymRoutine() {
        // "workout" should match "routine_gym" via synonym mapping
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "workout",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        assertTrue(results.any { it.id == "routine_gym" })
    }

    @Test
    fun testDeepStepTitleSearch() {
        // "Bench Press" only appears in routine_gym steps, not in title
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "Bench Press",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        assertEquals("routine_gym", results.first().id)
    }

    @Test
    fun testDeepInstructionSearch() {
        // "idioms" only appears in routine_comm step 3 instruction
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "idioms",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        assertEquals("routine_comm", results.first().id)
    }

    @Test
    fun testDurationConstraintQuick() {
        // "quick" matches routines <= 30 mins, ranks 30m comm routine above 4h study routine
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "quick",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        // routine_comm is 30m, routine_study is 4 hours
        assertTrue(results.any { it.totalDurationSeconds <= 1800 })
    }

    @Test
    fun testCodingIntentMatchesSoftwareDevelopment() {
        // "coding" or "dsa" matches study & dev sprint
        val results = SmartSearchEngine.search(
            routines = routines,
            categories = categories,
            rawQuery = "dsa",
            selectedCategoryId = null
        )
        assertFalse(results.isEmpty())
        assertEquals("routine_study", results.first().id)
    }
}
