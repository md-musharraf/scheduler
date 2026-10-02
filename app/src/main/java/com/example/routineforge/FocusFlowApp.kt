package com.example.routineforge

import android.app.Application
import android.content.Context
import com.example.routineforge.core.database.AppDatabase
import com.example.routineforge.core.database.LegacyImport
import com.example.routineforge.core.notifications.LiveWidget
import com.example.routineforge.core.notifications.Notifier
import com.example.routineforge.data.CalendarRepository
import com.example.routineforge.data.FocusTimer
import com.example.routineforge.data.GoalRepository
import com.example.routineforge.data.NoteRepository
import com.example.routineforge.data.ProfileRepository
import com.example.routineforge.data.ReminderRepository
import com.example.routineforge.data.SessionRepository
import com.example.routineforge.data.Stopwatch
import com.example.routineforge.data.SubjectRepository
import com.example.routineforge.data.TaskRepository
import com.example.routineforge.data.TodaySummary
import com.example.routineforge.domain.FocusAnalytics
import com.example.routineforge.domain.StatsPeriod
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Manual dependency container: one instance of each repository for the whole process. */
class AppContainer(context: Context) {
    val db = AppDatabase.build(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val profiles = ProfileRepository(db.profileDao())
    val subjects = SubjectRepository(db.subjectDao())
    val tasks = TaskRepository(db.taskDao())
    val goals = GoalRepository(db.goalDao())
    val sessions = SessionRepository(db.sessionDao())
    val timer = FocusTimer(context, db.sessionDao(), db.subjectDao(), profiles, ::todaySummary)
    val stopwatch = Stopwatch(context)
    val notes = NoteRepository(context, db.noteDao())
    val reminders = ReminderRepository(context, db.reminderDao())
    val calendar = CalendarRepository(db.eventDao(), reminders)

    private suspend fun todaySummary(): TodaySummary {
        val range = StatsPeriod.DAY.range(LocalDate.now())
        val stats = sessions.stats(range).first()
        val target = FocusAnalytics.overallTarget(goals.goals.first(), subjects.subjects.first(), StatsPeriod.DAY, range)
        return TodaySummary(stats.sessionCount, stats.totalMs, if (target > 0) (stats.totalMs * 100 / (target * 60_000L)).toInt() else null)
    }
}

class FocusFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        container.scope.launch {
            LegacyImport.runOnce(this@FocusFlowApp, container)
            // Force-stop and some OEM "clear all" wipe alarms; re-arm them on every start.
            container.timer.restore()
            container.stopwatch.restore()
            container.reminders.rescheduleAll()
        }
        // Keep the home widget in sync with whatever the timer and stopwatch are doing.
        container.scope.launch {
            with(container) {
                combine(timer.active, subjects.subjects, profiles.profile, stopwatch.state) { s, subs, p, sw ->
                    LiveWidget.State(s, s?.subjectId?.let { id -> subs.firstOrNull { it.id == id }?.name }, p.focusMinutes, sw)
                }.distinctUntilChanged().collect { LiveWidget.push(this@FocusFlowApp, it) }
            }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as FocusFlowApp).container
