package com.example.practice

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Weather(
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val code: Int,
    val time: String = ""
) {
    val summary: String
        get() = when (code) {
            0 -> "晴"
            1 -> "大致晴朗"
            2 -> "局部多云"
            3 -> "阴"
            45, 48 -> "雾"
            51, 53, 55 -> "毛毛雨"
            56, 57 -> "冻毛毛雨"
            61, 63, 65 -> "雨"
            66, 67 -> "冻雨"
            71, 73, 75, 77 -> "雪"
            80, 81, 82 -> "阵雨"
            85, 86 -> "阵雪"
            95 -> "雷雨"
            96, 99 -> "雷雨伴冰雹"
            else -> "天气代码 $code"
        }
}

enum class City(val label: String, val latitude: Double, val longitude: Double, val demo: Weather) {
    SHANGHAI("上海", 31.23, 121.47, Weather(19.2, 62, 11.4, 2)),
    BEIJING("北京", 39.90, 116.41, Weather(8.0, 37, 14.5, 0)),
    GUANGZHOU("广州", 23.13, 113.26, Weather(25.8, 75, 9.1, 61)),
    CHENGDU("成都", 30.57, 104.07, Weather(17.6, 69, 6.3, 3))
}

internal object WeatherApi {
    suspend fun current(city: City): Weather = withContext(Dispatchers.IO) {
        // 只请求预设城市的坐标和要显示的字段。auto 让时间随所选城市变化。
        val address = Uri.parse("https://api.open-meteo.com/v1/forecast").buildUpon()
            .appendQueryParameter("latitude", city.latitude.toString())
            .appendQueryParameter("longitude", city.longitude.toString())
            .appendQueryParameter("current", "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
            .appendQueryParameter("temperature_unit", "celsius")
            .appendQueryParameter("wind_speed_unit", "kmh")
            .appendQueryParameter("timezone", "auto")
            .appendQueryParameter("forecast_days", "1")
            .build()
        val connection = (URL(address.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) throw IOException("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            val temperature = current.getDouble("temperature_2m")
            val humidity = current.getInt("relative_humidity_2m")
            val wind = current.getDouble("wind_speed_10m")
            if (!temperature.isFinite() || humidity !in 0..100 || !wind.isFinite() || wind < 0) {
                throw JSONException("Invalid weather values")
            }
            Weather(
                temperature, humidity, wind, current.getInt("weather_code"),
                current.getString("time")
            )
        } finally {
            connection.disconnect()
        }
    }
}
