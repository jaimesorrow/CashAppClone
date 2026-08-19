package com.cashclone.backend.routes

import com.cashclone.backend.services.TransferService
import com.cashclone.backend.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Unit.co delivers events as a JSON:API array under "data". Signature
 * verification (x-unit-signature, HMAC-SHA256 of the raw body against
 * UNIT_WEBHOOK_SECRET) should be added here before going to production;
 * omitted in this scaffold for brevity.
 */
fun Route.webhookRoutes() {
    post("/webhooks/unit") {
        val body = call.receive<JsonObject>()
        val events = body["data"] as? JsonArray ?: JsonArray(emptyList())

        for (eventElement in events) {
            val event = eventElement.jsonObject
            val type = event["type"]?.jsonPrimitive?.content ?: continue
            val attrs = event["attributes"]?.jsonObject
            val eventType = attrs?.get("type")?.jsonPrimitive?.content ?: type

            when (eventType) {
                "application.approved" -> handleApplicationApproved(attrs)
                "application.denied" -> handleApplicationDenied(attrs)
                "payment.sent", "payment.clearing" -> handlePaymentCompleted(attrs)
                "payment.rejected" -> handlePaymentFailed(attrs)
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}

private fun handleApplicationApproved(attrs: JsonObject?) {
    val applicationId = attrs?.get("applicationId")?.jsonPrimitive?.content ?: return
    val customerId = attrs["customerId"]?.jsonPrimitive?.content ?: return
    val accountId = attrs["accountId"]?.jsonPrimitive?.content ?: return
    val user = UserService.findByUnitApplicationId(applicationId) ?: return
    UserService.approveKyc(user.id, customerId, accountId)
}

private fun handleApplicationDenied(attrs: JsonObject?) {
    val applicationId = attrs?.get("applicationId")?.jsonPrimitive?.content ?: return
    val user = UserService.findByUnitApplicationId(applicationId) ?: return
    UserService.setKycStatus(user.id, "DENIED")
}

private fun handlePaymentCompleted(attrs: JsonObject?) {
    val tags = attrs?.get("tags")?.jsonObject ?: return
    val transferId = tags["transferId"]?.jsonPrimitive?.content ?: return
    val paymentId = attrs["paymentId"]?.jsonPrimitive?.content
    TransferService.markCompleted(UUID.fromString(transferId), paymentId)
}

private fun handlePaymentFailed(attrs: JsonObject?) {
    val tags = attrs?.get("tags")?.jsonObject ?: return
    val transferId = tags["transferId"]?.jsonPrimitive?.content ?: return
    TransferService.markFailed(UUID.fromString(transferId))
}
