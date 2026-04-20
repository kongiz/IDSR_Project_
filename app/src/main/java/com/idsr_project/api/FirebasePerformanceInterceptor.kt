package com.idsr_project.api

import com.google.firebase.perf.FirebasePerformance
import okhttp3.Interceptor
import okhttp3.Response

class FirebasePerformanceInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val urlPath  = request.url.encodedPath
        val method   = request.method
        val traceName = "$method $urlPath"

        val trace = FirebasePerformance.getInstance()
            .newHttpMetric(request.url.toString(), method)

        trace.start()

        val response: Response
        try {
            response = chain.proceed(request)
            trace.setHttpResponseCode(response.code)
            trace.setResponseContentType(response.header("Content-Type"))
            val contentLength = response.body.contentLength() ?: -1
            if (contentLength > 0) trace.setResponsePayloadSize(contentLength)
        } finally {
            trace.stop()
        }

        return response
    }
}