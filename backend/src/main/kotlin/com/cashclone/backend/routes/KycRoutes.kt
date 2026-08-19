package com.cashclone.backend.routes

import com.cashclone.backend.models.KycApplicationRequest
import com.cashclone.backend.models.KycStatusResponse
import com.cashclone.backend.plugins.ApiException
import com.cashclone.backend.services.UserService
import com.cashclone.backend.unit.IndividualApplicationInput
import com.cashclone.backend.unit.UnitClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.kycRoutes(unitClient: UnitClient) {
    authenticate("auth-jwt") {
        post("/kyc/application") {
            val userId = call.requireUserId()
            val user = UserService.findById(userId)
                ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            if (user.kycStatus != "NOT_STARTED" && user.kycStatus != "DENIED") {
                throw ApiException(HttpStatusCode.Conflict, "KYC already ${user.kycStatus.lowercase()}")
            }

            val req = call.receive<KycApplicationRequest>()
            val (first, last) = user.fullName.trim().split(" ", limit = 2)
                .let { it.getOrElse(0) { "" } to it.getOrElse(1) { "" } }

            val application = unitClient.createIndividualApplication(
                IndividualApplicationInput(
                    firstName = first,
                    lastName = last,
                    email = user.email,
                    phoneNumber = user.phone ?: "",
                    ssn = req.ssn,
                    dateOfBirth = req.dateOfBirth,
                    addressStreet = req.addressStreet,
                    addressCity = req.addressCity,
                    addressState = req.addressState,
                    addressPostalCode = req.addressPostalCode,
                )
            )
            UserService.startKyc(userId, application.id)
            call.respond(HttpStatusCode.Accepted, KycStatusResponse(kycStatus = "PENDING"))
        }

        get("/kyc/status") {
            val userId = call.requireUserId()
            val user = UserService.findById(userId)
                ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            call.respond(KycStatusResponse(kycStatus = user.kycStatus, unitAccountId = user.unitAccountId))
        }
    }
}
