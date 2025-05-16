package com.example.skindex.core.util

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import java.util.Date
import javax.inject.Inject

class JwtUtils @Inject constructor() {

    fun getEmailFromToken(token: String): String? {
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.getClaim("email").asString()
        } catch (e: JWTDecodeException) {
            null
        }
    }

    fun getUserIdFromToken(token: String): Int? {
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.getClaim("userId").asInt()
        } catch (e: JWTDecodeException) {
            null
        }
    }

    fun getNameFromToken(token: String): String? {
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.getClaim("name").asString()
        } catch (e: JWTDecodeException) {
            null
        }
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            decodedJWT.expiresAt?.before(Date()) != false
        } catch (e: JWTDecodeException) {
            true
        }
    }
}