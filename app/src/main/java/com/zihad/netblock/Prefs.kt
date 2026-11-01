package com.zihad.netblock

import android.content.Context

/** Which network(s) an app is blocked on. */
enum class BlockMode { ALL, WIFI, MOBILE }

/** Stores blocking rules and settings on the device. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("netblock", Context.MODE_PRIVATE)

    /** package name -> where it is blocked. Apps not in the map are never blocked. */
    var rules: Map<String, BlockMode>
        get() = (sp.getStringSet("rules", emptySet()) ?: emptySet())
            .mapNotNull { entry ->
                val i = entry.lastIndexOf(':')
                if (i <= 0) null else try {
                    entry.substring(0, i) to BlockMode.valueOf(entry.substring(i + 1))
                } catch (_: Exception) {
                    null
                }
            }
            .toMap()
        set(value) {
            sp.edit()
                .putStringSet("rules", value.map { "${it.key}:${it.value.name}" }.toHashSet())
                .apply()
        }

    /** True while the user wants blocking on (used to restore it after a reboot). */
    var active: Boolean
        get() = sp.getBoolean("active", false)
        set(value) { sp.edit().putBoolean("active", value).apply() }

    /** Turn blocking back on automatically after the phone restarts. */
    var autoStart: Boolean
        get() = sp.getBoolean("autostart", true)
        set(value) { sp.edit().putBoolean("autostart", value).apply() }

    /** Blocked-attempt counters per app (see BlockStats). */
    var stats: Map<String, AppStat>
        get() = (sp.getStringSet("stats", emptySet()) ?: emptySet())
            .mapNotNull { entry ->
                val p = entry.split('|')
                if (p.size != 3) null else try {
                    AppStat(p[0], p[1].toInt(), p[2].toLong())
                } catch (_: Exception) {
                    null
                }
            }
            .associateBy { it.pkg }
        set(value) {
            sp.edit()
                .putStringSet("stats", value.values.map { "${it.pkg}|${it.count}|${it.lastMs}" }.toHashSet())
                .apply()
        }

    /** When counting started (epoch millis), 0 if nothing recorded yet. */
    var statsSince: Long
        get() = sp.getLong("stats_since", 0L)
        set(value) { sp.edit().putLong("stats_since", value).apply() }

    /** True once the first-run guide has been shown. */
    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(value) { sp.edit().putBoolean("onboarded", value).apply() }
}
