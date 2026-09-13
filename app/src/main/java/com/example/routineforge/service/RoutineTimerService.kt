package com.example.routineforge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.routineforge.MainActivity
import com.example.routineforge.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class RoutineTimerAction {
    TOGGLE_PLAY_PAUSE,
    NEXT_STEP,
    STOP
}

object RoutineTimerController {
    private val _actions = MutableSharedFlow<RoutineTimerAction>(extraBufferCapacity = 10)
    val actions: SharedFlow<RoutineTimerAction> = _actions.asSharedFlow()

    fun emitAction(action: RoutineTimerAction) {
        _actions.tryEmit(action)
    }
}

class RoutineTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "routine_timer_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_OR_UPDATE = "com.example.routineforge.action.START_OR_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.routineforge.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.routineforge.action.NEXT"
        const val ACTION_STOP = "com.example.routineforge.action.STOP"

        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_SUBTASK_TITLE = "extra_subtask_title"
        const val EXTRA_SUBTASK_REMAINING_SEC = "extra_subtask_remaining_sec"
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_STEP_INDEX = "extra_step_index"
        const val EXTRA_STEP_TOTAL_COUNT = "extra_step_total_count"
        const val EXTRA_CATEGORY_REMAINING_SEC = "extra_category_remaining_sec"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun update(
            context: Context,
            routineId: String,
            subtaskTitle: String,
            subtaskRemainingSec: Int,
            categoryName: String,
            stepIndex: Int,
            stepTotalCount: Int,
            categoryRemainingSec: Int,
            isPlaying: Boolean
        ) {
            val intent = Intent(context, RoutineTimerService::class.java).apply {
                action = ACTION_START_OR_UPDATE
                putExtra(EXTRA_ROUTINE_ID, routineId)
                putExtra(EXTRA_SUBTASK_TITLE, subtaskTitle)
                putExtra(EXTRA_SUBTASK_REMAINING_SEC, subtaskRemainingSec)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_STEP_INDEX, stepIndex)
                putExtra(EXTRA_STEP_TOTAL_COUNT, stepTotalCount)
                putExtra(EXTRA_CATEGORY_REMAINING_SEC, categoryRemainingSec)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                // Ignore background start restrictions if activity is paused
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RoutineTimerService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private var currentRoutineId: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                RoutineTimerController.emitAction(RoutineTimerAction.TOGGLE_PLAY_PAUSE)
            }
            ACTION_NEXT -> {
                RoutineTimerController.emitAction(RoutineTimerAction.NEXT_STEP)
            }
            ACTION_STOP -> {
                RoutineTimerController.emitAction(RoutineTimerAction.STOP)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_OR_UPDATE -> {
                currentRoutineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: currentRoutineId
                val subtaskTitle = intent.getStringExtra(EXTRA_SUBTASK_TITLE) ?: "Task"
                val subtaskRemainingSec = intent.getIntExtra(EXTRA_SUBTASK_REMAINING_SEC, 0)
                val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "Routine"
                val stepIndex = intent.getIntExtra(EXTRA_STEP_INDEX, 0)
                val stepTotalCount = intent.getIntExtra(EXTRA_STEP_TOTAL_COUNT, 1)
                val categoryRemainingSec = intent.getIntExtra(EXTRA_CATEGORY_REMAINING_SEC, 0)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)

                val notification = buildNotification(
                    routineId = currentRoutineId,
                    subtaskTitle = subtaskTitle,
                    subtaskRemainingSec = subtaskRemainingSec,
                    categoryName = categoryName,
                    stepIndex = stepIndex,
                    stepTotalCount = stepTotalCount,
                    categoryRemainingSec = categoryRemainingSec,
                    isPlaying = isPlaying
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(
        routineId: String,
        subtaskTitle: String,
        subtaskRemainingSec: Int,
        categoryName: String,
        stepIndex: Int,
        stepTotalCount: Int,
        categoryRemainingSec: Int,
        isPlaying: Boolean
    ): Notification {
        val min = subtaskRemainingSec / 60
        val sec = subtaskRemainingSec % 60
        val subtaskTimeStr = "%02d:%02d".format(min, sec)

        val catHours = categoryRemainingSec / 3600
        val catMins = (categoryRemainingSec % 3600) / 60
        val catSecs = categoryRemainingSec % 60
        val catTimeStr = if (catHours > 0) {
            "%02d:%02d:%02d".format(catHours, catMins, catSecs)
        } else {
            "%02d:%02d".format(catMins, catSecs)
        }

        val statusSymbol = if (isPlaying) "⏳" else "⏸"
        val titleText = "$statusSymbol $subtaskTimeStr  //  $subtaskTitle"
        val stepInfo = "Step ${stepIndex + 1}/$stepTotalCount"
        val contentText = "$categoryName • $stepInfo • $catTimeStr Total Left"

        // Open App Intent
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTINE_ID, routineId)
        }
        val pendingTapIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Play / Pause
        val playPauseIntent = Intent(this, RoutineTimerReceiver::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val pendingPlayPause = PendingIntent.getBroadcast(
            this,
            1,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Next Step
        val nextIntent = Intent(this, RoutineTimerReceiver::class.java).apply {
            action = ACTION_NEXT
        }
        val pendingNext = PendingIntent.getBroadcast(
            this,
            2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Stop
        val stopIntent = Intent(this, RoutineTimerReceiver::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getBroadcast(
            this,
            3,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIconBitmap = try {
            BitmapFactory.decodeResource(resources, R.drawable.app_brand_logo)
        } catch (_: Exception) {
            null
        }

        val remoteViews = android.widget.RemoteViews(packageName, R.layout.notification_timer_layout).apply {
            setTextViewText(R.id.notif_timer_text, subtaskTimeStr)
            setTextViewText(R.id.notif_subtask_title, subtaskTitle)
            setTextViewText(R.id.notif_category_info, "$categoryName • Step ${stepIndex + 1}/$stepTotalCount ($catTimeStr Left)")
            setImageViewResource(
                R.id.notif_btn_play_pause,
                if (isPlaying) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            )
            setOnClickPendingIntent(R.id.notif_btn_play_pause, pendingPlayPause)
            setOnClickPendingIntent(R.id.notif_btn_next, pendingNext)
            setOnClickPendingIntent(R.id.notif_btn_stop, pendingStop)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setSubText("ROUTINEFORGE // PROTOCOL")
            .setContentIntent(pendingTapIntent)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setOngoing(isPlaying)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setColor(0xFFD71921.toInt()) // Nothing Red accent
            .setColorized(false)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Routine Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows live subtask and category countdown timers with controls"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
