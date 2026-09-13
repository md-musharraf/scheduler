package com.example.routineforge.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class RoutineStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("routine_forge_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CATEGORIES = "categories_json"
        private const val KEY_ROUTINES = "routines_json"
        private const val KEY_SESSIONS = "sessions_json"
        private const val KEY_SCHEDULED = "scheduled_routines_json"
        private const val KEY_INITIALIZED = "is_initialized_v2"
        private const val KEY_THEME_MODE = "theme_mode_key"

        val DEFAULT_CATEGORIES = listOf(
            RoutineCategory("cat_comm", "Communication Skills", "🎙️", 0xFF8B5CF6),
            RoutineCategory("cat_gym", "Gym & Workout", "💪", 0xFFEF4444),
            RoutineCategory("cat_study", "Study", "📚", 0xFF6366F1),
            RoutineCategory("cat_mindfulness", "Mindfulness", "🧘", 0xFF10B981),
            RoutineCategory("cat_productivity", "Productivity", "⚡", 0xFFF59E0B)
        )

        val DEFAULT_ROUTINES = listOf(
            Routine(
                id = "routine_comm",
                title = "Communication Skills (30 Min)",
                description = "Daily fluency workout: Video record yourself, speak with AI, and immerse in podcast.",
                categoryId = "cat_comm",
                colorHex = 0xFF8B5CF6,
                iconName = "record_voice_over",
                steps = listOf(
                    RoutineStep(
                        title = "Video Record",
                        durationSeconds = 5 * 60,
                        stepType = StepType.WORK,
                        instruction = "Record yourself speaking on camera for 5 minutes. Notice body language & eye contact."
                    ),
                    RoutineStep(
                        title = "Speak with AI",
                        durationSeconds = 15 * 60,
                        stepType = StepType.WORK,
                        instruction = "Interactive conversational practice with AI on diverse topics."
                    ),
                    RoutineStep(
                        title = "Listen English Podcast",
                        durationSeconds = 10 * 60,
                        stepType = StepType.WORK,
                        instruction = "Active listening to native English podcast; observe pronunciation and idioms."
                    )
                )
            ),
            Routine(
                id = "routine_gym",
                title = "Gym Heavy Chest & Arms (2 Hours)",
                description = "Complete 2-hour hypertrophic gym routine with compound lifts and isolation.",
                categoryId = "cat_gym",
                colorHex = 0xFFEF4444,
                iconName = "fitness_center",
                steps = listOf(
                    RoutineStep(
                        title = "Dynamic Warmup & Rotator Cuff",
                        durationSeconds = 10 * 60,
                        stepType = StepType.PREPARE,
                        instruction = "Light cardio, arm circles, and resistance band warmups."
                    ),
                    RoutineStep(
                        title = "Barbell Bench Press",
                        durationSeconds = 25 * 60,
                        stepType = StepType.WORK,
                        instruction = "Warmup ramp up then 4 heavy working sets. Focus on arch and leg drive."
                    ),
                    RoutineStep(
                        title = "Incline Dumbbell Press",
                        durationSeconds = 25 * 60,
                        stepType = StepType.WORK,
                        instruction = "Upper chest isolation, 4 working sets of 8-12 reps."
                    ),
                    RoutineStep(
                        title = "Cable Chest Flyes & Deep Stretch",
                        durationSeconds = 20 * 60,
                        stepType = StepType.WORK,
                        instruction = "High-to-low and mid flyes for maximum chest stretch & pump."
                    ),
                    RoutineStep(
                        title = "Triceps Dips & Rope Pushdowns",
                        durationSeconds = 25 * 60,
                        stepType = StepType.WORK,
                        instruction = "Heavy weighted or bodyweight dips superset with rope pushdowns."
                    ),
                    RoutineStep(
                        title = "Cooldown & Stretching",
                        durationSeconds = 15 * 60,
                        stepType = StepType.REST,
                        instruction = "Full upper body stretch and recovery breathing."
                    )
                )
            ),
            Routine(
                id = "routine_study",
                title = "Study & Development Sprint (4 Hours)",
                description = "Intensive 4-hour mastery: 1 hour algorithms and 3 hours full-stack software development.",
                categoryId = "cat_study",
                colorHex = 0xFF06B6D4,
                iconName = "code",
                steps = listOf(
                    RoutineStep(
                        title = "DSA - Data Structures & Algorithms",
                        durationSeconds = 60 * 60,
                        stepType = StepType.WORK,
                        instruction = "Solve 2 Medium or 1 Hard problem. Focus on time and space complexity."
                    ),
                    RoutineStep(
                        title = "Software Development Deep Dive",
                        durationSeconds = 180 * 60,
                        stepType = StepType.WORK,
                        instruction = "Write clean architecture code, build features, and write unit tests."
                    )
                )
            ),

            Routine(
                id = "routine_hiit",
                title = "Full Body HIIT Circuit",
                description = "High intensity workout with timed work and short rest intervals.",
                categoryId = "cat_gym",
                colorHex = 0xFFEF4444,
                iconName = "fitness_center",
                steps = listOf(
                    RoutineStep(
                        title = "Dynamic Warmup",
                        durationSeconds = 60,
                        stepType = StepType.PREPARE,
                        instruction = "Jumping jacks, arm circles, and light high knees."
                    ),
                    RoutineStep(
                        title = "Explosive Push-ups",
                        durationSeconds = 45,
                        stepType = StepType.WORK,
                        instruction = "Keep core tight, full range of motion."
                    ),
                    RoutineStep(
                        title = "Rest & Breathe",
                        durationSeconds = 20,
                        stepType = StepType.REST,
                        instruction = "Deep inhales through nose, slow exhales."
                    ),
                    RoutineStep(
                        title = "Air Squats & Pulse",
                        durationSeconds = 45,
                        stepType = StepType.WORK,
                        instruction = "Chest up, weight on heels, push through glutes."
                    ),
                    RoutineStep(
                        title = "Rest & Shake Legs",
                        durationSeconds = 20,
                        stepType = StepType.REST,
                        instruction = "Shake out the lactic acid, get ready for core."
                    ),
                    RoutineStep(
                        title = "High Plank Hold",
                        durationSeconds = 45,
                        stepType = StepType.WORK,
                        instruction = "Squeeze abs, glutes, and shoulders solid as a rock."
                    ),
                    RoutineStep(
                        title = "Mountain Climbers",
                        durationSeconds = 45,
                        stepType = StepType.WORK,
                        instruction = "Drive knees up swiftly with controlled cadence."
                    ),
                    RoutineStep(
                        title = "Cooldown & Quad Stretch",
                        durationSeconds = 60,
                        stepType = StepType.REST,
                        instruction = "Gentle stretching and heart rate lowering."
                    )
                )
            ),
            Routine(
                id = "routine_coding",
                title = "LeetCode & Dev Deep Dive",
                description = "Structured problem solving and algorithmic reasoning session.",
                categoryId = "cat_coding",
                colorHex = 0xFF06B6D4,
                iconName = "code",
                steps = listOf(
                    RoutineStep(
                        title = "Problem Breakdown & Invariants",
                        durationSeconds = 10 * 60,
                        stepType = StepType.WORK,
                        instruction = "Understand constraints, identify edge cases, write pseudocode."
                    ),
                    RoutineStep(
                        title = "Focused Code Implementation",
                        durationSeconds = 30 * 60,
                        stepType = StepType.WORK,
                        instruction = "Write clean, modular code with optimal time/space complexity."
                    ),
                    RoutineStep(
                        title = "Eye Rest & Hydration",
                        durationSeconds = 5 * 60,
                        stepType = StepType.REST,
                        instruction = "Look 20 feet away to relax eye muscles, grab water."
                    ),
                    RoutineStep(
                        title = "Test Cases, Profiling & Refactor",
                        durationSeconds = 15 * 60,
                        stepType = StepType.WORK,
                        instruction = "Walk through boundary tests and verify Big-O efficiency."
                    )
                )
            ),
            Routine(
                id = "routine_mindfulness",
                title = "Morning Clarity & Breathing",
                description = "Calm your mind, regulate nervous system, and set day's intentions.",
                categoryId = "cat_mindfulness",
                colorHex = 0xFF10B981,
                iconName = "self_improvement",
                steps = listOf(
                    RoutineStep(
                        title = "Box Breathing (4-4-4-4)",
                        durationSeconds = 3 * 60,
                        stepType = StepType.WORK,
                        instruction = "Inhale 4s, Hold 4s, Exhale 4s, Hold 4s."
                    ),
                    RoutineStep(
                        title = "Mindful Body Scan",
                        durationSeconds = 3 * 60,
                        stepType = StepType.REST,
                        instruction = "Release tension in forehead, jaw, shoulders, and spine."
                    ),
                    RoutineStep(
                        title = "Daily Intentions & Gratitude",
                        durationSeconds = 2 * 60,
                        stepType = StepType.WORK,
                        instruction = "Mentally name 3 things you are grateful for and 1 primary goal."
                    )
                )
            )
        )
    }

    init {
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            saveCategories(DEFAULT_CATEGORIES)
            saveRoutines(DEFAULT_ROUTINES)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
        }
    }

    // Categories
    fun getCategories(): List<RoutineCategory> {
        val jsonStr = prefs.getString(KEY_CATEGORIES, null) ?: return DEFAULT_CATEGORIES
        val list = mutableListOf<RoutineCategory>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RoutineCategory(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        emoji = obj.optString("emoji", "🎯"),
                        colorHex = obj.optLong("colorHex", 0xFF6366F1),
                        isCustom = obj.optBoolean("isCustom", false)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DEFAULT_CATEGORIES
        }
        return list
    }

    fun saveCategories(categories: List<RoutineCategory>) {
        val arr = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("emoji", cat.emoji)
                put("colorHex", cat.colorHex)
                put("isCustom", cat.isCustom)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES, arr.toString()).apply()
    }

    fun addCategory(category: RoutineCategory) {
        val current = getCategories().toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category
        } else {
            current.add(category)
        }
        saveCategories(current)
    }

    fun deleteCategory(categoryId: String) {
        val current = getCategories().filter { it.id != categoryId }
        saveCategories(current)
    }

    // Routines
    fun getRoutines(): List<Routine> {
        val jsonStr = prefs.getString(KEY_ROUTINES, null) ?: return DEFAULT_ROUTINES
        val list = mutableListOf<Routine>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
                val stepsList = mutableListOf<RoutineStep>()
                for (j in 0 until stepsArr.length()) {
                    val sObj = stepsArr.getJSONObject(j)
                    val stepTypeStr = sObj.optString("stepType", StepType.WORK.name)
                    val stepType = try {
                        StepType.valueOf(stepTypeStr)
                    } catch (_: Exception) {
                        StepType.WORK
                    }
                    stepsList.add(
                        RoutineStep(
                            id = sObj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = sObj.getString("title"),
                            durationSeconds = sObj.getInt("durationSeconds"),
                            stepType = stepType,
                            instruction = sObj.optString("instruction", "")
                        )
                    )
                }

                list.add(
                    Routine(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        categoryId = obj.getString("categoryId"),
                        colorHex = obj.optLong("colorHex", 0xFF6366F1),
                        iconName = obj.optString("iconName", "timer"),
                        steps = stepsList,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DEFAULT_ROUTINES
        }
        return list
    }

    fun saveRoutines(routines: List<Routine>) {
        val arr = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("description", r.description)
                put("categoryId", r.categoryId)
                put("colorHex", r.colorHex)
                put("iconName", r.iconName)
                put("createdAt", r.createdAt)

                val stepsArr = JSONArray()
                for (s in r.steps) {
                    val sObj = JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("durationSeconds", s.durationSeconds)
                        put("stepType", s.stepType.name)
                        put("instruction", s.instruction)
                    }
                    stepsArr.put(sObj)
                }
                put("steps", stepsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ROUTINES, arr.toString()).apply()
    }

    fun getRoutineById(id: String): Routine? {
        return getRoutines().firstOrNull { it.id == id }
    }

    fun saveRoutine(routine: Routine) {
        val current = getRoutines().toMutableList()
        val index = current.indexOfFirst { it.id == routine.id }
        if (index >= 0) {
            current[index] = routine
        } else {
            current.add(0, routine)
        }
        saveRoutines(current)
    }

    fun deleteRoutine(routineId: String) {
        val current = getRoutines().filter { it.id != routineId }
        saveRoutines(current)
    }

    // Completed Sessions
    fun getCompletedSessions(): List<CompletedSession> {
        val jsonStr = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        val list = mutableListOf<CompletedSession>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CompletedSession(
                        id = obj.getString("id"),
                        routineId = obj.getString("routineId"),
                        routineTitle = obj.getString("routineTitle"),
                        categoryName = obj.optString("categoryName", "General"),
                        categoryEmoji = obj.optString("categoryEmoji", "🎯"),
                        categoryColorHex = obj.optLong("categoryColorHex", 0xFF6366F1),
                        completedAt = obj.getLong("completedAt"),
                        durationSeconds = obj.getInt("durationSeconds"),
                        stepsCompleted = obj.getInt("stepsCompleted"),
                        totalSteps = obj.getInt("totalSteps")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        return list
    }

    fun saveCompletedSession(session: CompletedSession) {
        val current = getCompletedSessions().toMutableList()
        current.add(0, session)
        // Keep last 100 sessions
        val trimmed = if (current.size > 100) current.take(100) else current

        val arr = JSONArray()
        for (s in trimmed) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("routineId", s.routineId)
                put("routineTitle", s.routineTitle)
                put("categoryName", s.categoryName)
                put("categoryEmoji", s.categoryEmoji)
                put("categoryColorHex", s.categoryColorHex)
                put("completedAt", s.completedAt)
                put("durationSeconds", s.durationSeconds)
                put("stepsCompleted", s.stepsCompleted)
                put("totalSteps", s.totalSteps)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    // Scheduled Routines (Calendar Planning)
    fun getScheduledRoutines(): List<ScheduledRoutine> {
        val jsonStr = prefs.getString(KEY_SCHEDULED, null)
        if (jsonStr == null) {
            // Seed a couple of upcoming scheduled routines for today & tomorrow
            val todayEpoch = java.time.LocalDate.now().toEpochDay()
            val initialScheduled = listOf(
                ScheduledRoutine(
                    id = "sched_comm_today",
                    routineId = "routine_comm",
                    routineTitle = "Communication Skills (30 Min)",
                    categoryName = "Communication Skills",
                    categoryEmoji = "🎙️",
                    categoryColorHex = 0xFF8B5CF6,
                    dateEpochDay = todayEpoch,
                    timeOfDay = "09:00",
                    isCompleted = false
                ),
                ScheduledRoutine(
                    id = "sched_gym_tomorrow",
                    routineId = "routine_gym",
                    routineTitle = "Gym Heavy Chest & Arms (2 Hours)",
                    categoryName = "Gym & Workout",
                    categoryEmoji = "💪",
                    categoryColorHex = 0xFFEF4444,
                    dateEpochDay = todayEpoch + 1,
                    timeOfDay = "18:00",
                    isCompleted = false
                ),
                ScheduledRoutine(
                    id = "sched_study_day3",
                    routineId = "routine_study",
                    routineTitle = "Study & Development Sprint (4 Hours)",
                    categoryName = "Study & Coding",
                    categoryEmoji = "💻",
                    categoryColorHex = 0xFF06B6D4,
                    dateEpochDay = todayEpoch + 2,
                    timeOfDay = "14:00",
                    isCompleted = false
                )
            )
            saveScheduledRoutines(initialScheduled)
            return initialScheduled
        }

        val list = mutableListOf<ScheduledRoutine>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ScheduledRoutine(
                        id = obj.getString("id"),
                        routineId = obj.getString("routineId"),
                        routineTitle = obj.optString("routineTitle", "Routine"),
                        categoryName = obj.optString("categoryName", "General"),
                        categoryEmoji = obj.optString("categoryEmoji", "🎯"),
                        categoryColorHex = obj.optLong("categoryColorHex", 0xFF6366F1),
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        timeOfDay = obj.optString("timeOfDay", "09:00"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        return list
    }

    fun saveScheduledRoutines(routines: List<ScheduledRoutine>) {
        val arr = JSONArray()
        for (r in routines) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("routineId", r.routineId)
                put("routineTitle", r.routineTitle)
                put("categoryName", r.categoryName)
                put("categoryEmoji", r.categoryEmoji)
                put("categoryColorHex", r.categoryColorHex)
                put("dateEpochDay", r.dateEpochDay)
                put("timeOfDay", r.timeOfDay)
                put("isCompleted", r.isCompleted)
                put("createdAt", r.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCHEDULED, arr.toString()).apply()
    }

    fun saveScheduledRoutine(item: ScheduledRoutine) {
        val current = getScheduledRoutines().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        saveScheduledRoutines(current)
    }

    fun deleteScheduledRoutine(id: String) {
        val current = getScheduledRoutines().filter { it.id != id }
        saveScheduledRoutines(current)
    }

    fun toggleScheduledRoutineCompleted(id: String) {
        val current = getScheduledRoutines().map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveScheduledRoutines(current)
    }

    // Theme Mode ("DARK", "LIGHT", "SYSTEM")
    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}

