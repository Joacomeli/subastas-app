package com.subastasapp.data.api

import com.subastasapp.data.model.BidDto
import com.subastasapp.data.model.PujarRequestDto
import com.subastasapp.data.model.PujarResponseDto
import com.subastasapp.data.model.SubastaDetalleDto
import com.subastasapp.data.model.SubastaResumenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SubastaApiService {
    @GET("api/subastas")
    suspend fun getSubastas(): Response<List<SubastaResumenDto>>

    @GET("api/subastas/{id}/detalle")
    suspend fun getDetalle(@Path("id") id: Long): Response<SubastaDetalleDto>

    @GET("api/subastas/{id}/pujas")
    suspend fun getPujas(@Path("id") id: Long): Response<List<BidDto>>

    @POST("api/subastas/{id}/pujar")
    suspend fun pujar(@Path("id") id: Long, @Body request: PujarRequestDto): Response<PujarResponseDto>
}
