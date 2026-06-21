package com.eyedroid.vpn.data.repository

import com.eyedroid.vpn.data.api.RetrofitClient
import com.eyedroid.vpn.data.session.SessionManager

class VpnRepository(private val session: SessionManager) {

    /** Fetches WireGuard config text from the API. Handles both JSON and plain-text responses. */
    suspend fun fetchConfig(): Result<String> = runCatching {
        val resp = RetrofitClient.api.getVpnConfig(session.bearerToken)
        when {
            resp.code() == 401 -> { session.clear(); error("Session expired") }
            !resp.isSuccessful -> error("Config fetch failed (${resp.code()})")
            else -> {
                val body = resp.body()
                // JSON field "config" OR fall back to raw body string
                body?.config?.takeIf { it.isNotBlank() }
                    ?: error("Empty VPN configuration")
            }
        }
    }
}
