package com.cashclone.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cashclone.app.data.model.BalanceResponse
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.data.model.UserProfile
import com.cashclone.app.data.repository.CashCloneRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val profile: UserProfile,
    val balance: BalanceResponse?,
    val recentActivity: List<TransferResponse>,
)

class HomeViewModel(private val repository: CashCloneRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Idle)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = try {
                val profile = repository.me()
                val balance = if (profile.kycStatus == "APPROVED") {
                    runCatching { repository.balance() }.getOrNull()
                } else {
                    null
                }
                val activity = runCatching { repository.history() }.getOrDefault(emptyList()).take(10)
                UiState.Success(HomeData(profile, balance, activity))
            } catch (e: Exception) {
                UiState.Error(e.toUserMessage())
            }
        }
    }
}
