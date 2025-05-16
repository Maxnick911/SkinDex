package com.example.skindex.data.api.model.auth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRegisterResponse(
    val message: String,
)