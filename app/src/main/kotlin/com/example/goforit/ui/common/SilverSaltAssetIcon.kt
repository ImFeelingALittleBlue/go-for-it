package com.example.goforit.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private const val SILVER_SALT_ASSET = "銀鹽.png"

@Composable
fun SilverSaltAssetIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = "時光銀鹽"
) {
    val context = LocalContext.current
    val image = remember {
        runCatching {
            context.assets.open(SILVER_SALT_ASSET)
                .use(BitmapFactory::decodeStream)
                .asImageBitmap()
        }.getOrNull()
    }

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF5C3D1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "銀",
                color = Color(0xFFD4A96A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
