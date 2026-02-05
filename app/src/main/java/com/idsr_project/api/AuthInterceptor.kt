package com.idsr_project.api

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.idsr_project.utils.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {

    private val client = OkHttpClient()

    override fun intercept(chain: Interceptor.Chain): Response {


        var request = chain.request()

        val accessToken = SessionManager.getAccessToken(context)
        if (!accessToken.isNullOrEmpty()) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        }
        Log.d("AUTH", "Sending token: Bearer $accessToken")

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()

            val refreshToken = SessionManager.getRefreshToken(context)
            if (!refreshToken.isNullOrEmpty()) {
                val newAccessToken = refreshAccessToken(refreshToken)

                if (!newAccessToken.isNullOrEmpty()) {

                    SessionManager.saveTokens(context, newAccessToken, refreshToken)

                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()

                    return chain.proceed(newRequest)
                } else {
                    SessionManager.clearSession(context)
                    Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        return response
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        try {
            val url = "http://192.168.1.9:5000/api/auth/refresh-token"

            val json = JSONObject()
            json.put("refresh_token", refreshToken)

            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody ?: "")
                    if (jsonResponse.optBoolean("success", false)) {
                        return jsonResponse.optString("access_token", null)
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }
}
