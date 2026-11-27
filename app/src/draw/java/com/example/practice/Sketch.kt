package com.example.practice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View

class SketchView(context: Context, private val model: DrawModel, private val changed: () -> Unit) : View(context) {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val draft = mutableListOf<InkPoint>()
    private var draftColor = model.color
    private var pointer = -1

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(context.getColor(R.color.canvas))
        model.strokes.forEach { stroke -> drawStroke(canvas, stroke.color, stroke.points) }
        drawStroke(canvas, draftColor, draft)
    }

    private fun drawStroke(canvas: Canvas, color: Int, points: List<InkPoint>) {
        val first = points.firstOrNull() ?: return
        brush.color = color
        if (points.size == 1) {
            brush.style = Paint.Style.FILL
            canvas.drawCircle(first.x * width, first.y * height, brush.strokeWidth / 2, brush)
            return
        }
        brush.style = Paint.Style.STROKE
        path.rewind()
        path.moveTo(first.x * width, first.y * height)
        for (index in 1 until points.size) {
            val point = points[index]
            path.lineTo(point.x * width, point.y * height)
        }
        canvas.drawPath(path, brush)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 绘制期间由画布处理手势，让外层 ScrollView 暂停拦截。
                parent.requestDisallowInterceptTouchEvent(true)
                pointer = event.getPointerId(0)
                draft.clear()
                draftColor = model.color
                addPoint(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(pointer)
                if (index >= 0) {
                    // MotionEvent 可以合并多个历史采样点，将它们按顺序补入折线。
                    for (sample in 0 until event.historySize) {
                        addPoint(event.getHistoricalX(index, sample), event.getHistoricalY(index, sample))
                    }
                    addPoint(event.getX(index), event.getY(index))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                if (pointer == event.getPointerId(index)) {
                    addPoint(event.getX(index), event.getY(index))
                    if (draft.size == 1) performClick()
                    model.strokes.add(Stroke(draftColor, draft.toList()))
                    endStroke()
                    changed()
                }
            }
            MotionEvent.ACTION_CANCEL -> endStroke()
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun addPoint(x: Float, y: Float) {
        if (width <= 0 || height <= 0) return
        // 将坐标存成 0 到 1。屏幕旋转后，按画布的新宽高重绘。
        val point = InkPoint((x / width).coerceIn(0f, 1f), (y / height).coerceIn(0f, 1f))
        if (draft.lastOrNull() != point) draft.add(point)
    }

    private fun endStroke() {
        pointer = -1
        draft.clear()
        parent.requestDisallowInterceptTouchEvent(false)
    }
}
