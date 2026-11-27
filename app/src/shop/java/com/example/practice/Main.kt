package com.example.practice

import android.app.AlertDialog
import android.graphics.Paint
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

private data class Item(val id: String, val name: String, val checked: Boolean)

class MainActivity : PracticeActivity() {
    private val prefs by lazy { getSharedPreferences("shopping", MODE_PRIVATE) }
    private var items = emptyList<Item>()
    private var loadError = false
    private lateinit var input: EditText
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = try {
            readItems()
        } catch (_: JSONException) {
            loadError = true
            emptyList()
        } catch (_: ClassCastException) {
            loadError = true
            emptyList()
        }

        val content = page("购物清单", "勾选已购买的商品，清单会保存在这台设备。")
        val form = content.card()
        input = form.field("输入商品名称").apply {
            filters = arrayOf(InputFilter.LengthFilter(80))
            setSingleLine(true)
            setText(savedInstanceState?.getString("draft"))
            isEnabled = !loadError
        }
        form.button("添加商品", ::addItem).isEnabled = !loadError
        list = content.card()
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Bundle 负责未提交的输入，SharedPreferences 负责重启后仍存在的清单。
        outState.putString("draft", input.text.toString())
    }

    private fun addItem() {
        val name = input.text.toString().trim()
        if (name.isEmpty()) {
            input.error = "请输入商品名称"
            return
        }
        save(items + Item(UUID.randomUUID().toString(), name, false))
        input.text.clear()
        render()
    }

    private fun render() {
        list.removeAllViews()
        val summary = list.heading(summaryText())
        when {
            loadError -> list.text("无法读取本地清单。")
            items.isEmpty() -> list.text("清单为空，先添加一项商品。")
        }
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            list.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            val check = CheckBox(this).apply {
                text = item.name
                isChecked = item.checked
                minHeight = dp(48)
                showChecked(item.checked)
            }
            row.addView(check, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            check.setOnCheckedChangeListener { _, checked ->
                // 用 ID 找记录。同名商品各自勾选，位置变化也不会改错记录。
                save(items.map { if (it.id == item.id) it.copy(checked = checked) else it })
                check.showChecked(checked)
                summary.text = summaryText()
            }
            val delete = Button(this).apply {
                text = "删除"
                contentDescription = "删除${item.name}"
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("从清单删除「${item.name}」？")
                        .setPositiveButton("删除") { _, _ ->
                            save(items.filter { it.id != item.id })
                            render()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
            row.addView(delete, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
    }

    private fun summaryText() = "待购 ${items.count { !it.checked }} · 共 ${items.size} 项"

    private fun CheckBox.showChecked(checked: Boolean) {
        paintFlags = if (checked) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

    private fun readItems(): List<Item> {
        val array = JSONArray(prefs.getString("items", "[]") ?: "[]")
        return List(array.length()) { index ->
            val value = array.getJSONObject(index)
            Item(value.getString("id"), value.getString("name"), value.getBoolean("checked"))
        }
    }

    private fun save(newItems: List<Item>) {
        // SharedPreferences 只能保存基本类型。把整个小清单编码为一个 JSON 字符串。
        val array = JSONArray()
        newItems.forEach {
            array.put(JSONObject().put("id", it.id).put("name", it.name).put("checked", it.checked))
        }
        prefs.edit().putString("items", array.toString()).apply()
        items = newItems
    }
}
