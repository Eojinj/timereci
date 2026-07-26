package com.timereci.focus.ui.nextup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BreakViewModel @Inject constructor(
    private val controller: FocusTimerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val breakMinutes: Int = savedStateHandle.get<Int>(Routes.ARG_BREAK_MINUTES) ?: 5
    private val task: String = savedStateHandle.get<String>(Routes.ARG_TASK).orEmpty()
    private val minutes: Int = savedStateHandle.get<Int>(Routes.ARG_MINUTES) ?: 25

    /** Starts the queued session that was waiting behind this break — called once the
     * countdown reaches zero (or is skipped); the caller then navigates to the timer. */
    fun startQueuedSession() {
        controller.start(plannedMs = minutes * 60_000L, taskLabel = task, backdropFileName = null)
    }
}
