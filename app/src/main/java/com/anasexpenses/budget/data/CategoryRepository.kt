package com.anasexpenses.budget.data

import androidx.room.withTransaction
import com.anasexpenses.budget.alerts.BudgetAlertCoordinator
import com.anasexpenses.budget.data.local.BudgetDatabase
import com.anasexpenses.budget.data.local.dao.AlertEventDao
import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.dao.RuleDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.RuleEntity
import com.anasexpenses.budget.data.local.entity.RuleSource
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.category.DefaultCategorySeeds
import com.anasexpenses.budget.domain.merchant.MerchantRuleSeeds
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

    suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun getByMonth(month: String): List<CategoryEntity> = categoryDao.getByMonth(month)

    /**
     * If [month] has no categories yet, inserts [DefaultCategorySeeds] once. Safe to call repeatedly.
     */
    suspend fun ensureDefaultCategoriesForMonth(monthKey: String) {
        if (categoryDao.getByMonth(monthKey).isNotEmpty()) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            for (row in DefaultCategorySeeds.rows) {
                categoryDao.insert(
                    CategoryEntity(
                        month = monthKey,
                        name = row.name,
                        monthlyTargetMilliJod = row.monthlyTargetMilliJod,
                        excludedFromSpend = row.excludedFromSpend,
                        createdAtEpochMillis = now,
                    ),
                )
            }
        }
        alertCoordinator.refreshAlerts(YearMonth.parse(monthKey))
    }

    /**
     * Inserts or replaces merchant rules for [MerchantRuleSeeds] against categories in [monthKey]
     * matched by **exact** category name. Skips a seed if that category name is missing in the month.
     * Idempotent per token ([RuleDao.insertReplace]).
     */
    suspend fun ensureMerchantRuleSeedsForMonth(monthKey: String) {
        val cats = categoryDao.getByMonth(monthKey)
        val idByName = cats.associateBy({ it.name }, { it.id })
        val now = System.currentTimeMillis()
        for ((token, defaultCategoryName) in MerchantRuleSeeds.pairs) {
            val categoryId = idByName[defaultCategoryName] ?: continue
            ruleDao.insertReplace(
                RuleEntity(
                    merchantToken = token,
                    categoryId = categoryId,
                    source = RuleSource.SEED,
                    createdAtEpochMillis = now,
                ),
            )
        }
    }

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
        alertCoordinator.refreshAlerts(YearMonth.parse(entity.month))
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
