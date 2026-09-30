package com.example.routineforge.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import com.example.routineforge.data.FocusStats
import com.example.routineforge.data.RoutineRepository
import java.time.LocalDate

/** Single entry point for focus time: logs it and celebrates goals crossed by this chunk. */
object FocusTracker {
    private const val CHANNEL_ID = "focus_goals"
    private const val NOTIFICATION_ID = 4242

    fun record(context: Context, subject: String, seconds: Int) {
        if (seconds <= 0 || subject.isBlank()) return
        val repo = RoutineRepository.getInstance(context)
        val goals = repo.goals.value
        val name = goals.firstOrNull { it.title.equals(subject.trim(), ignoreCase = true) }?.title
            ?: subject.trim().take(40)
        val today = LocalDate.now().toEpochDay()
        val before = FocusStats.days(repo.focusLog.value)[today].orEmpty()
        repo.addFocus(name, seconds)
        val after = FocusStats.days(repo.focusLog.value)[today].orEmpty()

        val target = FocusStats.dailyTarget(goals)
        val goal = goals.firstOrNull { it.title == name }
        when {
            target > 0 && FocusStats.goalProgress(goals, before) < target && FocusStats.goalProgress(goals, after) >= target ->
                notify(context, "Today's goal complete 🎉", "${TimeFormatters.formatDurationSeconds(after.values.sum())} focused today. Great work!")
            goal != null && (before[name] ?: 0) < goal.targetSeconds && (after[name] ?: 0) >= goal.targetSeconds ->
                notify(context, "${goal.title} done ✓", "${TimeFormatters.formatDurationMinutes(goal.minutes)} goal reached. Keep going!")
        }
    }

    private fun notify(context: Context, title: String, body: String) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Goal complete", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Celebrates when a daily focus goal is reached"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 700)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            )
        }
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            MainActivity.launchIntent(context, MainActivity.NAV_DASHBOARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            nm.notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_timer)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(open)
                    .build()
            )
        } catch (_: SecurityException) {
            // Notifications denied; the dashboard still shows the goal as done.
        }
    }
}
