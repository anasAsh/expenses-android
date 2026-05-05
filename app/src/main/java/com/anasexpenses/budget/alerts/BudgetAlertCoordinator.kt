package com.anasexpenses.budget.alerts

import com.anasexpenses.budget.data.local.dao.AlertEventDao
import com.anasexpenses.budget.data.local.dao.CategoryDao
import com.anasexpenses.budget.data.local.dao.TransactionDao
import com.anasexpenses.budget.data.local.entity.AlertEventEntity
import com.anasexpenses.budget.data.local.entity.AlertThresholdType
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.alerts.PredictiveEvaluator
import com.anasexpenses.budget.domain.alerts.SmallCategoryGate
import com.anasexpenses.budget.domain.time.BudgetCycle
import com.anasexpenses.budget.notifications.BudgetNotificationHelper
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class BudgetAlertCoordinator @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val alertEventDao: AlertEventDao,
    private val notifications: BudgetNotificationHelper,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    suspend fun refreshAlerts(month: YearMonth) {
        val cycleStartDay = userPreferencesRepository.budgetCycleStartDay.first()
        evaluateThresholds(month, cycleStartDay)
        evaluatePredictive(month, cycleStartDay)
    }

    private suspend fun evaluateThresholds(month: YearMonth, cycleStartDay: Int) {
        val monthStr = month.toString()
        val totalBudgetMilli = categoryDao.sumTargetsIncludedForMonth(monthStr)
        val range = BudgetCycle.epochDayRangeInclusive(month, cycleStartDay)
        val top = categoryDao.getByMonth(monthStr)
            .filter { !it.excludedFromSpend }
            .sortedByDescending { it.monthlyTargetMilliJod }
            .take(5)

        val pendingAlerts = mutableListOf<Triple<Int, String, String>>()

        for (c in top) {
            val target = c.monthlyTargetMilliJod
            if (target <= 0L) continue
            if (SmallCategoryGate.shouldSuppressPush(target, totalBudgetMilli)) continue

            val spent = transactionDao.sumSignedMilliJodForCategoryInRange(c.id, range.first, range.last) ?: 0L

            val tiers = listOf(
                Triple(AlertThresholdType.T100, 100L, "Budget exceeded"),
                Triple(AlertThresholdType.T85, 85L, "Approaching limit"),
                Triple(AlertThresholdType.T70, 70L, "Usage notice"),
            )
            for ((type, pct, tierTitle) in tiers) {
                if (spent * 100L < target * pct) continue
                if (alertEventDao.countFor(c.id, monthStr, type) > 0) continue

                alertEventDao.insert(
                    AlertEventEntity(
                        categoryId = c.id,
                        month = monthStr,
                        thresholdType = type,
                        sentAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
                val body = "${c.name}: spent ${spent / 1000.0} / ${target / 1000.0} JOD"
                val notifId = stableNotifId(c.id, monthStr, type)
                pendingAlerts.add(Triple(notifId, tierTitle, body))
            }
        }

        when {
            pendingAlerts.isEmpty() -> Unit
            pendingAlerts.size == 1 -> {
                val (id, title, body) = pendingAlerts.first()
                notifications.showBudgetAlert(id, title, body)
            }
            else -> {
                val pairs = pendingAlerts.map { it.second to it.third }
                notifications.showBudgetAlertDigest(monthStr, pairs)
            }
        }
    }

    private suspend fun evaluatePredictive(month: YearMonth, cycleStartDay: Int) {
        val today = LocalDate.now()
        if (BudgetCycle.labeledYearMonthForDate(today, cycleStartDay) != month) return
        val monthStr = month.toString()
        val totalBudgetMilli = categoryDao.sumTargetsIncludedForMonth(monthStr)
        val range = BudgetCycle.epochDayRangeInclusive(month, cycleStartDay)
        val top = categoryDao.getByMonth(monthStr)
            .filter { !it.excludedFromSpend }
            .sortedByDescending { it.monthlyTargetMilliJod }
            .take(5)

        val dayOfCycle = BudgetCycle.dayOfCycle(today, month, cycleStartDay) ?: return
        val dim = BudgetCycle.daysInCycle(month, cycleStartDay)

        for (c in top) {
            val target = c.monthlyTargetMilliJod
            if (target <= 0L) continue
            if (SmallCategoryGate.shouldSuppressPush(target, totalBudgetMilli)) continue

            val spent = transactionDao.sumSignedMilliJodForCategoryInRange(c.id, range.first, range.last) ?: 0L
            val projected = PredictiveEvaluator.projectedMonthEndSpend(spent, dayOfCycle, dim)
            if (!PredictiveEvaluator.exceedsPredictiveThreshold(projected, target)) continue
            if (alertEventDao.countFor(c.id, monthStr, AlertThresholdType.PREDICTIVE) > 0) continue

            alertEventDao.insert(
                AlertEventEntity(
                    categoryId = c.id,
                    month = monthStr,
                    thresholdType = AlertThresholdType.PREDICTIVE,
                    sentAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            notifications.showBudgetAlert(
                notificationId = stableNotifId(c.id, monthStr, AlertThresholdType.PREDICTIVE),
                title = "Spending pace",
                body = "${c.name}: at this pace you may exceed budget this month.",
            )
        }
    }

    private fun stableNotifId(categoryId: Long, month: String, thresholdType: String): Int {
        val raw = categoryId xor month.hashCode().toLong() xor thresholdType.hashCode().toLong()
        return (raw and 0x7fff_ffffL).toInt()
    }
}
