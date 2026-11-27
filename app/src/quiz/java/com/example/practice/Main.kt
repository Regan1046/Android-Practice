package com.example.practice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts

private data class Question(
    val prompt: String,
    val options: List<String>,
    val answer: Int,
    val reason: String
)

class MainActivity : PracticeActivity() {
    private val questions = listOf(
        Question("哪个 API 适合保存少量键值对？",
            listOf("SharedPreferences", "Canvas", "Intent"), 0,
            "SharedPreferences 适合存储少量数据，例如上次填写的身高、年龄或主题。"),
        Question("使用 SQLite，每天只能保存一条结果，应该怎样设计？",
            listOf("给结果编号", "把日期设置为唯一键", "用当前秒数作为主键"), 1,
            "日期设为唯一键后，同一天再写入时可更新已有记录。"),
        Question("从输入页把一个数值传给报告页，适合使用哪项？",
            listOf("RadioGroup", "ViewModelProvider", "Intent extra"), 2,
            "在 Intent 中用 putExtra 携带参数，报告页读取对应类型的值。")
    )
    private var index = 0
    private var chosen = -1
    private var correct = 0
    private var submitted = false

    // 结果页返回 RESULT_OK 代表重新开始，系统返回键会保留当前结果。
    private val openScore = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            index = 0
            chosen = -1
            correct = 0
            submitted = false
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = savedInstanceState?.getInt("index") ?: 0
        chosen = savedInstanceState?.getInt("chosen") ?: -1
        correct = savedInstanceState?.getInt("correct") ?: 0
        submitted = savedInstanceState?.getBoolean("submitted") ?: false
        render()
    }

    private fun render() {
        val body = page("Android 小测验")
        val card = body.card()
        val question = questions[index]
        card.text("第 ${index + 1} 题，共 ${questions.size} 题")
        card.heading(question.prompt)
        val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val optionIds = question.options.mapIndexed { optionIndex, optionText ->
            val radio = RadioButton(this).apply {
                id = View.generateViewId()
                text = optionText
                textSize = 16f
                minimumHeight = dp(48)
                isEnabled = !submitted
            }
            group.addView(radio)
            if (chosen == optionIndex) group.check(radio.id)
            radio.id
        }
        card.addView(group)
        val feedback = card.text(
            if (submitted) {
                val prefix = if (chosen == question.answer) "答对了。" else "正确答案：${question.options[question.answer]}。"
                "$prefix\n${question.reason}"
            } else " "
        )
        feedback.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        group.setOnCheckedChangeListener { _, checked ->
            chosen = optionIds.indexOf(checked)
            feedback.text = ""
        }

        val last = index == questions.lastIndex
        val label = when {
            !submitted -> "确认答案"
            last -> "查看成绩"
            else -> "下一题"
        }
        card.button(label) {
            when {
                chosen == -1 -> feedback.text = "请先选择一个答案。"
                !submitted -> {
                    // 锁定本题后才显示解析，防止重复提交时累计分数。
                    if (chosen == question.answer) correct++
                    submitted = true
                    render()
                }
                last -> openScore.launch(Intent(this, ScoreActivity::class.java)
                    .putExtra(ScoreActivity.CORRECT, correct)
                    .putExtra(ScoreActivity.TOTAL, questions.size))
                else -> {
                    index++
                    chosen = -1
                    submitted = false
                    render()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("index", index)
        outState.putInt("chosen", chosen)
        outState.putInt("correct", correct)
        outState.putBoolean("submitted", submitted)
        super.onSaveInstanceState(outState)
    }
}
