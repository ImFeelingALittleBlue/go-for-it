package com.example.goforit.ui.run

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.goforit.data.Heritage
import java.util.Locale

// 兩位說話者：HOST_A 主持人、HOST_B 文史達人（不同音調模擬兩人）
enum class PodcastSpeaker { HOST_A, HOST_B }

// 一行對話：說話者 + 台詞
data class DialogueLine(
    val speaker: PodcastSpeaker,
    val text: String
)

// Podcast 播放器：Android 內建 TTS，逐行切換說話者音調模擬兩人對話
// onPlaybackStarted  → 開始播放（更新 UI 狀態）
// onPlaybackCompleted → 播完（解鎖古蹟、更新 unlockedDuringRun）
// onPlaybackFailed   → 播放失敗（從 podcastRequestedDuringRun 移除，允許重試）
// onLineChanged      → 每換一行時回呼，null = 播放結束（更新對話卡 UI）
class DefaultPodcastPlayer(
    private val context: Context,
    private val onPlaybackStarted: (heritageId: Int) -> Unit,
    private val onPlaybackCompleted: (heritageId: Int) -> Unit,
    private val onPlaybackFailed: (heritageId: Int) -> Unit,
    private val onLineChanged: (DialogueLine?) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // 目前播放的古蹟與對話行列表
    private var currentHeritage: Heritage? = null
    private var pendingLines: List<DialogueLine> = emptyList()
    // TTS 初始化完成前若有 play 請求，先暫存
    private var pendingHeritage: Heritage? = null

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                // 優先用繁體中文，若裝置不支援則退回簡體中文
                val twLocale = Locale("zh", "TW")
                tts?.language = when (tts?.isLanguageAvailable(twLocale)) {
                    TextToSpeech.LANG_AVAILABLE,
                    TextToSpeech.LANG_COUNTRY_AVAILABLE -> twLocale
                    else -> Locale.CHINESE
                }
                setupListener()
                // 處理初始化前已排隊的播放請求
                pendingHeritage?.let { play(it) }
                pendingHeritage = null
            } else {
                mainHandler.post { pendingHeritage?.let { onPlaybackFailed(it.id) } }
            }
        }
    }

    // 監聽每行播放的開始、結束、錯誤事件
    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.toIntOrNull() ?: return
                // 切換 UI 顯示的對話行（需回到主執行緒）
                mainHandler.post { onLineChanged(pendingLines.getOrNull(idx)) }
            }
            override fun onDone(utteranceId: String?) {
                val next = (utteranceId?.toIntOrNull() ?: return) + 1
                if (next < pendingLines.size) {
                    speakLine(next)           // 播下一行
                } else {
                    mainHandler.post {
                        onLineChanged(null)   // 全部播完，清除 UI
                        currentHeritage?.let { onPlaybackCompleted(it.id) }
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                mainHandler.post {
                    onLineChanged(null)
                    currentHeritage?.let { onPlaybackFailed(it.id) }
                }
            }
        })
    }

    // 播放指定古蹟的 Podcast（TTS 未就緒則排隊）
    fun play(heritage: Heritage) {
        if (!ttsReady) { pendingHeritage = heritage; return }
        currentHeritage = heritage
        pendingLines = generateDialogue(heritage)
        mainHandler.post { onPlaybackStarted(heritage.id) }
        speakLine(0)
    }

    // 播放第 idx 行，並依說話者切換音調與語速
    private fun speakLine(idx: Int) {
        val line = pendingLines.getOrNull(idx) ?: return
        when (line.speaker) {
            PodcastSpeaker.HOST_A -> { tts?.setPitch(1.0f);  tts?.setSpeechRate(0.9f)  }
            PodcastSpeaker.HOST_B -> { tts?.setPitch(1.25f); tts?.setSpeechRate(0.82f) }
        }
        // QUEUE_FLUSH：立即開始這行，清掉任何殘留佇列
        tts?.speak(line.text, TextToSpeech.QUEUE_FLUSH, null, idx.toString())
    }

    fun stop() {
        tts?.stop()
        mainHandler.post { onLineChanged(null) }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

// 模板式兩人對話（之後可替換成 Firebase Functions + Claude API 生成的腳本）
private fun generateDialogue(heritage: Heritage): List<DialogueLine> {
    val name = heritage.name
    val year = heritage.year.ifBlank { "日治時期" }
    // 描述只取前 50 字，避免 TTS 一次唸太長
    val desc = heritage.description.take(50)
    return listOf(
        DialogueLine(PodcastSpeaker.HOST_A,
            "我們現在來到了「${name}」，這裡有什麼值得介紹的嗎？"),
        DialogueLine(PodcastSpeaker.HOST_B,
            "${name}大約建於${year}年。${desc}"),
        DialogueLine(PodcastSpeaker.HOST_A,
            "這樣豐富的歷史真的很令人感動！"),
        DialogueLine(PodcastSpeaker.HOST_B,
            "對，希望大家跑步的同時，也能感受臺南深厚的文化底蘊。")
    )
}
