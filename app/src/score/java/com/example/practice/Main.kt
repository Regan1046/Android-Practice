package com.example.practice

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

enum class Team(val label: String, val stateKey: String) {
    HOME("主队", "home"),
    AWAY("客队", "away")
}

class ScoreVm(private val state: SavedStateHandle) : ViewModel() {
    // SavedStateHandle 保存轻量分数，旋转或系统重建页面时仍可恢复。
    val homeScore = state.getStateFlow(Team.HOME.stateKey, 0)
    val awayScore = state.getStateFlow(Team.AWAY.stateKey, 0)

    fun adjust(team: Team, delta: Int) {
        val score = state.get<Int>(team.stateKey) ?: 0
        state[team.stateKey] = (score + delta).coerceIn(0, 999)
    }

    fun reset() {
        state[Team.HOME.stateKey] = 0
        state[Team.AWAY.stateKey] = 0
    }
}

class MainActivity : PracticeActivity() {
    private lateinit var model: ScoreVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[ScoreVm::class.java]
        val layout = page("双队计分器")
        val home = layout.scoreCard(Team.HOME, model.homeScore.value)
        val away = layout.scoreCard(Team.AWAY, model.awayScore.value)
        layout.button("重置比分") { model.reset() }

        // 页面可见时才收集比分。并行收集两个流，返回页面后会收到最新值。
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.homeScore.collect { home.showScore(Team.HOME, it) } }
                launch { model.awayScore.collect { away.showScore(Team.AWAY, it) } }
            }
        }
    }

    private fun LinearLayout.scoreCard(team: Team, initial: Int): TextView {
        val card = card()
        card.heading(team.label)
        val score = card.text("").apply {
            textSize = 48f
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            showScore(team, initial)
        }
        val actions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
        card.addView(actions, LinearLayout.LayoutParams(-1, -2))
        for (delta in listOf(1, 2, 3, -1)) {
            val label = if (delta > 0) "+$delta" else "−1"
            actions.button(label) { model.adjust(team, delta) }.apply {
                contentDescription =
                    if (delta > 0) "${team.label}加${delta}分" else "${team.label}减一分"
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply {
                    marginStart = dp(2)
                    marginEnd = dp(2)
                }
                setPadding(dp(2), paddingTop, dp(2), paddingBottom)
                minimumWidth = 0
                minWidth = 0
            }
        }
        return score
    }

    private fun TextView.showScore(team: Team, value: Int) {
        text = value.toString()
        contentDescription = "${team.label}${value}分"
    }
}
