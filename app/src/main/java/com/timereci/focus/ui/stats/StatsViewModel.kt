package com.timereci.focus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.ui.model.TaskStat
import com.timereci.focus.ui.model.TaskStatsBuilder
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUiState(
    val tasks: List<TaskStat> = emptyList(),
    val totalSessions: Int = 0,
    val totalFocusMs: Long = 0L,
    val thisWeekMs: Long = 0L,
    /** The heaviest task's total, so every row's bar can be drawn relative to it. */
    val topTaskMs: Long = 0L,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: FocusRepository,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = repository.observeReceipts()
        .map { receipts ->
            val tasks = TaskStatsBuilder.build(receipts)
            val weekAgo = LocalDate.now().minusDays(6)
            StatsUiState(
                tasks = tasks,
                totalSessions = receipts.size,
                totalFocusMs = receipts.sumOf { it.focusedMs },
                thisWeekMs = receipts
                    .filter { Formatters.localDate(it.issuedAtEpoch) >= weekAgo }
                    .sumOf { it.focusedMs },
                topTaskMs = tasks.firstOrNull()?.totalFocusMs ?: 0L,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
