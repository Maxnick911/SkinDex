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
import io.ktor.server.application.log
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.util.UUID

fun Route.userRoutes() {
    authenticate("auth-jwt") {
        get("/users") {
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && role != "doctor") {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins or doctors can access"))
            }
            val users = transaction {
                val query = if (role == "admin") {
                    Users.selectAll()
                } else {
                    Users.selectAll().where { Users.role eq "patient" }
                }
                query.map {
                    mapOf(
                        "id" to it[Users.id].value,
                        "email" to it[Users.email],
                        "role" to it[Users.role],
                        "name" to it[Users.name]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to users))
        }

        post("/add-patient") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val doctorId = principal?.payload?.getClaim("userId")?.asInt()
                if (doctorId == null) {
                    return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                }

                val patientInput = call.receive<PatientInput>()
                if (!isValidEmail(patientInput.email)) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email format"))
                }
                if (patientInput.name.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name cannot be empty"))
                }

                val normalizedEmail = patientInput.email.lowercase()
                val existingUser = transaction {
                    Users.selectAll().where { Users.email eq normalizedEmail }.firstOrNull()
                }
                if (existingUser != null) {
                    return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                }

                val randomPassword = UUID.randomUUID().toString().substring(0, 8)
                val hashedPassword = BCrypt.hashpw(randomPassword, BCrypt.gensalt())

                val patientId = transaction {
                    Users.insert {
                        it[role] = "patient"
                        it[name] = patientInput.name
                        it[email] = normalizedEmail
                        it[passwordHash] = hashedPassword
                        it[Users.doctorId] = doctorId
                    } get Users.id
                }

                call.respond(HttpStatusCode.Created, mapOf("message" to "Patient created with ID: ${patientId.value}"))
            } catch (e: Exception) {
                application.log.error("Error in /add-patient: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error: ${e.message}"))
            }
        }

        get("/patients") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val doctorId = principal?.payload?.getClaim("userId")?.asInt()
                if (doctorId == null) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                }

                val patients = transaction {
                    Users.selectAll().where { (Users.doctorId eq doctorId) and (Users.role eq "patient") }
                        .map {
                            mapOf(
                                "id" to it[Users.id].value,
                                "email" to it[Users.email],
                                "name" to it[Users.name]
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, mapOf("data" to patients))
            } catch (e: Exception) {
                application.log.error("Error in /patients: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error: ${e.message}"))
            }
        }

        get("/users/{id}") {
            val paramId = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && currentUserId != paramId && role != "doctor") {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            val user = transaction { Users.selectAll().where { Users.id eq paramId }.firstOrNull() }
            if (user == null) {
                return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            }
            call.respond(HttpStatusCode.OK, mapOf(
                "data" to mapOf(
                    "id" to user[Users.id].value,
                    "email" to user[Users.email],
                    "role" to user[Users.role],
                    "name" to user[Users.name]
                )
            ))
        }

        put("/users/{id}") {
            val paramId = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val currentUserId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && currentUserId != paramId) {
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            val input = call.receive<Map<String, String>>()
            val name = input["name"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name required"))
            transaction {
                Users.update({ Users.id eq paramId }) {
                    it[Users.name] = name
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "User updated"))
        }

        delete("/users/{id}") {
            val paramId = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "admin" && role != "doctor") {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins or doctors can delete"))
            }

            transaction {
                val imagesForPatient = Images.selectAll().where { Images.patientId eq paramId }.toList()
                imagesForPatient.forEach { image ->
                    Diagnoses.deleteWhere { Diagnoses.imageId eq image[Images.id].value }
                    File(image[Images.filePath]).delete()
                }
                Images.deleteWhere { Images.patientId eq paramId }
                Users.deleteWhere { Users.id eq paramId }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "User and related data deleted"))
        }
    }
}
