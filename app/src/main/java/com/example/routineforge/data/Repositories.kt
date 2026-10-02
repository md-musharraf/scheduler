package com.example.routineforge.data

import com.example.routineforge.core.database.Goal
import com.example.routineforge.core.database.GoalDao
import com.example.routineforge.core.database.GoalPeriod
import com.example.routineforge.core.database.ProfileDao
import com.example.routineforge.core.database.SessionDao
import com.example.routineforge.core.database.SessionSlice
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.database.SubjectDao
import com.example.routineforge.core.database.TaskDao
import com.example.routineforge.core.database.TaskItem
import com.example.routineforge.core.database.UserProfile
import com.example.routineforge.domain.DateRange
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.FocusStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/** Thrown for user-fixable input problems; the message is shown as-is. */
class ValidationException(message: String) : IllegalArgumentException(message)

internal fun validate(condition: Boolean, message: () -> String) {
    if (!condition) throw ValidationException(message())
}

class ProfileRepository(private val dao: ProfileDao) {
    val profile: Flow<UserProfile> = dao.observe().map { it ?: UserProfile() }.distinctUntilChanged()

    suspend fun get(): UserProfile = dao.get() ?: UserProfile()

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        val next = transform(get())
        dao.upsert(
            next.copy(
                name = next.name.trim().take(40),
                streakMinMinutes = next.streakMinMinutes.coerceIn(1, 600),
                focusMinutes = next.focusMinutes.coerceIn(1, 240),
                shortBreakMinutes = next.shortBreakMinutes.coerceIn(1, 60),
                longBreakMinutes = next.longBreakMinutes.coerceIn(1, 120),
                longBreakEvery = next.longBreakEvery.coerceIn(2, 12),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

class SubjectRepository(private val dao: SubjectDao) {
    val subjects: Flow<List<Subject>> = dao.observeAll()
    fun observe(id: Long): Flow<Subject?> = dao.observe(id)
    suspend fun get(id: Long): Subject? = dao.get(id)

    /** Inserts or updates; returns the id. */
    suspend fun save(subject: Subject): Long {
        val name = subject.name.trim()
        validate(name.isNotEmpty()) { "Give the subject a name" }
        validate(name.length <= 40) { "Name is too long (max 40 characters)" }
        validate(subject.dailyTargetMinutes in 0..1440) { "Daily target must be between 0 and 24 hours" }
        validate(subject.weeklyTargetMinutes in 0..10_080) { "Weekly target is too large" }
        validate(subject.monthlyTargetMinutes in 0..44_640) { "Monthly target is too large" }
        validate(subject.yearlyTargetMinutes in 0..525_600) { "Yearly target is too large" }
        val now = System.currentTimeMillis()
        val clean = subject.copy(name = name, description = subject.description.trim().take(200), priority = subject.priority.coerceIn(0, 2), updatedAt = now)
        return if (clean.id == 0L) dao.insert(clean.copy(createdAt = now)) else clean.id.also { dao.update(clean) }
    }

    suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived, System.currentTimeMillis())

    /** Sessions, notes and tasks keep existing with no subject, so focused time is never lost. */
    suspend fun delete(id: Long) = dao.delete(id)
}

class TaskRepository(private val dao: TaskDao) {
    val tasks: Flow<List<TaskItem>> = dao.observeAll()
    fun forSubject(subjectId: Long): Flow<List<TaskItem>> = dao.observeForSubject(subjectId)

    suspend fun save(task: TaskItem): Long {
        val title = task.title.trim()
        validate(title.isNotEmpty()) { "Task can't be empty" }
        val now = System.currentTimeMillis()
        val clean = task.copy(title = title.take(120), updatedAt = now)
        return if (clean.id == 0L) dao.insert(clean.copy(createdAt = now)) else clean.id.also { dao.update(clean) }
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val now = System.currentTimeMillis()
        dao.setDone(id, done, if (done) now else null, now)
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

class GoalRepository(private val dao: GoalDao) {
    val goals: Flow<Map<GoalPeriod, Goal>> = dao.observeAll().map { list -> list.associateBy { it.period } }

    suspend fun set(period: GoalPeriod, minutes: Int) {
        val max = when (period) {
            GoalPeriod.DAILY -> 1440
            GoalPeriod.WEEKLY -> 10_080
            GoalPeriod.MONTHLY -> 44_640
            GoalPeriod.YEARLY -> 525_600
        }
        validate(minutes in 1..max) { "Enter a target between 1 minute and ${max / 60} hours" }
        val now = System.currentTimeMillis()
        val existing = dao.get(period)
        dao.upsert(existing?.copy(targetMinutes = minutes, updatedAt = now) ?: Goal(period = period, targetMinutes = minutes, createdAt = now, updatedAt = now))
    }

    suspend fun clear(period: GoalPeriod) = dao.delete(period)
}

/** Read side of FocusSession: every progress number in the app is derived here. */
class SessionRepository(private val dao: SessionDao) {
    fun focusBetween(range: DateRange, zone: ZoneId = ZoneId.systemDefault()): Flow<List<SessionSlice>> =
        dao.observeFocusBetween(range.startMillis(zone), range.endMillis(zone))

    /** getFocusStats(startDate, endDate) for any period. */
    fun stats(range: DateRange, zone: ZoneId = ZoneId.systemDefault()): Flow<FocusStats> =
        focusBetween(range, zone).map { FocusAnalytics.stats(it, range, zone) }.flowOn(Dispatchers.Default)

    val allFocus: Flow<List<SessionSlice>> = dao.observeAllFocus()

    fun dayTotals(zone: ZoneId = ZoneId.systemDefault()): Flow<Map<LocalDate, Long>> =
        allFocus.map { FocusAnalytics.dayTotals(it, zone) }.flowOn(Dispatchers.Default)

    fun recentForSubject(subjectId: Long, limit: Int = 50) = dao.observeRecentForSubject(subjectId, limit)
}
