package com.zihad.netblock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

/** Turns blocking back on after the phone restarts, if it was on before. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        val ready = prefs.autoStart && prefs.active && prefs.rules.isNotEmpty()
        // prepare() returns null only when the VPN permission was already granted.
        if (ready && VpnService.prepare(context) == null) {
            BlockVpnService.start(context)
        }
    }
}
