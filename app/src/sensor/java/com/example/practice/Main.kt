package com.example.practice

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

class MainActivity : PracticeActivity(), SensorEventListener {
    private var manager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var rotation = Surface.ROTATION_0
    private val gravity = FloatArray(3)
    private var hasSample = false
    private var lastReadout = 0L
    private lateinit var gauge: BubbleView
    private lateinit var status: TextView
    private lateinit var values: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = getSystemService(SensorManager::class.java)
        accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val layout = page("水平仪", "将设备平放，观察气泡和倾斜角。")
        val level = layout.card()
        status = level.heading("等待传感器数据…")
        gauge = BubbleView(this)
        level.addView(gauge, LinearLayout.LayoutParams(-1, dp(232)))
        values = level.text("")
        if (accelerometer == null) showUnavailable("此设备没有可用的加速度计。")
    }

    override fun onResume() {
        super.onResume()
        val sensor = accelerometer ?: return
        hasSample = false
        lastReadout = 0L
        rotation = displayRotation()
        // 只在页面处于前台时监听。返回时清空滤波状态，从新读数开始。
        val listening = manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI) == true
        if (!listening) {
            showUnavailable("无法启动加速度计。")
            return
        }
        gauge.visibility = View.VISIBLE
        values.visibility = View.VISIBLE
        status.text = "等待传感器数据…"
        values.text = ""
    }

    override fun onPause() {
        manager?.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val raw = event.values
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || raw.size < 3) return
        // 低通滤波减轻手持抖动，让倾斜计算更接近重力方向。
        for (index in gravity.indices) {
            gravity[index] = if (hasSample) 0.82f * gravity[index] + 0.18f * raw[index] else raw[index]
        }
        hasSample = true

        val gx = gravity[0]
        val gy = gravity[1]
        val gz = gravity[2]
        // 传感器轴跟随设备天然方向。把 X、Y 转到当前显示方向供气泡使用。
        val (x, y) = when (rotation) {
            Surface.ROTATION_90 -> gy to -gx
            Surface.ROTATION_180 -> -gx to -gy
            Surface.ROTATION_270 -> -gy to gx
            else -> gx to gy
        }
        val flat = hypot(gx.toDouble(), gy.toDouble())
        val tilt = Math.toDegrees(atan2(flat, abs(gz).toDouble()))
        val stable = hypot(flat, gz.toDouble()) in 8.0..12.0
        val level = stable && tilt <= 2.0
        gauge.update(x / 4f, y / 4f, level)

        // 气泡跟随传感器刷新，数字较慢更新，阅读时更稳定。
        if (lastReadout != 0L && event.timestamp - lastReadout < 160_000_000L) return
        lastReadout = event.timestamp
        val message = if (stable) {
            val state = if (level) "接近水平" else "设备倾斜"
            String.format(Locale.ROOT, "%s · %.1f°", state, tilt)
        } else {
            "请保持设备静止"
        }
        val readout = String.format(
            Locale.ROOT,
            "加速度（m/s²）\nX  %+.2f    Y  %+.2f    Z  %+.2f",
            raw[0], raw[1], raw[2]
        )
        if (status.text != message) status.text = message
        if (values.text != readout) values.text = readout
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun showUnavailable(message: String) {
        status.text = message
        status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        gauge.visibility = View.GONE
        values.visibility = View.GONE
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(): Int = if (Build.VERSION.SDK_INT >= 30) {
        display?.rotation ?: Surface.ROTATION_0
    } else {
        windowManager.defaultDisplay.rotation
    }
}
