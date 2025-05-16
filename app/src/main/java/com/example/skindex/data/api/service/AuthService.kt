package com.example.skindex.data.api.service

import com.example.skindex.data.api.model.auth.UserLoginRequest
import com.example.skindex.data.api.model.auth.UserLoginResponse
import com.example.skindex.data.api.model.auth.UserRegisterRequest
import com.example.skindex.data.api.model.auth.UserRegisterResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("login")
    suspend fun login(@Body request: UserLoginRequest): UserLoginResponse

    @POST("register")
    suspend fun register(@Body request: UserRegisterRequest): UserRegisterResponse
}