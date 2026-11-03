package com.example.timerblock

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.ceil

/** গত ৭ দিনের বার চার্ট: আজকের বার ভায়োলেট, বাকিগুলো হালকা লাইলাক, লিমিট পার হলে লাল */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private var values: List<Long> = emptyList()
    private var labels: List<String> = emptyList()
    private var limitMs = 0L
    private val rect = RectF()

    private val primary = context.getColor(R.color.ui_primary)
    private val pastel = context.getColor(R.color.pastel_lilac)
    private val red = context.getColor(R.color.ui_red)
    private val secondary = context.getColor(R.color.ui_secondary)
    private val labelColor = context.getColor(R.color.ui_label)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ui_separator)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(1f * d, 6f * d), 0f)
    }
    private val limitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.ui_orange)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * d
        pathEffect = DashPathEffect(floatArrayOf(6f * d, 5f * d), 0f)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics
        )
    }

    /** limitMs = 0 হলে লিমিট লাইন দেখাবে না */
    fun setData(values: List<Long>, labels: List<String>, limitMs: Long) {
        this.values = values
        this.labels = labels
        this.limitMs = limitMs
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val labelW = 30 * d
        val topPad = 8 * d
        val bottomPad = 28 * d
        val chartW = w - labelW
        val chartH = h - topPad - bottomPad
        val baseY = topPad + chartH

        // স্কেল: পুরো ঘণ্টা, জোড় সংখ্যা (কমপক্ষে ২), যাতে মাঝের গ্রিডলাইনও পুরো ঘণ্টা হয়
        val maxMs = maxOf(values.maxOrNull() ?: 0L, limitMs, 3_600_000L)
        var maxH = ceil(maxMs / 3_600_000.0).toInt()
        if (maxH % 2 == 1) maxH += 1
        if (maxH < 2) maxH = 2
        val maxV = maxH * 3_600_000f

        // ডটেড গ্রিড + ডানে লেবেল (০, মাঝ, সর্বোচ্চ)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = secondary
        textPaint.isFakeBoldText = false
        for (k in 0..2) {
            val y = baseY - chartH * k / 2f
            canvas.drawLine(0f, y, chartW, y, gridPaint)
            val hours = maxH * k / 2
            val text = if (hours == 0) "০" else UsageHelper.bnDigits(hours.toString()) + "ঘ"
            canvas.drawText(text, w, y + 4 * d, textPaint)
        }

        val slot = chartW / values.size
        val barW = slot * 0.4f
        textPaint.textAlign = Paint.Align.CENTER
        for (i in values.indices) {
            val cx = slot * i + slot / 2f
            val isToday = i == values.lastIndex
            val barH = maxOf((values[i] / maxV) * chartH, barW)

            barPaint.color = when {
                limitMs > 0 && values[i] >= limitMs -> red
                isToday -> primary
                else -> pastel
            }
            rect.set(cx - barW / 2f, baseY - barH, cx + barW / 2f, baseY)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, barPaint)

            textPaint.color = if (isToday) labelColor else secondary
            textPaint.isFakeBoldText = isToday
            canvas.drawText(labels.getOrElse(i) { "" }, cx, h - 7 * d, textPaint)
        }

        if (limitMs > 0 && limitMs <= maxV) {
            val y = baseY - (limitMs / maxV) * chartH
            canvas.drawLine(0f, y, chartW, y, limitPaint)
        }
    }
}
