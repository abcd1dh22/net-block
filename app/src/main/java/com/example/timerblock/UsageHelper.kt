package com.example.timerblock

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class AppUsage(
    val pkg: String,
    val label: String,
    val icon: Drawable?,
    val ms: Long
)

object UsageHelper {

    // ───────── পারমিশন চেক ─────────
    fun hasPermission(c: Context): Boolean {
        val ops = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun isAccessibilityOn(c: Context): Boolean {
        val me = ComponentName(c, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun launcherPackage(c: Context): String? {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return c.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
    }

    // ───────── সময়ের হিসাব ─────────
    /** daysAgo = 0 মানে আজ রাত ১২টা, 1 মানে গতকাল রাত ১২টা */
    fun startOfDay(daysAgo: Int = 0): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // নিজের অ্যাপ, হোম স্ক্রিন ও সিস্টেম UI হিসাব থেকে বাদ
    private fun skipSet(c: Context): Set<String?> =
        setOf(c.packageName, launcherPackage(c), "com.android.systemui")

    fun totalForRange(c: Context, start: Long, end: Long): Long {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val skip = skipSet(c)
        return usm.queryAndAggregateUsageStats(start, end)
            .values
            .filter { it.packageName !in skip }
            .sumOf { it.totalTimeInForeground }
    }

    /** আজ রাত ১২টা থেকে এখন পর্যন্ত মোট ফোন ব্যবহার (মিলিসেকেন্ড) */
    fun todayUsageMs(c: Context): Long =
        totalForRange(c, startOfDay(0), System.currentTimeMillis())

    /** শেষ [days] দিনের দৈনিক মোট (পুরনো → আজ) */
    fun dailyTotals(c: Context, days: Int): List<Long> = (days - 1 downTo 0).map { i ->
        val start = startOfDay(i)
        val end = if (i == 0) System.currentTimeMillis() else startOfDay(i - 1)
        totalForRange(c, start, end)
    }

    private val DAY_NAMES = arrayOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")

    fun dayLabels(days: Int): List<String> = (days - 1 downTo 0).map { i ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
        DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    /** অ্যাপ অনুযায়ী ব্যবহার (বেশি → কম)। ১ মিনিটের কম ও লঞ্চ করা যায় না এমন অ্যাপ বাদ */
    fun appUsage(c: Context, start: Long, end: Long): List<AppUsage> {
        val usm = c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = c.packageManager
        val skip = skipSet(c)
        return usm.queryAndAggregateUsageStats(start, end)
            .values
            .filter { it.totalTimeInForeground >= 60_000 && it.packageName !in skip }
            .mapNotNull { st ->
                if (pm.getLaunchIntentForPackage(st.packageName) == null) {
                    null
                } else {
                    try {
                        val ai = pm.getApplicationInfo(st.packageName, 0)
                        AppUsage(
                            st.packageName,
                            ai.loadLabel(pm).toString(),
                            ai.loadIcon(pm),
                            st.totalTimeInForeground
                        )
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                }
            }
            .sortedByDescending { it.ms }
    }

    // ───────── বাংলা সংখ্যা ও সময় ফরম্যাট ─────────
    fun bnDigits(s: String): String = s.map {
        if (it in '0'..'9') '০' + (it - '0') else it
    }.joinToString("")

    private fun bn(n: Long): String = bnDigits(n.toString())

    fun format(ms: Long): String {
        val total = (ms / 60_000).coerceAtLeast(0)
        val h = total / 60
        val m = total % 60
        return when {
            h > 0 && m > 0 -> "${bn(h)} ঘণ্টা ${bn(m)} মিনিট"
            h > 0 -> "${bn(h)} ঘণ্টা"
            else -> "${bn(m)} মিনিট"
        }
    }

    fun formatMinutes(minutes: Int): String = format(minutes * 60_000L)

    fun short(ms: Long): String {
        val total = (ms / 60_000).coerceAtLeast(0)
        val h = total / 60
        val m = total % 60
        return if (h > 0) "${bn(h)}ঘ ${bn(m)}মি" else "${bn(m)} মি"
    }

    /** দিনের মিনিট → "রাত ১০:০০" */
    fun clock(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val period = when (h) {
            in 4..5 -> "ভোর"
            in 6..11 -> "সকাল"
            in 12..14 -> "দুপুর"
            in 15..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$period ${bnDigits(h12.toString())}:${bnDigits("%02d".format(m))}"
    }

    /** "১ ঘণ্টার জন্য", "১৫ মিনিটের জন্য" */
    fun forDuration(ms: Long): String {
        val f = format(ms)
        return if (f.endsWith("ঘণ্টা")) f + "র জন্য" else f + "ের জন্য"
    }

    /** বাকি সময় (উপরের দিকে মিনিট ধরে): "১ ঘণ্টা ২০ মিনিট" */
    fun remainingText(ms: Long): String {
        if (ms <= 0) return "০ মিনিট"
        val minutes = (ms + 59_999) / 60_000
        return format(minutes * 60_000)
    }

    /** লক কখন খুলবে: "রাত ১০:০০" / "আগামীকাল সকাল ৬:৩০" / "আজ রাত ১২:০০" */
    fun whenText(ts: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val today = Calendar.getInstance()
        val sameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        val min = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (!sameDay && min == 0) return "আজ রাত ১২:০০"
        return (if (sameDay) "" else "আগামীকাল ") + clock(min)
    }

    /** অ্যাপ সংখ্যা: "৩টি" বা "কোনোটি নয়" */
    fun countText(n: Int): String =
        if (n == 0) "কোনোটি নয়" else "${bnDigits(n.toString())}টি"

    private val WEEKDAYS = arrayOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )
    private val MONTHS = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    /** "মঙ্গলবার, ৩ নভেম্বর" */
    fun todayLabel(): String {
        val c = Calendar.getInstance()
        val day = bnDigits(c.get(Calendar.DAY_OF_MONTH).toString())
        return "${WEEKDAYS[c.get(Calendar.DAY_OF_WEEK) - 1]}, $day ${MONTHS[c.get(Calendar.MONTH)]}"
    }
}
