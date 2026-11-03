package com.example.timerblock

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** সফট সুইচ (৫২×৩২dp, ভায়োলেট ট্র্যাক, সাদা থাম্ব) */
class SoftSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var isChecked = false
        private set
    var onChange: ((Boolean) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private var t = 0f
    private var anim: ValueAnimator? = null
    private val evaluator = ArgbEvaluator()
    private val rect = RectF()
    private val offColor = context.getColor(R.color.ui_switch_off)
    private val onColor = context.getColor(R.color.ui_primary)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26000000 }

    init {
        setOnClickListener { toggle() }
    }

    fun toggle() = setCheckedState(!isChecked, animate = true, notify = true)

    fun setCheckedState(checked: Boolean, animate: Boolean, notify: Boolean) {
        isChecked = checked
        anim?.cancel()
        val target = if (checked) 1f else 0f
        if (animate) {
            anim = ValueAnimator.ofFloat(t, target).apply {
                duration = 200
                addUpdateListener {
                    t = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            t = target
            invalidate()
        }
        if (notify) onChange?.invoke(checked)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension((52 * density).toInt(), (32 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        trackPaint.color = evaluator.evaluate(t, offColor, onColor) as Int
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, h / 2, h / 2, trackPaint)

        val r = h / 2 - 2 * density
        val cx = h / 2 + (w - h) * t
        canvas.drawCircle(cx, h / 2 + density, r, shadowPaint)
        canvas.drawCircle(cx, h / 2, r, thumbPaint)
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        super.onDetachedFromWindow()
    }
}
