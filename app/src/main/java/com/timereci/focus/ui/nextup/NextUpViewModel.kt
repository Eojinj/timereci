package com.timereci.focus.ui.nextup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PlannedFocusEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NextUpViewModel @Inject constructor(
    private val repository: FocusRepository,
) : ViewModel() {

    /** The live todo queue, browsable as a swipeable card stack. */
    val queue: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Removes the queued item once the user actually commits to starting it. */
    fun consume(plannedId: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(plannedId) }
    }
}
