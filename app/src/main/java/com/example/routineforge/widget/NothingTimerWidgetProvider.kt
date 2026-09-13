package com.example.routineforge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import com.example.routineforge.service.TimerSessionManager

/**
 * 4-Number Nothing OS Style Timer Home Screen Widget Provider
 */
class NothingTimerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        const val EXTRA_TARGET_NAV = "extra_target_nav"
        const val EXTRA_PRESET_MIN = "extra_preset_min"
        const val NAV_SPECIAL_TIMER = "SPECIAL_TIMER"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, NothingTimerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (widgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_nothing_timer)
            val session = TimerSessionManager.session.value

            if (session.isRunning) {
                val totalSec = if (session.type == com.example.routineforge.service.ActiveTimerType.STOPWATCH) {
                    session.elapsedMillis / 1000
                } else {
                    session.remainingMillis / 1000
                }
                val m = (totalSec / 60)
                val s = totalSec % 60
                views.setTextViewText(R.id.widget_digit_mm, "%02d".format(m))
                views.setTextViewText(R.id.widget_digit_ss, "%02d".format(s))
                views.setTextViewText(R.id.widget_status, "RUNNING")
                views.setTextViewText(R.id.widget_title, session.title.ifEmpty { "TIMER" }.take(16))
                views.setImageViewResource(R.id.widget_btn_action, R.drawable.ic_notif_pause)
            } else {
                views.setTextViewText(R.id.widget_digit_mm, "05")
                views.setTextViewText(R.id.widget_digit_ss, "00")
                views.setTextViewText(R.id.widget_status, "STANDBY")
                views.setTextViewText(R.id.widget_title, "ROUTINEFORGE // 4-DIGIT")
                views.setImageViewResource(R.id.widget_btn_action, R.drawable.ic_notif_play)
            }

            // Click entire widget -> Open Special Timer
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_NAV, NAV_SPECIAL_TIMER)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                widgetId * 10,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_btn_action, openPendingIntent)

            // Preset 5M Click
            val preset5Intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_NAV, NAV_SPECIAL_TIMER)
                putExtra(EXTRA_PRESET_MIN, 5)
            }
            val pending5 = PendingIntent.getActivity(
                context,
                widgetId * 10 + 1,
                preset5Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_chip_5m, pending5)

            // Preset 15M Click
            val preset15Intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_NAV, NAV_SPECIAL_TIMER)
                putExtra(EXTRA_PRESET_MIN, 15)
            }
            val pending15 = PendingIntent.getActivity(
                context,
                widgetId * 10 + 2,
                preset15Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_chip_15m, pending15)

            // Preset 25M Click
            val preset25Intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_NAV, NAV_SPECIAL_TIMER)
                putExtra(EXTRA_PRESET_MIN, 25)
            }
            val pending25 = PendingIntent.getActivity(
                context,
                widgetId * 10 + 3,
                preset25Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_chip_25m, pending25)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
