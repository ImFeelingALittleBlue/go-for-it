package com.example.goforit.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.goforit.R
import com.example.goforit.data.Heritage
import com.example.goforit.data.MapBuildRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.SilverSaltStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val BUILD_COST = 100

@Composable
fun HeritageDetailSheet(
    heritage: Heritage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val points by SilverSaltStore.points(context)
    val restorationRecord = RestorationRepository.records()
        .firstOrNull { it.heritageId == heritage.id }
    val isRestored = restorationRecord != null
    val isBuilt = MapBuildRepository.records().any { it.heritageId == heritage.id }

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
                                    "解鎖於 ${formatUnlockTime(restorationRecord?.restoredAt ?: 0L)}",
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
                            isBuilt = isBuilt,
                            points = points,
                            onBuild = {
                                if (SilverSaltStore.spend(context, BUILD_COST)) {
                                    MapBuildRepository.add(heritage)
                                }
                            }
                        )
                    }
                }
            }
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
    isBuilt: Boolean,
    points: Int,
    onBuild: () -> Unit
) {
    when {
        !isRestored -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFC9C9C9))
            ) {
                Text("前往探索以解鎖舊照片", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        isBuilt -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF9B6A3F))
            ) {
                Icon(Icons.Default.Business, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("已創建於我的地圖", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        else -> {
            val enough = points >= BUILD_COST
            Button(
                onClick = onBuild,
                enabled = enough,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B6A3F))
            ) {
                Text(
                    if (enough) "創建我的地圖（花費 $BUILD_COST 銀鹽）"
                    else "銀鹽不足（需 $BUILD_COST，目前 $points）",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
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
