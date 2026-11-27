package com.example.practice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import kotlin.math.ceil

class WaterChart(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.muted)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, resources.displayMetrics)
    }
    private val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.rule) }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.accent)
        strokeWidth = 2.5f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.accent) }
    private val path = Path()
    private var samples = emptyList<WaterDay>()
    private var timestamps = emptyList<Long>()
    private var upper = 500

    fun plot(data: List<WaterDay>) {
        samples = data
        timestamps = data.map { requireNotNull(parseDay(it.date)).time }
        val maximum = data.maxOfOrNull { it.milliliters } ?: 0
        upper = (ceil(maximum / 500.0).toInt() * 500).coerceAtLeast(500)
        contentDescription = data.joinToString("，", prefix = "饮水量趋势，") { "${it.date} ${it.milliliters} 毫升" }
        visibility = if (data.isEmpty()) GONE else VISIBLE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (samples.isEmpty()) return
        val left = label.measureText(upper.toString()) + 12 * density
        val right = width - 10 * density
        val top = label.textSize
        val bottom = height - 2.2f * label.textSize
        if (right <= left || bottom <= top) return

        // 横轴使用真实日期间隔，纵轴从 0 到上方刻度按比例换算。
        val start = timestamps.first()
        val span = timestamps.last() - start
        fun x(index: Int): Float =
            if (span == 0L) (left + right) / 2 else left + (right - left) * ((timestamps[index] - start).toDouble() / span).toFloat()
        fun y(amount: Int): Float = bottom - (bottom - top) * amount.toFloat() / upper

        label.textAlign = Paint.Align.RIGHT
        for (step in 0..4) {
            val value = upper * step / 4
            canvas.drawLine(left, y(value), right, y(value), grid)
            canvas.drawText(value.toString(), left - 8 * density, y(value) + label.textSize * .32f, label)
        }

        path.rewind()
        samples.forEachIndexed { index, sample ->
            val point = y(sample.milliliters)
            if (index == 0) path.moveTo(x(index), point) else path.lineTo(x(index), point)
        }
        canvas.drawPath(path, line)
        samples.forEachIndexed { index, sample -> canvas.drawCircle(x(index), y(sample.milliliters), 4 * density, dot) }

        val baseline = bottom + 1.5f * label.textSize
        val first = samples.first().date.substring(5)
        if (samples.size == 1) {
            label.textAlign = Paint.Align.CENTER
            canvas.drawText(first, x(0), baseline, label)
        } else {
            label.textAlign = Paint.Align.LEFT
            canvas.drawText(first, left, baseline, label)
            label.textAlign = Paint.Align.RIGHT
            canvas.drawText(samples.last().date.substring(5), right, baseline, label)
        }
    }
}
