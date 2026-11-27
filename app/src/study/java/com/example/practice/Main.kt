package com.example.practice

import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException

class MainActivity : PracticeActivity() {
    private val samples = listOf(
        StudyCard("Intent 的 extra 可以做什么？", "它把少量数据附在 Intent 上，接收页面可用对应类型的方法读取。"),
        StudyCard("什么时候用 SharedPreferences？", "当数据可表示为少量键值对时，例如保存上一次选择的单位。"),
        StudyCard("如何让 SQLite 表每天只留一条记录？", "把日期设为主键。同一天再次插入时，按冲突规则更新当天的记录。")
    )
    private var cards = samples
    private var index = 0
    private var revealed = false
    private var generated = false
    private lateinit var position: TextView
    private lateinit var face: TextView
    private lateinit var flip: Button
    private lateinit var topic: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreDeck(savedInstanceState)
        val body = page("复习卡片", "先试着回答，再翻面核对。")
        val deck = body.card()
        position = deck.text("")
        face = deck.heading("")
        face.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        flip = deck.button("显示答案") {
            revealed = !revealed
            renderCard()
        }
        deck.button("下一张") {
            index = (index + 1) % cards.size
            revealed = false
            renderCard()
        }
        val sample = deck.button("恢复离线示例") {
            cards = samples
            generated = false
            index = 0
            revealed = false
            renderCard()
        }
        renderCard()

        val form = body.card()
        form.heading("按主题生成卡片")
        form.text("联网后将主题发送给 DeepSeek，生成 3 张卡片。")
        topic = form.field("复习主题，例如 Activity 生命周期")
        topic.filters = arrayOf(InputFilter.LengthFilter(80))
        val key = form.field("DeepSeek API 密钥", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        key.filters = arrayOf(InputFilter.LengthFilter(160))
        // 禁止保存或自动填充密钥。提交时读取一次，随后清除输入框。
        key.isSaveEnabled = false
        if (Build.VERSION.SDK_INT >= 26) key.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        val status = form.text("")
        status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        lateinit var generate: Button
        generate = form.button("联网生成") {
            val subject = topic.text.toString().trim()
            val credential = key.text.toString().trim()
            when {
                subject.isBlank() -> topic.error = "请输入复习主题"
                credential.isEmpty() || credential.any { it !in '!'..'~' } -> key.error = "请输入有效的 API 密钥"
                else -> {
                    key.text.clear()
                    generate.isEnabled = false
                    sample.isEnabled = false
                    status.text = "正在生成卡片…"
                    // 生命周期协程在页面销毁时取消，网络请求本身在 IO 调度器执行。
                    lifecycleScope.launch {
                        try {
                            cards = CardApi.generate(subject, credential)
                            generated = true
                            index = 0
                            revealed = false
                            renderCard()
                            status.text = "已生成 ${cards.size} 张卡片。"
                        } catch (error: CardHttp) {
                            status.text = when (error.code) {
                                401, 403 -> "DeepSeek 拒绝了密钥或访问请求，请检查密钥。"
                                402 -> "DeepSeek 账户余额不足。"
                                429 -> "请求过多，请稍后再试。"
                                else -> "DeepSeek 暂时无法生成卡片，请稍后再试。"
                            }
                        } catch (_: SocketTimeoutException) {
                            status.text = "连接超时，请稍后再试。"
                        } catch (_: IOException) {
                            status.text = "无法连接 DeepSeek，请检查网络。"
                        } catch (_: JSONException) {
                            status.text = "回复格式有误，请尝试换个主题。"
                        } finally {
                            generate.isEnabled = true
                            sample.isEnabled = true
                        }
                    }
                }
            }
        }
        topic.setText(savedInstanceState?.getString("topic").orEmpty())
    }

    private fun renderCard() {
        val card = cards[index]
        val source = if (generated) "DeepSeek 生成" else "离线示例"
        position.text = "$source · 第 ${index + 1} / ${cards.size} 张"
        face.text = if (revealed) card.answer else card.question
        flip.text = if (revealed) "显示问题" else "显示答案"
    }

    private fun restoreDeck(state: Bundle?) {
        generated = state?.getBoolean("generated") ?: false
        if (generated) {
            val questions = state?.getStringArrayList("questions").orEmpty()
            val answers = state?.getStringArrayList("answers").orEmpty()
            val restored = questions.zip(answers, ::StudyCard)
            if (restored.isEmpty()) generated = false else cards = restored
        }
        index = (state?.getInt("index") ?: 0).coerceIn(cards.indices)
        revealed = state?.getBoolean("revealed") ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("topic", topic.text.toString())
        outState.putInt("index", index)
        outState.putBoolean("revealed", revealed)
        outState.putBoolean("generated", generated)
        if (generated) {
            outState.putStringArrayList("questions", ArrayList(cards.map { it.question }))
            outState.putStringArrayList("answers", ArrayList(cards.map { it.answer }))
        }
        super.onSaveInstanceState(outState)
    }
}
