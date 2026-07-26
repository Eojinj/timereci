package com.timereci.focus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.ui.model.FeedBuilder
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.model.toSessionCard
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Content(
        val days: List<FeedDay>,
        val subtitle: String,
        /** Focused-vs-planned time across all sessions, as a whole percent. */
        val efficiencyPercent: Int,
        /** Average daily focused time over the most recent (up to 7) days, e.g. "1h 15m". */
        val weeklyAvgText: String,
    ) : FeedUiState
}

/** Which way the feed is shown: a per-day proportional strip, or the continuous vertical roll. */
enum class FeedViewMode { STRIP, ROLL }

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: FocusRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = repository.observeReceipts()
        .map { receipts ->
            val days = FeedBuilder.build(receipts)
            if (days.isEmpty()) {
                FeedUiState.Empty
            } else {
                val sessions = days.sumOf { it.sessions.size }
                val totalPlanned = receipts.sumOf { it.plannedMs }
                val totalFocused = receipts.sumOf { it.focusedMs }
                val efficiency = if (totalPlanned > 0) ((totalFocused * 100) / totalPlanned).toInt().coerceIn(0, 999) else 0
                val recentDays = days.take(7)
                val weeklyAvgMs = if (recentDays.isNotEmpty()) recentDays.sumOf { it.focusMs } / recentDays.size else 0L
                FeedUiState.Content(
                    days = days,
                    subtitle = "${days.size}일 · $sessions 세션",
                    efficiencyPercent = efficiency,
                    weeklyAvgText = Formatters.focusDuration(weeklyAvgMs),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState.Loading)

    /** All sessions newest-first, for the continuous roll view. */
    val rollSessions: StateFlow<List<SessionCard>> = repository.observeReceipts()
        .map { list -> list.sortedByDescending { it.issuedAtEpoch }.map { it.toSessionCard() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val photoAspect: StateFlow<PhotoAspect> = settingsRepository.settings
        .map { it.photoAspect }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoAspect.PORTRAIT)

    fun delete(receiptId: Long) {
        viewModelScope.launch {
            repository.getReceipt(receiptId)?.let { repository.deleteReceipt(it) }
        }
    }

    fun updateSession(receiptId: Long, task: String, comment: String) {
        viewModelScope.launch {
            repository.getReceipt(receiptId)?.let { receipt ->
                repository.updateReceipt(
                    receipt.copy(
                        taskLabel = task.trim(),
                        comment = comment.trim().ifBlank { null },
                    ),
                )
            }
        }
    }
}
