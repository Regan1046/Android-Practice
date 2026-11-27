package com.example.practice

import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class Phase { READY, RUNNING, PAUSED, FINISHED }

class MainActivity : PracticeActivity() {
    private var phase = Phase.READY
    private var deadline = 0L
    private var pausedMillis = 0L
    private var ticker: Job? = null

    private lateinit var minutes: EditText
    private lateinit var seconds: EditText
    private lateinit var display: TextView
    private lateinit var status: TextView
    private lateinit var startPause: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phase = Phase.entries.firstOrNull { it.name == savedInstanceState?.getString("phase") }
            ?: Phase.READY
        deadline = savedInstanceState?.getLong("deadline") ?: 0
        pausedMillis = savedInstanceState?.getLong("paused") ?: 0

        val layout = page("倒计时")
        val clock = layout.card()
        display = clock.text("00:00").apply {
            gravity = Gravity.CENTER
            textSize = 56f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        status = clock.text("").apply {
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val settings = layout.card()
        settings.heading("分钟")
        minutes = settings.field("0 至 99", InputType.TYPE_CLASS_NUMBER).apply {
            setText(savedInstanceState?.getString("minutes") ?: "1")
            contentDescription = "分钟"
        }
        settings.heading("秒钟")
        seconds = settings.field("0 至 59", InputType.TYPE_CLASS_NUMBER).apply {
            setText(savedInstanceState?.getString("seconds") ?: "0")
            contentDescription = "秒钟"
        }
        startPause = layout.button("开始") { toggleTimer() }
        layout.button("重置") {
            ticker?.cancel()
            phase = Phase.READY
            deadline = 0
            pausedMillis = 0
            render()
        }
        render()
    }

    override fun onStart() {
        super.onStart()
        render()
        startTicker()
    }

    override fun onStop() {
        // 退到后台时停止刷新 UI，截止时间仍保留，返回时重新计算剩余时间。
        ticker?.cancel()
        ticker = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 动态创建的输入框没有资源 ID，明确保存字段及计时状态供重建后恢复。
        outState.putString("phase", phase.name)
        outState.putLong("deadline", deadline)
        outState.putLong("paused", pausedMillis)
        outState.putString("minutes", minutes.text.toString())
        outState.putString("seconds", seconds.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun toggleTimer() {
        // 点击时再算一次时间，避免截止后与最后一帧 UI 刷新之间发生竞争。
        val currentMillis = remainingNow()
        if (phase == Phase.RUNNING && currentMillis == 0L) {
            phase = Phase.FINISHED
            render()
            return
        }
        if (phase == Phase.RUNNING) {
            pausedMillis = currentMillis
            phase = Phase.PAUSED
            ticker?.cancel()
        } else {
            val duration = if (phase == Phase.PAUSED) pausedMillis else readDuration() ?: return
            // elapsedRealtime 是单调时钟，日期调整和屏幕休眠不会破坏倒计时。
            deadline = SystemClock.elapsedRealtime() + duration
            phase = Phase.RUNNING
            startTicker()
        }
        render()
    }

    private fun readDuration(): Long? {
        val minutesValue = minutes.text.toString().trim().toIntOrNull()
        val secondsValue = seconds.text.toString().trim().toIntOrNull()
        if (minutesValue == null || minutesValue !in 0..99) {
            minutes.error = "请输入 0 至 99 分钟"
            return null
        }
        if (secondsValue == null || secondsValue !in 0..59) {
            seconds.error = "请输入 0 至 59 秒"
            return null
        }
        val duration = (minutesValue * 60L + secondsValue) * 1000
        if (duration == 0L) {
            seconds.error = "时长至少为 1 秒"
            return null
        }
        return duration
    }

    private fun startTicker() {
        ticker?.cancel()
        if (phase != Phase.RUNNING) return
        ticker = lifecycleScope.launch {
            while (phase == Phase.RUNNING) {
                render()
                delay(150L)
            }
        }
    }

    private fun remainingNow(): Long = when (phase) {
        Phase.RUNNING -> (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        Phase.PAUSED -> pausedMillis
        Phase.READY, Phase.FINISHED -> 0L
    }

    private fun render() {
        val millis = remainingNow()
        if (phase == Phase.RUNNING && millis == 0L) phase = Phase.FINISHED
        // 不足一秒时向上取整，真正到期才显示 00:00。
        val time = (millis + 999L) / 1000
        val formatted = String.format(Locale.ROOT, "%02d:%02d", time / 60, time % 60)
        if (display.text.toString() != formatted) display.text = formatted
        val message = when (phase) {
            Phase.READY -> "设置时长后开始"
            Phase.RUNNING -> "计时中"
            Phase.PAUSED -> "已暂停"
            Phase.FINISHED -> "时间到"
        }
        if (status.text.toString() != message) status.text = message
        startPause.text = when (phase) {
            Phase.RUNNING -> "暂停"
            Phase.PAUSED -> "继续"
            Phase.READY, Phase.FINISHED -> "开始"
        }
        val editable = phase == Phase.READY || phase == Phase.FINISHED
        minutes.isEnabled = editable
        seconds.isEnabled = editable
    }
}
