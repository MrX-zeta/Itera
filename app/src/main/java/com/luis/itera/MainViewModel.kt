package com.luis.itera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPrefsRepository: UserPrefsRepository
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> =
        userPrefsRepository.getOnboardingCompleted()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}