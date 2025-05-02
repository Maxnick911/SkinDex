package com.example.backend

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.jackson.jackson
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import org.flywaydb.core.Flyway
import java.util.regex.Pattern

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
    val databaseUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://db:5432/skindex?createDatabaseIfNotExist=true"
    val databaseUser = System.getenv("DATABASE_USER") ?: "admin"
    val databasePassword = System.getenv("DATABASE_PASSWORD") ?: "secret"

    Flyway.configure()
        .dataSource(databaseUrl, databaseUser, databasePassword)
        .locations("db/migration")
        .load()
        .migrate()

    val database = try {
        Database.connect(
            url = databaseUrl,
            driver = "org.postgresql.Driver",
            user = databaseUser,
            password = databasePassword
        )
    } catch (e: Exception) {
        println("Failed to connect to database: ${e.message}")
        throw e
    }

    try {
        transaction(database) {
            val testQuery = Users.selectAll().count()
            println("Test query result: $testQuery")
        }
    } catch (e: Exception) {
        println("Test transaction failed: ${e.message}")
        throw e
    }

    if (System.getenv("ENV") == "local") {
        transaction(database) {
            exec(readSqlFile("db/seed.sql"))
        }
    }
}

fun readSqlFile(path: String): String {
    return object {}.javaClass.classLoader.getResource(path)?.readText()
        ?: throw IllegalStateException("SQL file $path not found")
}

fun Application.configureRouting() {
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
                call.respond(HttpStatusCode.OK, "Successful entry for ${user[Users.email]}")
            } catch (e: Exception) {
                println("Login error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, "Invalid data: ${e.message}")
            }
        }
    }
}

data class UserRegisterInput(val email: String, val password: String, val role: String, val name: String)

data class UserLoginInput(val email: String, val password: String)

fun isValidEmail(email: String): Boolean {
    val emailPattern = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    )
    return emailPattern.matcher(email).matches()
}