package com.timereci.focus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.TimerPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ResumeTarget { TIMER, PUBLISH }

/**
 * App-scoped view model. Restores the timer on launch and, if a session was in flight (or
 * finished while the app was dead), asks the navigator to jump straight to the timer or
 * publish screen.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val controller: FocusTimerController,
) : ViewModel() {

    private val _resume = MutableStateFlow<ResumeTarget?>(null)
    val resume = _resume.asStateFlow()
    private var consumed = false

    init {
        controller.restore()
        viewModelScope.launch {
            controller.state.collect { state ->
                if (consumed) return@collect
                _resume.value = when (state.phase) {
                    TimerPhase.RUNNING, TimerPhase.PAUSED -> ResumeTarget.TIMER
                    TimerPhase.COMPLETED -> ResumeTarget.PUBLISH
                    TimerPhase.IDLE -> null
                }
            }
        }
    }

    fun consumeResume() {
        consumed = true
        _resume.value = null
    }
}
