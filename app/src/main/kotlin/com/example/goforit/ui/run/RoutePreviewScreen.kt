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
import com.example.goforit.BuildConfig
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
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
// 路線預覽畫面：顯示路線地圖 + AI Podcast 生成入口（選配），可直接按「開始跑步」
@Composable
fun RoutePreviewScreen(
    points: List<Point>,
    distanceKm: Float,
    planHeritages: List<Heritage> = emptyList(),  // 規劃路線模式傳入；GPX 模式為空
    onBack: () -> Unit,
    onStartRun: (Int) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    val allHeritages   = remember { HeritageRepository.loadHeritages(context) }

    var storyState     by remember { mutableStateOf(StoryState.IDLE) }
    var podcastMinutes by remember { mutableIntStateOf(8) }

    // 要生成腳本的古蹟清單：規劃模式直接用，GPX 模式從路線 200m 範圍推算
    val heritagesForGen = remember(points, planHeritages) {
        if (planHeritages.isNotEmpty()) planHeritages
        else heritagesOnRoute(points, allHeritages).map { it.first }
    }

    // 「生成路線故事」卡片按鈕：實際 call API 預生成，存入 PodcastCache
    LaunchedEffect(storyState) {
        if (storyState != StoryState.GENERATING) return@LaunchedEffect
        PodcastCache.clear()
        val apiKey    = BuildConfig.ANTHROPIC_API_KEY
        val minPerStop = (podcastMinutes / heritagesForGen.size.coerceAtLeast(1)).coerceAtLeast(1)
        heritagesForGen.forEach { h ->
            val lines = if (apiKey.isNotBlank()) {
                try { generateDialogueFromApi(h, minPerStop, apiKey) }
                catch (_: Exception) { generateDialogue(h, minPerStop) }
            } else { generateDialogue(h, minPerStop) }
            PodcastCache.store(h.id, lines)
        }
        storyState = StoryState.DONE
    }

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
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 8.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Text("路線預覽", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center))
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
                                CameraOptions.Builder().center(center).zoom(13.0).build())
                            if (points.size >= 2)
                                annotations.createPolylineAnnotationManager().create(
                                    PolylineAnnotationOptions().withPoints(points)
                                        .withLineColor("#D4822A").withLineWidth(4.0))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(16.dp), color = Color(0xFF5C3D1E)) {
                Text("距離 ${"%.1f".format(distanceKm)}km", color = Color.White,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        // ── 可捲動下半部 ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            StoryCard(
                state           = storyState,
                minutes         = podcastMinutes,
                heritageCount   = heritagesForGen.size,
                onMinutesChange = { podcastMinutes = it },
                onGenerate      = { storyState = StoryState.GENERATING },
                onCancel        = { storyState = StoryState.IDLE }
            )
            Spacer(Modifier.height(8.dp))
            NearbyHeritageSection(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }

        // ── 底部按鈕 ───────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp), enabled = false) {
                Text("先儲存", fontSize = 14.sp)
            }
            Button(
                onClick = { onStartRun(podcastMinutes) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text("開始跑步", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// AI 故事生成卡片
@Composable
private fun StoryCard(
    state: StoryState, minutes: Int, heritageCount: Int,
    onMinutesChange: (Int) -> Unit, onGenerate: () -> Unit, onCancel: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), color = Color.White) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("AI生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A),
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            when (state) {
                StoryState.IDLE -> {
                    Text("預先生成 Podcast 腳本", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("共 $heritageCount 個古蹟。預先生成後開始跑步可即刻播放；也可直接跑步，到達古蹟 40m 內自動生成。",
                        fontSize = 13.sp, color = Color(0xFF888888), lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    PodcastMinutesInput(minutes = minutes, onMinutesChange = onMinutesChange)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))) {
                        Text("預先生成腳本", color = Color.White, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
                StoryState.GENERATING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp, color = OrangeAccent)
                        Spacer(Modifier.width(10.dp))
                        Text("AI 腳本生成中…", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("正在為 $heritageCount 個古蹟生成 Podcast 腳本，完成後直接開始跑步即可播放",
                        fontSize = 13.sp, color = Color(0xFF888888))
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                        Text("取消", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                }
                StoryState.DONE -> {
                    Text("腳本生成完成 ✓", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("按下「開始跑步」即刻播放，無需等待",
                        fontSize = 13.sp, color = Color(0xFF888888))
                }
            }
        }
    }
}
