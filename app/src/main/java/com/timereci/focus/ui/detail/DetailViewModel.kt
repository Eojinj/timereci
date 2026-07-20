package com.timereci.focus.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timereci.focus.data.FocusRepository
import com.timereci.focus.data.ReceiptEntity
import com.timereci.focus.ui.Routes
import com.timereci.focus.ui.model.SessionCard
import com.timereci.focus.ui.util.Formatters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: FocusRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val receiptId: Long = savedStateHandle.get<Long>(Routes.ARG_RECEIPT_ID) ?: 0L

    val card: StateFlow<SessionCard?> = repository.observeReceipt(receiptId)
        .map { r -> r?.let(::toCard) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.getReceipt(receiptId)?.let { repository.deleteReceipt(it) }
            onDone()
        }
    }

    private fun toCard(r: ReceiptEntity) = SessionCard(
        id = r.id,
        stamp = Formatters.stamp(r.issuedAtEpoch),
        task = r.taskLabel,
        focus = Formatters.focus(r.focusedMs),
        photos = r.photos,
        comment = r.comment,
    )
}
