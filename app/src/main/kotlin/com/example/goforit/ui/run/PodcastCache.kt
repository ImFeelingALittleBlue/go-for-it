package com.example.goforit.ui.run

// 跑步前預生成的 AI 腳本快取（singleton）
// RoutePreviewScreen 在「開始跑步」前寫入，PodcastPlayer 播放時直接讀取，不再等 API
object PodcastCache {
    private val scripts = mutableMapOf<Int, List<DialogueLine>>()

    fun store(heritageId: Int, lines: List<DialogueLine>) { scripts[heritageId] = lines }
    fun get(heritageId: Int): List<DialogueLine>? = scripts[heritageId]
    fun hasAll(ids: Collection<Int>): Boolean = ids.all { scripts.containsKey(it) }
    fun clear() = scripts.clear()
}
