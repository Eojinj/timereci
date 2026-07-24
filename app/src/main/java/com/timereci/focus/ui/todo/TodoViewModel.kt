package com.timereci.focus.ui.todo

import android.net.Uri
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
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val planned: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val durationPresets: StateFlow<List<Int>> = settingsRepository.settings
        .map { it.durationPresets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_DURATION_PRESETS)

    /** File name of the custom background photo for this screen, or null for the default. */
    val background: StateFlow<String?> = settingsRepository.settings
        .map { it.todoBackgroundFileName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(label: String, minutes: Int) {
        viewModelScope.launch {
            repository.addPlannedFocus(label.trim(), minutes * 60_000L)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(id) }
    }

    /** Imports the picked photo into private storage and sets it as the background. */
    fun setBackground(uri: Uri) {
        viewModelScope.launch {
            val previous = background.value
            repository.photoStorageRef.import(uri)?.let { ref ->
                settingsRepository.setTodoBackground(ref.fileName)
                previous?.let { repository.photoStorageRef.delete(it) }
            }
        }
    }

    fun clearBackground() {
        viewModelScope.launch {
            val previous = background.value
            settingsRepository.setTodoBackground(null)
            previous?.let { repository.photoStorageRef.delete(it) }
        }
    }
}
