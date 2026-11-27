package com.example.practice

import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainActivity : PracticeActivity() {
    private lateinit var day: EditText
    private lateinit var amount: EditText
    private lateinit var save: Button
    private lateinit var status: TextView
    private lateinit var graph: WaterChart
    private lateinit var rows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = page("饮水记录", "录入某一天的总饮水量。同一天再次保存会更新该日记录。")
        val form = screen.card()
        day = form.field("日期（YYYY-MM-DD）").apply {
            setText(savedInstanceState?.getString("date") ?: dayFormat().format(Date()))
        }
        amount = form.field("总饮水量（mL）", InputType.TYPE_CLASS_NUMBER).apply {
            setText(savedInstanceState?.getString("amount").orEmpty())
        }
        save = form.button("保存记录") { saveDay() }
        status = form.text("").apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val history = screen.card()
        history.heading("最近 7 个记录日")
        graph = WaterChart(this)
        history.addView(graph, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(206)))
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        history.addView(rows)
        refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("date", day.text.toString())
        outState.putString("amount", amount.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun saveDay() {
        val date = day.text.toString().trim()
        val milliliters = amount.text.toString().trim().toIntOrNull()
        val today = dayFormat().format(Date())
        day.error = if (parseDay(date) == null || date > today) "请输入有效且不晚于今天的日期" else null
        amount.error = if (milliliters == null || milliliters !in 1..20_000) "请输入 1 到 20000 的整数" else null
        if (day.error != null || amount.error != null || milliliters == null) return
        refresh(WaterDay(date, milliliters))
    }

    private fun refresh(entry: WaterDay? = null) {
        save.isEnabled = false
        status.text = if (entry == null) "正在读取记录…" else "正在保存…"
        val app = applicationContext
        lifecycleScope.launch {
            try {
                // 在 IO 线程一次完成写入与查询。在旋转重建时，新页面会重新读取已保存数据。
                val data = withContext(Dispatchers.IO) {
                    WaterDb(app).use { db ->
                        entry?.let(db::save)
                        db.recent()
                    }
                }
                graph.plot(data)
                rows.removeAllViews()
                data.asReversed().forEach { row -> rows.text("${row.date}    ${row.milliliters} mL") }
                status.text = when {
                    entry != null -> "已保存 ${entry.date} 的记录。"
                    data.isEmpty() -> "暂无记录。输入日期和饮水量开始记录。"
                    else -> "已加载 ${data.size} 天的记录。"
                }
            } catch (_: SQLiteException) {
                status.text = "无法读取或保存记录。请重试。"
            } finally {
                save.isEnabled = true
            }
        }
    }
}
