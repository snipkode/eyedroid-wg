package com.eyedroid.vpn.ui.login

import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eyedroid.vpn.BuildConfig
import com.eyedroid.vpn.R
import com.eyedroid.vpn.data.api.RetrofitClient
import com.eyedroid.vpn.databinding.ActivityLoginBinding
import com.eyedroid.vpn.ui.dashboard.DashboardActivity
import com.eyedroid.vpn.util.DebugLogOverlay
import com.eyedroid.vpn.util.SecurityCheck
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels { LoginViewModelFactory(this) }
    private var debugReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.EyeDroidTheme)
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        RetrofitClient.init(applicationContext)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvTenantBadge.text = BuildConfig.TENANT_NAME
        debugReceiver = DebugLogOverlay.register(this)

        SecurityCheck.run(this)
        vm.checkSession(onValid = ::goToDashboard, onInvalid = {})

        b.btnLogin.setOnClickListener {
            val user = b.etUsername.text?.toString()?.trim() ?: ""
            val pass = b.etPassword.text?.toString() ?: ""
            b.tilUsername.error = if (user.isBlank()) "Required" else null
            b.tilPassword.error = if (pass.isBlank()) "Required" else null
            if (user.isNotBlank() && pass.isNotBlank()) vm.login(user, pass)
        }

        lifecycleScope.launch {
            vm.state.collect { state ->
                b.btnLogin.isEnabled = state !is LoginViewModel.State.Loading
                b.progressBar.visibility =
                    if (state is LoginViewModel.State.Loading) View.VISIBLE else View.GONE
                when (state) {
                    is LoginViewModel.State.Success -> goToDashboard()
                    is LoginViewModel.State.Error -> {
                        b.tilPassword.error = " "
                        androidx.appcompat.app.AlertDialog.Builder(this@LoginActivity)
                            .setTitle("Login Gagal")
                            .setMessage(state.msg)
                            .setPositiveButton("OK") { _, _ -> b.tilPassword.error = null }
                            .show()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugLogOverlay.unregister(this, debugReceiver)
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}
