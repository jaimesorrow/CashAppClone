package com.cashclone.backend.routes

import com.cashclone.backend.models.BalanceResponse
import com.cashclone.backend.models.UserProfile
import com.cashclone.backend.models.UserSearchResult
import com.cashclone.backend.plugins.ApiException
import com.cashclone.backend.services.UserService
import com.cashclone.backend.unit.UnitClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.accountRoutes(unitClient: UnitClient) {
    authenticate("auth-jwt") {
        get("/me") {
            val userId = call.requireUserId()
            val user = UserService.findById(userId)
                ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            call.respond(UserProfile(user.id.toString(), user.email, user.cashtag, user.fullName, user.kycStatus))
        }

        get("/accounts/balance") {
            val userId = call.requireUserId()
            val user = UserService.findById(userId)
                ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            val accountId = user.unitAccountId
                ?: throw ApiException(HttpStatusCode.PreconditionFailed, "No approved account yet; complete KYC first")
            val balance = unitClient.getAccountBalance(accountId)
            call.respond(BalanceResponse(availableCents = balance.availableCents, ledgerCents = balance.ledgerCents))
        }

        get("/users/search") {
            val prefix = call.request.queryParameters["cashtag"]
                ?: throw ApiException(HttpStatusCode.BadRequest, "cashtag query param required")
            val results = UserService.searchByCashtagPrefix(prefix)
                .map { UserSearchResult(it.cashtag, it.fullName) }
            call.respond(results)
        }
    }
}
