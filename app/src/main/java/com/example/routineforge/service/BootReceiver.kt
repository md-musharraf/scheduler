package com.example.routineforge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped on reboot/update and shift with clock or time-zone changes, so re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> CalendarAlarmScheduler.rescheduleAllUpcoming(context)
        }
    }
}
