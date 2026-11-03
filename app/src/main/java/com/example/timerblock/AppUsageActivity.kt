package com.example.timerblock

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** সব অ্যাপের ব্যবহারের তালিকা: আজ / ৭ দিন */
class AppUsageActivity : Activity() {

    private lateinit var segToday: TextView
    private lateinit var segWeek: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvAvgRange: TextView
    private lateinit var list: LinearLayout
    private var days = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_usage)

        segToday = findViewById(R.id.segToday)
        segWeek = findViewById(R.id.segWeek)
        tvTotal = findViewById(R.id.tvTotal)
        tvAvgRange = findViewById(R.id.tvAvgRange)
        list = findViewById(R.id.list)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        segToday.setOnClickListener { select(1) }
        segWeek.setOnClickListener { select(7) }
        select(1)
    }

    private fun select(d: Int) {
        days = d
        segToday.setBackgroundResource(if (d == 1) R.drawable.bg_segment_selected else 0)
        segWeek.setBackgroundResource(if (d == 7) R.drawable.bg_segment_selected else 0)
        load()
    }

    private fun load() {
        list.removeAllViews()
        if (!UsageHelper.hasPermission(this)) {
            tvTotal.text = "—"
            tvAvgRange.visibility = View.GONE
            list.addView(Ui.message(this, "Usage Access অনুমতি দিলে এখানে অ্যাপ অনুযায়ী হিসাব দেখা যাবে।"))
            return
        }
        tvTotal.text = "…"
        val requested = days
        Thread {
            val start = UsageHelper.startOfDay(requested - 1)
            val end = System.currentTimeMillis()
            val apps = UsageHelper.appUsage(this, start, end)
            val total = UsageHelper.totalForRange(this, start, end)
            runOnUiThread {
                if (isFinishing || isDestroyed || requested != days) return@runOnUiThread
                bind(apps, total)
            }
        }.start()
    }

    private fun bind(apps: List<AppUsage>, total: Long) {
        tvTotal.text = UsageHelper.format(total)
        if (days > 1) {
            tvAvgRange.visibility = View.VISIBLE
            tvAvgRange.text = "দৈনিক গড়: ${UsageHelper.format(total / days)}"
        } else {
            tvAvgRange.visibility = View.GONE
        }

        list.removeAllViews()
        if (apps.isEmpty()) {
            list.addView(Ui.message(this, "এই সময়ে কোনো অ্যাপের ব্যবহার পাওয়া যায়নি।"))
            return
        }
        val max = apps.first().ms
        apps.forEachIndexed { i, app ->
            if (i > 0) Ui.addDivider(this, list, 76)
            val row = layoutInflater.inflate(R.layout.item_app_usage, list, false)
            Ui.bindAppRow(row, app, max, i)
            list.addView(row)
        }
    }
}
