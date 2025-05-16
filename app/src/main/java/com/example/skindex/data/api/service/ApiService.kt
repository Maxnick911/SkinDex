package com.example.skindex.data.api.service

import com.example.skindex.data.api.model.*
import com.example.skindex.data.api.model.auth.PatientRegisterRequest
import com.example.skindex.data.api.model.auth.UserRegisterResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") id: Int): UserResponse

    @GET("patients")
    suspend fun getPatients(): List<Patient>

    @POST("add-patient")
    suspend fun addPatient(@Body request: PatientRegisterRequest): UserRegisterResponse

    @Multipart
    @POST("upload-image")
    suspend fun uploadImage(@Part image: MultipartBody.Part, @Part("patientId") patientId: RequestBody): Response<UploadResponse>

    @PUT("images/{id}")
    suspend fun updateImage(@Path("id") id: Int, @Body status: QualityStatusUpdate): Response<Unit>

    @POST("diagnoses")
    suspend fun postDiagnosis(@Body req: DiagnosisRequest): Response<Unit>

    @GET("images")
    suspend fun getImagesByPatient(@Query("patientId") patientId: Int): ImageListResponse

    @GET("diagnoses")
    suspend fun getDiagnosesByImage(@Query("imageId") imageId: Int): DiagnosisListResponse

    @GET("images/{id}")
    suspend fun getImageById(@Path("id") id: Int): SingleImageResponse

    @GET("diagnoses/{id}")
    suspend fun getDiagnosisById(@Path("id") id: Int): SingleDiagnosisResponse

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): Response<Unit>

    @DELETE("images/{id}")
    suspend fun deleteImage(@Path("id") id: Int): Response<Unit>

    @DELETE("diagnoses/{id}")
    suspend fun deleteDiagnosis(@Path("id") id: Int): Response<Unit>
}