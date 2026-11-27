package com.example.practice

import android.os.Bundle
import android.widget.TextView

class ScoreActivity : PracticeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val body = page("测验成绩")
        val card = body.card()
        // inflate 把 XML 里的结果视图加到卡片，再从 Intent 读取分数。
        val scoreView = layoutInflater.inflate(R.layout.score, card, false)
        card.addView(scoreView)
        scoreView.findViewById<TextView>(R.id.scoreTotal).text =
            getString(R.string.quiz_result, intent.getIntExtra(CORRECT, 0), intent.getIntExtra(TOTAL, 0))
        card.button("重新开始") {
            setResult(RESULT_OK)
            finish()
        }
    }

    internal companion object {
        const val CORRECT = "quiz.correct"
        const val TOTAL = "quiz.total"
    }
}
