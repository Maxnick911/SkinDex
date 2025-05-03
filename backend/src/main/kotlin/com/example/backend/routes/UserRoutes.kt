package com.example.backend.routes

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.backend.models.*

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        // user - get all
        get("/users") {
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin") return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access"))
            val users = transaction {
                Users.selectAll().map {
                    mapOf(
                        "id" to it[Users.id],
                        "email" to it[Users.email],
                        "role" to it[Users.role],
                        "name" to it[Users.name]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to users))
        }

        // user - get by id
        get("/users/{id}") {
            val userId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && currentUserId != userId) return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            val user = transaction { Users.selectAll().where { Users.id eq userId }.firstOrNull() }
            if (user == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            call.respond(HttpStatusCode.OK, mapOf(
                "data" to mapOf(
                    "id" to user[Users.id],
                    "email" to user[Users.email],
                    "role" to user[Users.role],
                    "name" to user[Users.name]
                )
            ))
        }

        // user - update
        put("/users/{id}") {
            val userId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && currentUserId != userId) return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            val input = call.receive<Map<String, String>>()
            val name = input["name"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name required"))
            transaction {
                Users.update({ Users.id eq userId }) {
                    it[Users.name] = name
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "User updated"))
        }

        // user - delete
        delete("/users/{id}") {
            val userId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin") return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can delete"))
            val deleted = transaction { Users.deleteWhere { Users.id eq userId } }
            if (deleted == 0) return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            call.respond(HttpStatusCode.OK, mapOf("message" to "User deleted"))
        }
    }
}