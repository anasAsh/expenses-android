package com.anasexpenses.budget.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.money.JodMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** When non-null, list is restricted to this category for the selected month. */
    val filterCategoryId: Long? =
        savedStateHandle.get<Long>("categoryId")?.takeIf { it > 0L }

    /** Same budget month as Home (controls which transactions load). Shown on manual-add dialog. */
    val selectedMonth: StateFlow<YearMonth> =
        userPreferencesRepository.selectedMonth.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            YearMonth.now(),
        )

    /** Full month list (used for category spend ordering in dropdowns). */
    private val monthTransactionsAll: StateFlow<List<TransactionEntity>> =
        userPreferencesRepository.selectedMonth
            .flatMapLatest { month ->
                transactionRepository.observeTransactionsForMonth(month)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> =
        monthTransactionsAll
            .map { list ->
                val fid = filterCategoryId
                val filtered =
                    if (fid != null) list.filter { it.categoryId == fid } else list
                filtered.sortedWith(
                    compareByDescending<TransactionEntity> { it.dateEpochDay }
                        .thenByDescending { it.timeSecondOfDay }
                        .thenByDescending { it.id },
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val categoriesRaw: StateFlow<List<CategoryEntity>> =
        userPreferencesRepository.selectedMonth
            .flatMapLatest { month ->
                categoryRepository.observeMonth(month.toString())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sorted by number of categorized transactions this month (highest first). */
    val categories: StateFlow<List<CategoryEntity>> =
        combine(monthTransactionsAll, categoriesRaw) { txns, cats ->
            val transactionCountById = txns
                .filter {
                    it.categoryId != null &&
                        it.categoryId!! > 0L &&
                        it.status != TxStatus.DISMISSED
                }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.size }
            cats.sortedWith(
                compareByDescending<CategoryEntity> { transactionCountById[it.id] ?: 0 }
                    .thenBy { it.name.lowercase(Locale.getDefault()) },
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertManualEntry(
        merchant: String,
        amountJodText: String,
        categoryId: Long?,
        onResult: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val month = userPreferencesRepository.selectedMonth.first()
            val milli = runCatching {
                JodMoney.parseToMilliJod(amountJodText.trim().replace(',', '.'))
            }.getOrNull()
            val ok =
                !merchant.isBlank() &&
                    milli != null &&
                    milli > 0L &&
                    runCatching {
                        transactionRepository.insertManualEntry(
                            merchant.trim(),
                            milli,
                            categoryId,
                            month,
                        )
                    }.getOrDefault(false)
            withContext(Dispatchers.Main.immediate) {
                onResult(ok)
            }
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
