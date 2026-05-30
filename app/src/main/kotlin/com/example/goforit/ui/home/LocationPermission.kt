package com.example.goforit.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 處理 GPS 定位權限的 Composable 函式。
 *
 * 怎麼運作：
 * 1. 一進畫面就檢查有沒有權限
 * 2. 沒有的話自動彈出「允許定位」對話框
 * 3. 使用者允許後呼叫 onGranted（讓地圖顯示藍點）
 *
 * @return 目前是否已取得權限（true = 有）
 */
@Composable
fun rememberLocationPermission(onGranted: () -> Unit): Boolean {
    val context = LocalContext.current

    // 先檢查手機上目前的權限狀態
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // launcher：負責彈出「允許定位」對話框，並接收使用者的選擇
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) onGranted()  // 使用者按「允許」才執行
    }

    // LaunchedEffect(Unit)：畫面第一次出現時執行一次
    LaunchedEffect(Unit) {
        if (hasPermission) {
            onGranted()                                           // 已有權限，直接開始
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)  // 彈出詢問框
        }
    }

    return hasPermission
}
