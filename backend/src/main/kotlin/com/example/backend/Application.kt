package com.example.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureSerialization()
        configureDatabase()
        configureRouting()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}

fun Application.configureDatabase() {
    val logger = LoggerFactory.getLogger("Database")
    try {
        val database = Database.connect(
            url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://db:5432/skindex",
            driver = "org.postgresql.Driver",
            user = System.getenv("DATABASE_USER") ?: "admin",
            password = System.getenv("DATABASE_PASSWORD") ?: "secret"
        )
        logger.info("Database connected successfully")
    } catch (e: Exception) {
        logger.error("Failed to connect to database: ${e.message}")
        throw e
    }
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("OK", ContentType.Text.Plain)
        }
        get("/test-db") {
            val count = transaction {
                Users.selectAll().count()
            }
            call.respondText("Users in DB: $count", ContentType.Text.Plain)
        }
    }
}