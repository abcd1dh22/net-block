package com.zihad.netblock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream

/**
 * Local VPN that captures traffic ONLY from the blocked apps and drops it.
 * Every other app keeps using the normal connection.
 *
 * Each app has a rule (all networks, Wi-Fi only, mobile data only). The tunnel is rebuilt
 * whenever the active network changes so only the apps whose rule matches are captured.
 */
class BlockVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    private var started = false
    private var callbacksRegistered = false

    private val wifiNetworks = mutableSetOf<Network>()
    private val cellNetworks = mutableSetOf<Network>()

    private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { track(wifiNetworks, network, true) }
        override fun onLost(network: Network) { track(wifiNetworks, network, false) }
    }

    private val cellCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { track(cellNetworks, network, true) }
        override fun onLost(network: Network) { track(cellNetworks, network, false) }
    }

    @Synchronized
    private fun track(set: MutableSet<Network>, network: Network, present: Boolean) {
        val changed = if (present) set.add(network) else set.remove(network)
        if (changed) applyRules()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = Prefs(this)
        if (intent?.action == ACTION_STOP) {
            prefs.active = false
            shutdown()
            return START_NOT_STICKY
        }
        if (prefs.rules.isEmpty()) {
            prefs.active = false
            shutdown()
            return START_NOT_STICKY
        }
        prefs.active = true
        startWatching()
        applyRules()
        return START_STICKY
    }

    @Synchronized
    private fun startWatching() {
        started = true
        state.value = true
        if (callbacksRegistered) return
        callbacksRegistered = true
        connectivity.registerNetworkCallback(requestFor(NetworkCapabilities.TRANSPORT_WIFI), wifiCallback)
        connectivity.registerNetworkCallback(requestFor(NetworkCapabilities.TRANSPORT_CELLULAR), cellCallback)
    }

    private fun requestFor(transport: Int) = NetworkRequest.Builder()
        .addTransportType(transport)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    /** Rebuilds the tunnel so it captures only the apps blocked on the current network. */
    @Synchronized
    private fun applyRules() {
        if (!started) return
        val rules = Prefs(this).rules
        if (rules.isEmpty()) {
            Prefs(this).active = false
            shutdown()
            return
        }

        val onWifi = wifiNetworks.isNotEmpty()
        val onMobile = !onWifi && cellNetworks.isNotEmpty()
        val targets = rules.filter { (_, mode) ->
            mode == BlockMode.ALL ||
                (mode == BlockMode.WIFI && onWifi) ||
                (mode == BlockMode.MOBILE && onMobile)
        }.keys

        val builder = Builder()
            .setSession("Net Block")
            .addAddress("10.8.0.2", 32)
            .addAddress("fd00::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)

        var added = 0
        for (pkg in targets) {
            try {
                builder.addAllowedApplication(pkg)
                added++
            } catch (_: PackageManager.NameNotFoundException) {
                // App was uninstalled; skip it.
            }
        }

        // With no allowed apps a VPN would capture EVERY app, so only open it when needed.
        if (added == 0) {
            closeTunnel()
            return
        }

        // Open the new tunnel before closing the old one so there is no gap.
        val old = tun
        val fresh = builder.establish()
        tun = fresh
        try { old?.close() } catch (_: Exception) {}

        if (fresh != null) {
            // Read and discard packets so the blocked apps simply get no connection.
            Thread {
                try {
                    val input = FileInputStream(fresh.fileDescriptor)
                    val buffer = ByteArray(32767)
                    while (input.read(buffer) >= 0) { /* drop */ }
                } catch (_: Exception) {
                    // Tunnel closed.
                }
            }.also { it.isDaemon = true; it.start() }
        }
    }

    @Synchronized
    private fun closeTunnel() {
        try { tun?.close() } catch (_: Exception) {}
        tun = null
    }

    @Synchronized
    private fun stopWatching() {
        if (!callbacksRegistered) return
        callbacksRegistered = false
        try { connectivity.unregisterNetworkCallback(wifiCallback) } catch (_: Exception) {}
        try { connectivity.unregisterNetworkCallback(cellCallback) } catch (_: Exception) {}
        wifiNetworks.clear()
        cellNetworks.clear()
    }

    @Synchronized
    private fun shutdown() {
        started = false
        stopWatching()
        closeTunnel()
        state.value = false
        stopSelf()
    }

    override fun onRevoke() {
        // Another VPN took over.
        Prefs(this).active = false
        shutdown()
    }

    override fun onDestroy() {
        started = false
        stopWatching()
        closeTunnel()
        state.value = false
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.zihad.netblock.STOP"
        private val state = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> get() = state

        fun start(context: Context) {
            context.startService(Intent(context, BlockVpnService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BlockVpnService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
