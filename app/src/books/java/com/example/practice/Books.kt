package com.example.practice

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

internal data class Book(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val year: Int,
    val summary: String
)

internal const val BOOK_ID = "com.example.practice.books.ID"

internal fun readBooks(assets: AssetManager): List<Book>? = try {
    // 学习样本随 APK 打包。use 在读取结束或抛出异常时关闭文件流。
    val json = assets.open("books.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    val entries = JSONArray(json)
    List(entries.length()) { index ->
        val entry = entries.getJSONObject(index)
        Book(
            id = entry.getString("id"),
            title = entry.getString("title"),
            author = entry.getString("author"),
            category = entry.getString("category"),
            year = entry.getInt("year"),
            summary = entry.getString("summary")
        )
    }
} catch (_: IOException) {
    null
} catch (_: JSONException) {
    null
}
