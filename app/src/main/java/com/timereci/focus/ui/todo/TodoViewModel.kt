package com.timereci.focus.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Today's completed-sessions footer, e.g. "2 sessions completed today · 1h 10m focused." */
data class TodaySummary(val sessionCount: Int, val focusedMs: Long)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
) : ViewModel() {

    val planned: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todaySummary: StateFlow<TodaySummary> = repository.observeReceipts()
        .map { receipts ->
            val today = LocalDate.now()
            val todays = receipts.filter { Formatters.localDate(it.issuedAtEpoch) == today }
            TodaySummary(todays.size, todays.sumOf { it.focusedMs })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodaySummary(0, 0L))

    fun remove(id: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(id) }
    }

    fun update(item: PlannedFocusEntity, label: String, minutes: Int, isRepeating: Boolean) {
        viewModelScope.launch {
            repository.updatePlannedFocus(
                item.copy(
                    label = label.trim(),
                    plannedMs = minutes * 60_000L,
                    isRepeating = isRepeating,
                ),
            )
        }
    }

    /**
     * Starts the session directly (no confirmation screen) — the caller just navigates to the
     * (already-running) timer right after calling this. A one-off is consumed by starting it;
     * a repeating task stays put so it can be run again tomorrow and keep building up stats.
     */
    fun startItem(item: PlannedFocusEntity) {
        if (!item.isRepeating) {
            viewModelScope.launch { repository.deletePlannedFocus(item.id) }
        }
        val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
        controller.start(plannedMs = minutes * 60_000L, taskLabel = item.label, backdropFileName = null)
    }
}
