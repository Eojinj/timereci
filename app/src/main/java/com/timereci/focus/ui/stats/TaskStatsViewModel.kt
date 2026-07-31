package com.timereci.focus.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskStatsUiState(
    val stat: TaskStat? = null,
    val trend: List<TrendBar> = emptyList(),
    val sessions: List<SessionCard> = emptyList(),
)

@HiltViewModel
class TaskStatsViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val key: String = savedStateHandle[Routes.ARG_TASK_KEY] ?: ""

    val uiState: StateFlow<TaskStatsUiState> = repository.observeReceipts()
        .map { receipts ->
            val mine = TaskStatsBuilder.sessionsOf(receipts, key)
            TaskStatsUiState(
                stat = TaskStatsBuilder.build(mine).firstOrNull(),
                trend = TaskStatsBuilder.trend(receipts, key),
                sessions = mine.map { it.toSessionCard() },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskStatsUiState())

    /** Runs this task again at the length it was last run — the whole point of a reusable task. */
    fun startAgain() {
        val stat = uiState.value.stat ?: return
        controller.start(
            plannedMs = stat.typicalMinutes * 60_000L,
            taskLabel = stat.label,
            backdropFileName = null,
        )
    }

    /** Files it on Today as a repeating task, so it's one tap away from here on. */
    fun addToToday() {
        val stat = uiState.value.stat ?: return
        viewModelScope.launch {
            repository.addPlannedFocus(
                label = stat.label,
                plannedMs = stat.typicalMinutes * 60_000L,
                isRepeating = true,
            )
        }
    }
}
