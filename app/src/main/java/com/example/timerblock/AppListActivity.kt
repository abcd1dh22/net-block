package com.example.timerblock

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * একই স্ক্রিন দুই কাজে:
 *  MODE_ALLOWED → "লক হবে না এমন অ্যাপ"
 *  MODE_BLOCKED → "ব্লক হবে এমন অ্যাপ" (শুধু বেছে নেওয়া অ্যাপ মোডে)
 */
class AppListActivity : Activity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_ALLOWED = "allowed"
        const val MODE_BLOCKED = "blocked"
    }

    private data class Item(val pkg: String, val label: String, val icon: Drawable)

    private lateinit var list: LinearLayout
    private var allowedMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        allowedMode = intent.getStringExtra(EXTRA_MODE) != MODE_BLOCKED
        list = findViewById(R.id.list)

        findViewById<TextView>(R.id.tvTitle)
            .setText(if (allowedMode) R.string.allowed_title else R.string.blocked_title)
        findViewById<TextView>(R.id.tvDesc)
            .setText(if (allowedMode) R.string.allowed_desc else R.string.blocked_desc)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val locked = Prefs.strictLockActive(this)
        findViewById<View>(R.id.tvNotice).visibility = if (locked) View.VISIBLE else View.GONE

        list.addView(Ui.message(this, "লোড হচ্ছে…"))

        Thread {
            val pm = packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val items = pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != packageName }
                .map { Item(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                build(items, locked)
            }
        }.start()
    }

    private fun build(items: List<Item>, locked: Boolean) {
        list.removeAllViews()
        if (items.isEmpty()) {
            list.addView(Ui.message(this, "কোনো অ্যাপ পাওয়া যায়নি।"))
            return
        }
        val selected = if (allowedMode) Prefs.allowedApps(this) else Prefs.blockedApps(this)
        items.forEachIndexed { i, item ->
            if (i > 0) Ui.addDivider(this, list, 72)
            val row = layoutInflater.inflate(R.layout.item_app_toggle, list, false)
            row.findViewById<ImageView>(R.id.appIcon).setImageDrawable(item.icon)
            row.findViewById<TextView>(R.id.appName).text = item.label

            val sw = row.findViewById<SoftSwitch>(R.id.sw)
            sw.setCheckedState(item.pkg in selected, animate = false, notify = false)
            sw.onChange = { on ->
                if (allowedMode) Prefs.setAllowed(this, item.pkg, on)
                else Prefs.setBlocked(this, item.pkg, on)
            }
            row.setOnClickListener { sw.toggle() }

            if (locked) { // কঠোর লক চলাকালীন তালিকা বদলানো বন্ধ
                sw.isEnabled = false
                row.isEnabled = false
                row.alpha = 0.5f
            }
            list.addView(row)
        }
    }
}
