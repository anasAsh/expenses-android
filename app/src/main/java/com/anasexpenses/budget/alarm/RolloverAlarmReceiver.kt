package com.anasexpenses.budget.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anasexpenses.budget.di.AlarmReceiverEntryPoint
import com.anasexpenses.budget.domain.time.BudgetCycle
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RolloverAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val ep = EntryPointAccessors.fromApplication(context.applicationContext, AlarmReceiverEntryPoint::class.java)
                val cycleStartDay = ep.userPreferencesRepository().budgetCycleStartDay.first()
                val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycleStartDay)
                ep.categoryRepository().rolloverFromPreviousMonth(ym)
                ep.budgetAlarmScheduler().scheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
