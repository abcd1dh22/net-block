package com.example.timerblock

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

object Ui {

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    /** কার্ডের ভেতরে সরু আলাদা-রেখা (iOS স্টাইল) */
    fun addDivider(c: Context, parent: LinearLayout, startDp: Int) {
        val line = View(c)
        line.setBackgroundColor(c.getColor(R.color.ui_separator))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            maxOf(1, dp(c, 1) / 2)
        )
        lp.marginStart = dp(c, startDp)
        parent.addView(line, lp)
    }

    /** স্ট্যাটাস চিপ: লেখা [color] রঙে, ব্যাকগ্রাউন্ড একই রঙের হালকা ছায়া (bg_chip লাগানো TextView-তে) */
    fun chip(tv: TextView, text: String, color: Int) {
        tv.text = text
        tv.setTextColor(color)
        tv.backgroundTintList = ColorStateList.valueOf((color and 0x00FFFFFF) or 0x26000000)
    }

    /** কার্ডের ভেতরে ছোট মেসেজ (খালি অবস্থা ইত্যাদি) */
    fun message(c: Context, text: String): TextView {
        val tv = TextView(c)
        tv.text = text
        tv.textSize = 15f
        tv.setTextColor(c.getColor(R.color.ui_secondary))
        val p = dp(c, 16)
        tv.setPadding(p, p, p, p)
        return tv
    }

    /** item_app_usage.xml সারি ভরাট করে। maxMs = তালিকার সবচেয়ে বেশি সময় (বার-এর মাপের জন্য) */
    fun bindAppRow(row: View, app: AppUsage, maxMs: Long, index: Int = 0) {
        row.findViewById<ImageView>(R.id.appIcon).setImageDrawable(app.icon)
        row.findViewById<TextView>(R.id.appName).text = app.label
        row.findViewById<TextView>(R.id.appTime).text = UsageHelper.short(app.ms)
        val ratio = if (maxMs > 0) (app.ms.toFloat() / maxMs).coerceIn(0.03f, 1f) else 0.03f
        row.findViewById<View>(R.id.barFill).backgroundTintList =
            ColorStateList.valueOf(pastel(row.context)[index % 6])
        row.findViewById<View>(R.id.barFill).layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, ratio)
        row.findViewById<View>(R.id.barRest).layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - ratio)
    }

    /** প্যাস্টেল রঙের সারি: পীচ, মিন্ট, স্কাই, লেমন, রোজ, লাইলাক */
    fun pastel(c: Context): IntArray = intArrayOf(
        c.getColor(R.color.pastel_peach), c.getColor(R.color.pastel_mint),
        c.getColor(R.color.pastel_sky), c.getColor(R.color.pastel_lemon),
        c.getColor(R.color.pastel_rose), c.getColor(R.color.pastel_lilac)
    )

    /** ডোনাট চার্টের পাশের লেজেন্ড সারি: ● নাম ........ ৪৫% */
    fun legendRow(c: Context, color: Int, name: String, pct: String): View {
        val row = LinearLayout(c)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(c, 5), 0, dp(c, 5))

        val dot = View(c)
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(color)
        dot.background = bg
        row.addView(dot, LinearLayout.LayoutParams(dp(c, 10), dp(c, 10)))

        val n = TextView(c)
        n.text = name
        n.textSize = 14f
        n.maxLines = 1
        n.ellipsize = TextUtils.TruncateAt.END
        n.setTextColor(c.getColor(R.color.ui_label))
        val nlp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        nlp.marginStart = dp(c, 10)
        row.addView(n, nlp)

        val p = TextView(c)
        p.text = pct
        p.textSize = 14f
        p.setTypeface(p.typeface, Typeface.BOLD)
        p.setTextColor(c.getColor(R.color.ui_secondary))
        val plp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        plp.marginStart = dp(c, 8)
        row.addView(p, plp)
        return row
    }
}
