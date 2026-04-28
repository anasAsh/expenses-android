package com.anasexpenses.budget.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val month: YearMonth = YearMonth.now()

    val transactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.observeTransactionsForMonth(month)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.observeMonth(month.toString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertManualLine(line: String, categoryId: Long?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = transactionRepository.insertManualLine(line, categoryId)
            onResult(ok)
        }
    }

    fun assignCategory(
        transactionId: Long,
        categoryId: Long,
        createRule: Boolean,
        backApply: Boolean,
    ) {
        viewModelScope.launch {
            transactionRepository.assignCategoryAndOptionalRule(
                transactionId = transactionId,
                categoryId = categoryId,
                createRule = createRule,
                backApplyUncategorizedSameMonth = backApply,
            )
        }
    }
}
