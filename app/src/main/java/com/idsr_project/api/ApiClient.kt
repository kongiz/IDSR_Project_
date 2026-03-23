package com.idsr_project.api

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
        private const val SERVER_IP = "172.20.10.3"
        private const val SERVER_PORT = "5000"

        const val BASE_HOST = "http://$SERVER_IP:$SERVER_PORT"
        private const val BASE_URL = "$BASE_HOST/api/"


    fun getClient(context: Context): ApiServices {
        val gson = GsonBuilder().setLenient().create()

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()


        return retrofit.create(ApiServices::class.java)

    }
}