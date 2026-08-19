package com.cashclone.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashclone.app.data.model.KycApplicationRequest
import com.cashclone.app.data.model.KycStatusResponse
import com.cashclone.app.data.repository.CashCloneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KycViewModel(private val repository: CashCloneRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<KycStatusResponse>>(UiState.Idle)
    val state: StateFlow<UiState<KycStatusResponse>> = _state.asStateFlow()

    fun submit(request: KycApplicationRequest) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.submitKyc(request))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.kycStatus())
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }
}
