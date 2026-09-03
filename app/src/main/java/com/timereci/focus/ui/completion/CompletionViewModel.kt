package com.timereci.focus.ui.completion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.timer.SessionRecorder
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompletionUiState(
    val visible: Boolean = false,
    val task: String = "",
    val focus: String = "",
    val comment: String = "",
    val photo: PhotoRef? = null,
    val next: PlannedFocusEntity? = null,
)

/**
 * Drives the sheet shown over Today after a session completes. Everything here edits a row
 * that already exists — there is no "save" and no "discard", so dismissing the sheet without
 * touching anything still leaves the session recorded.
 */
@HiltViewModel
class CompletionViewModel @Inject constructor(
    private val recorder: SessionRecorder,
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
) : ViewModel() {

    private val _ui = MutableStateFlow(CompletionUiState())
    val ui: StateFlow<CompletionUiState> = _ui.asStateFlow()

    val alarmActive: StateFlow<Boolean> = controller.alarmActive

    /** Photos from past sessions, offered as a quick "reuse this one" alternative. */
    val recentPhotos: StateFlow<List<PhotoRef>> = repository.observeRecentPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var receiptId: Long? = null

    init {
        viewModelScope.launch {
            recorder.lastSavedId.collect { id ->
                if (id == null) {
                    receiptId = null
                    _ui.value = CompletionUiState()
                    return@collect
                }
                val receipt = repository.getReceipt(id) ?: return@collect
                receiptId = id
                _ui.value = CompletionUiState(
                    visible = true,
                    task = receipt.taskLabel,
                    focus = Formatters.focus(receipt.focusedMs),
                    comment = receipt.comment.orEmpty(),
                    photo = receipt.photos.firstOrNull(),
                    next = repository.nextPlannedFocus(excludingLabel = receipt.taskLabel),
                )
            }
        }
    }

    fun stopAlarm() = controller.stopAlarm()

    fun setComment(text: String) {
        _ui.value = _ui.value.copy(comment = text)
        persist { it.copy(comment = text.ifBlank { null }) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val added = repository.photoStorageRef.import(uri) ?: return@launch
            _ui.value = _ui.value.copy(photo = added)
            persist { it.copy(photos = listOf(added)) }
        }
    }

    fun setExistingPhoto(ref: PhotoRef) {
        viewModelScope.launch {
            val added = repository.reusePhoto(ref, 0) ?: return@launch
            _ui.value = _ui.value.copy(photo = added)
            persist { it.copy(photos = listOf(added)) }
        }
    }

    fun removePhoto() {
        _ui.value = _ui.value.copy(photo = null)
        persist { it.copy(photos = emptyList()) }
    }

    /** Starts the offered task, then closes the sheet so the timer screen is what's left. */
    fun startNext(task: PlannedFocusEntity) {
        controller.start(
            plannedMs = task.plannedMs,
            taskLabel = task.label,
            backdropFileName = null,
        )
        recorder.dismissSheet()
    }

    fun dismiss() = recorder.dismissSheet()

    private fun persist(edit: (ReceiptEntity) -> ReceiptEntity) {
        val id = receiptId ?: return
        viewModelScope.launch {
            repository.getReceipt(id)?.let { repository.updateReceipt(edit(it)) }
        }
    }
}
