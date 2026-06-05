package com.example.goforit.ui.run

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.goforit.data.Heritage
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunSummaryScreen(
    trackPoints: List<Point>,
    routePoints: List<Point>,
    coveredKm: Float,
    elapsedSeconds: Int,
    unlockedHeritages: List<Heritage>,
    silverEarned: Int,
    onFinish: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    var showShareSheet by remember { mutableStateOf(false) }

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
            .padding(horizontal = 8.dp, vertical = 12.dp)) {
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Text("跑步結算", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center))
        }

        // ── 地圖 ────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            val center = routePoints.getOrElse(routePoints.size / 2) {
                                Point.fromLngLat(120.2028, 23.0000)
                            }
                            mapboxMap.setCamera(
                                CameraOptions.Builder().center(center).zoom(13.0).build())
                            val mgr = annotations.createPolylineAnnotationManager()
                            if (routePoints.size >= 2)
                                mgr.create(PolylineAnnotationOptions()
                                    .withPoints(routePoints).withLineColor("#C8A46A").withLineWidth(3.0))
                            if (trackPoints.size >= 2)
                                mgr.create(PolylineAnnotationOptions()
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

            // 統計三欄
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color.White) {
                Row(modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    SummaryStatItem("${"%.2f".format(coveredKm)} km", "距離")
                    SummaryStatItem(formatOverlayTime(elapsedSeconds), "時間")
                    SummaryStatItem("+$silverEarned", "時光銀鹽")
                }
            }

            Spacer(Modifier.height(10.dp))

            // AI Podcast 佔位卡
            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp), color = Color(0xFF3A2416)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A),
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("語音導覽生成後將出現在此處",
                        fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {}, enabled = false,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Text("下載 Podcast（功能開發中）", fontSize = 13.sp)
                    }
                }
            }

            // 本次解鎖古蹟
            if (unlockedHeritages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("本次解鎖古蹟", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                    Text("已解鎖", modifier = Modifier.padding(
                                        horizontal = 10.dp, vertical = 4.dp),
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
                shape = RoundedCornerShape(24.dp)) {
                Text("再跑一次", fontSize = 14.sp)
            }
            Button(onClick = { showShareSheet = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)) {
                Text("分享", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // ── 分享底頁：預覽成就圖片 + 儲存/下載 ────────────────────────────
    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            ShareExportSheet(
                coveredKm        = coveredKm,
                elapsedSeconds   = elapsedSeconds,
                unlockedHeritages = unlockedHeritages,
                silverEarned     = silverEarned,
                trackPoints      = trackPoints,
                onDismiss        = { showShareSheet = false }
            )
        }
    }
}

// 分享底頁：預覽成就圖片 + 儲存圖片 / 下載 GPX 兩個動作
@Composable
private fun ShareExportSheet(
    coveredKm: Float,
    elapsedSeconds: Int,
    unlockedHeritages: List<Heritage>,
    silverEarned: Int,
    trackPoints: List<Point>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // 預先產生成就圖片（只算一次）
    val bitmap = remember {
        createShareBitmap(
            coveredKm      = coveredKm,
            timeStr        = formatOverlayTime(elapsedSeconds),
            heritageCount  = unlockedHeritages.size,
            silverEarned   = silverEarned
        )
    }
    var imgMsg by remember { mutableStateOf<String?>(null) }
    var gpxMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("分享此次探索", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        // 成就圖片預覽
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "成就圖片",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().aspectRatio(900f / 480f)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(Modifier.height(20.dp))

        // 儲存圖片
        imgMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFF4CAF50)) }
        Button(
            onClick = {
                imgMsg = if (saveBitmapToGallery(context, bitmap)) "✓ 已儲存至相簿"
                         else "儲存失敗，請再試"
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
        ) {
            Text("儲存圖片", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        // 下載 GPX
        gpxMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFF4CAF50)) }
        OutlinedButton(
            onClick = {
                gpxMsg = if (saveGpxToDownloads(context, trackPoints)) "✓ GPX 已存至下載資料夾"
                          else "下載失敗，請再試"
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("下載 GPX 軌跡", fontSize = 14.sp)
        }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDismiss) {
            Text("關閉", color = Color(0xFF888888))
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
    }
}
