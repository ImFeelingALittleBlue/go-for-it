package com.example.goforit.data

// 一筆古蹟資料，對應 metadata.csv 的一列
// data class：Kotlin 用來「裝資料」的類別，會自動產生 equals、toString 等
data class Heritage(
    val id: Int,
    val name: String,         // title 名稱
    val year: String,         // date 年代（例 1908）
    val description: String,  // 描述
    val lat: Double,          // 緯度
    val lng: Double,          // 經度
    val photoFile: String     // 老照片檔名（修復後解鎖顯示，例 001_1500001_大北門.png）
)
