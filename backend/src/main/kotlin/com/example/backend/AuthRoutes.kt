package com.example.backend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.Date

fun Application.configureRouting() {
    val jwtSecret = System.getenv("JWT_SECRET") ?: "some_unique_secret"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: "http://localhost:8080"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "http://localhost:8080"

    routing {
        get("/") {
            call.respondText("OK")
        }
        get("/test-db") {
            val count = transaction {
                Users.selectAll().count()
            }
            call.respondText("Users in DB: $count")
        }
        post("/register") {
            try {
                val userInput = call.receive<UserRegisterInput>()

                if (!isValidEmail(userInput.email)) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid email format")
                    return@post
                }
                if (userInput.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, "Password must be at least 6 characters")
                    return@post
                }
                if (userInput.role !in listOf("doctor", "patient", "admin")) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid role. Must be 'doctor', 'patient', or 'admin'")
                    return@post
                }
                if (userInput.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Name cannot be empty")
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
                    call.respond(HttpStatusCode.Conflict, "Email already registered")
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

                call.respond(HttpStatusCode.Created, "User created with ID: $userId")
            } catch (e: Exception) {
                println("Registration error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Invalid input: ${e.message}")
            }
        }
        post("/login") {
            try {
                val loginInput = call.receive<UserLoginInput>()
                val user = transaction {
                    addLogger(StdOutSqlLogger)
                    Users.selectAll().where { Users.email eq loginInput.email.lowercase() }.firstOrNull()
                }
                if (user == null || !BCrypt.checkpw(loginInput.password, user[Users.passwordHash])) {
                    call.respond(HttpStatusCode.Unauthorized, "Wrong email or password")
                    return@post
                }

                val token = JWT.create()
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .withClaim("email", user[Users.email])
                    .withClaim("role", user[Users.role])
                    .withExpiresAt(Date(System.currentTimeMillis() + 604800000))
                    .sign(Algorithm.HMAC256(jwtSecret))

                call.respond(HttpStatusCode.OK, mapOf("token" to token, "message" to "Successful entry for ${user[Users.email]}"))
            } catch (e: Exception) {
                println("Login error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Invalid data: ${e.message}")
            }
        }
        authenticate("auth-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.payload?.getClaim("email")?.asString() ?: "Unknown"
                val role = principal?.payload?.getClaim("role")?.asString() ?: "Unknown"
                call.respondText("Hello, $email! Your role is $role")
            }
        }

    }
}