package com.example.goforit.ui.run

import android.graphics.BitmapFactory
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
import com.example.goforit.data.PendingRunStore
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RouteRepository
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.example.goforit.ui.home.TextGray
import com.example.goforit.ui.home.rememberLocationPermission
import com.example.goforit.ui.common.LocateMeButton
import com.example.goforit.ui.common.moveMapToPoint
import com.example.goforit.ui.common.requestCurrentLocationPoint
import com.example.goforit.ui.common.showCurrentLocationMarker
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
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
    data class RoutePreview(val points: List<Point>, val distanceKm: Float, val gpxContent: String = "", val fromPlan: Boolean = false, val routePlanHeritages: List<Heritage> = emptyList()) : RunNav()
    object RoutePlan       : RunNav()
    data class Summary(
        val trackPoints: List<Point>,
        val routePoints: List<Point>,
        val coveredKm: Float,
        val elapsedSeconds: Int,
        val unlockedHeritages: List<Heritage>,
        val silverEarned: Int,
        val routeName: String,
        val podcastHeritages: List<Heritage> = emptyList()  // 規劃模式的 5 個選取古蹟
    ) : RunNav()
}

// 跑步的兩個階段
private enum class RunPhase { PRE_RUN, RUNNING, PAUSED }
private const val HERITAGE_UNLOCK_RADIUS_METERS = 40.0
// 計算本次跑步獲得的時光銀鹽
// 公式：Steps × 0.013 × (0.5 + Speed/10)，Speed > 20km/hr（作弊/搭車）則歸零
fun calculateSilverReward(steps: Int, coveredKm: Float, elapsedSeconds: Int): Int {
    val avgSpeedKmh = if (elapsedSeconds > 0)
        (coveredKm / (elapsedSeconds / 3600.0)) else 0.0
    return if (avgSpeedKmh <= 20.0)
        (steps * 0.013 * (0.5 + avgSpeedKmh / 10.0)).toInt()
    else 0
}

