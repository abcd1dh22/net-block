package com.example.timerblock

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/** Apple Screen Time-এর মতো গোল প্রগ্রেস রিং (স্ট্রোক ভিউয়ের মাপের ~১১%) */
class RingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress = 0f
    private var arcColor = context.getColor(R.color.ui_primary)
    private var anim: ValueAnimator? = null
    private val rect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = context.getColor(R.color.ui_track)
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    /** ডার্ক ব্যাকগ্রাউন্ডে (ব্লক স্ক্রিন) হালকা ট্র্যাকের জন্য */
    fun setTrackColor(color: Int) {
        trackPaint.color = color
        invalidate()
    }

    fun setProgress(target: Float, color: Int, animate: Boolean) {
        arcColor = color
        anim?.cancel()
        val to = target.coerceIn(0f, 1f)
        if (!animate) {
            progress = to
            invalidate()
            return
        }
        anim = ValueAnimator.ofFloat(progress, to).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = minOf(width, height) * 0.115f
        trackPaint.strokeWidth = stroke
        arcPaint.strokeWidth = stroke
        val half = stroke / 2f
        rect.set(half, half, width - half, height - half)

        canvas.drawOval(rect, trackPaint)
        if (progress > 0f) {
            arcPaint.color = arcColor
            canvas.drawArc(rect, -90f, 360f * progress, false, arcPaint)
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}
