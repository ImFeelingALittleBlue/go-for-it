package com.example.goforit.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.Heritage
import com.example.goforit.ui.home.TextGray

// 跑步中底部深色統計列：左距離、中計時、右停止按鈕
@Composable
fun RunningStatsBar(
    coveredKm: Float,
    elapsedSeconds: Int,
    onStop: () -> Unit
) {
    Surface(color = Color(0xFF2C2218), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 已跑距離
            Column(modifier = Modifier.weight(1f)) {
                Text("${"%.2f".format(coveredKm)} km", color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("距離", color = Color(0xFF9E8E7A), fontSize = 12.sp)
            }
            // 已跑時間
            Column(modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatOverlayTime(elapsedSeconds), color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("時間", color = Color(0xFF9E8E7A), fontSize = 12.sp)
            }
            // 停止
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("停止", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// 剛解鎖古蹟時出現的通知卡（浮在統計列上方）
@Composable
fun HeritageUnlockCard(
    heritage: Heritage,
    silverReward: Int,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text("古蹟解鎖！", fontSize = 12.sp, color = Color(0xFF888888))
            }
            // 銀鹽獎勵徽章
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF3E0)) {
                Text(
                    "+$silverReward 銀鹽",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color(0xFFD4822A), fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Text("繼續", color = Color(0xFFD4822A), fontSize = 13.sp)
            }
        }
    }
}

// 結束旅程確認對話框（選擇路線跑步按暫停時出現）
@Composable
fun StopConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("要結束旅程嗎？", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
            ) {
                Text("結束跑步", color = Color.White)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4822A))
            ) {
                Text("繼續跑步", color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

// 直接跑步的統計列（只有距離+時間，沒有按鈕）
@Composable
fun DirectRunStatsRow(coveredKm: Float, elapsedSeconds: Int) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFF5F0EB))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${"%.2f".format(coveredKm)} km",
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("距離", fontSize = 12.sp, color = TextGray)
        }
        Column(modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatOverlayTime(elapsedSeconds),
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("時間", fontSize = 12.sp, color = TextGray)
        }
    }
}

// 最近的古蹟預覽卡（直接跑步時固定顯示在底部）
@Composable
fun NearestHeritagePreviewCard(
    heritage: Heritage,
    distanceMeters: Float,
    isRestored: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text("最近的古蹟", fontSize = 11.sp, color = TextGray)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(heritage.year.ifBlank { "臺南古蹟" },
                    fontSize = 12.sp, color = TextGray)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isRestored) Color(0xFF4CAF50) else Color(0xFFEEEEEE)
            ) {
                Text(
                    if (isRestored) "已解鎖" else "未解鎖",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = if (isRestored) Color.White else Color(0xFF666666)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val (value, unit) = if (distanceMeters < 1000f)
                "${"%.0f".format(distanceMeters)}" to "公尺"
            else
                "${"%.1f".format(distanceMeters / 1000f)}" to "公里"
            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C2218))
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 14.sp, color = TextGray,
                modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

// mm:ss 格式（供 RunningStatsBar 使用）
internal fun formatOverlayTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
