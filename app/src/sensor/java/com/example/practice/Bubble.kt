package com.example.practice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.hypot

internal class BubbleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scale = resources.displayMetrics.density
    private val surface = context.getColor(R.color.canvas)
    private val lines = context.getColor(R.color.rule)
    private val green = context.getColor(R.color.accent)
    private val amber = Color.rgb(155, 84, 20)
    private var x = 0f
    private var y = 0f
    private var bubble = green

    init {
        // 界面上的倾斜角和水平状态提供等效的文本信息。
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun update(horizontal: Float, vertical: Float, level: Boolean) {
        // 对角线方向也要受圆形边界限制。Canvas 的 Y 轴与设备的 Y 轴相反。
        val distance = maxOf(1f, hypot(horizontal, vertical))
        x = horizontal / distance
        y = -vertical / distance
        bubble = if (level) green else amber
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(cx, cy) - 8f * scale
        if (radius <= 0f) return

        paint.style = Paint.Style.FILL
        paint.color = surface
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = lines
        paint.strokeWidth = 1.5f * scale
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, radius * 0.26f, paint)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, paint)
        canvas.drawLine(cx, cy - radius, cx, cy + radius, paint)

        val bubbleRadius = radius * 0.13f
        val travel = radius - bubbleRadius - 3f * scale
        paint.color = bubble
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx + x * travel, cy + y * travel, bubbleRadius, paint)
    }
}
