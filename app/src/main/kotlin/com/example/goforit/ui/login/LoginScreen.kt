package com.example.goforit.ui.login

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.ImageBitmap
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
private val Brown   = Color(0xFF94603B)  // 赭石棕副標
private val Gray    = Color(0xFFA9A9A9)  // 淡墨灰說明
private val DarkBtn = Color(0xFF3B211A)  // 焦墨棕按鈕
private val Sun     = Color(0xFFE0553B)  // 朱紅落日

// 登入頁：尚未登入時顯示，按下 Google 按鈕登入
@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }   // 登入中 → 按鈕轉圈圈
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        val titleTopGap = (maxHeight * 0.15f).coerceIn(96.dp, 142.dp)
        val illustrationHeight = (maxHeight * 0.27f).coerceIn(230.dp, 300.dp)
        val buttonToIllustrationGap = (maxHeight * 0.05f).coerceIn(34.dp, 54.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(titleTopGap))

            // ── 標題 ─────────────────────────────────────────────────────────
            Text("古 復 矣", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Go For It!", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Brown)

            Spacer(Modifier.height(28.dp))

            // ── 標語 ─────────────────────────────────────────────────────────
            Text(
                "跑過台灣的大街小巷\n修復消失中的歷史記憶",
                fontSize = 20.sp,
                color = Gray,
                textAlign = TextAlign.Center,
                lineHeight = 31.sp
            )

            Spacer(Modifier.weight(1f))

            // ── Google 登入按鈕 ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(14.dp),
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
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "以 Google 帳號登入",
                            color = Paper,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── 錯誤訊息 / 條款 ──────────────────────────────────────────
                if (errorMessage != null) {
                    Text(errorMessage!!, color = Sun, fontSize = 12.sp, textAlign = TextAlign.Center)
                } else {
                    Text("登入即表示同意 服務條款 與 隱私政策", color = Gray, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(buttonToIllustrationGap))

            CoverIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(illustrationHeight)
            )
        }
    }
}

// Google 按鈕左邊的「G」標誌（先用簡單版，保持按鈕版面接近設計稿）
@Composable
private fun GoogleBadge() {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 21.sp)
    }
}

@Composable
private fun CoverIllustration(modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val coverOffsetX by animateDpAsState(
        targetValue = if (entered) 0.dp else (-120).dp,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "coverOffsetX"
    )
    val sunOffsetX by animateDpAsState(
        targetValue = if (entered) 0.dp else 120.dp,
        animationSpec = tween(durationMillis = 950, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "sunOffsetX"
    )

    val cover = rememberAssetBitmap("封面.png")
    val sun = rememberAssetBitmap("封面太陽.png")

    BoxWithConstraints(modifier = modifier) {
        val coverHeight = maxWidth * (495f / 383f)
        val sunSize = (maxWidth * 0.17f).coerceIn(62.dp, 82.dp)

        if (sun != null) {
            Image(
                bitmap = sun,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 58.dp)
                    .offset(x = sunOffsetX)
                    .size(sunSize)
            )
        }

        if (cover != null) {
            Image(
                bitmap = cover,
                contentDescription = "登入插圖",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = coverOffsetX)
                    .requiredWidth(maxWidth)
                    .requiredHeight(coverHeight)
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 58.dp)
                    .offset(x = sunOffsetX)
                    .size(sunSize)
                    .clip(CircleShape)
                    .background(Sun)
            )
        }
    }
}

@Composable
private fun rememberAssetBitmap(assetName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(context, assetName) {
        runCatching {
            context.assets.open(assetName)
                .use(BitmapFactory::decodeStream)
                .asImageBitmap()
        }.getOrNull()
    }
}
