package com.example.goforit.ui.run

import android.app.Activity
import android.graphics.Bitmap
import com.mapbox.maps.MapView

// MapView の現在の画面（底图＋路線折線）を Bitmap としてキャプチャする
// 内部では ShareUtils.kt の captureMapView（PixelCopy 方式）を使用
// PixelCopy は Android 8 以降対応で、OpenGL レンダリングの MapView にも有効
// 失敗した場合（古い端末・View 未 layout）は純路線图にフォールバック
fun captureMapSnapshot(
    mapView: MapView,
    onResult: (Bitmap) -> Unit
) {
    val activity = mapView.context as? Activity ?: run {
        onResult(createRouteShareBitmap(emptyList()))
        return
    }
    captureMapView(activity, mapView) { bitmap ->
        onResult(bitmap ?: createRouteShareBitmap(emptyList()))
    }
}
