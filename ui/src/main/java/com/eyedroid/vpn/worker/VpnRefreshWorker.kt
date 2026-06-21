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

    override suspend fun doWork(): Result = runCatching {
        RetrofitClient.init(applicationContext)
        val session = SessionManager(applicationContext)
        if (!session.isLoggedIn) return Result.success()

        val configText = VpnRepository(session).fetchConfig().getOrElse { return Result.retry() }
        val wgConfig = Config.parse(configText.reader().buffered())

        // TunnelManager may be unavailable if process was restarted by OS
        val manager = runCatching { Application.getTunnelManager() }.getOrNull()
            ?: return Result.success()

        val tunnel = manager.getTunnels()["eyedroid"] ?: return Result.success()
        manager.setTunnelConfig(tunnel, wgConfig)
        manager.setTunnelState(tunnel, Tunnel.State.UP)
        Result.success()
    }.getOrElse { Result.retry() }
}
