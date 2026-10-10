package com.markvoronin.immichswipe.data.api

import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.DiagStatus
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
    fun create(
        config: SessionConfig,
        onStatusUpdate: ((level: ConnectionLevel, type: DiagStatus, statusCode: Int?, rawMessage: String?) -> Unit)? = null
    ): ImmichApi {

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }


        val apiKeyInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", config.apiKey)
                .build()
            chain.proceed(request)
        }


        val connectivityInterceptor = Interceptor { chain ->
            val request = chain.request()
            val urlPath = request.url.encodedPath
            try {
                val response = chain.proceed(request)
                
                when (response.code) {
                    in 200..299 -> {
                        onStatusUpdate?.invoke(ConnectionLevel.ONLINE, DiagStatus.CONNECTED, response.code, null)
                    }
                    401, 403 -> {
                        AppLogger.e("Retrofit", "Authentication error (${response.code}) on $urlPath")
                        onStatusUpdate?.invoke(ConnectionLevel.ISSUES, DiagStatus.AUTH_ERROR, response.code, "Authentication error")
                    }
                    404 -> {
                        AppLogger.d("Retrofit", "Resource not found (404) on $urlPath")
                    }
                    502, 503, 504 -> {
                        AppLogger.e("Retrofit", "Server unavailable (${response.code}) on $urlPath")
                        onStatusUpdate?.invoke(ConnectionLevel.ISSUES, DiagStatus.UNAVAILABLE, response.code, "Server unavailable")
                    }
                    else -> {
                        if (response.code >= 500) {
                            AppLogger.w("Retrofit", "Server error (${response.code}) on $urlPath")
                            onStatusUpdate?.invoke(ConnectionLevel.ISSUES, DiagStatus.UNEXPECTED, response.code, "Server error")
                        }
                    }
                }
                response
            } catch (e: Exception) {
                val isCanceled = (e is IOException && e.message?.contains("canceled", ignoreCase = true) == true) ||
                        e.message?.contains("socket closed", ignoreCase = true) == true ||
                        e.message?.contains("stream was reset", ignoreCase = true) == true

                if (!isCanceled) {
                    val (level, statusType) = when (e) {
                        is UnknownHostException -> ConnectionLevel.OFFLINE to DiagStatus.DNS_ERROR
                        is SocketTimeoutException -> ConnectionLevel.ISSUES to DiagStatus.TIMEOUT
                        is IOException -> ConnectionLevel.OFFLINE to DiagStatus.NO_INTERNET
                        else -> ConnectionLevel.OFFLINE to DiagStatus.CONNECTION_ERROR
                    }
                    onStatusUpdate?.invoke(level, statusType, null, e.message)
                    AppLogger.e("Retrofit", "Network error ($statusType) on $urlPath: ${e.message}", e)
                }
                throw e
            }
        }

        val retryInterceptor = Interceptor { chain ->
            val request = chain.request()
            var response: okhttp3.Response? = null
            var exception: Exception? = null
            var tryCount = 0
            val maxRetries = 3

            while (true) {
                try {
                    response?.close() // Close the previous response if it exists
                    response = chain.proceed(request)
                    if (response.isSuccessful || tryCount >= maxRetries - 1) {
                        break
                    }
                } catch (e: Exception) {
                    exception = e
                    val isTransientError = e is UnknownHostException || e is SocketTimeoutException || e is IOException
                    
                    if (!isTransientError || tryCount >= maxRetries - 1) {
                        break
                    }
                }
                
                tryCount++
                // Exponential backoff: 500ms, 1000ms, 2000ms...
                val sleepTime = 500L * (1 shl (tryCount - 1))
                AppLogger.d("Retrofit", "Network request failed. Retrying ($tryCount/$maxRetries) in ${sleepTime}ms...")
                Thread.sleep(sleepTime)
            }

            // If we successfully got a response, return it. Otherwise, throw the last exception.
            response ?: throw exception ?: IOException("Unknown network error occurred after $maxRetries retries")
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(retryInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(connectivityInterceptor)
            .addInterceptor(logging)
            .build()


        val sanitizedUrl = if (config.baseUrl.endsWith("/")) config.baseUrl else "${config.baseUrl}/"
        
        val retrofit = Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ImmichApi::class.java)
    }
}
