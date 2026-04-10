package com.idsr_project.api

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.idsr_project.utils.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AuthInterceptor(private val context: Context) : Interceptor {

    private val refreshClient = OkHttpClient()

    companion object {
        private const val TAG = "AuthInterceptor"

        private val AUTH_PATHS = listOf(
            "/api/v1/login",                   // POST login
            "/api/v1/signup",                  // POST register
            "/api/v1/adminRegister",           // POST admin register
            "/api/v1/logout",                  // POST logout — 401 here means already logged out, not session expired
            "/api/auth/v1/refresh_token",      // POST token refresh (used internally)
            "/api/auth/v1/resend-otp",         // POST resend OTP
            "/api/auth/v1/verify-email",       // POST verify email
            "/api/auth/v1/forgot-password",    // POST forgot password
            "/api/auth/v1/verify-reset-otp",   // POST verify reset OTP
            "/api/auth/v1/reset-password",     // POST reset password
        )

        private const val MAX_RETRY_COUNT  = 2
        private const val RETRY_COUNT_KEY  = "Retry-Count"

        private const val DEFAULT_RETRY_AFTER_SECONDS = 60L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val accessToken = SessionManager.getAccessToken(context)
        if (!accessToken.isNullOrEmpty()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        }

        val isAuthPath = AUTH_PATHS.any { request.url.encodedPath.contains(it) }

        val response = chain.proceed(request)

        return when {

            response.code == 401 -> {
                if (isAuthPath) {
                    // Login/register/etc: bad credentials — let the screen handle it.
                    Log.d(TAG, "401 on auth path — passing through to caller")
                    response
                } else {
                    handle401(chain, request, response)
                }
            }

            response.code == 429 -> {
                handle429(chain, request, response)
            }

            response.code == 403 -> {
                if (!isAuthPath) {
                    Handler(Looper.getMainLooper()).post {
                        com.idsr_project.activities.Error_Activity.launch403(context)
                    }
                }
                response
            }

            response.code == 500 -> {
                Handler(Looper.getMainLooper()).post {
                    com.idsr_project.activities.Error_Activity.launch500(context)
                }
                response
            }

            else -> response
        }
    }

    private fun handle401(
        chain: Interceptor.Chain,
        request: Request,
        response: Response
    ): Response {
        response.close()

        val refreshToken = SessionManager.getRefreshToken(context)
        if (refreshToken.isNullOrEmpty()) {
            Log.w(TAG, "401 with no refresh token — session expired")
            handleSessionExpired()
            return buildErrorResponse(request, 401, "Session expired")
        }

        Log.d(TAG, "401 received — attempting token refresh")
        val tokens = refreshAccessToken(refreshToken)

        return if (tokens != null) {
            val (newAccessToken, newRefreshToken) = tokens
            SessionManager.saveTokens(context, newAccessToken, newRefreshToken)
            Log.d(TAG, "Token refreshed — retrying original request")

            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
            chain.proceed(retryRequest)
        } else {
            Log.e(TAG, "Token refresh failed — session expired")
            handleSessionExpired()
            buildErrorResponse(request, 401, "Session expired")
        }
    }


    private fun handle429(
        chain: Interceptor.Chain,
        request: Request,
        response: Response
    ): Response {
        val retryCount = request.header(RETRY_COUNT_KEY)?.toIntOrNull() ?: 0

        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "429: max retries ($MAX_RETRY_COUNT) reached for ${request.url}")
            return response
        }


        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
            ?: DEFAULT_RETRY_AFTER_SECONDS
        response.close()

        Log.w(TAG, "429: waiting ${retryAfterSeconds}s before retry ${retryCount + 1}/$MAX_RETRY_COUNT")

        try {
            Thread.sleep(retryAfterSeconds * 1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return buildErrorResponse(request, 429, "Retry interrupted")
        }

        val retryRequest = request.newBuilder()
            .header(RETRY_COUNT_KEY, (retryCount + 1).toString())
            .build()

        return chain.proceed(retryRequest)
    }

    private fun handleSessionExpired() {
        SessionManager.clearSession(context)
        Handler(Looper.getMainLooper()).post {
            com.idsr_project.activities.Error_Activity.launch401(context)
        }
    }


    private fun refreshAccessToken(refreshToken: String): Pair<String, String>? {
        return try {
            val url  = "${ApiClient.BASE_HOST}/api/v1/auth/refresh_token"
            val json = JSONObject().apply { put("refresh_token", refreshToken) }
            val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            refreshClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Refresh failed with HTTP ${response.code}")
                    return null
                }

                val responseBody  = response.body.string() ?: return null
                val jsonResponse  = JSONObject(responseBody)

                if (!jsonResponse.optBoolean("success", false)) {
                    Log.e(TAG, "Refresh rejected: ${jsonResponse.optString("message")}")
                    return null
                }

                val newAccessToken  = jsonResponse.optString("access_token")
                val newRefreshToken = jsonResponse.optString("refresh_token")

                if (newAccessToken.isEmpty() || newRefreshToken.isEmpty()) {
                    Log.e(TAG, "Refresh response missing tokens")
                    return null
                }

                Log.d(TAG, "Tokens refreshed successfully")
                Pair(newAccessToken, newRefreshToken)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Refresh network error: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Refresh unexpected error: ${e.message}", e)
            null
        }
    }

    private fun buildErrorResponse(request: Request, code: Int, message: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(ResponseBody.create(null, ByteArray(0)))
            .build()
    }
}