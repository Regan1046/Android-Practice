package com.example.practice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal data class StudyCard(val question: String, val answer: String)
internal class CardHttp(val code: Int) : IOException()

internal object CardApi {
    suspend fun generate(topic: String, key: String): List<StudyCard> = withContext(Dispatchers.IO) {
        val connection = URL("https://api.deepseek.com/chat/completions").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val payload = request(topic).toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val code = connection.responseCode
            if (code !in 200..299) throw CardHttp(code)
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun request(topic: String): JSONObject {
        // system 约定 JSON 格式，user 仅传题目主题。密钥仅用于 HTTPS 请求头。
        val instruction = """
            为用户提供的主题制作 3 张简体中文知识复习卡。每张包括短问题和不超过 100 字的答案。
            只输出 JSON 对象，例如 {"cards":[{"question":"问题","answer":"答案"}]}，实际返回 3 条。
        """.trimIndent()
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", instruction))
            .put(JSONObject().put("role", "user").put("content", topic))
        return JSONObject()
            .put("model", "deepseek-flash")
            .put("stream", false)
            .put("max_tokens", 1000)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("response_format", JSONObject().put("type", "json_object"))
            .put("messages", messages)
    }

    private fun parse(response: String): List<StudyCard> {
        // 先解析 DeepSeek 的响应信封，再解析由模型生成的卡片 JSON。
        val choice = JSONObject(response).getJSONArray("choices").getJSONObject(0)
        val content = choice.getJSONObject("message").opt("content") as? String
        if (choice.optString("finish_reason") != "stop" || content.isNullOrBlank()) {
            throw JSONException("Incomplete reply")
        }
        val deck = JSONObject(content).getJSONArray("cards")
        if (deck.length() != 3) throw JSONException("Expected three cards")
        return List(deck.length()) { index ->
            val item = deck.getJSONObject(index)
            val question = (item.opt("question") as? String)?.trim().orEmpty()
            val answer = (item.opt("answer") as? String)?.trim().orEmpty()
            if (question.isEmpty() || question.length > 200 || answer.isEmpty() || answer.length > 600) {
                throw JSONException("Invalid card")
            }
            StudyCard(question, answer)
        }
    }
}
