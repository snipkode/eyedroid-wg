package com.eyedroid.vpn.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eyedroid.vpn.data.api.RetrofitClient
import com.eyedroid.vpn.data.repository.VpnRepository
import com.eyedroid.vpn.data.session.SessionManager
import com.wireguard.android.Application
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config

class VpnRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        RetrofitClient.init(context)
        val session = SessionManager(context)
        if (!session.isLoggedIn) return Result.success()

        return VpnRepository(session).fetchConfig().fold(
            onSuccess = { configText ->
                runCatching {
                    val wgConfig = Config.parse(configText.reader().buffered())
                    val manager = Application.getTunnelManager()
                    val tunnel = manager.getTunnels()["eyedroid"] ?: return Result.success()
                    manager.setTunnelConfig(tunnel, wgConfig)
                    manager.setTunnelState(tunnel, Tunnel.State.UP)
                }.fold({ Result.success() }, { Result.retry() })
            },
            onFailure = { Result.retry() }
        )
    }
}
