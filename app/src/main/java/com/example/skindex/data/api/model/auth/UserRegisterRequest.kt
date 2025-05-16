package com.example.skindex.data.api.model.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String = "doctor"
)