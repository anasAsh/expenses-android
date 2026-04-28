package com.anasexpenses.budget.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import java.time.YearMonth

/** Daily refresh for predictive alerts and missed thresholds (PRD §9). */
class DailyBudgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val coordinator = EntryPointAccessors.fromApplication(
            applicationContext,
            BudgetWorkerEntryPoint::class.java,
        ).budgetAlertCoordinator()
        coordinator.refreshAlerts(YearMonth.now())
        return Result.success()
    }
}
