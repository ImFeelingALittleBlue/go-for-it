package com.example.goforit.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.mapbox.geojson.Point

// 跨頁面傳遞「再跑一次」的路線資料
// CollectionScreen 按再跑一次 → 存入此處 → RunScreen 觀察到後跳路線預覽
object PendingRunStore {
    private val _hasPending = mutableStateOf(false)
    val hasPending: State<Boolean> = _hasPending

    var points: List<Point> = emptyList()
        private set
    var gpxContent: String = ""
        private set
    var distanceKm: Float = 0f
        private set

    fun set(pts: List<Point>, gpx: String, dist: Float) {
        points = pts; gpxContent = gpx; distanceKm = dist
        _hasPending.value = true
    }

    fun clear() {
        points = emptyList(); gpxContent = ""; distanceKm = 0f
        _hasPending.value = false
    }
}
