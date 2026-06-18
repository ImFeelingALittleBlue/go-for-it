package com.example.goforit.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.goforit.R
import com.example.goforit.data.Heritage
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.SilverSaltStore
import com.example.goforit.ui.common.SilverSaltHelpButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val RESTORE_COST = 100

private enum class QuizState { ASKING, CORRECT, WRONG }

@Composable
fun HeritageDetailSheet(
    heritage: Heritage,
    onDismiss: () -> Unit,
    onStartExplore: () -> Unit = onDismiss
) {
    val context = LocalContext.current
    val points by SilverSaltStore.points(context)
    val restorationRecord = RestorationRepository.records()
        .firstOrNull { it.heritageId == heritage.id }
    val isRestored = restorationRecord != null
    var quizState by remember(heritage.id) { mutableStateOf<QuizState?>(null) }
    var showDeleteDialog by remember(heritage.id) { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }
    val hasLocationPermission = rememberLocationPermission { locationGranted = true }
    val isNearHeritage by rememberIsNearHeritage(
        heritage = heritage,
        hasLocationPermission = hasLocationPermission || locationGranted
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8F3EB)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    DetailTopBar(isRestored = isRestored, onBack = onDismiss)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (isRestored) {
                            HeritagePhoto(heritage.photoFile)
                        } else if (quizState != null) {
                            HeritageQuizHero(
                                heritage = heritage,
                                points = points,
                                state = quizState ?: QuizState.ASKING,
                                onAnswer = { isCorrect ->
                                    if (isCorrect) {
                                        val reward = (RESTORE_COST - points).coerceAtLeast(0) + 2
                                        if (reward > 0) {
                                            SilverSaltStore.add(context, reward)
                                        }
                                        quizState = QuizState.CORRECT
                                    } else {
                                        quizState = QuizState.WRONG
                                    }
                                },
                                onRetry = { quizState = QuizState.ASKING },
                                onCancel = { quizState = null }
                            )
                        } else {
                            CurrentStreetView(heritage)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (heritage.year.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(8.dp), color = ChipBg) {
                                    Text(
                                        "${heritage.year} 年",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 12.sp,
                                        color = Color(0xFF7A6A55)
                                    )
                                }
                            }

                            if (isRestored) {
                                Text(
                                    "解鎖於 ${formatUnlockTime(
                                        restorationRecord?.restoredAt ?: 0L
                                    )}",
                                    fontSize = 12.sp,
                                    color = OrangeAccent
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            heritage.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF2D1D19)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            heritage.description,
                            fontSize = 14.sp,
                            color = Color(0xFF3F3935),
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(24.dp))

                        HeritageActionButton(
                            isRestored = isRestored,
                            isNearHeritage = isNearHeritage,
                            points = points,
                            onRestore = {
                                if (SilverSaltStore.spend(context, RESTORE_COST)) {
                                    RestorationRepository.add(heritage)
                                }
                            },
                            onNeedSilver = { quizState = QuizState.ASKING },
                            onExplore = onStartExplore
                        )

                        if (isRestored) {
                            Spacer(Modifier.height(12.dp))
                            DeleteRecordButton(
                                text = "刪除已修復紀錄",
                                onClick = { showDeleteDialog = true }
                            )
                        }
                        }
                    }
                }

                SilverSaltHelpButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteRecordDialog(
            onConfirm = {
                restorationRecord?.docId
                    ?.takeIf { it.isNotBlank() }
                    ?.let(RestorationRepository::delete)
                showDeleteDialog = false
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun NearbyUnlockHero(
    heritage: Heritage,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(heritage.photoFile) {
        runCatching {
            context.assets.open("photos/${heritage.photoFile}")
                .use(BitmapFactory::decodeStream)
                .asImageBitmap()
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFB5ADA3)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(14.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF6B625B).copy(alpha = 0.46f))
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFF8F3EB)
            ) {
                Icon(
                    Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = Color(0xFF615951),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "解鎖舊貌",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFF6E6863).copy(alpha = 0.78f)
            ) {
                Text(
                    "✓ 已靠近此古蹟\n｜使用銀鹽即可修復老照片",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onRestore,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF8F3EB),
                    contentColor = Color(0xFF8E5D3B)
                )
            ) {
                Text("使用 $RESTORE_COST 銀鹽修復")
            }
        }
    }
}

@Composable
private fun InsufficientSilverHero(
    points: Int,
    onQuizClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF777777))
    ) {
        PerspectiveGrid(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.height(68.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFFF8F3EB)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color(0xFF6A625B)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "修復老照片",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF8C8A86)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("修復所需時光銀鹽", color = Color.White, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { (points / RESTORE_COST.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp),
                            color = Color(0xFFF6E5B5),
                            trackColor = Color(0xFFD4D0C9)
                        )
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("$points / $RESTORE_COST", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onQuizClick,
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF8F3EB),
                    contentColor = Color(0xFF8E5D3B)
                )
            ) {
                Text("以問答補足")
            }
        }
    }
}

