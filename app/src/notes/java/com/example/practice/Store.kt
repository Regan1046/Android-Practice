package com.example.practice

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class Note(val id: String, val title: String, val body: String)

internal class NoteStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("notes", Context.MODE_PRIVATE)

    fun read(): List<Note> {
        val array = JSONArray(prefs.getString("entries", "[]") ?: "[]")
        return List(array.length()) { index ->
            val entry = array.getJSONObject(index)
            Note(entry.getString("id"), entry.getString("title"), entry.getString("body"))
        }
    }

    fun write(notes: List<Note>) {
        // 这份练习用一个 JSON 字符串保存笔记，并保留列表中的顺序。
        val array = JSONArray()
        notes.forEach {
            array.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body))
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
