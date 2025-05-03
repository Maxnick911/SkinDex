package com.example.backend.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.backend.routes.diagnosisRoutes
import com.example.backend.routes.imageRoutes
import com.example.backend.routes.userRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.*
import com.example.backend.models.*

fun Application.configureRouting() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "some_unique_secret"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "http://localhost:8080"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "http://localhost:8080"

    routing {
        // check server
        get("/") {
            call.respond(HttpStatusCode.OK, mapOf("message" to "OK"))
        }

        // test database
        get("/test-db") {
            val count = transaction {
                Users.selectAll().count()
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to mapOf("userCount" to count)))
        }

        // register
        post("/register") {
            try {
                val userInput = call.receive<UserRegisterInput>()

                if (!isValidEmail(userInput.email)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid email format"))
                    return@post
                }
                if (userInput.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password must be at least 6 characters"))
                    return@post
                }
                if (userInput.role !in listOf("doctor", "patient", "admin")) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid role. Must be 'doctor', 'patient', or 'admin'"))
                    return@post
                }
                if (userInput.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name cannot be empty"))
                    return@post
                }

                val normalizedEmail = userInput.email.lowercase()
                println("Registering user with email: $normalizedEmail")
                val existingUser = transaction {
                    addLogger(StdOutSqlLogger)
                    Users.selectAll().where { Users.email eq normalizedEmail }.firstOrNull()
                }
                println("Existing user found: $existingUser")
                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                    return@post
                }

                val hashedPassword = BCrypt.hashpw(userInput.password, BCrypt.gensalt())
                val userId = transaction {
                    Users.insert {
                        it[role] = userInput.role
                        it[name] = userInput.name
                        it[email] = normalizedEmail
                        it[passwordHash] = hashedPassword
                    } get Users.id
                }

                call.respond(HttpStatusCode.Created, mapOf("message" to "User created with ID: $userId"))
            } catch (e: Exception) {
                println("Registration error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid input: ${e.message}"))
            }
        }

        // login
        post("/login") {
            try {
                val loginInput = call.receive<UserLoginInput>()
                val user = transaction {
                    addLogger(StdOutSqlLogger)
                    Users.selectAll().where { Users.email eq loginInput.email.lowercase() }.firstOrNull()
                }
                if (user == null || !BCrypt.checkpw(loginInput.password, user[Users.passwordHash])) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Wrong email or password"))
                    return@post
                }

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("email", user[Users.email])
                    .withClaim("role", user[Users.role])
                    .withClaim("userId", user[Users.id])
                    .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(HttpStatusCode.OK, mapOf("token" to token, "message" to "Successful entry for ${user[Users.email]}"))
            } catch (e: Exception) {
                println("Login error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid data: ${e.message}"))
            }
        }

        // protected test
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

        authenticate("auth-jwt") {
            // get all logs
            get("/logs") {
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "admin") return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access"))
                val logs = transaction {
                    Logs.selectAll().map {
                        mapOf("userId" to it[Logs.userId], "action" to it[Logs.action], "details" to it[Logs.details])
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("data" to logs))
            }

            // get log by id
            get("/logs/{id}") {
                val logId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                val principal = call.principal<JWTPrincipal>()
                val role = principal?.payload?.getClaim("role")?.asString()
                if (role != "admin") return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins can access"))
                val log = transaction { Logs.selectAll().where { Logs.id eq logId }.firstOrNull() }
                if (log == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Log not found"))
                call.respond(HttpStatusCode.OK, mapOf(
                    "data" to mapOf(
                        "userId" to log[Logs.userId],
                        "action" to log[Logs.action],
                        "details" to log[Logs.details]
                    )
                ))
            }
        }
    }
}