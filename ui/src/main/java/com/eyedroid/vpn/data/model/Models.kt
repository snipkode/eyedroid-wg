package com.eyedroid.vpn.data.model

data class LoginRequest(
    val tenantId: String,
    val username: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: Long?,
    val username: String,
    val tenantId: String?,
    val tenantName: String?,
    val role: String?,
    val displayName: String?,
    val features: List<String>?,
    val maxDevices: Int?
)

// Wrapper for /api/auth/me response
data class MeResponse(
    val success: Boolean,
    val user: UserInfo
)
