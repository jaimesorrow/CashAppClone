package com.cashclone.app.data.repository

import com.cashclone.app.data.model.BalanceResponse
import com.cashclone.app.data.model.KycApplicationRequest
import com.cashclone.app.data.model.KycStatusResponse
import com.cashclone.app.data.model.LoginRequest
import com.cashclone.app.data.model.RequestMoneyRequest
import com.cashclone.app.data.model.SendMoneyRequest
import com.cashclone.app.data.model.SignupRequest
import com.cashclone.app.data.model.TransferResponse
import com.cashclone.app.data.model.UserProfile
import com.cashclone.app.data.model.UserSearchResult
import com.cashclone.app.data.remote.ApiService
import com.cashclone.app.data.remote.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CashCloneRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
) {
    val isLoggedIn: Flow<Boolean> = tokenStore.tokenFlow.map { it != null }

    suspend fun signup(email: String, password: String, cashtag: String, fullName: String, phone: String?): UserProfile {
        val response = api.signup(SignupRequest(email, password, cashtag, fullName, phone))
        tokenStore.save(response.token)
        return response.user
    }

    suspend fun login(email: String, password: String): UserProfile {
        val response = api.login(LoginRequest(email, password))
        tokenStore.save(response.token)
        return response.user
    }

    suspend fun logout() = tokenStore.clear()

    suspend fun me(): UserProfile = api.me()

    suspend fun submitKyc(request: KycApplicationRequest): KycStatusResponse = api.submitKycApplication(request)

    suspend fun kycStatus(): KycStatusResponse = api.kycStatus()

    suspend fun balance(): BalanceResponse = api.balance()

    suspend fun searchUsers(cashtagPrefix: String): List<UserSearchResult> = api.searchUsers(cashtagPrefix)

    suspend fun sendMoney(toCashtag: String, amountCents: Long, note: String?): TransferResponse =
        api.sendMoney(SendMoneyRequest(toCashtag, amountCents, note))

    suspend fun requestMoney(fromCashtag: String, amountCents: Long, note: String?): TransferResponse =
        api.requestMoney(RequestMoneyRequest(fromCashtag, amountCents, note))

    suspend fun fulfillRequest(transferId: String): TransferResponse = api.fulfillRequest(transferId)

    suspend fun history(): List<TransferResponse> = api.history()
}
