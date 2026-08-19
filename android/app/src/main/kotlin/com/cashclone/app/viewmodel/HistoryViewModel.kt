package com.cashclone.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.data.repository.CashCloneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: CashCloneRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<TransferResponse>>>(UiState.Idle)
    val state: StateFlow<UiState<List<TransferResponse>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.history())
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun fulfill(transferId: String) {
        viewModelScope.launch {
            try {
                repository.fulfillRequest(transferId)
                load()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.toUserMessage())
            }
        }
    }
}
