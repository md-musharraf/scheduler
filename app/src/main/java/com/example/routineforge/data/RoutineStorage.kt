package com.example.routineforge.data

import android.content.Context
import android.content.SharedPreferences
import com.example.routineforge.util.TimeFormatters
import org.json.JSONArray
import org.json.JSONObject

/**
 * Thread-safe, synchronized persistent storage for routines, categories, history and schedules.
 * Hardened against concurrent write corruption and malformed input entries.
 */
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
        private const val KEY_DATA_CLEARED = "is_data_cleared_v1"

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
            )
        )
    }

    init {
        synchronized(this) {
            val isCleared = prefs.getBoolean(KEY_DATA_CLEARED, false)
            val isInitialized = prefs.getBoolean(KEY_INITIALIZED, false)
            if (!isInitialized && !isCleared) {
                saveCategories(DEFAULT_CATEGORIES)
                saveRoutines(emptyList()) // Start with clean empty database
                prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            }
        }
    }

    @Synchronized
    fun clearAllData() {
        prefs.edit()
            .putBoolean(KEY_DATA_CLEARED, true)
            .putString(KEY_ROUTINES, "[]")
            .putString(KEY_SESSIONS, "[]")
            .putString(KEY_SCHEDULED, "[]")
            .apply()
    }

    // Categories
    @Synchronized
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
                    ).sanitized()
                )
            }
        } catch (_: Exception) {
            return DEFAULT_CATEGORIES
        }
        return list
    }

    @Synchronized
    fun saveCategories(categories: List<RoutineCategory>) {
        val arr = JSONArray()
        for (cat in categories) {
            val clean = cat.sanitized()
            val obj = JSONObject().apply {
                put("id", clean.id)
                put("name", clean.name)
                put("emoji", clean.emoji)
                put("colorHex", clean.colorHex)
                put("isCustom", clean.isCustom)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CATEGORIES, arr.toString()).apply()
    }

    @Synchronized
    fun addCategory(category: RoutineCategory) {
        val current = getCategories().toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category.sanitized()
        } else {
            current.add(category.sanitized())
        }
        saveCategories(current)
    }

    @Synchronized
    fun deleteCategory(categoryId: String) {
        val current = getCategories().filter { it.id != categoryId }
        saveCategories(current)
    }

    // Routines
    @Synchronized
    fun getRoutines(): List<Routine> {
        val isCleared = prefs.getBoolean(KEY_DATA_CLEARED, false)
        val jsonStr = prefs.getString(KEY_ROUTINES, null)
        if (jsonStr == null || jsonStr == "[]" || (isCleared && jsonStr.isBlank())) {
            return emptyList()
        }
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
                        ).sanitized()
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
                    ).sanitized()
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    @Synchronized
    fun saveRoutines(routines: List<Routine>) {
        val arr = JSONArray()
        for (r in routines) {
            val clean = r.sanitized()
            val obj = JSONObject().apply {
                put("id", clean.id)
                put("title", clean.title)
                put("description", clean.description)
                put("categoryId", clean.categoryId)
                put("colorHex", clean.colorHex)
                put("iconName", clean.iconName)
                put("createdAt", clean.createdAt)

                val stepsArr = JSONArray()
                for (s in clean.steps) {
                    val cleanStep = s.sanitized()
                    val sObj = JSONObject().apply {
                        put("id", cleanStep.id)
                        put("title", cleanStep.title)
                        put("durationSeconds", cleanStep.durationSeconds)
                        put("stepType", cleanStep.stepType.name)
                        put("instruction", cleanStep.instruction)
                    }
                    stepsArr.put(sObj)
                }
                put("steps", stepsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_ROUTINES, arr.toString()).apply()
    }

    @Synchronized
    fun getRoutineById(id: String): Routine? {
        return getRoutines().firstOrNull { it.id == id }
    }

    @Synchronized
    fun saveRoutine(routine: Routine) {
        val current = getRoutines().toMutableList()
        val clean = routine.sanitized()
        val index = current.indexOfFirst { it.id == clean.id }
        if (index >= 0) {
            current[index] = clean
        } else {
            current.add(0, clean)
        }
        saveRoutines(current)
    }

    @Synchronized
    fun deleteRoutine(routineId: String) {
        val current = getRoutines().filter { it.id != routineId }
        saveRoutines(current)
    }

    // Completed Sessions
    @Synchronized
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
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    @Synchronized
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

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    // Scheduled Routines (Calendar Planning)
    @Synchronized
    fun getScheduledRoutines(): List<ScheduledRoutine> {
        val jsonStr = prefs.getString(KEY_SCHEDULED, null) ?: return emptyList()
        val list = mutableListOf<ScheduledRoutine>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawTime = obj.optString("timeOfDay", "09:00")
                val safeTime = if (TimeFormatters.isValidTimeOfDay(rawTime)) rawTime else "09:00"

                list.add(
                    ScheduledRoutine(
                        id = obj.getString("id"),
                        routineId = obj.optString("routineId", ""),
                        routineTitle = obj.optString("routineTitle", "Routine"),
                        categoryName = obj.optString("categoryName", "General"),
                        categoryEmoji = obj.optString("categoryEmoji", "🎯"),
                        categoryColorHex = obj.optLong("categoryColorHex", 0xFF6366F1),
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        timeOfDay = safeTime,
                        durationMinutes = obj.optInt("durationMinutes", 30),
                        remindBeforeMinutes = obj.optInt("remindBeforeMinutes", 1),
                        remindAtTime = obj.optBoolean("remindAtTime", true),
                        vibrationPattern = obj.optString("vibrationPattern", "NOTHING_PULSE"),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ).sanitized()
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return list
    }

    @Synchronized
    fun saveScheduledRoutines(routines: List<ScheduledRoutine>) {
        val arr = JSONArray()
        for (r in routines) {
            val clean = r.sanitized()
            val obj = JSONObject().apply {
                put("id", clean.id)
                put("routineId", clean.routineId)
                put("routineTitle", clean.routineTitle)
                put("categoryName", clean.categoryName)
                put("categoryEmoji", clean.categoryEmoji)
                put("categoryColorHex", clean.categoryColorHex)
                put("dateEpochDay", clean.dateEpochDay)
                put("timeOfDay", clean.timeOfDay)
                put("durationMinutes", clean.durationMinutes)
                put("remindBeforeMinutes", clean.remindBeforeMinutes)
                put("remindAtTime", clean.remindAtTime)
                put("vibrationPattern", clean.vibrationPattern)
                put("isCompleted", clean.isCompleted)
                put("createdAt", clean.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SCHEDULED, arr.toString()).apply()
    }

    @Synchronized
    fun saveScheduledRoutine(item: ScheduledRoutine) {
        val current = getScheduledRoutines().toMutableList()
        val clean = item.sanitized()
        val index = current.indexOfFirst { it.id == clean.id }
        if (index >= 0) {
            current[index] = clean
        } else {
            current.add(clean)
        }
        saveScheduledRoutines(current)
    }

    @Synchronized
    fun deleteScheduledRoutine(id: String) {
        val current = getScheduledRoutines().filter { it.id != id }
        saveScheduledRoutines(current)
    }

    @Synchronized
    fun toggleScheduledRoutineCompleted(id: String) {
        val current = getScheduledRoutines().map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        saveScheduledRoutines(current)
    }

    @Synchronized
    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
    }

    @Synchronized
    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }
}
