package com.example.goforit.navigation

sealed class Screen(
    val route: String,
    val label: String,
    val iconAsset: String
) {
    object Home       : Screen("home",       "我的地圖", "我的地圖.png")
    object Run        : Screen("run",        "去探索",   "去探索.png")
    object Records    : Screen("records",    "紀錄",     "探索紀錄.png")
    object Account    : Screen("account",    "帳號",     "我的帳號.png")

    companion object {
        val items = listOf(Home, Run, Records, Account)
    }
}
