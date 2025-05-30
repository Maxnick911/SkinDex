package com.example.backend.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*
import com.example.backend.models.*
import io.ktor.server.http.content.staticFiles
import java.io.File

fun Application.configureRouting() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "some_unique_secret"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "http://localhost:8080"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "http://localhost:8080"

    routing {
        get("/") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "OK"))
        }

        get("/test-db") {
            val count = transaction {
                Users.selectAll().count()
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to mapOf("userCount" to count)))
        }

        post("/register") {
            try {
                val userInput = call.receive<UserRegisterInput>()
                if (!isValidEmail(userInput.email)) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email format"))
                }
                if (userInput.password.length < 6) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password must be at least 6 characters"))
                }
                if (userInput.name.isBlank()) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name cannot be empty"))
                }

                val normalizedEmail = userInput.email.lowercase()
                val existingUser = transaction {
                    Users.selectAll().where { Users.email eq normalizedEmail }.firstOrNull()
                }
                if (existingUser != null) {
                    return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                }

                val hashedPassword = BCrypt.hashpw(userInput.password, BCrypt.gensalt())
                val userId: Int = transaction {
                    val insertedId = Users.insertAndGetId {
                        it[role] = "doctor"
                        it[name] = userInput.name
                        it[email] = normalizedEmail
                        it[passwordHash] = hashedPassword
                        it[doctorId] = null
                    }
                    Users.update({ Users.id eq insertedId }) {
                        it[doctorId] = insertedId.value
                    }
                    insertedId.value
                }

                call.respond(HttpStatusCode.Created, mapOf("message" to "User created with ID: $userId"))
            } catch (e: Exception) {
                application.log.error("Error in /register: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Server error: ${e.message}"))
            }
        }

        post("/login") {
            val loginInput = call.receive<UserLoginInput>()
            val user = transaction {
                Users.selectAll().where { Users.email eq loginInput.email.lowercase() }.firstOrNull()
            }
            if (user == null || !BCrypt.checkpw(loginInput.password, user[Users.passwordHash])) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Wrong email or password"))
            }

            val token = JWT.create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("email", user[Users.email])
                .withClaim("name", user[Users.name])
                .withClaim("role", user[Users.role])
                .withClaim("userId", user[Users.id].value)
                .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                .sign(Algorithm.HMAC256(jwtSecret))

            call.respond(HttpStatusCode.OK, mapOf(
                "token" to token,
                "message" to "Successful entry"
            ))
        }

        authenticate("auth-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.payload?.getClaim("email")?.asString() ?: "Unknown"
                val role = principal?.payload?.getClaim("role")?.asString() ?: "Unknown"
                call.respond(HttpStatusCode.OK, mapOf("message" to "Hello, $email! Your role is $role"))
            }
        }

        userRoutes()
        imageRoutes()
        diagnosisRoutes()
        logRoutes()
        staticFiles("/uploads", File("uploads"))
    }
}