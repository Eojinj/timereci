package com.timereci.focus.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusSettings
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.ui.i18n.AppLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<FocusSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSettings())

    fun setDefaultDuration(ms: Long) = viewModelScope.launch {
        settingsRepository.setDefaultDuration(ms)
    }

    fun setKeepRunning(value: Boolean) = viewModelScope.launch {
        settingsRepository.setKeepRunningWhileCommenting(value)
    }


    fun setAlertWhenSessionEnds(value: Boolean) = viewModelScope.launch {
        settingsRepository.setAlertWhenSessionEnds(value)
    }

    fun setVibrateWhenSessionEnds(value: Boolean) = viewModelScope.launch {
        settingsRepository.setVibrateWhenSessionEnds(value)
    }

    fun setLanguage(value: AppLanguage) = viewModelScope.launch {
        settingsRepository.setLanguage(value)
    }
}
