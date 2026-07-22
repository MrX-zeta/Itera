package com.luis.itera.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El onboarding ya NO pide datos (peso/meta quedan en sus defaults, editables luego en
 * Ajustes → Perfil): es solo una presentación. "Saltar" y "Empezar" hacen lo mismo.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) : ViewModel() {

    fun onFinish(onDone: () -> Unit) {
        viewModelScope.launch {
            userPrefsRepository.setOnboardingCompleted(true)
            onDone()
        }
    }
}
