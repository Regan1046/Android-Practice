package com.example.practice

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import org.json.JSONException
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private enum class Filter(val label: String) {
    ALL("全部"), OPEN("未完成"), DONE("已完成")
}

class MainActivity : PracticeActivity() {
    private val store by lazy { TaskStore(this) }
    private var tasks = emptyList<Task>()
    private var loadError = false
    private var filter = Filter.ALL
    private var editingId: String? = null
    private var due: String? = null
    private lateinit var titleField: EditText
    private lateinit var formTitle: TextView
    private lateinit var dueButton: Button
    private lateinit var clearDue: Button
    private lateinit var submit: Button
    private lateinit var cancel: Button
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasks = try {
            store.read()
        } catch (_: JSONException) {
            loadError = true
            emptyList()
        } catch (_: ClassCastException) {
            loadError = true
            emptyList()
        }
        val selected = savedInstanceState?.getInt("filter") ?: 0
        filter = Filter.entries.getOrElse(selected) { Filter.ALL }
        editingId = savedInstanceState?.getString("editingId")
        due = savedInstanceState?.getString("due")

        val content = page("待办事项", "填写事项、设置截止日，然后勾选完成。")
        val form = content.card()
        formTitle = form.heading("")
        titleField = form.field("事项名称").apply {
            filters = arrayOf(InputFilter.LengthFilter(100))
            setText(savedInstanceState?.getString("draft"))
            isEnabled = !loadError
        }
        dueButton = form.button("选择截止日期", ::pickDue).apply { isEnabled = !loadError }
        clearDue = form.button("取消截止日期") {
            due = null
            updateForm()
        }
        submit = form.button("添加待办", ::submitTask).apply { isEnabled = !loadError }
        cancel = form.button("取消编辑", ::resetForm)
        updateForm()

        val controls = content.card()
        controls.heading("按状态筛选")
        val spinner = Spinner(this).apply {
            contentDescription = "按状态筛选"
            minimumHeight = dp(48)
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item,
                Filter.entries.map(Filter::label)).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(filter.ordinal)
            isEnabled = !loadError
        }
        controls.addView(spinner)
        list = content.card()
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filter = Filter.entries[position]
                render()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Bundle 保留正在编辑的表单，已保存的事项从 SharedPreferences 重新读取。
        outState.putInt("filter", filter.ordinal)
        outState.putString("editingId", editingId)
        outState.putString("draft", titleField.text.toString())
        outState.putString("due", due)
    }

    private fun pickDue() {
        val today = Calendar.getInstance()
        val parts = due?.split("-")?.mapNotNull(String::toIntOrNull)?.takeIf { it.size == 3 }
        val year = parts?.get(0) ?: today.get(Calendar.YEAR)
        val month = parts?.get(1)?.minus(1) ?: today.get(Calendar.MONTH)
        val day = parts?.get(2) ?: today.get(Calendar.DAY_OF_MONTH)
        // DatePickerDialog 的月份下标从 0 开始，写入日期字符串时需要加 1。
        DatePickerDialog(this, { _, y, m, d ->
            due = "%04d-%02d-%02d".format(Locale.ROOT, y, m + 1, d)
            updateForm()
        }, year, month, day).show()
    }

    private fun submitTask() {
        val title = titleField.text.toString().trim()
        if (title.isEmpty()) {
            titleField.error = "请输入事项名称"
            return
        }
        val id = editingId
        if (id == null) {
            save(tasks + Task(UUID.randomUUID().toString(), title, due, false))
        } else {
            save(tasks.map { if (it.id == id) it.copy(title = title, due = due) else it })
        }
        resetForm()
    }

    private fun render() {
        list.removeAllViews()
        list.heading("已完成 ${tasks.count { it.done }} / ${tasks.size}")
        val shown = tasks.filter {
            when (filter) {
                Filter.ALL -> true
                Filter.OPEN -> !it.done
                Filter.DONE -> it.done
            }
        }.sortedWith(compareBy<Task> { it.done }.thenBy { it.due == null }.thenBy { it.due })
        when {
            loadError -> list.text("无法读取本地待办事项。")
            tasks.isEmpty() -> list.text("还没有待办事项。")
            shown.isEmpty() -> list.text("没有符合此筛选条件的事项。")
        }
        shown.forEach { task ->
            val row = list.card()
            row.addView(CheckBox(this).apply {
                text = task.title
                minHeight = dp(48)
                isChecked = task.done
                setOnCheckedChangeListener { _, done ->
                    save(tasks.map { if (it.id == task.id) it.copy(done = done) else it })
                }
            })
            row.text(task.due?.let { "截止 $it" } ?: "没有截止日期")
            row.button("编辑") {
                editingId = task.id
                due = task.due
                titleField.setText(task.title)
                updateForm()
                titleField.requestFocus()
            }.contentDescription = "编辑${task.title}"
            row.button("删除") {
                AlertDialog.Builder(this)
                    .setMessage("删除「${task.title}」？")
                    .setPositiveButton("删除") { _, _ ->
                        save(tasks.filter { it.id != task.id })
                        if (editingId == task.id) resetForm()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }.contentDescription = "删除${task.title}"
        }
    }

    private fun save(items: List<Task>) {
        store.save(items)
        tasks = items
        render()
    }

    private fun resetForm() {
        editingId = null
        due = null
        titleField.text.clear()
        titleField.error = null
        updateForm()
    }

    private fun updateForm() {
        formTitle.text = if (editingId == null) "新增待办" else "编辑待办"
        submit.text = if (editingId == null) "添加待办" else "保存更改"
        dueButton.text = due?.let { "截止日期 $it" } ?: "选择截止日期"
        clearDue.visibility = if (due == null) View.GONE else View.VISIBLE
        cancel.visibility = if (editingId == null) View.GONE else View.VISIBLE
    }
}
