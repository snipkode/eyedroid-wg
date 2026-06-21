package com.eyedroid.vpn.ui.login

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import kotlin.math.*
import kotlin.random.Random

class TechParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float, var y: Float,
        val vx: Float, val vy: Float,
        val radius: Float, val alpha: Int
    )

    private val particles = mutableListOf<Particle>()
    private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6366F1.toInt()
    }
    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6366F1.toInt()
        strokeWidth = 0.8f
    }
    private var animator: ValueAnimator? = null
    private val connectDist = 160f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        val count = ((w * h) / 18000).coerceIn(20, 55)
        repeat(count) {
            particles += Particle(
                x = Random.nextFloat() * w,
                y = Random.nextFloat() * h,
                vx = (Random.nextFloat() - 0.5f) * 0.6f,
                vy = (Random.nextFloat() - 0.5f) * 0.6f,
                radius = Random.nextFloat() * 2f + 1.2f,
                alpha = Random.nextInt(80, 180)
            )
        }
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val w = width.toFloat(); val h = height.toFloat()
                for (p in particles) {
                    p.x += p.vx
                    p.y += p.vy
                    if (p.x < 0) p.x = w
                    if (p.x > w) p.x = 0f
                    if (p.y < 0) p.y = h
                    if (p.y > h) p.y = 0f
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Draw gradient overlay
        canvas.drawColor(0xFF0F0F0F.toInt())

        // Draw connecting lines
        for (i in particles.indices) {
            for (j in i + 1 until particles.size) {
                val dx = particles[i].x - particles[j].x
                val dy = particles[i].y - particles[j].y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < connectDist) {
                    val alpha = ((1f - dist / connectDist) * 60).toInt()
                    paintLine.alpha = alpha
                    canvas.drawLine(particles[i].x, particles[i].y, particles[j].x, particles[j].y, paintLine)
                }
            }
        }

        // Draw dots
        for (p in particles) {
            paintDot.alpha = p.alpha
            canvas.drawCircle(p.x, p.y, p.radius, paintDot)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
