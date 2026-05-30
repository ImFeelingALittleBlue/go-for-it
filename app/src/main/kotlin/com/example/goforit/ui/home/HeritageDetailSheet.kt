package com.example.goforit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.Heritage

// 古蹟詳情卡（從底部彈出）
// 目前是「未解鎖」狀態：老照片用鎖住的灰底佔位，修復功能留到第 4 步
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeritageDetailSheet(
    heritage: Heritage,
    onDismiss: () -> Unit            // 關閉卡片時呼叫（把 selected 設回 null）
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())   // 描述太長時可以滑動
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── 未解鎖標籤 ───────────────────────────────────────────────
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2C2C2C)) {
                Text(
                    "✕ 未解鎖",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 鎖住的老照片佔位區 ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEDEAE3)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeAccent)
                    Spacer(Modifier.height(8.dp))
                    Text("解鎖舊貌", fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                    Text(
                        "收集足夠時光銀鹽即可修復",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 年代 ─────────────────────────────────────────────────────
            if (heritage.year.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = ChipBg) {
                    Text(
                        "${heritage.year} 年",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF7A6A55)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── 名稱 ─────────────────────────────────────────────────────
            Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 22.sp)

            Spacer(Modifier.height(12.dp))

            // ── 描述 ─────────────────────────────────────────────────────
            Text(
                heritage.description,
                fontSize = 14.sp,
                color = Color(0xFF444444),
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── 修復按鈕（第 4 步才接「時光銀鹽」點數，目前先停用）──────────
            Button(
                onClick = { /* TODO 第 4 步：檢查時光銀鹽是否足夠 → 解鎖老照片 */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("修復古蹟（即將開放）", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
