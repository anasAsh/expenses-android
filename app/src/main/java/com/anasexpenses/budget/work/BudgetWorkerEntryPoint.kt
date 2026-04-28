package com.anasexpenses.budget.work

import com.anasexpenses.budget.alerts.BudgetAlertCoordinator
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetWorkerEntryPoint {
    fun budgetAlertCoordinator(): BudgetAlertCoordinator
    fun userPreferencesRepository(): UserPreferencesRepository
}
