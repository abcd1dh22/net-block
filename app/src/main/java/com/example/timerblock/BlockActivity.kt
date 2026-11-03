package com.example.timerblock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class BlockActivity : Activity() {

    companion object {
        const val EXTRA_REASON = "reason"
        const val REASON_LIMIT = "limit"
        const val REASON_DOWNTIME = "downtime"
        const val REASON_LOCK = "lock"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (bind()) handler.postDelayed(this, 15_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
        findViewById<RingView>(R.id.ring).setTrackColor(0x33FFFFFF)
        findViewById<Button>(R.id.btnHome).setOnClickListener { goHome() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    /** স্ক্রিন আপডেট করে। ব্লকের কারণ আর না থাকলে finish() করে false ফেরত দেয় */
    private fun bind(): Boolean {
        val reason = intent?.getStringExtra(EXTRA_REASON) ?: REASON_LIMIT

        val ring = findViewById<RingView>(R.id.ring)
        val imgIcon = findViewById<ImageView>(R.id.imgIcon)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvBig = findViewById<TextView>(R.id.tvUsedBig)
        val tvSub = findViewById<TextView>(R.id.tvSub)
        val tvInfo = findViewById<TextView>(R.id.tvLimitInfo)

        when (reason) {
            REASON_LOCK -> {
                if (!Prefs.lockActive(this)) { finish(); return false }
                val now = System.currentTimeMillis()
                val until = Prefs.lockUntil(this)
                val total = until - Prefs.lockStart(this)
                val remaining = until - now
                val fraction = if (total > 0) remaining.toFloat() / total else 0f

                ring.setProgress(fraction, getColor(R.color.pastel_lilac), true)
                imgIcon.setImageResource(R.drawable.ic_lock)
                tvTitle.setText(R.string.block_title_lock)
                tvBig.text = UsageHelper.remainingText(remaining)
                tvSub.text = "পর অ্যাপ আবার খোলা যাবে"
                tvInfo.text = "লক খুলবে: ${UsageHelper.whenText(until)}" +
                    if (Prefs.lockIsStrict(this)) "\nকঠোর মোড চালু আছে" else ""
            }
            REASON_DOWNTIME -> {
                if (!Prefs.inDowntime(this)) { finish(); return false }
                val start = Prefs.downStart(this)
                val end = Prefs.downEnd(this)
                ring.setProgress(1f, getColor(R.color.ui_orange), true)
                imgIcon.setImageResource(R.drawable.ic_moon)
                tvTitle.setText(R.string.block_title_downtime)
                tvBig.text = "${UsageHelper.clock(end)} পর্যন্ত"
                tvSub.text = "এই সময়ে অন্য অ্যাপ বন্ধ থাকবে"
                tvInfo.text = "ডাউনটাইম: ${UsageHelper.clock(start)} – ${UsageHelper.clock(end)}"
            }
            else -> {
                val used = UsageHelper.todayUsageMs(this)
                val limitMs = Prefs.limitMinutes(this) * 60_000L
                if (!Prefs.enabled(this) || limitMs <= 0 || used < limitMs) { finish(); return false }
                ring.setProgress(1f, getColor(R.color.ui_red), true)
                imgIcon.setImageResource(R.drawable.ic_hourglass)
                tvTitle.setText(R.string.block_title)
                tvBig.text = UsageHelper.format(used)
                tvSub.setText(R.string.block_sub)
                tvInfo.text =
                    "আপনার দৈনিক লিমিট ${UsageHelper.format(limitMs)}।\nকাল আবার ব্যবহার করতে পারবেন।"
            }
        }
        return true
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()
}
