package com.example.timerblock

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

/**
 * চারটি ট্যাব: ০ হোম · ১ পরিসংখ্যান · ২ নিয়ম · ৩ সেটআপ
 * নিচের ভাসমান বারের মাঝের গোল বাটন = ফোকাস লক
 */
class MainActivity : Activity() {

    // ── ট্যাব ──
    private lateinit var pages: List<View>
    private lateinit var navIcons: List<ImageView>
    private lateinit var navPills: List<View>
    private var tab = 0

    // ── হোম ──
    private lateinit var tvDate: TextView
    private lateinit var banner: View
    private lateinit var ring: RingView
    private lateinit var tvPct: TextView
    private lateinit var tvUsed: TextView
    private lateinit var tvStatusChip: TextView
    private lateinit var tvHeroLimit: TextView
    private lateinit var tvHeroLeft: TextView
    private lateinit var tvLockTitle: TextView
    private lateinit var tvLockBody: TextView
    private lateinit var btnLock: Button
    private lateinit var btnLockCancel: TextView
    private lateinit var listTopApps: LinearLayout
    private lateinit var tvQuickAvg: TextView
    private lateinit var tvQuickDown: TextView

    // ── পরিসংখ্যান ──
    private lateinit var barChart: BarChartView
    private lateinit var tvAvg: TextView
    private lateinit var tvChartHint: TextView
    private lateinit var donut: DonutView
    private lateinit var tvDonutTotal: TextView
    private lateinit var legendBox: LinearLayout
    private lateinit var listApps: LinearLayout

    // ── নিয়ম ──
    private lateinit var sw: SoftSwitch
    private lateinit var seek: SeekBar
    private lateinit var limitBox: View
    private lateinit var tvLimitValue: TextView
    private lateinit var swDown: SoftSwitch
    private lateinit var downBox: View
    private lateinit var rowDownStart: View
    private lateinit var rowDownEnd: View
    private lateinit var tvDownStart: TextView
    private lateinit var tvDownEnd: TextView
    private lateinit var rowStrict: View
    private lateinit var swStrict: SoftSwitch
    private lateinit var tvScopeValue: TextView
    private lateinit var blockedBox: View
    private lateinit var tvBlockedCount: TextView
    private lateinit var tvAllowedCount: TextView

    // ── সেটআপ ──
    private lateinit var tvUsageStatus: TextView
    private lateinit var tvAccStatus: TextView

