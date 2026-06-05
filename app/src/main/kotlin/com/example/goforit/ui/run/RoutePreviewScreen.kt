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

// 故事生成的三個狀態
private enum class StoryState { IDLE, GENERATING, DONE }

// 路線預覽畫面：上傳 GPX 後顯示路線地圖，並提供 AI 故事生成入口
@Composable
fun RoutePreviewScreen(
    points: List<Point>,
    distanceKm: Float,
    onBack: () -> Unit,
    onStartRun: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    var storyState     by remember { mutableStateOf(StoryState.IDLE) }

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

        // ── 地圖 + 距離徽章 ────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            val center = if (points.isNotEmpty()) points[points.size / 2]
                                         else Point.fromLngLat(120.2028, 23.0000)
                            mapboxMap.setCamera(
                                CameraOptions.Builder().center(center).zoom(13.0).build()
                            )
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
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF5C3D1E)
            ) {
                Text(
                    "距離 ${"%.1f".format(distanceKm)}km",
                    color = Color.White, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // ── 可捲動下半部 ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            StoryCard(
                state = storyState,
                onGenerate = { storyState = StoryState.GENERATING },
                onCancel   = { storyState = StoryState.IDLE }
            )
            Spacer(Modifier.height(8.dp))
            NearbyHeritageSection(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        // ── 底部按鈕 ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { },
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

// AI 故事生成卡片，依 state 顯示不同內容
@Composable
private fun StoryCard(
    state: StoryState,
    onGenerate: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A),
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            when (state) {
                StoryState.IDLE -> {
                    Text("為上傳路線生成故事？",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("AI 將根據路線上的古蹟，生成一段語音故事導覽",
                        fontSize = 13.sp, color = Color(0xFF888888), lineHeight = 20.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
                    ) {
                        Text("生成路線故事", color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }

                StoryState.GENERATING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = OrangeAccent
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("生成路線故事中...",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("正在根據路線上的古蹟生成導覽故事，請稍候",
                        fontSize = 13.sp, color = Color(0xFF888888))
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("取消", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                }

                StoryState.DONE -> {
                    // Firebase Functions + Claude API 接入後填入真實內容
                    Text("語音導覽生成完成",
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("點選下方「開始跑步」即可邊跑邊聽導覽故事",
                        fontSize = 13.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}
