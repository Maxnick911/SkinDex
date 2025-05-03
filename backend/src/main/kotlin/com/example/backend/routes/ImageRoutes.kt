package com.example.backend.routes

import com.example.backend.models.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

fun Route.imageRoutes() {
    authenticate("auth-jwt") {
        // upload image
        post("/upload-image") {
            try {
                val principal = call.principal<JWTPrincipal>()
                val email = principal?.payload?.getClaim("email")?.asString()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))

                val user = transaction {
                    Users.selectAll().where { Users.email eq email }.firstOrNull()
                } ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))

                val multipart = call.receiveMultipart()
                var filePath: String? = null

                multipart.forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val fileName = "uploads/${System.currentTimeMillis()}-${part.originalFileName}"
                        val file = File(fileName)
                        file.parentFile.mkdirs()
                        val channel = part.provider()
                        channel.copyAndClose(file.writeChannel())
                        filePath = fileName
                    }
                    part.dispose()
                }

                if (filePath == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No image provided"))
                    return@post
                }

                val imageId = transaction {
                    Images.insert {
                        it[userId] = if (user[Users.role] == "patient") user[Users.id] else null
                        it[patientId] = if (user[Users.role] == "patient") user[Users.id] else null
                        it[Images.filePath] = filePath
                        it[qualityStatus] = "pending"
                    } get Images.id
                }

                transaction {
                    Logs.insert {
                        it[userId] = user[Users.id]
                        it[action] = "Uploaded image"
                        it[details] = "Image ID: $imageId"
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("message" to "Image uploaded with ID: $imageId"))
            } catch (e: Exception) {
                println("Upload error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Error: ${e.message}"))
            }
        }

        // get all images
        get("/images") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val images = transaction {
                val query = when (role) {
                    "admin" -> Images.selectAll()
                    "doctor" -> Images.selectAll().where { Images.userId eq userId }
                    else -> Images.selectAll().where { Images.patientId eq userId }
                }
                query.toList().map {
                    mapOf(
                        "id" to it[Images.id],
                        "filePath" to it[Images.filePath],
                        "qualityStatus" to it[Images.qualityStatus],
                        "userId" to it[Images.userId],
                        "patientId" to it[Images.patientId]
                    )
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to images))
        }

        // get image by id
        get("/images/{id}") {
            val imageId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val image = transaction {
                Images.selectAll().where { Images.id eq imageId }.firstOrNull()
            }
            if (image == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (role != "admin" && image[Images.userId] != userId && image[Images.patientId] != userId) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            call.respond(HttpStatusCode.OK, mapOf(
                "data" to mapOf(
                    "id" to image[Images.id],
                    "filePath" to image[Images.filePath],
                    "qualityStatus" to image[Images.qualityStatus],
                    "userId" to image[Images.userId],
                    "patientId" to image[Images.patientId]
                )
            ))
        }

        // update image
        put("/images/{id}") {
            val imageId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val image = transaction { Images.selectAll().where { Images.id eq imageId }.firstOrNull() }
            if (image == null) return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (role != "admin" && image[Images.userId] != userId) return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            val input = call.receive<Map<String, String>>()
            val qualityStatus = input["qualityStatus"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Quality status required"))
            transaction {
                Images.update({ Images.id eq imageId }) {
                    it[Images.qualityStatus] = qualityStatus
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Image updated"))
        }

        // delete image
        delete("/images/{id}") {
            val imageId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val image = transaction { Images.selectAll().where { Images.id eq imageId }.firstOrNull() }
            if (image == null) return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (role != "admin" && image[Images.userId] != userId) return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            transaction {
                File(image[Images.filePath]).delete()
                Images.deleteWhere { Images.id eq imageId }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Image deleted"))
        }
    }
}