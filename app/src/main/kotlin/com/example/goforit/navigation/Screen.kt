package com.example.goforit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

// sealed class 就像「有限種類的列舉」，每個 Screen 代表一個底部分頁
// route 是頁面的「名字」，NavController 用它來知道要跳去哪
sealed class Screen(
    val route: String,      // 導航用的路徑字串
    val label: String,      // 顯示在導航列的文字
    val icon: ImageVector   // 顯示在導航列的圖示
) {
    object Home       : Screen("home",       "首頁", Icons.Default.Home)
    object Run        : Screen("run",        "跑步", Icons.Default.DirectionsRun)
    object Map        : Screen("map",        "地圖", Icons.Default.Map)
    object Collection : Screen("collection", "收藏", Icons.Default.EmojiEvents)

    companion object {
        // 所有分頁的清單，BottomNavBar 用這個自動產生按鈕
        val items = listOf(Home, Run, Map, Collection)
    }
}
