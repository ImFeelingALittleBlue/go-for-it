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

// 去探索的子畫面
private sealed class RunNav {
    object Main        : RunNav()
    object SavedRoutes : RunNav()
    data class RoutePreview(val points: List<Point>, val distanceKm: Float, val gpxContent: String = "") : RunNav()
    data class Summary(
        val trackPoints: List<Point>,
        val routePoints: List<Point>,
        val coveredKm: Float,
        val elapsedSeconds: Int,
        val unlockedHeritages: List<Heritage>,
        val silverEarned: Int,
        val routeName: String             // 與存進 Firestore 的名稱一致
    ) : RunNav()
}

// 跑步的兩個階段
private enum class RunPhase { PRE_RUN, RUNNING, PAUSED }
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
    var selectedRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var selectedRouteGpx    by remember { mutableStateOf("") }   // 原始 GPX 字串，跑完存進紀錄
    var notifHeritage       by remember { mutableStateOf<Heritage?>(null) }
    var showStopDialog      by remember { mutableStateOf(false) }
    val silverPoints        by SilverSaltStore.points(context)
    val restoredIds         = RestorationRepository.records().map { it.heritageId }.toSet()
    val nearestHeritage     = remember(pointCount) {
        val from = tracker.points.lastOrNull() ?: Point.fromLngLat(120.2028, 23.0000)
        heritages.minByOrNull { h -> distanceMeters(from.latitude(), from.longitude(), h.lat, h.lng) }
    }
    val distanceToNearestM  = remember(pointCount) {
        val from = tracker.points.lastOrNull() ?: Point.fromLngLat(120.2028, 23.0000)
        nearestHeritage?.let { h -> distanceMeters(from.latitude(), from.longitude(), h.lat, h.lng).toFloat() } ?: 999f
    }
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
                onGpxLoaded = { pts, dist, gpx -> nav = RunNav.RoutePreview(pts, dist, gpx) }
            )
            return
        }
        is RunNav.RoutePreview -> {
            RoutePreviewScreen(
                points = n.points,
                distanceKm = n.distanceKm,
                onBack = { nav = RunNav.SavedRoutes },
                onStartRun = {
                    selectedRoutePoints = n.points      // 保存 GPX 路線供地圖顯示
                    selectedRouteGpx    = n.gpxContent  // 保存原始 GPX，跑完存進紀錄
                    elapsedSeconds = 0
                    unlockedDuringRun.clear()
                    if (locationGranted) tracker.start()
                    phase = RunPhase.RUNNING
                    nav = RunNav.Main
                }
            )
            return
        }
        is RunNav.Summary -> {
            RunSummaryScreen(
                trackPoints      = n.trackPoints,
                routePoints      = n.routePoints,
                coveredKm        = n.coveredKm,
                elapsedSeconds   = n.elapsedSeconds,
                unlockedHeritages = n.unlockedHeritages,
                silverEarned     = n.silverEarned,
                initialRouteName = n.routeName,
                onFinish         = { nav = RunNav.Main }
            )
            return
        }
        else -> Unit
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        // ── 頁首：路線跑步時顯示銀鹽+統計；直接跑步時顯示標題或暫停確認 ───
        if (selectedRoutePoints.isNotEmpty() && phase != RunPhase.PRE_RUN) {
            // 路線跑步頂部：時光銀鹽 banner + 距離/時間統計
            RouteRunningTopPanel(
                silverPoints   = silverPoints,
                coveredKm      = coveredKm,
                elapsedSeconds = elapsedSeconds
            )
        } else if (selectedRoutePoints.isEmpty()) {
            when (phase) {
                RunPhase.PRE_RUN -> Box(
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text("去跑步", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("探索臺南古蹟", fontSize = 13.sp, color = TextGray)
                    }
                }
                RunPhase.RUNNING -> Column {
                    // 時光銀鹽 banner
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFF5F0EB))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(50),
                            color = Color(0xFF5C3D1E)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("銀", color = Color(0xFFD4A96A), fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("時光銀鹽", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("收集數量的進度累計", fontSize = 11.sp, color = TextGray)
                        }
                        Text("+$silverPoints", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = OrangeAccent)
                    }
                    // 統計列（無按鈕）
                    DirectRunStatsRow(coveredKm, elapsedSeconds)
                }
                RunPhase.PAUSED -> Column {
                    // 橘色確認列
                    Row(
                        modifier = Modifier.fillMaxWidth().background(OrangeAccent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("要結束旅程嗎？", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { phase = RunPhase.RUNNING }) {
                            Text("繼續跑步", color = Color.White, fontSize = 13.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = {
                                val pts     = tracker.points.toList()
                                val runName = "台南${detectRegion(pts)}探索"
                                val summary = RunNav.Summary(
                                    trackPoints       = pts,
                                    routePoints       = emptyList(),
                                    coveredKm         = coveredKm,
                                    elapsedSeconds    = elapsedSeconds,
                                    unlockedHeritages = heritages.filter { it.id in unlockedDuringRun },
                                    silverEarned      = unlockedDuringRun.size * HERITAGE_UNLOCK_REWARD,
                                    routeName         = runName
                                )
                                RouteRepository.addRun(runName, pts)
                                tracker.stop()
                                notifHeritage = null
                                phase = RunPhase.PRE_RUN
                                nav = summary
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
                        ) { Text("結束跑步", color = Color.White, fontSize = 13.sp) }
                    }
                    DirectRunStatsRow(coveredKm, elapsedSeconds)
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
                            showStopDialog = false        // 清掉可能殘留的路線跑步 dialog 狀態
                            selectedRoutePoints = emptyList() // 確保直接開始不帶著舊路線
                            elapsedSeconds = 0
                            unlockedDuringRun.clear()
                            if (locationGranted) tracker.start()
                            phase = RunPhase.RUNNING
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("直接開始", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (selectedRoutePoints.isNotEmpty()) {
                // 解鎖通知卡浮在地圖底部（有解鎖才顯示）
                notifHeritage?.let { h ->
                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        HeritageUnlockCard(
                            heritage = h,
                            silverReward = HERITAGE_UNLOCK_REWARD,
                            onDismiss = { notifHeritage = null }
                        )
                    }
                }
            }
            // 直接跑步：地圖內無 overlay
        }

        // ── 路線跑步底部面板（地圖下方：Podcast + 最近古蹟 + 按鈕）─────────
        if (selectedRoutePoints.isNotEmpty() && phase != RunPhase.PRE_RUN) {
            RouteRunningBottomPanel(
                routePoints        = selectedRoutePoints,
                heritages          = heritages,
                nearestHeritage    = nearestHeritage,
                distanceToNearestM = distanceToNearestM,
                unlockedIds        = restoredIds + unlockedDuringRun.toSet(),
                onPause            = { showStopDialog = true },
                onStop             = { showStopDialog = true }
            )
        }

        // ── 直接跑步底部：古蹟卡 + 暫停按鈕（地圖外） ──────────────────────
        if (selectedRoutePoints.isEmpty() && phase != RunPhase.PRE_RUN) {
            // 優先顯示解鎖通知，否則顯示最近古蹟預覽
            if (notifHeritage != null) {
                HeritageUnlockCard(
                    heritage = notifHeritage!!,
                    silverReward = HERITAGE_UNLOCK_REWARD,
                    onDismiss = { notifHeritage = null }
                )
            } else {
                nearestHeritage?.let { h ->
                    NearestHeritagePreviewCard(
                        heritage = h,
                        distanceMeters = distanceToNearestM,
                        isRestored = h.id in restoredIds
                    )
                }
            }
            if (phase == RunPhase.RUNNING) {
                Button(
                    onClick = { showStopDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                ) {
                    Text("暫停", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── 停止確認對話框（直接跑步 + 選路線跑步共用）──────────────────────────
    if (showStopDialog) {
        StopConfirmDialog(
            onConfirm = {
                showStopDialog = false
                // 用 GPS 軌跡（或路線點備援）推算地區，生成與結算頁相同的名稱
                val pts     = tracker.points.toList().ifEmpty { selectedRoutePoints }
                val runName = "台南${detectRegion(pts)}探索"
                val summary = RunNav.Summary(
                    trackPoints       = tracker.points.toList(),
                    routePoints       = selectedRoutePoints.toList(),
                    coveredKm         = coveredKm,
                    elapsedSeconds    = elapsedSeconds,
                    unlockedHeritages = heritages.filter { it.id in unlockedDuringRun },
                    silverEarned      = unlockedDuringRun.size * HERITAGE_UNLOCK_REWARD,
                    routeName         = runName
                )
                RouteRepository.addRun(runName, tracker.points.toList(), selectedRouteGpx)
                tracker.stop()
                selectedRoutePoints = emptyList()
                selectedRouteGpx    = ""
                notifHeritage = null
                phase = RunPhase.PRE_RUN
                nav = summary
            },
            onDismiss = { showStopDialog = false }
        )
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
