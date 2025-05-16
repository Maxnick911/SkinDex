package com.example.backend.models

import java.util.regex.Pattern

data class UserRegisterInput(val email: String, val password: String, val role: String, val name: String)

data class UserLoginInput(val email: String, val password: String)

data class PatientInput(val email: String, val name: String)

fun isValidEmail(email: String): Boolean {
    val emailPattern = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    )
    return emailPattern.matcher(email).matches()
}