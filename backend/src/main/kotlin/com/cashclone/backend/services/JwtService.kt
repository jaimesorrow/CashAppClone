package com.cashclone.backend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.cashclone.backend.config.Env
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

object JwtService {
    private val algorithm = Algorithm.HMAC256(Env.jwtSecret)

    fun generateToken(userId: UUID): String =
        JWT.create()
            .withIssuer(Env.jwtIssuer)
            .withAudience(Env.jwtAudience)
            .withClaim("userId", userId.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)))
            .sign(algorithm)
}
