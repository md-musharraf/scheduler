package com.example.routineforge.data

import java.util.UUID

enum class StepType {
    WORK,
    REST,
    PREPARE;

    fun displayName(): String = when (this) {
        WORK -> "Work / Focus"
        REST -> "Rest / Break"
        PREPARE -> "Prepare / Warm Up"
    }
}

data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationSeconds: Int,
    val stepType: StepType = StepType.WORK,
    val instruction: String = ""
) {
    fun formattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return when {
            minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

data class RoutineCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long,
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            RoutineCategory("cat_comm", "Communication", "🎙️", 0xFF8B5CF6),
            RoutineCategory("cat_gym", "Workout", "💪", 0xFFEF4444),
            RoutineCategory("cat_study", "Study", "📚", 0xFF6366F1),
            RoutineCategory("cat_mindfulness", "Mindfulness", "🧘", 0xFF10B981),
            RoutineCategory("cat_productivity", "Focus", "⚡", 0xFFF59E0B)
        )
    }
}

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val categoryId: String,
    val colorHex: Long = 0xFF6366F1,
    val iconName: String = "timer",
    val steps: List<RoutineStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val totalDurationSeconds: Int
        get() = steps.sumOf { it.durationSeconds }

    val formattedDuration: String
        get() {
            val total = totalDurationSeconds
            val hours = total / 3600
            val minutes = (total % 3600) / 60
            val seconds = total % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m"
                else -> "${seconds}s"
            }
        }
}

data class CompletedSession(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val routineTitle: String,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryColorHex: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val stepsCompleted: Int,
    val totalSteps: Int
) {
    fun formattedDuration(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return when {
            minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
}

data class ScheduledRoutine(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String,
    val routineTitle: String = "",
    val categoryName: String = "General",
    val categoryEmoji: String = "🎯",
    val categoryColorHex: Long = 0xFF6366F1,
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val timeOfDay: String = "09:00", // e.g. "07:00", "18:30"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

