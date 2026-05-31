package com.example.goforit.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.SilverSaltStore

// 修復一座古蹟需要的時光銀鹽點數
private const val RESTORE_COST = 100

// 古蹟詳情卡（從底部彈出）
// 依「是否已修復」切換：未修復顯示鎖住畫面+修復按鈕；已修復顯示老照片
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeritageDetailSheet(
    heritage: Heritage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 觀察：目前點數、雲端修復紀錄。任一改變這張卡都會自動重畫
    val points by SilverSaltStore.points(context)
    val records = RestorationRepository.records()
    val isRestored = records.any { it.heritageId == heritage.id }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── 解鎖狀態標籤 ─────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isRestored) Color(0xFF4CAF50) else Color(0xFF2C2C2C)
            ) {
                Text(
                    if (isRestored) "✓ 已修復" else "✕ 未解鎖",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 照片區：已修復顯示老照片，未修復顯示鎖住佔位 ───────────────
            if (isRestored) {
                HeritagePhoto(heritage.photoFile)
            } else {
                LockedPhotoPlaceholder()
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

            // ── 名稱 + 描述 ───────────────────────────────────────────────
            Text(heritage.name, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                heritage.description,
                fontSize = 14.sp,
                color = Color(0xFF444444),
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── 修復按鈕（已修復就不再顯示）─────────────────────────────────
            if (!isRestored) {
                val enough = points >= RESTORE_COST
                Button(
                    onClick = {
                        // 先扣點數，扣成功才寫一筆修復紀錄到雲端
                        if (SilverSaltStore.spend(context, RESTORE_COST)) {
                            RestorationRepository.add(heritage)
                        }
                    },
                    enabled = enough,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (enough) "修復古蹟（花費 $RESTORE_COST 銀鹽）"
                        else "銀鹽不足（需 $RESTORE_COST，目前 $points）",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// 已修復：從 assets/photos 載入老照片並顯示
@Composable
private fun HeritagePhoto(photoFile: String) {
    val context = LocalContext.current
    // remember(photoFile)：同一張照片只解碼一次，不每次重畫都重讀
    val bitmap = remember(photoFile) {
        runCatching {
            context.assets.open("photos/$photoFile").use { BitmapFactory.decodeStream(it) }
                .asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        )
    } else {
        // 找不到照片檔時的退路
        PhotoBox { Text("找不到老照片", color = TextGray, fontSize = 13.sp) }
    }
}

// 未修復：鎖住的灰底佔位
@Composable
private fun LockedPhotoPlaceholder() {
    PhotoBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeAccent)
            Spacer(Modifier.height(8.dp))
            Text("解鎖舊貌", fontWeight = FontWeight.Bold, color = Color(0xFF555555))
            Text("收集足夠時光銀鹽即可修復", fontSize = 12.sp, color = TextGray)
        }
    }
}

// 照片區共用的灰底外框
@Composable
private fun PhotoBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEDEAE3)),
        contentAlignment = Alignment.Center
    ) { content() }
}
