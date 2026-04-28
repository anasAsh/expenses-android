package com.anasexpenses.budget.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anasexpenses.budget.data.local.dao.AlertEventDao
import com.anasexpenses.budget.data.local.dao.BankTemplateDao
import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.AlertEventEntity
import com.anasexpenses.budget.data.local.entity.BankTemplateEntity
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.RuleEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        RuleEntity::class,
        BankTemplateEntity::class,
        AlertEventEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao
    abstract fun bankTemplateDao(): BankTemplateDao
    abstract fun alertEventDao(): AlertEventDao
}
