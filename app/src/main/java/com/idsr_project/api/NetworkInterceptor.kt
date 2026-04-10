package com.idsr_project.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.idsr_project.activities.Error_Activity
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class NetworkInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isOnline()) {
            throw IOException("No internet connection")
        }

        return try {
            val response = chain.proceed(chain.request())

            if (response.code == 404) {
                Handler(Looper.getMainLooper()).post {
                    Error_Activity.launch404(context)
                }
            }

            response
        } catch (e: IOException) {
            throw e
        }
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}