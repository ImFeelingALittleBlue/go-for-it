package com.example.goforit.ui.run

import android.content.ContentValues
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.goforit.data.Heritage
import java.io.File
import java.util.Locale

// 把路線所有古蹟的兩人對話，用 TTS 逐行合成 WAV → 串接 → 存至 Downloads
// onProgress(current, total)：回呼主執行緒更新進度 UI
// onComplete(success)：完成或失敗後回呼主執行緒
fun exportPodcastToWav(
    context: Context,
    routeName: String,
    heritages: List<Heritage>,
    minutes: Int = 8,
    onProgress: (Int, Int) -> Unit,
    onComplete: (Boolean) -> Unit
) {
    val mainHandler = Handler(Looper.getMainLooper())
    // 優先用快取（與跑步中播放的相同腳本），沒有快取才用模板
    val minPerStop = (minutes / heritages.size.coerceAtLeast(1)).coerceAtLeast(1)
    val lines = heritages.flatMap { h ->
        PodcastCache.get(h.id) ?: generateDialogue(h, minPerStop)
    }
    if (lines.isEmpty()) { mainHandler.post { onComplete(false) }; return }

    val tempDir   = context.cacheDir
    val tempFiles = MutableList(lines.size) { File(tempDir, "pod_line_$it.wav") }
    var currentIdx = 0
    var ttsRef: TextToSpeech? = null

    // 失敗：清暫存檔、釋放 TTS、通知 UI
    fun fail() {
        ttsRef?.shutdown(); ttsRef = null
        tempFiles.forEach { it.delete() }
        mainHandler.post { onComplete(false) }
    }

    // 合成第 currentIdx 行，完成後遞迴呼叫下一行
    fun synthesizeNext(tts: TextToSpeech) {
        if (currentIdx >= lines.size) {
            // 所有行合成完畢 → 串接 WAV → 存入 Downloads
            val merged = mergeWavFiles(tempFiles)
            tempFiles.forEach { it.delete() }
            ttsRef?.shutdown(); ttsRef = null
            if (merged == null) { mainHandler.post { onComplete(false) }; return }
            val saved = saveWavToDownloads(context, routeName, merged)
            mainHandler.post { onComplete(saved) }
            return
        }
        val line = lines[currentIdx]
        // 依說話者切換音調（模擬兩人聲音）
        when (line.speaker) {
            PodcastSpeaker.HOST_A -> { tts.setPitch(1.0f);  tts.setSpeechRate(0.9f)  }
            PodcastSpeaker.HOST_B -> { tts.setPitch(1.25f); tts.setSpeechRate(0.82f) }
        }
        mainHandler.post { onProgress(currentIdx + 1, lines.size) }
        tts.synthesizeToFile(line.text, null, tempFiles[currentIdx], currentIdx.toString())
    }

    // 初始化 TTS（非同步，初始化完成後開始合成）
    ttsRef = TextToSpeech(context) { status ->
        val tts = ttsRef ?: return@TextToSpeech
        if (status != TextToSpeech.SUCCESS) { fail(); return@TextToSpeech }
        val twLocale = Locale("zh", "TW")
        tts.language = when (tts.isLanguageAvailable(twLocale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE -> twLocale
            else -> Locale.CHINESE
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?)  { currentIdx++; synthesizeNext(tts) }
            override fun onError(id: String?) { fail() }
        })
        synthesizeNext(tts)
    }
}

// 把多個 WAV 的 PCM 資料串接成單一 WAV
// 原理：WAV 標頭固定 44 bytes，其餘是 PCM 資料，直接拼接即可
private fun mergeWavFiles(files: List<File>): ByteArray? {
    val valid = files.filter { it.exists() && it.length() > 44 }
    if (valid.isEmpty()) return null
    return try {
        val header    = valid[0].readBytes().copyOf(44)   // 沿用第一個檔的格式標頭
        val dataParts = valid.map { it.readBytes().drop(44).toByteArray() }
        val totalData = dataParts.sumOf { it.size }
        val result    = ByteArray(44 + totalData)
        System.arraycopy(header, 0, result, 0, 44)
        writeInt32LE(result, 4,  36 + totalData)   // 更新 RIFF chunk size
        writeInt32LE(result, 40, totalData)         // 更新 data chunk size
        var pos = 44
        dataParts.forEach { part ->
            System.arraycopy(part, 0, result, pos, part.size); pos += part.size
        }
        result
    } catch (_: Exception) { null }
}

// 把 32 位元整數以 little-endian 寫入 ByteArray（WAV 格式規定）
private fun writeInt32LE(buf: ByteArray, offset: Int, value: Int) {
    buf[offset    ] = (value         and 0xFF).toByte()
    buf[offset + 1] = (value shr  8  and 0xFF).toByte()
    buf[offset + 2] = (value shr 16  and 0xFF).toByte()
    buf[offset + 3] = (value shr 24  and 0xFF).toByte()
}

// 把合併好的 WAV byte[] 存到 Downloads（不需要寫入權限，Android 10+）
private fun saveWavToDownloads(context: Context, routeName: String, wav: ByteArray): Boolean {
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME,
                "GoForIt_Podcast_${System.currentTimeMillis()}.wav")
            put(MediaStore.Downloads.MIME_TYPE, "audio/wav")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver
            .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { it.write(wav) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)
        true
    } catch (_: Exception) { false }
}
