package com.example.routineforge.data

import android.content.Context
import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.SessionDao
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.database.SubjectDao
import com.example.routineforge.core.notifications.Alarms
import com.example.routineforge.core.notifications.Notifier
import com.example.routineforge.domain.TimerMath
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** Today's totals, shown when a focus session completes. [goalPercent] is null without a daily goal. */
data class TodaySummary(val sessions: Int, val focusedMs: Long, val goalPercent: Int?)

sealed interface TimerEvent {
    /** [savedMs] = focus time persisted (0 when discarded or for breaks). [summary] is set for completed focus sessions. */
    data class Finished(
        val type: SessionType, val savedMs: Long, val natural: Boolean, val next: SessionType,
        val nextStarted: Boolean = false, val summary: TodaySummary? = null
    ) : TimerEvent
}

/**
 * The timer use case. All state lives in the focus_sessions table; this class only moves a
 * session between states (each move guarded in SQL, so completing twice is a no-op) and keeps
 * the ongoing notification and the alarms in sync.
 */
class FocusTimer(
    private val context: Context,
    private val dao: SessionDao,
    private val subjects: SubjectDao,
    private val profiles: ProfileRepository,
    private val today: suspend () -> TodaySummary = { TodaySummary(0, 0L, null) },
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    val active: Flow<FocusSession?> = dao.observeActive()


    /**
     * Every state change runs to the end even if the caller is cancelled: completing a session
     * changes the flow the caller may be collecting with collectLatest, which would otherwise
     * cancel it between the database write and the notification / auto-start that follow.
     */
    private suspend fun <T> locked(block: suspend () -> T): T = withContext(NonCancellable) { mutex.withLock { block() } }

    /** Starts a session unless one is already active, in which case that one is returned. */
    suspend fun start(type: SessionType, subjectId: Long?, plannedMs: Long): FocusSession = locked {
        dao.getActive().firstOrNull() ?: startLocked(type, subjectId, plannedMs)
    }

    /** Starts [type] with its default length (notification and widget buttons). */
    suspend fun startDefault(type: SessionType) {
        start(type, if (type == SessionType.FOCUS) dao.lastFocusSubject() else null, profiles.get().minutesFor(type) * 60_000L)
        Notifier.cancel(context, Notifier.ID_TIMER_DONE)
    }

    private suspend fun startLocked(type: SessionType, subjectId: Long?, plannedMs: Long): FocusSession {
        require(plannedMs in 60_000L..(24 * 3_600_000L)) { "Duration must be between 1 minute and 24 hours" }
        val now = clock()
        val session = FocusSession(
            id = UUID.randomUUID().toString(),
            subjectId = if (type == SessionType.FOCUS) subjectId else null,
            sessionType = type,
            status = SessionStatus.RUNNING,
            plannedDurationMs = plannedMs,
            startTime = now,
            segmentStartedAt = now,
            createdAt = now,
            updatedAt = now
        )
        dao.insert(session)
        sync(session)
        return session
    }

    suspend fun pause(id: String) = locked {
        val s = dao.get(id) ?: return@locked
        val now = clock()
        if (TimerMath.isDue(s, now)) return@locked completeDueLocked(s)
        if (dao.pause(id, TimerMath.elapsed(s, now), now) == 1) sync(dao.get(id))
    }

    suspend fun resume(id: String) = locked {
        if (dao.resume(id, clock()) == 1) sync(dao.get(id))
    }

    /**
     * Ends the session now. Focus time of at least a minute is saved when [save] is true;
     * breaks and shorter sessions are cancelled (kept for the record, never counted).
     */
    suspend fun stop(id: String, save: Boolean): Long = locked {
        val s = dao.get(id) ?: return@locked 0L
        val now = clock()
        if (TimerMath.isDue(s, now)) {
            completeDueLocked(s)
            return@locked if (s.sessionType == SessionType.FOCUS) s.plannedDurationMs else 0L
        }
        val elapsed = TimerMath.elapsed(s, now)
        val keep = save && s.sessionType == SessionType.FOCUS && elapsed >= TimerMath.MIN_SAVE_MS
        val changed = if (keep) dao.complete(id, elapsed, now, now) else dao.cancel(id, elapsed, now)
        if (changed == 1) {
            clearSideEffects()
            _events.tryEmit(TimerEvent.Finished(s.sessionType, if (keep) elapsed else 0L, natural = false, next = nextAfter(s.sessionType)))
        }
        if (keep && changed == 1) elapsed else 0L
    }

    /** Completes the active session if its time is up. Safe to call from anywhere, any number of times. */
    suspend fun completeIfDue() = locked {
        val now = clock()
        dao.getActive().filter { TimerMath.isDue(it, now) }.forEach { completeDueLocked(it) }
    }

    /** The one-minute warning alarm: posts it only if the running session really ends within about a minute. */
    suspend fun warnIfEndingSoon() = locked {
        val s = dao.getActive().firstOrNull() ?: return@locked
        val left = TimerMath.remaining(s, clock())
        if (s.status == SessionStatus.RUNNING && left in 1..WARN_WINDOW_MS + 5_000) {
            Notifier.showEndingSoon(context, s.sessionType, s.subjectId?.let { subjects.get(it)?.name }, left)
        }
    }

    /** Re-posts the notification and alarm for whatever is active (after reboot or app start). */
    suspend fun restore() {
        completeIfDue()
        locked { sync(dao.getActive().firstOrNull()) }
    }

    private suspend fun completeDueLocked(s: FocusSession) {
        // The session really ended at endsAt, even if we only notice later (app killed, device asleep).
        val end = TimerMath.endsAt(s) ?: clock()
        if (dao.complete(s.id, s.plannedDurationMs, end, clock()) != 1) return
        clearSideEffects()
        val profile = profiles.get()
        val next = nextAfter(s.sessionType)
        val isFocus = s.sessionType == SessionType.FOCUS
        val autoStart = if (isFocus) profile.autoStartBreak else profile.autoStartFocus
        val summary = if (isFocus) today() else null
        Notifier.showTimerDone(context, s.sessionType, s.plannedDurationMs, summary, next, autoStart, profile.soundEnabled, profile.vibrationEnabled)
        _events.tryEmit(TimerEvent.Finished(s.sessionType, if (isFocus) s.plannedDurationMs else 0L, natural = true, next = next, nextStarted = autoStart, summary = summary))
        if (autoStart) startLocked(next, if (next == SessionType.FOCUS) dao.lastFocusSubject() else null, profile.minutesFor(next) * 60_000L)
    }

    private suspend fun nextAfter(type: SessionType): SessionType {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return TimerMath.nextType(type, dao.countFocusSince(startOfDay), profiles.get().longBreakEvery)
    }

    private fun clearSideEffects() {
        Alarms.cancelTimer(context)
        Notifier.cancel(context, Notifier.ID_TIMER)
        Notifier.cancel(context, Notifier.ID_ENDING_SOON)
    }

    private suspend fun sync(s: FocusSession?) {
        if (s == null || (s.status != SessionStatus.RUNNING && s.status != SessionStatus.PAUSED)) {
            clearSideEffects()
            return
        }
        val now = clock()
        val endsAt = TimerMath.endsAt(s)
        Notifier.cancel(context, Notifier.ID_ENDING_SOON)
        // Warn a minute before the end, for sessions long enough for that to mean something.
        val warnAt = endsAt?.minus(WARN_WINDOW_MS)?.takeIf { s.plannedDurationMs >= 3 * WARN_WINDOW_MS && it > now }
        if (endsAt != null) Alarms.setTimer(context, endsAt, warnAt) else Alarms.cancelTimer(context)
        val name = s.subjectId?.let { subjects.get(it)?.name }
        Notifier.showTimer(context, s.sessionType, name, s.status == SessionStatus.RUNNING, endsAt, TimerMath.remaining(s, now))
    }

    private companion object {
        const val WARN_WINDOW_MS = 60_000L
    }
}
