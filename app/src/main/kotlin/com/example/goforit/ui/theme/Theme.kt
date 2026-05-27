package com.example.goforit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 深色模式配色
private val DarkColorScheme = darkColorScheme(
    primary = TainanBrickDark,
    secondary = TainanGold,
    background = TainanSandDark,
    surface = TainanSandDark,
    onPrimary = TainanSand,
    onBackground = TainanSand,
    onSurface = TainanSand,
)

// 淺色模式配色（預設）
private val LightColorScheme = lightColorScheme(
    primary = TainanBrick,
    secondary = TainanGold,
    tertiary = TainanGreen,
    background = TainanSand,
    surface = TainanSand,
    onPrimary = TainanSand,
    onBackground = TainanDark,
    onSurface = TainanDark,
)

// App 的全域主題包裝元件
// dynamicColor：Android 12+ 可以跟著系統主題色變（壁紙取色）
@Composable
fun GoForItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 關閉動態色，保持臺南風格配色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
