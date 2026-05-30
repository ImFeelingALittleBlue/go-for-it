package com.example.goforit.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 暫時假資料（組員建好 Room DB 後換成從資料庫讀取）
// Triple = (名稱, 時期•年份, 是否已解鎖)
private val sampleSites = listOf(
    Triple("蓬萊咖啡館", "日治時期 • 1930", false),
    Triple("臺南圖書館", "日治時期 • 1930", false),
    Triple("臺南郵便局", "日治時期 • 1915", false),
    Triple("赤崁樓",     "明鄭時期 • 1653", true),
)

@Composable
fun NearbyHeritageSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ── 區塊標題列 ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("附近古蹟與遺址", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "展開", tint = TextGray)
        }

        // ── 篩選標籤 ─────────────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { StatusChip(text = "未解鎖 3", active = false) }
            item { StatusChip(text = "未得 5",   active = false) }
            item { StatusChip(text = "已得 2",   active = true)  }
        }

        Spacer(Modifier.height(4.dp))

        // ── 古蹟列表 ─────────────────────────────────────────────────────────
        LazyColumn {
            items(sampleSites.size) { i ->
                val (name, period, unlocked) = sampleSites[i]
                HeritageItem(name = name, period = period, unlocked = unlocked)
                HorizontalDivider(color = ChipBg, thickness = 1.dp)
            }
        }
    }
}

// 篩選標籤元件
@Composable
fun StatusChip(text: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (active) OrangeAccent else ChipBg
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = if (active) Color.White else Color(0xFF1A1A1A)
        )
    }
}

// 單一古蹟列表項目
@Composable
fun HeritageItem(name: String, period: String, unlocked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(period, fontSize = 12.sp, color = TextGray)
        }
        // 解鎖狀態徽章
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (unlocked) Color(0xFF4CAF50) else OrangeAccent
        ) {
            Text(
                text = if (unlocked) "已解鎖" else "未解鎖",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
