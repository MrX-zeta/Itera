package com.luis.itera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.presentation.widget.WidgetPinner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val widgetPinner: WidgetPinner
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> =
        userPrefsRepository.getOnboardingCompleted()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var pinAttempted = false

    /**
     * Dispara una única vez el diálogo del sistema para anclar el widget en la
     * pantalla de inicio. Se llama tras completar el onboarding: en MIUI/Xiaomi
     * los widgets de terceros quedan "sepultados" en el selector propio de la
     * capa, así que este anclado automático es la forma fiable de que aparezca
     * sin obligar al usuario a buscarlo. Solo se marca como hecho si el launcher
     * aceptó la petición, para poder reintentar en el siguiente arranque.
     */
    fun maybeAutoPinWidget() {
        if (pinAttempted) return
        pinAttempted = true
        viewModelScope.launch {
            if (userPrefsRepository.getWidgetPinRequested().first()) return@launch
            if (widgetPinner.requestPin()) {
                userPrefsRepository.setWidgetPinRequested(true)
            }
        }
    }
}
