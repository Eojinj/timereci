package com.haruchi.today.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haruchi.today.data.FocusRepository
import com.haruchi.today.ui.model.FeedBuilder
import com.haruchi.today.ui.model.FeedDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    /** Raw values only — the screen formats them, since the wording is language-dependent. */
    data class Content(
        val days: List<FeedDay>,
        val sessionCount: Int,
        val month: LocalDate,
        val thisWeekMs: Long,
        val dayStreak: Int,
    ) : FeedUiState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: FocusRepository,
) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = repository.observeReceipts()
        .map { receipts ->
            val days = FeedBuilder.build(receipts)
            if (days.isEmpty()) {
                FeedUiState.Empty
            } else {
                val weekAgo = LocalDate.now().minusDays(6)
                FeedUiState.Content(
                    days = days,
                    sessionCount = days.sumOf { it.sessions.size },
                    month = LocalDate.now(),
                    thisWeekMs = days.filter { it.date >= weekAgo }.sumOf { it.focusMs },
                    dayStreak = currentStreak(days),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState.Loading)

    /** Consecutive days (from the most recent) that have at least one session. */
    private fun currentStreak(days: List<FeedDay>): Int {
        if (days.isEmpty()) return 0
        var streak = 1
        var previous = days[0].epochDay
        for (i in 1 until days.size) {
            if (days[i].epochDay == previous - 1) {
                streak++
                previous = days[i].epochDay
            } else {
                break
            }
        }
        return streak
    }
}
