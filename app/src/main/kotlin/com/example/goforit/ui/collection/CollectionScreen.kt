package com.example.goforit.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.goforit.data.PendingRunStore
import com.example.goforit.data.RouteRecord
import com.example.goforit.data.RouteRepository
import com.example.goforit.navigation.Screen
import com.example.goforit.ui.home.OrangeAccent
import com.example.goforit.ui.run.RecordDetailScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Cream  = Color(0xFFF5F0EB)
private val Gray   = Color(0xFF999999)
private val Brown  = Color(0xFF8A5A2B)

// 紀錄頁：顯示跑過的路線，點擊查看結算詳情
@Composable
fun CollectionScreen(navController: NavController) {
    val records = RouteRepository.records()
    var query           by remember { mutableStateOf("") }
    var selectedRecord  by remember { mutableStateOf<RouteRecord?>(null) }

    // 點了某筆紀錄 → 顯示詳情畫面
    selectedRecord?.let { record ->
        RecordDetailScreen(
            record    = record,
            onBack    = { selectedRecord = null },
            onRunAgain = { pts, gpx, dist ->
                // 存入暫存 → 切到去探索頁 → RunScreen 偵測後跳路線預覽
                PendingRunStore.set(pts, gpx, dist)
                selectedRecord = null
                navController.navigate(Screen.Run.route)
            }
        )
        return
    }

    val filtered = records.filter { it.name.contains(query, ignoreCase = true) }
    // 依年月分組，最新在最上面
    val grouped = filtered
        .groupBy { SimpleDateFormat("yyyy-MM", Locale.TAIWAN).format(Date(it.recordedAt)) }
        .entries.sortedByDescending { it.key }

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        SearchField(query = query, onChange = { query = it })
        if (filtered.isEmpty()) {
            EmptyHint()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                for ((yearMonth, recs) in grouped) {
                    item(key = "header-$yearMonth") { MonthHeader(yearMonth, recs.size) }
                    items(recs, key = { it.docId }) { record ->
                        RouteCard(
                            record = record,
                            onClick = { selectedRecord = record },
                            onHeartClick = { RouteRepository.toggleLike(record.docId, !record.liked) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Gray)
            TextField(value = query, onValueChange = onChange,
                placeholder = { Text("搜尋紀錄", color = Gray, fontSize = 14.sp) },
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
}

// 月份標題，例：「5月份 · 3 筆」
@Composable
private fun MonthHeader(yearMonth: String, count: Int) {
    val month = yearMonth.substringAfter("-").toIntOrNull() ?: 0
    Text("${month}月份 · $count 筆",
        fontSize = 13.sp, color = Gray, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 4.dp))
}

// 單筆紀錄卡片
@Composable
private fun RouteCard(record: RouteRecord, onClick: () -> Unit, onHeartClick: () -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = record.recordedAt }
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color.White) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // 左：日期
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(38.dp)) {
                Text("${cal.get(Calendar.DAY_OF_MONTH)}",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
                Text("${cal.get(Calendar.MONTH) + 1}月",
                    fontSize = 10.sp, color = Gray)
            }
            Spacer(Modifier.width(10.dp))
            // 主內容
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row {
                    TagChip("台南市")
                    Spacer(Modifier.width(4.dp))
                    val region = extractRegion(record.name)
                    if (region.isNotBlank()) TagChip(region)
                }
                if (record.unlockedCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    TagChip("${record.unlockedCount} 古蹟解鎖", isOrange = true)
                }
                Spacer(Modifier.height(4.dp))
                Text(formatDist(record.distanceMeters), fontSize = 12.sp, color = Brown)
            }
            Spacer(Modifier.width(8.dp))
            // 右：地圖縮圖佔位 + 愛心
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(60.dp, 44.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFDDD0BF)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Map, null, tint = Color(0xFF9E8E7A),
                        modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onHeartClick, modifier = Modifier.size(36.dp)) {
                    Icon(if (record.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null, tint = if (record.liked) Color(0xFFE91E63) else Gray,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// 標籤 Chip
@Composable
private fun TagChip(text: String, isOrange: Boolean = false) {
    Surface(shape = RoundedCornerShape(8.dp),
        color = if (isOrange) Color(0xFFF5E6CE) else Color(0xFFF0EDE8)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp, color = if (isOrange) OrangeAccent else Gray)
    }
}

@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("還沒有跑步紀錄\n完成一次跑步後就會出現在這裡",
            color = Gray, fontSize = 14.sp)
    }
}

// 從路線名稱「台南中西區探索」提取地區部分「中西區」
private fun extractRegion(name: String): String =
    name.removePrefix("台南").removeSuffix("探索").trim()

private fun formatDist(meters: Double): String =
    if (meters >= 1000) "%.1f km".format(meters / 1000) else "%.0f m".format(meters)