@Composable
private fun PerspectiveGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val horizon = size.height * 0.48f
        val gridColor = Color.White.copy(alpha = 0.32f)

        for (index in -7..7) {
            val bottomX = size.width / 2f + index * size.width / 7f
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, horizon),
                end = androidx.compose.ui.geometry.Offset(bottomX, size.height),
                strokeWidth = 2f
            )
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(size.width / 2f, horizon),
                end = androidx.compose.ui.geometry.Offset(bottomX, 0f),
                strokeWidth = 2f
            )
        }

        listOf(0.06f, 0.15f, 0.27f, 0.39f, 0.61f, 0.73f, 0.85f, 0.94f).forEach {
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height * it),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height * it),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun DetailTopBar(
    isRestored: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF302A26)
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isRestored) Color(0xFF4CAF50) else Color(0xFF1F1F1F)
        ) {
            Text(
                if (isRestored) "✓ 已解鎖" else "X 未解鎖",
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CurrentStreetView(heritage: Heritage) {
    val context = LocalContext.current
    val configuredKey = context.getString(R.string.street_view_api_key)
    val apiKey = configuredKey.ifBlank { context.getString(R.string.google_api_key) }
    val streetViewUrl = remember(heritage.id, apiKey) {
        val location = URLEncoder.encode(
            "${heritage.lat},${heritage.lng}",
            Charsets.UTF_8.name()
        )
        "https://maps.googleapis.com/maps/api/streetview" +
            "?size=640x400" +
            "&location=$location" +
            "&heading=${heritage.heading}" +
            "&pitch=${heritage.pitch}" +
            "&fov=80" +
            "&radius=100" +
            "&return_error_code=true" +
            "&key=$apiKey"
    }
    var state by remember(heritage.id) {
        mutableStateOf<StreetViewState>(StreetViewState.Loading)
    }

    LaunchedEffect(streetViewUrl) {
        state = loadStreetView(streetViewUrl)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFE7E1D8)),
        contentAlignment = Alignment.Center
    ) {
        when (val result = state) {
            StreetViewState.Loading -> CircularProgressIndicator(color = OrangeAccent)
            is StreetViewState.Success -> Image(
                bitmap = result.bitmap.asImageBitmap(),
                contentDescription = "${heritage.name}目前街景",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            is StreetViewState.Error -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    "目前街景無法顯示",
                    color = Color(0xFF554D47),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result.message,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private sealed interface StreetViewState {
    data object Loading : StreetViewState
    data class Success(val bitmap: Bitmap) : StreetViewState
    data class Error(val message: String) : StreetViewState
}

private suspend fun loadStreetView(url: String): StreetViewState = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val bitmap = connection.inputStream.use(BitmapFactory::decodeStream)
                    if (bitmap != null) {
                        StreetViewState.Success(bitmap)
                    } else {
                        StreetViewState.Error("街景圖片格式無法讀取")
                    }
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    val errorBody = connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    StreetViewState.Error(streetViewErrorMessage(errorBody))
                }
                HttpURLConnection.HTTP_NOT_FOUND ->
                    StreetViewState.Error("這個地點附近目前沒有可用街景")
                else ->
                    StreetViewState.Error("街景服務暫時無法連線（${connection.responseCode}）")
            }
        } finally {
            connection.disconnect()
        }
    }.getOrElse {
        StreetViewState.Error("請確認網路連線後再試一次")
    }
}

private fun streetViewErrorMessage(errorBody: String): String {
    val googleMessage = runCatching {
        JSONObject(errorBody).optString("error_message")
    }.getOrDefault("")

    return when {
        googleMessage.contains("not authorized", ignoreCase = true) ->
            "目前的 Google API key 未授權使用 Street View Static API"
        googleMessage.contains("not activated", ignoreCase = true) ->
            "請先在 Google Cloud 啟用 Street View Static API"
        googleMessage.contains("billing", ignoreCase = true) ->
            "請先為 Google Cloud 專案啟用計費"
        else ->
            "Google Street View 拒絕了這次請求"
    }
}

