package com.example.goforit.ui.run

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.Heritage

// 分享底頁：地圖截圖儲存 + 下載 Podcast 語音檔（WAV）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    routeName: String,                              // 路線名稱
    mapBitmap: Bitmap,                              // 含底圖的地圖截圖
    routeHeritages: List<Heritage> = emptyList(),   // 路線古蹟，用來合成 Podcast
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var imgSaved      by remember { mutableStateOf(false) }
    var isExporting   by remember { mutableStateOf(false) }
    var exportDone    by remember { mutableStateOf(false) }
    var exportFailed  by remember { mutableStateOf(false) }
    // 合成進度（current, total 行數）
    var exportProgress by remember { mutableStateOf(0 to 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("完成探索！", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            // 地圖截圖（含底圖 + 路線）
            Image(
                bitmap = mapBitmap.asImageBitmap(),
                contentDescription = "路線地圖",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(mapBitmap.width.toFloat() / mapBitmap.height.toFloat())
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))

            // 儲存地圖截圖到相簿
            Button(
                onClick = { if (saveBitmapToGallery(context, mapBitmap)) imgSaved = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
            ) {
                Text(
                    if (imgSaved) "✓ 已儲存至相簿" else "儲存圖片",
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // AI Podcast 資訊卡
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF5F0EB)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI 生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A),
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(3.dp))
                    // 合成中顯示進度，否則顯示說明文字
                    if (isExporting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFD4A96A)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "合成中... (${exportProgress.first}/${exportProgress.second} 行)",
                                fontSize = 12.sp, color = Color(0xFF888888)
                            )
                        }
                    } else {
                        Text(
                            if (routeHeritages.isEmpty()) "沿路線跑步即可解鎖古蹟語音"
                            else "包含 ${routeHeritages.size} 個古蹟停靠點・TTS 合成語音",
                            fontSize = 12.sp, color = Color(0xFF888888)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(routeName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 下載 Podcast 語音 WAV 按鈕
            Button(
                onClick = {
                    if (!isExporting && !exportDone && routeHeritages.isNotEmpty()) {
                        isExporting  = true
                        exportFailed = false
                        exportPodcastToWav(
                            context    = context,
                            routeName  = routeName,
                            heritages  = routeHeritages,
                            onProgress = { cur, total -> exportProgress = cur to total },
                            onComplete = { success ->
                                isExporting = false
                                if (success) exportDone = true else exportFailed = true
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = routeHeritages.isNotEmpty() && !isExporting && !exportDone,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
            ) {
                Text(
                    when {
                        exportDone               -> "✓ 已下載至 Downloads"
                        isExporting              -> "合成中，請稍候..."
                        exportFailed             -> "下載失敗，請重試"
                        routeHeritages.isEmpty() -> "下載 Podcast（無路線古蹟）"
                        else                     -> "下載語音 Podcast"
                    },
                    color = Color.White, fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("關閉", fontSize = 14.sp)
            }
        }
    }
}
