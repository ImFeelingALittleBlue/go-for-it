package com.example.goforit.ui.home

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.goforit.data.Heritage
import com.example.goforit.data.HeritageRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.SilverSaltStore
import com.example.goforit.ui.applyWarmMapStyle
import com.example.goforit.ui.common.SilverSaltAssetIcon
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.OnCircleAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.gms.common.api.ApiException
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.delay

val OrangeAccent = Color(0xFFD4822A)    // 設計稿主色
val TextGray     = Color(0xFF888888)
val ChipBg       = Color(0xFFF0EDE8)
enum class HeritageFilter { ALL, RESTORED, PENDING }

private data class SelectedPlace(
    val name: String,
    val address: String,
    val point: Point
)

@Composable
fun HomeScreen(
    onStartExplore: () -> Unit = {},
    onOpenExplore: () -> Unit = onStartExplore,
    onOpenAccount: () -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView        = remember { MapView(context) }
    val placesClient = remember {
        if (Places.isInitialized()) Places.createClient(context) else null
    }

    // 讀取 CSV 裡的古蹟資料（只在第一次組畫面時讀一次）
    val heritages = remember { HeritageRepository.loadHeritages(context) }
    val restorationRecords = RestorationRepository.records().toList()
    val restoredIds = restorationRecords.map { it.heritageId }.toSet()
    var heritageFilter by remember { mutableStateOf(HeritageFilter.ALL) }
    // 古蹟清單是否展開（收合時把空間讓給地圖）
    var listExpanded by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember {
        mutableStateOf<List<AutocompletePrediction>>(emptyList())
    }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedPlace by remember { mutableStateOf<SelectedPlace?>(null) }
    var searchSessionToken by remember { mutableStateOf(AutocompleteSessionToken.newInstance()) }
    val statusFilteredHeritages = when (heritageFilter) {
        HeritageFilter.ALL -> heritages
        HeritageFilter.RESTORED -> heritages.filter { it.id in restoredIds }
        HeritageFilter.PENDING -> heritages.filter { it.id !in restoredIds }
    }
    val visibleHeritages = statusFilteredHeritages
    var markerManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    var restoredMarkerManager by remember { mutableStateOf<PointAnnotationManager?>(null) }
    var searchMarkerManager by remember { mutableStateOf<CircleAnnotationManager?>(null) }
    val markerLookup = remember { mutableMapOf<String, Heritage>() }
    var markerClickListenerInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        searchError = null
        val query = searchQuery.trim()
        if (query.length < 2 || selectedPlace?.name == query) {
            searchSuggestions = emptyList()
            return@LaunchedEffect
        }
        if (placesClient == null) {
            searchError = "尚未設定 Google Maps API key"
            return@LaunchedEffect
        }
        delay(350L)
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries("TW")
            .setSessionToken(searchSessionToken)
            .build()
        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (searchQuery.trim() == query) {
                    searchSuggestions = response.autocompletePredictions.take(5)
                }
            }
            .addOnFailureListener { exception ->
                if (searchQuery.trim() == query) {
                    searchSuggestions = emptyList()
                    Log.e("PlacesSearch", "Autocomplete failed", exception)
                    searchError = placesErrorMessage(exception)
                }
            }
    }

    // 地圖跟著 Activity 生命週期啟動 / 暫停 / 銷毀
    DisposableEffect(lifecycleOwner) {
        val obs = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner)   { mapView.onStart() }
            override fun onStop(owner: LifecycleOwner)    { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); mapView.onDestroy() }
    }

    // 權限狀態：用 state 追蹤，這樣變更時 AndroidView 的 update 會被觸發
    var locationGranted by remember { mutableStateOf(false) }
    rememberLocationPermission { locationGranted = true }

    // 目前被點選的古蹟（null = 沒有點任何古蹟，不顯示詳情卡）
    var selected by remember { mutableStateOf<Heritage?>(null) }

    // 篩選標籤切換時，直接用現有 manager 重畫標記（不依賴 AndroidView update 的非同步 getStyle）
    LaunchedEffect(heritageFilter, markerManager, restoredMarkerManager, restoredIds) {
        val pendingManager = markerManager ?: return@LaunchedEffect
        val photoManager = restoredMarkerManager ?: return@LaunchedEffect
        setHeritageMarkers(
            context,
            pendingManager,
            photoManager,
            visibleHeritages,
            restoredIds,
            markerLookup
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // ── 搜尋列 ──────────────────────────────────────────────────────────
        Box(modifier = Modifier.zIndex(2f)) {
            HomeSearchBar(
                query = searchQuery,
                onQueryChange = {
                    searchQuery = it
                    selectedPlace = null
                }
            )
            if (searchSuggestions.isNotEmpty() || searchError != null) {
                PlaceSuggestionsCard(
                    suggestions = searchSuggestions,
                    error = searchError,
                    onSelect = { prediction ->
                        val client = placesClient ?: return@PlaceSuggestionsCard
                        val request = FetchPlaceRequest.builder(
                            prediction.placeId,
                            listOf(
                                Place.Field.DISPLAY_NAME,
                                Place.Field.FORMATTED_ADDRESS,
                                Place.Field.LOCATION
                            )
                        )
                            .setSessionToken(searchSessionToken)
                            .build()
                        client.fetchPlace(request)
                            .addOnSuccessListener { response ->
                                val place = response.place
                                val latLng = place.location ?: return@addOnSuccessListener
                                val result = SelectedPlace(
                                    name = place.displayName ?: prediction.getPrimaryText(null).toString(),
                                    address = place.formattedAddress.orEmpty(),
                                    point = Point.fromLngLat(latLng.longitude, latLng.latitude)
                                )
                                selectedPlace = result
                                searchQuery = result.name
                                searchSuggestions = emptyList()
                                searchSessionToken = AutocompleteSessionToken.newInstance()
                            }
                            .addOnFailureListener { exception ->
                                Log.e("PlacesSearch", "Place details failed", exception)
                                searchError = placesErrorMessage(exception)
                            }
                    }
                )
            }
        }

        // ── 時光銀鹽累計橫條 ────────────────────────────────────────────────
        SilverSaltBanner()

        // ── 地圖：清單收合時撐滿剩餘空間，展開時占 55% ──────────────────────
        Box(modifier = if (listExpanded) Modifier.weight(0.55f) else Modifier.weight(1f)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        gestures.updateSettings {
                            pinchToZoomEnabled = true
                            pinchScrollEnabled = true
                            scrollEnabled = true
                            doubleTapToZoomInEnabled = true
                            doubleTouchToZoomOutEnabled = true
                            quickZoomEnabled = true
                        }
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
                            applyWarmMapStyle(it)
                            mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat(120.2028, 23.0000))
                                    .zoom(13.0)
                                    .pitch(48.0)
                                    .bearing(-18.0)
                                    .build()
                            )
                        }
                    }
                },
                // update：當 locationGranted 變成 true 時被呼叫，此時樣式已載入完成
                update = { mv ->
                    if (locationGranted) {
                        mv.location.updateSettings { enabled = true }
                    }
                    mv.mapboxMap.getStyle {
                        val manager = markerManager
                            ?: mv.annotations.createCircleAnnotationManager().also { markerManager = it }
                        val photoManager = restoredMarkerManager
                            ?: mv.annotations.createPointAnnotationManager().also {
                                restoredMarkerManager = it
                            }
                        val placeManager = searchMarkerManager
                            ?: mv.annotations.createCircleAnnotationManager().also {
                                searchMarkerManager = it
                            }
                        if (!markerClickListenerInstalled) {
                            manager.addClickListener(
                                OnCircleAnnotationClickListener { annotation ->
                                    markerLookup[annotation.id]?.let { selected = it }
                                    true
                                }
                            )
                            markerClickListenerInstalled = true
                            photoManager.addClickListener(
                                OnPointAnnotationClickListener { annotation ->
                                    markerLookup[annotation.id]?.let { selected = it }
                                    true
                                }
                            )
                        }
                        setHeritageMarkers(
                            context,
                            manager,
                            photoManager,
                            visibleHeritages,
                            restoredIds,
                            markerLookup
                        )
                        placeManager.deleteAll()
                        selectedPlace?.let { place ->
                            placeManager.create(
                                CircleAnnotationOptions()
                                    .withPoint(place.point)
                                    .withCircleRadius(11.0)
                                    .withCircleColor("#356AE6")
                                    .withCircleStrokeWidth(3.0)
                                    .withCircleStrokeColor("#FFFFFF")
                            )
                            mv.mapboxMap.setCamera(
                                CameraOptions.Builder()
                                    .center(place.point)
                                    .zoom(16.0)
                                    .build()
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // 地圖底部的兩顆按鈕
            MapOverlayButtons(
                onMissingPlaceClick = onOpenAccount,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // ── 全部古蹟列表：展開時占 45%，收合時只留標題列 ────────────────────
        MapHeritageSection(
            heritages = heritages,
            records = restorationRecords,
            selectedFilter = heritageFilter,
            onFilterChange = { heritageFilter = it },
            onHeritageClick = { selected = it },
            expanded = listExpanded,
            onExpandedChange = { listExpanded = it },
            modifier = if (listExpanded) Modifier.weight(0.70f) else Modifier
        )
    }

    // ── 古蹟詳情卡：selected 有值才顯示 ──────────────────────────────────────
    selected?.let { heritage ->
        HeritageDetailSheet(
            heritage = heritage,
            onDismiss = { selected = null },
            onStartExplore = {
                selected = null
                onOpenExplore()
            }
        )
    }
}

private fun setHeritageMarkers(
    context: android.content.Context,
    pendingManager: CircleAnnotationManager,
    restoredManager: PointAnnotationManager,
    heritages: List<Heritage>,
    restoredIds: Set<Int>,
    markerLookup: MutableMap<String, Heritage>
) {
    pendingManager.deleteAll()
    restoredManager.deleteAll()
    markerLookup.clear()
    val pendingHeritages = heritages.filter { it.id !in restoredIds }
    val pendingOptions = pendingHeritages.map { h ->
        CircleAnnotationOptions()
            .withPoint(Point.fromLngLat(h.lng, h.lat))
            .withCircleRadius(8.0)
            .withCircleColor("#888888")
            .withCircleStrokeWidth(2.0)
            .withCircleStrokeColor("#FFFFFF")
    }
    val pendingCreated = pendingManager.create(pendingOptions)
    markerLookup.putAll(
        pendingCreated.zip(pendingHeritages).associate { (annotation, h) -> annotation.id to h }
    )

    val restoredMarkers = heritages.filter { it.id in restoredIds }.mapNotNull { heritage ->
        val bitmap = runCatching {
            context.assets.open("thumbnails/${heritage.photoFile}")
                .use(BitmapFactory::decodeStream)
        }.getOrNull() ?: return@mapNotNull null
        heritage to PointAnnotationOptions()
            .withPoint(Point.fromLngLat(heritage.lng, heritage.lat))
            .withIconImage(bitmap)
            .withIconSize(2.6)
    }
    val restoredCreated = restoredManager.create(restoredMarkers.map { it.second })
    markerLookup.putAll(
        restoredCreated.zip(restoredMarkers.map { it.first })
            .associate { (annotation, h) -> annotation.id to h }
    )
}

private fun placesErrorMessage(exception: Exception): String {
    val detail = exception.message?.substringBefore('\n')?.takeIf { it.isNotBlank() }
    val status = (exception as? ApiException)?.statusCode
    return when {
        status != null && detail != null -> "Google Places 錯誤 $status：$detail"
        status != null -> "Google Places 錯誤 $status，請檢查 API key 限制與 Places API (New)"
        detail != null -> "無法搜尋地點：$detail"
        else -> "無法搜尋地點，請檢查網路與 Google Places 設定"
    }
}

@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = ChipBg,
        shadowElevation = 2.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜尋你的地圖", color = TextGray, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextGray)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清除搜尋", tint = TextGray)
                    }
                }
            } else {
                null
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun PlaceSuggestionsCard(
    suggestions: List<AutocompletePrediction>,
    error: String?,
    onSelect: (AutocompletePrediction) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 72.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column {
            if (error != null) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            } else {
                suggestions.forEachIndexed { index, prediction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(prediction) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prediction.getPrimaryText(null).toString(),
                                color = Color(0xFF2F2925),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val secondaryText = prediction.getSecondaryText(null).toString()
                            if (secondaryText.isNotBlank()) {
                                Text(
                                    text = secondaryText,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = Color(0xFFEDE9E4)
                        )
                    }
                }
                Text(
                    text = "Powered by Google",
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    color = TextGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun MapOverlayButtons(
    onMissingPlaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        MapPill("找不到你要的地點？", onClick = onMissingPlaceClick)
    }
}

@Composable
fun MapPill(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = Color(0xFF1A1A1A)
        )
    }
}

// 搜尋列下方：時光銀鹽累計橫條
@Composable
private fun SilverSaltBanner() {
    val context = LocalContext.current
    val silver by SilverSaltStore.points(context)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SilverSaltAssetIcon(modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            // 中間文字
            Column(modifier = Modifier.weight(1f)) {
                Text("時光銀鹽", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C2218))
                Text("依步數及速度累計", fontSize = 11.sp, color = TextGray)
            }
            // 右側累計數字
            Text(
                text = "$silver",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OrangeAccent
            )
        }
    }
}
