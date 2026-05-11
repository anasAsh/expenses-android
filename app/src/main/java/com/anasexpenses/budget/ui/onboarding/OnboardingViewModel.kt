package com.anasexpenses.budget.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.CategoryRepository
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
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

    /** Idempotent: inserts starter categories for the labeled budget month if none exist yet. */
    fun ensureStarterCategoriesIfNeeded() {
        viewModelScope.launch {
            val cycle = prefs.budgetCycleStartDay.first()
            val month = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycle).toString()
            categoryRepository.ensureDefaultCategoriesForMonth(month)
            categoryRepository.ensureMerchantRuleSeedsForMonth(month)
        }
    }

    /** Ensures starter categories and merchant rule seeds, then marks onboarding complete. */
    fun finishOnboarding() {
        viewModelScope.launch {
            val cycle = prefs.budgetCycleStartDay.first()
            val month = BudgetCycle.labeledYearMonthForDate(LocalDate.now(), cycle).toString()
            categoryRepository.ensureDefaultCategoriesForMonth(month)
            categoryRepository.ensureMerchantRuleSeedsForMonth(month)
            prefs.setOnboardingComplete(true)
        }
    }

    fun markSmsSkipped() {
        viewModelScope.launch { prefs.setSmsPermissionSkipped(true) }
    }
}
