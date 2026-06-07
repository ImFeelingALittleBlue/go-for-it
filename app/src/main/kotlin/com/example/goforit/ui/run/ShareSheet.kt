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

// 分享底頁（符合 分享.png）
// mapBitmap：呼叫端先用 mapboxMap.snapshot{} 截好地圖再傳入，使用者儲存的就是這張
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    routeName: String,   // 路線名稱（顯示在 Podcast 資訊區塊）
    mapBitmap: Bitmap,   // 含底圖的地圖截圖
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var imgSaved by remember { mutableStateOf(false) }

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

            // AI 生成 Podcast 資訊卡（佔位）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF5F0EB)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI 生成 Podcast", fontSize = 11.sp, color = Color(0xFFD4A96A),
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(3.dp))
                    Text("語音導覽生成後可在此下載", fontSize = 12.sp, color = Color(0xFF888888))
                    Spacer(Modifier.height(2.dp))
                    Text(routeName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 下載 Podcast（功能開發中）
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = false,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C3D1E))
            ) {
                Text("下載 Podcast", color = Color.White, fontWeight = FontWeight.SemiBold)
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
