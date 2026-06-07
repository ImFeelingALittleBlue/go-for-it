package com.example.goforit.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val Brown = Color(0xFF9B6A3F)
private val Cream = Color(0xFFF8F3EB)
private val DarkBrown = Color(0xFF38231A)

@Composable
fun SilverSaltHelpButton(
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(false) }

    Surface(
        onClick = { showHelp = true },
        modifier = modifier.size(42.dp),
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "?",
                color = Brown,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showHelp) {
        SilverSaltHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun SilverSaltHelpDialog(
    onDismiss: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "銀鹽介紹 ${page + 1}/2",
                        color = Color(0xFF8D8884),
                        fontSize = 12.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }

                if (page == 0) {
                    WhatIsSilverSalt()
                } else {
                    HowToUseSilverSalt()
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        if (page == 0) page = 1 else onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brown)
                ) {
                    Text(if (page == 0) "繼續" else "開始探索，收集銀鹽！")
                }

                if (page == 0) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFAAA19A)
                        ),
                        elevation = null
                    ) {
                        Text("略過說明", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WhatIsSilverSalt() {
    Text(
        "什麼是時光銀鹽？",
        color = DarkBrown,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(16.dp))
    SilverSaltIcon()
    Spacer(Modifier.height(18.dp))
    Text(
        "銀鹽是修復古蹟記憶的材料。探索古蹟、完成問答可以獲得銀鹽，累積足夠後即可將解鎖的古蹟建立在你的地圖上。",
        color = Color(0xFF594A43),
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "在這裡，時光銀鹽是你用雙腳換來的時光通行證。",
        color = DarkBrown,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HowToUseSilverSalt() {
    Text(
        "如何使用時光銀鹽？",
        color = DarkBrown,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
    Text("探索越多，沖洗越多", color = Color(0xFF9B918B), fontSize = 11.sp)
    Spacer(Modifier.height(22.dp))
    HelpStep(
        number = "1",
        title = "收集銀鹽",
        description = "跑步探索古蹟並完成 Podcast；銀鹽不足時，也能回答古蹟問題取得銀鹽。"
    )
    Spacer(Modifier.height(16.dp))
    HelpStep(
        number = "2",
        title = "使用銀鹽，收藏歷史！",
        description = "舊照片解鎖後，使用銀鹽把古蹟建立成地圖上的 2.5D 建築。"
    )
}

@Composable
private fun SilverSaltIcon() {
    Surface(
        modifier = Modifier.size(76.dp),
        shape = CircleShape,
        color = Cream
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                tint = Color(0xFF6F665F),
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun HelpStep(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(DarkBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(Modifier.size(10.dp))
        Column {
            Text(title, color = DarkBrown, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                description,
                color = Color(0xFF675851),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
