package com.eyedroid.vpn.data.api

import android.content.Context
import android.content.Intent
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

class DebugInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val reqBody = req.body?.let {
            val buf = Buffer(); it.writeTo(buf); buf.readUtf8()
        } ?: ""

        val response = chain.proceed(req)
        val rawBody = response.body?.string() ?: ""
        val contentType = response.body?.contentType()

        val log = buildString {
            append("▶ ${req.method} ${req.url}\n")
            if (reqBody.isNotBlank()) append("\n📤 Request:\n$reqBody\n")
            append("\n📥 Response [${response.code}]:\n$rawBody")
        }

        context.sendBroadcast(Intent(ACTION_LOG).putExtra(EXTRA_LOG, log))

        return response.newBuilder()
            .body(rawBody.toResponseBody(contentType))
            .build()
    }

    companion object {
        const val ACTION_LOG = "com.eyedroid.vpn.DEBUG_LOG"
        const val EXTRA_LOG = "log"
    }
}
