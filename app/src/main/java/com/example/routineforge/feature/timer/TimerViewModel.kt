package com.example.routineforge.feature.timer

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.routineforge.AppContainer
import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.database.Subject
import com.example.routineforge.core.database.UserProfile
import com.example.routineforge.core.ui.FFViewModel
import com.example.routineforge.core.ui.todayFlow
import com.example.routineforge.data.TimerEvent
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.FocusStats
import com.example.routineforge.domain.StatsPeriod
import com.example.routineforge.domain.TimerMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Immutable
data class TimerUi(
    val session: FocusSession? = null,
    val now: Long = System.currentTimeMillis(),
    val mode: SessionType = SessionType.FOCUS,
    val selectedSubjectId: Long? = null,
    val profile: UserProfile = UserProfile(),
    val subjects: List<Subject> = emptyList(),
    val today: FocusStats? = null,
    val overallDailyTarget: Int = 0
) {
    val type: SessionType get() = session?.sessionType ?: mode
    val isActive: Boolean get() = session != null
    val isRunning: Boolean get() = session?.status == SessionStatus.RUNNING
    val plannedMs: Long get() = session?.plannedDurationMs ?: (profile.minutesFor(mode) * 60_000L)
    val remainingMs: Long get() = session?.let { TimerMath.remaining(it, now) } ?: plannedMs
    val elapsedMs: Long get() = session?.let { TimerMath.elapsed(it, now) } ?: 0L
    /** Share of the planned time still left at [at], 1 → 0; the hourglass reads it every frame. */
    fun remainingFraction(at: Long): Float =
        session?.let { if (it.plannedDurationMs <= 0) 0f else TimerMath.remaining(it, at).toFloat() / it.plannedDurationMs } ?: 1f
    val subjectId: Long? get() = if (session != null) session.subjectId else selectedSubjectId
    val subject: Subject? get() = subjectId?.let { id -> subjects.firstOrNull { it.id == id } }
    val activeSubjects: List<Subject> get() = subjects.filter { !it.isArchived }

    /** Today's focused ms and target for the chosen subject, or overall without one. */
    val todayProgress: Pair<Long, Int>
        get() {
            val s = subject
            return if (s != null) (today?.bySubject?.get(s.id) ?: 0L) to s.dailyTargetMinutes
            else (today?.totalMs ?: 0L) to overallDailyTarget
        }
}

