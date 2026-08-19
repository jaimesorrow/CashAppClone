package com.cashclone.backend.unit

import com.cashclone.backend.config.Env
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Thin wrapper around Unit.co's JSON:API-style REST API (sandbox base URL by
 * default: https://api.s.unit.sh). Only the handful of endpoints this app
 * needs are modeled here; consult Unit's current API reference before
 * extending, since fintech partner APIs evolve frequently.
 */
class UnitClient(
    baseUrl: String = Env.unitApiBaseUrl,
    private val apiToken: String = Env.unitApiToken,
) {
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest { url(baseUrl) }
    }

    private suspend fun authedPost(path: String, body: JsonObject): JsonObject {
        val response = http.post(path) {
            header("Authorization", "Bearer $apiToken")
            contentType(ContentType.parse("application/vnd.api+json"))
            setBody(body)
        }
        return response.body<JsonObject>()
    }

    private suspend fun authedGet(path: String): JsonObject {
        val response = http.get(path) {
            header("Authorization", "Bearer $apiToken")
        }
        return response.body<JsonObject>()
    }

    /** Creates an individual application (KYC) for a new customer. */
    suspend fun createIndividualApplication(input: IndividualApplicationInput): UnitApplication {
        val body = buildJsonObject {
            putJsonObject("data") {
                put("type", "individualApplication")
                putJsonObject("attributes") {
                    put("ssn", input.ssn)
                    put("fullName", buildJsonObject {
                        put("first", input.firstName)
                        put("last", input.lastName)
                    })
                    put("dateOfBirth", input.dateOfBirth)
                    put("address", buildJsonObject {
                        put("street", input.addressStreet)
                        put("city", input.addressCity)
                        put("state", input.addressState)
                        put("postalCode", input.addressPostalCode)
                        put("country", "US")
                    })
                    put("email", input.email)
                    put("phone", buildJsonObject {
                        put("countryCode", "1")
                        put("number", input.phoneNumber)
                    })
                }
            }
        }
        val json = authedPost("/applications", body)
        val data = json["data"]!!.jsonObject
        return UnitApplication(
            id = data["id"]!!.jsonPrimitive.content,
            status = data["attributes"]!!.jsonObject["status"]!!.jsonPrimitive.content,
        )
    }

    suspend fun getApplication(applicationId: String): UnitApplication {
        val json = authedGet("/applications/$applicationId")
        val data = json["data"]!!.jsonObject
        val attrs = data["attributes"]!!.jsonObject
        return UnitApplication(
            id = data["id"]!!.jsonPrimitive.content,
            status = attrs["status"]!!.jsonPrimitive.content,
            customerId = data["relationships"]?.jsonObject
                ?.get("customer")?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("id")?.jsonPrimitive?.content,
        )
    }

    /** Fetches the DDA account's current balance. */
    suspend fun getAccountBalance(accountId: String): UnitBalance {
        val json = authedGet("/accounts/$accountId")
        val attrs = json["data"]!!.jsonObject["attributes"]!!.jsonObject
        return UnitBalance(
            availableCents = attrs["available"]!!.jsonPrimitive.content.toLong(),
            ledgerCents = attrs["balance"]!!.jsonPrimitive.content.toLong(),
        )
    }

    /** Books an instant internal transfer between two Unit deposit accounts. */
    suspend fun createBookPayment(
        fromAccountId: String,
        toAccountId: String,
        amountCents: Long,
        description: String,
        tags: Map<String, String> = emptyMap(),
    ): UnitPayment {
        val body = buildJsonObject {
            putJsonObject("data") {
                put("type", "bookPayment")
                putJsonObject("attributes") {
                    put("amount", amountCents)
                    put("description", description.take(80))
                    putJsonObject("tags") {
                        tags.forEach { (key, value) -> put(key, value) }
                    }
                }
                putJsonObject("relationships") {
                    putJsonObject("account") {
                        putJsonObject("data") {
                            put("type", "depositAccount")
                            put("id", fromAccountId)
                        }
                    }
                    putJsonObject("counterpartyAccount") {
                        putJsonObject("data") {
                            put("type", "depositAccount")
                            put("id", toAccountId)
                        }
                    }
                }
            }
        }
        val json = authedPost("/payments", body)
        val data = json["data"]!!.jsonObject
        return UnitPayment(
            id = data["id"]!!.jsonPrimitive.content,
            status = data["attributes"]!!.jsonObject["status"]!!.jsonPrimitive.content,
        )
    }
}

data class IndividualApplicationInput(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val ssn: String,
    val dateOfBirth: String,
    val addressStreet: String,
    val addressCity: String,
    val addressState: String,
    val addressPostalCode: String,
)

data class UnitApplication(
    val id: String,
    val status: String,
    val customerId: String? = null,
)

data class UnitBalance(
    val availableCents: Long,
    val ledgerCents: Long,
)

data class UnitPayment(
    val id: String,
    val status: String,
)
