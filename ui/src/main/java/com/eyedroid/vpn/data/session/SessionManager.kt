package com.eyedroid.vpn.data.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "eyedroid_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String?
        get() = prefs.getString("token", null)
        set(v) = prefs.edit().putString("token", v).apply()

    var username: String?
        get() = prefs.getString("username", null)
        set(v) = prefs.edit().putString("username", v).apply()

    var role: String?
        get() = prefs.getString("role", null)
        set(v) = prefs.edit().putString("role", v).apply()

    fun clear() = prefs.edit().clear().apply()

    val bearerToken get() = "Bearer $token"

    val isLoggedIn get() = !token.isNullOrEmpty()
}
