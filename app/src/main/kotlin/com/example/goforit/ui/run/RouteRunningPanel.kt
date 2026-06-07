package com.example.goforit.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// ─── 頂部面板（地圖上方）：距離/時間統計 + 本次預計銀鹽 ─────────────────────
@Composable
fun RouteRunningTopPanel(liveSilver: Int, coveredKm: Float, elapsedSeconds: Int) {
    // 統計列：已跑距離 | 已跑時間
    Row(modifier = Modifier.fillMaxWidth()
        .background(Color.White)
        .padding(horizontal = 20.dp, vertical = 12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${"%.1f".format(coveredKm)} km",
                fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("距離", fontSize = 11.sp, color = TextGray)
        }
        Column(modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatOverlayTime(elapsedSeconds),
                fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("時間", fontSize = 11.sp, color = TextGray)
        }
    }
    // 本次預計銀鹽（距離/時間下方）
    RunSilverRow(liveSilver)
}

@Composable
fun RouteRunningBottomPanel(
    routePoints: List<Point>,
    heritages: List<Heritage>,
    nearestHeritage: Heritage?,
    distanceToNearestM: Float,
    unlockedIds: Set<Int>,
    activePodcastHeritage: Heritage? = null,   // 目前正在播放 Podcast 的古蹟
    activeLine: DialogueLine? = null,           // 目前正在唸的對話行
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    // 計算路線停靠點（路線不變就不重算）
    val stops = remember(routePoints) { heritagesOnRoute(routePoints, heritages) }
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        AIPodcastCard(stops = stops, unlockedIds = unlockedIds,
            activePodcastHeritage = activePodcastHeritage, activeLine = activeLine)
        nearestHeritage?.let { NearestHeritageSection(it, distanceToNearestM, unlockedIds) }
        // 暫停按鈕（按下後跳出結束旅程確認對話框）
        Button(
            onClick = onPause,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
        ) { Text("暫停", color = Color.White, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun AIPodcastCard(
    stops: List<Pair<Heritage, Int>>,
    unlockedIds: Set<Int>,
    activePodcastHeritage: Heritage? = null,
    activeLine: DialogueLine? = null
) {
    val isPlaying = activePodcastHeritage != null
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF2C2218)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 播放鍵：播放中顯示橘色，靜止時顯示金色
                Surface(modifier = Modifier.size(40.dp), shape = CircleShape,
                    color = Color(0xFF3D2E1C)) {
                    Box(contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()) {
                        Text("▶", color = if (isPlaying) OrangeAccent else Color(0xFFD4A96A),
                            fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AI 旁白 Podcast", fontSize = 11.sp, color = Color(0xFF9E8E7A))
                    // 播放中顯示古蹟名稱，靜止時顯示「AI 導覽路線」
                    Text(
                        if (isPlaying) activePodcastHeritage!!.name else "AI 導覽路線",
                        fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                    Text("含 ${stops.size} 個古蹟停靠點",
                        fontSize = 11.sp, color = Color(0xFF9E8E7A))
                }
            }
            // 播放中：顯示說話者角色與當前對話文字
            if (isPlaying && activeLine != null) {
                Spacer(Modifier.height(8.dp))
                val role = if (activeLine.speaker == PodcastSpeaker.HOST_B) "文史達人" else "主持人"
                Text("$role・${activeLine.text.take(36)}…",
                    fontSize = 11.sp, color = OrangeAccent, maxLines = 1)
            }
            Spacer(Modifier.height(12.dp))
            // 古蹟進度小點列（依路線順序排列；已解鎖=橘色，未解鎖=深褐色）
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically) {
                if (stops.isEmpty()) {
                    Text("路線沿途無古蹟停靠點", fontSize = 11.sp, color = Color(0xFF9E8E7A))
                } else {
                    stops.forEachIndexed { i, (h, _) ->
                        val unlocked = h.id in unlockedIds
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(if (unlocked) OrangeAccent else Color(0xFF5C4A30)))
                        if (i < stops.size - 1)
                            Box(modifier = Modifier.width(14.dp).height(2.dp)
                                .background(Color(0xFF3D2E1C)))
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
        "${"%.0f".format(distanceM)}" to "公尺"
    else
        "${"%.1f".format(distanceM / 1000f)}" to "公里"

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)
        .padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text("最近的古蹟", fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    // 解鎖狀態徽章
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
            // 距離數字（大字體，靠右）
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2218))
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 14.sp, color = TextGray,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

private fun routePanelHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6_371_000.0
    val dLat = (lat2 - lat1) * PI / 180
    val dLon = (lon2 - lon1) * PI / 180
    val a = sin(dLat / 2).pow(2) +
            cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2).pow(2)
    return 2 * R * atan2(sqrt(a), sqrt(1 - a))
}
