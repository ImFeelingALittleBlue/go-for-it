package com.example.goforit.ui.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.goforit.R
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

// 規劃路線畫面：在地圖上點選古蹟點，透過 Mapbox Directions API 計算可行走的步行路線
@Composable
fun RoutePlanScreen(
    onBack: () -> Unit,
    onRouteDone: (points: List<Point>, distanceKm: Float, heritages: List<Heritage>) -> Unit
) {
    val context         = LocalContext.current
    val lifecycleOwner  = LocalLifecycleOwner.current
    val mapView         = remember { MapView(context) }
    val heritages       = remember { HeritageRepository.loadHeritages(context) }
    val waypoints        = remember { mutableStateListOf<Point>() }
    // 與 waypoints 一一對應：吸附到古蹟時存 Heritage，否則 null（用於直接傳給 RunScreen）
    val snappedHeritages = remember { mutableStateListOf<Heritage?>() }
    var routePoints     by remember { mutableStateOf<List<Point>>(emptyList()) }
    var distanceKm      by remember { mutableStateOf(0f) }
    var isLoading       by remember { mutableStateOf(false) }
    // 三層 annotation manager（由下到上：古蹟點 → 路線折線 → 路線點）
    val heritageManager = remember { mutableStateOf<CircleAnnotationManager?>(null) }
    val polylineManager = remember { mutableStateOf<PolylineAnnotationManager?>(null) }
    val circleManager   = remember { mutableStateOf<CircleAnnotationManager?>(null) }
    // 點選古蹟時浮出的名稱提示框
    var tooltipHeritage by remember { mutableStateOf<Heritage?>(null) }
    var tooltipX        by remember { mutableStateOf(0f) }
    var tooltipY        by remember { mutableStateOf(0f) }

    // 提示框 2 秒後自動消失
    LaunchedEffect(tooltipHeritage) {
        if (tooltipHeritage != null) {
            delay(2_000L)
            tooltipHeritage = null
        }
    }

    // waypoints 變更 → 呼叫 Mapbox Directions API 取可行走路線
    LaunchedEffect(waypoints.toList()) {
        if (waypoints.size >= 2) {
            isLoading = true
            val token  = context.getString(R.string.mapbox_access_token)
            val result = withContext(Dispatchers.IO) {
                fetchWalkingRoute(token, waypoints.toList())
            }
            routePoints = result?.first ?: emptyList()
            distanceKm  = result?.second ?: 0f
            isLoading   = false
        } else {
            routePoints = emptyList()
            distanceKm  = 0f
        }
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
        // ── 頂部列 ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 8.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Column(modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("規劃路線", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    if (waypoints.isEmpty()) "點擊古蹟加入路線點"
                    else "已選 ${waypoints.size} 個點",
                    fontSize = 12.sp, color = Color(0xFF888888)
                )
            }
        }

        // ── 地圖（主要操作區域）────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                            applyWarmMapStyle(style)
                            mapboxMap.setCamera(CameraOptions.Builder()
                                .center(Point.fromLngLat(120.2028, 23.0000)).zoom(13.0).build())
                            // 最底層：古蹟灰點（與首頁相同）
                            heritageManager.value = annotations.createCircleAnnotationManager()
                            heritageManager.value?.create(heritages.map { h ->
                                CircleAnnotationOptions()
                                    .withPoint(Point.fromLngLat(h.lng, h.lat))
                                    .withCircleRadius(7.0).withCircleColor("#888888")
                                    .withCircleStrokeWidth(1.5).withCircleStrokeColor("#FFFFFF")
                            })
                            // 中層：Directions API 回傳的路線折線
                            polylineManager.value = annotations.createPolylineAnnotationManager()
                            // 最上層：使用者選取的路線點
                            circleManager.value   = annotations.createCircleAnnotationManager()
                            // 點擊地圖：只允許點選 80m 內的古蹟標點，空白處不加點
                            mapboxMap.addOnMapClickListener { tapped ->
                                val snapped = heritages
                                    .minByOrNull { h -> planDistM(tapped.latitude(), tapped.longitude(), h.lat, h.lng) }
                                    ?.takeIf { h -> planDistM(tapped.latitude(), tapped.longitude(), h.lat, h.lng) < 80.0 }
                                if (snapped == null) return@addOnMapClickListener false  // 空白處忽略
                                waypoints.add(Point.fromLngLat(snapped.lng, snapped.lat))
                                snappedHeritages.add(snapped)
                                val px = mapboxMap.pixelForCoordinate(Point.fromLngLat(snapped.lng, snapped.lat))
                                tooltipX = px.x.toFloat()
                                tooltipY = px.y.toFloat()
                                tooltipHeritage = snapped
                                true
                            }
                        }
                    }
                },
                update = { _ ->
                    // 重繪 Directions API 路線折線
                    polylineManager.value?.let { pm ->
                        pm.deleteAll()
                        if (routePoints.size >= 2) pm.create(
                            PolylineAnnotationOptions().withPoints(routePoints)
                                .withLineColor("#D4822A").withLineWidth(4.0))
                    }
                    // 重繪使用者路線點（起點綠、終點紅、中間橘）
                    circleManager.value?.let { cm ->
                        cm.deleteAll()
                        val pts = waypoints.toList()
                        pts.forEachIndexed { i, pt ->
                            val color = when (i) {
                                0            -> "#4CAF50"
                                pts.size - 1 -> "#E53935"
                                else         -> "#D4822A"
                            }
                            cm.create(CircleAnnotationOptions().withPoint(pt)
                                .withCircleRadius(10.0).withCircleColor(color)
                                .withCircleStrokeWidth(2.0).withCircleStrokeColor("#FFFFFF"))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isLoading) CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = OrangeAccent, strokeWidth = 3.dp)
            // 古蹟名稱浮動提示框（點擊古蹟後出現，點其他地方消失）
            tooltipHeritage?.let { h ->
                HeritageNameTooltip(name = h.name, x = tooltipX, y = tooltipY,
                    onDismiss = { tooltipHeritage = null })
            }
        }

        // ── 底部面板：距離 + 撤銷 / 完成 ──────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, tonalElevation = 4.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (distanceKm > 0f) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF5C3D1E)) {
                            Text("路線距離 ${"%.2f".format(distanceKm)} km",
                                color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (waypoints.isNotEmpty()) {
                                waypoints.removeLast()
                                snappedHeritages.removeLast()
                            }
                        },
                        enabled = waypoints.isNotEmpty(),
                        modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)
                    ) { Text("撤銷上一點", fontSize = 13.sp) }
                    Button(
                        onClick = {
                            val pts = routePoints.ifEmpty { waypoints.toList() }
                            if (pts.isNotEmpty())
                                // 傳吸附到的古蹟清單（filterNotNull 排除沒吸附的點）
                                onRouteDone(pts, distanceKm, snappedHeritages.filterNotNull())
                        },
                        enabled = waypoints.size >= 2 && !isLoading,
                        modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
                    ) { Text("完成路線", color = Color.White, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// 勾股定理估算兩點距離（米），精度足夠用於 80m 吸附判斷
private fun planDistM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dlat = (lat1 - lat2) * 111_000
    val dlng = (lon1 - lon2) * 111_000 * cos(Math.toRadians(lat1))
    return sqrt(dlat * dlat + dlng * dlng)
}

// 呼叫 Mapbox Directions API（步行模式）：回傳 (路線點列表, 距離km)；失敗回傳 null
private fun fetchWalkingRoute(token: String, pts: List<Point>): Pair<List<Point>, Float>? {
    return try {
        val coords = pts.joinToString(";") { "${it.longitude()},${it.latitude()}" }
        val url    = "https://api.mapbox.com/directions/v5/mapbox/walking/$coords" +
                     "?access_token=$token&geometries=geojson&overview=full"
        val conn   = (URL(url).openConnection() as HttpURLConnection)
            .also { it.connectTimeout = 8_000; it.readTimeout = 8_000 }
        val body     = conn.inputStream.bufferedReader().readText()
        val route    = JSONObject(body).getJSONArray("routes").getJSONObject(0)
        val distM    = route.getDouble("distance").toFloat()
        val coordArr = route.getJSONObject("geometry").getJSONArray("coordinates")
        val routePts = (0 until coordArr.length()).map { i ->
            coordArr.getJSONArray(i).let { c -> Point.fromLngLat(c.getDouble(0), c.getDouble(1)) }
        }
        Pair(routePts, distM / 1000f)
    } catch (e: Exception) { null }
}
