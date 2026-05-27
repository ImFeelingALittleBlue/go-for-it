package com.example.goforit.ui.run

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// 跑步畫面（暫時只顯示文字，之後會加 Mapbox 地圖和計時器）
@Composable
fun RunScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "跑步",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
