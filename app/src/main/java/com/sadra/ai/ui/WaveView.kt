package com.sadra.ai.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.abs
import kotlin.math.sin

/** Lightweight View-based waveform for the overlay. The animator only runs while [active]. */
class WaveView(context: Context) : View(context) {
    var level = 0.15f
    private var phase = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var animator: ValueAnimator? = null

    var active = false
        set(v) {
            field = v
            if (v) {
                if (animator == null) animator = ValueAnimator.ofFloat(0f, 6.2832f).apply {
                    duration = 1100; repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator()
                    addUpdateListener { phase = it.animatedValue as Float; invalidate() }
                }
                animator?.start()
            } else { animator?.cancel(); invalidate() }
        }

    override fun onDraw(c: Canvas) {
        paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), 0xFF00E5FF.toInt(), 0xFF2979FF.toInt(), Shader.TileMode.CLAMP)
        val n = 28
        val step = width.toFloat() / n
        val amp = if (active) level.coerceIn(0.15f, 1f) else 0.05f
        for (i in 0 until n) {
            val h = height * (0.08f + 0.92f * amp * (if (active) abs(sin(phase + i * 0.45f)) else 0.5f))
            rect.set(i * step + step * 0.2f, (height - h) / 2f, i * step + step * 0.8f, (height + h) / 2f)
            c.drawRoundRect(rect, step * 0.3f, step * 0.3f, paint)
        }
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }
}
