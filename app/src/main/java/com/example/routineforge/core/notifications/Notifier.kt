package com.example.routineforge.core.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import com.example.routineforge.core.database.Reminder
import com.example.routineforge.core.database.SessionType
import com.example.routineforge.core.time.TimeFormat
import com.example.routineforge.data.TodaySummary
import com.example.routineforge.domain.StopwatchState

/** Notification channels and builders. Posting is skipped silently when permission is denied. */
object Notifier {
    const val CH_TIMER = "focus_timer"
    const val CH_DONE_SOUND = "timer_done_sound"
    const val CH_DONE_VIBRATE = "timer_done_vibrate"
    const val CH_DONE_SILENT = "timer_done_silent"
    const val CH_REMINDERS = "reminders_v2"

    const val ID_TIMER = 1001
    const val ID_TIMER_DONE = 1002
    const val ID_STOPWATCH = 1003
    const val ID_ENDING_SOON = 1004

    private const val COLOR_FOCUS = 0xFF1F5E45.toInt()
    private const val COLOR_BREAK = 0xFFA45F1E.toInt()
    private const val COLOR_STOPWATCH = 0xFF2F7BF6.toInt()
    private fun colorOf(type: SessionType) = if (type == SessionType.FOCUS) COLOR_FOCUS else COLOR_BREAK
    private fun iconOf(type: SessionType) = if (type == SessionType.FOCUS) R.drawable.ic_notification_timer else R.drawable.ic_cup

