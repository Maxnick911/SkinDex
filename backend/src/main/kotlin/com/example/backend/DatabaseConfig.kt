package com.example.backend

import com.example.backend.models.Users
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

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