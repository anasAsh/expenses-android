package com.anasexpenses.budget.data

import androidx.room.withTransaction
import com.anasexpenses.budget.alerts.BudgetAlertCoordinator
import com.anasexpenses.budget.data.local.BudgetDatabase
import com.anasexpenses.budget.data.local.dao.AlertEventDao
import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class CategoryRepository @Inject constructor(
    private val db: BudgetDatabase,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val alertEventDao: AlertEventDao,
    private val ruleDao: RuleDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val alertCoordinator: BudgetAlertCoordinator,
) {
    fun observeMonth(month: String) = categoryDao.observeByMonth(month)

    suspend fun getByMonth(month: String): List<CategoryEntity> = categoryDao.getByMonth(month)

    suspend fun addCategory(
        month: String,
        name: String,
        monthlyTargetMilliJod: Long,
        excludedFromSpend: Boolean,
    ): Long {
        val entity = CategoryEntity(
            month = month,
            name = name,
            monthlyTargetMilliJod = monthlyTargetMilliJod,
            excludedFromSpend = excludedFromSpend,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        return categoryDao.insert(entity)
    }

    suspend fun updateCategory(entity: CategoryEntity) {
        categoryDao.update(entity)
    }

    suspend fun sumIncludedTargetsMilli(month: String): Long =
        categoryDao.sumTargetsIncludedForMonth(month)

    /**
     * Removes the category and clears its id on **all** transactions (any month).
     * Drops merchant rules and alert history rows for this category id.
     */
    suspend fun deleteCategory(categoryId: Long) {
        val cat = categoryDao.getById(categoryId) ?: return
        val ymCat = YearMonth.parse(cat.month)
        val now = System.currentTimeMillis()
        db.withTransaction {
            transactionDao.clearCategoryAssignments(categoryId, now)
            alertEventDao.deleteByCategoryId(categoryId)
            ruleDao.deleteByCategoryId(categoryId)
            categoryDao.deleteById(categoryId)
        }
        alertCoordinator.refreshAlerts(ymCat)
        val ymSelected = userPreferencesRepository.selectedMonth.first()
        if (ymSelected != ymCat) {
            alertCoordinator.refreshAlerts(ymSelected)
        }
    }

    /** Prefill empty month from previous month (PRD §4.6). */
    suspend fun rolloverFromPreviousMonth(targetMonth: YearMonth) {
        val key = targetMonth.toString()
        if (categoryDao.getByMonth(key).isNotEmpty()) return
        val prev = targetMonth.minusMonths(1)
        val prevRows = categoryDao.getByMonth(prev.toString())
        val now = System.currentTimeMillis()
        for (c in prevRows) {
            categoryDao.insert(
                CategoryEntity(
                    month = key,
                    name = c.name,
                    monthlyTargetMilliJod = c.monthlyTargetMilliJod,
                    excludedFromSpend = c.excludedFromSpend,
                    createdAtEpochMillis = now,
                ),
            )
        }
    }
}
