package com.example.practice

import android.app.AlertDialog
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.text.InputFilter
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
import java.math.BigDecimal

class MainActivity : PracticeActivity() {
    private var editingId: Long? = null
    private var busy = false
    private lateinit var titleField: EditText
    private lateinit var amountField: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var retryButton: Button
    private lateinit var formTitle: TextView
    private lateinit var status: TextView
    private lateinit var total: TextView
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = savedInstanceState?.getLong("id")?.takeIf { it > 0 }
        val content = page("日常记账", "添加、编辑或删除本机账目，并查看总支出。")
        val form = content.card()
        formTitle = form.heading("")
        titleField = form.field("支出名称，如午餐").apply {
            filters = arrayOf(InputFilter.LengthFilter(80))
            setSingleLine(true)
            setText(savedInstanceState?.getString("title"))
        }
        amountField = form.field("金额（元）", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            .apply {
                filters = arrayOf(InputFilter.LengthFilter(12))
                setText(savedInstanceState?.getString("amount"))
            }
        saveButton = form.button("记一笔", ::submit)
        cancelButton = form.button("取消编辑", ::clearForm)

        val overview = content.card()
        total = overview.heading("")
        status = overview.text("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        retryButton = overview.button("重新读取") { load() }
        list = content.card()
        updateForm()
        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("id", editingId ?: 0)
        outState.putString("title", titleField.text.toString())
        outState.putString("amount", amountField.text.toString())
    }

    private fun submit() {
        val title = titleField.text.toString().trim()
        val cents = parseCents(amountField.text.toString())
        if (title.isEmpty()) titleField.error = "请输入支出名称"
        if (cents == null) amountField.error = "请输入大于 0 的金额，最多两位小数"
        if (title.isEmpty() || cents == null) return
        val id = editingId
        load({ db -> db.save(id, title, cents) }, ::clearForm)
    }

    private fun load(write: (ExpenseDb) -> Unit = {}, after: () -> Unit = {}) {
        if (busy) return
        busy = true
        retryButton.visibility = View.GONE
        status.text = "正在读取账目…"
        updateForm()
        lifecycleScope.launch {
            try {
                // 打开、写入、查询、关闭数据库都在 IO 线程执行，界面更新留在主线程。
                val ledger = withContext(Dispatchers.IO) {
                    ExpenseDb(applicationContext).use { it.snapshot(write) }
                }
                render(ledger)
                status.text = "${ledger.expenses.size} 笔记录"
                after()
            } catch (_: SQLiteException) {
                status.text = "无法访问本地账目，请重试。"
                retryButton.visibility = View.VISIBLE
            } finally {
                busy = false
                updateForm()
            }
        }
    }

    private fun render(ledger: Ledger) {
        total.text = "总支出 ¥${formatCents(ledger.cents)}"
        list.removeAllViews()
        list.heading("账目明细")
        if (ledger.expenses.isEmpty()) list.text("还没有支出，添加一笔试试。")
        ledger.expenses.forEach { expense ->
            val row = list.card()
            row.heading(expense.title)
            row.text("¥${formatCents(expense.cents)}")
            row.button("编辑") {
                if (!busy) {
                    editingId = expense.id
                    titleField.setText(expense.title)
                    amountField.setText(formatCents(expense.cents))
                    updateForm()
                    titleField.requestFocus()
                }
            }.contentDescription = "编辑${expense.title}"
            row.button("删除") {
                if (!busy) confirmDelete(expense)
            }.contentDescription = "删除${expense.title}"
        }
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setMessage("删除「${expense.title}」？")
            .setPositiveButton("删除") { _, _ ->
                load({ db -> db.delete(expense.id) }) {
                    if (editingId == expense.id) clearForm()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateForm() {
        val editing = editingId != null
        formTitle.text = if (editing) "编辑支出" else "新增支出"
        saveButton.text = if (editing) "保存更改" else "记一笔"
        saveButton.isEnabled = !busy
        titleField.isEnabled = !busy
        amountField.isEnabled = !busy
        cancelButton.visibility = if (editing) View.VISIBLE else View.GONE
        cancelButton.isEnabled = !busy
    }

    private fun clearForm() {
        editingId = null
        titleField.text.clear()
        amountField.text.clear()
        titleField.error = null
        amountField.error = null
        updateForm()
    }

    private fun parseCents(input: String): Long? {
        val decimal = input.trim().replace(',', '.')
        if (!decimal.matches(Regex("[0-9]+(?:\\.[0-9]{1,2})?"))) return null
        // longValueExact 可拒绝溢出或无法用整数分表示的输入。
        return try {
            BigDecimal(decimal).movePointRight(2).longValueExact().takeIf { it > 0 }
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun formatCents(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString()
}
