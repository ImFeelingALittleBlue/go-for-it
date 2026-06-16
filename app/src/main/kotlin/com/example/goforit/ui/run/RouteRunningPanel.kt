package com.example.goforit.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.Heritage
import com.example.goforit.ui.home.OrangeAccent
import com.example.goforit.ui.home.TextGray
import com.mapbox.geojson.Point
import kotlin.math.*

// 計算路線上的古蹟停靠點：距路線 200m 以內的古蹟，依路線前後順序排列
fun heritagesOnRoute(
    routePoints: List<Point>,
    allHeritages: List<Heritage>,
    radiusM: Double = 200.0
): List<Pair<Heritage, Int>> {
    if (routePoints.isEmpty()) return emptyList()
    return allHeritages.mapNotNull { h ->
        var minDist = Double.MAX_VALUE
        var minIdx  = -1
        routePoints.forEachIndexed { idx, pt ->
            val d = routePanelHaversine(pt.latitude(), pt.longitude(), h.lat, h.lng)
            if (d < minDist) { minDist = d; minIdx = idx }
        }
        if (minDist <= radiusM) Pair(h, minIdx) else null
    }.sortedBy { it.second }
}

// ─── 頂部面板：距離 + 時間 + 銀鹽 ─────────────────────────────────────────────
@Composable
fun RouteRunningTopPanel(liveSilver: Int, coveredKm: Float, elapsedSeconds: Int) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.White)
        .padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${"%.1f".format(coveredKm)} km", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("距離", fontSize = 11.sp, color = TextGray)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatOverlayTime(elapsedSeconds), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("時間", fontSize = 11.sp, color = TextGray)
        }
    }
    RunSilverRow(liveSilver)
}

// ─── 底部面板：停靠點列表 + toggle + play/pause + 暫停跑步 ────────────────────
// autoTriggerEnabled：toggle「到達地點後自動播放對應音檔」的狀態
// toggle ON  → 跑到古蹟 40m 自動觸發，使用者也可點卡片手動播
// toggle OFF → 使用者手動選；選完後自動依順序接續播（不觸發 GPS）
@Composable
fun RouteRunningBottomPanel(
    routePoints: List<Point>,
    heritages: List<Heritage>,
    planHeritages: List<Heritage> = emptyList(),
    unlockedIds: Set<Int>,
    playedIds: Set<Int> = emptySet(),
    activePodcastHeritage: Heritage? = null,
    activeLine: DialogueLine? = null,
    autoTriggerEnabled: Boolean = false,
    onAutoTriggerToggle: (Boolean) -> Unit = {},
    onPlayHeritage: (Heritage) -> Unit = {},   // 點卡片
    onPlayPause: () -> Unit = {},              // play / pause 按鈕
    onPause: () -> Unit                        // 暫停跑步（觸發結束對話框）
) {
    // 規劃模式：使用者點選的順序；路線模式：沿 GPX 從起點排序
    val stops = remember(routePoints, planHeritages) {
        if (planHeritages.isNotEmpty()) planHeritages.mapIndexed { i, h -> Pair(h, i) }
        else heritagesOnRoute(routePoints, heritages)
    }
    val isPlaying = activePodcastHeritage != null

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {

        // ── 停靠點列表（左側時間軸 + 卡片）──────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            if (stops.isEmpty()) {
                item {
                    Text("路線沿途無古蹟停靠點", fontSize = 12.sp, color = TextGray,
                        modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                itemsIndexed(stops, key = { _, pair -> pair.first.id }) { idx, (h, _) ->
                    val isActive  = h.id == activePodcastHeritage?.id
                    val hasPlayed = h.id in playedIds
                    val isLast    = idx == stops.size - 1
                    val dotColor  = when {
                        isActive  -> OrangeAccent
                        hasPlayed -> Color(0xFF4CAF50)
                        else      -> Color(0xFFCCCCCC)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // 時間軸：圓點 + 向下連接線
                        Column(
                            modifier = Modifier.width(20.dp).padding(top = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier
                                .size(if (isActive) 12.dp else 8.dp)
                                .background(dotColor, CircleShape))
                            if (!isLast) Box(modifier = Modifier
                                .width(2.dp).height(58.dp)
                                .background(Color(0xFFE0E0E0)))
                        }
                        Spacer(Modifier.width(10.dp))
                        // 古蹟資訊卡
                        Surface(
                            onClick  = { onPlayHeritage(h) },
                            modifier = Modifier.weight(1f)
                                .padding(bottom = if (isLast) 0.dp else 6.dp),
                            shape    = RoundedCornerShape(8.dp),
                            color    = if (isActive) Color(0xFFFFF3E0) else Color(0xFFF5F5F5)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (isActive) OrangeAccent else Color(0xFF2C2218))
                                    when {
                                        isActive && activeLine != null -> {
                                            val role = if (activeLine.speaker == PodcastSpeaker.HOST_B)
                                                "文史達人" else "主持人"
                                            Text("$role・${activeLine.text.take(22)}…",
                                                fontSize = 10.sp, color = Color(0xFF888888), maxLines = 1)
                                        }
                                        isActive -> Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(Modifier.size(10.dp),
                                                strokeWidth = 1.5.dp, color = OrangeAccent)
                                            Spacer(Modifier.width(4.dp))
                                            Text("生成中…", fontSize = 10.sp, color = Color(0xFF888888))
                                        }
                                        h.year.isNotBlank() ->
                                            Text(h.year, fontSize = 11.sp, color = Color(0xFF888888))
                                    }
                                }
                                when {
                                    isActive  -> Text("▶", fontSize = 14.sp, color = OrangeAccent)
                                    hasPlayed -> Text("✓", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF0EDE8), thickness = 1.dp)

        // ── Toggle：到達地點後自動播放 ────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("到達地點後自動播放對應音檔",
                modifier = Modifier.weight(1f), fontSize = 13.sp, color = Color(0xFF2C2218))
            Switch(
                checked = autoTriggerEnabled,
                onCheckedChange = onAutoTriggerToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor   = OrangeAccent,
                    uncheckedTrackColor = Color(0xFFCCCCCC)
                )
            )
        }

        // ── Podcast play / pause 按鈕 ─────────────────────────────────────
        Button(
            onClick = onPlayPause,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 64.dp)
                .fillMaxWidth()
                .height(48.dp),
            shape  = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
        ) {
            Text(if (isPlaying) "⏸  pause" else "▶  play",
                color = Color(0xFF2C2218), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onPause, modifier = Modifier.fillMaxWidth().height(36.dp)) {
            Text("暫停跑步", color = TextGray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun routePanelHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2).pow(2) + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2).pow(2)
    return 2 * R * atan2(sqrt(a), sqrt(1 - a))
}
