package com.example.goforit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.location

val OrangeAccent = Color(0xFFD4822A)    // 設計稿主色
val TextGray     = Color(0xFF888888)
val ChipBg       = Color(0xFFF0EDE8)

@Composable
fun HomeScreen() {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }

    // 地圖跟著 Activity 生命週期啟動 / 暫停 / 銷毀
    DisposableEffect(lifecycleOwner) {
        val obs = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)   { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)    { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    // 權限狀態：用 state 追蹤，這樣變更時 AndroidView 的 update 會被觸發
    var locationGranted by ㄒㄧㄢremember { mutableStateOf(false) }
    rememberLocationPermission { locationGranted = true }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // ── 搜尋列 ──────────────────────────────────────────────────────────
        HomeSearchBar()

        // ── 地圖（占畫面 55%）───────────────────────────────────────────────
        Box(modifier = Modifier.weight(0.55f)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(120.2028, 23.0000))
                                    .zoom(13.0).build()
                            )
                        }
                    }
                },
                // update：當 locationGranted 變成 true 時被呼叫，此時樣式已載入完成
                update = { mv ->
                    if (locationGranted) {
                        mv.location.updateSettings { enabled = true }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // 地圖底部的兩顆按鈕
            MapOverlayButtons(modifier = Modifier.align(Alignment.BottomCenter))
        }

        // ── 附近古蹟列表（占 45%）────────────────────────────────────────────
        NearbyHeritageSection(modifier = Modifier.weight(0.45f))
    }
}

@Composable
fun HomeSearchBar() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = ChipBg,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
            Spacer(Modifier.width(8.dp))
            Text("搜尋你的地圖", color = TextGray, fontSize = 14.sp)
        }
    }
}

@Composable
fun MapOverlayButtons(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MapPill("找不到你要的地點？")
        MapPill("開啟 Google Map")
    }
}

@Composable
fun MapPill(text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 4.dp) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = Color(0xFF1A1A1A)
        )
    }
}
