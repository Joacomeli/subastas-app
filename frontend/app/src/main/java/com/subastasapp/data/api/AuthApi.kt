package com.subastasapp.data.api

import com.subastasapp.data.model.JwtResponse
import com.subastasapp.data.model.LoginRequest
import com.subastasapp.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<JwtResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<String>
}
