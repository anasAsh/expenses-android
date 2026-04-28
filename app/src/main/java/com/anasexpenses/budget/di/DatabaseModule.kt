package com.anasexpenses.budget.di

import android.content.Context
import androidx.room.Room
import com.anasexpenses.budget.data.local.BudgetDatabase
import com.anasexpenses.budget.data.local.dao.AlertEventDao
import com.anasexpenses.budget.data.local.dao.BankTemplateDao
import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBudgetDatabase(@ApplicationContext context: Context): BudgetDatabase =
        Room.databaseBuilder(context, BudgetDatabase::class.java, "budget.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTransactionDao(db: BudgetDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideCategoryDao(db: BudgetDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideRuleDao(db: BudgetDatabase): RuleDao = db.ruleDao()
    @Provides fun provideBankTemplateDao(db: BudgetDatabase): BankTemplateDao = db.bankTemplateDao()
    @Provides fun provideAlertEventDao(db: BudgetDatabase): AlertEventDao = db.alertEventDao()
}
