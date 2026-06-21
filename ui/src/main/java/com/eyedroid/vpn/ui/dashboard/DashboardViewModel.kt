package com.eyedroid.vpn.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyedroid.vpn.data.repository.VpnRepository
import com.eyedroid.vpn.data.session.SessionManager
import com.wireguard.android.Application
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.model.ObservableTunnel
import com.wireguard.config.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val vpnRepo: VpnRepository,
    val session: SessionManager
) : ViewModel() {

    sealed class VpnUiState {
        object Idle : VpnUiState()
        object Connecting : VpnUiState()
        object Connected : VpnUiState()
        object Disconnected : VpnUiState()
        data class Error(val msg: String) : VpnUiState()
    }

    private val _vpnState = MutableStateFlow<VpnUiState>(VpnUiState.Idle)
    val vpnState: StateFlow<VpnUiState> = _vpnState

    private var activeTunnel: ObservableTunnel? = null

    fun loadAndConnect() = viewModelScope.launch {
        _vpnState.value = VpnUiState.Connecting
        vpnRepo.fetchConfig()
            .onSuccess { configText -> applyTunnel(configText) }
            .onFailure { _vpnState.value = VpnUiState.Error(it.message ?: "Config error") }
    }

    private suspend fun applyTunnel(configText: String) {
        try {
            val wgConfig = Config.parse(configText.reader().buffered())
            val manager = Application.getTunnelManager()
            val tunnels = manager.getTunnels()

            // Reuse or create the eyedroid tunnel
            val tunnel = tunnels["eyedroid"] ?: manager.create("eyedroid", wgConfig)
            activeTunnel = tunnel

            // Update config if tunnel already exists
            if (tunnels["eyedroid"] != null) {
                manager.setTunnelConfig(tunnel, wgConfig)
            }

            manager.setTunnelState(tunnel, Tunnel.State.UP)
            _vpnState.value = VpnUiState.Connected
        } catch (e: Exception) {
            _vpnState.value = VpnUiState.Error("Tunnel error: ${e.message}")
        }
    }

    fun disconnect() = viewModelScope.launch {
        try {
            val tunnel = activeTunnel
                ?: Application.getTunnelManager().getTunnels()["eyedroid"]
            tunnel?.let {
                Application.getTunnelManager().setTunnelState(it, Tunnel.State.DOWN)
            }
            _vpnState.value = VpnUiState.Disconnected
        } catch (e: Exception) {
            _vpnState.value = VpnUiState.Error(e.message ?: "Disconnect error")
        }
    }

    fun refreshConfig() = loadAndConnect()
}
