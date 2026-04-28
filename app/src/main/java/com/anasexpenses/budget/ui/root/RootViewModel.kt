package com.anasexpenses.budget.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anasexpenses.budget.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface RootUiState {
    data object Loading : RootUiState
    data object NeedsOnboarding : RootUiState
    data object Ready : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    prefs: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<RootUiState> = prefs.onboardingComplete
        .map { done ->
            if (done) RootUiState.Ready else RootUiState.NeedsOnboarding
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RootUiState.Loading)
}
