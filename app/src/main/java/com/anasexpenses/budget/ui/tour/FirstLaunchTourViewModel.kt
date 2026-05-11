package com.anasexpenses.budget.ui.tour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FirstLaunchTourViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    val tourCompleted: StateFlow<Boolean> = prefs.firstLaunchTourCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    fun markTourCompleted() {
        viewModelScope.launch {
            prefs.setFirstLaunchTourCompleted(true)
        }
    }
}
