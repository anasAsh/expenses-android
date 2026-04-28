package com.anasexpenses.budget

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anasexpenses.budget.data.BudgetSeed
import com.anasexpenses.budget.notifications.BudgetNotificationChannels
import com.anasexpenses.budget.work.DailyBudgetWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class BudgetApplication : Application() {
    @Inject lateinit var budgetSeed: BudgetSeed

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        BudgetNotificationChannels.ensureCreated(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_budget_alerts",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyBudgetWorker>(24, TimeUnit.HOURS).build(),
        )
        applicationScope.launch(Dispatchers.IO) {
            budgetSeed.ensureArabBankEnglishTemplate()
        }
    }
}
