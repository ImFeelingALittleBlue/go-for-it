package com.example.goforit.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView

@Composable
fun LocateMeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.46f),
        shape = CircleShape,
        color = Color(0xFFFFFDF8),
        shadowElevation = 5.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "定位到目前位置",
                tint = Color(0xFF6E5E52),
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@SuppressLint("MissingPermission")
fun currentLocationPoint(context: Context): Point? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }
            }.getOrNull()
        }
        .maxByOrNull { it.time }
        ?.let { Point.fromLngLat(it.longitude, it.latitude) }
}

fun moveMapToPoint(
    mapView: MapView,
    point: Point,
    zoom: Double = 16.0,
    pitch: Double? = null,
    bearing: Double? = null
) {
    val camera = CameraOptions.Builder()
        .center(point)
        .zoom(zoom)
        .also { builder ->
            pitch?.let(builder::pitch)
            bearing?.let(builder::bearing)
        }
        .build()
    mapView.mapboxMap.setCamera(camera)
}