/** Presentation of the timer; all state changes go through FocusTimer and come back from Room. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModel(private val c: AppContainer) : FFViewModel() {
    private val mode = MutableStateFlow(SessionType.FOCUS)
    val stopwatch = c.stopwatch
    /** Which tab the Timer screen shows; set by the stopwatch notification and widget too. */
    var showStopwatch by mutableStateOf(false)
    /** The focus session that just completed, shown as the "Session Complete" screen until dismissed. */
    var completion by mutableStateOf<TimerEvent.Finished?>(null)
        private set
    private val selectedSubject = MutableStateFlow<Long?>(null)

    /** Ticks only while a session runs, exactly when the displayed second changes; a paused or idle timer doesn't redraw. */
    private val clock = c.timer.active.flatMapLatest { s ->
        if (s?.status == SessionStatus.RUNNING) flow {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                delay(TimerMath.msToNextSecond(TimerMath.remaining(s, now)))
            }
        } else flowOf(System.currentTimeMillis())
    }

    private val todayStats = todayFlow().flatMapLatest { d -> c.sessions.stats(StatsPeriod.DAY.range(d)) }
    private val dailyGoal = combine(c.goals.goals, c.subjects.subjects, todayFlow()) { goals, subjects, d ->
        FocusAnalytics.overallTarget(goals, subjects, StatsPeriod.DAY, StatsPeriod.DAY.range(d))
    }

    val ui: StateFlow<TimerUi> = combine(
        combine(c.timer.active, clock, mode, selectedSubject) { s, now, m, sub -> TimerUi(session = s, now = now, mode = m, selectedSubjectId = sub) },
        c.profiles.profile, c.subjects.subjects, todayStats, dailyGoal
    ) { base, profile, subjects, today, goal ->
        base.copy(profile = profile, subjects = subjects, today = today, overallDailyTarget = goal)
    }.stateIn(TimerUi())

    init {
        // Complete the moment time is up while the app is open (the alarm covers it otherwise).
        viewModelScope.launch {
            c.timer.active.collectLatest { s ->
                val end = s?.let { TimerMath.endsAt(it) } ?: return@collectLatest
                delay((end - System.currentTimeMillis()).coerceAtLeast(0L))
                c.timer.completeIfDue()
            }
        }
        // Keep the running session's subject selected, so the next session defaults to it
        // (also after the ViewModel is recreated following process death).
        viewModelScope.launch {
            c.timer.active.collect { s -> if (s?.sessionType == SessionType.FOCUS) selectedSubject.value = s.subjectId }
        }
        // After a session ends, get the next mode ready.
        viewModelScope.launch {
            c.timer.events.collect { e ->
                // A discarded focus session doesn't count as a round, so stay on Focus.
                if (e is TimerEvent.Finished && (e.natural || e.savedMs > 0 || e.type != SessionType.FOCUS || skipping)) mode.value = e.next
                if (e is TimerEvent.Finished && e.natural && e.type == SessionType.FOCUS) completion = e
                skipping = false
            }
        }
    }

    /** Closes the Session Complete screen and starts the break, unless it already started by itself. */
    fun continueAfterCompletion() {
        val done = completion ?: return
        completion = null
        if (!done.nextStarted && ui.value.session == null) toggle()
    }

    fun setMode(type: SessionType) {
        if (ui.value.session == null) mode.value = type
    }

    fun selectSubject(id: Long?) {
        if (ui.value.session == null) selectedSubject.value = id
    }

    fun toggle() = launchSafe {
        val s = c.timer.active.first()
        when (s?.status) {
            null -> c.timer.start(mode.value, selectedSubject.value, c.profiles.get().minutesFor(mode.value) * 60_000L)
            SessionStatus.RUNNING -> c.timer.pause(s.id)
            SessionStatus.PAUSED -> c.timer.resume(s.id)
            else -> Unit
        }
    }

    /** Starts a focus session for [subjectId] (from Home, Subjects, Subject detail). Returns false if one is running. */
    fun startForSubject(subjectId: Long?, minutes: Int? = null) = launchSafe {
        if (c.timer.active.first() != null) {
            say("A session is already running")
            return@launchSafe
        }
        mode.value = SessionType.FOCUS
        selectedSubject.value = subjectId
        c.timer.start(SessionType.FOCUS, subjectId, (minutes ?: c.profiles.get().focusMinutes) * 60_000L)
    }

    fun stop(save: Boolean) = launchSafe {
        val s = c.timer.active.first() ?: return@launchSafe
        val saved = c.timer.stop(s.id, save)
        say(if (saved > 0) "Saved ${com.example.routineforge.core.time.TimeFormat.duration(saved)} of focus" else "Session discarded")
    }

    /** Ends the current session (focus of 1 min+ is saved) and moves on to the next mode. */
    fun skip() = launchSafe {
        val s = c.timer.active.first()
        if (s == null) {
            mode.value = if (mode.value == SessionType.FOCUS) SessionType.SHORT_BREAK else SessionType.FOCUS
            return@launchSafe
        }
        skipping = true
        c.timer.stop(s.id, save = true)
    }

    private var skipping = false

    /** Edits the default length of [type] (used for the next session). */
    fun setDuration(type: SessionType, minutes: Int) = launchSafe {
        require(minutes in 1..240) { "Choose between 1 and 240 minutes" }
        c.profiles.update {
            when (type) {
                SessionType.FOCUS -> it.copy(focusMinutes = minutes)
                SessionType.SHORT_BREAK -> it.copy(shortBreakMinutes = minutes)
                SessionType.LONG_BREAK -> it.copy(longBreakMinutes = minutes)
            }
        }
    }

}
