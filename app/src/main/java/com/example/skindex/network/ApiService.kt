package com.example.skindex.network

import retrofit2.http.GET
import retrofit2.http.Path
import com.example.skindex.data.Post

interface ApiService {
    @GET("posts")
    suspend fun getPosts(): List<Post>

    @GET("posts/{id}")
    suspend fun getPostById(@Path("id") id: Int): Post
}