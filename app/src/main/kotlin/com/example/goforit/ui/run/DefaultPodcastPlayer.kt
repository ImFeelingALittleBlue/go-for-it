package com.example.goforit.ui.run

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.goforit.data.Heritage
import java.util.Locale

class DefaultPodcastPlayer(
    context: Context,
    private val onPlaybackStarted: (Int) -> Unit,
    private val onPlaybackCompleted: (Int) -> Unit,
    private val onPlaybackFailed: (Int) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pending = ArrayDeque<Heritage>()
    private var isReady = false
    private var activeHeritageId: Int? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            val player = textToSpeech ?: return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                player.language = Locale.TAIWAN
                player.setSpeechRate(0.92f)
                player.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) {
                            val heritageId = utteranceId.toIntOrNull() ?: return
                            mainHandler.post { onPlaybackStarted(heritageId) }
                        }

                        override fun onDone(utteranceId: String) {
                            val heritageId = utteranceId.toIntOrNull()
                            mainHandler.post {
                                heritageId?.let(onPlaybackCompleted)
                                activeHeritageId = null
                                playNext()
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String) {
                            val heritageId = utteranceId.toIntOrNull()
                            mainHandler.post {
                                heritageId?.let(onPlaybackFailed)
                                activeHeritageId = null
                                playNext()
                            }
                        }
                    }
                )
                isReady = true
                playNext()
            }
        }
    }

    fun play(heritage: Heritage) {
        if (heritage.id == activeHeritageId || pending.any { it.id == heritage.id }) return
        pending.addLast(heritage)
        playNext()
    }

    fun stop() {
        pending.clear()
        activeHeritageId = null
        textToSpeech?.stop()
    }

    fun release() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun playNext() {
        if (!isReady || activeHeritageId != null || pending.isEmpty()) return
        val heritage = pending.removeFirst()
        activeHeritageId = heritage.id
        val narration = buildString {
            append("你現在經過的是")
            append(heritage.name)
            append("。")
            append(heritage.description)
        }
        val result = textToSpeech?.speak(
            narration,
            TextToSpeech.QUEUE_FLUSH,
            Bundle(),
            heritage.id.toString()
        )
        if (result == TextToSpeech.ERROR) {
            activeHeritageId = null
            onPlaybackFailed(heritage.id)
            playNext()
        }
    }
}
