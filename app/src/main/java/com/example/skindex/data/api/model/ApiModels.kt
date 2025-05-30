package com.example.skindex.data.api.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class QualityStatusUpdate(val qualityStatus: String)

@JsonClass(generateAdapter = true)
data class DiagnosisRequest(val imageId: Int,
                            val diagnosis: String,
                            val probability: Float)

@JsonClass(generateAdapter = true)
data class ImageResponse(val id: Long,
                         val filePath: String,
                         val description: String?)

@JsonClass(generateAdapter = true)
data class DiagnosisResponse(val id: Long,
                             val imageId: Long,
                             @Json(name = "diagnosis") val diseaseName: String,
                             @Json(name = "probability") val confidence: Float,
                             @Json(name = "doctorComment") val doctorComment: String?)

@JsonClass(generateAdapter = true)
data class UploadResponse(val message: String)

@JsonClass(generateAdapter = true)
data class UserResponse(val data: UserData)

@JsonClass(generateAdapter = true)
data class UserData(val id: Int,
                    val email: String,
                    val name: String,
                    val role: String)

@JsonClass(generateAdapter = true)
data class Patient(val id: Int,
                   val name: String,
                   val email: String)

@JsonClass(generateAdapter = true)
data class ImageListResponse(val data: List<ImageResponse>)

@JsonClass(generateAdapter = true)
data class DiagnosisListResponse(val data: List<DiagnosisResponse>)

@JsonClass(generateAdapter = true)
data class SingleImageResponse(val data: ImageResponse)

@JsonClass(generateAdapter = true)
data class SingleDiagnosisResponse(val data: DiagnosisResponse)

@JsonClass(generateAdapter = true)
data class PatientListResponse(
    val data: List<Patient>
)