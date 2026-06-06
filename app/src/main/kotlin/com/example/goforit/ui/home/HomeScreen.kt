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
import com.example.goforit.data.MapBuildRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.ui.applyWarmMapStyle
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.OnCircleAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val OrangeAccent = Color(0xFFD4822A)    // 設計稿主色
val TextGray     = Color(0xFF888888)
val ChipBg       = Color(0xFFF0EDE8)
private const val BUILT_BUILDINGS_SOURCE_ID = "built-heritage-buildings-source"
private const val BUILT_BUILDINGS_PLAZA_SOURCE_ID = "built-heritage-plazas-source"
private const val BUILT_BUILDINGS_ROOF_SOURCE_ID = "built-heritage-roofs-source"
private const val BUILT_BUILDINGS_TOWER_SOURCE_ID = "built-heritage-towers-source"
private const val BUILT_BUILDINGS_PLAZA_LAYER_ID = "built-heritage-plazas-layer"
private const val BUILT_BUILDINGS_BODY_LAYER_ID = "built-heritage-buildings-body-layer"
private const val BUILT_BUILDINGS_ROOF_LAYER_ID = "built-heritage-buildings-roof-layer"
private const val BUILT_BUILDINGS_TOWER_LAYER_ID = "built-heritage-towers-layer"
private const val BUILT_BUILDING_SIZE_METERS = 145.0

enum class HeritageFilter { ALL, RESTORED, PENDING, BUILT }

@Composable
fun HomeScreen(
    onStartExplore: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }

    // 讀取 CSV 裡的古蹟資料（只在第一次組畫面時讀一次）
    val heritages = remember { HeritageRepository.loadHeritages(context) }
    val restorationRecords = RestorationRepository.records().toList()
    val restoredIds = restorationRecords.map { it.heritageId }.toSet()
    val buildRecords = MapBuildRepository.records().toList()
    val builtIds = buildRecords.map { it.heritageId }.toSet()
    var heritageFilter by remember { mutableStateOf(HeritageFilter.ALL) }
    val visibleHeritages = when (heritageFilter) {
        HeritageFilter.ALL -> heritages
        HeritageFilter.RESTORED -> heritages.filter { it.id in restoredIds }
        HeritageFilter.PENDING -> heritages.filter { it.id !in restoredIds }
        HeritageFilter.BUILT -> heritages.filter { it.id in builtIds }
    }
    var markerManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    val markerLookup = remember { mutableMapOf<String, Heritage>() }
    var markerClickListenerInstalled by remember { mutableStateOf(false) }
    var buildingScale by remember { mutableFloatStateOf(1f) }
    var knownBuiltIds by remember { mutableStateOf<Set<Int>?>(null) }

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

    LaunchedEffect(builtIds) {
        val known = knownBuiltIds
        if (known == null) {
            knownBuiltIds = builtIds
            return@LaunchedEffect
        }
        val newlyBuilt = builtIds - known
        knownBuiltIds = builtIds
        if (newlyBuilt.isNotEmpty()) {
            val frames = 24
            for (frame in 0..frames) {
                val t = frame / frames.toFloat()
                buildingScale = (0.22f + 0.9f * easeOutBack(t)).coerceIn(0.22f, 1.12f)
                delay(16L)
            }
            buildingScale = 1f
        }
    }

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
                            applyWarmMapStyle(it)
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(120.2028, 23.0000))
                                    .zoom(13.0)
                                    .pitch(48.0)
                                    .bearing(-18.0)
                                    .build()
                            )
                        }
                    }
                },
                // update：當 locationGranted 變成 true 時被呼叫，此時樣式已載入完成
                update = { mv ->
                    if (locationGranted) {
                        mv.location.updateSettings { enabled = true }
                    }
                    mv.mapboxMap.getStyle {
                        val manager = markerManager
                            ?: mv.annotations.createCircleAnnotationManager().also { markerManager = it }
                        if (!markerClickListenerInstalled) {
                            manager.addClickListener(
                                OnCircleAnnotationClickListener { annotation ->
                                    markerLookup[annotation.id]?.let { selected = it }
                                    true
                                }
                            )
                            markerClickListenerInstalled = true
                        }
                        setBuiltHeritageBuildings(
                            style = it,
                            builtHeritages = visibleHeritages.filter { heritage -> heritage.id in builtIds },
                            scale = buildingScale
                        )
                        setHeritageMarkers(manager, visibleHeritages, restoredIds, builtIds, markerLookup)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // 地圖底部的兩顆按鈕
            MapOverlayButtons(modifier = Modifier.align(Alignment.BottomCenter))
        }

        // ── 全部古蹟列表（占 45%）：用狀態區分已修復 / 待修復 ───────────────
        MapHeritageSection(
            heritages = heritages,
            records = restorationRecords,
            builtIds = builtIds,
            selectedFilter = heritageFilter,
            onFilterChange = { heritageFilter = it },
            onHeritageClick = { selected = it },
            modifier = Modifier.weight(0.70f)
        )
    }

    // ── 古蹟詳情卡：selected 有值才顯示 ──────────────────────────────────────
    selected?.let { heritage ->
        HeritageDetailSheet(
            heritage = heritage,
            onDismiss = { selected = null },
            onStartExplore = {
                selected = null
                onStartExplore()
            }
        )
    }
}

