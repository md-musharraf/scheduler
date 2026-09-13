package com.example.routineforge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RoutineTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            RoutineTimerService.ACTION_PLAY_PAUSE -> {
                RoutineTimerController.emitAction(RoutineTimerAction.TOGGLE_PLAY_PAUSE)
            }
            RoutineTimerService.ACTION_NEXT -> {
                RoutineTimerController.emitAction(RoutineTimerAction.NEXT_STEP)
            }
            RoutineTimerService.ACTION_STOP -> {
                RoutineTimerController.emitAction(RoutineTimerAction.STOP)
                if (context != null) {
                    RoutineTimerService.stop(context)
                }
            }
        }
    }
}
