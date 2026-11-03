package com.zihad.netblock

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppItem(val label: String, val pkg: String)

/** Which networks the phone is connected to right now. */
data class NetState(val wifi: Boolean = false, val mobile: Boolean = false)

/** Whole-phone transfer speed in bytes per second (-1 when the device cannot report it). */
data class Speed(val downBps: Long = 0, val upBps: Long = 0)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val url: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class AppsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val _rules = MutableStateFlow(prefs.rules)
    val rules: StateFlow<Map<String, BlockMode>> = _rules.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _autoStart = MutableStateFlow(prefs.autoStart)
    val autoStart: StateFlow<Boolean> = _autoStart.asStateFlow()

    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    /** Read once at start; the guide is shown when this is false. */
    val onboarded: Boolean = prefs.onboarded

    // ---- Connected networks (for the Wi-Fi / Mobile data status cards) ----
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private val netLock = Any()
    private val wifiNets = mutableSetOf<Network>()
    private val cellNets = mutableSetOf<Network>()

    private val _network = MutableStateFlow(NetState())
    val network: StateFlow<NetState> = _network.asStateFlow()

    private fun watcher(set: MutableSet<Network>) = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(netLock) { set.add(network); publishNetwork() }
        }

        override fun onLost(network: Network) {
            synchronized(netLock) { set.remove(network); publishNetwork() }
        }
    }

    private val wifiWatcher = watcher(wifiNets)
    private val cellWatcher = watcher(cellNets)

    private fun publishNetwork() {
        _network.value = NetState(wifi = wifiNets.isNotEmpty(), mobile = cellNets.isNotEmpty())
    }

    private fun request(transport: Int) = NetworkRequest.Builder()
        .addTransportType(transport)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    // ---- Live speed, sampled once a second while the dashboard is on screen ----
    val speed: StateFlow<Speed> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastTime = SystemClock.elapsedRealtime()
        while (true) {
            delay(1000)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val time = SystemClock.elapsedRealtime()
            val dt = (time - lastTime).coerceAtLeast(1)
            if (rx < 0 || tx < 0 || lastRx < 0 || lastTx < 0) {
                emit(Speed(-1, -1))
            } else {
                emit(Speed(((rx - lastRx) * 1000 / dt).coerceAtLeast(0), ((tx - lastTx) * 1000 / dt).coerceAtLeast(0)))
            }
            lastRx = rx
            lastTx = tx
            lastTime = time
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Speed())

    init {
        try {
            connectivity.registerNetworkCallback(request(NetworkCapabilities.TRANSPORT_WIFI), wifiWatcher)
            connectivity.registerNetworkCallback(request(NetworkCapabilities.TRANSPORT_CELLULAR), cellWatcher)
        } catch (_: Exception) {
            // Status cards simply show "Not connected".
        }
        BlockStats.load(app)
        viewModelScope.launch(Dispatchers.IO) { loadApps() }
    }

    override fun onCleared() {
        try { connectivity.unregisterNetworkCallback(wifiWatcher) } catch (_: Exception) {}
        try { connectivity.unregisterNetworkCallback(cellWatcher) } catch (_: Exception) {}
        super.onCleared()
    }

    fun markOnboarded() {
        prefs.onboarded = true
    }

    private fun loadApps() {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val items = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != ctx.packageName }
            .mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    AppItem(label = pm.getApplicationLabel(info).toString(), pkg = pkg)
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
        _apps.value = items
        _loading.value = false
    }

    /** mode == null removes the rule (app is not blocked). */
    fun setRule(pkg: String, mode: BlockMode?) {
        val next = _rules.value.toMutableMap()
        if (mode == null) next.remove(pkg) else next[pkg] = mode
        _rules.value = next
        prefs.rules = next
    }

    fun setAutoStart(value: Boolean) {
        _autoStart.value = value
        prefs.autoStart = value
    }

    /** Manual update check against the latest GitHub release. */
    fun checkForUpdates() {
        if (_update.value == UpdateState.Checking) return
        val repo = BuildConfig.GITHUB_REPO
        if (repo.contains("USERNAME")) {
            _update.value = UpdateState.Failed("Set GITHUB_REPO in app/build.gradle.kts first.")
            return
        }
        _update.value = UpdateState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            _update.value = try {
                val conn = URL("https://api.github.com/repos/$repo/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "NetBlock")
                try {
                    when (conn.responseCode) {
                        200 -> {
                            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                            val tag = json.getString("tag_name")
                            val url = json.getString("html_url")
                            if (isNewer(tag, BuildConfig.VERSION_NAME)) UpdateState.Available(tag, url)
                            else UpdateState.UpToDate
                        }
                        404 -> UpdateState.Failed("No release published yet.")
                        else -> UpdateState.Failed("GitHub replied with ${conn.responseCode}.")
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                UpdateState.Failed("Could not reach GitHub. Check your connection.")
            }
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").split('.', '-')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val a = parts(remote)
        val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

}
