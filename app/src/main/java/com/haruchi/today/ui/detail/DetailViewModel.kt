package com.haruchi.today.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haruchi.today.data.FocusRepository
import com.haruchi.today.ui.Routes
import com.haruchi.today.ui.model.SessionCard
import com.haruchi.today.ui.model.toSessionCard
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
        .map { r -> r?.toSessionCard() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.getReceipt(receiptId)?.let { repository.deleteReceipt(it) }
            onDone()
        }
    }

    fun updateNote(text: String) {
        viewModelScope.launch {
            repository.getReceipt(receiptId)?.let { receipt ->
                repository.updateReceipt(receipt.copy(comment = text.trim().ifBlank { null }))
            }
        }
    }
}
