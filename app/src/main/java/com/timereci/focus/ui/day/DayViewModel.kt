package com.timereci.focus.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.PhotoAspect
import com.timereci.focus.data.SettingsRepository
import com.timereci.focus.ui.Routes
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class DayUiState(
    val title: String = "",
    val summary: String = "",
    val sessions: List<SessionCard> = emptyList(),
)

@HiltViewModel
class DayViewModel @Inject constructor(
    repository: FocusRepository,
    settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val epochDay: Long = savedStateHandle.get<Long>(Routes.ARG_EPOCH_DAY) ?: 0L
    private val date: LocalDate = LocalDate.ofEpochDay(epochDay)

    val photoAspect: StateFlow<PhotoAspect> = settingsRepository.settings
        .map { it.photoAspect }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoAspect.PORTRAIT)

    val uiState: StateFlow<DayUiState> = repository.observeReceipts()
        .map { receipts ->
            val forDay = receipts
                .filter { Formatters.localDate(it.issuedAtEpoch) == date }
                .sortedByDescending { it.issuedAtEpoch }
            DayUiState(
                title = Formatters.dayTitle(date),
                summary = Formatters.daySummary(forDay.size, forDay.sumOf { it.focusedMs }),
                sessions = forDay.map { r ->
                    SessionCard(
                        id = r.id,
                        stamp = Formatters.stamp(r.issuedAtEpoch),
                        task = r.taskLabel,
                        focus = Formatters.focus(r.focusedMs),
                        photos = r.photos,
                        comment = r.comment,
                    )
                },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayUiState())
}
