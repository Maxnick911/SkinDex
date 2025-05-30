package com.example.backend

import com.example.backend.models.Users
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

fun parseRenderDatabaseUrl(): Triple<String, String, String> {
    val databaseUrl = System.getenv("DATABASE_URL")
    println("DATABASE_URL from environment: $databaseUrl")
    if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
        val uri = URI(databaseUrl)
        val userInfo = uri.userInfo.split(":")
        val jdbcUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}"
        println("Parsed JDBC URL: $jdbcUrl, User: ${userInfo[0]}, Password: ${userInfo[1]}")
        return Triple(jdbcUrl, userInfo[0], userInfo[1])
    } else {
        val fallbackUrl = "jdbc:postgresql://db:5432/skindex"
        val fallbackUser = "admin"
        val fallbackPassword = "secret"
        println("Using fallback: $fallbackUrl, User: $fallbackUser, Password: $fallbackPassword")
        return Triple(fallbackUrl, fallbackUser, fallbackPassword)
    }
}

fun Application.configureDatabase() {
    val (databaseUrl, databaseUser, databasePassword) = parseRenderDatabaseUrl()

    val (jdbcUrl, username, password) = parseRenderDatabaseUrl()
    Flyway.configure()
        .dataSource(jdbcUrl, username, password)
        .locations("db/migration")
        .load()
        .migrate()

    val database = Database.connect(
        url = jdbcUrl,
        driver = "org.postgresql.Driver",
        user = username,
        password = password
    )

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