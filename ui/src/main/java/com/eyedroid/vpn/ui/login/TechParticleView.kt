package com.eyedroid.vpn.ui.login

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class TechParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF122131.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridSize = 40f
    private var scanY = 0f
    private var animator: ValueAnimator? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, height.toFloat()).apply {
            duration = 8000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                scanY = animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Grid lines — vertical
        var x = 0f
        while (x <= w) {
            canvas.drawLine(x, 0f, x, h, gridPaint)
            x += gridSize
        }
        // Grid lines — horizontal
        var y = 0f
        while (y <= h) {
            canvas.drawLine(0f, y, w, y, gridPaint)
            y += gridSize
        }

        // Scanline gradient
        if (height > 0) {
            scanPaint.shader = LinearGradient(
                0f, scanY - 60f, 0f, scanY + 60f,
                intArrayOf(0x00C3F5FF.toInt(), 0x33C3F5FF.toInt(), 0x0000E5FF.toInt()),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, scanY - 60f, w, scanY + 60f, scanPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