    private var ready = false
    private var lastUsed = 0L
    private var topApps: List<AppUsage> = emptyList()
    private var week: List<Long> = emptyList()
    @Volatile private var loading = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 30_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // প্রথমবার খুললে গাইড ও পারমিশন স্ক্রিন
        if (!Prefs.onboardingDone(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        bindViews()
        ring.setTrackColor(getColor(R.color.ui_card))

        setupTabs()
        setupLock()
        setupLimit()
        setupDowntime()
        setupScope()
        setupNavigation()

        showTab(savedInstanceState?.getInt("tab") ?: 0, animate = false)
        ready = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", tab)
    }

    private fun bindViews() {
        pages = listOf<View>(
            findViewById(R.id.pageHome), findViewById(R.id.pageInsights),
            findViewById(R.id.pageRules), findViewById(R.id.pageSetup)
        )
        navIcons = listOf<ImageView>(
            findViewById(R.id.icHome), findViewById(R.id.icInsights),
            findViewById(R.id.icRules), findViewById(R.id.icSetup)
        )
        navPills = listOf<View>(
            findViewById(R.id.pillHome), findViewById(R.id.pillInsights),
            findViewById(R.id.pillRules), findViewById(R.id.pillSetup)
        )

        tvDate = findViewById(R.id.tvDate)
        banner = findViewById(R.id.bannerSetup)
        ring = findViewById(R.id.ring)
        tvPct = findViewById(R.id.tvPct)
        tvUsed = findViewById(R.id.tvUsed)
        tvStatusChip = findViewById(R.id.tvStatusChip)
        tvHeroLimit = findViewById(R.id.tvHeroLimit)
        tvHeroLeft = findViewById(R.id.tvHeroLeft)
        tvLockTitle = findViewById(R.id.tvLockTitle)
        tvLockBody = findViewById(R.id.tvLockBody)
        btnLock = findViewById(R.id.btnLock)
        btnLockCancel = findViewById(R.id.btnLockCancel)
        listTopApps = findViewById(R.id.listTopApps)
        tvQuickAvg = findViewById(R.id.tvQuickAvg)
        tvQuickDown = findViewById(R.id.tvQuickDown)

        barChart = findViewById(R.id.barChart)
        tvAvg = findViewById(R.id.tvAvg)
        tvChartHint = findViewById(R.id.tvChartHint)
        donut = findViewById(R.id.donut)
        tvDonutTotal = findViewById(R.id.tvDonutTotal)
        legendBox = findViewById(R.id.legendBox)
        listApps = findViewById(R.id.listApps)

        sw = findViewById(R.id.sw)
        seek = findViewById(R.id.seek)
        limitBox = findViewById(R.id.limitBox)
        tvLimitValue = findViewById(R.id.tvLimitValue)
        swDown = findViewById(R.id.swDown)
        downBox = findViewById(R.id.downBox)
        rowDownStart = findViewById(R.id.rowDownStart)
        rowDownEnd = findViewById(R.id.rowDownEnd)
        tvDownStart = findViewById(R.id.tvDownStart)
        tvDownEnd = findViewById(R.id.tvDownEnd)
        rowStrict = findViewById(R.id.rowStrict)
        swStrict = findViewById(R.id.swStrict)
        tvScopeValue = findViewById(R.id.tvScopeValue)
        blockedBox = findViewById(R.id.blockedBox)
        tvBlockedCount = findViewById(R.id.tvBlockedCount)
        tvAllowedCount = findViewById(R.id.tvAllowedCount)

        tvUsageStatus = findViewById(R.id.tvUsageStatus)
        tvAccStatus = findViewById(R.id.tvAccStatus)
    }

    // ───────── ট্যাব ─────────
    private fun setupTabs() {
        findViewById<View>(R.id.navHome).setOnClickListener { showTab(0) }
        findViewById<View>(R.id.navInsights).setOnClickListener { showTab(1) }
        findViewById<View>(R.id.navRules).setOnClickListener { showTab(2) }
        findViewById<View>(R.id.navSetup).setOnClickListener { showTab(3) }

        // মাঝের গোল বাটন: লক না থাকলে সময় বাছাই, থাকলে হোমে নিয়ে যায়
        findViewById<View>(R.id.btnFocus).setOnClickListener {
            if (Prefs.lockActive(this)) showTab(0) else chooseDuration()
        }
    }

    private fun showTab(i: Int, animate: Boolean = true) {
        val changed = i != tab
        tab = i
        pages.forEachIndexed { idx, p -> p.visibility = if (idx == i) View.VISIBLE else View.GONE }
        val active = getColor(R.color.ui_primary)
        val idle = getColor(R.color.ui_secondary)
        navIcons.forEachIndexed { idx, ic -> ic.setColorFilter(if (idx == i) active else idle) }
        navPills.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.INVISIBLE }

        if (animate && changed) {
            val page = pages[i]
            page.alpha = 0f
            page.animate().alpha(1f).setDuration(180).start()
        }
    }

