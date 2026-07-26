package com.timereci.focus.ui.publish

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoRef
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.timer.FocusTimerController
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublishUiState(
    val stamp: String = "",
    val task: String = "",
    val focus: String = "",
    val comment: String = "",
    /** At most one photo — Session Complete manages a single optional photo (Merci v5 screen 5). */
    val photo: PhotoRef? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val controller: FocusTimerController,
    private val repository: FocusRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    // Captured once: the completed session powering this publish screen.
    private val completed = controller.state.value
    private val issuedAt = System.currentTimeMillis()

    /** True the first time this screen is shown for a session with no photo yet — the caller
     * opens the Choose Photo sheet automatically then, per the "ask for a photo" setting. */
    val askForPhotoAfterSession: StateFlow<Boolean> = settingsRepository.settings
        .map { it.askForPhotoAfterSession }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** True while the completion sound/vibration from finishing this session is still ringing. */
    val alarmActive: StateFlow<Boolean> = controller.alarmActive

    fun stopAlarm() = controller.stopAlarm()

    /** Photos from past sessions, offered as a quick "reuse this one" alternative. */
    val recentPhotos: StateFlow<List<PhotoRef>> = repository.observeRecentPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(
        PublishUiState(
            stamp = Formatters.stamp(issuedAt),
            task = completed.taskLabel,
            focus = Formatters.focus(completed.focusedMs),
            comment = completed.draftComment,
            photo = completed.backdropFileName?.let { PhotoRef(fileName = it) },
        ),
    )
    val ui: StateFlow<PublishUiState> = _ui.asStateFlow()

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            repository.photoStorageRef.import(uri)?.let { added ->
                _ui.value = _ui.value.copy(photo = added)
            }
        }
    }

    fun setExistingPhoto(ref: PhotoRef) {
        viewModelScope.launch {
            repository.reusePhoto(ref, 0)?.let { added ->
                _ui.value = _ui.value.copy(photo = added)
            }
        }
    }

    fun removePhoto() {
        _ui.value = _ui.value.copy(photo = null)
    }

    fun setComment(text: String) {
        _ui.value = _ui.value.copy(comment = text)
    }

    /**
     * Commit to History, clear the session, then hand back whatever is next in the todo
     * queue (if anything) so the caller can offer to continue straight into it.
     */
    fun store(onDone: (PlannedFocusEntity?) -> Unit) {
        if (_ui.value.saving) return
        _ui.value = _ui.value.copy(saving = true)
        viewModelScope.launch {
            repository.publish(
                ReceiptEntity(
                    issuedAtEpoch = issuedAt,
                    focusedMs = completed.focusedMs,
                    plannedMs = completed.plannedMs,
                    taskLabel = completed.taskLabel,
                    comment = _ui.value.comment.ifBlank { null },
                    photos = listOfNotNull(_ui.value.photo),
                ),
            )
            val next = repository.nextPlannedFocus()
            controller.reset()
            onDone(next)
        }
    }

    /** Discard the session without publishing — leaves no trace, like an abandon. */
    fun discard(onDone: (PlannedFocusEntity?) -> Unit) {
        viewModelScope.launch {
            val next = repository.nextPlannedFocus()
            controller.reset()
            onDone(next)
        }
    }
}
