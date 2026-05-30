package com.example.goforit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home       : Screen("home",       "我的地圖", Icons.Default.Map)
    object Run        : Screen("run",        "去探索",   Icons.Default.DirectionsRun)
    object Records    : Screen("records",    "紀錄",     Icons.Default.History)
    object Account    : Screen("account",    "帳號",     Icons.Default.Person)

    companion object {
        val items = listOf(Home, Run, Records, Account)
    }
}
