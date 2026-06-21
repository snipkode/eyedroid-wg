package com.eyedroid.vpn.data.api

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.eyedroid.vpn.AppConfig
import com.eyedroid.vpn.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var _api: ApiService? = null

    fun init(context: Context) {
        if (_api != null) return
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                DebugInterceptor(LocalBroadcastManager.getInstance(context))
            )
        }
        _api = Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val api: ApiService get() = _api ?: error("RetrofitClient not initialized. Call init(context) first.")
}
