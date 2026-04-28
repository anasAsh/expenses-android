package com.anasexpenses.budget

import android.app.Application
import com.anasexpenses.budget.data.BudgetSeed
import com.anasexpenses.budget.notifications.BudgetNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BudgetApplication : Application() {
    @Inject lateinit var budgetSeed: BudgetSeed

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        BudgetNotificationChannels.ensureCreated(this)
        applicationScope.launch(Dispatchers.IO) {
            budgetSeed.ensureArabBankEnglishTemplate()
        }
    }
}
