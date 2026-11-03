package com.zihad.netblock

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile that turns blocking on and off from the notification shade. */
class BlockTileService : TileService() {

    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = s
        s.launch { BlockVpnService.isRunning.collect { render(it) } }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
    }

    override fun onClick() {
        if (BlockVpnService.isRunning.value) {
            BlockVpnService.stop(this)
            return
        }
        // Turning on goes through a short-lived activity, because a background app is not
        // allowed to start the VPN service directly (this is what failed after closing the app).
        if (isLocked) unlockAndRun { launchTurnOn() } else launchTurnOn()
    }

    private fun render(running: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Net Block"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (running) "On" else "Off"
        tile.updateTile()
    }

    private fun launchTurnOn() {
        val intent = Intent(this, TileActionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
