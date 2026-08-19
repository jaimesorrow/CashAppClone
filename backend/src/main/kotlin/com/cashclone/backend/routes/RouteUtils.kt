package com.cashclone.backend.routes

import com.cashclone.backend.plugins.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

fun ApplicationCall.requireUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Missing or invalid token")
    val raw = principal.payload.getClaim("userId").asString()
        ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid token")
    return UUID.fromString(raw)
}
