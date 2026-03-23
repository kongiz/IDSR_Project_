package com.idsr_project.api

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.idsr_project.activities.Login_Activity
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
                val tokens = refreshAccessToken(refreshToken)

                if (tokens != null) {
                    val (newAccessToken, newRefreshToken) = tokens

                    SessionManager.saveTokens(context, newAccessToken, newRefreshToken)

                    val newRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newAccessToken")
                        .build()

                    return chain.proceed(newRequest)
                } else {
                    handleSessionExpired()
                }
            } else {
                handleSessionExpired()
            }
        }

        return response
    }

    private fun handleSessionExpired() {
        SessionManager.clearSession(context)

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context,
                "Session expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()


            val intent = Intent(context, Login_Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }

    private fun refreshAccessToken(refreshToken: String): Pair<String, String>? {
        try {
            val url = "${ApiClient.BASE_HOST}/api/auth/refresh_token"

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
                        val newAccessToken = jsonResponse.optString("access_token")
                        val newRefreshToken = jsonResponse.optString("refresh_token")
                        if (newAccessToken.isNotEmpty() && newRefreshToken.isNotEmpty()) {
                            Log.d("AUTH", "Tokens refreshed successfully")
                            return Pair(newAccessToken, newRefreshToken)
                        }
                    } else {
                        Log.e("AUTH", "Token refresh failed: ${jsonResponse.optString("message")}")
                    }
                } else {
                    Log.e("AUTH", "Token refresh failed with code: ${response.code}")
                }
            }
        } catch (e: IOException) {
            Log.e("AUTH", "Token refresh exception: ${e.message}", e)
        }

        return null
    }
}