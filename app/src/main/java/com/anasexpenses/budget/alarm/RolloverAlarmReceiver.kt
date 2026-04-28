package com.anasexpenses.budget.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anasexpenses.budget.di.AlarmReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RolloverAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val ep = EntryPointAccessors.fromApplication(context.applicationContext, AlarmReceiverEntryPoint::class.java)
                ep.categoryRepository().rolloverFromPreviousMonth(YearMonth.now())
                ep.budgetAlarmScheduler().scheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
