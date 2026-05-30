package com.example.goforit.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style

// 臺南市中心座標
private const val TAINAN_LNG = 120.2028
private const val TAINAN_LAT = 23.0000

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // remember：讓 MapView 在重新組合時不會重建，避免地圖閃爍
    val mapView = remember { MapView(context) }

    // DisposableEffect：跟著 lifecycleOwner 的生命週期啟動/暫停/銷毀地圖
    // 這樣切換 App 時地圖會正確暫停，不會繼續耗電
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)  { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)   { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner){ mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // AndroidView：在 Compose 裡嵌入傳統 View（MapView 不是 Compose 元件）
    AndroidView(
        factory = {
            mapView.apply {
                // 載入地圖樣式，載入完成後設定初始鏡頭對準臺南
                mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                    mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(TAINAN_LNG, TAINAN_LAT))
                            .zoom(13.0)
                            .build()
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
