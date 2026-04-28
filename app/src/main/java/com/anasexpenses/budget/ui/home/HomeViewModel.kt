package com.anasexpenses.budget.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.domain.budget.BudgetRollup
import com.anasexpenses.budget.domain.money.JodMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySpendRow(
    val category: CategoryEntity,
    val spentMilliJod: Long,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val month: YearMonth = YearMonth.now()

    val rows: StateFlow<List<CategorySpendRow>> = combine(
        categoryRepository.observeMonth(month.toString()),
        transactionRepository.observeTransactionsForMonth(month),
    ) { categories: List<CategoryEntity>, transactions: List<TransactionEntity> ->
        categories.map { c ->
            val spent = transactions
                .filter { it.categoryId == c.id && it.status != TxStatus.DISMISSED }
                .sumOf { BudgetRollup.signedAmountMilliJod(it) }
            CategorySpendRow(c, spent)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            categoryRepository.addCategory(month.toString(), n, milli, excludedFromSpend)
            onResult(true)
        }
    }
}
