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
        // diagnosis - get all
        get("/diagnoses") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnoses = transaction {
                Diagnoses.selectAll().where {
                    if (role == "admin") Op.TRUE else Images.id eq Diagnoses.imageId and
                            if (role == "doctor") Images.userId eq userId else Images.patientId eq userId
                }.map {
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

        // diagnosis - get by id
        get("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction {
                Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull()
            }
            if (diagnosis == null) return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction { Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull() }
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

        // diagnosis - update
        put("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction { Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull() }
            if (diagnosis == null) return@put call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction { Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull() }
            if (role != "admin" && role != "doctor" && image?.get(Images.userId) != userId) {
                return@put call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
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

        // diagnosis - delete
        delete("/diagnoses/{id}") {
            val diagnosisId = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val role = principal?.payload?.getClaim("role")?.asString()
            val diagnosis = transaction { Diagnoses.selectAll().where { Diagnoses.id eq diagnosisId }.firstOrNull() }
            if (diagnosis == null) return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Diagnosis not found"))
            val image = transaction { Images.selectAll().where { Images.id eq diagnosis[Diagnoses.imageId] }.firstOrNull() }
            if (role != "admin" && role != "doctor" && image?.get(Images.userId) != userId) {
                return@delete call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
            }
            transaction { Diagnoses.deleteWhere { Diagnoses.id eq diagnosisId } }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Diagnosis deleted"))
        }
    }
}