package com.haruchi.today.ui.quickstart

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haruchi.today.data.FocusRepository
import com.haruchi.today.data.PhotoRef
import com.haruchi.today.data.SettingsRepository
import com.haruchi.today.timer.FocusTimerController
import com.haruchi.today.ui.model.RecentTask
import com.haruchi.today.ui.util.QuickEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Quick Start's resolved input: what will actually be queued/started. */
data class QuickStartResolved(val label: String, val minutes: Int)

@HiltViewModel
class QuickStartViewModel @Inject constructor(
    private val repository: FocusRepository,
    private val controller: FocusTimerController,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text

    // Falls back to 25 unless a Recent chip set it — there's no length picker in the UI
    // anymore, so this only ever changes via fillFromRecent.
    private val _presetMinutes = MutableStateFlow(25)
    val presetMinutes: StateFlow<Int> = _presetMinutes

    private val _backdrop = MutableStateFlow<PhotoRef?>(null)
    val backdrop: StateFlow<PhotoRef?> = _backdrop

    /** Whether "Save to Today" files this as a task that survives being started. */
    private val _repeating = MutableStateFlow(false)
    val repeating: StateFlow<Boolean> = _repeating

    val recentPhotos: StateFlow<List<PhotoRef>> = repository.observeRecentPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Persisted (DataStore), not in-memory — otherwise a dismissed chip reappeared the moment
    // this screen was left and reopened, since a fresh QuickStartViewModel is created each time.
    private val dismissedRecent: Flow<Set<String>> = settingsRepository.settings
        .map { it.dismissedRecentLabels }

    /** Recently completed tasks, offered as one-tap "fill the field" chips. */
    val recentCompleted: StateFlow<List<RecentTask>> = combine(
        repository.observeReceipts(),
        dismissedRecent,
    ) { receipts, dismissed ->
        receipts
            .asSequence()
            .filter { it.taskLabel.isNotBlank() }
            .distinctBy { it.taskLabel }
            .filterNot { it.taskLabel in dismissed }
            .take(6)
            .map { RecentTask(it.taskLabel, (it.plannedMs / 60_000L).toInt().coerceAtLeast(1)) }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The live "Laundry 20" -> "Laundry · 20 min" preview. No number in the text falls back
     * to whichever length preset is selected (25 by default). */
    val resolved: StateFlow<QuickStartResolved> = combine(_text, _presetMinutes) { raw, preset ->
        val (label, minutesFromText) = QuickEntry.parse(raw)
        QuickStartResolved(label = label, minutes = minutesFromText ?: preset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickStartResolved("", 25))

    fun setText(value: String) { _text.value = value }
    fun fillFromRecent(task: RecentTask) {
        _text.value = task.label
        _presetMinutes.value = task.minutes
    }
    fun dismissRecent(label: String) {
        viewModelScope.launch { settingsRepository.dismissRecentLabel(label) }
    }
    fun setRepeating(value: Boolean) { _repeating.value = value }

    fun importBackdrop(uri: Uri) {
        viewModelScope.launch {
            repository.photoStorageRef.import(uri)?.let { _backdrop.value = it }
        }
    }

    fun useRecentBackdrop(ref: PhotoRef) {
        viewModelScope.launch {
            repository.reusePhoto(ref, ref.toneIndex)?.let { _backdrop.value = it }
        }
    }

    fun clearBackdrop() { _backdrop.value = null }

    /** Starts the session right away (no separate confirmation screen) — the caller navigates
     * straight to the (already-running) timer after calling this. */
    fun start() {
        val r = resolved.value
        // r.label is already correct as-is: blank for a bare "20" (just a number, no task
        // name), or the full text when QuickEntry couldn't parse a number out of it at all.
        controller.start(
            plannedMs = r.minutes * 60_000L,
            taskLabel = r.label,
            backdropFileName = _backdrop.value?.fileName,
        )
    }

    /**
     * Files the task on Today instead of starting it now. Marking it repeating is what makes
     * it reusable — it stays on the list every time you run it, so its stats build up.
     */
    fun saveToToday() {
        val r = resolved.value
        viewModelScope.launch {
            repository.addPlannedFocus(
                label = r.label,
                plannedMs = r.minutes * 60_000L,
                isRepeating = _repeating.value,
            )
        }
    }
}
