package com.timereci.focus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.ui.model.FeedBuilder
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Content(
        val days: List<FeedDay>,
        val subtitle: String,
        val thisWeekText: String,
        val dayStreak: Int,
    ) : FeedUiState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    repository: FocusRepository,
) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = repository.observeReceipts()
        .map { receipts ->
            val days = FeedBuilder.build(receipts)
            if (days.isEmpty()) {
                FeedUiState.Empty
            } else {
                val sessions = days.sumOf { it.sessions.size }
                val monthName = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                val weekAgo = LocalDate.now().minusDays(6)
                val thisWeekMs = days.filter { it.date >= weekAgo }.sumOf { it.focusMs }
                FeedUiState.Content(
                    days = days,
                    subtitle = "$monthName · $sessions session${if (sessions == 1) "" else "s"}",
                    thisWeekText = Formatters.focusDuration(thisWeekMs),
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
