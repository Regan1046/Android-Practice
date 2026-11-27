package com.example.practice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class MainActivity : PracticeActivity() {
    private var books: List<Book>? = null
    private lateinit var search: EditText
    private lateinit var sort: Spinner
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private val titleOrder = Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
    private val words = Regex("\\s+")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = page("离线图书目录")
        search = layout.field("搜索书名、作者或分类").apply {
            setText(savedInstanceState?.getString("query").orEmpty())
        }
        sort = Spinner(this).apply {
            contentDescription = "结果排序"
            minimumHeight = dp(48)
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("按年份从新到旧", "按书名排序")
            )
            setSelection(savedInstanceState?.getInt("sort") ?: 0)
        }
        layout.addView(sort, LinearLayout.LayoutParams(-1, -2))
        status = layout.text("正在读取目录…")
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(results, LinearLayout.LayoutParams(-1, -2))
        search.doAfterTextChanged { renderBooks() }
        sort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                renderBooks()

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val files = assets
        lifecycleScope.launch {
            books = withContext(Dispatchers.IO) { readBooks(files) }
            if (books == null) status.text = "无法读取图书目录" else renderBooks()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", search.text.toString())
        outState.putInt("sort", sort.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }

    private fun renderBooks() {
        val source = books ?: return
        val terms = search.text.toString().trim().split(words).filter(String::isNotEmpty)
        // 先筛选本地数据再排序。多个关键词都需要出现在同一本书的字段中。
        val matching = source.filter { book ->
            val searchable = "${book.title} ${book.author} ${book.category}"
            terms.all { searchable.contains(it, ignoreCase = true) }
        }
        val sorted = if (sort.selectedItemPosition == 1) {
            matching.sortedWith { left, right -> titleOrder.compare(left.title, right.title) }
        } else {
            matching.sortedByDescending { it.year }
        }
        status.text = if (sorted.isEmpty()) "没有找到匹配的图书" else "共 ${sorted.size} 本"
        results.removeAllViews()
        for (book in sorted) {
            val item = results.card()
            item.heading(book.title)
            item.text("${book.author}  ·  ${book.category}  ·  ${book.year} 年")
            item.button("查看详情") {
                // 只传稳定 ID，详情页可在任何一次重建后重新读取完整数据。
                startActivity(
                    Intent(this, BookActivity::class.java).putExtra(BOOK_ID, book.id)
                )
            }.contentDescription = "查看《${book.title}》详情"
        }
    }
}
