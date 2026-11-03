package com.example.timerblock

import android.content.Context
import java.util.Calendar

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("blocker", Context.MODE_PRIVATE)

    // ── দৈনিক লিমিট ──
    fun limitMinutes(c: Context): Int = sp(c).getInt("limit", 120)
    fun setLimit(c: Context, minutes: Int) = sp(c).edit().putInt("limit", minutes).apply()

    fun enabled(c: Context): Boolean = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean("enabled", on).apply()

    // ── অনবোর্ডিং ──
    fun onboardingDone(c: Context): Boolean = sp(c).getBoolean("onboarded", false)
    fun setOnboardingDone(c: Context, v: Boolean) = sp(c).edit().putBoolean("onboarded", v).apply()

    // ── ডাউনটাইম (মিনিট, রাত ১২টা থেকে গোনা) ──
    fun downtimeEnabled(c: Context): Boolean = sp(c).getBoolean("down_on", false)
    fun setDowntimeEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean("down_on", on).apply()

    fun downStart(c: Context): Int = sp(c).getInt("down_start", 22 * 60)
    fun setDownStart(c: Context, minutes: Int) = sp(c).edit().putInt("down_start", minutes).apply()

    fun downEnd(c: Context): Int = sp(c).getInt("down_end", 6 * 60)
    fun setDownEnd(c: Context, minutes: Int) = sp(c).edit().putInt("down_end", minutes).apply()

    /** এখন ডাউনটাইম চলছে কি না (রাত পেরিয়ে যাওয়া সময়ও সামলায়, যেমন ২২:০০–০৬:০০) */
    fun inDowntime(c: Context): Boolean {
        if (!downtimeEnabled(c)) return false
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val s = downStart(c)
        val e = downEnd(c)
        return when {
            s == e -> false
            s < e -> now in s until e
            else -> now >= s || now < e
        }
    }

    // ── ফোকাস লক (নির্দিষ্ট সময় / সারাদিন) ──
    fun lockUntil(c: Context): Long = sp(c).getLong("lock_until", 0L)
    fun lockStart(c: Context): Long = sp(c).getLong("lock_start", 0L)
    fun lockActive(c: Context): Boolean = System.currentTimeMillis() < lockUntil(c)

    /** লক শুরুর সময়ের কঠোর-মোড স্ন্যাপশট (লক চলার মাঝে সেটিং বদলালেও এটা বদলায় না) */
    fun lockIsStrict(c: Context): Boolean = sp(c).getBoolean("lock_strict", false)
    fun strictLockActive(c: Context): Boolean = lockActive(c) && lockIsStrict(c)

    fun strictMode(c: Context): Boolean = sp(c).getBoolean("strict_mode", false)
    fun setStrictMode(c: Context, on: Boolean) = sp(c).edit().putBoolean("strict_mode", on).apply()

    fun startLock(c: Context, durationMs: Long) {
        val now = System.currentTimeMillis()
        sp(c).edit()
            .putLong("lock_start", now)
            .putLong("lock_until", now + durationMs)
            .putBoolean("lock_strict", strictMode(c))
            .apply()
    }

    /** আজ রাত ১২টা পর্যন্ত (সারাদিনের লক) */
    fun startLockUntilMidnight(c: Context) {
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        startLock(c, midnight - System.currentTimeMillis())
    }

    /** কঠোর লক চললে false ফেরত দেয় (বাতিল হয় না) */
    fun cancelLock(c: Context): Boolean {
        if (strictLockActive(c)) return false
        sp(c).edit().putLong("lock_until", 0L).apply()
        return true
    }

    // ── ব্লকের পরিধি: সব অ্যাপ নাকি শুধু বেছে নেওয়া অ্যাপ ──
    fun scopeSelected(c: Context): Boolean = sp(c).getBoolean("scope_selected", false)
    fun setScopeSelected(c: Context, v: Boolean) = sp(c).edit().putBoolean("scope_selected", v).apply()

    fun blockedApps(c: Context): Set<String> =
        sp(c).getStringSet("blocked", emptySet()) ?: emptySet()

    fun setBlocked(c: Context, pkg: String, blocked: Boolean) {
        val s = HashSet(blockedApps(c))
        if (blocked) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("blocked", s).apply()
    }

    // ── লক হবে না এমন অ্যাপ ──
    fun allowedApps(c: Context): Set<String> =
        sp(c).getStringSet("allowed", emptySet()) ?: emptySet()

    fun setAllowed(c: Context, pkg: String, allowed: Boolean) {
        val s = HashSet(allowedApps(c))
        if (allowed) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("allowed", s).apply()
    }
}
