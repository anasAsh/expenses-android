package com.anasexpenses.budget.data

import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
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
