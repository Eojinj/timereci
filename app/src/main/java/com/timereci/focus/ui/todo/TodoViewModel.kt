package com.timereci.focus.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.DEFAULT_DURATION_PRESETS
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: FocusRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val planned: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val durationPresets: StateFlow<List<Int>> = settingsRepository.settings
        .map { it.durationPresets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_DURATION_PRESETS)

    fun add(label: String, minutes: Int) {
        viewModelScope.launch {
            repository.addPlannedFocus(label.trim(), minutes * 60_000L)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(id) }
    }
}
