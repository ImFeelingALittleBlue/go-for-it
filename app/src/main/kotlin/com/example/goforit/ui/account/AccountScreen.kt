package com.example.goforit.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.AuthRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RouteRepository

private val DarkBrown  = Color(0xFF2C1A10)
private val CardBrown  = Color(0xFF3D2416)
private val LabelColor = Color(0xFF888888)

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val user    = AuthRepository.user
    val displayName = user?.displayName?.ifBlank { null } ?: "探索者"
    val email       = user?.email ?: ""

    // 從現有 Repository 讀取統計數字
    val runCount      = RouteRepository.records().count { it.elapsedSeconds > 0 }
    // distinctBy 去重：同一古蹟若被重複寫入，只算一次
    val unlockedCount = RestorationRepository.records().distinctBy { it.heritageId }.size

    // 設定開關狀態（本地暫存）
    var notifyRun     by remember { mutableStateOf(true) }
    var autoPodcast   by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F0EB))
            .verticalScroll(rememberScrollState())
    ) {
        // ── 個人資料卡（深棕背景）─────────────────────────────────────────
        ProfileCard(displayName, email, runCount, unlockedCount)

        Spacer(Modifier.height(16.dp))

        // ── 設定 ──────────────────────────────────────────────────────────
        SectionLabel("設定")
        SettingsCard(notifyRun, autoPodcast,
            onNotifyChange = { notifyRun = it },
            onPodcastChange = { autoPodcast = it })

        Spacer(Modifier.height(16.dp))

        // ── 台文史社群 ────────────────────────────────────────────────────
        SectionLabel("台文史社群")
        CommunityCard()

        Spacer(Modifier.height(16.dp))

        // ── 資料來源 ──────────────────────────────────────────────────────
        SectionLabel("資料來源")
        DataSourceCard()

        Spacer(Modifier.height(24.dp))

        // 登出
        TextButton(
            onClick = { AuthRepository.signOut() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("登出", color = LabelColor, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// 個人資料卡：頭像 + 名稱/Email + 三欄統計
@Composable
private fun ProfileCard(name: String, email: String, runs: Int, unlocked: Int) {
    Surface(color = DarkBrown, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 頭像：顯示名字第一個字
                Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(50),
                    color = Color(0xFFC8956A)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(name.take(1), fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = Color.White)
                    if (email.isNotBlank())
                        Text(email, fontSize = 12.sp, color = Color(0xFFB0A090))
                }
            }
            Spacer(Modifier.height(16.dp))
            // 探索與修復統計
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(runs.toString(), "次探索")
                StatItem(unlocked.toString(), "已修復古蹟")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardBrown) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = Color(0xFFB0A090))
        }
    }
}

// 設定卡：兩個 Toggle + 語言
@Composable
private fun SettingsCard(notifyRun: Boolean, autoPodcast: Boolean,
    onNotifyChange: (Boolean) -> Unit, onPodcastChange: (Boolean) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column {
            ToggleRow("跑步通知", notifyRun, onNotifyChange)
            HorizontalDivider(color = Color(0xFFF0EDE8))
            ToggleRow("自動播放 Podcast", autoPodcast, onPodcastChange)
            HorizontalDivider(color = Color(0xFFF0EDE8))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("語言", fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("繁體中文", fontSize = 14.sp, color = LabelColor)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4CAF50)))
    }
}

// 台文史社群卡
@Composable
private fun CommunityCard() {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column {
            CommunityRow("關於台文史社群", "由文史工作者、研究員、志工組成的協作平台")
            HorizontalDivider(color = Color(0xFFF0EDE8))
            CommunityRow("找不到你要的地點？", "申請新增古蹟地標 →",
                subColor = Color(0xFFD4822A))
            HorizontalDivider(color = Color(0xFFF0EDE8))
            CommunityRow("加入台文史社群", "成為文史工作者或志工 ↗")
        }
    }
}

@Composable
private fun CommunityRow(title: String, subtitle: String,
    subColor: Color = LabelColor) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, fontSize = 12.sp, color = subColor)
    }
}

// 資料來源卡
@Composable
private fun DataSourceCard() {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceRow("古蹟資料", "depositar.io")
            SourceRow("歷史地圖", "中研院 WMTS")
            SourceRow("老照片", "台大資料庫")
            SourceRow("Podcast", "AI生成 + 台文史社群審核")
        }
    }
}

@Composable
private fun SourceRow(label: String, source: String) {
    Row {
        Text("$label · ", fontSize = 13.sp, color = LabelColor)
        Text(source, fontSize = 13.sp, color = Color(0xFF2C2218))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, color = LabelColor, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
}
