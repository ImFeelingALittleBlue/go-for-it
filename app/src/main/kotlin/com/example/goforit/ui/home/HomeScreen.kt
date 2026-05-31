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
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.OnCircleAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

val OrangeAccent = Color(0xFFD4822A)    // 設計稿主色
val TextGray     = Color(0xFF888888)
val ChipBg       = Color(0xFFF0EDE8)

@Composable
fun HomeScreen() {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }

    // 讀取 CSV 裡的古蹟資料（只在第一次組畫面時讀一次）
    val heritages = remember { HeritageRepository.loadHeritages(context) }

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
    var locationGranted by remember { mutableStateOf(false) }
    rememberLocationPermission { locationGranted = true }

    // 目前被點選的古蹟（null = 沒有點任何古蹟，不顯示詳情卡）
    var selected by remember { mutableStateOf<Heritage?>(null) }

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
                            // 樣式載入完成後，把所有古蹟畫成磚紅色圓點
                            // 點到圓點時，把 selected 設成對應古蹟 → 彈出詳情卡
                            addHeritageMarkers(mapView, heritages) { selected = it }
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

    // ── 古蹟詳情卡：selected 有值才顯示 ──────────────────────────────────────
    selected?.let { heritage ->
        HeritageDetailSheet(heritage = heritage, onDismiss = { selected = null })
    }
}

// 在地圖上把每筆古蹟畫成一個圓點（待修復的古蹟）
// onMarkerClick：點到某個圓點時，把對應的 Heritage 傳回去
private fun addHeritageMarkers(
    mapView: MapView,
    heritages: List<Heritage>,
    onMarkerClick: (Heritage) -> Unit
) {
    // CircleAnnotationManager：MapView 上負責管理一堆圓點標記的工具
    val manager = mapView.annotations.createCircleAnnotationManager()
    val options = heritages.map { h ->
        CircleAnnotationOptions()
            .withPoint(Point.fromLngLat(h.lng, h.lat))  // 圓點位置
            .withCircleRadius(8.0)                        // 半徑（大一點比較好點）
            .withCircleColor("#B5651D")                   // 磚紅色
            .withCircleStrokeWidth(2.0)                   // 白色外框
            .withCircleStrokeColor("#FFFFFF")
    }

    // create 回傳的圓點清單，順序跟 heritages 一樣 → 用 id 對應回古蹟
    val created = manager.create(options)
    val byId = created.zip(heritages).associate { (annotation, h) -> annotation.id to h }

    // 加上點擊監聽：點到圓點 → 找出對應古蹟 → 回傳；return true 表示「已處理這次點擊」
    manager.addClickListener(
        OnCircleAnnotationClickListener { annotation ->
            byId[annotation.id]?.let(onMarkerClick)
            true
        }
    )
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
