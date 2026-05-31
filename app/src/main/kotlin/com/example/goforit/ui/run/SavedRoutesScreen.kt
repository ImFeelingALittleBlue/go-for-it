package com.example.goforit.ui.run

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
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
import com.example.goforit.ui.home.OrangeAccent
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SavedRoute(
    val name: String, val distanceKm: Float, val unlockSite: String, val description: String
)

private val completedRoutes = listOf(
    SavedRoute("古城晨跑",     4.2f, "赤崁樓",   "赤崁樓與荷蘭時代的故事"),
    SavedRoute("安平古蹟探索", 5.5f, "億載金城", "安平港與清代台灣的海防故事"),
)

// onGpxLoaded：GPX 解析成功後把座標與距離傳回上層（RunScreen）
@Composable
fun SavedRoutesScreen(
    onBack: () -> Unit,
    onGpxLoaded: (List<Point>, Float) -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }

    // 系統檔案選擇器：在主執行緒取得 InputStream，再切到 IO 執行緒解析
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // 在 callback（主執行緒）立刻拿到 stream，避免 URI 在背景執行緒失效
        val stream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            errorMsg = "無法開啟檔案"
            return@rememberLauncherForActivityResult
        } ?: return@rememberLauncherForActivityResult
        isLoading = true
        errorMsg  = null
        scope.launch(Dispatchers.IO) {
            try {
                val result = stream.use { parseGpx(it) }
                withContext(Dispatchers.Main) {
                    isLoading = false
                    onGpxLoaded(result.points, result.distanceKm)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMsg = "解析失敗：${e.message}"
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F0EB))) {

        // ── 頂部列 ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("← 返回", color = Color(0xFF1A1A1A), fontSize = 14.sp)
            }
            Text(
                "已儲存的路線",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ── 搜尋列 ─────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF888888))
                Spacer(Modifier.width(8.dp))
                Text("搜尋你的路線", color = Color(0xFF888888), fontSize = 14.sp)
            }
        }

        // ── 可捲動內容 ─────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("已完成路線", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
            completedRoutes.forEach { route ->
                CompletedRouteCard(route)
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("預先儲存路線", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold, fontSize = 14.sp)
            EmptyRoutesCard()
            Spacer(Modifier.height(16.dp))
            // 解析錯誤提示
            errorMsg?.let {
                Text(it, color = Color(0xFFE53935),
                    modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp)
            }
        }

        // ── 上傳 GPX 按鈕 ──────────────────────────────────────────────────
        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isLoading) "解析中…" else "上傳新路線GPX",
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CompletedRouteCard(route: SavedRoute) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), color = Color.White) {
        Column(Modifier.padding(16.dp)) {
            Text(route.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text("${route.distanceKm} km · 解鎖 ${route.unlockSite}",
                fontSize = 13.sp, color = Color(0xFF888888))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = OrangeAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(route.description, fontSize = 13.sp, color = Color(0xFF888888))
            }
        }
    }
}

@Composable
private fun EmptyRoutesCard() {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp), color = Color.White) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("沒有任何結果", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text("訂閱即可預先儲存路線與生成故事",
                fontSize = 13.sp, color = Color(0xFF888888), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { /* TODO */ },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)) {
                Text("立即訂閱", color = Color.White)
            }
        }
    }
}
