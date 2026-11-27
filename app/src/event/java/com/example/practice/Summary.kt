package com.example.practice

import android.os.Bundle
import android.widget.TextView

class SummaryActivity : PracticeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = page("报名摘要")
        val card = body.card()
        // Intent 的键由表单页和摘要页共享，XML 负责固定布局。
        val info = layoutInflater.inflate(R.layout.signup, card, false) as TextView
        info.text = getString(
            R.string.signup_info,
            intent.getStringExtra(NAME).orEmpty(),
            intent.getStringExtra(EMAIL).orEmpty(),
            intent.getStringExtra(EVENT).orEmpty(),
            intent.getStringExtra(DATE).orEmpty()
        )
        card.addView(info)
        card.button("返回修改") { finish() }
    }

    internal companion object {
        const val NAME = "event.name"
        const val EMAIL = "event.email"
        const val EVENT = "event.type"
        const val DATE = "event.date"
    }
}
