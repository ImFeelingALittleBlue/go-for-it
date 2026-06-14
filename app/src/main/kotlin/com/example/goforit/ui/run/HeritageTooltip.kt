package com.example.goforit.ui.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.goforit.ui.home.OrangeAccent

// 地圖上點選古蹟時浮出的小名稱標籤，定位在古蹟點正上方
// x, y 來自 mapboxMap.pixelForCoordinate()（邏輯像素，即 dp）
@Composable
fun HeritageNameTooltip(
    name: String,
    x: Float,
    y: Float,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .absoluteOffset(x = (x - 60).dp, y = (y - 48).dp)
            .clickable(onClick = onDismiss),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, OrangeAccent)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2C2218)
        )
    }
}
