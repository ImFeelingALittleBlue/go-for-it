package com.example.goforit.ui.run

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.goforit.data.Heritage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// 兩位說話者：HOST_A 主持人、HOST_B 文史達人（不同音調模擬兩人）
enum class PodcastSpeaker { HOST_A, HOST_B }

// 一行對話：說話者 + 台詞
data class DialogueLine(
    val speaker: PodcastSpeaker,
    val text: String
)

// Podcast 播放器：Android 內建 TTS，逐行切換說話者音調模擬兩人對話
// apiKey 不為空時呼叫 Claude API 生成台本；空字串則用模板 fallback
// onPlaybackStarted  → API 生成完畢、TTS 開始前觸發（更新 UI 狀態）
// onPlaybackCompleted → 播完（解鎖古蹟）
// onPlaybackFailed   → 失敗（API 或 TTS 錯誤）
// onLineChanged      → 每換一行時回呼，null = 播放結束
class DefaultPodcastPlayer(
    private val context: Context,
    private val apiKey: String = "",
    private val onPlaybackStarted: (heritageId: Int) -> Unit,
    private val onPlaybackCompleted: (heritageId: Int) -> Unit,
    private val onPlaybackFailed: (heritageId: Int) -> Unit,
    private val onLineChanged: (DialogueLine?) -> Unit = {}
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null

    private var currentHeritage: Heritage? = null
    private var pendingLines: List<DialogueLine> = emptyList()
    private var pendingHeritage: Heritage? = null
    private var pendingMinutes: Int = 8

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
                pendingHeritage?.let { play(it, pendingMinutes) }
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

    // 播放指定古蹟的 Podcast
    // 優先讀 PodcastCache（RoutePreviewScreen 已預生成）→ 有快取直接播，無需等 API
    // TTS 未就緒則排隊，等初始化完成後再觸發
    fun play(heritage: Heritage, minutes: Int = 8) {
        if (!ttsReady) { pendingHeritage = heritage; pendingMinutes = minutes; return }
        currentHeritage = heritage
        currentJob?.cancel()
        tts?.stop()                                      // 立刻停止舊 TTS，不等 API
        mainHandler.post { onLineChanged(null) }
        currentJob = scope.launch {
            val cached = PodcastCache.get(heritage.id)
            val lines = when {
                cached != null      -> cached            // 快取命中，直接用
                apiKey.isNotBlank() -> try {
                    generateDialogueFromApi(heritage, minutes, apiKey)
                } catch (_: Exception) { generateDialogue(heritage, minutes) }
                else                -> generateDialogue(heritage, minutes)
            }
            pendingLines = lines
            withContext(Dispatchers.Main) {
                onPlaybackStarted(heritage.id)
                speakLine(0)
            }
        }
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
        currentJob?.cancel()
        tts?.stop()
        mainHandler.post { onLineChanged(null) }
    }

    fun release() {
        currentJob?.cancel()
        scope.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

// 模板式兩人對話，minutes = 這個古蹟分配到的分鐘數
// 每行中文約 20 秒（TTS 速率 0.9），所以 1 分鐘 ≈ 3 行
// internal：讓同 package 的 ShareSheet / PodcastExporter 可以呼叫
internal fun generateDialogue(heritage: Heritage, minutes: Int = 8): List<DialogueLine> {
    val name = heritage.name
    val year = heritage.year.ifBlank { "日治時期" }
    val desc = heritage.description.take(60)
    val pool = listOf(
        DialogueLine(PodcastSpeaker.HOST_A, "你知道嗎，「${name}」背後藏著一段很多人不知道的故事。"),
        DialogueLine(PodcastSpeaker.HOST_B, "沒錯，這裡大約建於${year}。${desc}"),
        DialogueLine(PodcastSpeaker.HOST_A, "聽起來這裡有相當深厚的歷史積累，能再多介紹嗎？"),
        DialogueLine(PodcastSpeaker.HOST_B, "「${name}」歷經多個時代的更迭，每個時代都在這裡留下了獨特的印記，至今仍清晰可見。"),
        DialogueLine(PodcastSpeaker.HOST_A, "在建築上，這裡有什麼特色值得我們特別注意？"),
        DialogueLine(PodcastSpeaker.HOST_B, "從建築風格可以看出當時工匠的精湛技藝，也反映了那個年代的審美觀與生活方式，非常值得細細品味。"),
        DialogueLine(PodcastSpeaker.HOST_A, "確實，能夠保存到今日實屬不易。"),
        DialogueLine(PodcastSpeaker.HOST_B, "維護這些古蹟需要耗費大量人力與財力，我們應該珍惜並感謝這些默默付出的人。"),
        DialogueLine(PodcastSpeaker.HOST_A, "除了建築本身，「${name}」在文化上有什麼重要性嗎？"),
        DialogueLine(PodcastSpeaker.HOST_B, "「${name}」不只是一棟建築，它承載著當地居民幾代人的集體記憶與情感，是社區認同的重要象徵。"),
        DialogueLine(PodcastSpeaker.HOST_A, "您說得很對，古蹟的價值不只在於建築本身，更在於它所代表的文化意涵。"),
        DialogueLine(PodcastSpeaker.HOST_B, "透過這些古蹟，我們得以與過去的歷史產生連結，理解自己所站立的這片土地曾經發生過什麼故事。"),
        DialogueLine(PodcastSpeaker.HOST_A, "您有聽說過關於「${name}」的在地故事或傳說嗎？"),
        DialogueLine(PodcastSpeaker.HOST_B, "臺南是一座充滿故事的城市，每個角落都有自己的傳說。「${name}」也不例外，在地耆老口中流傳著許多動人的故事。"),
        DialogueLine(PodcastSpeaker.HOST_A, "這真的很迷人，讓人對臺南的歷史有更深的認識與感動。"),
        DialogueLine(PodcastSpeaker.HOST_B, "每次來到這裡，我都感受到一種穿越時空的奇妙感覺，彷彿能與幾百年前的古人對話，感受他們的喜怒哀樂。"),
        DialogueLine(PodcastSpeaker.HOST_A, "如果今天的聽眾想深入了解「${name}」，有什麼推薦的方式嗎？"),
        DialogueLine(PodcastSpeaker.HOST_B, "除了親身到訪，也可以查閱臺南市文化局的資料，或是參加在地導覽行程，能獲得更豐富完整的歷史脈絡。"),
        DialogueLine(PodcastSpeaker.HOST_A, "今天能夠親身走訪「${name}」，真的是很難得的體驗，謝謝您的精彩介紹。"),
        DialogueLine(PodcastSpeaker.HOST_B, "希望大家在跑步的同時，也能靜下心來感受臺南古蹟的魅力。保護文化遺產，從認識它開始，這也是我們設計這趟旅程的初衷。")
    )
    // 每行約 20 秒（TTS 0.9 速率），1 分鐘 ≈ 3 行；至少 2 行，最多取滿整個池子
    val linesCount = (minutes * 3).coerceIn(2, pool.size)
    return pool.take(linesCount)
}
