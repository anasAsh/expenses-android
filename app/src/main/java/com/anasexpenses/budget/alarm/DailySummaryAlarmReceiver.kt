package com.anasexpenses.budget.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anasexpenses.budget.di.AlarmReceiverEntryPoint
import com.anasexpenses.budget.domain.time.BudgetCycle
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DailySummaryAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val ep = EntryPointAccessors.fromApplication(context.applicationContext, AlarmReceiverEntryPoint::class.java)
                val cycleStartDay = ep.userPreferencesRepository().budgetCycleStartDay.first()
                val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycleStartDay)
                ep.budgetNotificationHelper().showSummary(
                    notificationId = abs("daily_summary_$ym".hashCode()) and 0x5fff_ffff,
                    title = "Budget summary",
                    body = "Review spending for $ym — tap to open transactions.",
                )
                // After quiet hours (summary 09:00); picks up thresholds skipped overnight without a new txn.
                runCatching { ep.budgetAlertCoordinator().refreshAlerts(ym) }
                ep.budgetAlarmScheduler().scheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
