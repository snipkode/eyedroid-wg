package com.eyedroid.vpn.data.repository

import com.eyedroid.vpn.AppConfig
import com.eyedroid.vpn.BuildConfig
import com.eyedroid.vpn.data.api.RetrofitClient
import com.eyedroid.vpn.data.model.LoginRequest
import com.eyedroid.vpn.data.session.SessionManager

class AuthRepository(private val session: SessionManager) {

    suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val resp = RetrofitClient.api.login(
            LoginRequest(AppConfig.TENANT_ID, username, password)
        )
        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string() ?: "(empty)"
            val debugInfo = if (BuildConfig.DEBUG) "\n[${resp.code()}] $errorBody" else ""
            error("Login failed (${resp.code()})$debugInfo")
        }
        val body = resp.body() ?: error("Empty response")
        session.token = body.token
        session.username = body.user.username
        session.role = body.user.role ?: "user"
    }

    /** Returns true if token is valid, false if expired/invalid (clears session on 401). */
    suspend fun validateSession(): Boolean = runCatching {
        val resp = RetrofitClient.api.me(session.bearerToken)
        if (resp.code() == 401) {
            session.clear()
            return false
        }
        resp.isSuccessful
    }.getOrElse { false }
}
