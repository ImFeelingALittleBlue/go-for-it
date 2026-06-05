package com.example.goforit.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.NearbyHeritageSection
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager

// 路線預覽畫面（對應設計稿「去探索3」）
// 顯示上傳 GPX 後的路線地圖、AI 語音導覽資訊、附近古蹟
@Composable
fun RoutePreviewScreen(
    points: List<Point>,       // GPX 解析出的軌跡點
    distanceKm: Float,         // 總距離
    onBack: () -> Unit,
    onStartRun: () -> Unit     // 開始跑步，切換到計時畫面
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val obs = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)   { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)    { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F0EB))) {

        // ── 頂部列 ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Text(
                "上傳路線生成",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── 地圖（固定高度）+ 距離徽章覆蓋 ─────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            // 路線中心點作為鏡頭目標
                            val center = if (points.isNotEmpty())
                                points[points.size / 2]
                            else
                                Point.fromLngLat(120.2028, 23.0000)
                            mapboxMap.setCamera(
                                CameraOptions.Builder().center(center).zoom(13.0).build()
                            )
                            // 畫出橘色路線折線
                            if (points.size >= 2) {
                                annotations.createPolylineAnnotationManager().create(
                                    PolylineAnnotationOptions()
                                        .withPoints(points)
                                        .withLineColor("#D4822A")
                                        .withLineWidth(4.0)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // 距離徽章
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF5C3D1E)
            ) {
                Text(
                    text = "距離 ${"%.1f".format(distanceKm)}km",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // ── 可捲動的下半部內容 ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // AI 語音導覽卡片（Firebase Functions + Claude API 尚未接入，顯示佔位符）
            PodcastPlaceholderCard()
            Spacer(Modifier.height(8.dp))
            // 附近古蹟列表（複用首頁的元件，後續可換成路線附近的篩選結果）
            NearbyHeritageSection(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        // ── 底部按鈕 ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // premium 功能：先儲存（目前停用）
            OutlinedButton(
                onClick = { /* TODO: 儲存路線功能 */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = false
            ) {
                Text("先儲存", fontSize = 14.sp)
            }
            Button(
                onClick = onStartRun,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("開始跑步", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// AI 語音導覽佔位符卡片（等 Firebase Functions + Claude API 接入後替換）
@Composable
private fun PodcastPlaceholderCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF5C3D1E)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A))
            Spacer(Modifier.height(6.dp))
            Text(
                "語音導覽生成中，上傳完成後自動產生…",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
