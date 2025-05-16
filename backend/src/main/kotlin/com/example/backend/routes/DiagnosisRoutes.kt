package com.example.backend.routes

import com.example.backend.models.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.diagnosisRoutes() {
    authenticate("auth-jwt") {
        post("/diagnoses") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            if (role != "doctor") {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only doctors can add diagnoses"))
            }

            val input = call.receive<Map<String, String>>()
            val imageId = input["imageId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Image ID required"))
            val diagnosis = input["diagnosis"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Diagnosis required"))
            val probability = input["probability"]?.toBigDecimalOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Probability required"))

            val image = transaction { Images.selectAll().where { Images.id eq imageId }.firstOrNull() }
            if (image == null) return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Image not found"))
            if (image[Images.qualityStatus] != "accepted") return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Image must be accepted"))
            if (image[Images.patientId] == null) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Image must be associated with a patient"))

            val patient = transaction { Users.selectAll().where { Users.id eq image[Images.patientId]!! }.firstOrNull() }
            if (patient == null || patient[Users.role] != "patient") return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid patient"))

            val diagnosisId = transaction {
                Diagnoses.insert {
                    it[Diagnoses.imageId] = imageId
                    it[Diagnoses.diagnosis] = diagnosis
                    it[Diagnoses.probability] = probability
                } get Diagnoses.id
            }

            transaction {
                Logs.insert {
                    it[Logs.userId] = userId
                    it[action] = "Added diagnosis"
                    it[details] = "Diagnosis ID: $diagnosisId for Image ID: $imageId"
                }
            }

            call.respond(HttpStatusCode.Created, mapOf("message" to "Diagnosis created with ID: $diagnosisId"))
        }

        get("/diagnoses") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val imageId = call.request.queryParameters["imageId"]?.toIntOrNull()

            val diagnoses = transaction {
                (Diagnoses innerJoin Images)
                    .selectAll()
                    .where {
                        val baseCondition = if (role == "admin") Op.TRUE
                        else if (role == "doctor") Images.userId eq userId
                        else if (role == "patient") Images.patientId eq userId
                        else Op.FALSE

                        if (imageId != null) {
                            baseCondition and (Diagnoses.imageId eq imageId)
                        } else {
                            baseCondition
                        }
                    }
                    .map {
                        mapOf(
                            "id" to it[Diagnoses.id],
                            "imageId" to it[Diagnoses.imageId],
                            "diagnosis" to it[Diagnoses.diagnosis],
                            "probability" to it[Diagnoses.probability]
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, mapOf("data" to diagnoses))
        }

        get("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction {
                Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull()
            }
            if (diagnosis == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction {
                Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull()
            }
            if (role != "admin" && image?.get(Images.userId) != userId && image?.get(Images.patientId) != userId) {
                return@get call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            call.respond(HttpStatusCode.OK, mapOf(
                "data" to mapOf(
                    "id" to diagnosis[Diagnoses.id],
                    "imageId" to diagnosis[Diagnoses.imageId],
                    "diagnosis" to diagnosis[Diagnoses.diagnosis],
                    "probability" to diagnosis[Diagnoses.probability]
                )
            ))
        }

        put("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction {
                Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull()
            }
            if (diagnosis == null) return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction {
                Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull()
            }
            if (role != "admin" && role != "doctor") {
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins or doctors can update diagnoses"))
            }
            val input = call.receive<Map<String, String>>()
            val newDiagnosis = input["diagnosis"] ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Diagnosis required"))
            transaction {
                Diagnoses.update({ Diagnoses.id eq diagnosisId }) {
                    it[Diagnoses.diagnosis] = newDiagnosis
                    it[probability] = input["probability"]?.toBigDecimal() ?: 1.0.toBigDecimal()
                }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Diagnosis updated"))
        }

        delete("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction {
                Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull()
            }
            if (diagnosis == null) return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction {
                Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull()
            }
            if (role != "admin" && role != "doctor") {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Only admins or doctors can delete diagnoses"))
            }
            transaction {
                Diagnoses.deleteWhere { id eq diagnosisId }
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Diagnosis deleted"))
        }
    }
}