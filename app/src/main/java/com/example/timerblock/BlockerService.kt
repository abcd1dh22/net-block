package com.example.timerblock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

class BlockerService : AccessibilityService() {

    // এই অ্যাপগুলো কখনো ব্লক হবে না (সিস্টেম, ফোন, কীবোর্ড, হোম, নিজের অ্যাপ)
    private val whitelist = mutableSetOf(
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.android.dialer"
    )
    private var lastBlockAt = 0L

    override fun onServiceConnected() {
        whitelist.add(packageName)
        UsageHelper.launcherPackage(this)?.let { whitelist.add(it) }
        (getSystemService(Context.TELECOM_SERVICE) as TelecomManager)
            .defaultDialerPackage?.let { whitelist.add(it) }
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .enabledInputMethodList.forEach { whitelist.add(it.packageName) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // ১) সবসময় অনুমোদিত অ্যাপ কখনো ব্লক হয় না
        if (pkg in whitelist || pkg in Prefs.allowedApps(this)) return
        // ২) "শুধু বেছে নেওয়া অ্যাপ" মোডে তালিকার বাইরের অ্যাপ ছেড়ে দেওয়া হয়
        if (Prefs.scopeSelected(this) && pkg !in Prefs.blockedApps(this)) return

        val now = System.currentTimeMillis()
        if (now - lastBlockAt < 1000) return

        // অগ্রাধিকার: ফোকাস লক → ডাউনটাইম → দৈনিক লিমিট (সস্তা চেক আগে)
        val reason: String? = when {
            Prefs.lockActive(this) -> BlockActivity.REASON_LOCK
            Prefs.inDowntime(this) -> BlockActivity.REASON_DOWNTIME
            Prefs.enabled(this) && limitReached() -> BlockActivity.REASON_LIMIT
            else -> null
        }
        if (reason == null) return

        lastBlockAt = now
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockActivity.EXTRA_REASON, reason)
        )
    }

    private fun limitReached(): Boolean {
        val limitMs = Prefs.limitMinutes(this) * 60_000L
        return limitMs > 0 && UsageHelper.todayUsageMs(this) >= limitMs
    }

    override fun onInterrupt() {}
}
