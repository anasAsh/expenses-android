package com.anasexpenses.budget.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val transactions: StateFlow<List<TransactionEntity>> =
        userPreferencesRepository.selectedMonth
            .flatMapLatest { month ->
                transactionRepository.observeTransactionsForMonth(month)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        userPreferencesRepository.selectedMonth
            .flatMapLatest { month ->
                categoryRepository.observeMonth(month.toString())
            }
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
