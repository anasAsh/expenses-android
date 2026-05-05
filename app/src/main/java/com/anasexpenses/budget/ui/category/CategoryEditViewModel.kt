package com.anasexpenses.budget.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.local.entity.CategoryEntity
import com.anasexpenses.budget.domain.money.JodMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class CategoryEditUiState {
    data object Loading : CategoryEditUiState()
    data object NotFound : CategoryEditUiState()
    data class Ready(val entity: CategoryEntity) : CategoryEditUiState()
}

sealed class CategoryEditSaveResult {
    data object Success : CategoryEditSaveResult()
    data object InvalidName : CategoryEditSaveResult()
    data object InvalidAmount : CategoryEditSaveResult()
    data object DuplicateName : CategoryEditSaveResult()
}

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val categoryId: Long = checkNotNull(savedStateHandle.get<Long>("categoryId")) {
        "categoryId"
    }

    val uiState: StateFlow<CategoryEditUiState> =
        flow {
            emit(CategoryEditUiState.Loading)
            when (val c = categoryRepository.getById(categoryId)) {
                null -> emit(CategoryEditUiState.NotFound)
                else -> emit(CategoryEditUiState.Ready(c))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryEditUiState.Loading)

    fun save(
        name: String,
        targetText: String,
        excludedFromSpend: Boolean,
        onResult: (CategoryEditSaveResult) -> Unit,
    ) {
        val n = name.trim()
        if (n.isEmpty()) {
            onResult(CategoryEditSaveResult.InvalidName)
            return
        }
        val milli = try {
            JodMoney.parseToMilliJod(targetText.trim())
        } catch (_: Exception) {
            onResult(CategoryEditSaveResult.InvalidAmount)
            return
        }
        if (milli <= 0L) {
            onResult(CategoryEditSaveResult.InvalidAmount)
            return
        }
        viewModelScope.launch {
            val current = categoryRepository.getById(categoryId)
            if (current == null) {
                onResult(CategoryEditSaveResult.InvalidName)
                return@launch
            }
            val dup = categoryRepository.getByMonth(current.month).any {
                it.id != categoryId && it.name.trim().equals(n, ignoreCase = true)
            }
            if (dup) {
                onResult(CategoryEditSaveResult.DuplicateName)
                return@launch
            }
            categoryRepository.updateCategory(
                current.copy(
                    name = n,
                    monthlyTargetMilliJod = milli,
                    excludedFromSpend = excludedFromSpend,
                ),
            )
            onResult(CategoryEditSaveResult.Success)
        }
    }
}