    // ───────── ফোকাস লক ─────────
    private fun setupLock() {
        swStrict.setCheckedState(Prefs.strictMode(this), animate = false, notify = false)
        swStrict.onChange = { on -> Prefs.setStrictMode(this, on) }
        rowStrict.setOnClickListener { swStrict.toggle() }

        btnLock.setOnClickListener { chooseDuration() }
        btnLockCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("লক বাতিল করবেন?")
                .setMessage("লক শেষ হওয়ার আগেই সব অ্যাপ আবার খুলে যাবে।")
                .setPositiveButton("বাতিল করুন") { _, _ ->
                    if (!Prefs.cancelLock(this)) {
                        Toast.makeText(this, "কঠোর মোডে লক বাতিল করা যায় না", Toast.LENGTH_SHORT).show()
                    }
                    renderLock()
                }
                .setNegativeButton("থাক", null)
                .show()
        }
    }

    private fun chooseDuration() {
        // লক কাজ করতে Accessibility লাগে, না থাকলে আগে সেটআপে পাঠাই
        if (!UsageHelper.isAccessibilityOn(this)) {
            Toast.makeText(this, "আগে Accessibility সার্ভিস চালু করুন", Toast.LENGTH_LONG).show()
            openGuide(3)
            return
        }
        val labels = arrayOf(
            "১৫ মিনিট", "৩০ মিনিট", "১ ঘণ্টা", "২ ঘণ্টা", "৪ ঘণ্টা",
            "আজ সারাদিন (রাত ১২টা পর্যন্ত)", "নিজে সময় দিন…"
        )
        val minutes = intArrayOf(15, 30, 60, 120, 240)
        AlertDialog.Builder(this)
            .setTitle("কতক্ষণের জন্য লক করবেন?")
            .setItems(labels) { _, which ->
                when {
                    which < minutes.size -> {
                        val ms = minutes[which] * 60_000L
                        confirmLock(UsageHelper.forDuration(ms)) { Prefs.startLock(this, ms) }
                    }
                    which == minutes.size ->
                        confirmLock("আজ রাত ১২টা পর্যন্ত") { Prefs.startLockUntilMidnight(this) }
                    else -> askCustomMinutes()
                }
            }
            .setNegativeButton("থাক", null)
            .show()
    }

    private fun askCustomMinutes() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "কত মিনিট? (১–১৪৪০)"
        val box = FrameLayout(this)
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0)
        box.addView(input)
        AlertDialog.Builder(this)
            .setTitle("নিজে সময় দিন")
            .setView(box)
            .setPositiveButton("পরবর্তী") { _, _ ->
                val m = input.text.toString().toIntOrNull() ?: 0
                if (m in 1..1440) {
                    val ms = m * 60_000L
                    confirmLock(UsageHelper.forDuration(ms)) { Prefs.startLock(this, ms) }
                } else {
                    Toast.makeText(this, "১ থেকে ১৪৪০ এর মধ্যে মিনিট দিন", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("থাক", null)
            .show()
    }

    private fun confirmLock(phrase: String, start: () -> Unit) {
        val extra = if (Prefs.strictMode(this)) {
            "\n\nকঠোর মোড চালু: একবার শুরু হলে লক বাতিল করা যাবে না।"
        } else {
            "\n\nপ্রয়োজনে হোম স্ক্রিন থেকে বাতিল করতে পারবেন।"
        }
        AlertDialog.Builder(this)
            .setTitle("লক শুরু করবেন?")
            .setMessage("$phrase অনুমোদিত অ্যাপ ছাড়া সব অ্যাপ বন্ধ থাকবে।$extra")
            .setPositiveButton("লক করুন") { _, _ ->
                start()
                showTab(0)
                renderLock()
            }
            .setNegativeButton("থাক", null)
            .show()
    }

    private fun renderLock() {
        val active = Prefs.lockActive(this)
        val strict = Prefs.lockIsStrict(this)

        if (active) {
            val until = Prefs.lockUntil(this)
            val remaining = until - System.currentTimeMillis()
            tvLockTitle.text = "ফোকাস লক চলছে"
            tvLockBody.text = "আর ${UsageHelper.remainingText(remaining)} বাকি।\n" +
                "খুলবে: ${UsageHelper.whenText(until)}" +
                if (strict) "\nকঠোর মোড: বাতিল করা যাবে না।" else ""
            btnLock.visibility = View.GONE
            btnLockCancel.visibility = if (strict) View.GONE else View.VISIBLE
        } else {
            tvLockTitle.setText(R.string.lock_title_idle)
            tvLockBody.setText(R.string.lock_body_idle)
            btnLock.visibility = View.VISIBLE
            btnLockCancel.visibility = View.GONE
        }

        // লক চলাকালীন কঠোর মোড সুইচ বদলানো যাবে না
        rowStrict.isEnabled = !active
        swStrict.isEnabled = !active
        rowStrict.alpha = if (active) 0.4f else 1f
        swStrict.setCheckedState(
            if (active) strict else Prefs.strictMode(this), animate = false, notify = false
        )
    }

    // ───────── দৈনিক লিমিট ─────────
    private fun setupLimit() {
        // স্লাইডার: ৫ মিনিট থেকে ১২ ঘণ্টা, ৫ মিনিটের ধাপে
        seek.max = 143
        seek.progress = (Prefs.limitMinutes(this) / 5 - 1).coerceIn(0, 143)
        tvLimitValue.text = UsageHelper.formatMinutes((seek.progress + 1) * 5)

        val enabled = Prefs.enabled(this)
        sw.setCheckedState(enabled, animate = false, notify = false)
        applyLimitUi(enabled)

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val m = (progress + 1) * 5
                tvLimitValue.text = UsageHelper.formatMinutes(m)
                if (fromUser) {
                    Prefs.setLimit(this@MainActivity, m)
                    render()
                    renderChart()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        sw.onChange = { on ->
            Prefs.setEnabled(this, on)
            applyLimitUi(on)
            render()
            renderChart()
        }
        findViewById<View>(R.id.rowSwitch).setOnClickListener { sw.toggle() }
    }

    private fun applyLimitUi(on: Boolean) {
        seek.isEnabled = on
        limitBox.alpha = if (on) 1f else 0.4f
    }

    // ───────── ডাউনটাইম ─────────
    private fun setupDowntime() {
        val on = Prefs.downtimeEnabled(this)
        swDown.setCheckedState(on, animate = false, notify = false)
        applyDowntimeUi(on)

        swDown.onChange = { v ->
            Prefs.setDowntimeEnabled(this, v)
            applyDowntimeUi(v)
            renderSettings()
        }
        findViewById<View>(R.id.rowDown).setOnClickListener { swDown.toggle() }
        rowDownStart.setOnClickListener { pickTime(true) }
        rowDownEnd.setOnClickListener { pickTime(false) }
    }

    private fun applyDowntimeUi(on: Boolean) {
        downBox.alpha = if (on) 1f else 0.4f
        rowDownStart.isEnabled = on
        rowDownEnd.isEnabled = on
    }

    private fun pickTime(isStart: Boolean) {
        val cur = if (isStart) Prefs.downStart(this) else Prefs.downEnd(this)
        TimePickerDialog(this, { _, h, m ->
            val v = h * 60 + m
            if (isStart) Prefs.setDownStart(this, v) else Prefs.setDownEnd(this, v)
            renderSettings()
        }, cur / 60, cur % 60, false).show()
    }

    // ───────── ব্লকের পরিধি ও অনুমোদিত অ্যাপ ─────────
    private fun setupScope() {
        findViewById<View>(R.id.rowScope).setOnClickListener { pickScope() }
        findViewById<View>(R.id.rowBlocked).setOnClickListener { openAppList(AppListActivity.MODE_BLOCKED) }
        findViewById<View>(R.id.rowAllowed).setOnClickListener { openAppList(AppListActivity.MODE_ALLOWED) }
    }

    private fun pickScope() {
        if (Prefs.strictLockActive(this)) {
            Toast.makeText(this, "কঠোর লক চলছে, এখন বদলানো যাবে না", Toast.LENGTH_SHORT).show()
            return
        }
        val items = arrayOf("সব অ্যাপ (অনুমোদিত বাদে)", "শুধু বেছে নেওয়া অ্যাপ")
        AlertDialog.Builder(this)
            .setTitle("কোন অ্যাপ ব্লক হবে?")
            .setSingleChoiceItems(items, if (Prefs.scopeSelected(this)) 1 else 0) { d, which ->
                Prefs.setScopeSelected(this, which == 1)
                d.dismiss()
                renderSettings()
                if (which == 1 && Prefs.blockedApps(this).isEmpty()) {
                    openAppList(AppListActivity.MODE_BLOCKED)
                }
            }
            .setNegativeButton("বন্ধ", null)
            .show()
    }

    private fun openAppList(mode: String) {
        startActivity(
            Intent(this, AppListActivity::class.java).putExtra(AppListActivity.EXTRA_MODE, mode)
        )
    }

    // ───────── নেভিগেশন ─────────
    private fun setupNavigation() {
        findViewById<View>(R.id.btnHelp).setOnClickListener { openGuide(0) }
        findViewById<View>(R.id.rowHomeApps).setOnClickListener { openAppUsage() }
        findViewById<View>(R.id.rowAllApps).setOnClickListener { openAppUsage() }
        findViewById<View>(R.id.cardQuickDown).setOnClickListener { showTab(2) }

        findViewById<View>(R.id.rowUsage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        findViewById<View>(R.id.rowAcc).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.rowGuide).setOnClickListener { openGuide(0) }
        findViewById<View>(R.id.btnSetup).setOnClickListener {
            openGuide(if (!UsageHelper.hasPermission(this)) 2 else 3)
        }
    }

    private fun openAppUsage() = startActivity(Intent(this, AppUsageActivity::class.java))

    private fun openGuide(page: Int) {
        startActivity(
            Intent(this, OnboardingActivity::class.java)
                .putExtra(OnboardingActivity.EXTRA_PAGE, page)
                .putExtra(OnboardingActivity.EXTRA_FROM_MAIN, true)
        )
    }

    // ───────── লাইফসাইকেল ─────────
    override fun onResume() {
        super.onResume()
        if (!ready) return
        renderSettings()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }

    // ───────── ডেটা লোড (ব্যাকগ্রাউন্ড থ্রেডে) ─────────
    private fun refresh() {
        renderLock() // লকের কাউন্টডাউন প্রতি ৩০ সেকেন্ডে নতুন হয়
        if (!UsageHelper.hasPermission(this)) {
            lastUsed = 0L
            topApps = emptyList()
            week = emptyList()
            renderAll()
            return
        }
        if (loading) return
        loading = true
        Thread {
            try {
                val used = UsageHelper.todayUsageMs(this)
                val apps = UsageHelper.appUsage(
                    this, UsageHelper.startOfDay(0), System.currentTimeMillis()
                )
                val days = UsageHelper.dailyTotals(this, 7)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    lastUsed = used
                    topApps = apps
                    week = days
                    renderAll()
                }
            } finally {
                loading = false
            }
        }.start()
    }

    private fun renderAll() {
        render()
        renderChart()
        renderApps(listTopApps, 3)
        renderApps(listApps, 5)
        renderDonut()
    }

    // ───────── UI আঁকা ─────────
    private fun render() {
        val hasUsage = UsageHelper.hasPermission(this)
        val accOn = UsageHelper.isAccessibilityOn(this)
        val enabled = Prefs.enabled(this)
        val limitMs = Prefs.limitMinutes(this) * 60_000L
        val used = lastUsed
        val ratio = if (limitMs > 0) used.toFloat() / limitMs else 0f

        banner.visibility = if (hasUsage && accOn) View.GONE else View.VISIBLE
        tvDate.text = UsageHelper.todayLabel()
        tvUsed.text = UsageHelper.short(used)

        val pct = (ratio * 100).toInt().coerceIn(0, 999)
        tvPct.text = if (hasUsage && enabled) UsageHelper.bnDigits("$pct") + "%" else "—"

        val (chipText, chipRes) = when {
            !hasUsage -> "অনুমতি বাকি" to R.color.ui_orange
            !enabled -> "লিমিট বন্ধ" to R.color.ui_secondary
            ratio >= 1f -> "লিমিট শেষ" to R.color.ui_red
            ratio >= 0.8f -> "প্রায় শেষ" to R.color.ui_orange
            else -> "লিমিটের ভেতরে" to R.color.ui_green
        }
        Ui.chip(tvStatusChip, chipText, getColor(chipRes))

        tvHeroLimit.text = if (enabled) "লিমিট: ${UsageHelper.format(limitMs)}" else "দৈনিক লিমিট বন্ধ আছে"
        tvHeroLeft.text = when {
            !hasUsage || !enabled -> ""
            ratio >= 1f -> "আজকের লিমিট শেষ"
            else -> "বাকি: ${UsageHelper.format(limitMs - used)}"
        }
        tvHeroLeft.visibility = if (tvHeroLeft.text.isEmpty()) View.GONE else View.VISIBLE

        val colorRes = when {
            ratio >= 1f -> R.color.ui_red
            ratio >= 0.8f -> R.color.ui_orange
            else -> R.color.ui_primary
        }
        ring.setProgress(if (enabled && hasUsage) ratio else 0f, getColor(colorRes), true)

        setStatus(tvUsageStatus, hasUsage)
        setStatus(tvAccStatus, accOn)
    }

    private fun renderChart() {
        val limit = if (Prefs.enabled(this)) Prefs.limitMinutes(this) * 60_000L else 0L
        barChart.setData(week, UsageHelper.dayLabels(7), limit)
        if (week.isEmpty()) {
            tvAvg.text = "—"
            tvQuickAvg.text = "—"
            tvChartHint.visibility = View.VISIBLE
            tvChartHint.text = "Usage Access দিলে এখানে গত ৭ দিনের হিসাব দেখা যাবে।"
        } else {
            val avg = week.sum() / week.size
            tvAvg.text = UsageHelper.format(avg)
            tvQuickAvg.text = UsageHelper.short(avg)
            tvChartHint.visibility = View.GONE
        }
    }

    private fun renderApps(container: LinearLayout, count: Int) {
        container.removeAllViews()
        if (!UsageHelper.hasPermission(this)) {
            container.addView(
                Ui.message(this, "Usage Access দিলে এখানে অ্যাপ অনুযায়ী হিসাব দেখা যাবে।")
            )
            return
        }
        if (topApps.isEmpty()) {
            container.addView(Ui.message(this, "আজ এখনো কোনো অ্যাপের ব্যবহার নেই।"))
            return
        }
        val top = topApps.take(count)
        val max = top.first().ms
        top.forEachIndexed { i, app ->
            val row = layoutInflater.inflate(R.layout.item_app_usage, container, false)
            Ui.bindAppRow(row, app, max, i)
            container.addView(row)
        }
    }

    private fun renderDonut() {
        legendBox.removeAllViews()
        val colors = Ui.pastel(this)
        if (!UsageHelper.hasPermission(this) || topApps.isEmpty()) {
            donut.setData(emptyList(), colors)
            tvDonutTotal.text = "—"
            legendBox.addView(Ui.message(this, "এখনো তথ্য নেই"))
            return
        }
        val total = topApps.sumOf { it.ms }
        val values = ArrayList<Float>()
        val sliceColors = ArrayList<Int>()

        topApps.take(4).forEachIndexed { i, app ->
            values.add(app.ms.toFloat())
            sliceColors.add(colors[i % colors.size])
            legendBox.addView(Ui.legendRow(this, colors[i % colors.size], app.label, percent(app.ms, total)))
        }
        val others = topApps.drop(4).sumOf { it.ms }
        if (others > 0) {
            val gray = getColor(R.color.ui_chevron)
            values.add(others.toFloat())
            sliceColors.add(gray)
            legendBox.addView(Ui.legendRow(this, gray, "অন্যান্য", percent(others, total)))
        }
        donut.setData(values, sliceColors.toIntArray())
        tvDonutTotal.text = UsageHelper.short(total)
    }

    private fun percent(part: Long, total: Long): String {
        val p = if (total > 0) (part * 100f / total).toInt() else 0
        return UsageHelper.bnDigits("$p") + "%"
    }

    /** সেটিংস-নির্ভর ছোট টেক্সটগুলো */
    private fun renderSettings() {
        tvDownStart.text = UsageHelper.clock(Prefs.downStart(this))
        tvDownEnd.text = UsageHelper.clock(Prefs.downEnd(this))
        tvQuickDown.text = if (Prefs.downtimeEnabled(this)) {
            "${UsageHelper.clock(Prefs.downStart(this))} – ${UsageHelper.clock(Prefs.downEnd(this))}"
        } else {
            "বন্ধ"
        }

        val selected = Prefs.scopeSelected(this)
        tvScopeValue.text = if (selected) "বেছে নেওয়া অ্যাপ" else "সব অ্যাপ"
        blockedBox.visibility = if (selected) View.VISIBLE else View.GONE
        tvBlockedCount.text = UsageHelper.countText(Prefs.blockedApps(this).size)
        tvAllowedCount.text = UsageHelper.countText(Prefs.allowedApps(this).size)

        renderLock()
        render()
    }

    private fun setStatus(tv: TextView, on: Boolean) {
        if (on) Ui.chip(tv, "চালু", getColor(R.color.ui_green))
        else Ui.chip(tv, "বন্ধ", getColor(R.color.ui_red))
    }
}
