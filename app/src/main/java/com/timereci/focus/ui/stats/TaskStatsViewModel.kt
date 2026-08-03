package com.timereci.focus.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.TaskKey
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.ui.Routes
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.model.TaskStat
import com.timereci.focus.ui.model.TaskStatsBuilder
import com.timereci.focus.ui.model.TrendBar
import com.timereci.focus.ui.model.toSessionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskStatsUiState(
    val label: String = "",
    /** What the length field starts at: the last length used, else the saved task's own. */
    val defaultMinutes: Int = 25,
    /** Null until the task has actually been finished at least once. */
    val stat: TaskStat? = null,
    val trend: List<TrendBar> = emptyList(),
    val sessions: List<SessionCard> = emptyList(),
    val isFavorite: Boolean = false,
)

@HiltViewModel
class TaskStatsViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val key: String = savedStateHandle[Routes.ARG_TASK_KEY] ?: ""

    /**
     * Reads both history and the todo list: a favorite that has never been run has no receipts
     * to take its name or length from, and it still has to be startable from here.
     */
    val uiState: StateFlow<TaskStatsUiState> = combine(
        repository.observeReceipts(),
        repository.observePlannedFocus(),
    ) { receipts, planned ->
        val mine = TaskStatsBuilder.sessionsOf(receipts, key)
        val stat = TaskStatsBuilder.build(mine).firstOrNull()
        val saved = planned.firstOrNull { TaskKey.of(it.label) == key }
        TaskStatsUiState(
            // Raw and possibly blank: the "unnamed session" fallback is UI copy, so the
            // screen substitutes it rather than baking English into the state.
            label = stat?.label ?: saved?.label?.trim() ?: "",
            defaultMinutes = stat?.typicalMinutes
                ?: saved?.let { (it.plannedMs / 60_000L).toInt().coerceAtLeast(1) }
                ?: 25,
            stat = stat,
            trend = TaskStatsBuilder.trend(receipts, key),
            sessions = mine.map { it.toSessionCard() },
            isFavorite = saved?.isRepeating == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskStatsUiState())

    /** Runs this task again for [minutes] — the caller navigates to the running timer after. */
    fun startAgain(minutes: Int) {
        controller.start(
            plannedMs = minutes.coerceIn(1, 300) * 60_000L,
            taskLabel = uiState.value.label,
            backdropFileName = null,
        )
    }

    /** Files it on Today as a repeating task, so it's one tap away from here on. */
    fun addToToday() {
        val state = uiState.value
        if (state.isFavorite) return
        viewModelScope.launch {
            repository.addPlannedFocus(
                label = state.label,
                plannedMs = state.defaultMinutes * 60_000L,
                isRepeating = true,
            )
        }
    }
}
