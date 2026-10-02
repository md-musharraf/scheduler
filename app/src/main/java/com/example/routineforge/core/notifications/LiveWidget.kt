package com.example.routineforge.core.notifications

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.routineforge.R
import com.example.routineforge.core.database.FocusSession
import com.example.routineforge.core.database.SessionStatus
import com.example.routineforge.domain.StopwatchState
import com.example.routineforge.domain.TimerMath

/** Home widget: the timer card (idle or active), plus the stopwatch card while it's in use. */
class LiveWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // If the process just started, the app's collector pushes the first state itself.
        LiveWidget.latest?.let { LiveWidget.push(context, it) }
    }
}

/** Pushed from FocusFlowApp whenever the timer, stopwatch, subjects or profile change. */
object LiveWidget {
    data class State(val session: FocusSession?, val subjectName: String?, val focusMinutes: Int, val stopwatch: StopwatchState)

    @Volatile var latest: State? = null
        private set

    fun push(context: Context, s: State) {
        latest = s
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, LiveWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        val timer = s.session?.let {
            Notifier.timerCard(context, it.sessionType, s.subjectName, it.status == SessionStatus.RUNNING, TimerMath.endsAt(it), TimerMath.remaining(it, now))
        } ?: Notifier.idleTimerCard(context, s.focusMinutes)
        val root = RemoteViews(context.packageName, R.layout.widget_live).apply {
            removeAllViews(R.id.widget_list)
            addView(R.id.widget_list, Notifier.liveViews(context, timer, R.layout.widget_live_item, showIcon = true))
            if (!s.stopwatch.isIdle) {
                addView(R.id.widget_list, Notifier.liveViews(context, Notifier.stopwatchCard(context, s.stopwatch, now), R.layout.widget_live_item, showIcon = true))
            }
        }
        manager.updateAppWidget(ids, root)
    }
}
