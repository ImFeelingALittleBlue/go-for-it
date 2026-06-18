package com.example.goforit.navigation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// 底部導航列元件
// NavController 由 MainActivity 傳進來，這樣點擊才能真正切換頁面
@Composable
fun BottomNavBar(navController: NavController) {

    // currentBackStackEntry：NavController 目前頁面的紀錄
    // by 是 Kotlin 委派語法，讓 navBackStackEntry 自動跟著狀態更新
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        // 用 Screen.items 自動產生每一個分頁按鈕，不用手寫四次
        Screen.items.forEach { screen ->
            NavigationBarItem(
                icon  = {
                    BottomNavAssetIcon(
                        assetName = screen.iconAsset,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                // 判斷這個按鈕是不是目前所在的頁面（決定是否高亮）
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // 回到起點再跳，避免按很多次後退堆疊一堆頁面
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true  // 離開時保留這個頁面的狀態
                        }
                        launchSingleTop = true  // 同一頁面不重複疊加
                        restoreState = true     // 回來時還原之前的狀態
                    }
                }
            )
        }
    }
}

@Composable
private fun BottomNavAssetIcon(
    assetName: String,
    contentDescription: String
) {
    val context = LocalContext.current
    val bitmap = remember(context, assetName) {
        runCatching {
            context.assets.open(assetName).use { stream ->
                val source = BitmapFactory.decodeStream(stream)
                val iconHeight = (source.height * 0.72f).toInt().coerceIn(1, source.height)
                Bitmap.createBitmap(source, 0, 0, source.width, iconHeight)
                    .asImageBitmap()
            }
        }.getOrNull()
    }

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color(0xFF2F2A27)),
            modifier = Modifier.size(38.dp)
        )
    }
}
