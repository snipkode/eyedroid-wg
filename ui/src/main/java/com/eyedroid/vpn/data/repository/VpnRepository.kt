package com.eyedroid.vpn.data.repository

import com.eyedroid.vpn.data.api.RetrofitClient
import com.eyedroid.vpn.data.session.SessionManager
import org.json.JSONObject

class VpnRepository(private val session: SessionManager) {

    /** Fetches WireGuard config text from the API. Handles both JSON and plain-text responses. */
    suspend fun fetchConfig(): Result<String> = runCatching {
        val resp = RetrofitClient.api.getVpnConfig(session.bearerToken)
        when {
            resp.code() == 401 -> { session.clear(); error("Session expired") }
            !resp.isSuccessful -> {
                val errorBody = resp.errorBody()?.string()?.trim() ?: ""
                val apiMsg = runCatching {
                    val j = org.json.JSONObject(errorBody)
                    j.optString("message").ifBlank { j.optString("error") }.ifBlank { null }
                }.getOrNull()
                error(apiMsg ?: "Config fetch failed (${resp.code()})")
            }
            else -> {
                val raw = resp.body()?.string()?.trim() ?: error("Empty VPN configuration")
                // Handle JSON wrapper: {"config":"[Interface]..."} or {"success":true,"config":"..."}
                if (raw.startsWith("{")) {
                    JSONObject(raw).getString("config").takeIf { it.isNotBlank() }
                        ?: error("Empty config in response")
                } else {
                    raw.takeIf { it.isNotBlank() } ?: error("Empty VPN configuration")
                }
            }
        }
    }
}
