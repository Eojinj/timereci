package com.timereci.focus.ui.nextup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NextUpViewModel @Inject constructor(
    private val repository: FocusRepository,
) : ViewModel() {

    /** Removes the queued item once the user actually commits to starting it. */
    fun consume(plannedId: Long) {
        viewModelScope.launch { repository.deletePlannedFocus(plannedId) }
    }
}
