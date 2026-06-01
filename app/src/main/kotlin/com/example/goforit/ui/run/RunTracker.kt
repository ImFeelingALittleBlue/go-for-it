package com.example.goforit.ui.run

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import com.mapbox.geojson.Point

// 負責收集跑步中的 GPS 軌跡點
// 使用 Android 內建 LocationManager，不需要額外依賴
class RunTracker(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // 已收集的軌跡點清單
    // mutableStateListOf：Compose 能觀察到清單新增 → 地圖折線自動更新
    val points = mutableStateListOf<Point>()

    // 每收到一個 GPS 位置就加進清單
    private val listener = LocationListener { location: Location ->
        points.add(Point.fromLngLat(location.longitude, location.latitude))
    }

    // 開始追蹤（呼叫前須確認已有 GPS 權限）
    // minTime = 3 秒、minDistance = 5 公尺：跑步精度夠用且省電
    @SuppressLint("MissingPermission")
    fun start() {
        points.clear()  // 每次開始都從空軌跡出發
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            3_000L,               // 最短更新間隔（毫秒）
            5f,                   // 最短移動距離（公尺）
            listener,
            Looper.getMainLooper()  // 在主執行緒回呼，Compose state 才能直接修改
        )
    }

    // 停止追蹤，移除 GPS 監聽避免耗電
    fun stop() {
        locationManager.removeUpdates(listener)
    }
}
