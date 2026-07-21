package com.timereci.focus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.ui.model.FeedBuilder
import com.timereci.focus.ui.model.FeedDay
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.model.toSessionCard
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
    data class Content(val days: List<FeedDay>, val subtitle: String) : FeedUiState
}

/** Which way the feed is shown: the date-grouped grid or the continuous vertical roll. */
enum class FeedViewMode { GRID, ROLL }

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
                FeedUiState.Content(days, "${days.size}일 · $sessions 세션")
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

    val planned: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun deletePlanned(id: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(id) }
    }
}
