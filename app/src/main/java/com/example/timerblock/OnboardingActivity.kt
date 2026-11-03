package com.example.timerblock

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper

/**
 * প্রথমবার অ্যাপ খুললে যে গাইড:
 * ০ স্বাগতম → ১ কীভাবে কাজ করে → ২ Usage Access → ৩ Accessibility → ৪ শেষ
 *
 * পারমিশন পেজে নিচের প্রধান বাটনটাই "অনুমতি দিন" হয়ে যায়; অনুমতি দিয়ে ফিরলে নিজে থেকে পরের পেজে যায়।
 * "এখন নয়" চাপলে ওই পেজ বাদ দিয়ে পরেরটায় যায়, আর উপরের "স্কিপ" পুরো গাইড বন্ধ করে।
 */
class OnboardingActivity : Activity() {

    companion object {
        const val EXTRA_PAGE = "page"
        const val EXTRA_FROM_MAIN = "from_main"
        private const val PAGE_USAGE = 2
        private const val PAGE_ACC = 3
    }

    private lateinit var flipper: ViewFlipper
    private lateinit var btnBack: TextView
    private lateinit var btnSkip: TextView
    private lateinit var btnNext: Button
    private lateinit var btnLater: TextView
    private lateinit var dots: LinearLayout
    private lateinit var statusUsage: TextView
    private lateinit var statusAcc: TextView
    private lateinit var statusUsage2: TextView
    private lateinit var statusAcc2: TextView
    private lateinit var btnAppInfo: TextView
    private lateinit var tvDoneBody: TextView

    private val lastPage = 4
    private var page = 0
    private var fromMain = false
    private var awaitingPermission = false
    private val nextLabels = intArrayOf(
        R.string.ob_lets, R.string.ob_next, R.string.ob_next, R.string.ob_next, R.string.ob_start
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        fromMain = intent.getBooleanExtra(EXTRA_FROM_MAIN, false)

        flipper = findViewById(R.id.flipper)
        btnBack = findViewById(R.id.btnBack)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        btnLater = findViewById(R.id.btnLater)
        dots = findViewById(R.id.dots)
        statusUsage = findViewById(R.id.statusUsage)
        statusAcc = findViewById(R.id.statusAcc)
        statusUsage2 = findViewById(R.id.statusUsage2)
        statusAcc2 = findViewById(R.id.statusAcc2)
        btnAppInfo = findViewById(R.id.btnAppInfo)
        tvDoneBody = findViewById(R.id.tvDoneBody)

        buildDots()

        btnBack.setOnClickListener { goTo(page - 1) }
        btnSkip.setOnClickListener { finishOnboarding() }
        btnLater.setOnClickListener { goTo(page + 1) }
        btnNext.setOnClickListener { onPrimary() }
        btnAppInfo.setOnClickListener { openAppInfo() }

        val start = intent.getIntExtra(EXTRA_PAGE, 0).coerceIn(0, lastPage)
        flipper.displayedChild = start
        page = start
        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi() // সেটিংস থেকে ফিরলে বাটনের লেখা ও অবস্থা আপডেট

        // অনুমতি দিয়ে ফিরলে নিজে থেকে পরের পেজে যাই
        if (awaitingPermission) {
            awaitingPermission = false
            if (!needsGrant()) {
                val current = page
                btnNext.postDelayed({
                    if (!isFinishing && page == current) goTo(current + 1)
                }, 450)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (page > 0) goTo(page - 1) else super.onBackPressed()
    }

    // ───────── প্রধান বাটন ─────────
    private fun needsGrant(): Boolean =
        (page == PAGE_USAGE && !UsageHelper.hasPermission(this)) ||
            (page == PAGE_ACC && !UsageHelper.isAccessibilityOn(this))

    private fun onPrimary() {
        when {
            page == PAGE_USAGE && needsGrant() -> {
                awaitingPermission = true
                openUsageSettings()
            }
            page == PAGE_ACC && needsGrant() -> {
                awaitingPermission = true
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            page == lastPage -> finishOnboarding()
            else -> goTo(page + 1)
        }
    }

    // ───────── পেজ বদল ─────────
    private fun goTo(target: Int) {
        if (target < 0 || target > lastPage || target == page) return
        val forward = target > page
        flipper.setInAnimation(this, if (forward) R.anim.slide_in_right else R.anim.slide_in_left)
        flipper.setOutAnimation(this, if (forward) R.anim.slide_out_left else R.anim.slide_out_right)
        flipper.displayedChild = target
        page = target
        updateUi()
    }

    private fun updateUi() {
        btnBack.visibility = if (page > 0) View.VISIBLE else View.INVISIBLE
        btnSkip.visibility = if (page < lastPage) View.VISIBLE else View.INVISIBLE

        val grant = needsGrant()
        btnNext.setText(
            when {
                grant && page == PAGE_USAGE -> R.string.ob_grant_usage
                grant && page == PAGE_ACC -> R.string.ob_grant_acc
                else -> nextLabels[page]
            }
        )
        btnLater.visibility = if (grant) View.VISIBLE else View.INVISIBLE

        updateDots()
        updateStatus()
    }

    // ───────── পেজ ইন্ডিকেটর (সক্রিয় ডট লম্বা পিল) ─────────
    private fun buildDots() {
        for (i in 0..lastPage) dots.addView(View(this))
    }

    private fun updateDots() {
        val active = getColor(R.color.ui_primary)
        val inactive = getColor(R.color.ui_separator)
        for (i in 0 until dots.childCount) {
            val dot = dots.getChildAt(i)
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = Ui.dp(this, 4).toFloat()
            bg.setColor(if (i == page) active else inactive)
            dot.background = bg
            val lp = LinearLayout.LayoutParams(Ui.dp(this, if (i == page) 22 else 8), Ui.dp(this, 8))
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0)
            dot.layoutParams = lp
        }
    }

    // ───────── অনুমতির অবস্থা ─────────
    private fun updateStatus() {
        val usageOk = UsageHelper.hasPermission(this)
        val accOk = UsageHelper.isAccessibilityOn(this)

        setStatus(statusUsage, usageOk)
        setStatus(statusUsage2, usageOk)
        setStatus(statusAcc, accOk)
        setStatus(statusAcc2, accOk)

        btnAppInfo.visibility = if (accOk) View.GONE else View.VISIBLE
        tvDoneBody.setText(if (usageOk && accOk) R.string.ob_done_ok else R.string.ob_done_partial)
    }

    private fun setStatus(tv: TextView, on: Boolean) {
        if (on) {
            Ui.chip(tv, "✓ চালু আছে", getColor(R.color.ui_green))
        } else {
            Ui.chip(tv, "বন্ধ আছে", getColor(R.color.ui_orange))
        }
    }

    // ───────── সেটিংস খোলা ─────────
    private fun openUsageSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun openAppInfo() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
        )
    }

    // ───────── শেষ / স্কিপ ─────────
    private fun finishOnboarding() {
        Prefs.setOnboardingDone(this, true)
        if (!fromMain) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
