package com.cashclone.app.data.remote

import com.cashclone.app.data.model.AuthResponse
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
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("me")
    suspend fun me(): UserProfile

    @POST("kyc/application")
    suspend fun submitKycApplication(@Body request: KycApplicationRequest): KycStatusResponse

    @GET("kyc/status")
    suspend fun kycStatus(): KycStatusResponse

    @GET("accounts/balance")
    suspend fun balance(): BalanceResponse

    @GET("users/search")
    suspend fun searchUsers(@Query("cashtag") cashtag: String): List<UserSearchResult>

    @POST("transfers/send")
    suspend fun sendMoney(@Body request: SendMoneyRequest): TransferResponse

    @POST("transfers/request")
    suspend fun requestMoney(@Body request: RequestMoneyRequest): TransferResponse

    @POST("transfers/{id}/fulfill")
    suspend fun fulfillRequest(@Path("id") transferId: String): TransferResponse

    @GET("transfers/history")
    suspend fun history(): List<TransferResponse>
}
