package com.example.backend.routes

import com.example.backend.models.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.logRoutes() {
    authenticate("auth-jwt") {
        get("/logs") {
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin") return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access"))
            val logs = transaction {
                Logs.selectAll().map {
                    mapOf(
                        "id" to it[Logs.id].value,
                        "userId" to it[Logs.userId],
                        "action" to it[Logs.action],
                        "details" to it[Logs.details],
                        "timestamp" to it[Logs.timestamp].toString()
                    )
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to logs))
        }

        get("/logs/{id}") {
            val logId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin") return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access"))
            val log = transaction { Logs.selectAll().where { Logs.id eq logId }.firstOrNull() }
            if (log == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Log not found"))
            call.respond(HttpStatusCode.OK, mapOf(
                "data" to mapOf(
                    "id" to log[Logs.id].value,
                    "userId" to log[Logs.userId],
                    "action" to log[Logs.action],
                    "details" to log[Logs.details],
                    "timestamp" to log[Logs.timestamp].toString()
                )
            ))
        }
    }
}