package com.example.practice

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.util.Locale

sealed interface Forecast {
    data object Idle : Forecast
    data object Loading : Forecast
    data class Ready(val data: Weather, val demo: Boolean) : Forecast
    data class Error(val text: String) : Forecast
}

data class WeatherState(val city: City, val forecast: Forecast = Forecast.Idle)

class WeatherVm(private val saved: SavedStateHandle) : ViewModel() {
    private val state = MutableStateFlow(
        WeatherState(City.entries.firstOrNull { it.name == saved.get<String>("city") } ?: City.SHANGHAI)
    )
    val ui = state.asStateFlow()
    private var request: Job? = null

    fun select(city: City) {
        if (city == state.value.city) return
        request?.cancel()
        saved["city"] = city.name
        state.value = WeatherState(city)
    }

    fun showDemo() {
        request?.cancel()
        val city = state.value.city
        state.value = WeatherState(city, Forecast.Ready(city.demo, demo = true))
    }

    fun refresh() {
        request?.cancel()
        val city = state.value.city
        state.value = WeatherState(city, Forecast.Loading)
        request = viewModelScope.launch {
            val result = try {
                Forecast.Ready(WeatherApi.current(city), demo = false)
            } catch (_: IOException) {
                Forecast.Error("查询失败，请检查网络后重试，或查看离线示例。")
            } catch (_: JSONException) {
                Forecast.Error("天气数据格式不正确，请稍后重试。")
            }
            // 切换城市或示例模式会取消旧请求，避免旧结果覆盖当前界面。
            ensureActive()
            state.value = WeatherState(city, result)
        }
    }
}

class MainActivity : PracticeActivity() {
    private lateinit var model: WeatherVm
    private lateinit var query: Button
    private lateinit var cityTitle: TextView
    private lateinit var status: TextView
    private lateinit var temperature: TextView
    private lateinit var details: TextView
    private lateinit var credits: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[WeatherVm::class.java]
        val layout = page("城市天气")
        val selector = layout.card()
        selector.heading("城市")
        val spinner = Spinner(this).apply {
            contentDescription = "选择城市"
            minimumHeight = dp(48)
            adapter = ArrayAdapter(
                this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                City.entries.map { it.label }
            )
            setSelection(model.ui.value.city.ordinal)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    model.select(City.entries[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        selector.addView(spinner, LinearLayout.LayoutParams(-1, -2))
        query = selector.button("查询天气") { model.refresh() }
        selector.button("查看离线示例") { model.showDemo() }

        val report = layout.card()
        cityTitle = report.heading("")
        status = report.text("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        temperature = report.text("").apply { textSize = 38f }
        details = report.text("")
        credits = report.text(
            Html.fromHtml(
                """天气数据 <a href="https://open-meteo.com/">Open-Meteo</a> · <a href="https://creativecommons.org/licenses/by/4.0/">CC BY 4.0</a> · 天气描述已翻译""",
                Html.FROM_HTML_MODE_LEGACY
            )
        ).apply { movementMethod = LinkMovementMethod.getInstance() }
        render(model.ui.value)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { model.ui.collect(::render) }
        }
    }

    private fun render(state: WeatherState) {
        cityTitle.text = state.city.label
        val forecast = state.forecast
        query.isEnabled = forecast != Forecast.Loading
        temperature.isVisible = forecast is Forecast.Ready
        details.isVisible = forecast is Forecast.Ready
        credits.isVisible = forecast is Forecast.Ready && !forecast.demo
        status.text = when (forecast) {
            Forecast.Idle -> "选择查询天气或查看离线示例"
            Forecast.Loading -> "正在查询…"
            is Forecast.Error -> forecast.text
            is Forecast.Ready -> if (forecast.demo) "离线示例 · 固定练习数据"
                else "天气模型 · 当地时间 ${forecast.data.time.replace('T', ' ')}"
        }
        if (forecast is Forecast.Ready) {
            val data = forecast.data
            temperature.text = String.format(Locale.ROOT, "%.1f °C", data.temperature)
            details.text = String.format(
                Locale.ROOT, "%s  ·  湿度 %d%%  ·  风速 %.1f km/h",
                data.summary, data.humidity, data.windSpeed
            )
        }
    }
}
