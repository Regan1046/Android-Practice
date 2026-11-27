package com.example.practice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class InkPoint(val x: Float, val y: Float)
data class Stroke(val color: Int, val points: List<InkPoint>)

class DrawModel : ViewModel() {
    // ViewModel 让笔迹和笔色随 Activity 旋转保留。导出的 PNG 可以长期保存。
    val strokes = mutableListOf<Stroke>()
    var color = Color.rgb(20, 108, 99)
}

class MainActivity : PracticeActivity() {
    private val model by lazy { ViewModelProvider(this)[DrawModel::class.java] }
    private lateinit var board: SketchView
    private lateinit var status: TextView
    private lateinit var brushLabel: TextView
    private lateinit var undo: Button
    private lateinit var clear: Button
    private lateinit var exportButton: Button
    private var exporting = false
    private val colors = linkedMapOf(
        "青绿" to Color.rgb(20, 108, 99),
        "靛蓝" to Color.rgb(48, 67, 158),
        "砖红" to Color.rgb(168, 61, 48)
    )

    // 系统文件选择器返回可写 URI，应用可以在用户选定的位置写入 PNG。
    private val createImage = registerForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri == null || board.width == 0 || board.height == 0 || model.strokes.isEmpty()) return@registerForActivityResult
        val bitmap = Bitmap.createBitmap(board.width, board.height, Bitmap.Config.ARGB_8888)
        board.draw(Canvas(bitmap))
        exporting = true
        refresh()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = contentResolver.openOutputStream(uri, "w") ?: throw IOException("No output")
                    output.use {
                        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("PNG encoding failed")
                    }
                }
                status.text = "PNG 已保存。"
            } catch (_: IOException) {
                status.text = "无法保存 PNG，请重试。"
            } catch (_: SecurityException) {
                status.text = "无法写入所选位置，请重新选择。"
            } finally {
                bitmap.recycle()
                exporting = false
                refresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = page("涂鸦板", "在画布上拖动手指绘制。可以撤销、换色或保存为 PNG。")
        val controls = screen.card()
        brushLabel = controls.heading("")
        val palette = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(palette)
        colors.forEach { (name, color) ->
            palette.addView(Button(this).apply {
                text = name
                isAllCaps = false
                minHeight = dp(48)
                setTextColor(color)
                setOnClickListener { model.color = color; updateBrush() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        updateBrush()
        val sheet = screen.card()
        board = SketchView(this, model) {
            status.text = ""
            refresh()
        }
        sheet.addView(board, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(268)))
        status = sheet.text("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        undo = sheet.button("撤销一笔") {
            if (model.strokes.isNotEmpty()) model.strokes.removeAt(model.strokes.lastIndex)
            board.invalidate()
            status.text = ""
            refresh()
        }
        clear = sheet.button("清空画布") {
            model.strokes.clear()
            board.invalidate()
            status.text = ""
            refresh()
        }
        exportButton = sheet.button("保存 PNG") { createImage.launch("sketch.png") }
        refresh()
    }

    private fun updateBrush() {
        brushLabel.text = "画笔颜色 · ${colors.entries.first { it.value == model.color }.key}"
    }

    private fun refresh() {
        val hasDrawing = model.strokes.isNotEmpty()
        undo.isEnabled = hasDrawing
        clear.isEnabled = hasDrawing
        exportButton.isEnabled = hasDrawing && !exporting
        board.contentDescription = "画布，${model.strokes.size} 笔"
        if (status.text.isEmpty()) status.text = "共 ${model.strokes.size} 笔"
    }
}
