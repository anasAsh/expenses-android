package com.anasexpenses.budget.di

import com.anasexpenses.budget.alarm.BudgetAlarmScheduler
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.notifications.BudgetNotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AlarmReceiverEntryPoint {
    fun categoryRepository(): CategoryRepository
    fun budgetNotificationHelper(): BudgetNotificationHelper
    fun budgetAlarmScheduler(): BudgetAlarmScheduler
}
