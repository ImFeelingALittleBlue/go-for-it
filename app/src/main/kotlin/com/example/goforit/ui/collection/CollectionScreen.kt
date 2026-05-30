package com.example.goforit.ui.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.SilverSaltStore

// 收藏畫面：顯示目前的「時光銀鹽」點數
// （之後第 4、5 步會在這裡加上已修復古蹟的老照片牆）
@Composable
fun CollectionScreen() {
    val context = LocalContext.current

    // by + points()：觀察共用點數狀態，數字一變這個畫面就自動重畫
    val points by SilverSaltStore.points(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("時光銀鹽", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("探索越多，沖洗越多", fontSize = 13.sp, color = Color(0xFF888888))

        Spacer(Modifier.height(20.dp))

        // ── 點數卡片 ─────────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF8A5A2B),          // 古銅褐
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF5D58A))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("目前擁有", color = Color(0xFFE8D8C0), fontSize = 13.sp)
                    Text(
                        "$points 銀鹽",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 測試用按鈕：手動加 10 點，驗證「會增加」且「重開還在」──────────────
        // （正式版會改成跑步、答題時自動發放，這顆之後會移除）
        Button(
            onClick = { SilverSaltStore.add(context, 10) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("賺取 +10（測試用）", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
