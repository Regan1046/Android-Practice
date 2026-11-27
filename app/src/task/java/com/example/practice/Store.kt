package com.example.practice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class Task(
    val id: String,
    val title: String,
    val due: String?,
    val done: Boolean
)

internal class TaskStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    fun read(): List<Task> {
        val array = JSONArray(prefs.getString("items", "[]").orEmpty())
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            Task(
                item.getString("id"),
                item.getString("title"),
                if (item.isNull("due")) null else item.getString("due"),
                item.getBoolean("done")
            )
        }
    }

    fun save(tasks: List<Task>) {
        // 截止日期使用 yyyy-MM-dd 字符串，换时区也不会改变选定的日历日期。
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().put("id", task.id)
                .put("title", task.title)
                .put("due", task.due ?: JSONObject.NULL)
                .put("done", task.done))
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}
