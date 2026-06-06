package com.example.goforit.data

// 一筆跑步路線紀錄。gpxContent 保留原始 GPX，讓紀錄頁存的是「路線」本身。
data class RouteRecord(
    val docId: String = "",
    val name: String = "",
    val gpxFileName: String = "",
    val gpxContent: String = "",
    val distanceMeters: Double = 0.0,
    val pointCount: Int = 0,
    val startedAt: Long = 0L,
    val recordedAt: Long = 0L,
    val liked: Boolean = false   // 使用者在紀錄頁按愛心 → 出現在已儲存路線
)
