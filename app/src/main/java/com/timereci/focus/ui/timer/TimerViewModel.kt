package com.timereci.focus.ui.timer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.TimerState
import com.timereci.focus.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Preset durations offered before a session starts (minutes). */
val DURATION_PRESETS = listOf(15, 25, 45, 60, 90)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val controller: FocusTimerController,
    private val repository: FocusRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = controller.state

    val keepRunningWhileCommenting: StateFlow<Boolean> = settingsRepository.settings
        .map { it.keepRunningWhileCommenting }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // ---- Pre-start setup ----
    private val _durationMs = MutableStateFlow(25 * 60_000L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _taskLabel = MutableStateFlow("")
    val taskLabel: StateFlow<String> = _taskLabel

    private val _backdrop = MutableStateFlow<PhotoRef?>(null)
    val backdrop: StateFlow<PhotoRef?> = _backdrop

    private val _commentSheetOpen = MutableStateFlow(false)
    val commentSheetOpen: StateFlow<Boolean> = _commentSheetOpen

    init {
        // Prefill from a planned focus, if we arrived here by tapping one.
        val argTask = savedStateHandle.get<String>(Routes.ARG_TASK).orEmpty()
        val argMinutes = savedStateHandle.get<Int>(Routes.ARG_MINUTES) ?: 0
        if (argTask.isNotBlank()) _taskLabel.value = argTask
        if (argMinutes > 0) {
            _durationMs.value = argMinutes * 60_000L
        } else {
            viewModelScope.launch {
                _durationMs.value = settingsRepository.settings.first().defaultDurationMs
            }
        }
    }

    fun setDuration(ms: Long) { _durationMs.value = ms }
    fun setTask(text: String) { _taskLabel.value = text }

    fun importBackdrop(uri: Uri) {
        viewModelScope.launch {
            repository.photoStorageRef.import(uri)?.let { _backdrop.value = it }
        }
    }

    fun clearBackdrop() { _backdrop.value = null }

    fun start() {
        controller.start(
            plannedMs = _durationMs.value,
            taskLabel = _taskLabel.value.trim(),
            backdropFileName = _backdrop.value?.fileName,
        )
    }

    fun pause() = controller.pause()
    fun resume() = controller.resume()
    fun completeNow() = controller.completeNow()
    fun abandon() = controller.abandon()

    fun openComment() { _commentSheetOpen.value = true }
    fun closeComment() { _commentSheetOpen.value = false }
    fun updateComment(text: String) = controller.updateComment(text)
}
