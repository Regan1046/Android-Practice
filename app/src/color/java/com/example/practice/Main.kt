package com.example.practice

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import java.util.Locale

class MainActivity : PracticeActivity() {
    private val channels = listOf("红色（R）", "绿色（G）", "蓝色（B）")
    private var rgb = intArrayOf(20, 108, 99)
    private lateinit var swatch: TextView
    private lateinit var labels: List<TextView>
    private lateinit var feedback: TextView
    private var hex = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getIntArray("rgb")?.let { rgb = it }
        val body = page("RGB 调色板", "拖动滑块可预览颜色，并复制十六进制色值。")
        val panel = body.card()
        swatch = panel.heading("").apply {
            gravity = Gravity.CENTER
            minimumHeight = dp(136)
        }
        feedback = panel.text("")
        feedback.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        panel.button("复制 HEX") {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("HEX", hex))
            feedback.text = "已复制 $hex"
        }

        val sliders = body.card()
        val controls = channels.mapIndexed { index, channel ->
            val label = sliders.text("")
            val bar = SeekBar(this).apply {
                max = 255
                progress = rgb[index]
                minimumHeight = dp(48)
                contentDescription = channel
            }
            sliders.addView(bar)
            label to bar
        }
        labels = controls.map { it.first }
        controls.forEachIndexed { index, (_, bar) ->
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                    rgb[index] = progress
                    feedback.text = ""
                    renderColor()
                }
                override fun onStartTrackingTouch(view: SeekBar) = Unit
                override fun onStopTrackingTouch(view: SeekBar) = Unit
            })
        }
        renderColor()
    }

    private fun renderColor() {
        val color = Color.rgb(rgb[0], rgb[1], rgb[2])
        // Color.rgb 返回 ARGB 整数，按位与只保留红、绿、蓝三部分，再补齐六位。
        hex = String.format(Locale.ROOT, "#%06X", color and 0x00FFFFFF)
        swatch.text = hex
        swatch.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), getColor(R.color.rule))
        }
        // 按背景亮度选择前景色，使浅色和深色色块上的 HEX 都清晰。
        swatch.setTextColor(if (ColorUtils.calculateLuminance(color) > 0.179) Color.BLACK else Color.WHITE)
        labels.forEachIndexed { index, label -> label.text = "${channels[index]}：${rgb[index]}" }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray("rgb", rgb.copyOf())
        super.onSaveInstanceState(outState)
    }
}
