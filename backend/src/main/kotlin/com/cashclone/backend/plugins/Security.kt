package com.cashclone.backend.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.cashclone.backend.config.Env
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity() {
    authentication {
        jwt("auth-jwt") {
            realm = Env.jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(Env.jwtSecret))
                    .withIssuer(Env.jwtIssuer)
                    .withAudience(Env.jwtAudience)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}
