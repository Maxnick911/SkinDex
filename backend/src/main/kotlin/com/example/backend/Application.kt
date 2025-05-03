package com.example.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.backend.routes.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSerialization()
        configureAuthentication()
        configureDatabase()
        configureRouting()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}

fun Application.configureAuthentication() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "some_unique_secret"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "http://localhost:8080"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "http://localhost:8080"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "SkinDex API"
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("email").asString().isNotEmpty()) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}