package com.example.practice

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookActivity : PracticeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = page("图书详情")
        val info = layout.card()
        val title = info.heading("正在读取图书…")
        val id = intent.getStringExtra(BOOK_ID)
        val files = assets
        lifecycleScope.launch {
            val book = withContext(Dispatchers.IO) {
                readBooks(files)?.firstOrNull { it.id == id }
            }
            if (book == null) {
                title.text = "无法找到这本书"
            } else {
                title.text = book.title
                info.text("${book.author}  ·  ${book.category}  ·  ${book.year} 年")
                info.heading("内容简介")
                info.text(book.summary)
            }
        }
        layout.button("返回目录") { finish() }
    }
}
