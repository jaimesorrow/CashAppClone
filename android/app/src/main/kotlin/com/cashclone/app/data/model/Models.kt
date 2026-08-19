package com.cashclone.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
    val cashtag: String,
    val fullName: String,
    val phone: String? = null,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val user: UserProfile)

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val cashtag: String,
    val fullName: String,
    val kycStatus: String,
)

@Serializable
data class KycApplicationRequest(
    val dateOfBirth: String,
    val ssn: String,
    val addressStreet: String,
    val addressCity: String,
    val addressState: String,
    val addressPostalCode: String,
)

@Serializable
data class KycStatusResponse(val kycStatus: String, val unitAccountId: String? = null)

@Serializable
data class BalanceResponse(val availableCents: Long, val ledgerCents: Long, val currency: String = "USD")

@Serializable
data class UserSearchResult(val cashtag: String, val fullName: String)

@Serializable
data class SendMoneyRequest(val toCashtag: String, val amountCents: Long, val note: String? = null)

@Serializable
data class RequestMoneyRequest(val fromCashtag: String, val amountCents: Long, val note: String? = null)

@Serializable
data class TransferResponse(
    val id: String,
    val counterpartyCashtag: String,
    val amountCents: Long,
    val note: String?,
    val type: String,
    val status: String,
    val createdAt: String,
    val direction: String,
)

@Serializable
data class ErrorResponse(val error: String)
