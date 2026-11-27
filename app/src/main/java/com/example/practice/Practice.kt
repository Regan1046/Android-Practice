package com.example.practice

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/** 示例复用页面边距和控件样式，每个变体实现自己的业务逻辑。 */
abstract class PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val background = getColor(R.color.canvas)
        enableEdgeToEdge(SystemBarStyle.light(background, background), SystemBarStyle.light(background, background))
    }

    protected fun page(title: String, description: String? = null): LinearLayout {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(getColor(R.color.canvas))
        }
        // 给状态栏、导航栏和软键盘留出空间，同一套布局可在横竖屏滚动。
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            isFocusableInTouchMode = true
        }
        scroll.addView(content)
        setContentView(scroll)
        content.heading(title).textSize = 29f
        description?.let { content.text(it).setTextColor(getColor(R.color.muted)) }
        return content
    }

    protected fun LinearLayout.text(value: CharSequence): TextView = TextView(context).apply {
        text = value
        textSize = 16f
        setTextColor(getColor(R.color.ink))
        setLineSpacing(dp(3).toFloat(), 1f)
        this@text.addView(this, row())
    }

    protected fun LinearLayout.heading(value: CharSequence): TextView = text(value).apply {
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
        ViewCompat.setAccessibilityHeading(this, true)
    }

    protected fun LinearLayout.card(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(6), dp(14), dp(14))
        background = roundBox(getColor(R.color.surface), 18)
        // apply 内的 this 是新卡片，将卡片加入外层 LinearLayout。
        this@card.addView(this, row(14))
    }

    protected fun LinearLayout.field(hint: String, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(context).apply {
            this.hint = hint
            inputType = type
            setSingleLine((type and InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0)
            textSize = 16f
            minHeight = dp(52)
            setPadding(dp(14), dp(9), dp(14), dp(9))
            setTextColor(getColor(R.color.ink))
            setHintTextColor(getColor(R.color.muted))
            background = roundBox(getColor(R.color.surface), 12).apply {
                setStroke(dp(1), getColor(R.color.rule))
            }
            this@field.addView(this, row())
        }

    protected fun LinearLayout.button(label: String, action: () -> Unit): Button = Button(context).apply {
        text = label
        isAllCaps = false
        minHeight = dp(48)
        val enabled = intArrayOf(android.R.attr.state_enabled)
        backgroundTintList = ColorStateList(
            arrayOf(enabled, intArrayOf()),
            intArrayOf(getColor(R.color.accent), getColor(R.color.rule))
        )
        setTextColor(ColorStateList(arrayOf(enabled, intArrayOf()), intArrayOf(Color.WHITE, getColor(R.color.muted))))
        setOnClickListener { action() }
        this@button.addView(this, row())
    }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun row(top: Int = 8): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(top) }

    private fun roundBox(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
}
