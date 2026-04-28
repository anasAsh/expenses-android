package com.anasexpenses.budget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anasexpenses.budget.domain.time.BudgetCycle
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/** Daily refresh for predictive alerts and missed thresholds (PRD §9). */
class DailyBudgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext,
            BudgetWorkerEntryPoint::class.java,
        )
        val cycleStartDay = ep.userPreferencesRepository().budgetCycleStartDay.first()
        val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycleStartDay)
        ep.budgetAlertCoordinator().refreshAlerts(ym)
        return Result.success()
    }
}
