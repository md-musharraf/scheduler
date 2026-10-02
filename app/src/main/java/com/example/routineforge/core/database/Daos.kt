package com.example.routineforge.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfile?

    @Upsert
    suspend fun upsert(profile: UserProfile)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY isArchived, priority DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observe(id: Long): Flow<Subject?>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun get(id: Long): Subject?

    @Insert
    suspend fun insert(subject: Subject): Long

    @Update
    suspend fun update(subject: Subject)

    @Query("UPDATE subjects SET isArchived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING','PAUSED') ORDER BY createdAt DESC LIMIT 1")
    fun observeActive(): Flow<FocusSession?>

    @Query("SELECT * FROM focus_sessions WHERE status IN ('RUNNING','PAUSED') ORDER BY createdAt DESC")
    suspend fun getActive(): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun get(id: String): FocusSession?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: FocusSession)

    // Every state change is guarded by the expected current status, so a repeated or racing
    // call updates 0 rows instead of double counting.
    @Query("UPDATE focus_sessions SET status = 'PAUSED', actualDurationMs = :banked, segmentStartedAt = NULL, updatedAt = :now WHERE id = :id AND status = 'RUNNING'")
    suspend fun pause(id: String, banked: Long, now: Long): Int

    @Query("UPDATE focus_sessions SET status = 'RUNNING', segmentStartedAt = :now, updatedAt = :now WHERE id = :id AND status = 'PAUSED'")
    suspend fun resume(id: String, now: Long): Int

    @Query("UPDATE focus_sessions SET status = 'COMPLETED', actualDurationMs = :actual, endTime = :end, segmentStartedAt = NULL, updatedAt = :now WHERE id = :id AND status IN ('RUNNING','PAUSED')")
    suspend fun complete(id: String, actual: Long, end: Long, now: Long): Int

    @Query("UPDATE focus_sessions SET status = 'CANCELLED', actualDurationMs = :actual, endTime = :now, segmentStartedAt = NULL, updatedAt = :now WHERE id = :id AND status IN ('RUNNING','PAUSED')")
    suspend fun cancel(id: String, actual: Long, now: Long): Int


    /** Completed focus time overlapping [from, to). */
    @Query(
        "SELECT subjectId, startTime, endTime, actualDurationMs FROM focus_sessions " +
            "WHERE status = 'COMPLETED' AND sessionType = 'FOCUS' AND endTime > :from AND startTime < :to"
    )
    fun observeFocusBetween(from: Long, to: Long): Flow<List<SessionSlice>>

    @Query("SELECT subjectId, startTime, endTime, actualDurationMs FROM focus_sessions WHERE status = 'COMPLETED' AND sessionType = 'FOCUS'")
    fun observeAllFocus(): Flow<List<SessionSlice>>

    @Query("SELECT * FROM focus_sessions WHERE subjectId = :subjectId AND status = 'COMPLETED' AND sessionType = 'FOCUS' ORDER BY endTime DESC LIMIT :limit")
    fun observeRecentForSubject(subjectId: Long, limit: Int): Flow<List<FocusSession>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND sessionType = 'FOCUS' AND endTime >= :from")
    suspend fun countFocusSince(from: Long): Int

    @Insert
    suspend fun insertAll(sessions: List<FocusSession>)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE period = :period")
    suspend fun get(period: GoalPeriod): Goal?

    @Upsert
    suspend fun upsert(goal: Goal)

    @Query("DELETE FROM goals WHERE period = :period")
    suspend fun delete(period: GoalPeriod)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isDone, dueDate IS NULL, dueDate, priority DESC, createdAt")
    fun observeAll(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY isDone, dueDate IS NULL, dueDate, createdAt")
    fun observeForSubject(subjectId: Long): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskItem?

    @Insert
    suspend fun insert(task: TaskItem): Long

    @Update
    suspend fun update(task: TaskItem)

    @Query("UPDATE tasks SET isDone = :done, completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?, now: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY isPinned DESC, updatedAt DESC")
    fun observeForSubject(subjectId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM note_attachments WHERE noteId = :noteId")
    suspend fun attachments(noteId: Long): List<NoteAttachment>

    @Query("SELECT * FROM note_attachments")
    fun observeAttachments(): Flow<List<NoteAttachment>>

    @Query("SELECT * FROM note_attachments WHERE id = :id")
    suspend fun attachment(id: Long): NoteAttachment?

    @Insert
    suspend fun insertAttachment(attachment: NoteAttachment): Long

    @Query("DELETE FROM note_attachments WHERE id = :id")
    suspend fun deleteAttachment(id: Long)

    @Query("SELECT * FROM drawings WHERE id = :id")
    suspend fun drawing(id: Long): Drawing?

    @Query("SELECT * FROM drawings WHERE noteId = :noteId")
    suspend fun drawingsFor(noteId: Long): List<Drawing>

    @Query("SELECT * FROM drawings")
    fun observeDrawings(): Flow<List<Drawing>>

    @Insert
    suspend fun insertDrawing(drawing: Drawing): Long

    @Update
    suspend fun updateDrawing(drawing: Drawing)

    @Query("DELETE FROM drawings WHERE id = :id")
    suspend fun deleteDrawing(id: Long)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM calendar_events WHERE (repeatRule = 'NONE' AND endTime > :from AND startTime < :to) OR (repeatRule != 'NONE' AND startTime < :to)")
    fun observeCandidates(from: Long, to: Long): Flow<List<CalendarEvent>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun get(id: Long): CalendarEvent?

    @Insert
    suspend fun insert(event: CalendarEvent): Long

    @Update
    suspend fun update(event: CalendarEvent)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isDone, triggerTime")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND isDone = 0")
    suspend fun getActive(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun get(id: Long): Reminder?

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE reminders SET lastFiredAt = :at WHERE id = :id")
    suspend fun markFired(id: Long, at: Long)
}
