package com.anasexpenses.budget.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules exact clock alarms for rollover (00:05) and daily summary (09:00) local time. */
@Singleton
class BudgetAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)!!

    fun scheduleAll() {
        scheduleAt(HOUR_ROLLOVER, MIN_ROLLOVER, RC_ROLLOVER, RolloverAlarmReceiver::class.java)
        scheduleAt(HOUR_SUMMARY, MIN_SUMMARY, RC_SUMMARY, DailySummaryAlarmReceiver::class.java)
    }

    private fun scheduleAt(hour: Int, minute: Int, requestCode: Int, receiver: Class<*>) {
        val trigger = nextCalendarMillis(hour, minute)
        val tick = Intent(context, receiver)
        val showPi = PendingIntent.getBroadcast(
            context,
            requestCode + 1000,
            tick,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val op = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(trigger, showPi),
                op,
            )
        } else {
            @Suppress("DEPRECATION")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger, op)
        }
    }

    private fun nextCalendarMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    companion object {
        private const val HOUR_ROLLOVER = 0
        private const val MIN_ROLLOVER = 5
        private const val HOUR_SUMMARY = 9
        private const val MIN_SUMMARY = 0
        private const val RC_ROLLOVER = 0xB001
        private const val RC_SUMMARY = 0xB002
    }
}
