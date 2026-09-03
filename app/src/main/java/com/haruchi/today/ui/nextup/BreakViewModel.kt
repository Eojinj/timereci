package com.haruchi.today.ui.nextup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.haruchi.today.ui.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BreakViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val breakMinutes: Int = savedStateHandle.get<Int>(Routes.ARG_BREAK_MINUTES) ?: 5
}
