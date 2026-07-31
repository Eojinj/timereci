package com.timereci.focus.ui.nextup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.model.toSessionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NextUpViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
) : ViewModel() {

    /** The live todo queue — just the head leads here; anything else waits. */
    val queue: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The session that was just saved, powering the "Session saved · N min · Task" header. */
    val justSaved: StateFlow<SessionCard?> = repository.observeReceipts()
        .map { receipts -> receipts.maxByOrNull { it.issuedAtEpoch }?.toSessionCard() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Starts [item] right now — consumes it from the queue and starts the session directly;
     * the caller just navigates to the (already-running) timer after calling this. */
    fun startNow(item: PlannedFocusEntity) {
        consume(item)
        val minutes = (item.plannedMs / 60_000L).toInt().coerceAtLeast(1)
        controller.start(plannedMs = minutes * 60_000L, taskLabel = item.label, backdropFileName = null)
    }

    /** Consumes [item] ahead of a break — the break screen starts it once the break ends. */
    fun consumeForBreak(item: PlannedFocusEntity) = consume(item)

    /** A one-off leaves the queue once it's been taken; a repeating task always stays. */
    private fun consume(item: PlannedFocusEntity) {
        if (!item.isRepeating) {
            viewModelScope.launch { repository.deletePlannedFocus(item.id) }
        }
    }
}
