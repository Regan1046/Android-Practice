package com.example.practice

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import java.math.BigDecimal

private enum class LengthUnit(val label: String, val meters: BigDecimal) {
    MM("毫米", BigDecimal("0.001")),
    CM("厘米", BigDecimal("0.01")),
    M("米", BigDecimal.ONE),
    KM("千米", BigDecimal("1000"))
}

class MainActivity : PracticeActivity() {
    private lateinit var amount: EditText
    private lateinit var from: Spinner
    private lateinit var to: Spinner
    private lateinit var answer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = page("长度单位换算", "选择原单位和目标单位，再输入要换算的长度。")
        val form = body.card()
        amount = form.field("长度，例如 2.5", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        amount.filters = arrayOf(InputFilter.LengthFilter(24))
        amount.setText(savedInstanceState?.getString("amount").orEmpty())

        val fromLabel = form.text("原单位")
        from = Spinner(this).apply { id = View.generateViewId() }
        fromLabel.labelFor = from.id
        to = Spinner(this).apply { id = View.generateViewId() }
        val units = LengthUnit.entries
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units.map { it.label })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fun addSpinner(spinner: Spinner, initial: Int) {
            spinner.adapter = adapter
            spinner.minimumHeight = dp(48)
            form.addView(spinner)
            spinner.setSelection(initial)
        }
        addSpinner(from, savedInstanceState?.getInt("from", LengthUnit.M.ordinal) ?: LengthUnit.M.ordinal)
        val toLabel = form.text("目标单位")
        toLabel.labelFor = to.id
        addSpinner(to, savedInstanceState?.getInt("to", LengthUnit.CM.ordinal) ?: LengthUnit.CM.ordinal)

        val result = body.card()
        result.heading("换算结果")
        answer = result.text(savedInstanceState?.getString("answer") ?: "输入长度后即可换算。")
        answer.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE

        form.button("换算") {
            // 校验十进制输入，保留 BigDecimal 的精确值，例如 0.1 米 = 10 厘米。
            val raw = amount.text.toString().trim().replace(',', '.')
            val value = raw.takeIf { it.matches(DECIMAL) }?.toBigDecimalOrNull()
            if (value == null) {
                answer.text = "输入长度后即可换算。"
                amount.error = "请输入有效的非负数字"
                amount.requestFocus()
            } else {
                amount.error = null
                val original = units[from.selectedItemPosition]
                val target = units[to.selectedItemPosition]
                // 先换算成统一基准（米），再除以目标单位的米数。
                val converted = value.multiply(original.meters).divide(target.meters)
                answer.text = "${value.plain()} ${original.label} = ${converted.plain()} ${target.label}"
                getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(amount.windowToken, 0)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // 保存表单和上一次结果，旋转屏幕时由新 Activity 接着显示。
        outState.putString("amount", amount.text.toString())
        outState.putInt("from", from.selectedItemPosition)
        outState.putInt("to", to.selectedItemPosition)
        outState.putString("answer", answer.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun BigDecimal.plain() = stripTrailingZeros().toPlainString()

    private companion object {
        val DECIMAL = Regex("""(?:\d+(?:\.\d*)?|\.\d+)""")
    }
}
