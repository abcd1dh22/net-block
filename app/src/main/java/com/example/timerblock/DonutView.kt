package com.example.timerblock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** অ্যাপ অনুযায়ী ব্যবহারের ডোনাট চার্ট (প্যাস্টেল স্লাইস, স্লাইসের মাঝে ছোট ফাঁক) */
class DonutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()
    private var colors: IntArray = IntArray(0)
    private val rect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.ui_track)
    }
    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    fun setData(values: List<Float>, colors: IntArray) {
        this.values = values
        this.colors = colors
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = minOf(width, height) * 0.17f
        trackPaint.strokeWidth = stroke
        slicePaint.strokeWidth = stroke
        val half = stroke / 2f
        rect.set(half, half, width - half, height - half)

        canvas.drawOval(rect, trackPaint)

        val total = values.sum()
        if (total <= 0f) return

        val gap = if (values.size > 1) 4f else 0f
        var start = -90f
        for (i in values.indices) {
            val sweep = values[i] / total * 360f
            slicePaint.color = colors[i % colors.size]
            canvas.drawArc(rect, start + gap / 2f, maxOf(sweep - gap, 1f), false, slicePaint)
            start += sweep
        }
    }
}
