package com.example.goforit.ui.run

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.RouteRecord
import com.example.goforit.data.RouteRepository
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// onGpxLoaded：路線解析完成後把座標、距離、原始 GPX 字串傳回上層（RunScreen）
@Composable
fun SavedRoutesScreen(
    onBack: () -> Unit,
    onGpxLoaded: (List<Point>, Float, String) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // 只顯示使用者在紀錄頁按愛心的路線
    val likedRoutes = RouteRepository.records().filter { it.liked }

    var query      by remember { mutableStateOf("") }
    var isLoading  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }

    val filtered = likedRoutes.filter { it.name.contains(query, ignoreCase = true) }

    // 點選收藏路線時，從儲存的 GPX 內容解析座標，進入路線預覽
    fun loadAndRun(record: RouteRecord) {
        if (record.gpxContent.isBlank()) { errorMsg = "路線資料遺失"; return }
        isLoading = true; errorMsg = null
        scope.launch(Dispatchers.IO) {
            try {
                val result = record.gpxContent.byteInputStream().use { parseGpx(it) }
                withContext(Dispatchers.Main) {
                    isLoading = false
                    onGpxLoaded(result.points, result.distanceKm, record.gpxContent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false; errorMsg = "載入失敗：${e.message}" }
            }
        }
    }

    // 臨時上傳新 GPX：先讀成字串保留原始內容，再解析座標
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val stream = try { context.contentResolver.openInputStream(uri) }
        catch (e: Exception) { errorMsg = "無法開啟檔案"; return@rememberLauncherForActivityResult }
        ?: return@rememberLauncherForActivityResult
        isLoading = true; errorMsg = null
        scope.launch(Dispatchers.IO) {
            try {
                val gpxText = stream.bufferedReader().use { it.readText() }
                val result  = gpxText.byteInputStream().use { parseGpx(it) }
                withContext(Dispatchers.Main) { isLoading = false; onGpxLoaded(result.points, result.distanceKm, gpxText) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false; errorMsg = "解析失敗：${e.message}" }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F0EB))) {

        // ── 頂部列 ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().background(Color.White)
            .padding(horizontal = 8.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Text("已儲存的路線", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center))
        }

        // ── 搜尋列 ──────────────────────────────────────────────────────────
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp), color = Color.White) {
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF888888))
                Spacer(Modifier.width(8.dp))
                TextField(value = query, onValueChange = { query = it },
                    placeholder = { Text("搜尋收藏路線", color = Color(0xFF888888), fontSize = 14.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── 收藏路線清單 ────────────────────────────────────────────────────
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFDDDDDD),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("還沒有收藏的路線", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = Color(0xFF888888))
                    Spacer(Modifier.height(6.dp))
                    Text("在紀錄頁面按 ❤ 即可收藏", fontSize = 13.sp, color = Color(0xFFAAAAAA),
                        textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)
                .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filtered, key = { it.docId }) { record ->
                    LikedRouteCard(record = record, loading = isLoading,
                        onClick = { loadAndRun(record) })
                }
            }
        }

        errorMsg?.let {
            Text(it, color = Color(0xFFE53935), fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // ── 上傳新 GPX（臨時使用，不儲存到雲端）──────────────────────────
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp),
                    color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "載入中…" else "上傳新路線 GPX",
                color = Color.White, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

// 收藏路線卡片：點擊後載入 GPX 進入路線預覽
@Composable
private fun LikedRouteCard(record: RouteRecord, loading: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp), color = Color.White) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63),
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(3.dp))
                Text("${formatSavedDist(record.distanceMeters)} · ${formatSavedDate(record.recordedAt)}",
                    fontSize = 12.sp, color = Color(0xFF888888))
            }
            Text("▶", color = OrangeAccent, fontSize = 16.sp)
        }
    }
}

private fun formatSavedDist(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)

private fun formatSavedDate(millis: Long): String =
    SimpleDateFormat("M月d日", Locale.TAIWAN).format(Date(millis))
