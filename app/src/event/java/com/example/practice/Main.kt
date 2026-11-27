package com.example.practice

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import java.util.Calendar

class MainActivity : PracticeActivity() {
    private val events = listOf("请选择活动类型", "技术分享", "读书交流", "志愿服务")
    private var date: IntArray? = null
    private lateinit var dateButton: Button
    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var event: Spinner
    private lateinit var error: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val form = page("活动报名", "填写信息后查看报名摘要。").card()
        date = savedInstanceState?.getIntArray("date")
        name = form.field("姓名")
        name.filters = arrayOf(InputFilter.LengthFilter(80))
        email = form.field("电子邮箱", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        email.filters = arrayOf(InputFilter.LengthFilter(254))
        name.setText(savedInstanceState?.getString("name").orEmpty())
        email.setText(savedInstanceState?.getString("email").orEmpty())

        val label = form.text("活动类型")
        event = Spinner(this).apply {
            id = View.generateViewId()
            minimumHeight = dp(48)
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, events).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(savedInstanceState?.getInt("event") ?: 0)
        }
        label.labelFor = event.id
        form.addView(event)
        dateButton = form.button("选择活动日期") { pickDate() }
        renderDate()
        form.button("查看报名摘要") { openSummary() }
        error = form.text("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
    }

    private fun pickDate() {
        val today = Calendar.getInstance()
        val selected = date ?: intArrayOf(
            today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)
        )
        // DatePicker 的月份从 0 开始，保存选择时保留原值，展示时换算成人读的月份。
        DatePickerDialog(this, { _, year, month, day ->
            date = intArrayOf(year, month, day)
            renderDate()
            error.text = ""
        }, selected[0], selected[1], selected[2]).show()
    }

    private fun openSummary() {
        val enteredName = name.text.toString().trim()
        val enteredEmail = email.text.toString().trim()
        val validEmail = Patterns.EMAIL_ADDRESS.matcher(enteredEmail).matches()
        name.error = if (enteredName.isEmpty()) "请输入姓名。" else null
        email.error = if (validEmail) null else "请输入有效的邮箱地址。"
        val selectedDate = dateText()
        val selectedEvent = event.selectedItemPosition
        error.text = when {
            selectedEvent <= 0 -> "请选择活动类型。"
            selectedDate == null -> "请选择活动日期。"
            else -> ""
        }
        if (enteredName.isEmpty() || !validEmail || selectedEvent <= 0 || selectedDate == null) return
        // 校验完成后，通过显式 Intent 将几个字符串交给摘要页。
        startActivity(Intent(this, SummaryActivity::class.java)
            .putExtra(SummaryActivity.NAME, enteredName)
            .putExtra(SummaryActivity.EMAIL, enteredEmail)
            .putExtra(SummaryActivity.EVENT, events[selectedEvent])
            .putExtra(SummaryActivity.DATE, selectedDate))
    }

    private fun dateText() = date?.let { "${it[0]} 年 ${it[1] + 1} 月 ${it[2]} 日" }

    private fun renderDate() {
        val selected = dateText()
        dateButton.text = selected ?: "选择活动日期"
        dateButton.contentDescription = selected?.let { "活动日期：$it" }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("name", name.text.toString())
        outState.putString("email", email.text.toString())
        outState.putInt("event", event.selectedItemPosition)
        outState.putIntArray("date", date)
        super.onSaveInstanceState(outState)
    }
}
