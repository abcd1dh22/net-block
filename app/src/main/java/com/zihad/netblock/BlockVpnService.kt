package com.zihad.netblock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.net.InetAddress
import java.net.InetSocketAddress

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
        BlockStats.load(applicationContext)
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

        if (fresh != null) startDrain(fresh, targets.toSet())
    }

    /**
     * Reads and discards the packets of the blocked apps (so they get no connection) and
     * records each new connection attempt in BlockStats.
     */
    private fun startDrain(fd: ParcelFileDescriptor, owners: Set<String>) {
        Thread {
            val recent = HashMap<String, Long>()
            var lastPersist = System.currentTimeMillis()
            try {
                val input = FileInputStream(fd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (n == 0) continue
                    try { inspect(buffer, n, owners, recent) } catch (_: Exception) {}
                    val now = System.currentTimeMillis()
                    if (now - lastPersist > 5000) {
                        lastPersist = now
                        BlockStats.persist(applicationContext)
                    }
                }
            } catch (_: Exception) {
                // Tunnel closed.
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private fun port(b: ByteArray, off: Int) =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    /** Counts a packet as an "attempt" if it starts a new TCP connection or is a UDP packet. */
    private fun inspect(b: ByteArray, n: Int, owners: Set<String>, recent: HashMap<String, Long>) {
        val version = (b[0].toInt() and 0xFF) ushr 4
        val addrLen: Int
        val ihl: Int
        val proto: Int
        when (version) {
            4 -> {
                if (n < 28) return
                ihl = (b[0].toInt() and 0x0F) * 4
                proto = b[9].toInt() and 0xFF
                addrLen = 4
            }
            6 -> {
                if (n < 48) return
                ihl = 40
                proto = b[6].toInt() and 0xFF
                addrLen = 16
            }
            else -> return
        }
        if (proto != 6 && proto != 17) return
        if (n < ihl + 14) return

        if (proto == 6) {
            val flags = b[ihl + 13].toInt() and 0xFF
            // Only the first packet of a connection (SYN without ACK).
            if ((flags and 0x02) == 0 || (flags and 0x10) != 0) return
        }

        val srcOff = if (addrLen == 4) 12 else 8
        val dstOff = srcOff + addrLen
        val srcPort = port(b, ihl)
        val dstPort = port(b, ihl + 2)
        val dst = InetAddress.getByAddress(b.copyOfRange(dstOff, dstOff + addrLen))

        // Ignore repeats of the same attempt within 3 seconds.
        val key = "$proto|${dst.hostAddress}|$dstPort|$srcPort"
        val now = System.currentTimeMillis()
        val last = recent[key]
        if (last != null && now - last < 3000) return
        recent[key] = now
        if (recent.size > 400) recent.entries.removeAll { now - it.value > 3000 }

        val pkg = ownerOf(proto, b.copyOfRange(srcOff, srcOff + addrLen), srcPort, dst, dstPort, owners)
        val shownDest = if (addrLen == 16) "[${dst.hostAddress}]:$dstPort" else "${dst.hostAddress}:$dstPort"
        BlockStats.record(LogEntry(now, pkg, if (proto == 6) "TCP" else "UDP", shownDest))
    }

    /** Finds which blocked app made the connection. */
    private fun ownerOf(
        proto: Int,
        srcBytes: ByteArray,
        srcPort: Int,
        dst: InetAddress,
        dstPort: Int,
        owners: Set<String>
    ): String {
        // Only blocked apps are inside the tunnel, so a single one is certain.
        if (owners.size == 1) return owners.first()
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val uid = connectivity.getConnectionOwnerUid(
                    proto,
                    InetSocketAddress(InetAddress.getByAddress(srcBytes), srcPort),
                    InetSocketAddress(dst, dstPort)
                )
                if (uid >= 0) {
                    val pkgs = packageManager.getPackagesForUid(uid)
                    return pkgs?.firstOrNull { it in owners } ?: pkgs?.firstOrNull() ?: BlockStats.UNKNOWN
                }
            } catch (_: Exception) {
                // Fall through to unknown.
            }
        }
        return BlockStats.UNKNOWN
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
        BlockStats.persist(applicationContext)
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
        BlockStats.persist(applicationContext)
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
