package com.example.backend.routes

import com.example.backend.models.Diagnoses
import com.example.backend.models.Images
import com.example.backend.models.Users
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.log
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
        post("/upload-image") {
            val principal     = call.principal<JWTPrincipal>()!!
            val doctorId      = principal.payload.getClaim("userId").asInt()
            val role          = principal.payload.getClaim("role").asString()
            if (role != "doctor") {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only doctors can upload images"))
            }

            val multipart     = call.receiveMultipart()
            var tempFilePath: String? = null
            var patientIdRaw : String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val tmpName = "uploads/tmp-${System.currentTimeMillis()}-${part.originalFileName}"
                        val f       = File(tmpName).apply { parentFile.mkdirs() }
                        part.provider().copyAndClose(f.writeChannel())
                        tempFilePath = tmpName
                    }
                    is PartData.FormItem -> {
                        if (part.name == "patientId") {
                            patientIdRaw = part.value
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            if (tempFilePath == null) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No image provided"))
            }
            val patientId = patientIdRaw?.toIntOrNull()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Patient ID required"))

            val patient = transaction {
                Users.selectAll().where { Users.id eq patientId }.singleOrNull()
            } ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid patient ID"))

            if (patient[Users.role] != "patient") {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User is not a patient"))
            }

            val imageId = transaction {
                Images.insert {
                    it[userId]       = doctorId
                    it[this.patientId] = patientId
                    it[filePath]     = tempFilePath
                    it[qualityStatus]= "pending"
                } get Images.id
            }

            val newFileName = "uploads/img_${imageId}_p${patientId}_${System.currentTimeMillis()}.jpg"
            val tmpFile      = File(tempFilePath)
            val newFile      = File(newFileName)
            tmpFile.parentFile.mkdirs()
            if (tmpFile.renameTo(newFile)) {
                transaction {
                    Images.update({ Images.id eq imageId }) {
                        it[filePath] = newFileName
                    }
                }
            } else {
                application.log.error("Failed to rename $tempFilePath to $newFileName")
            }

            call.respond(HttpStatusCode.OK, mapOf("message" to "Image uploaded with ID: ${imageId.value}"))
        }

        get("/images") {
            val principal = call.principal<JWTPrincipal>()!!
            val meId      = principal.payload.getClaim("userId").asInt()
            val role      = principal.payload.getClaim("role").asString()

            val patientIdParam = call.request.queryParameters["patientId"]?.toIntOrNull()

            val images = transaction {
                var query = Images.selectAll()

                if(patientIdParam != null) {
                    query = query.andWhere { Images.patientId eq patientIdParam }
                } else {
                    query = when (role) {
                        "admin"   -> query
                        "doctor"  -> query.andWhere { Images.userId eq meId }
                        "patient" -> query.andWhere { Images.patientId eq meId }
                        else      -> query.andWhere { Op.FALSE }
                    }
                }

                query.map {
                    mapOf(
                        "id"            to it[Images.id].value,
                        "filePath"      to it[Images.filePath],
                        "qualityStatus" to it[Images.qualityStatus],
                        "userId"        to it[Images.userId],
                        "patientId"     to it[Images.patientId]
                    )
                }
            }

            call.respond(HttpStatusCode.OK, mapOf("data" to images))
        }

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
                    "id" to image[Images.id].value,
                    "filePath" to image[Images.filePath],
                    "qualityStatus" to image[Images.qualityStatus],
                    "userId" to image[Images.userId],
                    "patientId" to image[Images.patientId]
                )
            ))
        }

        put("/images/{id}") {
            val imageId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val image = transaction { Images.selectAll().where { Images.id eq imageId }.firstOrNull() }
            if (image == null) return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (role != "admin" && image[Images.userId] != userId) {
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            val input = call.receive<Map<String, String>>()
            val qualityStatus = input["qualityStatus"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Quality status required"))
            transaction {
                Images.update({ Images.id eq imageId }) {
                    it[Images.qualityStatus] = qualityStatus
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Image updated"))
        }

        delete("/images/{id}") {
            val imageId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val image = transaction { Images.selectAll().where { Images.id eq imageId }.firstOrNull() }
            if (image == null) return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (role != "admin" && image[Images.userId] != userId) {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            transaction {
                Diagnoses.deleteWhere { Diagnoses.imageId eq imageId }
                File(image[Images.filePath]).delete()
                Images.deleteWhere { id eq imageId }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Image and related diagnosis deleted"))
        }
    }
}