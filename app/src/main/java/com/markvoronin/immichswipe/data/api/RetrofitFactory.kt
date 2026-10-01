package com.markvoronin.immichswipe.data.api

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object RetrofitFactory {
    fun create(config: SessionConfig): ImmichApi {
        // Intercepteur pour logger les requêtes et réponses HTTP
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Intercepteur pour ajouter automatiquement la clé API dans les headers de chaque requête.
        val apiKeyInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", config.apiKey)
                .build()
            chain.proceed(request)
        }

        // Intercepteur de Diagnostic pour logger les requêtes réseau
        val connectivityInterceptor = Interceptor { chain ->
            val request = chain.request()
            val urlPath = request.url.encodedPath
            try {
                val response = chain.proceed(request)
                
                when (response.code) {
                    in 200..299 -> {
                        // Success response
                    }
                    401, 403 -> {
                        AppLogger.e("Retrofit", "Erreur d'authentification (${response.code}) sur $urlPath")
                    }
                    404 -> {
                        AppLogger.d("Retrofit", "Ressource non trouvée (404) sur $urlPath")
                    }
                    502, 503, 504 -> {
                        AppLogger.e("Retrofit", "Serveur indisponible (${response.code}) sur $urlPath")
                    }
                    else -> {
                        if (response.code >= 500) {
                            AppLogger.w("Retrofit", "Erreur serveur (${response.code}) sur $urlPath")
                        }
                    }
                }
                response
            } catch (e: Exception) {
                val isCanceled = (e is IOException && e.message?.contains("canceled", ignoreCase = true) == true) ||
                        e.message?.contains("socket closed", ignoreCase = true) == true ||
                        e.message?.contains("stream was reset", ignoreCase = true) == true

                if (!isCanceled) {
                    val statusStr = when (e) {
                        is UnknownHostException -> "DNS_ERROR"
                        is SocketTimeoutException -> "TIMEOUT"
                        is IOException -> "NO_INTERNET"
                        else -> "CONNECTION_ERROR"
                    }
                    AppLogger.e("Retrofit", "Erreur réseau ($statusStr) sur $urlPath: ${e.message}", e)
                }
                throw e
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(logging)
            .build()

        // Configure Retrofit avec l'URL de base, le client HTTP et le convertisseur JSON (Gson).
        // On s'assure que l'URL se termine par un slash pour Retrofit.
        val sanitizedUrl = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"
        
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ImmichApi::class.java)
    }
}
