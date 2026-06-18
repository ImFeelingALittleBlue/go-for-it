package com.example.goforit.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.Heritage
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RestorationRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 暫時假資料（組員建好 Room DB 後換成從資料庫讀取）
// Triple = (名稱, 時期•年份, 是否已解鎖)
private val sampleSites = listOf(
    Triple("蓬萊咖啡館", "日治時期 • 1930", false),
    Triple("臺南圖書館", "日治時期 • 1930", false),
    Triple("臺南郵便局", "日治時期 • 1915", false),
    Triple("赤崁樓",     "明鄭時期 • 1653", true),
)

@Composable
fun MapHeritageSection(
    heritages: List<Heritage>,
    records: List<RestorationRecord>,
    selectedFilter: HeritageFilter,
    onFilterChange: (HeritageFilter) -> Unit,
    onHeritageClick: (Heritage) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    filterHeritages: Boolean = true,
    chipHeritages: List<Heritage> = heritages,
    chipSelectedFilter: HeritageFilter = selectedFilter
) {
    val restoredAtById = records.associate { it.heritageId to it.restoredAt }
    val restoredCount = chipHeritages.count { it.id in restoredAtById }
    val pendingCount = chipHeritages.size - restoredCount
    // 列表搜尋字串：只過濾下方清單，不影響地圖標記
    var searchQuery by remember { mutableStateOf("") }
    val statusFiltered = if (!filterHeritages) {
        heritages
    } else {
        when (selectedFilter) {
            HeritageFilter.ALL -> heritages
            HeritageFilter.RESTORED -> heritages.filter { it.id in restoredAtById }
            HeritageFilter.PENDING -> heritages.filter { it.id !in restoredAtById }
        }
    }
    val query = searchQuery.trim()
    val filteredHeritages = if (query.isBlank()) {
        statusFiltered
    } else {
        statusFiltered.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF3EEE6))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("古蹟與遺址", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            // 展開時箭頭朝下，收合時轉 180° 朝上
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "收合" else "展開",
                tint = TextGray,
                modifier = Modifier.rotate(if (expanded) 0f else 180f)
            )
        }

        if (expanded) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    StatusChip(
                        text = "全部 ${chipHeritages.size}",
                        active = chipSelectedFilter == HeritageFilter.ALL,
                        onClick = { onFilterChange(HeritageFilter.ALL) }
                    )
                }
                item {
                    StatusChip(
                        text = "已修復 $restoredCount",
                        active = chipSelectedFilter == HeritageFilter.RESTORED,
                        indicatorColor = OrangeAccent,          // 橘色：已修復
                        onClick = { onFilterChange(HeritageFilter.RESTORED) }
                    )
                }
                item {
                    StatusChip(
                        text = "待修復 $pendingCount",
                        active = chipSelectedFilter == HeritageFilter.PENDING,
                        indicatorColor = Color(0xFF888888),     // 灰色：待修復
                        onClick = { onFilterChange(HeritageFilter.PENDING) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 古蹟名稱搜尋框：即時過濾下方清單
            HeritageSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            Spacer(Modifier.height(4.dp))

            if (filteredHeritages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("沒有符合條件的古蹟", color = TextGray, fontSize = 14.sp)
                }
            } else {
                LazyColumn {
                    items(filteredHeritages, key = { it.id }) { heritage ->
                        MapHeritageItem(
                            heritage = heritage,
                            restoredAt = restoredAtById[heritage.id] ?: 0L,
                            onClick = { onHeritageClick(heritage) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RestoredHeritageSection(
    heritages: List<Heritage>,
    records: List<RestorationRecord>,
    onHeritageClick: (Heritage) -> Unit,
    modifier: Modifier = Modifier
) {
    MapHeritageSection(
        heritages = heritages,
        records = records,
        selectedFilter = HeritageFilter.RESTORED,
        onFilterChange = {},
        onHeritageClick = onHeritageClick,
        modifier = modifier
    )
}

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
        // 只有少量資料，用 Column 即可；LazyColumn 不能放在 verticalScroll 裡
        Column {
            sampleSites.forEach { (name, period, unlocked) ->
                HeritageItem(name = name, period = period, unlocked = unlocked)
            }
        }
    }
}

// 古蹟名稱搜尋框
@Composable
private fun HeritageSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("搜尋古蹟名稱", color = TextGray, fontSize = 13.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "清除", tint = TextGray)
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ChipBg,
            unfocusedContainerColor = ChipBg,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

// 篩選標籤元件
@Composable
fun StatusChip(
    text: String,
    active: Boolean,
    indicatorColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (active) Color(0xFF6F6B65) else Color(0xFFFFFDF8)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (indicatorColor != null) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = RoundedCornerShape(50),
                    color = indicatorColor
                ) {}
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                color = if (active) Color.White else Color(0xFF7A756F)
            )
        }
    }
}

@Composable
private fun MapHeritageItem(
    heritage: Heritage,
    restoredAt: Long,
    onClick: () -> Unit
) {
    val restored = restoredAt > 0L
    val silverSaltReady = !restored && RestorationRepository.isDebugSilverSaltReady(heritage.id)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color(0xFFFFFDF8),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(heritage.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF342820))
                Spacer(Modifier.height(2.dp))
                Text(
                    when {
                        restored -> "解鎖於 ${formatUnlockTime(restoredAt)}"
                        silverSaltReady -> "可使用時光銀鹽解鎖"
                        else -> heritage.year.ifBlank { "待修復" }
                    },
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (restored || silverSaltReady) Color(0xFFEDE3D9) else Color(0xFFF0F0EF)
            ) {
                Text(
                    text = when {
                        restored -> "已修復"
                        silverSaltReady -> "可解鎖"
                        else -> "未解鎖"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = if (restored || silverSaltReady) OrangeAccent else Color(0xFF7A756F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
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

private fun formatUnlockTime(millis: Long): String =
    if (millis > 0L) SimpleDateFormat("M月d日 HH:mm", Locale.TAIWAN).format(Date(millis))
    else "未知時間"
