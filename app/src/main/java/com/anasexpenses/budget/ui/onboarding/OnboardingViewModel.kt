package com.anasexpenses.budget.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import com.anasexpenses.budget.domain.money.JodMoney
import com.anasexpenses.budget.domain.time.BudgetCycle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    fun finishFirstCategory(
        name: String,
        targetText: String,
        excludedFromSpend: Boolean,
        onError: () -> Unit,
    ) {
        val n = name.trim()
        if (n.isEmpty()) {
            onError()
            return
        }
        val milli = try {
            JodMoney.parseToMilliJod(targetText.trim())
        } catch (_: Exception) {
            onError()
            return
        }
        if (milli <= 0L) {
            onError()
            return
        }
        viewModelScope.launch {
            val cycle = prefs.budgetCycleStartDay.first()
            val month = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycle).toString()
            categoryRepository.addCategory(month, n, milli, excludedFromSpend)
            prefs.setOnboardingComplete(true)
        }
    }

    fun markSmsSkipped() {
        viewModelScope.launch { prefs.setSmsPermissionSkipped(true) }
    }
}
