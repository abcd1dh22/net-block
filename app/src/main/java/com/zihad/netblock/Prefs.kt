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
}
