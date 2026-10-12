package com.subastasapp.data.api

import android.content.Context
import com.subastasapp.data.datastore.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // La URL se inyecta vía buildConfigField en app/build.gradle (propiedad -PsubastasBaseUrl;
    // por defecto http://10.0.2.2:8080/, el backend local visto desde el emulador).
    private val BASE_URL: String = com.subastasapp.BuildConfig.BASE_URL

    /** En modo demo, el último interceptor responde en memoria y nunca llega a la red. */
    private fun OkHttpClient.Builder.withDemo(): OkHttpClient.Builder {
        if (com.subastasapp.BuildConfig.DEMO_MODE) addInterceptor(DemoBackend)
        return this
    }

    private fun getLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun getAuthInterceptor(tokenManager: TokenManager) = Interceptor { chain ->
        val token = runBlocking { tokenManager.token.first() }
        val request = chain.request().newBuilder()
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        chain.proceed(request.build())
    }

    fun getAuthApiService(): AuthApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().addInterceptor(getLoggingInterceptor()).withDemo().build())
            .build()
            .create(AuthApiService::class.java)
    }

    fun getUserApiService(context: Context): UserApiService {
        val tokenManager = TokenManager(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(getLoggingInterceptor())
            .addInterceptor(getAuthInterceptor(tokenManager))
            .withDemo()
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(UserApiService::class.java)
    }

    // Subastas: los GET son públicos, pero pujar requiere el token -> incluimos el auth interceptor.
    fun getSubastaApiService(context: Context): SubastaApiService {
        val tokenManager = TokenManager(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(getLoggingInterceptor())
            .addInterceptor(getAuthInterceptor(tokenManager))
            .withDemo()
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(SubastaApiService::class.java)
    }
}
