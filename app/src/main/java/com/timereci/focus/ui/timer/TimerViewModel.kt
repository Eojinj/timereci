package com.timereci.focus.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The Focus screen is running-only now — Today, Quick Start, Up Next and Break all start the
 * session (via [FocusTimerController]) themselves before ever navigating here, so there's no
 * pre-start setup state to own.
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val controller: FocusTimerController,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val timerState: StateFlow<TimerState> = controller.state

    val keepRunningWhileCommenting: StateFlow<Boolean> = settingsRepository.settings
        .map { it.keepRunningWhileCommenting }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun pause() = controller.pause()
    fun resume() = controller.resume()
    fun completeNow() = controller.completeNow()
    fun abandon() = controller.abandon()
    fun updateComment(text: String) = controller.updateComment(text)
}
