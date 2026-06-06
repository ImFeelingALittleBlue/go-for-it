package com.example.goforit.ui.collection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.RouteRecord
import com.example.goforit.data.RouteRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Cream = Color(0xFFF3EFE7)
private val Brown = Color(0xFF8A5A2B)
private val Gray = Color(0xFF999999)

// 紀錄頁：顯示跑過的 GPX 路線，而不是已修復古蹟。
@Composable
fun CollectionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val records = RouteRepository.records()

    var query by remember { mutableStateOf("") }
    val filtered = records.filter { it.name.contains(query, ignoreCase = true) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch { RouteRepository.addGpx(context, uri) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        SearchField(query = query, onChange = { query = it })
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { launcher.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("匯入 GPX 路線")
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyHint()
        } else {
            val today = filtered.filter { isToday(it.recordedAt) }
            val past = filtered.filter { !isToday(it.recordedAt) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (today.isNotEmpty()) {
                    item { SectionLabel(todayLabel()) }
                    items(today, key = { it.docId }) { RouteCard(it) }
                }
                if (past.isNotEmpty()) {
                    item { SectionLabel("過去紀錄") }
                    items(past, key = { it.docId }) { RouteCard(it) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Gray)
            TextField(
                value = query,
                onValueChange = onChange,
                placeholder = { Text("搜尋你的路線", color = Gray, fontSize = 14.sp) },
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

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, color = Gray, fontWeight = FontWeight.Medium)
}

// 單筆跑步紀錄卡片：愛心收藏 + 刪除
@Composable
private fun RouteCard(record: RouteRecord) {
    Surface(shape = RoundedCornerShape(16.dp), color = Cream) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("${formatDistance(record.distanceMeters)} · ${record.pointCount} 點",
                    fontSize = 12.sp, color = Brown)
                Spacer(Modifier.height(4.dp))
                Text(formatTime(record.recordedAt), fontSize = 12.sp, color = Gray)
            }
            // 愛心：按下後出現在「已儲存路線」，再按取消收藏
            IconButton(onClick = { RouteRepository.toggleLike(record.docId, !record.liked) }) {
                Icon(
                    imageVector = if (record.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (record.liked) "取消收藏" else "收藏",
                    tint = if (record.liked) Color(0xFFE91E63) else Gray
                )
            }
            // 刪除此筆紀錄
            IconButton(onClick = { RouteRepository.delete(record.docId) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "刪除", tint = Gray)
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("還沒有路線紀錄\n匯入跑過的 GPX 路線吧", color = Gray, fontSize = 14.sp)
    }
}

// ── 時間工具 ──────────────────────────────────────────────────────────────
private fun isToday(millis: Long): Boolean {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
}

private fun todayLabel(): String =
    SimpleDateFormat("M月d日 · 今天", Locale.TAIWAN).format(Date())

private fun formatTime(millis: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.TAIWAN).format(Date(millis))

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) String.format(Locale.TAIWAN, "%.2f km", meters / 1000)
    else String.format(Locale.TAIWAN, "%.0f m", meters)

private fun startedAtText(millis: Long): String =
    if (millis > 0L) " · 起跑 ${formatTime(millis)}" else ""
