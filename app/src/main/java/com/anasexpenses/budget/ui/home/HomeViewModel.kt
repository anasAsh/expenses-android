package com.anasexpenses.budget.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.budget.BudgetRollup
import com.anasexpenses.budget.domain.money.JodMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySpendRow(
    val category: CategoryEntity,
    val spentMilliJod: Long,
    /** Non-dismissed transactions in this category for the visible month. */
    val transactionCount: Int,
)

/** Sum of monthly targets and spend for categories that count toward the month plan (!excluded, target > 0). */
data class MonthBudgetSummary(
    val totalTargetMilliJod: Long,
    val includedSpentMilliJod: Long,
) {
    val remainingMilliJod: Long get() = totalTargetMilliJod - includedSpentMilliJod
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val selectedMonth: StateFlow<YearMonth> =
        userPreferencesRepository.selectedMonth.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            YearMonth.now(),
        )

    val rows: StateFlow<List<CategorySpendRow>> =
        userPreferencesRepository.selectedMonth.flatMapLatest { month ->
            combine(
                categoryRepository.observeMonth(month.toString()),
                transactionRepository.observeTransactionsForMonth(month),
            ) { categories: List<CategoryEntity>, transactions: List<TransactionEntity> ->
                categories.map { c ->
                    val txnsForCat = transactions.filter {
                        it.categoryId == c.id && it.status != TxStatus.DISMISSED
                    }
                    CategorySpendRow(
                        c,
                        txnsForCat.sumOf { BudgetRollup.signedAmountMilliJod(it) },
                        txnsForCat.size,
                    )
                }.sortedWith(
                    compareByDescending<CategorySpendRow> { it.transactionCount }
                        .thenBy { it.category.name.lowercase(Locale.getDefault()) },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthBudgetSummary: StateFlow<MonthBudgetSummary> =
        rows.map { list ->
            var target = 0L
            var spent = 0L
            for (row in list) {
                val c = row.category
                val t = c.monthlyTargetMilliJod
                if (!c.excludedFromSpend && t > 0L) {
                    target += t
                    spent += row.spentMilliJod
                }
            }
            MonthBudgetSummary(totalTargetMilliJod = target, includedSpentMilliJod = spent)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MonthBudgetSummary(0L, 0L),
        )

    /** Spend in transactions with no category (non-dismissed), signed milli-JOD. */
    val unassignedSpendMilliJod: StateFlow<Long> =
        userPreferencesRepository.selectedMonth.flatMapLatest { month ->
            transactionRepository.observeTransactionsForMonth(month).map { transactions ->
                transactions
                    .filter { it.categoryId == null && it.status != TxStatus.DISMISSED }
                    .sumOf { BudgetRollup.signedAmountMilliJod(it) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun addCategory(
        name: String,
        targetText: String,
        excludedFromSpend: Boolean,
        onResult: (Boolean) -> Unit,
    ) {
        val n = name.trim()
        if (n.isEmpty()) {
            onResult(false)
            return
        }
        val milli = try {
            JodMoney.parseToMilliJod(targetText.trim())
        } catch (_: Exception) {
            onResult(false)
            return
        }
        if (milli <= 0L) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val month = userPreferencesRepository.selectedMonth.first()
            categoryRepository.addCategory(month.toString(), n, milli, excludedFromSpend)
            onResult(true)
        }
    }

    /**
     * Updates the selected budget month. When [yearMonth] has no categories yet, copies names,
     * targets, and excluded flags from the previous calendar month (same DB path as rollover alarm).
     */
    suspend fun selectMonth(yearMonth: YearMonth) {
        userPreferencesRepository.setSelectedMonth(yearMonth)
        categoryRepository.rolloverFromPreviousMonth(yearMonth)
    }

    fun deleteCategory(categoryId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
            onDone()
        }
    }
}
