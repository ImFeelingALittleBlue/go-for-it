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
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(
    trackPoints: List<Point>,
    routePoints: List<Point>,
    coveredKm: Float,
    elapsedSeconds: Int,
    unlockedHeritages: List<Heritage>,
    silverEarned: Int,
    initialRouteName: String,
    onFinish: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context, MapInitOptions(context = context, textureView = true)) }
    var showShareSheet   by remember { mutableStateOf(false) }
    var snapBitmap       by remember { mutableStateOf<Bitmap?>(null) }
    var generatingShare  by remember { mutableStateOf(false) }
    var routeName        by remember { mutableStateOf(initialRouteName) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var editingName      by remember { mutableStateOf("") }

    // 路線古蹟：有 GPX 路線時用路線算，直接跑步時用解鎖古蹟代替
    val allHeritages   = remember { HeritageRepository.loadHeritages(context) }
    val routeHeritages = remember(routePoints) {
        if (routePoints.isNotEmpty()) heritagesOnRoute(routePoints, allHeritages).map { it.first }
        else unlockedHeritages
    }
    val region  = remember(trackPoints, routePoints) { detectRegion(trackPoints.ifEmpty { routePoints }) }
    val dateStr = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }

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

        // ── 頂部列 ──────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 8.dp, vertical = 10.dp)) {
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Column(modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(routeName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(2.dp))
                    IconButton(onClick = { editingName = routeName; showRenameDialog = true },
                        modifier = Modifier.size(28.dp)) {
                        Text("✏", fontSize = 13.sp, color = Color(0xFF888888))
                    }
                }
                Text("$dateStr · $region", fontSize = 11.sp, color = Color(0xFF888888))
            }
        }

        // ── 地圖 ────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            val center = routePoints.getOrElse(routePoints.size / 2) {
                                Point.fromLngLat(120.2028, 23.0000) }
                            mapboxMap.setCamera(CameraOptions.Builder().center(center).zoom(13.0).build())
                            val mgr = annotations.createPolylineAnnotationManager()
                            if (routePoints.size >= 2) mgr.create(PolylineAnnotationOptions()
                                .withPoints(routePoints).withLineColor("#C8A46A").withLineWidth(3.0))
                            if (trackPoints.size >= 2) mgr.create(PolylineAnnotationOptions()
                                .withPoints(trackPoints).withLineColor("#D4822A").withLineWidth(5.0))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── 可捲動內容 ──────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))

            // 1. Podcast 卡（深棕色）含下載按鈕
            SummaryPodcastCard(routeName = routeName, routeHeritages = routeHeritages)

            Spacer(Modifier.height(10.dp))

            // 2. 此次獲得銀鹽
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color.White) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("此次獲得銀鹽", fontSize = 13.sp, color = Color(0xFF888888))
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("+$silverEarned", fontSize = 36.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
                        Spacer(Modifier.width(6.dp))
                        Text("銀鹽", fontSize = 14.sp, color = Color(0xFF888888),
                            modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }

            // 3. 經過古蹟列表
            if (unlockedHeritages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("經過古蹟", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        unlockedHeritages.forEach { h ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(h.year.ifBlank { "臺南古蹟" },
                                        fontSize = 12.sp, color = Color(0xFF888888))
                                }
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF4CAF50)) {
                                    Text("✓ 已解鎖",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = Color.White, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF0EDE8), thickness = 1.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 底部按鈕 ────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onFinish,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp)) { Text("再跑一次", fontSize = 14.sp) }
            Button(
                onClick = {
                    generatingShare = true
                    captureMapSnapshot(mapView) { bmp ->
                        snapBitmap = bmp; generatingShare = false; showShareSheet = true
                    }
                },
                enabled = !generatingShare,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text(if (generatingShare) "生成中..." else "分享",
                    color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重新命名路線", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { OutlinedTextField(value = editingName, onValueChange = { editingName = it },
                singleLine = true, label = { Text("路線名稱") }) },
            confirmButton = { Button(onClick = {
                if (editingName.isNotBlank()) routeName = editingName; showRenameDialog = false
            }) { Text("確認") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("取消") } }
        )
    }

    if (showShareSheet) {
        snapBitmap?.let { bmp ->
            ShareBottomSheet(routeName = routeName, mapBitmap = bmp,
                routeHeritages = routeHeritages, onDismiss = { showShareSheet = false })
        }
    }
}

// Podcast 卡：深棕色背景，含下載語音 WAV 功能
@Composable
private fun SummaryPodcastCard(routeName: String, routeHeritages: List<Heritage>) {
    val context = LocalContext.current
    var isExporting    by remember { mutableStateOf(false) }
    var exportDone     by remember { mutableStateOf(false) }
    var exportFailed   by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0 to 0) }

    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), color = Color(0xFF2C2218)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape,
                    color = Color(0xFF3D2E1C)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("▶", color = Color(0xFFD4A96A), fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AI 旁白 Podcast", fontSize = 11.sp, color = Color(0xFF9E8E7A))
                    Text(routeName, fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("含 ${routeHeritages.size} 個古蹟停靠點",
                        fontSize = 11.sp, color = Color(0xFF9E8E7A))
                }
            }
            if (isExporting) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp, color = OrangeAccent)
                    Spacer(Modifier.width(8.dp))
                    Text("合成中... (${exportProgress.first}/${exportProgress.second} 行)",
                        fontSize = 12.sp, color = Color(0xFF9E8E7A))
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (!isExporting && !exportDone && routeHeritages.isNotEmpty()) {
                        isExporting = true; exportFailed = false
                        exportPodcastToWav(context, routeName, routeHeritages,
                            onProgress = { cur, total -> exportProgress = cur to total },
                            onComplete = { ok -> isExporting = false
                                if (ok) exportDone = true else exportFailed = true })
                    }
                },
                enabled = routeHeritages.isNotEmpty() && !isExporting && !exportDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) {
                Text(when {
                    exportDone               -> "✓ 已下載至 Downloads"
                    isExporting              -> "合成中，請稍候..."
                    exportFailed             -> "下載失敗，請重試"
                    routeHeritages.isEmpty() -> "（無路線古蹟）"
                    else                     -> "下載 Podcast"
                }, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 依路線座標推算最接近的台南行政區
internal fun detectRegion(points: List<Point>): String {
    if (points.isEmpty()) return "台南市"
    val lat = points.map { it.latitude() }.average()
    val lng = points.map { it.longitude() }.average()
    return when {
        lng < 120.17                  -> "安平區"
        lat > 23.04                   -> "安南區"
        lat > 23.01 && lng < 120.21   -> "北區"
        lat > 23.01                   -> "永康區"
        lng < 120.20                  -> "中西區"
        lng < 120.22                  -> "東區"
        lat < 22.98                   -> "南區"
        else                          -> "台南市區"
    }
}
