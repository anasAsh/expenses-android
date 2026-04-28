package com.anasexpenses.budget.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anasexpenses.budget.di.AlarmReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, AlarmReceiverEntryPoint::class.java)
        ep.budgetAlarmScheduler().scheduleAll()
    }
}
