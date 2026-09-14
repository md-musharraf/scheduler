package com.example.routineforge.data

import androidx.compose.runtime.Immutable
import com.example.routineforge.util.TimeFormatters
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

@Immutable
data class RoutineStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val durationSeconds: Int,
    val stepType: StepType = StepType.WORK,
    val instruction: String = ""
) {
    fun formattedDuration(): String = TimeFormatters.formatDurationSeconds(durationSeconds)

    fun sanitized(): RoutineStep {
        return copy(
            title = title.trim().take(80),
            durationSeconds = durationSeconds.coerceIn(1, 86400),
            instruction = instruction.trim().take(200)
        )
    }
}

@Immutable
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

    fun sanitized(): RoutineCategory {
        return copy(
            name = name.trim().take(40),
            emoji = if (emoji.isBlank()) "🎯" else emoji.trim().take(4)
        )
    }
}

@Immutable
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
        get() = TimeFormatters.formatDurationSeconds(totalDurationSeconds)

    fun sanitized(): Routine {
        return copy(
            title = title.trim().take(80),
            description = description.trim().take(300),
            steps = steps.map { it.sanitized() }
        )
    }
}

@Immutable
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
    fun formattedDuration(): String = TimeFormatters.formatDurationSeconds(durationSeconds)
}

@Immutable
data class ScheduledRoutine(
    val id: String = UUID.randomUUID().toString(),
    val routineId: String = "",
    val routineTitle: String = "",
    val categoryName: String = "General",
    val categoryEmoji: String = "🎯",
    val categoryColorHex: Long = 0xFF6366F1,
    val dateEpochDay: Long, // LocalDate.toEpochDay()
    val timeOfDay: String = "09:00", // e.g. "07:00", "18:30"
    val durationMinutes: Int = 30, // Default duration in minutes
    val remindBeforeMinutes: Int = 1, // Alert 1 min before (customizable)
    val remindAtTime: Boolean = true, // Alert at exact scheduled time
    val vibrationPattern: String = "NOTHING_PULSE",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedDuration(): String = TimeFormatters.formatDurationMinutes(durationMinutes)

    fun sanitized(): ScheduledRoutine {
        val safeTime = if (TimeFormatters.isValidTimeOfDay(timeOfDay)) timeOfDay else "09:00"
        return copy(
            routineTitle = routineTitle.trim().take(80),
            categoryName = categoryName.trim().take(40),
            timeOfDay = safeTime,
            durationMinutes = durationMinutes.coerceIn(1, 1440),
            remindBeforeMinutes = remindBeforeMinutes.coerceIn(0, 120)
        )
    }
}
