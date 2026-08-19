package com.cashclone.backend.config

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv {
    ignoreIfMissing = true
}

object Env {
    val dbUrl: String = get("DB_URL", "jdbc:postgresql://localhost:5432/cashclone")
    val dbUser: String = get("DB_USER", "cashclone")
    val dbPassword: String = get("DB_PASSWORD", "cashclone")

    val jwtSecret: String = get("JWT_SECRET", "dev-only-change-me")
    val jwtIssuer: String = get("JWT_ISSUER", "cashclone-backend")
    val jwtAudience: String = get("JWT_AUDIENCE", "cashclone-app")
    val jwtRealm: String = get("JWT_REALM", "cashclone")

    // Unit.co sandbox: https://api.s.unit.sh
    val unitApiBaseUrl: String = get("UNIT_API_BASE_URL", "https://api.s.unit.sh")
    val unitApiToken: String = get("UNIT_API_TOKEN", "")
    val unitWebhookSecret: String = get("UNIT_WEBHOOK_SECRET", "")

    private fun get(key: String, default: String): String =
        System.getenv(key) ?: dotenv[key] ?: default
}