// 在地圖上把所有古蹟畫成圓點，已修復 / 待修復使用不同顏色。
// onMarkerClick：點到某個圓點時，把對應的 Heritage 傳回去
private fun setHeritageMarkers(
    manager: CircleAnnotationManager,
    heritages: List<Heritage>,
    restoredIds: Set<Int>,
    builtIds: Set<Int>,
    markerLookup: MutableMap<String, Heritage>
) {
    manager.deleteAll()
    markerLookup.clear()
    val options = heritages.map { h ->
        CircleAnnotationOptions()
            .withPoint(Point.fromLngLat(h.lng, h.lat))  // 圓點位置
            .withCircleRadius(if (h.id in builtIds) 4.0 else 8.0)
            .withCircleColor(
                when {
                    h.id in builtIds -> "#F7D47A"
                    h.id in restoredIds -> "#4CAF50"
                    else -> "#D4822A"
                }
            )
            .withCircleStrokeWidth(if (h.id in builtIds) 1.5 else 2.0)
            .withCircleStrokeColor(if (h.id in builtIds) "#C46A2D" else "#FFFFFF")
    }

    val created = manager.create(options)
    markerLookup.putAll(created.zip(heritages).associate { (annotation, h) -> annotation.id to h })
}

private fun setBuiltHeritageBuildings(
    style: Style,
    builtHeritages: List<Heritage>,
    scale: Float
) {
    if (style.styleLayerExists(BUILT_BUILDINGS_ROOF_LAYER_ID)) {
        style.removeStyleLayer(BUILT_BUILDINGS_ROOF_LAYER_ID)
    }
    if (style.styleLayerExists(BUILT_BUILDINGS_TOWER_LAYER_ID)) {
        style.removeStyleLayer(BUILT_BUILDINGS_TOWER_LAYER_ID)
    }
    if (style.styleLayerExists(BUILT_BUILDINGS_BODY_LAYER_ID)) {
        style.removeStyleLayer(BUILT_BUILDINGS_BODY_LAYER_ID)
    }
    if (style.styleLayerExists(BUILT_BUILDINGS_PLAZA_LAYER_ID)) {
        style.removeStyleLayer(BUILT_BUILDINGS_PLAZA_LAYER_ID)
    }
    if (style.styleSourceExists(BUILT_BUILDINGS_TOWER_SOURCE_ID)) {
        style.removeStyleSource(BUILT_BUILDINGS_TOWER_SOURCE_ID)
    }
    if (style.styleSourceExists(BUILT_BUILDINGS_ROOF_SOURCE_ID)) {
        style.removeStyleSource(BUILT_BUILDINGS_ROOF_SOURCE_ID)
    }
    if (style.styleSourceExists(BUILT_BUILDINGS_SOURCE_ID)) {
        style.removeStyleSource(BUILT_BUILDINGS_SOURCE_ID)
    }
    if (style.styleSourceExists(BUILT_BUILDINGS_PLAZA_SOURCE_ID)) {
        style.removeStyleSource(BUILT_BUILDINGS_PLAZA_SOURCE_ID)
    }

    geoJsonSource(BUILT_BUILDINGS_PLAZA_SOURCE_ID)
        .featureCollection(buildBuildingFeatureCollection(builtHeritages, scale, BuildingPart.PLAZA))
        .bindTo(style)
    geoJsonSource(BUILT_BUILDINGS_SOURCE_ID)
        .featureCollection(buildBuildingFeatureCollection(builtHeritages, scale, BuildingPart.BODY))
        .bindTo(style)
    geoJsonSource(BUILT_BUILDINGS_ROOF_SOURCE_ID)
        .featureCollection(buildBuildingFeatureCollection(builtHeritages, scale, BuildingPart.ROOF))
        .bindTo(style)
    geoJsonSource(BUILT_BUILDINGS_TOWER_SOURCE_ID)
        .featureCollection(buildBuildingFeatureCollection(builtHeritages, scale, BuildingPart.TOWER))
        .bindTo(style)

    FillExtrusionLayer(BUILT_BUILDINGS_PLAZA_LAYER_ID, BUILT_BUILDINGS_PLAZA_SOURCE_ID)
        .fillExtrusionColor("#FFF0C7")
        .fillExtrusionHeight(10.0 * scale)
        .fillExtrusionBase(0.0)
        .fillExtrusionOpacity(0.96)
        .fillExtrusionVerticalGradient(true)
        .minZoom(10.8)
        .bindTo(style)

    FillExtrusionLayer(BUILT_BUILDINGS_BODY_LAYER_ID, BUILT_BUILDINGS_SOURCE_ID)
        .fillExtrusionColor("#FF9F5A")
        .fillExtrusionHeight(122.0 * scale)
        .fillExtrusionBase(10.0 * scale)
        .fillExtrusionOpacity(0.96)
        .fillExtrusionVerticalGradient(true)
        .minZoom(10.8)
        .bindTo(style)

    FillExtrusionLayer(BUILT_BUILDINGS_ROOF_LAYER_ID, BUILT_BUILDINGS_ROOF_SOURCE_ID)
        .fillExtrusionColor("#FFE36D")
        .fillExtrusionHeight(176.0 * scale)
        .fillExtrusionBase(122.0 * scale)
        .fillExtrusionOpacity(0.98)
        .fillExtrusionVerticalGradient(true)
        .minZoom(10.8)
        .bindTo(style)

    FillExtrusionLayer(BUILT_BUILDINGS_TOWER_LAYER_ID, BUILT_BUILDINGS_TOWER_SOURCE_ID)
        .fillExtrusionColor("#FF6F61")
        .fillExtrusionHeight(226.0 * scale)
        .fillExtrusionBase(122.0 * scale)
        .fillExtrusionOpacity(0.98)
        .fillExtrusionVerticalGradient(true)
        .minZoom(10.8)
        .bindTo(style)
}

