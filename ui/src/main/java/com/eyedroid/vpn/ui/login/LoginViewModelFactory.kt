package com.eyedroid.vpn.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eyedroid.vpn.data.repository.AuthRepository
import com.eyedroid.vpn.data.session.SessionManager

class LoginViewModelFactory(ctx: Context) : ViewModelProvider.Factory {
    private val session = SessionManager(ctx.applicationContext)
    private val auth = AuthRepository(session)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LoginViewModel(auth, session) as T
}
