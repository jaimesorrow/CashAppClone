package com.cashclone.backend

import com.cashclone.backend.db.DatabaseFactory
import com.cashclone.backend.plugins.configureCors
import com.cashclone.backend.plugins.configureSecurity
import com.cashclone.backend.plugins.configureSerialization
import com.cashclone.backend.plugins.configureStatusPages
import com.cashclone.backend.routes.accountRoutes
import com.cashclone.backend.routes.authRoutes
import com.cashclone.backend.routes.kycRoutes
import com.cashclone.backend.routes.transferRoutes
import com.cashclone.backend.routes.webhookRoutes
import com.cashclone.backend.unit.UnitClient
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()

    install(CallLogging)
    configureSerialization()
    configureStatusPages()
    configureCors()
    configureSecurity()

    val unitClient = UnitClient()

    routing {
        authRoutes()
        kycRoutes(unitClient)
        accountRoutes(unitClient)
        transferRoutes(unitClient)
        webhookRoutes()
    }
}
