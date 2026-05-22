package com.idsr_project.api

import android.content.Context
import com.google.firebase.perf.FirebasePerformance
import com.google.gson.GsonBuilder
import com.idsr_project.BuildConfig
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val SERVER_IP   = BuildConfig.SERVER_IP
    private const val SERVER_PORT = BuildConfig.SERVER_PORT

    private const val PINNED_HOST = BuildConfig.PINNED_HOST

    internal val BASE_URL = "https://$PINNED_HOST/api/v1/"

    @Volatile
    var instance: ApiServices? = null

    fun getClient(context: Context): ApiServices {
        return instance ?: synchronized(this) {
            instance ?: buildClient(context.applicationContext).also { instance = it }
        }
    }

    private fun buildClient(context: Context): ApiServices {
        val gson = GsonBuilder()
            .setLenient()
            .create()

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(NetworkInterceptor(context))
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(logging)
            .addInterceptor(FirebasePerformanceInterceptor())
            .apply {
                // Only pin in production — skip during local development
                certificatePinner(buildCertificatePinner())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiServices::class.java)
    }

    private fun buildCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(PINNED_HOST, "sha256/${BuildConfig.SSL_PIN_PRIMARY}")
            .add(PINNED_HOST, "sha256/${BuildConfig.SSL_PIN_BACKUP}")
            .build()
    }
}