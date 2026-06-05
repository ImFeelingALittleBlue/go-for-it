package com.example.goforit.ui.account

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
import com.example.goforit.data.AuthRepository
import com.example.goforit.data.SilverSaltStore

// 帳號頁：顯示目前的「時光銀鹽」點數
// （正式版的銀鹽會改成跑步、答題時自動發放，目前先用測試按鈕手動增加）
@Composable
fun AccountScreen() {
    val context = LocalContext.current

    // by + points()：觀察共用點數狀態，數字一變這個畫面就自動重畫
    val points by SilverSaltStore.points(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("我的帳號", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        // 顯示目前登入的 Google 帳號（沒名字就顯示 email）
        val user = AuthRepository.user
        Text(
            user?.displayName ?: user?.email ?: "探索越多，沖洗越多",
            fontSize = 13.sp,
            color = Color(0xFF888888)
        )

        Spacer(Modifier.height(20.dp))

        // ── 時光銀鹽點數卡片 ─────────────────────────────────────────────────
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
                    Text("時光銀鹽", color = Color(0xFFE8D8C0), fontSize = 13.sp)
                    Text(
                        "$points",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 測試用按鈕：手動加 10 點（之後改成跑步/答題自動發放）─────────────
        Button(
            onClick = { SilverSaltStore.add(context, 10) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("賺取 +10（測試用）", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(12.dp))

        // ── 登出：登出後 MainActivity 會自動切回登入頁 ──────────────────────────
        OutlinedButton(
            onClick = { AuthRepository.signOut() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("登出", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
