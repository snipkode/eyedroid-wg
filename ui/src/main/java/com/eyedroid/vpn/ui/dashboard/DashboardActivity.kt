package com.eyedroid.vpn.ui.dashboard

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eyedroid.vpn.BuildConfig
import com.eyedroid.vpn.R
import com.eyedroid.vpn.databinding.ActivityDashboardBinding
import com.eyedroid.vpn.ui.login.LoginActivity
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var b: ActivityDashboardBinding
    private val vm: DashboardViewModel by viewModels { DashboardViewModelFactory(this) }

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { if (it.resultCode == RESULT_OK) vm.loadAndConnect() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvTenantName.text = BuildConfig.TENANT_NAME
        b.tvUsername.text = vm.session.username ?: "—"
        b.tvRole.text = (vm.session.role ?: "user").uppercase()

        b.btnConnect.setOnClickListener { requestVpnAndConnect() }
        b.btnDisconnect.setOnClickListener { vm.disconnect() }
        b.btnRefresh.setOnClickListener { requestVpnAndConnect() }
        b.btnLogout.setOnClickListener { logout() }

        lifecycleScope.launch {
            vm.vpnState.collect { state ->
                b.tvVpnStatus.text = when (state) {
                    is DashboardViewModel.VpnUiState.Connecting -> getString(R.string.vpn_status_connecting)
                    is DashboardViewModel.VpnUiState.Connected  -> getString(R.string.vpn_status_connected)
                    is DashboardViewModel.VpnUiState.Disconnected -> getString(R.string.vpn_status_disconnected)
                    is DashboardViewModel.VpnUiState.Error -> state.msg
                    else -> "—"
                }
                val connected = state is DashboardViewModel.VpnUiState.Connected
                val busy = state is DashboardViewModel.VpnUiState.Connecting
                b.btnConnect.isEnabled = !connected && !busy
                b.btnDisconnect.isEnabled = connected
                b.btnRefresh.isEnabled = !busy
            }
        }

        requestVpnAndConnect()
    }

    private fun requestVpnAndConnect() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermission.launch(intent) else vm.loadAndConnect()
    }

    private fun logout() {
        vm.session.clear()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
