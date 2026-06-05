package com.example.goforit.ui.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RouteRepository
import com.example.goforit.data.SilverSaltStore
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.example.goforit.ui.home.TextGray
import com.example.goforit.ui.home.rememberLocationPermission
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// 去探索的三個子畫面
private sealed class RunNav {
    object Main        : RunNav()   // 地圖 + 選擇路線/直接跑步
    object SavedRoutes : RunNav()   // 已儲存路線清單
    data class RoutePreview(        // GPX 路線預覽
        val points: List<Point>,
        val distanceKm: Float
    ) : RunNav()
}

// 跑步的兩個階段
private enum class RunPhase { PRE_RUN, RUNNING }
private const val HERITAGE_UNLOCK_RADIUS_METERS = 40.0
private const val HERITAGE_UNLOCK_REWARD = 25

@Composable
fun RunScreen() {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    val tracker        = remember { RunTracker(context) }
    val heritages      = remember { HeritageRepository.loadHeritages(context) }

    var nav             by remember { mutableStateOf<RunNav>(RunNav.Main) }
    var phase           by remember { mutableStateOf(RunPhase.PRE_RUN) }
    var elapsedSeconds  by remember { mutableStateOf(0) }
    var locationGranted by remember { mutableStateOf(false) }
    var polylineManager    by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    var routeLineManager   by remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    val pointCount         = tracker.points.size   // 觀察 GPS 新點，觸發 update block
    val unlockedDuringRun  = remember { mutableStateListOf<Int>() }
    // 選擇路線後儲存 GPX 點位，供地圖顯示與停止後清空
    var selectedRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    // 剛解鎖的古蹟，用於顯示通知卡
    var notifHeritage       by remember { mutableStateOf<Heritage?>(null) }
    // 即時計算已跑距離（每新增一個 GPS 點就重算）
    val coveredKm = remember(pointCount) {
        if (tracker.points.size < 2) 0f
        else tracker.points.zipWithNext { a, b ->
            (distanceMeters(a.latitude(), a.longitude(), b.latitude(), b.longitude()) / 1000.0).toFloat()
        }.sum()
    }

    rememberLocationPermission { locationGranted = true }

    DisposableEffect(lifecycleOwner) {
        val obs = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)   { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)    { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); tracker.stop(); mapView.onDestroy() }
    }

    LaunchedEffect(phase) {
        if (phase == RunPhase.RUNNING) {
            while (true) { delay(1_000L); elapsedSeconds++ }
        }
    }

    LaunchedEffect(pointCount, phase) {
        if (phase != RunPhase.RUNNING || pointCount == 0) return@LaunchedEffect
        val latest = tracker.points.last()
        val restoredIds = RestorationRepository.records().map { it.heritageId }.toSet()
        heritages
            .filter { it.id !in restoredIds && it.id !in unlockedDuringRun }
            .firstOrNull { heritage ->
                distanceMeters(
                    latest.latitude(),
                    latest.longitude(),
                    heritage.lat,
                    heritage.lng
                ) <= HERITAGE_UNLOCK_RADIUS_METERS
            }
            ?.let { heritage ->
                unlockedDuringRun.add(heritage.id)
                RestorationRepository.add(heritage)
                SilverSaltStore.add(context, HERITAGE_UNLOCK_REWARD)
                notifHeritage = heritage   // 觸發底部通知卡
            }
    }

    // ── 子畫面路由 ────────────────────────────────────────────────────────
    when (val n = nav) {
        is RunNav.SavedRoutes -> {
            SavedRoutesScreen(
                onBack = { nav = RunNav.Main },
                onGpxLoaded = { pts, dist -> nav = RunNav.RoutePreview(pts, dist) }
            )
            return
        }
        is RunNav.RoutePreview -> {
            RoutePreviewScreen(
                points = n.points,
                distanceKm = n.distanceKm,
                onBack = { nav = RunNav.SavedRoutes },
                onStartRun = {
                    selectedRoutePoints = n.points   // 保存 GPX 路線供地圖顯示
                    elapsedSeconds = 0
                    unlockedDuringRun.clear()
                    if (locationGranted) tracker.start()
                    phase = RunPhase.RUNNING
                    nav = RunNav.Main
                }
            )
            return
        }
        else -> Unit   // RunNav.Main：繼續顯示主畫面
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ── 頁首：選了路線跑步時隱藏，讓地圖佔滿；直接跑步仍顯示計時器 ────────
        if (selectedRoutePoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                if (phase == RunPhase.PRE_RUN) {
                    Column {
                        Text("去跑步", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("探索臺南古蹟", fontSize = 13.sp, color = TextGray)
                    }
                } else {
                    Text(
                        text = formatTime(elapsedSeconds),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // ── 地圖 + 底部按鈕覆蓋 ─────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(120.2028, 23.0000))
                                    .zoom(13.0).build()
                            )
                            annotations.createCircleAnnotationManager().create(
                                heritages.map { h ->
                                    CircleAnnotationOptions()
                                        .withPoint(Point.fromLngLat(h.lng, h.lat))
                                        .withCircleRadius(7.0).withCircleColor("#B5651D")
                                        .withCircleStrokeWidth(1.5).withCircleStrokeColor("#FFFFFF")
                                }
                            )
                            // routeLineManager 先建（在下層）→ polylineManager 在上層蓋 GPS 軌跡
                            routeLineManager = annotations.createPolylineAnnotationManager()
                            polylineManager  = annotations.createPolylineAnnotationManager()
                        }
                    }
                },
                update = { mv ->
                    if (locationGranted) mv.location.updateSettings { enabled = true }
                    // 畫 GPX 預定路線（淡橘色參考線）
                    if (selectedRoutePoints.size >= 2) {
                        routeLineManager?.deleteAll()
                        routeLineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(selectedRoutePoints)
                                .withLineColor("#C8A46A").withLineWidth(3.0)
                        )
                    }
                    // 畫 GPS 即時軌跡（深橘色）
                    val pts = tracker.points.toList()
                    if (pts.size >= 2) {
                        polylineManager?.deleteAll()
                        polylineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(pts).withLineColor("#D4822A").withLineWidth(5.0)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (phase == RunPhase.PRE_RUN) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.95f))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { nav = RunNav.SavedRoutes },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, OrangeAccent)
                    ) {
                        Text("選擇路線", color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            elapsedSeconds = 0
                            unlockedDuringRun.clear()
                            if (locationGranted) tracker.start()
                            phase = RunPhase.RUNNING
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("直接跑步", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (selectedRoutePoints.isNotEmpty()) {
                // ── 選擇路線跑步：統計列 + 古蹟解鎖通知 ────────────────────
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    notifHeritage?.let { h ->
                        HeritageUnlockCard(
                            heritage = h,
                            silverReward = HERITAGE_UNLOCK_REWARD,
                            onDismiss = { notifHeritage = null }
                        )
                    }
                    RunningStatsBar(
                        coveredKm = coveredKm,
                        elapsedSeconds = elapsedSeconds,
                        onStop = {
                            RouteRepository.addRun(tracker.points.toList())
                            tracker.stop()
                            selectedRoutePoints = emptyList()
                            notifHeritage = null
                            phase = RunPhase.PRE_RUN
                        }
                    )
                }
            } else {
                // ── 直接跑步：圓形停止按鈕（等一下更新） ───────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.95f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            RouteRepository.addRun(tracker.points.toList())
                            tracker.stop()
                            phase = RunPhase.PRE_RUN
                        },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("停止", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)
    val h = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * atan2(sqrt(h), sqrt(1 - h))
}
