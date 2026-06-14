package com.example.goforit.ui.run

import com.example.goforit.data.Heritage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// 呼叫 Claude API 生成兩人 Podcast 對話腳本
// minutes = 這個古蹟分配到的分鐘數（上游已除以古蹟總數）
// 失敗時拋出例外，由 PodcastPlayer 捕捉後 fallback 到模板對話
suspend fun generateDialogueFromApi(
    heritage: Heritage,
    minutes: Int,
    apiKey: String
): List<DialogueLine> = withContext(Dispatchers.IO) {

    val name = heritage.name
    val year = heritage.year.ifBlank { "年代久遠" }
    val desc = heritage.description.take(200)
    // 中文 TTS 朗讀速度約 200 字/分鐘
    val targetChars = minutes * 200

    val prompt = """
        你是台南古蹟 Podcast 的腳本作家，風格像《故事 FM》——不說廢話、直接切入故事核心，用第一人稱視角或懸念開場，讓聽眾立刻被吸引。

        請以「主持人（A）」和「文史達人（B）」兩人對話，介紹以下古蹟：
        名稱：${name}，年代：${year}
        簡介：${desc}

        寫作規則：
        - 繁體中文，總字數約 ${targetChars} 字（TTS 朗讀約 ${minutes} 分鐘）
        - 禁止使用開場白：不得有「歡迎」「大家好」「今天我們來到」等套話，直接從有趣的故事或懸念切入
        - 內容要有趣：加入具體的歷史八卦、冷知識、衝突事件、或讓人意外的細節
        - 對話要自然：像朋友聊天，A 可以提問或驚訝，B 說故事而不是唸資料
        - 每段台詞不超過 70 字，方便 TTS 斷句
        - 只回傳 JSON 陣列，不要 markdown 或其他說明

        格式：[{"speaker":"A","text":"..."},{"speaker":"B","text":"..."}]
    """.trimIndent()

    val requestBody = JSONObject().apply {
        put("model", "claude-haiku-4-5-20251001")
        put("max_tokens", (minutes * 700).coerceAtLeast(512))
        put("messages", JSONArray().put(
            JSONObject().put("role", "user").put("content", prompt)
        ))
    }

    val conn = (URL("https://api.anthropic.com/v1/messages").openConnection()
            as HttpURLConnection).apply {
        requestMethod = "POST"
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("x-api-key", apiKey)
        setRequestProperty("anthropic-version", "2023-06-01")
        connectTimeout = 30_000
        readTimeout = 60_000
        doOutput = true
    }
    conn.outputStream.bufferedWriter().use { it.write(requestBody.toString()) }

    if (conn.responseCode != 200) {
        val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
        throw Exception("API HTTP ${conn.responseCode}: $err")
    }

    val raw = conn.inputStream.bufferedReader().readText()
    val text = JSONObject(raw)
        .getJSONArray("content").getJSONObject(0)
        .getString("text").trim()
    // Claude 偶爾會包 ```json ... ```，先去掉
    val json = text
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```").trim()

    val arr = JSONArray(json)
    (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        val speaker = if (obj.getString("speaker") == "A") PodcastSpeaker.HOST_A
                      else PodcastSpeaker.HOST_B
        DialogueLine(speaker, obj.getString("text"))
    }
}
