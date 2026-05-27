package com.example.goforit.ui.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// 收藏畫面（暫時只顯示文字，之後會加時光銀鹽積分和老照片）
@Composable
fun CollectionScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "收藏",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
