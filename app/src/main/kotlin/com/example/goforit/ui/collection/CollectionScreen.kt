package com.example.goforit.ui.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.RestorationRecord
import com.example.goforit.data.RestorationRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Cream = Color(0xFFF3EFE7)
private val Brown = Color(0xFF8A5A2B)
private val Gray = Color(0xFF999999)

// 收藏頁：顯示已修復古蹟的紀錄（含完成時間，可刪除）
@Composable
fun CollectionScreen() {
    // 觀察雲端紀錄清單（新增/刪除會自動更新）
    val records = RestorationRepository.records()

    // 搜尋字串：依古蹟名稱過濾
    var query by remember { mutableStateOf("") }
    val filtered = records.filter { it.name.contains(query, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        SearchField(query = query, onChange = { query = it })
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyHint()
        } else {
            // 依「今天 / 過去」分兩組顯示
            val today = filtered.filter { isToday(it.restoredAt) }
            val past = filtered.filter { !isToday(it.restoredAt) }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (today.isNotEmpty()) {
                    item { SectionLabel(todayLabel()) }
                    items(today, key = { it.docId }) { RecordCard(it) }
                }
                if (past.isNotEmpty()) {
                    item { SectionLabel("過去紀錄") }
                    items(past, key = { it.docId }) { RecordCard(it) }
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
                placeholder = { Text("搜尋你的紀錄", color = Gray, fontSize = 14.sp) },
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

// 單筆修復紀錄卡片
@Composable
private fun RecordCard(record: RestorationRecord) {
    Surface(shape = RoundedCornerShape(16.dp), color = Cream) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("修復於 ${formatTime(record.restoredAt)}", fontSize = 12.sp, color = Brown)
                Spacer(Modifier.height(4.dp))
                Text(
                    record.description,
                    fontSize = 12.sp,
                    color = Gray,
                    maxLines = 2
                )
            }
            // 刪除按鈕
            IconButton(onClick = { RestorationRepository.delete(record.docId) }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "刪除", tint = Gray)
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("還沒有修復任何古蹟\n到地圖上花費時光銀鹽修復吧", color = Gray, fontSize = 14.sp)
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
