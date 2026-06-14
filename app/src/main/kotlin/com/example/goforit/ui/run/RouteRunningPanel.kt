package com.example.goforit.ui.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// ─── 頂部面板（地圖上方）─────────────────────────────────────────────────────
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

// ─── 底部面板（地圖下方）─────────────────────────────────────────────────────
@Composable
fun RouteRunningBottomPanel(
    routePoints: List<Point>,
    heritages: List<Heritage>,
    planHeritages: List<Heritage> = emptyList(),
    nearestHeritage: Heritage?,
    distanceToNearestM: Float,
    unlockedIds: Set<Int>,
    playedIds: Set<Int> = emptySet(),              // 已觸發播放的古蹟 ID 集合
    activePodcastHeritage: Heritage? = null,
    activeLine: DialogueLine? = null,
    onPlayHeritage: (Heritage) -> Unit = {},       // 點選卡片重播
    onPausePodcast: () -> Unit = {},               // 暫停目前播放
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    // 規劃模式用使用者選取的點；路線模式才跑 heritagesOnRoute
    val stops = remember(routePoints, planHeritages) {
        if (planHeritages.isNotEmpty()) planHeritages.mapIndexed { i, h -> Pair(h, i) }
        else heritagesOnRoute(routePoints, heritages)
    }
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        AIPodcastCard(
            stops                = stops,
            playedIds            = playedIds,
            activePodcastHeritage = activePodcastHeritage,
            activeLine           = activeLine,
            onPlayHeritage       = onPlayHeritage,
            onPausePodcast       = onPausePodcast
        )
        nearestHeritage?.let { NearestHeritageSection(it, distanceToNearestM, unlockedIds) }
        Button(
            onClick = onPause,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
        ) { Text("暫停", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

// ─── AI Podcast 卡片：垂直排列各古蹟，可點選重播，有暫停鍵 ──────────────────
@Composable
private fun AIPodcastCard(
    stops: List<Pair<Heritage, Int>>,
    playedIds: Set<Int>,
    activePodcastHeritage: Heritage?,
    activeLine: DialogueLine?,
    onPlayHeritage: (Heritage) -> Unit,
    onPausePodcast: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF2C2218))
        .padding(horizontal = 14.dp, vertical = 12.dp)) {
        // 標題列 + 暫停鍵
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI 旁白 Podcast", fontSize = 12.sp, color = Color(0xFF9E8E7A),
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (activePodcastHeritage != null) {
                TextButton(onClick = onPausePodcast,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("⏸ 暫停", color = OrangeAccent, fontSize = 12.sp)
                }
            }
        }
        // 目前播放的對話行文字（activeLine == null 表示 API 還在生成）
        if (activePodcastHeritage != null) {
            if (activeLine != null) {
                val role = if (activeLine.speaker == PodcastSpeaker.HOST_B) "文史達人" else "主持人"
                Text("$role・${activeLine.text.take(30)}…",
                    fontSize = 10.sp, color = Color(0xFFD4A96A), maxLines = 1)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp, color = Color(0xFFD4A96A))
                    Text("AI 腳本生成中…", fontSize = 10.sp, color = Color(0xFFD4A96A))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 古蹟卡片列表：LazyColumn 確保多項時可捲動（最高 200dp）
        if (stops.isEmpty()) {
            Text("路線沿途無古蹟停靠點", fontSize = 12.sp, color = Color(0xFF9E8E7A))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(stops, key = { it.first.id }) { (h, _) ->
                    val isActive  = h.id == activePodcastHeritage?.id
                    val hasPlayed = h.id in playedIds
                    Surface(
                        onClick  = { onPlayHeritage(h) },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        color    = if (isActive) Color(0xFFFFF3E0) else Color.White,
                        border   = if (isActive) BorderStroke(1.5.dp, OrangeAccent) else null
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (h.year.isNotBlank())
                                    Text(h.year, fontSize = 11.sp, color = Color(0xFF888888))
                            }
                            when {
                                isActive  -> Text("▶ 播放中", fontSize = 11.sp, color = OrangeAccent,
                                    fontWeight = FontWeight.SemiBold)
                                hasPlayed -> Text("✓ 已播", fontSize = 11.sp, color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearestHeritageSection(heritage: Heritage, distanceM: Float, unlockedIds: Set<Int>) {
    val isUnlocked = heritage.id in unlockedIds
    val (value, unit) = if (distanceM < 1000f)
        "${"%.0f".format(distanceM)}" to "公尺" else "${"%.1f".format(distanceM / 1000f)}" to "公里"
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("最近的古蹟", fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(10.dp),
                        color = if (isUnlocked) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)) {
                        Text(if (isUnlocked) "已解鎖" else "未解鎖",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = if (isUnlocked) Color(0xFF4CAF50) else Color(0xFF888888))
                    }
                }
                Text(heritage.year.ifBlank { "臺南古蹟" }, fontSize = 12.sp, color = TextGray)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 14.sp, color = TextGray, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

private fun routePanelHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2).pow(2) + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2).pow(2)
    return 2 * R * atan2(sqrt(a), sqrt(1 - a))
}
