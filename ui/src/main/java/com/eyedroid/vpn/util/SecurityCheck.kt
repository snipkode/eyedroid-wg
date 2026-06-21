package com.eyedroid.vpn.util

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.os.Build
import androidx.appcompat.app.AlertDialog
import java.io.File

object SecurityCheck {

    fun run(activity: Activity) {
        val issues = buildList {
            if (isRooted()) add("Rooted device detected")
            if (isEmulator()) add("Emulator detected")
            if (isDebuggable(activity)) add("Debug mode active")
        }
        if (issues.isNotEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle("Security Warning")
                .setMessage(issues.joinToString("\n") + "\n\nThis app may not function securely.")
                .setPositiveButton("Continue") { d, _ -> d.dismiss() }
                .setNegativeButton("Exit") { _, _ -> activity.finish() }
                .setCancelable(false)
                .show()
        }
    }

    private fun isRooted() = arrayOf(
        "/su", "/system/bin/su", "/system/xbin/su",
        "/sbin/su", "/system/app/Superuser.apk", "/system/app/SuperSU.apk"
    ).any { File(it).exists() }

    private fun isEmulator() =
        Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator") ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.MODEL.contains("Android SDK", ignoreCase = true) ||
        Build.MANUFACTURER.equals("Genymotion", ignoreCase = true) ||
        Build.HARDWARE.equals("goldfish") ||
        Build.HARDWARE.equals("ranchu") ||
        Build.PRODUCT.startsWith("sdk")

    private fun isDebuggable(activity: Activity) =
        (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
