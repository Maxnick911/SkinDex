package com.example.backend.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.dao.id.IntIdTable


object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50)
    val name = varchar("name", 255)
    val doctorId = integer("doctor_id").references(id).nullable()
}

object Images : IntIdTable() {
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val patientId = integer("patient_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val filePath = varchar("file_path", 255)
    val uploadDate = datetime("upload_date").defaultExpression(CurrentDateTime)
    val qualityStatus = varchar("quality_status", 20)
    val qualityComment = text("quality_comment").nullable()
}

object Diagnoses : IntIdTable() {
    val imageId = integer("image_id").references(Images.id, onDelete = ReferenceOption.CASCADE)
    val diagnosis = varchar("diagnosis", 255)
    val probability = decimal("probability", 5, 2)
    val doctorComment = text("doctor_comment").nullable()
    val dateAdded = datetime("date_added").defaultExpression(CurrentDateTime)
}

object Logs : IntIdTable() {
    val userId = integer("user_id").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    val action = varchar("action", 255)
    val details = text("details").nullable()
    val timestamp = datetime("timestamp").defaultExpression(CurrentDateTime)
}