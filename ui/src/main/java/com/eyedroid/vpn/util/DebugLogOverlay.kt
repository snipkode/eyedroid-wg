package com.eyedroid.vpn.util

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.ScrollView
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
        LocalBroadcastManager.getInstance(activity)
            .registerReceiver(receiver, IntentFilter(DebugInterceptor.ACTION_LOG))
        return receiver
    }

    fun unregister(activity: Activity, receiver: BroadcastReceiver?) {
        if (receiver == null) return
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiver)
    }
}
