package com.example.practice

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import java.math.BigDecimal

class MainActivity : PracticeActivity() {
    private lateinit var amount: EditText
    private lateinit var people: EditText
    private lateinit var result: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = page("账单分摊", "以分为单位分摊，结果合计等于账单总额。")
        val form = body.card()
        amount = form.field("账单金额（元），例如 100.00", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        amount.filters = arrayOf(InputFilter.LengthFilter(18))
        people = form.field("人数，例如 3", InputType.TYPE_CLASS_NUMBER)
        people.filters = arrayOf(InputFilter.LengthFilter(10))
        form.button("计算分摊") { split() }

        val output = body.card()
        output.heading("分摊结果")
        result = output.text(savedInstanceState?.getString("result") ?: EMPTY)
        result.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        amount.setText(savedInstanceState?.getString("amount").orEmpty())
        people.setText(savedInstanceState?.getString("people").orEmpty())
    }

    private fun split() {
        // 金额格式最多两个小数位，先用 BigDecimal 转成整数分，再进行整除。
        val raw = amount.text.toString().trim().replace(',', '.')
        val cents = raw.takeIf { it.matches(MONEY) }?.toBigDecimalOrNull()
            ?.movePointRight(2)?.longValueExact()?.takeIf { it > 0 }
        val count = people.text.toString().trim().toIntOrNull()?.takeIf { it > 0 }
        amount.error = if (cents == null) "请输入大于 0 的金额，最多两位小数。" else null
        people.error = if (count == null) "请输入大于 0 的整数人数。" else null
        if (cents == null || count == null) {
            result.text = EMPTY
            if (cents == null) amount.requestFocus() else people.requestFocus()
            return
        }
        val base = cents / count
        val extra = (cents % count).toInt()
        // 将剩余的每一分钱分配给一位参与者，两组之和正好是原金额。
        result.text = buildString {
            append("总额 ${money(cents)} · $count 人\n")
            if (extra == 0) {
                append("每人 ${money(base)}")
            } else {
                append("$extra 人各付 ${money(base + 1)}\n")
                append("${count - extra} 人各付 ${money(base)}")
            }
        }
        getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(amount.windowToken, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("amount", amount.text.toString())
        outState.putString("people", people.text.toString())
        outState.putString("result", result.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun money(cents: Long) = "¥${BigDecimal.valueOf(cents, 2).toPlainString()}"

    private companion object {
        // 限制整数位数，转换后的整数分始终落在 Long 的范围内。
        val MONEY = Regex("""(?:\d{1,15}(?:\.\d{0,2})?|\.\d{1,2})""")
        const val EMPTY = "输入金额和人数后查看结果。"
    }
}
