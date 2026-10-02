package com.example.routineforge.core.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SessionStatus { RUNNING, PAUSED, COMPLETED, CANCELLED }

enum class SessionType(val label: String) { FOCUS("Focus"), SHORT_BREAK("Short Break"), LONG_BREAK("Long Break") }

enum class GoalPeriod(val label: String) { DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly"), YEARLY("Yearly") }

enum class RepeatRule(val label: String) { NONE("Does not repeat"), DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly") }

enum class EventCategory(val label: String, val color: Long) {
    STUDY("Study", 0xFF5B5FEF),
    WORK("Work", 0xFF2F80ED),
    PERSONAL("Personal", 0xFF27AE87),
    EXAM("Exam", 0xFFE5484D),
    MEETING("Meeting", 0xFFF2994A)
}

enum class ReminderCategory(val label: String) { STUDY("Study"), PERSONAL("Personal"), EXAM("Exam"), WORK("Work") }

enum class AttachmentKind { IMAGE, FILE }

/** Single row (id = 1): the user's name and app-wide preferences. */
@Immutable
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val onboardingDone: Boolean = false,
    val themeMode: String = THEME_SYSTEM,
    val streakMinMinutes: Int = 15,
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakEvery: Int = 4,
    val autoStartBreak: Boolean = false,
    val autoStartFocus: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val updatedAt: Long = 0L
) {
    fun minutesFor(type: SessionType): Int = when (type) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    }

    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }
}

@Immutable
@Entity(tableName = "subjects", indices = [Index("isArchived")])
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "book",
    val color: Long = 0xFF5B5FEF,
    val dailyTargetMinutes: Int = 60,
    /** 0 = derived from the daily target. */
    val weeklyTargetMinutes: Int = 0,
    val monthlyTargetMinutes: Int = 0,
    val yearlyTargetMinutes: Int = 0,
    /** LocalDate.toEpochDay(), or null. */
    val examDate: Long? = null,
    /** 0 low, 1 medium, 2 high. */
    val priority: Int = 1,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isArchived: Boolean = false
)

/**
 * The source of truth for focused time. While RUNNING, focused time is
 * [actualDurationMs] + (now - [segmentStartedAt]); pauses never count.
 */
@Immutable
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [ForeignKey(entity = Subject::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("subjectId"), Index("status"), Index("endTime")]
)
data class FocusSession(
    @PrimaryKey val id: String,
    val subjectId: Long?,
    val sessionType: SessionType,
    val status: SessionStatus,
    val plannedDurationMs: Long,
    /** Focused time banked before the current running segment; final value once COMPLETED. */
    val actualDurationMs: Long = 0L,
    val startTime: Long,
    val endTime: Long? = null,
    /** Wall-clock start of the current running segment; null unless RUNNING. */
    val segmentStartedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Immutable
@Entity(tableName = "goals", indices = [Index(value = ["period"], unique = true)])
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: GoalPeriod,
    val targetMinutes: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Immutable
@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = Subject::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("subjectId"), Index("dueDate")]
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subjectId: Long? = null,
    /** LocalDate.toEpochDay(), or null for "someday". */
    val dueDate: Long? = null,
    val priority: Int = 1,
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Immutable
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(entity = Subject::class, parentColumns = ["id"], childColumns = ["subjectId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("subjectId"), Index("updatedAt")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    /** JSON list of blocks, see NoteContent. */
    val content: String = "[]",
    /** Plain text of all blocks, for search and previews. */
    val plainText: String = "",
    val subjectId: Long? = null,
    /** Comma-separated. */
    val tags: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Immutable
@Entity(
    tableName = "note_attachments",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")]
)
data class NoteAttachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val kind: AttachmentKind,
    val fileName: String,
    /** Path inside the app's private files dir. */
    val path: String,
    val mimeType: String,
    val createdAt: Long = 0L
)

@Immutable
@Entity(
    tableName = "drawings",
    foreignKeys = [ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("noteId")]
)
data class Drawing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    /** JSON stroke list, see DrawingStrokes. */
    val strokes: String = "[]",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Immutable
@Entity(tableName = "calendar_events", indices = [Index("startTime")])
data class CalendarEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startTime: Long,
    val endTime: Long,
    val category: EventCategory = EventCategory.STUDY,
    val color: Long = category.color,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminderId: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Immutable
@Entity(tableName = "reminders", indices = [Index("triggerTime"), Index("eventId")])
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** First trigger; repeats are computed from here. */
    val triggerTime: Long,
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val enabled: Boolean = true,
    /** 0 low, 1 normal, 2 high. */
    val priority: Int = 1,
    val category: ReminderCategory = ReminderCategory.STUDY,
    val subjectId: Long? = null,
    val taskId: Long? = null,
    val eventId: Long? = null,
    val isDone: Boolean = false,
    val lastFiredAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/** Lightweight projection used by analytics. */
@Immutable
data class SessionSlice(
    val subjectId: Long?,
    val startTime: Long,
    val endTime: Long,
    val actualDurationMs: Long
)
