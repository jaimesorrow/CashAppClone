package com.cashclone.backend.routes

import com.cashclone.backend.models.AuthResponse
import com.cashclone.backend.models.LoginRequest
import com.cashclone.backend.models.SignupRequest
import com.cashclone.backend.models.UserProfile
import com.cashclone.backend.plugins.ApiException
import com.cashclone.backend.services.JwtService
import com.cashclone.backend.services.PasswordService
import com.cashclone.backend.services.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

private val CASHTAG_REGEX = Regex("^[a-zA-Z0-9_]{3,20}$")

fun Route.authRoutes() {
    post("/auth/signup") {
        val req = call.receive<SignupRequest>()

        if (!CASHTAG_REGEX.matches(req.cashtag)) {
            throw ApiException(HttpStatusCode.BadRequest, "Cashtag must be 3-20 letters, numbers or underscores")
        }
        if (req.password.length < 8) {
            throw ApiException(HttpStatusCode.BadRequest, "Password must be at least 8 characters")
        }
        if (UserService.findByEmail(req.email) != null) {
            throw ApiException(HttpStatusCode.Conflict, "Email already registered")
        }
        if (UserService.findByCashtag(req.cashtag) != null) {
            throw ApiException(HttpStatusCode.Conflict, "Cashtag already taken")
        }

        val user = UserService.create(
            email = req.email,
            passwordHash = PasswordService.hash(req.password),
            cashtag = req.cashtag,
            fullName = req.fullName,
            phone = req.phone,
        )
        val token = JwtService.generateToken(user.id)
        call.respond(
            HttpStatusCode.Created,
            AuthResponse(
                token = token,
                user = UserProfile(user.id.toString(), user.email, user.cashtag, user.fullName, user.kycStatus),
            ),
        )
    }

    post("/auth/login") {
        val req = call.receive<LoginRequest>()
        val user = UserService.findByEmail(req.email)
            ?: throw ApiException(HttpStatusCode.Unauthorized, "Invalid email or password")
        if (!PasswordService.verify(req.password, user.passwordHash)) {
            throw ApiException(HttpStatusCode.Unauthorized, "Invalid email or password")
        }
        val token = JwtService.generateToken(user.id)
        call.respond(
            AuthResponse(
                token = token,
                user = UserProfile(user.id.toString(), user.email, user.cashtag, user.fullName, user.kycStatus),
            ),
        )
    }
}
