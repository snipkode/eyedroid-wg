package com.eyedroid.vpn.data.model

data class LoginRequest(
    val tenantId: String,
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String?,
    val username: String,
    val role: String?
)

// Backend may return {"config":"..."} or plain text — handled in VpnRepository
data class VpnConfigResponse(
    val config: String?
)
