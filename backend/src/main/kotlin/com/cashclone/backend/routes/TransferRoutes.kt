package com.cashclone.backend.routes

import com.cashclone.backend.models.RequestMoneyRequest
import com.cashclone.backend.models.SendMoneyRequest
import com.cashclone.backend.models.TransferResponse
import com.cashclone.backend.plugins.ApiException
import com.cashclone.backend.services.TransferRecord
import com.cashclone.backend.services.TransferService
import com.cashclone.backend.services.UserRecord
import com.cashclone.backend.services.UserService
import com.cashclone.backend.unit.UnitClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun requireApprovedAccount(user: UserRecord): String =
    user.unitAccountId ?: throw ApiException(
        HttpStatusCode.PreconditionFailed,
        "${user.cashtag} does not have an approved account yet",
    )

private fun TransferRecord.toResponse(meId: UUID, counterparty: UserRecord): TransferResponse = TransferResponse(
    id = id.toString(),
    counterpartyCashtag = counterparty.cashtag,
    amountCents = amountCents,
    note = note,
    type = type,
    status = status,
    createdAt = DateTimeFormatter.ISO_INSTANT.format(createdAt),
    direction = if (fromUserId == meId) "OUTGOING" else "INCOMING",
)

fun Route.transferRoutes(unitClient: UnitClient) {
    authenticate("auth-jwt") {
        post("/transfers/send") {
            val userId = call.requireUserId()
            val me = UserService.findById(userId) ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            val req = call.receive<SendMoneyRequest>()

            if (req.amountCents <= 0) throw ApiException(HttpStatusCode.BadRequest, "Amount must be positive")
            val recipient = UserService.findByCashtag(req.toCashtag)
                ?: throw ApiException(HttpStatusCode.NotFound, "No user with cashtag ${req.toCashtag}")
            if (recipient.id == me.id) throw ApiException(HttpStatusCode.BadRequest, "Cannot send money to yourself")

            val fromAccountId = requireApprovedAccount(me)
            val toAccountId = requireApprovedAccount(recipient)

            val transfer = TransferService.create(
                fromUserId = me.id,
                toUserId = recipient.id,
                amountCents = req.amountCents,
                note = req.note,
                type = "SEND",
                status = "PENDING",
            )

            val payment = try {
                unitClient.createBookPayment(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amountCents = req.amountCents,
                    description = req.note ?: "Payment from ${me.cashtag}",
                    tags = mapOf("transferId" to transfer.id.toString()),
                )
            } catch (e: Exception) {
                TransferService.markFailed(transfer.id)
                throw ApiException(HttpStatusCode.BadGateway, "Payment provider error: ${e.message}")
            }

            val status = if (payment.status.equals("Sent", ignoreCase = true)) "COMPLETED" else "PENDING"
            if (status == "COMPLETED") TransferService.markCompleted(transfer.id, payment.id)

            call.respond(
                HttpStatusCode.Created,
                (TransferService.findById(transfer.id) ?: transfer).toResponse(me.id, recipient),
            )
        }

        post("/transfers/request") {
            val userId = call.requireUserId()
            val me = UserService.findById(userId) ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            val req = call.receive<RequestMoneyRequest>()

            if (req.amountCents <= 0) throw ApiException(HttpStatusCode.BadRequest, "Amount must be positive")
            val payer = UserService.findByCashtag(req.fromCashtag)
                ?: throw ApiException(HttpStatusCode.NotFound, "No user with cashtag ${req.fromCashtag}")
            if (payer.id == me.id) throw ApiException(HttpStatusCode.BadRequest, "Cannot request money from yourself")

            val transfer = TransferService.create(
                fromUserId = payer.id,
                toUserId = me.id,
                amountCents = req.amountCents,
                note = req.note,
                type = "REQUEST",
                status = "PENDING",
            )
            call.respond(HttpStatusCode.Created, transfer.toResponse(me.id, payer))
        }

        post("/transfers/{id}/fulfill") {
            val userId = call.requireUserId()
            val me = UserService.findById(userId) ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            val transferId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ApiException(HttpStatusCode.BadRequest, "Invalid transfer id")

            val transfer = TransferService.findById(transferId)
                ?: throw ApiException(HttpStatusCode.NotFound, "Transfer not found")
            if (transfer.type != "REQUEST" || transfer.status != "PENDING") {
                throw ApiException(HttpStatusCode.Conflict, "Transfer is not a pending request")
            }
            if (transfer.fromUserId != me.id) {
                throw ApiException(HttpStatusCode.Forbidden, "Only the requested payer can fulfill this request")
            }

            val requester = UserService.findById(transfer.toUserId)
                ?: throw ApiException(HttpStatusCode.NotFound, "Requester not found")
            val fromAccountId = requireApprovedAccount(me)
            val toAccountId = requireApprovedAccount(requester)

            val payment = try {
                unitClient.createBookPayment(
                    fromAccountId = fromAccountId,
                    toAccountId = toAccountId,
                    amountCents = transfer.amountCents,
                    description = transfer.note ?: "Request fulfillment",
                    tags = mapOf("transferId" to transfer.id.toString()),
                )
            } catch (e: Exception) {
                throw ApiException(HttpStatusCode.BadGateway, "Payment provider error: ${e.message}")
            }

            if (payment.status.equals("Sent", ignoreCase = true)) {
                TransferService.markCompleted(transfer.id, payment.id)
            }
            call.respond((TransferService.findById(transfer.id) ?: transfer).toResponse(me.id, requester))
        }

        get("/transfers/history") {
            val userId = call.requireUserId()
            val me = UserService.findById(userId) ?: throw ApiException(HttpStatusCode.NotFound, "User not found")
            val history = TransferService.historyFor(me.id)
            val counterpartyCache = mutableMapOf<UUID, UserRecord?>()
            val response = history.mapNotNull { t ->
                val counterpartyId = if (t.fromUserId == me.id) t.toUserId else t.fromUserId
                val counterparty = counterpartyCache.getOrPut(counterpartyId) { UserService.findById(counterpartyId) }
                counterparty?.let { t.toResponse(me.id, it) }
            }
            call.respond(response)
        }
    }
}
