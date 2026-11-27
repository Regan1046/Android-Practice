package com.example.practice

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : PracticeActivity() {
    private val prefs by lazy { getSharedPreferences("habit", MODE_PRIVATE) }
    private val checked = mutableSetOf<String>()
    private var loadError = false
    private lateinit var summary: TextView
    private lateinit var week: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // 复制返回的字符串集合，避免直接修改 SharedPreferences 管理的值。
            checked.addAll(prefs.getStringSet("days", emptySet()).orEmpty())
        } catch (_: ClassCastException) {
            loadError = true
        }
        val content = page("每日打卡", "点按日期打卡或撤销，记录保存在这台设备。")
        summary = content.card().heading("")
        week = content.card()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        week.removeAllViews()
        week.heading("最近 7 天")
        if (loadError) {
            summary.text = "无法读取打卡记录"
            week.text("本地记录格式异常。")
            return
        }
        val today = Calendar.getInstance()
        val day = today.clone() as Calendar
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val labelFormat = SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
        val dates = List(7) {
            val date = day.time
            // 按日历上的一天递减，跨时区夏令时也不用假设一天恰好有 24 小时。
            day.add(Calendar.DAY_OF_YEAR, -1)
            date
        }
        val dateKeys = dates.map { keyFormat.format(it) }
        updateSummary(today, keyFormat, dateKeys)
        dates.forEachIndexed { index, date ->
            val key = dateKeys[index]
            val label = labelFormat.format(date)
            val row = CheckBox(this).apply {
                text = if (index == 0) "今天 · $label" else label
                textSize = 16f
                minHeight = dp(48)
                isChecked = key in checked
                setOnCheckedChangeListener { _, marked ->
                    if (marked) checked.add(key) else checked.remove(key)
                    prefs.edit().putStringSet("days", checked.toSet()).apply()
                    updateSummary(today, keyFormat, dateKeys)
                }
            }
            week.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
    }

    private fun updateSummary(today: Calendar, format: SimpleDateFormat, weekKeys: List<String>) {
        val day = today.clone() as Calendar
        // 今天还没打卡时，从昨天算起。遇到第一个断档就结束连续计数。
        if (format.format(day.time) !in checked) day.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (format.format(day.time) in checked) {
            streak++
            day.add(Calendar.DAY_OF_YEAR, -1)
        }
        summary.text = "连续打卡 ${streak} 天\n最近 7 天已打卡 ${weekKeys.count { it in checked }} 天"
    }
}