@Composable
private fun HeritageActionButton(
    isRestored: Boolean,
    isNearHeritage: Boolean,
    points: Int,
    onRestore: () -> Unit,
    onNeedSilver: () -> Unit,
    onExplore: () -> Unit
) {
    when {
        isRestored -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9B6A3F),
                    disabledContainerColor = Color(0xFFC9C9C9)
                )
            ) {
                Text("已修復", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        !isNearHeritage -> {
            Button(
                onClick = onExplore,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B6A3F))
            ) {
                Text("前往古蹟附近以修復老照片", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        else -> {
            val enough = points >= RESTORE_COST
            Button(
                onClick = if (enough) onRestore else onNeedSilver,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B6A3F))
            ) {
                Text(
                    if (enough) "以時光銀鹽解鎖老照片"
                    else "問答補足時光銀鹽，以解鎖老照片",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DeleteRecordButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFFB5483B)
        ),
        elevation = null
    ) {
        Icon(Icons.Default.DeleteOutline, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(text)
    }
}

@Composable
private fun DeleteRecordDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "刪除已修復紀錄？",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "老照片會重新鎖定，地圖標記也會恢復為待修復圓點。"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB5483B))
            ) {
                Text("確認刪除")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextGray
                ),
                elevation = null
            ) {
                Text("取消")
            }
        },
        containerColor = Color(0xFFF8F3EB)
    )
}

@Composable
private fun HeritageQuizHero(
    heritage: Heritage,
    points: Int,
    state: QuizState,
    onAnswer: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val answer = heritage.year.ifBlank { heritage.name }
    val question = if (heritage.year.isNotBlank()) {
        "這張歷史照片標示的年代是？"
    } else {
        "你剛剛解鎖的這處古蹟名稱是？"
    }
    val choices = remember(heritage.id) {
        if (heritage.year.isNotBlank()) {
            buildYearChoices(heritage.year)
        } else {
            listOf(heritage.name, "臺南州廳", "赤崁樓").shuffled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF777777))
    ) {
        PerspectiveGrid(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SilverProgressCard(points = points)
            Spacer(Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF351A15).copy(alpha = 0.94f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "「${heritage.name}」$question",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    when (state) {
                        QuizState.ASKING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                choices.take(2).forEach { choice ->
                                    QuizChoiceButton(
                                        text = choice,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onAnswer(choice == answer) }
                                    )
                                }
                            }
                            choices.drop(2).forEach { choice ->
                                Spacer(Modifier.height(8.dp))
                                QuizChoiceButton(
                                    text = choice,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onAnswer(choice == answer) }
                                )
                            }
                        }
                        QuizState.CORRECT -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF435C35)
                            ) {
                                Text(
                                    "恭喜答對喔",
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        QuizState.WRONG -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6B2722)
                            ) {
                                Text(
                                    "答錯囉",
                                    modifier = Modifier.padding(vertical = 11.dp),
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (state) {
                            QuizState.ASKING -> "答對即可獲得時光銀鹽"
                            QuizState.CORRECT -> "獲得時光銀鹽，已達修復門檻"
                            QuizState.WRONG -> "沒有關係，再試一次"
                        },
                        color = Color(0xFFCDBFB8),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state != QuizState.CORRECT) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF8F3EB),
                            contentColor = Color(0xFF7A563D)
                        )
                    ) {
                        Text("取消解答", fontSize = 12.sp)
                    }
                }
                if (state == QuizState.WRONG) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF8F3EB),
                            contentColor = Color(0xFF7A563D)
                        )
                    ) {
                        Text("再試一次", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SilverProgressCard(points: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF8B8985).copy(alpha = 0.9f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text("修復所需時光銀鹽", color = Color.White, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (points / RESTORE_COST.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp),
                    color = Color(0xFFF6E5B5),
                    trackColor = Color(0xFFD0CDC7)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("$points / $RESTORE_COST", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun QuizChoiceButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF5B2B25),
            contentColor = Color.White
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp,
            vertical = 8.dp
        )
    ) {
        Text(text, fontSize = 11.sp)
    }
}

private fun buildYearChoices(year: String): List<String> {
    val numericYear = year.toIntOrNull()
    val distractors = if (numericYear != null) {
        listOf((numericYear - 10).toString(), (numericYear + 10).toString())
    } else {
        listOf("1910", "1930")
    }
    return (listOf(year) + distractors.filter { it != year }).distinct().take(3).shuffled()
}

private fun formatUnlockTime(millis: Long): String =
    if (millis > 0L) SimpleDateFormat("yyyy/M/d HH:mm", Locale.TAIWAN).format(Date(millis))
    else "未知時間"

@Composable
private fun HeritagePhoto(photoFile: String) {
    val context = LocalContext.current
    val bitmap = remember(photoFile) {
        runCatching {
            context.assets.open("photos/$photoFile").use { BitmapFactory.decodeStream(it) }
                .asImageBitmap()
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFFE7E1D8)),
            contentAlignment = Alignment.Center
        ) {
            Text("找不到老照片", color = TextGray, fontSize = 13.sp)
        }
    }
}
