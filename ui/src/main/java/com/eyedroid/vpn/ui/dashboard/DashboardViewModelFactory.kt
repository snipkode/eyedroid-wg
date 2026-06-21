package com.eyedroid.vpn.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.eyedroid.vpn.data.repository.VpnRepository
import com.eyedroid.vpn.data.session.SessionManager

class DashboardViewModelFactory(ctx: Context) : ViewModelProvider.Factory {
    private val session = SessionManager(ctx.applicationContext)
    private val repo = VpnRepository(session)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        DashboardViewModel(repo, session) as T
}