private enum class BuildingPart { PLAZA, BODY, ROOF, TOWER }

private fun buildBuildingFeatureCollection(
    heritages: List<Heritage>,
    scale: Float,
    part: BuildingPart
): FeatureCollection {
    val features = heritages.map { heritage ->
        Feature.fromGeometry(buildFootprint(heritage, scale = scale, part = part))
    }
    return FeatureCollection.fromFeatures(features)
}

private fun buildFootprint(
    heritage: Heritage,
    scale: Float,
    part: BuildingPart
): Polygon {
    val partScale = when (part) {
        BuildingPart.PLAZA -> 1.36
        BuildingPart.BODY -> 1.0
        BuildingPart.ROOF -> 0.62
        BuildingPart.TOWER -> 0.28
    }
    val size = BUILT_BUILDING_SIZE_METERS * scale * partScale
    val variant = heritage.id % 5
    val baseOffsets = when (variant) {
        0 -> listOf(-0.74 to -0.44, 0.74 to -0.44, 0.74 to 0.44, -0.74 to 0.44)
        1 -> listOf(0.0 to -0.72, 0.72 to 0.0, 0.0 to 0.72, -0.72 to 0.0)
        2 -> (0 until 8).map { i ->
            val angle = PI * 2.0 * i / 8.0 + PI / 8.0
            cos(angle) * 0.68 to sin(angle) * 0.68
        }
        3 -> listOf(-0.72 to -0.72, 0.72 to -0.72, 0.72 to -0.18, 0.18 to -0.18, 0.18 to 0.72, -0.72 to 0.72)
        else -> listOf(-0.46 to -0.78, 0.46 to -0.78, 0.68 to 0.0, 0.46 to 0.78, -0.46 to 0.78, -0.68 to 0.0)
    }
    val offsets = when (part) {
        BuildingPart.PLAZA -> baseOffsets
        BuildingPart.BODY -> baseOffsets
        BuildingPart.ROOF -> baseOffsets.map { (x, y) -> x * 0.72 to y * 0.72 }
        BuildingPart.TOWER -> {
            val towerCenter = when (variant) {
                0 -> 0.34 to -0.18
                1 -> 0.0 to -0.34
                2 -> 0.24 to 0.24
                3 -> -0.28 to -0.28
                else -> -0.18 to 0.28
            }
            (0 until 6).map { i ->
                val angle = PI * 2.0 * i / 6.0 + PI / 6.0
                towerCenter.first + cos(angle) * 0.5 to towerCenter.second + sin(angle) * 0.5
            }
        }
    }
    val ring = offsets.map { (x, y) ->
        metersToPoint(heritage.lng, heritage.lat, eastMeters = x * size, northMeters = y * size)
    }.let { it + it.first() }
    return Polygon.fromLngLats(listOf(ring))
}

private fun metersToPoint(
    lng: Double,
    lat: Double,
    eastMeters: Double,
    northMeters: Double
): Point {
    val lngPerMeter = 1.0 / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.2))
    val latPerMeter = 1.0 / 111_320.0
    return Point.fromLngLat(
        lng + eastMeters * lngPerMeter,
        lat + northMeters * latPerMeter
    )
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
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
