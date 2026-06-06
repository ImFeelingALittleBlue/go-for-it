package com.example.goforit.ui.run

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
                Text("Podcast 已播放，舊照片解鎖！", fontSize = 12.sp, color = Color(0xFF888888))
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) {
                Text("繼續", color = Color(0xFFD4822A), fontSize = 13.sp)
            }
        }
    }
}

// 停止確認對話框：避免不小心結束跑程
@Composable
fun StopConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("要結束跑程嗎？", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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

// mm:ss 格式（供 RunningStatsBar 使用）
internal fun formatOverlayTime(seconds: Int): String =
    "%02d:%02d".format(seconds / 60, seconds % 60)
