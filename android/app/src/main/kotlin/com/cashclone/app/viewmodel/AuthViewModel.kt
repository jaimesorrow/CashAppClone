package com.cashclone.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashclone.app.data.model.UserProfile
import com.cashclone.app.data.repository.CashCloneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: CashCloneRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<UserProfile>>(UiState.Idle)
    val state: StateFlow<UiState<UserProfile>> = _state.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun signup(email: String, password: String, cashtag: String, fullName: String, phone: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.signup(email, password, cashtag, fullName, phone))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                UiState.Success(repository.login(email, password))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = UiState.Idle
        }
    }

    fun resetState() {
        _state.value = UiState.Idle
    }
}