    private val LEGACY_CHANNELS = listOf(
        "routine_timer_channel_v2", "reminders_sound_vibe", "reminders_vibe_only",
        "reminders_sound_only", "calendar_routine_reminders_channel_v1"
    )

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        LEGACY_CHANNELS.forEach { nm.deleteNotificationChannel(it) }
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_TIMER, "Running timer", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows the focus timer and stopwatch while they run"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CH_DONE_SOUND, "Timer finished", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Alerts with sound and vibration when a session ends"
                    setSound(sound, attrs)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                },
                NotificationChannel(CH_DONE_VIBRATE, "Timer finished (vibrate)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Vibrates when a session ends"
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                },
                NotificationChannel(CH_DONE_SILENT, "Timer finished (silent)", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Silent notice when a session ends"
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Reminders and event alerts you schedule"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setSound(sound, attrs)
                    enableVibration(true)
                }
            )
        )
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)

    private fun openApp(context: Context, nav: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode, MainActivity.launchIntent(context, nav),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun timerAction(context: Context, action: String, requestCode: Int, type: SessionType? = null): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode, Intent(context, TimerReceiver::class.java).setAction(action).putExtra(TimerReceiver.EXTRA_TYPE, type?.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** What the shared live card (ongoing notification, home widget) shows. [tickBase] (elapsedRealtime) makes the time tick by itself. */
    class LiveCard(
        val icon: Int, val color: Int, val toggleBg: Int, val closeIcon: Int,
        val time: String, val subtitle: String, val running: Boolean,
        val tickBase: Long?, val countDown: Boolean,
        val toggle: PendingIntent, val close: PendingIntent?, val open: PendingIntent
    )

    fun liveViews(context: Context, card: LiveCard, layout: Int = R.layout.live_card, showIcon: Boolean = false) =
        RemoteViews(context.packageName, layout).apply {
            setImageViewResource(R.id.live_icon, card.icon)
            setInt(R.id.live_icon, "setColorFilter", card.color)
            setViewVisibility(R.id.live_icon, if (showIcon) View.VISIBLE else View.GONE)
            val ticking = card.tickBase != null
            setChronometer(R.id.live_chrono, card.tickBase ?: 0L, null, ticking)
            setChronometerCountDown(R.id.live_chrono, card.countDown)
            setTextViewText(R.id.live_time, card.time)
            setViewVisibility(R.id.live_chrono, if (ticking) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.live_time, if (ticking) View.GONE else View.VISIBLE)
            setTextViewText(R.id.live_subtitle, card.subtitle)
            setImageViewResource(R.id.live_toggle, if (card.running) R.drawable.ic_notif_pause else R.drawable.ic_notif_play)
            setContentDescription(R.id.live_toggle, if (card.running) "Pause" else "Start")
            setOnClickPendingIntent(R.id.live_toggle, card.toggle)
            setInt(R.id.live_toggle, "setBackgroundResource", card.toggleBg)
            setImageViewResource(R.id.live_close, card.closeIcon)
            setViewVisibility(R.id.live_close, if (card.close != null) View.VISIBLE else View.GONE)
            card.close?.let { setOnClickPendingIntent(R.id.live_close, it) }
            setOnClickPendingIntent(R.id.live_root, card.open)
        }

    fun timerCard(context: Context, type: SessionType, subjectName: String?, running: Boolean, endsAt: Long?, remainingMs: Long) = LiveCard(
        icon = iconOf(type), color = colorOf(type),
        toggleBg = if (type == SessionType.FOCUS) R.drawable.bg_live_circle_focus else R.drawable.bg_live_circle_break,
        closeIcon = R.drawable.ic_notif_stop,
        time = TimeFormat.clock(remainingMs),
        subtitle = listOfNotNull(if (running) null else "Paused", type.label, subjectName).joinToString(" · "),
        running = running,
        // +999 so the chronometer rounds up like TimeFormat.clock and the app's own countdown.
        tickBase = if (running && endsAt != null) SystemClock.elapsedRealtime() + (endsAt - System.currentTimeMillis()) + 999 else null,
        countDown = true,
        toggle = if (running) timerAction(context, TimerReceiver.ACTION_PAUSE, 11) else timerAction(context, TimerReceiver.ACTION_RESUME, 12),
        close = timerAction(context, TimerReceiver.ACTION_FINISH, 13),
        open = openApp(context, MainActivity.NAV_TIMER, 10)
    )

    /** Widget card when no session is active: play starts a focus session of the default length. */
    fun idleTimerCard(context: Context, focusMinutes: Int) = LiveCard(
        icon = R.drawable.ic_notification_timer, color = COLOR_FOCUS,
        toggleBg = R.drawable.bg_live_circle_focus, closeIcon = R.drawable.ic_notif_stop,
        time = TimeFormat.clock(focusMinutes * 60_000L),
        subtitle = "Focus · tap play to start",
        running = false, tickBase = null, countDown = true,
        toggle = timerAction(context, TimerReceiver.ACTION_START, 15, SessionType.FOCUS),
        close = null,
        open = openApp(context, MainActivity.NAV_TIMER, 10)
    )

    fun stopwatchCard(context: Context, s: StopwatchState, now: Long) = LiveCard(
        icon = R.drawable.ic_stopwatch, color = COLOR_STOPWATCH,
        toggleBg = R.drawable.bg_live_circle_accent, closeIcon = R.drawable.ic_notif_close,
        time = TimeFormat.stopwatch(s.elapsed(now)),
        subtitle = when {
            !s.running -> "Paused"
            s.laps.isNotEmpty() -> "Stopwatch · Lap ${s.laps.size + 1}"
            else -> "Stopwatch"
        },
        running = s.running,
        tickBase = if (s.running) SystemClock.elapsedRealtime() - s.elapsed(now) else null,
        countDown = false,
        toggle = timerAction(context, TimerReceiver.ACTION_SW_TOGGLE, 21),
        close = timerAction(context, TimerReceiver.ACTION_SW_RESET, 22),
        open = openApp(context, MainActivity.NAV_STOPWATCH, 20)
    )

    private fun showLive(context: Context, id: Int, card: LiveCard, title: String) = post(
        context, id,
        NotificationCompat.Builder(context, CH_TIMER)
            .setSmallIcon(card.icon)
            // Plain title/text stay as the fallback for lock screens and wearables.
            .setContentTitle(title)
            .setContentText("${card.time} · ${card.subtitle}")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(liveViews(context, card))
            .setContentIntent(card.open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            // Own group per card, so the system doesn't bundle timer + stopwatch into one collapsed summary.
            .setGroup("live_$id")
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(card.color)
    )

    /** Ongoing timer card; while running the system chronometer counts down to [endsAt]. */
    fun showTimer(context: Context, type: SessionType, subjectName: String?, running: Boolean, endsAt: Long?, remainingMs: Long) =
        showLive(context, ID_TIMER, timerCard(context, type, subjectName, running, endsAt, remainingMs), listOfNotNull(type.label, subjectName).joinToString(" · "))

    fun showStopwatch(context: Context, s: StopwatchState, now: Long) =
        showLive(context, ID_STOPWATCH, stopwatchCard(context, s, now), "Stopwatch")

    /** "Focus Session Complete!" with today's totals, or "Break Over!"; offers to start what comes next. */
    fun showTimerDone(
        context: Context, type: SessionType, plannedMs: Long, summary: TodaySummary?,
        next: SessionType, nextStarted: Boolean, sound: Boolean, vibrate: Boolean
    ) {
        val channel = when {
            sound -> CH_DONE_SOUND
            vibrate -> CH_DONE_VIBRATE
            else -> CH_DONE_SILENT
        }
        val focus = type == SessionType.FOCUS
        val text = if (focus) "You focused for ${TimeFormat.duration(plannedMs)}." else "Time to get back to work."
        val totals = summary?.let { t ->
            listOfNotNull(
                "${t.sessions} ${if (t.sessions == 1) "session" else "sessions"}",
                "${TimeFormat.duration(t.focusedMs)} today",
                t.goalPercent?.let { "$it% of daily goal" }
            ).joinToString(" · ")
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(iconOf(type))
            .setContentTitle(if (focus) "Focus Session Complete!" else "Break Over!")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(text, totals, "${next.label} started".takeIf { nextStarted }).joinToString("\n")))
            .setContentIntent(openApp(context, MainActivity.NAV_TIMER, 14))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(colorOf(type))
        if (!nextStarted) builder.addAction(iconOf(next), "Start ${next.label}", timerAction(context, TimerReceiver.ACTION_START, 16, next))
        builder.addAction(R.drawable.ic_notif_close, "Dismiss", timerAction(context, TimerReceiver.ACTION_DISMISS, 17))
        post(context, ID_TIMER_DONE, builder)
    }

    /** Gentle heads-up a minute before the session ends; it removes itself when time is up. */
    fun showEndingSoon(context: Context, type: SessionType, subjectName: String?, leftMs: Long) = post(
        context, ID_ENDING_SOON,
        NotificationCompat.Builder(context, CH_DONE_SILENT)
            .setSmallIcon(iconOf(type))
            .setContentTitle("${type.label} Ending Soon")
            .setContentText(listOfNotNull(if (type == SessionType.FOCUS) "1 minute left. Finish up!" else "1 minute left. Get ready to focus.", subjectName).joinToString(" · "))
            .setContentIntent(openApp(context, MainActivity.NAV_TIMER, 18))
            .setAutoCancel(true)
            .setTimeoutAfter(leftMs)
            .setColor(colorOf(type))
    )

    fun showReminder(context: Context, r: Reminder, occurrenceAt: Long) {
        val done = PendingIntent.getBroadcast(
            context, Alarms.requestCode(r.id, Alarms.SLOT_DONE),
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_DONE).putExtra(ReminderReceiver.EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context, Alarms.requestCode(r.id, Alarms.SLOT_SNOOZE_ACTION),
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SNOOZE).putExtra(ReminderReceiver.EXTRA_ID, r.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val body = r.description.ifBlank { TimeFormat.time(occurrenceAt) }
        post(
            context, notificationId(r.id),
            NotificationCompat.Builder(context, CH_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContentTitle(r.title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openApp(context, MainActivity.NAV_REMINDERS, Alarms.requestCode(r.id, Alarms.SLOT_OPEN)))
                .setAutoCancel(true)
                .setPriority(if (r.priority >= 2) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF5B5FEF.toInt())
                .addAction(R.drawable.ic_notif_close, "Done", done)
                .addAction(R.drawable.ic_notification_timer, "Snooze 10 min", snooze)
        )
    }

    fun notificationId(reminderId: Long): Int = 5000 + (reminderId % 1_000_000).toInt()
}
