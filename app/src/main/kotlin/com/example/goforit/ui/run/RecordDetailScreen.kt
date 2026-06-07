package com.example.goforit.ui.run

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.goforit.data.RouteRecord
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 單筆跑步紀錄的詳情頁（從紀錄頁點擊進入）
@Composable
fun RecordDetailScreen(
    record: RouteRecord,
    onBack: () -> Unit,
    onRunAgain: (List<Point>, String, Float) -> Unit = { _, _, _ -> }
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    var routePoints    by remember { mutableStateOf<List<Point>>(emptyList()) }
    var routeDrawn     by remember { mutableStateOf(false) }
    var showShareSheet   by remember { mutableStateOf(false) }
    var snapBitmap       by remember { mutableStateOf<Bitmap?>(null) }
    var generatingShare  by remember { mutableStateOf(false) }

    // 背景解析 GPX 取得路線座標
    LaunchedEffect(record.gpxContent) {
        if (record.gpxContent.isNotBlank()) {
            try {
                val result = withContext(Dispatchers.IO) {
                    record.gpxContent.byteInputStream().use { parseGpx(it) }
                }
                routePoints = result.points
            } catch (_: Exception) { }
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

    val dateStr = remember(record.recordedAt) {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(record.recordedAt))
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F0EB))) {
        // 頂部列
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 8.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Column(modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(dateStr, fontSize = 11.sp, color = Color(0xFF888888))
            }
        }
        // 地圖（路線解析完成後畫折線）
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            AndroidView(
                factory = { mapView.apply { mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { applyWarmMapStyle(it) } } },
                update = { mv ->
                    if (routePoints.size >= 2 && !routeDrawn) {
                        routeDrawn = true
                        mv.mapboxMap.setCamera(CameraOptions.Builder()
                            .center(routePoints[routePoints.size / 2]).zoom(13.0).build())
                        try {
                            mv.annotations.createPolylineAnnotationManager().create(
                                PolylineAnnotationOptions().withPoints(routePoints)
                                    .withLineColor("#D4822A").withLineWidth(4.0))
                        } catch (_: Exception) { }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            // AI Podcast 佔位卡
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color(0xFF2C2218)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color(0xFF3D2E1C)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("▶", color = Color(0xFFD4A96A), fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("AI 旁白 Podcast", fontSize = 11.sp, color = Color(0xFF9E8E7A))
                        Text(record.name, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("$dateStr 的探索記錄", fontSize = 11.sp, color = Color(0xFF9E8E7A))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 統計數字
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color.White) {
                Row(modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("+${record.silverEarned}", "時光銀鹽")
                    StatItem("${record.unlockedCount}", "解鎖古蹟")
                    StatItem(fmtDist(record.distanceMeters), "距離")
                    if (record.elapsedSeconds > 0) StatItem(fmtTime(record.elapsedSeconds), "時間")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        // 底部按鈕列
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { if (routePoints.isNotEmpty()) onRunAgain(routePoints, record.gpxContent, (record.distanceMeters / 1000).toFloat()) },
                enabled = routePoints.isNotEmpty(),
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)
            ) { Text("再跑一次", fontSize = 14.sp) }
            Button(
                onClick = {
                    generatingShare = true
                    // 截取 MapView 目前畫面（地圖底圖 + 路線折線已一起渲染）
                    captureMapSnapshot(mapView) { bmp ->
                        snapBitmap = bmp
                        generatingShare = false
                        showShareSheet = true
                    }
                },
                enabled = !generatingShare,
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text(if (generatingShare) "生成中..." else "分享", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
    if (showShareSheet) {
        snapBitmap?.let { bmp ->
            ShareBottomSheet(routeName = record.name, mapBitmap = bmp, onDismiss = { showShareSheet = false })
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
        Text(label, fontSize = 11.sp, color = Color(0xFF888888))
    }
}

private fun fmtDist(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)

private fun fmtTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
