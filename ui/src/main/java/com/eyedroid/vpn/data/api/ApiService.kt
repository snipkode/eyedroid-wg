package com.eyedroid.vpn.data.api

import com.eyedroid.vpn.data.model.LoginRequest
import com.eyedroid.vpn.data.model.LoginResponse
import com.eyedroid.vpn.data.model.UserInfo
import com.eyedroid.vpn.data.model.VpnConfigResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") token: String): Response<UserInfo>

    @GET("vpn/config")
    suspend fun getVpnConfig(@Header("Authorization") token: String): Response<VpnConfigResponse>
}
