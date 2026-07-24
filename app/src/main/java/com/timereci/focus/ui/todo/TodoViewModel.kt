package com.timereci.focus.ui.todo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PlannedFocusEntity
import com.timereci.focus.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A previously done task, offered as a quick "add it again" chip. */
data class RecentTask(val label: String, val minutes: Int)

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val planned: StateFlow<List<PlannedFocusEntity>> = repository.observePlannedFocus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** File name of the custom background photo for this screen, or null for the default. */
    val background: StateFlow<String?> = settingsRepository.settings
        .map { it.todoBackgroundFileName }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Labels the user has dismissed from the "다시 할까요?" row — in-memory only, so a fresh
     * app launch offers everything again. */
    private val dismissedRecent = MutableStateFlow<Set<String>>(emptySet())

    /** Recently completed tasks (most recent per distinct label), offered as "add it again"
     * chips — skips anything already sitting in today's queue or dismissed by the user. */
    val recentCompleted: StateFlow<List<RecentTask>> = combine(
        repository.observeReceipts(),
        planned,
        dismissedRecent,
    ) { receipts, queued, dismissed ->
        val queuedLabels = queued.map { it.label }.toSet()
        receipts
            .asSequence()
            .filter { it.taskLabel.isNotBlank() }
            .distinctBy { it.taskLabel }
            .filterNot { it.taskLabel in queuedLabels || it.taskLabel in dismissed }
            .take(6)
            .map { RecentTask(it.taskLabel, (it.plannedMs / 60_000L).toInt().coerceAtLeast(1)) }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismissRecent(label: String) {
        dismissedRecent.value = dismissedRecent.value + label
    }

    fun add(label: String, minutes: Int) {
        viewModelScope.launch {
            repository.addPlannedFocus(label.trim(), minutes * 60_000L)
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(id) }
    }

    /** Imports the picked photo into private storage and sets it as the background. */
    fun setBackground(uri: Uri) {
        viewModelScope.launch {
            val previous = background.value
            repository.photoStorageRef.import(uri)?.let { ref ->
                settingsRepository.setTodoBackground(ref.fileName)
                previous?.let { repository.photoStorageRef.delete(it) }
            }
        }
    }

    fun clearBackground() {
        viewModelScope.launch {
            val previous = background.value
            settingsRepository.setTodoBackground(null)
            previous?.let { repository.photoStorageRef.delete(it) }
        }
    }
}
