package com.haruchi.today.timer

import com.haruchi.today.data.FocusRepository
import com.haruchi.today.data.ReceiptEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saves a session the moment the timer completes, so no screen has to be visited for the
 * record to exist. The completion sheet then edits the row this produced, rather than
 * deciding whether to create one.
 */
@Singleton
class SessionRecorder @Inject constructor(
    private val controller: FocusTimerController,
    private val repository: FocusRepository,
) {

    private val _lastSavedId = MutableStateFlow<Long?>(null)

    /** Row id of the session saved most recently, which the completion sheet edits.
     * Null once the sheet has been dismissed. */
    val lastSavedId: StateFlow<Long?> = _lastSavedId.asStateFlow()

    /** Called once from the Application. Watches for completions for the process's lifetime. */
    fun attach(scope: CoroutineScope) {
        scope.launch {
            controller.state.collect { state ->
                if (state.phase != TimerPhase.COMPLETED) return@collect
                record(repository, state)?.let { _lastSavedId.value = it }
                controller.reset()
            }
        }
    }

    fun dismissSheet() {
        _lastSavedId.value = null
    }

    companion object {
        /**
         * Writes the row, or returns null if this session is already recorded. Kept in the
         * companion so it can be tested without a coroutine scope or a live controller.
         */
        suspend fun record(repository: FocusRepository, state: TimerState): Long? {
            if (repository.receiptExistsForSession(state.startedAtEpoch)) return null
            return repository.publish(
                ReceiptEntity(
                    issuedAtEpoch = System.currentTimeMillis(),
                    focusedMs = state.focusedMs,
                    plannedMs = state.plannedMs,
                    taskLabel = state.taskLabel,
                    comment = state.draftComment.ifBlank { null },
                    photos = emptyList(),
                    startedAtEpoch = state.startedAtEpoch,
                ),
            )
        }
    }
}
