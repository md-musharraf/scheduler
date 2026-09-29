package com.example.routineforge.data

import androidx.compose.runtime.Immutable
import com.example.routineforge.util.TimeFormatters
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val title: String
        get() = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(60) ?: "Untitled"

    val preview: String
        get() = text.lineSequence().filter { it.isNotBlank() }.drop(1).joinToString(" ").take(140)
}

@Immutable
data class TimerSettings(
    val label: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoRepeat: Boolean = false,
    val lastDurationSeconds: Int = 5 * 60
)

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
    val createdAt: Long = System.currentTimeMillis(),
    val repeat: String = REPEAT_NONE, // NONE / DAILY / WEEKLY / MONTHLY
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val note: String = ""
) {
    companion object {
        const val REPEAT_NONE = "NONE"
        const val REPEAT_DAILY = "DAILY"
        const val REPEAT_WEEKLY = "WEEKLY"
        const val REPEAT_MONTHLY = "MONTHLY"
        val REPEAT_OPTIONS = listOf(REPEAT_NONE, REPEAT_DAILY, REPEAT_WEEKLY, REPEAT_MONTHLY)
    }

    val isRepeating: Boolean get() = repeat != REPEAT_NONE

    fun formattedDuration(): String = TimeFormatters.formatDurationMinutes(durationMinutes)

    /** The first start time strictly after [after], or null if a one-off reminder is already past. */
    fun nextOccurrence(after: LocalDateTime): LocalDateTime? {
        val start = LocalDate.ofEpochDay(dateEpochDay).atTime(LocalTime.parse(sanitized().timeOfDay))
        if (start.isAfter(after)) return start
        if (!isRepeating) return null
        // Step from the original start each time so monthly Jan 31 → Feb 28 → Mar 31 stays anchored.
        // Daily jumps straight to ~today; weekly/monthly loop at most ~52/12 times per elapsed year.
        var k = if (repeat == REPEAT_DAILY) maxOf(1L, ChronoUnit.DAYS.between(start, after)) else 1L
        while (true) {
            val next = when (repeat) {
                REPEAT_DAILY -> start.plusDays(k)
                REPEAT_WEEKLY -> start.plusWeeks(k)
                else -> start.plusMonths(k)
            }
            if (next.isAfter(after)) return next
            k++
        }
    }

    fun sanitized(): ScheduledRoutine {
        val safeTime = if (TimeFormatters.isValidTimeOfDay(timeOfDay)) timeOfDay else "09:00"
        return copy(
            routineTitle = routineTitle.trim().take(80),
            categoryName = categoryName.trim().take(40),
            timeOfDay = safeTime,
            durationMinutes = durationMinutes.coerceIn(0, 1440),
            remindBeforeMinutes = remindBeforeMinutes.coerceIn(0, 120),
            repeat = if (repeat in REPEAT_OPTIONS) repeat else REPEAT_NONE,
            vibrationEnabled = vibrationEnabled || !soundEnabled, // never fully silent
            note = note.trim().take(500)
        )
    }
}
