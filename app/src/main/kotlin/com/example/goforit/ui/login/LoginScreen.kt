package com.example.goforit.ui.login

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.data.AuthRepository
import kotlinx.coroutines.launch

// 水墨風配色（登入頁專用，照設計稿）
private val Paper   = Color(0xFFF5F1E8)  // 宣紙米白底
private val Ink     = Color(0xFF2E2E2E)  // 墨黑標題
private val Brown   = Color(0xFF8B6F4E)  // 赭石棕副標
private val Gray    = Color(0xFF8A857B)  // 淡墨灰說明
private val DarkBtn = Color(0xFF3A2E26)  // 焦墨棕按鈕
private val Sun     = Color(0xFFE0553B)  // 朱紅落日

// 登入頁：尚未登入時顯示，按下 Google 按鈕登入
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }   // 登入中 → 按鈕轉圈圈
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.18f))

        // ── 標題 ─────────────────────────────────────────────────────────────
        Text("古 復 矣", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("Go For It!", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Brown)

        Spacer(Modifier.height(20.dp))

        // ── 標語 ─────────────────────────────────────────────────────────────
        Text(
            "跑過台灣的大街小巷\n修復消失中的歷史記憶",
            fontSize = 15.sp,
            color = Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(24.dp))

        // ── 水墨插圖（放 assets/login_hero.png 就會顯示，否則畫一輪朱紅落日）──
        HeroIllustration(modifier = Modifier.weight(1f))

        Spacer(Modifier.height(24.dp))

        // ── Google 登入按鈕 ──────────────────────────────────────────────────
        Button(
            onClick = {
                if (isLoading) return@Button
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = AuthRepository.signInWithGoogle(context)
                    isLoading = false
                    result.onFailure { errorMessage = "登入失敗：${it.message}" }
                    // 成功不用做事：AuthRepository.user 一變，MainActivity 會自動切到主畫面
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBtn)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Paper,
                    strokeWidth = 2.dp
                )
            } else {
                GoogleBadge()
                Spacer(Modifier.width(10.dp))
                Text("以 Google 帳號登入", color = Paper, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 錯誤訊息 / 條款 ──────────────────────────────────────────────────
        if (errorMessage != null) {
            Text(errorMessage!!, color = Sun, fontSize = 12.sp, textAlign = TextAlign.Center)
        } else {
            Text("登入即表示同意 服務條款 與 隱私政策", color = Gray, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(0.12f))
    }
}

// Google 按鈕左邊的白底「G」標誌（先用簡單版，之後可換官方彩色 logo）
@Composable
private fun GoogleBadge() {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// 嘗試載入 assets/login_hero.png 當插圖；沒有就畫一輪朱紅落日當裝飾
@Composable
private fun HeroIllustration(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember {
        runCatching {
            context.assets.open("login_hero.png").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
        }.getOrNull()
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "登入插圖",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 沒有插圖檔時的退路：一輪朱紅落日
            Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Sun))
        }
    }
}
