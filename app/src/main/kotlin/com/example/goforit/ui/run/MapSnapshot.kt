package com.example.goforit.ui.run

import android.graphics.Bitmap
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapView

// 截取 MapView 當前畫面（含底圖與已繪製的路線折線），用於分享圖片
// Mapbox snapshot API 在主執行緒回呼 onResult，可直接更新 Compose state
// 若截圖失敗（地圖尚未載入完成），退回純路線圖
@OptIn(MapboxExperimental::class)
fun captureMapSnapshot(
    mapView: MapView,
    onResult: (Bitmap) -> Unit
) {
    mapView.mapboxMap.snapshot { bitmap ->
        onResult(bitmap ?: createRouteShareBitmap(emptyList()))
    }
}
