package com.timereci.focus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.TimerPhase
import com.timereci.focus.ui.i18n.English
import com.timereci.focus.ui.i18n.Strings
import com.timereci.focus.ui.i18n.stringsFor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where a relaunch should land. Completion no longer has a destination of its own — the
 * session is already saved, and the completion sheet rides on top of whatever is showing. */
enum class ResumeTarget { TIMER }

/**
 * App-scoped view model. Restores the timer on launch and, if a session was in flight, asks
 * the navigator to jump straight to the timer.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val controller: FocusTimerController,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    /** The whole UI's copy, resolved once here and provided down the tree via LocalStrings. */
    val strings: StateFlow<Strings> = settingsRepository.settings
        .map { stringsFor(it.language) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), English)

    private val _resume = MutableStateFlow<ResumeTarget?>(null)
    val resume = _resume.asStateFlow()
    private var consumed = false

    /** True while a session is running/paused — the bottom tab bar hides so the timer's own
     * stop/complete controls own the bottom of the screen (and you can't tab away mid-session). */
    val timerActive: StateFlow<Boolean> = controller.state
        .map { it.phase == TimerPhase.RUNNING || it.phase == TimerPhase.PAUSED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        controller.restore()
        viewModelScope.launch {
            controller.state.collect { state ->
                if (consumed) return@collect
                _resume.value = when (state.phase) {
                    TimerPhase.RUNNING, TimerPhase.PAUSED -> ResumeTarget.TIMER
                    TimerPhase.COMPLETED -> null
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
