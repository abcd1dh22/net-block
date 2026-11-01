package com.zihad.netblock

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppStat(val pkg: String, val count: Int, val lastMs: Long)

data class LogEntry(val timeMs: Long, val pkg: String, val proto: String, val dest: String)

/**
 * Counts connection attempts from blocked apps. Only the app, protocol and destination
 * address of an attempt are kept, never any content. Everything stays on the device.
 */
object BlockStats {
    /** Used when an attempt cannot be matched to one app. */
    const val UNKNOWN = "?"
    private const val MAX_LOG = 100

    private val _stats = MutableStateFlow<Map<String, AppStat>>(emptyMap())
    val stats: StateFlow<Map<String, AppStat>> = _stats.asStateFlow()

    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val _since = MutableStateFlow(0L)
    val since: StateFlow<Long> = _since.asStateFlow()

    private var loaded = false

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = Prefs(context)
        _stats.value = prefs.stats
        _since.value = prefs.statsSince
    }

    @Synchronized
    fun record(entry: LogEntry) {
        if (_since.value == 0L) _since.value = entry.timeMs
        val current = _stats.value
        val old = current[entry.pkg]
        _stats.value = current + (entry.pkg to AppStat(entry.pkg, (old?.count ?: 0) + 1, entry.timeMs))
        _log.value = (listOf(entry) + _log.value).take(MAX_LOG)
    }

    @Synchronized
    fun persist(context: Context) {
        val prefs = Prefs(context)
        prefs.stats = _stats.value
        prefs.statsSince = _since.value
    }

    @Synchronized
    fun clear(context: Context) {
        _stats.value = emptyMap()
        _log.value = emptyList()
        _since.value = 0L
        persist(context)
    }
}
