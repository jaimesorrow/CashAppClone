package com.cashclone.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.data.model.UserSearchResult
import com.cashclone.app.data.repository.CashCloneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SendRequestViewModel(private val repository: CashCloneRepository) : ViewModel() {
    private val _transferState = MutableStateFlow<UiState<TransferResponse>>(UiState.Idle)
    val transferState: StateFlow<UiState<TransferResponse>> = _transferState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val searchResults: StateFlow<List<UserSearchResult>> = _searchResults.asStateFlow()

    fun searchCashtag(prefix: String) {
        if (prefix.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = runCatching { repository.searchUsers(prefix) }.getOrDefault(emptyList())
        }
    }

    fun send(toCashtag: String, amountCents: Long, note: String?) {
        viewModelScope.launch {
            _transferState.value = UiState.Loading
            _transferState.value = try {
                UiState.Success(repository.sendMoney(toCashtag, amountCents, note))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun request(fromCashtag: String, amountCents: Long, note: String?) {
        viewModelScope.launch {
            _transferState.value = UiState.Loading
            _transferState.value = try {
                UiState.Success(repository.requestMoney(fromCashtag, amountCents, note))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun resetTransferState() {
        _transferState.value = UiState.Idle
    }
}
