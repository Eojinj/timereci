package com.timereci.focus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.ui.model.FeedBuilder
import com.timereci.focus.ui.model.FeedDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Content(val days: List<FeedDay>, val subtitle: String) : FeedUiState
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
                FeedUiState.Content(days, "${days.size}일 · $sessions 세션")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState.Loading)
}
