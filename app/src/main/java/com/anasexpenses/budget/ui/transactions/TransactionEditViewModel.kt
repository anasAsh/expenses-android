package com.anasexpenses.budget.ui.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.TransactionRepository
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.time.BudgetCycle
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.data.local.entity.TransactionEntity
import com.anasexpenses.budget.data.local.entity.TxStatus
import com.anasexpenses.budget.data.local.entity.TxSource
import com.anasexpenses.budget.domain.PrdConstants
import com.anasexpenses.budget.domain.money.JodMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TransactionEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val transactionId: Long = checkNotNull(savedStateHandle.get<Long>("id")) {
        "transaction id"
    }

    val transaction: StateFlow<TransactionEntity?> = flow {
        emit(transactionRepository.getTransaction(transactionId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val categories: StateFlow<List<CategoryEntity>> = transaction
        .filterNotNull()
        .flatMapLatest { e ->
            userPreferencesRepository.budgetCycleStartDay.flatMapLatest { startDay ->
                val ym = BudgetCycle.labeledYearMonthForDate(LocalDate.ofEpochDay(e.dateEpochDay), startDay)
                categoryRepository.observeMonth(ym.toString())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(
        amountJodText: String,
        dateText: String,
        isRefund: Boolean,
        dismissed: Boolean,
        categoryId: Long?,
        onResult: (Boolean) -> Unit,
    ) {
        val entity = transaction.value
        if (entity == null) {
            onResult(false)
            return
        }
        val milli = try {
            JodMoney.parseToMilliJod(amountJodText.trim())
        } catch (_: Exception) {
            onResult(false)
            return
        }
        if (milli <= 0L) {
            onResult(false)
            return
        }
        val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
        if (date == null) {
            onResult(false)
            return
        }
        val status = if (dismissed) {
            TxStatus.DISMISSED
        } else {
            when (entity.status) {
                TxStatus.DISMISSED -> {
                    if (entity.source == TxSource.MANUAL) {
                        TxStatus.MANUAL
                    } else {
                        if (entity.confidence >= PrdConstants.CONFIDENCE_AUTO_MIN) TxStatus.AUTO else TxStatus.NEEDS_REVIEW
                    }
                }
                else -> entity.status
            }
        }
        val secondOfDay = entity.timeSecondOfDay
        val updated = entity.copy(
            amountMilliJod = milli,
            dateEpochDay = date.toEpochDay(),
            isRefund = isRefund,
            status = status,
            categoryId = categoryId,
            timeSecondOfDay = secondOfDay,
        )
        viewModelScope.launch {
            transactionRepository.updateTransaction(updated)
            onResult(true)
        }
    }
}