@Composable
fun RunScreen(
    autoStartRequest: Int = 0
) {
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
    var currentLocationMarkerManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var currentLocationLoaded by remember { mutableStateOf(false) }
    val pointCount         = tracker.points.size   // 觀察 GPS 新點，觸發 update block
    val unlockedDuringRun  = remember { mutableStateListOf<Int>() }
    val podcastRequestedDuringRun = remember { mutableStateListOf<Int>() }
    var selectedRoutePoints by remember { mutableStateOf<List<Point>>(emptyList()) }
    var selectedRouteGpx    by remember { mutableStateOf("") }   // 原始 GPX 字串，跑完存進紀錄
    var notifHeritage            by remember { mutableStateOf<Heritage?>(null) }
    var showStopDialog           by remember { mutableStateOf(false) }
    // 目前正在播放 Podcast 的古蹟與對話行（傳給 RouteRunningPanel 更新卡片 UI）
    var activePodcastHeritage    by remember { mutableStateOf<Heritage?>(null) }
    var activeLine               by remember { mutableStateOf<DialogueLine?>(null) }
    val restoredIds = RestorationRepository.records().map { it.heritageId }.toSet()
    val nearestHeritage     = remember(pointCount) {
        val from = tracker.points.lastOrNull() ?: Point.fromLngLat(120.2028, 23.0000)
        heritages.minByOrNull { h -> distanceMeters(from.latitude(), from.longitude(), h.lat, h.lng) }
    }
    val distanceToNearestM  = remember(pointCount) {
        val from = tracker.points.lastOrNull() ?: Point.fromLngLat(120.2028, 23.0000)
        nearestHeritage?.let { h -> distanceMeters(from.latitude(), from.longitude(), h.lat, h.lng).toFloat() } ?: 999f
    }
    var consumedAutoStartRequest by remember { mutableIntStateOf(0) }
    var podcastDuration    by remember { mutableIntStateOf(8) }   // 每個古蹟播幾分鐘，預設 8
    var planHeritages      by remember { mutableStateOf<List<Heritage>>(emptyList()) }
    var podcastAutoPlay    by remember { mutableStateOf(false) }  // 路線模式：自動依序播放
    var podcastAutoTrigger by remember { mutableIntStateOf(0) }   // 遞增觸發起始播放
    // 地圖上古蹟點被點選時浮出的名稱提示框
    var tooltipHeritage    by remember { mutableStateOf<Heritage?>(null) }
    var tooltipX           by remember { mutableStateOf(0f) }
    var tooltipY           by remember { mutableStateOf(0f) }

    // 計步器：記錄開始跑步時的累計步數，結束時相減得到本次步數
    // TYPE_STEP_COUNTER 從手機開機後持續累計，不會重設
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor    = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    var stepCountAtStart by remember { mutableIntStateOf(-1) }   // -1 = 尚未記錄
    var stepCountNow     by remember { mutableIntStateOf(0) }
    val stepListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val total = event.values[0].toInt()
                // 第一個事件：記錄起始值
                if (stepCountAtStart < 0) stepCountAtStart = total
                stepCountNow = total
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    val podcastPlayer = remember {
        DefaultPodcastPlayer(
            context = context,
            apiKey  = com.example.goforit.BuildConfig.ANTHROPIC_API_KEY,
            onPlaybackStarted = { heritageId ->
                activePodcastHeritage = heritages.firstOrNull { it.id == heritageId }
            },
            onPlaybackCompleted = { heritageId ->
                activePodcastHeritage = null
            },
            onPlaybackFailed = { heritageId ->
                activePodcastHeritage = null
                // 不移除已請求清單：自動播放模式不重試失敗項目，使用者可點卡片手動重播
            },
            onLineChanged = { line -> activeLine = line }
        )
    }
    // 即時計算已跑距離（每新增一個 GPS 點就重算）
    val coveredKm = remember(pointCount) {
        if (tracker.points.size < 2) 0f
        else tracker.points.zipWithNext { a, b ->
            (distanceMeters(a.latitude(), a.longitude(), b.latitude(), b.longitude()) / 1000.0).toFloat()
        }.sum()
    }

    // 即時估算本次跑步銀鹽（每秒/每步更新，結束時才是真正結算值）
    val liveSteps = if (stepCountAtStart >= 0) stepCountNow - stepCountAtStart
                    else (coveredKm * 1333).toInt()
    val liveSilver = calculateSilverReward(liveSteps, coveredKm, elapsedSeconds)

    rememberLocationPermission { locationGranted = true }

    // 計步器：跑步中啟動，停跑後停止
    LaunchedEffect(phase) {
        if (phase == RunPhase.RUNNING) {
            stepCountAtStart = -1  // 重設，第一個感測器事件會記錄起點
            stepSensor?.let {
                sensorManager.registerListener(stepListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else if (phase == RunPhase.PRE_RUN) {
            sensorManager.unregisterListener(stepListener)
        }
    }

    // 首頁「立即探索」按鈕 → 自動開始直接跑步
    LaunchedEffect(autoStartRequest, locationGranted) {
        if (
            autoStartRequest > consumedAutoStartRequest &&
            locationGranted &&
            phase == RunPhase.PRE_RUN
        ) {
            consumedAutoStartRequest = autoStartRequest
            selectedRoutePoints = emptyList()
            planHeritages       = emptyList()
            podcastDuration     = 8
            podcastAutoPlay     = false
            elapsedSeconds = 0
            unlockedDuringRun.clear()
            podcastRequestedDuringRun.clear()
            tracker.start()
            phase = RunPhase.RUNNING
        }
    }

    // 偵測「再跑一次」請求：CollectionScreen 設置 PendingRunStore 後切換到此 tab
    val hasPendingRun by PendingRunStore.hasPending
    LaunchedEffect(hasPendingRun) {
        if (hasPendingRun) {
            val pts  = PendingRunStore.points
            val gpx  = PendingRunStore.gpxContent
            val dist = PendingRunStore.distanceKm
            PendingRunStore.clear()
            if (pts.isNotEmpty()) nav = RunNav.RoutePreview(pts, dist, gpx)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)   { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)    { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            sensorManager.unregisterListener(stepListener)
            tracker.stop()
            podcastPlayer.release()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(phase) {
        if (phase == RunPhase.RUNNING) {
            while (true) { delay(1_000L); elapsedSeconds++ }
        }
    }

    // 古蹟名稱提示框 2 秒後自動消失
    LaunchedEffect(tooltipHeritage) {
        if (tooltipHeritage != null) {
            delay(2_000L)
            tooltipHeritage = null
        }
    }

    // 直接跑步：GPS 進入 40m 範圍才觸發 Podcast（路線模式改用自動播放）
    LaunchedEffect(pointCount, phase) {
        if (phase != RunPhase.RUNNING || pointCount == 0) return@LaunchedEffect
        if (podcastAutoPlay) return@LaunchedEffect   // 路線/規劃模式由下方 LaunchedEffect 自動排序播放
        val latest = tracker.points.last()
        heritages
            .filter { !RestorationRepository.isRestored(it.id) && it.id !in podcastRequestedDuringRun }
            .firstOrNull { h -> distanceMeters(latest.latitude(), latest.longitude(), h.lat, h.lng) <= HERITAGE_UNLOCK_RADIUS_METERS }
            ?.let { h ->
                podcastRequestedDuringRun.add(h.id)
                podcastPlayer.play(h, podcastDuration)
            }
    }

    // 路線/規劃模式：進入跑步立刻播第一站
    LaunchedEffect(podcastAutoTrigger) {
        if (podcastAutoTrigger == 0) return@LaunchedEffect
        delay(400L)
        val queue = if (planHeritages.isNotEmpty()) planHeritages
                    else heritagesOnRoute(selectedRoutePoints, heritages).map { it.first }
        val minPerStop = (podcastDuration / queue.size.coerceAtLeast(1)).coerceAtLeast(1)
        queue.firstOrNull()?.let { first ->
            podcastRequestedDuringRun.add(first.id)
            activePodcastHeritage = first   // 先標記，讓 UI 顯示「生成中」
            podcastPlayer.play(first, minPerStop)
        }
    }

    // 路線/規劃模式：每首播完自動播下一站（使用者暫停時 podcastAutoPlay = false）
    LaunchedEffect(activePodcastHeritage) {
        if (activePodcastHeritage != null) return@LaunchedEffect
        if (!podcastAutoPlay || phase != RunPhase.RUNNING) return@LaunchedEffect
        if (podcastRequestedDuringRun.isEmpty()) return@LaunchedEffect
        delay(800L)
        val queue = if (planHeritages.isNotEmpty()) planHeritages
                    else heritagesOnRoute(selectedRoutePoints, heritages).map { it.first }
        val minPerStop = (podcastDuration / queue.size.coerceAtLeast(1)).coerceAtLeast(1)
        val next = queue.firstOrNull { it.id !in podcastRequestedDuringRun }
        next?.let {
            podcastRequestedDuringRun.add(it.id)
            activePodcastHeritage = it      // 先標記，讓 UI 顯示「生成中」
            podcastPlayer.play(it, minPerStop)
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
        is RunNav.RoutePlan -> {
            RoutePlanScreen(
                onBack      = { nav = RunNav.Main },
                onRouteDone = { pts, dist, hs -> nav = RunNav.RoutePreview(pts, dist, fromPlan = true, routePlanHeritages = hs) }
            )
            return
        }
        is RunNav.RoutePreview -> {
            RoutePreviewScreen(
                points        = n.points,
                distanceKm    = n.distanceKm,
                planHeritages = n.routePlanHeritages,
                onBack        = { nav = if (n.fromPlan) RunNav.Main else RunNav.SavedRoutes },
                onStartRun    = { duration ->
                    selectedRoutePoints = n.points
                    selectedRouteGpx    = n.gpxContent
                    podcastDuration     = duration
                    // 規劃模式：直接使用 RoutePlanScreen 吸附確認的古蹟清單，不再重算距離
                    planHeritages = n.routePlanHeritages
                    podcastAutoPlay = true
                    elapsedSeconds = 0
                    unlockedDuringRun.clear()
                    podcastRequestedDuringRun.clear()
                    if (locationGranted) tracker.start()
                    phase = RunPhase.RUNNING
                    nav = RunNav.Main
                    podcastAutoTrigger++   // 觸發 LaunchedEffect 播放第一站
                }
            )
            return
        }
        is RunNav.Summary -> {
            RunSummaryScreen(
                trackPoints       = n.trackPoints,
                routePoints       = n.routePoints,
                coveredKm         = n.coveredKm,
                elapsedSeconds    = n.elapsedSeconds,
                unlockedHeritages = n.unlockedHeritages,
                silverEarned      = n.silverEarned,
                initialRouteName  = n.routeName,
                podcastHeritages  = n.podcastHeritages,
                onFinish          = { nav = RunNav.Main }
            )
            return
        }
        else -> Unit
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3EEE6))) {

        // ── 頁首：路線跑步時顯示銀鹽+統計；直接跑步時顯示標題或暫停確認 ───
        if (selectedRoutePoints.isNotEmpty() && phase != RunPhase.PRE_RUN) {
            // 路線跑步頂部：時光銀鹽 banner + 距離/時間統計
            RouteRunningTopPanel(
                liveSilver     = liveSilver,
                coveredKm      = coveredKm,
                elapsedSeconds = elapsedSeconds
            )
        } else if (selectedRoutePoints.isEmpty()) {
            when (phase) {
                RunPhase.PRE_RUN -> Box(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFFFDF8))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text("去跑步", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("探索臺南古蹟", fontSize = 13.sp, color = TextGray)
                    }
                }
                RunPhase.RUNNING -> Column {
                    // 統計列（距離 + 時間）
                    DirectRunStatsRow(coveredKm, elapsedSeconds)
                    // 本次預計銀鹽（距離/時間下方）
                    RunSilverRow(liveSilver)
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
                                // 計算步數：感測器有值用感測器，否則從距離估算（1km ≈ 1333步）
                                val steps = if (stepCountAtStart >= 0) stepCountNow - stepCountAtStart
                                            else (coveredKm * 1333).toInt()
                                val silver = calculateSilverReward(steps, coveredKm, elapsedSeconds)
                                val summary = RunNav.Summary(
                                    trackPoints       = pts,
                                    routePoints       = emptyList(),
                                    coveredKm         = coveredKm,
                                    elapsedSeconds    = elapsedSeconds,
                                    unlockedHeritages = heritages.filter { it.id in unlockedDuringRun },
                                    silverEarned      = silver,
                                    routeName         = runName
                                )
                                RouteRepository.addRun(runName, pts, "",
                                    elapsedSeconds, silver, unlockedDuringRun.size)
                                tracker.stop()
                                podcastPlayer.stop()
                                notifHeritage = null
                                phase = RunPhase.PRE_RUN
                                nav = summary
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B211A))
                        ) { Text("結束跑步", color = Color.White, fontSize = 13.sp) }
                    }
                    DirectRunStatsRow(coveredKm, elapsedSeconds)
                    RunSilverRow(liveSilver)
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
                                heritages.filter { it.id !in restoredIds }.map { h ->
                                    CircleAnnotationOptions()
                                        .withPoint(Point.fromLngLat(h.lng, h.lat))
                                        .withCircleRadius(7.0)
                                        .withCircleColor("#496F8E")
                                        .withCircleStrokeWidth(1.5)
                                        .withCircleStrokeColor("#FFFFFF")
                                }
                            )
                            annotations.createPointAnnotationManager().create(
                                heritages.filter { it.id in restoredIds }.mapNotNull { h ->
                                    val bitmap = runCatching {
                                        context.assets.open("thumbnails/${h.photoFile}")
                                            .use(BitmapFactory::decodeStream)
                                    }.getOrNull() ?: return@mapNotNull null
                                    PointAnnotationOptions()
                                        .withPoint(Point.fromLngLat(h.lng, h.lat))
                                        .withIconImage(bitmap)
                                        .withIconSize(2.6)
                                }
                            )
                            // routeLineManager 先建（在下層）→ polylineManager 在上層蓋 GPS 軌跡
                            routeLineManager = annotations.createPolylineAnnotationManager()
                            polylineManager  = annotations.createPolylineAnnotationManager()
                            currentLocationMarkerManager = annotations.createCircleAnnotationManager()
                            // 點擊地圖：偵測 80m 內最近的古蹟，顯示名稱提示框
                            mapboxMap.addOnMapClickListener { tapped ->
                                val near = heritages.minByOrNull { h ->
                                    distanceMeters(tapped.latitude(), tapped.longitude(), h.lat, h.lng)
                                }?.takeIf { h ->
                                    distanceMeters(tapped.latitude(), tapped.longitude(), h.lat, h.lng) <= 80.0
                                }
                                if (near != null) {
                                    val px = mapboxMap.pixelForCoordinate(Point.fromLngLat(near.lng, near.lat))
                                    tooltipX = px.x.toFloat()
                                    tooltipY = px.y.toFloat()
                                    tooltipHeritage = near
                                } else {
                                    tooltipHeritage = null
                                }
                                false  // 不攔截事件，保留地圖手勢
                            }
                        }
                    }
                },
                update = { mv ->
                    if (locationGranted) mv.location.updateSettings { enabled = false }
                    // 畫 GPX 預定路線（淡橘色參考線）
                    if (selectedRoutePoints.size >= 2) {
                        routeLineManager?.deleteAll()
                        routeLineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(selectedRoutePoints)
                                .withLineColor("#C9B08D").withLineWidth(3.0)
                        )
                    }
                    // 畫 GPS 即時軌跡（深橘色）
                    val pts = tracker.points.toList()
                    val currentManager = currentLocationMarkerManager
                        ?: mv.annotations.createCircleAnnotationManager().also {
                            currentLocationMarkerManager = it
                        }
                    val latestPoint = pts.lastOrNull()
                    if (latestPoint != null) {
                        currentLocationLoaded = true
                        showCurrentLocationMarker(currentManager, latestPoint)
                    } else if (locationGranted && !currentLocationLoaded) {
                        currentLocationLoaded = true
                        requestCurrentLocationPoint(context) { point ->
                            showCurrentLocationMarker(currentManager, point)
                        }
                    }
                    if (pts.size >= 2) {
                        polylineManager?.deleteAll()
                        polylineManager?.create(
                            PolylineAnnotationOptions()
                                .withPoints(pts).withLineColor("#9B6A3F").withLineWidth(5.0)
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            LocateMeButton(
                enabled = locationGranted,
                onClick = {
                    fun moveToCurrent(point: Point) {
                        val manager = currentLocationMarkerManager
                            ?: mapView.annotations.createCircleAnnotationManager().also {
                                currentLocationMarkerManager = it
                            }
                        showCurrentLocationMarker(manager, point)
                        moveMapToPoint(
                            mapView = mapView,
                            point = point,
                            zoom = if (phase == RunPhase.RUNNING) 17.0 else 16.0
                        )
                    }
                    tracker.points.lastOrNull()?.let(::moveToCurrent)
                        ?: requestCurrentLocationPoint(context, ::moveToCurrent)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
            )

            if (phase == RunPhase.PRE_RUN) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xFFFBF7F0).copy(alpha = 0.96f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showStopDialog      = false
                            selectedRoutePoints = emptyList()
                            planHeritages       = emptyList()
                            podcastDuration     = 8
                            podcastAutoPlay     = false
                            elapsedSeconds = 0
                            unlockedDuringRun.clear()
                            podcastRequestedDuringRun.clear()
                            if (locationGranted) tracker.start()
                            phase = RunPhase.RUNNING
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) {
                        Text("直接開始", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { nav = RunNav.SavedRoutes },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, OrangeAccent)
                        ) {
                            Text("選擇路線", color = OrangeAccent, fontSize = 14.sp)
                        }
                        OutlinedButton(
                            onClick = { nav = RunNav.RoutePlan },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, OrangeAccent)
                        ) {
                            Text("規劃路線", color = OrangeAccent, fontSize = 14.sp)
                        }
                    }
                }
            } else if (selectedRoutePoints.isNotEmpty()) {
                // 解鎖通知卡浮在地圖底部（有解鎖才顯示）
                notifHeritage?.let { h ->
                    Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        HeritageUnlockCard(
                            heritage = h,
                            onDismiss = { notifHeritage = null }
                        )
                    }
                }
            }
            // 直接跑步：地圖內無 overlay
            // 古蹟名稱浮動提示框：點擊地圖上的古蹟點後出現，點其他地方消失
            tooltipHeritage?.let { h ->
                HeritageNameTooltip(name = h.name, x = tooltipX, y = tooltipY,
                    onDismiss = { tooltipHeritage = null })
            }
        }

        // ── 路線跑步底部面板（地圖下方：Podcast + 最近古蹟 + 按鈕）─────────
        if (selectedRoutePoints.isNotEmpty() && phase != RunPhase.PRE_RUN) {
            RouteRunningBottomPanel(
                routePoints           = selectedRoutePoints,
                heritages             = heritages,
                planHeritages         = planHeritages,
                unlockedIds           = restoredIds + unlockedDuringRun.toSet(),
                playedIds             = podcastRequestedDuringRun.toSet(),
                activePodcastHeritage = activePodcastHeritage,
                activeLine            = activeLine,
                autoTriggerEnabled    = podcastAutoPlay,
                onAutoTriggerToggle   = { enabled -> podcastAutoPlay = enabled },
                onPlayHeritage        = { h ->
                    podcastRequestedDuringRun.remove(h.id)
                    podcastAutoPlay = true
                    val queue = if (planHeritages.isNotEmpty()) planHeritages
                                else heritagesOnRoute(selectedRoutePoints, heritages).map { it.first }
                    val minPerStop = (podcastDuration / queue.size.coerceAtLeast(1)).coerceAtLeast(1)
                    podcastPlayer.play(h, minPerStop)
                },
                onPlayPause           = {
                    if (activePodcastHeritage != null) {
                        podcastPlayer.stop()
                        activePodcastHeritage = null
                        podcastAutoPlay = false
                    } else {
                        podcastAutoPlay = true
                        podcastAutoTrigger++
                    }
                },
                onPause               = { showStopDialog = true }
            )
        }

        // ── 直接跑步底部：古蹟卡 + 暫停按鈕（地圖外） ──────────────────────
        if (selectedRoutePoints.isEmpty() && phase != RunPhase.PRE_RUN) {
            // 優先顯示解鎖通知，否則顯示最近古蹟預覽
            if (notifHeritage != null) {
                HeritageUnlockCard(
                    heritage = notifHeritage!!,
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

        // ── [測試用] 手動觸發 Podcast 播放，確認 TTS 正常後可刪除 ────────────
        if (phase == RunPhase.RUNNING) {
            // 路線模式取路線第一個古蹟；直接模式取最近古蹟
            val testTarget = when {
                planHeritages.isNotEmpty()       -> planHeritages.firstOrNull { it.id !in podcastRequestedDuringRun } ?: planHeritages.firstOrNull()
                selectedRoutePoints.isNotEmpty() -> heritagesOnRoute(selectedRoutePoints, heritages).firstOrNull()?.first
                else                             -> nearestHeritage ?: heritages.firstOrNull()
            }
            TextButton(
                onClick = {
                    testTarget?.let {
                        podcastRequestedDuringRun.remove(it.id)
                        podcastPlayer.play(it, podcastDuration)
                    }
                },
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFAF5EF))
            ) {
                Text("🎧 測試播放：${testTarget?.name ?: "無古蹟"}",
                    color = OrangeAccent, fontSize = 12.sp)
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
                // 計算步數：感測器有值用感測器，否則從距離估算（1km ≈ 1333步）
                val steps = if (stepCountAtStart >= 0) stepCountNow - stepCountAtStart
                            else (coveredKm * 1333).toInt()
                val silver = calculateSilverReward(steps, coveredKm, elapsedSeconds)
                val summary = RunNav.Summary(
                    trackPoints       = tracker.points.toList(),
                    routePoints       = selectedRoutePoints.toList(),
                    coveredKm         = coveredKm,
                    elapsedSeconds    = elapsedSeconds,
                    unlockedHeritages = heritages.filter { it.id in unlockedDuringRun },
                    silverEarned      = silver,
                    routeName         = runName,
                    podcastHeritages  = planHeritages.toList()
                )
                RouteRepository.addRun(runName, tracker.points.toList(), selectedRouteGpx,
                    elapsedSeconds, silver, unlockedDuringRun.size)
                tracker.stop()
                podcastPlayer.stop()
                podcastAutoPlay     = false
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
