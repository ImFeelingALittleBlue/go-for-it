package com.example.goforit.ui.run

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 故事生成的三個狀態，RoutePreviewScreen 與 RunSummaryScreen 共用
enum class StoryState { IDLE, GENERATING, DONE }

// Podcast 時長輸入框：使用者輸入分鐘數（1–60），各頁面共用
// minutes 為目前值，onMinutesChange 通知父層更新
@Composable
fun PodcastMinutesInput(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // text 儲存使用者正在輸入的字串，允許暫時空字串；只有合法值才呼叫 onMinutesChange
    var text by remember { mutableStateOf(minutes.toString()) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }.take(2) // 最多兩位數字
                text = digits
                digits.toIntOrNull()?.let { v -> if (v in 1..60) onMinutesChange(v) }
            },
            modifier    = Modifier.width(72.dp),
            singleLine  = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("分鐘", fontSize = 11.sp) }
        )
        Text(
            "整段 Podcast 約 ${text.toIntOrNull()?.let { "$it" } ?: minutes} 分鐘",
            fontSize = 12.sp, color = Color(0xFF888888)
        )
    }
}
