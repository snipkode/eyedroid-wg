package com.eyedroid.vpn.util

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eyedroid.vpn.BuildConfig
import com.eyedroid.vpn.data.api.DebugInterceptor

object DebugLogOverlay {

    fun register(activity: Activity): BroadcastReceiver? {
        if (!BuildConfig.DEBUG) return null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (activity.isFinishing || activity.isDestroyed) return
                val log = intent.getStringExtra(DebugInterceptor.EXTRA_LOG) ?: return
                val tv = TextView(activity).apply {
                    text = log
                    textSize = 11f
                    fontFeatureSettings = "monospace"
                    setPadding(24, 16, 24, 16)
                }
                val scroll = ScrollView(activity).apply { addView(tv) }
                AlertDialog.Builder(activity)
                    .setTitle("🔍 Debug Log")
                    .setView(scroll)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(DebugInterceptor.ACTION_LOG),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        return receiver
    }

    fun unregister(activity: Activity, receiver: BroadcastReceiver?) {
        if (receiver == null) return
        activity.unregisterReceiver(receiver)
    }
}
